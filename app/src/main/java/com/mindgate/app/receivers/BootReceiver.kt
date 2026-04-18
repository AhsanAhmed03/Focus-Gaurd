package com.mindgate.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mindgate.app.services.AppMonitorService
import com.mindgate.app.services.LockScreenService
import com.mindgate.app.utils.AppPreferences

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            Log.d("BootReceiver", "Received: $action — restarting services")
            if (AppPreferences.isServiceEnabled(context)) {
                if (AppPreferences.isLockScreenEnabled(context)) LockScreenService.start(context)
                if (AppPreferences.isAppMonitorEnabled(context)) AppMonitorService.start(context)
            }
        }
    }
}
