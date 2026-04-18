package com.mindgate.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ScreenStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ScreenStateReceiver", "Action: ${intent.action}")
        // Handled by LockScreenService's internal receiver
    }
}
