package com.example.sleepy

import android.app.admin.DevicePolicyManager
import android.content.*
import android.os.Build
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {

    private val CHANNEL = "sleepy/device"
    private val REQUEST_CODE_ENABLE_ADMIN = 1

    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    private var pendingResult: MethodChannel.Result? = null
    private var channel: MethodChannel? = null

    private val timerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == TimerService.ACTION_TICK) {
                val remaining = intent.getIntExtra(TimerService.EXTRA_REMAINING, 0)
                val isRunning = intent.getBooleanExtra("IS_RUNNING", false)
                
                channel?.invokeMethod("onTimerTick", mapOf(
                    "remaining" to remaining,
                    "isRunning" to isRunning
                ))

                if (remaining <= 0 && isRunning) {
                    lockScreen()
                }
            }
        }
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        devicePolicyManager =
            getSystemService(Context.DEVICE_POLICY_SERVICE)
                    as DevicePolicyManager

        adminComponent = ComponentName(
            this,
            MyDeviceAdminReceiver::class.java
        )

        channel = MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            CHANNEL
        )
        
        channel?.setMethodCallHandler { call, result ->
            when (call.method) {
                "isAdminActive" -> {
                    result.success(
                        devicePolicyManager.isAdminActive(adminComponent)
                    )
                }

                "requestAdmin" -> {
                    if (devicePolicyManager.isAdminActive(adminComponent)) {
                        result.success(true)
                    } else if (pendingResult != null) {
                        result.error("ALREADY_REQUESTING", "Already requesting admin rights", null)
                    } else {
                        pendingResult = result
                        requestAdmin()
                    }
                }

                "lockScreen" -> {
                    lockScreen()
                    result.success(null)
                }

                "startTimerService" -> {
                    val seconds = call.argument<Int>("seconds") ?: 0
                    
                    // Запрос разрешения на уведомления для Android 13+
                    if (Build.VERSION.SDK_INT >= 33) {
                        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
                        }
                    }

                    val intent = Intent(this, TimerService::class.java).apply {
                        action = TimerService.ACTION_START
                        putExtra(TimerService.EXTRA_SECONDS, seconds)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }
                    result.success(null)
                }

                "stopTimerService" -> {
                    val intent = Intent(this, TimerService::class.java).apply {
                        action = TimerService.ACTION_STOP
                    }
                    startService(intent)
                    result.success(null)
                }

                else -> {
                    result.notImplemented()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(timerReceiver, IntentFilter(TimerService.ACTION_TICK), Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(timerReceiver, IntentFilter(TimerService.ACTION_TICK))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(timerReceiver)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ENABLE_ADMIN) {
            val isActive = devicePolicyManager.isAdminActive(adminComponent)
            pendingResult?.success(isActive)
            pendingResult = null
        }
    }

    private fun requestAdmin() {
        val intent = Intent(
            DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN
        )

        intent.putExtra(
            DevicePolicyManager.EXTRA_DEVICE_ADMIN,
            adminComponent
        )

        intent.putExtra(
            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            "Sleepy использует это разрешение только для блокировки экрана после окончания таймера."
        )

        startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN)
    }

    private fun lockScreen() {
        if (devicePolicyManager.isAdminActive(adminComponent)) {
            devicePolicyManager.lockNow()
        }
    }
}
