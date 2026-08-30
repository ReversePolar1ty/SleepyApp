package com.example.sleepy

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class TimerService : Service() {

    companion object {
        const val CHANNEL_ID = "TimerServiceChannel"
        const val EXPIRED_CHANNEL_ID = "TimerExpiredChannel"
        const val NOTIFICATION_ID = 101
        const val EXPIRED_NOTIFICATION_ID = 102

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_ADD_TIME = "ACTION_ADD_TIME"
        const val ACTION_TICK = "com.example.sleepy.TICK"

        const val EXTRA_SECONDS = "EXTRA_SECONDS"
        const val EXTRA_REMAINING = "EXTRA_REMAINING"
    }

    private var remainingSeconds = 0
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    private val runnable = object : Runnable {
        override fun run() {
            if (remainingSeconds > 0) {
                remainingSeconds--
                broadcastUpdate()
                updateNotification()
                if (remainingSeconds <= 0) {
                    onTimerExpired()
                    stopTimer()
                } else {
                    handler.postDelayed(this, 1000)
                }
            } else {
                stopTimer()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val seconds = intent.getIntExtra(EXTRA_SECONDS, 0)
                startTimer(seconds)
            }
            ACTION_STOP -> {
                stopTimer()
            }
            ACTION_ADD_TIME -> {
                addTime(600) // +10 минут
            }
        }
        return START_NOT_STICKY
    }

    private fun startTimer(seconds: Int) {
        remainingSeconds = seconds
        android.util.Log.d("Sleepy", "Starting timer: $seconds seconds")
        cancelExpiredNotification()
        if (!isRunning) {
            isRunning = true
            createNotificationChannel()
            if (android.os.Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, buildNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, buildNotification())
            }
            handler.post(runnable)
        } else {
            updateNotification()
            broadcastUpdate()
        }
    }

    private fun onTimerExpired() {
        val locked = ScreenLockHelper.lockScreenIfPossible(this)
        if (!locked) {
            showExpiredFallbackNotification()
        }
    }

    private fun showExpiredFallbackNotification() {
        createExpiredNotificationChannel()

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntentFlags =
            PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

        val contentIntent = PendingIntent.getActivity(this, 3, openAppIntent, pendingIntentFlags)
        val fullScreenIntent = PendingIntent.getActivity(this, 4, openAppIntent, pendingIntentFlags)

        val notification = NotificationCompat.Builder(this, EXPIRED_CHANNEL_ID)
            .setContentTitle(getString(R.string.timer_expired_title))
            .setContentText(getString(R.string.timer_expired_text))
            .setSmallIcon(R.drawable.ic_sleepy_moon)
            .setContentIntent(contentIntent)
            .setFullScreenIntent(fullScreenIntent, true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(EXPIRED_NOTIFICATION_ID, notification)
    }

    private fun cancelExpiredNotification() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(EXPIRED_NOTIFICATION_ID)
    }

    private fun createExpiredNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                EXPIRED_CHANNEL_ID,
                getString(R.string.timer_expired_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.timer_expired_channel_description)
                enableVibration(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun stopTimer() {
        isRunning = false
        handler.removeCallbacks(runnable)
        broadcastUpdate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun addTime(seconds: Int) {
        remainingSeconds += seconds
        broadcastUpdate()
        updateNotification()
    }

    private fun broadcastUpdate() {
        val intent = Intent(ACTION_TICK)
        intent.setPackage("com.example.sleepy")
        intent.putExtra(EXTRA_REMAINING, remainingSeconds)
        intent.putExtra("IS_RUNNING", isRunning)
        android.util.Log.d("Sleepy", "Broadcasting tick: $remainingSeconds")
        sendBroadcast(intent)
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val minutes = remainingSeconds / 60
        val timeText = if (minutes > 0) "$minutes мин" else "< 1 мин"

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val addIntent = Intent(this, NotificationActionReceiver::class.java).apply { action = ACTION_ADD_TIME }
        val addPendingIntent = PendingIntent.getBroadcast(
            this, 1, addIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val stopIntent = Intent(this, NotificationActionReceiver::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this, 2, stopIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Sleepy запущен")
            .setContentText("Осталось: $timeText")
            .setSmallIcon(com.example.sleepy.R.drawable.ic_sleepy_moon)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_input_add, "+10 мин", addPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Отмена", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Timer Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
