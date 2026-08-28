package com.example.sleepy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        
        val serviceIntent = Intent(context, TimerService::class.java).apply {
            this.action = action
        }
        
        // В Android 12+ для запуска сервиса из ресивера могут быть ограничения, 
        // но если сервис уже запущен как Foreground, то startService работает.
        context.startService(serviceIntent)
    }
}
