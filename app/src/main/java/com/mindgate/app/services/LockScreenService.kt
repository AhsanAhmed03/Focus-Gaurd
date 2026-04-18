package com.mindgate.app.services

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.mindgate.app.R
import com.mindgate.app.activities.MainActivity
import com.mindgate.app.activities.MindGateOverlayActivity
import com.mindgate.app.models.TriggerType
import com.mindgate.app.utils.AppPreferences

class LockScreenService : Service() {

    companion object {
        private const val TAG = "LockScreenService"
        private const val CHANNEL_ID = "mindgate_lock_channel"
        private const val NOTIFICATION_ID = 1002
        private const val UNLOCK_COOLDOWN_MS = 0L
        const val ACTION_STOP = "com.mindgate.app.STOP_LOCK_SERVICE"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, LockScreenService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LockScreenService::class.java))
        }
    }

    private var receiverRegistered = false
    private var wakeLock: PowerManager.WakeLock? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "Broadcast received: ${intent?.action}")
            when (intent?.action) {
                Intent.ACTION_USER_PRESENT -> {
                    // USER_PRESENT = user dismissed keyguard (entered PIN / swiped / biometric)
                    if (!AppPreferences.isLockScreenEnabled(this@LockScreenService)) return
                    handleUnlock()
                }
                Intent.ACTION_SCREEN_ON -> {
                    // Fallback for devices with no lock screen — screen on == unlocked
                    if (!AppPreferences.isLockScreenEnabled(this@LockScreenService)) return
                    val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                    if (!km.isKeyguardLocked) handleUnlock()
                }
                Intent.ACTION_USER_UNLOCKED -> {
                    // Fallback for devices with no lock screen — screen on == unlocked
                    if (!AppPreferences.isLockScreenEnabled(this@LockScreenService)) return
                    val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                    if (!km.isKeyguardLocked) handleUnlock()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, buildNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        )
        registerScreenReceiver()
        Log.d(TAG, "LockScreenService started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        if (intent?.action == ACTION_STOP) {
            stopSelf();
            Log.d(TAG, "onStartCommand Returning")
            return START_NOT_STICKY
        }
        // Re-register if the service was restarted by the OS after being killed
        if (!receiverRegistered) registerScreenReceiver()
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Schedule self-restart via AlarmManager when swiped from recents
        Log.d(TAG, "onTaskRemoved — scheduling alarm restart")
        receiverRegistered = false
        scheduleRestart()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (receiverRegistered) {
            try { unregisterReceiver(screenReceiver) } catch (_: Exception) {}
            receiverRegistered = false
        }
        releaseWakeLock()
        // Schedule restart so the service comes back after OS kills it
        scheduleRestart()
        Log.d(TAG, "LockScreenService destroyed — restart scheduled")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun handleUnlock() {
        val now = System.currentTimeMillis()
        // Read from SharedPrefs so cooldown persists across service restarts
        val lastUnlock = AppPreferences.getLastUnlockTime(this)
        if (now - lastUnlock < UNLOCK_COOLDOWN_MS) {
            Log.d(TAG, "Unlock within cooldown — skipping")
            return
        }
        AppPreferences.setLastUnlockTime(this, now)
        Log.d(TAG, "Unlock detected — showing overlay")

        // Acquire a brief wake lock to ensure the activity launches while screen is on
        acquireWakeLock()
        Handler(Looper.getMainLooper()).postDelayed({ releaseWakeLock() }, 3000)

        showOverlayScreen()
    }

    private fun registerScreenReceiver() {
        Log.d(TAG, "Register receiver Method")
        if (receiverRegistered) {
            Log.d(TAG, "Register receiver Returning")
            return
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
            priority = IntentFilter.SYSTEM_HIGH_PRIORITY
        }
        // System broadcasts require RECEIVER_EXPORTED on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(screenReceiver, filter)
        }
        receiverRegistered = true
        Log.d(TAG, "Screen receiver registered")
    }

    private fun showOverlayScreen() {
        try {
            startActivity(Intent(this, MindGateOverlayActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                putExtra(MindGateOverlayActivity.EXTRA_TRIGGER_TYPE, TriggerType.LOCK_SCREEN.name)
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error showing overlay: ${e.message}")
        }
    }

    private fun scheduleRestart() {
        try {
            val pi = PendingIntent.getService(
                applicationContext, 200,
                Intent(applicationContext, LockScreenService::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val triggerAt = SystemClock.elapsedRealtime() + 2000L
            // Use setExactAndAllowWhileIdle so it fires even in Doze mode
            am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi)
            Log.e(TAG, "Alarm Scheduled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule restart: ${e.message}")
        }
    }


    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, "MindGate:LockWakeLock"
            ).also { it.acquire(10 * 60 * 1000L) }  // max 10 min, released in onDestroy
        } catch (e: Exception) {
            Log.e(TAG, "Exception acquiring wakelock: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try { wakeLock?.let { if (it.isHeld) it.release() }; wakeLock = null } catch (e: Exception) {
            Log.e(TAG, "Exception releasing wakelock: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        val ch = NotificationChannel(CHANNEL_ID, "FocusGuard Lock Screen",
            NotificationManager.IMPORTANCE_LOW).apply {
            description = "Detects screen unlock"; setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun buildNotification(): Notification {
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FocusGuard Active")
            .setContentText("Lock screen mindfulness enabled")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pi).setOngoing(true).setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW).build()
    }
}
