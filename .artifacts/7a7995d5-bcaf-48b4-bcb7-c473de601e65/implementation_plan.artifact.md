# План исправления совместимости с Android 15

Приложение «Sleepy» сталкивается с проблемами на Android 15 (API 35) из-за ужесточения правил безопасности, введенных в Android 14 (API 34) и 15. Основные причины: отсутствие типа фоновой службы в манифесте и отсутствие флагов при регистрации динамических ресиверов.

## Предлагаемые изменения

### [Android] [Core]

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/danii/Sleepy/phone_timer/android/app/src/main/AndroidManifest.xml)
- Добавление разрешения `android.permission.FOREGROUND_SERVICE_SPECIAL_USE`.
- Указание `android:foregroundServiceType="specialUse"` для `TimerService`.
- Добавление обязательного мета-свойства для обоснования использования `specialUse`.

#### [MODIFY] [TimerService.kt](file:///C:/Users/danii/Sleepy/phone_timer/android/app/src/main/kotlin/com/example/sleepy/TimerService.kt)
- Добавление импорта `android.content.pm.ServiceInfo`.
- Обновление вызова `startForeground` для передачи типа службы на устройствах с Android 14+.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/danii/Sleepy/phone_timer/android/app/src/main/kotlin/com/example/sleepy/MainActivity.kt)
- Обновление `registerReceiver` для использования флага `RECEIVER_NOT_EXPORTED` на Android 13+, что является обязательным для внутренних событий приложения.

## Verification Plan

### Automated Tests
- Сборка приложения в режиме отладки: `flutter build apk --debug`.

### Manual Verification
- Запуск на устройстве с Android 15.
- Проверка, что приложение открывается без ошибки "очистить кэш".
- Запуск таймера и проверка появления уведомления.
- Проверка блокировки экрана по истечении времени.
