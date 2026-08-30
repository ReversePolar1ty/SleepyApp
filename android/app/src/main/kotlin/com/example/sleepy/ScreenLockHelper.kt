package com.example.sleepy

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context

object ScreenLockHelper {

    fun isAdminActive(context: Context): Boolean {
        val devicePolicyManager =
            context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(context, MyDeviceAdminReceiver::class.java)
        return devicePolicyManager.isAdminActive(adminComponent)
    }

    fun lockScreenIfPossible(context: Context): Boolean {
        val devicePolicyManager =
            context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(context, MyDeviceAdminReceiver::class.java)

        return if (devicePolicyManager.isAdminActive(adminComponent)) {
            devicePolicyManager.lockNow()
            true
        } else {
            false
        }
    }
}
