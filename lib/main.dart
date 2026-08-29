import 'dart:async';
import 'dart:math';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await SystemChrome.setPreferredOrientations([
    DeviceOrientation.portraitUp,
    DeviceOrientation.portraitDown,
  ]);
  final prefs = await SharedPreferences.getInstance();
  final isDark = prefs.getBool('isDark') ?? false;
  
  runApp(PhoneTimerApp(initialIsDark: isDark));
}

class PhoneTimerApp extends StatefulWidget {
  final bool initialIsDark;
  const PhoneTimerApp({super.key, required this.initialIsDark});

  @override
  State<PhoneTimerApp> createState() => _PhoneTimerAppState();
}

class _PhoneTimerAppState extends State<PhoneTimerApp> {
  late ThemeMode _themeMode;

  @override
  void initState() {
    super.initState();
    _themeMode = widget.initialIsDark ? ThemeMode.dark : ThemeMode.light;
  }

  Future<void> _toggleTheme() async {
    final newMode = 
      _themeMode == ThemeMode.light ? ThemeMode.dark : ThemeMode.light;
      
    setState(() {
      _themeMode = newMode;
    });
    
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('isDark', newMode == ThemeMode.dark);
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'Sleepy',
      themeMode: _themeMode,

      // Светлая тема: красно-белая.
      theme: ThemeData(
        brightness: Brightness.light,
        scaffoldBackgroundColor: const Color(0xFFFFFFFF),
        colorScheme: const ColorScheme.light(
          primary: Color(0xFFD32F2F),
          onPrimary: Colors.white,
          secondary: Color(0xFFFF5252),
          surface: Color(0xFFFFFFFF),
          onSurface: Color(0xFF171717),
        ),
        appBarTheme: const AppBarTheme(
          backgroundColor: Color(0xFFFFFFFF),
          foregroundColor: Color(0xFF171717),
          elevation: 0,
          centerTitle: true,
        ),
        useMaterial3: true,
      ),

      // Тёмная тема: оранжево-тёмная.
      darkTheme: ThemeData(
        brightness: Brightness.dark,
        scaffoldBackgroundColor: const Color(0xFF17110D),
        colorScheme: const ColorScheme.dark(
          primary: Color(0xFFFF9800),
          onPrimary: Color(0xFF17110D),
          secondary: Color(0xFFFFB74D),
          surface: Color(0xFF211710),
          onSurface: Color(0xFFFFF3E8),
        ),
        appBarTheme: const AppBarTheme(
          backgroundColor: Color(0xFF17110D),
          foregroundColor: Color(0xFFFFF3E8),
          elevation: 0,
          centerTitle: true,
        ),
        useMaterial3: true,
      ),

      home: PhoneTimerPage(
        onThemeToggle: _toggleTheme,
        isDark: _themeMode == ThemeMode.dark,
      ),
    );
  }
}

class PhoneTimerPage extends StatefulWidget {
  final VoidCallback onThemeToggle;
  final bool isDark;

  const PhoneTimerPage({
    super.key,
    required this.onThemeToggle,
    required this.isDark,
  });

  @override
  State<PhoneTimerPage> createState() => _PhoneTimerPageState();
}

class _PhoneTimerPageState extends State<PhoneTimerPage> with WidgetsBindingObserver {
  static const platform = MethodChannel('sleepy/device');
  int _minutes = 30;
  int _remainingSeconds = 30 * 60;

  bool _running = false;
  bool _isAdmin = false;
  // Timer? _timer; // Больше не нужен, используем системный сервис

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _checkAdminStatus();
    
    platform.setMethodCallHandler((call) async {
      if (call.method == "onTimerTick") {
        final remaining = call.arguments["remaining"] as int;
        final isRunning = call.arguments["isRunning"] as bool;
        
        if (mounted) {
          setState(() {
            _running = isRunning;
            if (isRunning) {
              _remainingSeconds = remaining;
            } else {
              // Если сервис остановился (кнопка "Отмена" в пуше или конец таймера),
              // сбрасываем время до выбранного на диске.
              _remainingSeconds = _minutes * 60;
            }
          });
        }
      }
    });
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _checkAdminStatus();
    }
  }

  Future<void> _checkAdminStatus() async {
    final status = await _isAdminActive();
    if (mounted && status != _isAdmin) {
      setState(() {
        _isAdmin = status;
      });
    }
  }

  Future<void> _lockScreen() async {
    try {
      await platform.invokeMethod('lockScreen');
    } on PlatformException catch (e) {
      debugPrint('Не удалось заблокировать экран: ${e.message}');
    }
  }

  Future<bool> _isAdminActive() async {
    try {
      final result = await platform.invokeMethod<bool>('isAdminActive');
      return result ?? false;
    } on PlatformException catch (e) {
      debugPrint(
        'Не удалось проверить права администратора: ${e.message}',
      );
      return false;
    }
  }

  Future<void> _requestAdmin() async {
    try {
      final bool? result = await platform.invokeMethod<bool>('requestAdmin');
      if (mounted && result != null) {
        setState(() {
          _isAdmin = result;
        });
      }
    } on PlatformException catch (e) {
      debugPrint('PlatformException');
      debugPrint('code: ${e.code}');
      debugPrint('message: ${e.message}');
      debugPrint('details: ${e.details}');
    } catch (e, stackTrace) {
      debugPrint('Неизвестная ошибка: $e');
      debugPrint('$stackTrace');
    }
  }

  void _setMinutes(int minutes) {
    if (_running) return;

    setState(() {
      _minutes = minutes;
      _remainingSeconds = minutes * 60;
    });
  }

  Future<void> _toggleTimer() async {
    if (_running) {
      platform.invokeMethod('stopTimerService');
      setState(() {
        _running = false;
        // Сбрасываем оставшееся время до выбранного на диске
        _remainingSeconds = _minutes * 60;
      });
      return;
    }

    // Запрашиваем разрешение на уведомления перед запуском сервиса
    final bool permissionGranted = await platform.invokeMethod<bool>('requestNotificationPermission') ?? false;
    if (!permissionGranted) {
      // Можно показать Snackbar или диалог, если разрешение отклонено
      return;
    }

    if (_remainingSeconds <= 0) {
      _remainingSeconds = _minutes * 60;
    }

    platform.invokeMethod('startTimerService', {
      'seconds': _remainingSeconds,
    });

    setState(() {
      _running = true;
    });
  }

  String get _timeText {
    final minutes = _remainingSeconds ~/ 60;
    final seconds = _remainingSeconds % 60;

    return '${minutes.toString().padLeft(2, '0')}:'
        '${seconds.toString().padLeft(2, '0')}';
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    return Scaffold(
      appBar: AppBar(
        title: const Text(
          'Sleepy',
          style: TextStyle(
            fontWeight: FontWeight.w600,
          ),
        ),
        actions: [
          IconButton(
            tooltip: 'Сменить тему',
            onPressed: widget.onThemeToggle,
            icon: Icon(
              widget.isDark
                  ? Icons.light_mode_rounded
                  : Icons.dark_mode_rounded,
            ),
          ),
          const SizedBox(width: 8),
        ],
      ),

      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(
              maxWidth: 420,
            ),
            child: Column(
              children: [
                const Spacer(),

                Stack(
                  alignment: Alignment.center,
                  children: [
                    TimerDial(
                      value: _minutes,
                      min: 1,
                      max: 120,
                      onChanged: _setMinutes,
                      enabled: !_running,
                      color: colorScheme.primary,
                      trackColor: colorScheme.primary.withAlpha(45),
                    ),

                    AnimatedSwitcher(
                      duration: const Duration(milliseconds: 150),
                      child: Text(
                        _running ? _timeText : '$_minutes',
                        key: ValueKey(
                          _running ? _timeText : _minutes,
                        ),
                        style: theme.textTheme.displayLarge?.copyWith(
                          fontWeight: FontWeight.w300,
                          fontSize: 56,
                          color: colorScheme.onSurface,
                        ),
                      ),
                    ),
                  ],
                ),

                const Spacer(),
                Padding(
                  padding: const EdgeInsets.only(bottom: 20),
                  child: OutlinedButton.icon(
                    onPressed: _isAdmin ? null : _requestAdmin,
                    icon: Icon(
                      _isAdmin
                          ? Icons.lock_outline_rounded
                          : Icons.lock_open_rounded,
                    ),
                    label: Text(
                      _isAdmin
                          ? 'Блокировка разрешена'
                          : 'Разрешить блокировку',
                    ),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.only(bottom: 48),
                  child: FilledButton(
                    onPressed: _toggleTimer,
                    style: FilledButton.styleFrom(
                      backgroundColor: colorScheme.primary,
                      foregroundColor: colorScheme.onPrimary,
                      padding: const EdgeInsets.all(20),
                      shape: const CircleBorder(),
                    ),
                    child: Icon(
                      _running
                          ? Icons.stop_rounded
                          : Icons.play_arrow_rounded,
                      size: 32,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class TimerDial extends StatelessWidget {
  final int value;
  final int min;
  final int max;
  final ValueChanged<int> onChanged;
  final bool enabled;
  final Color color;
  final Color trackColor;

  const TimerDial({
    super.key,
    required this.value,
    required this.min,
    required this.max,
    required this.onChanged,
    required this.enabled,
    required this.color,
    required this.trackColor,
  });

  int _valueFromPosition(Offset position, Size size) {
    final center = Offset(
      size.width / 2,
      size.height / 2,
    );

    final dx = position.dx - center.dx;
    final dy = position.dy - center.dy;

    // Угол относительно верхней точки круга.
    var angle = atan2(dx, -dy);

    if (angle < 0) {
      angle += 2 * pi;
    }

    final progress = angle / (2 * pi);

    final rawValue = min + progress * (max - min);

    // Всегда округляем до целой минуты.
    return rawValue.round().clamp(min, max);
  }

  void _updateValue(Offset position, Size size) {
    if (!enabled) return;

    final newValue = _valueFromPosition(
      position,
      size,
    );

    if (newValue != value) {
      onChanged(newValue);
    }
  }

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 280,
      height: 280,
      child: LayoutBuilder(
        builder: (context, constraints) {
          final size = Size(
            constraints.maxWidth,
            constraints.maxHeight,
          );

          return GestureDetector(
            behavior: HitTestBehavior.opaque,

            // Можно просто нажать на нужное место.
            onTapDown: enabled
                ? (details) {
              _updateValue(
                details.localPosition,
                size,
              );
            }
                : null,

            // Или вести пальцем.
            onPanStart: enabled
                ? (details) {
              _updateValue(
                details.localPosition,
                size,
              );
            }
                : null,

            onPanUpdate: enabled
                ? (details) {
              _updateValue(
                details.localPosition,
                size,
              );
            }
                : null,

            child: CustomPaint(
              painter: TimerDialPainter(
                value: value,
                min: min,
                max: max,
                color: color,
                trackColor: trackColor,
              ),
            ),
          );
        },
      ),
    );
  }
}

class TimerDialPainter extends CustomPainter {
  final int value;
  final int min;
  final int max;
  final Color color;
  final Color trackColor;

  TimerDialPainter({
    required this.value,
    required this.min,
    required this.max,
    required this.color,
    required this.trackColor,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(
      size.width / 2,
      size.height / 2,
    );

    final radius = size.width / 2 - 18;

    final trackPaint = Paint()
      ..color = trackColor
      ..style = PaintingStyle.stroke
      ..strokeWidth = 6;

    final progressPaint = Paint()
      ..color = color
      ..style = PaintingStyle.stroke
      ..strokeWidth = 6
      ..strokeCap = StrokeCap.round;

    // Полный серый круг.
    canvas.drawCircle(
      center,
      radius,
      trackPaint,
    );

    // Текущий прогресс.
    final progress = (value - min) / (max - min);

    final progressAngle = progress * 2 * pi;

    canvas.drawArc(
      Rect.fromCircle(
        center: center,
        radius: radius,
      ),
      -pi / 2,
      progressAngle,
      false,
      progressPaint,
    );

    // Позиция ползунка.
    final knobAngle = -pi / 2 + progressAngle;

    final knobCenter = Offset(
      center.dx + radius * cos(knobAngle),
      center.dy + radius * sin(knobAngle),
    );

    // Небольшая тень под ползунком.
    final shadowPaint = Paint()
      ..color = Colors.black.withAlpha(35)
      ..maskFilter = const MaskFilter.blur(
        BlurStyle.normal,
        4,
      );

    canvas.drawCircle(
      knobCenter.translate(0, 2),
      12,
      shadowPaint,
    );

    // Сам ползунок.
    final knobPaint = Paint()
      ..color = color;

    canvas.drawCircle(
      knobCenter,
      11,
      knobPaint,
    );

    // Маленький центр.
    final innerPaint = Paint()
      ..color = ThemeData.estimateBrightnessForColor(color) ==
          Brightness.light
          ? Colors.black.withAlpha(30)
          : Colors.white.withAlpha(80);

    canvas.drawCircle(
      knobCenter,
      4,
      innerPaint,
    );
  }

  @override
  bool shouldRepaint(
      covariant TimerDialPainter oldDelegate,
      ) {
    return oldDelegate.value != value ||
        oldDelegate.color != color ||
        oldDelegate.trackColor != trackColor;
  }
}