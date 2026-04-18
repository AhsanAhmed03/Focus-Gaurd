package com.mindgate.app.services

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.mindgate.app.R
import com.mindgate.app.activities.MainActivity
import com.mindgate.app.activities.MindGateOverlayActivity
import com.mindgate.app.models.TriggerType
import com.mindgate.app.utils.AppPreferences

class AppMonitorService : Service() {

    companion object {
        private const val TAG = "AppMonitorService"
        private const val CHANNEL_ID = "mindgate_monitor_channel"
        private const val NOTIFICATION_ID = 1001
        private const val CHECK_INTERVAL = 500L
        private const val COOLDOWN_PREFS = "mindgate_cooldown"
        private const val APP_COOLDOWN_MS = 0L
        const val ACTION_STOP = "com.mindgate.app.STOP_MONITOR"

        fun start(context: Context) {
            context.startForegroundService(Intent(context, AppMonitorService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AppMonitorService::class.java))
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private lateinit var cooldownPrefs: SharedPreferences
    private var wakeLock: PowerManager.WakeLock? = null

    private val checkRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                checkForegroundApp()
                handler.postDelayed(this, CHECK_INTERVAL)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        cooldownPrefs = getSharedPreferences(COOLDOWN_PREFS, Context.MODE_PRIVATE)
        createNotificationChannel()
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, buildNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        )
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        if (intent?.action == ACTION_STOP) {
            stopSelf();
            Log.d(TAG, "onStartCommand Returning")
            return START_NOT_STICKY
        }
        if (!isRunning) {
            isRunning = true
            handler.post(checkRunnable)
            Log.d(TAG, "AppMonitorService polling started")
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "onTaskRemoved — scheduling restart")
        isRunning = false
        scheduleRestart()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        releaseWakeLock()
        scheduleRestart()
        Log.d(TAG, "AppMonitorService destroyed — restart scheduled")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun checkForegroundApp() {
        if (!AppPreferences.isAppMonitorEnabled(this)) return

        val currentApp = getActualForegroundApp() ?: return
        if (currentApp == packageName) {
            AppPreferences.setLastForegroundApp(this, currentApp)
            return
        }
        if (isSystemPackage(currentApp)) {
            AppPreferences.setLastForegroundApp(this, currentApp)
            return
        }

        // Read persisted last app — survives service restart so we don't re-trigger on restart
        val lastApp = AppPreferences.getLastForegroundApp(this)
        if (currentApp == lastApp) return

        Log.d(TAG, "Foreground changed: $lastApp → $currentApp")
        AppPreferences.setLastForegroundApp(this, currentApp)

        val selectedApps = AppPreferences.getSelectedApps(this)
        if (selectedApps.isEmpty() || !selectedApps.contains(currentApp)) return

        val now = System.currentTimeMillis()
        val lastShown = cooldownPrefs.getLong(currentApp, 0L)

        if (now - lastShown > APP_COOLDOWN_MS) {
            Log.d(TAG, "Triggering overlay for: $currentApp")
            cooldownPrefs.edit().putLong(currentApp, now).apply()
            showOverlayScreen(currentApp)
        } else {
            val remaining = (APP_COOLDOWN_MS - (now - lastShown)) / 1000
            Log.d(TAG, "Skipping $currentApp — cooldown ${remaining}s remaining")
        }
    }

    private fun getActualForegroundApp(): String? {
        return try {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            val events = usm.queryEvents(now - 3000, now)
            val event = UsageEvents.Event()
            var lastPkg: String? = null
            var lastTime = 0L
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if ((event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                     event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) &&
                    event.timeStamp > lastTime) {
                    lastTime = event.timeStamp
                    lastPkg = event.packageName
                }
            }
            lastPkg
        } catch (e: Exception) {
            Log.e(TAG, "UsageEvents failed: ${e.message}")
            getForegroundAppFallback()
        }
    }

    private fun getForegroundAppFallback(): String? {
        return try {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 10_000, now)
                ?.filter { it.lastTimeUsed > 0 }
                ?.maxByOrNull { it.lastTimeUsed }?.packageName
        } catch (e: Exception) { null }
    }

    private fun isSystemPackage(pkg: String): Boolean {
        val launchers = listOf(
            "com.android.systemui", "com.android.launcher",
            "com.google.android.apps.nexuslauncher",
            "com.sec.android.app.launcher", "com.miui.home",
            "com.huawei.android.launcher", "com.oppo.launcher",
            "com.vivo.launcher", "com.oneplus.launcher"
        )
        return launchers.any { pkg.startsWith(it) } || pkg == packageName
    }

    private fun showOverlayScreen(pkg: String) {
        try {
            startActivity(Intent(this, MindGateOverlayActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                putExtra(MindGateOverlayActivity.EXTRA_TRIGGER_TYPE, TriggerType.APP_LAUNCH.name)
                putExtra(MindGateOverlayActivity.EXTRA_PACKAGE_NAME, pkg)
            })
        } catch (e: Exception) { Log.e(TAG, "Error showing overlay: ${e.message}") }
    }

    private fun scheduleRestart() {
        try {
            val pi = PendingIntent.getService(
                applicationContext, 100,
                Intent(applicationContext, AppMonitorService::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val triggerAt = SystemClock.elapsedRealtime() + 2000L
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
                PowerManager.PARTIAL_WAKE_LOCK, "MindGate:MonitorWakeLock"
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
        val ch = NotificationChannel(CHANNEL_ID, "FocusGuard Monitor",
            NotificationManager.IMPORTANCE_LOW).apply {
            description = "Monitors app usage"; setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun buildNotification(): Notification {
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FocusGuard Active")
            .setContentText("Monitoring your app usage")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pi).setOngoing(true).setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW).build()
    }

}
