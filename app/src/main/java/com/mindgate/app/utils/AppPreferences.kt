package com.mindgate.app.utils

import android.content.Context
import android.content.SharedPreferences
import com.mindgate.app.models.ScreenContent

object AppPreferences {
    private const val PREF_NAME = "mindgate_prefs"
    private const val KEY_ENABLED = "service_enabled"
    private const val KEY_LOCK_SCREEN_ENABLED = "lock_screen_enabled"
    private const val KEY_APP_MONITOR_ENABLED = "app_monitor_enabled"
    private const val KEY_SCREEN_DURATION = "screen_duration"
    private const val KEY_SCREEN_TITLE = "screen_title"
    private const val KEY_SCREEN_QUOTE = "screen_quote"
    private const val KEY_SCREEN_IMAGE_URI = "screen_image_uri"
    private const val KEY_SELECTED_APPS = "selected_apps"
    private const val KEY_REDIRECT_APP = "redirect_app_package"   // NEW
    private const val KEY_FIRST_LAUNCH = "first_launch"
    private const val KEY_PERMISSIONS_GRANTED = "permissions_granted"
    private const val KEY_THEME_MODE = "theme_mode"
    // Persist last unlock time so it survives service restart across idle/Doze
    private const val KEY_LAST_UNLOCK_TIME = "last_unlock_time"
    // Persist last foreground app so service restart doesn't re-trigger immediately
    private const val KEY_LAST_FOREGROUND_APP = "last_foreground_app"

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun isServiceEnabled(context: Context) = getPrefs(context).getBoolean(KEY_ENABLED, false)
    fun setServiceEnabled(context: Context, v: Boolean) = getPrefs(context).edit().putBoolean(KEY_ENABLED, v).apply()

    fun isLockScreenEnabled(context: Context) = getPrefs(context).getBoolean(KEY_LOCK_SCREEN_ENABLED, true)
    fun setLockScreenEnabled(context: Context, v: Boolean) = getPrefs(context).edit().putBoolean(KEY_LOCK_SCREEN_ENABLED, v).apply()

    fun isAppMonitorEnabled(context: Context) = getPrefs(context).getBoolean(KEY_APP_MONITOR_ENABLED, true)
    fun setAppMonitorEnabled(context: Context, v: Boolean) = getPrefs(context).edit().putBoolean(KEY_APP_MONITOR_ENABLED, v).apply()

    fun getScreenDuration(context: Context) = getPrefs(context).getInt(KEY_SCREEN_DURATION, 10)
    fun setScreenDuration(context: Context, v: Int) = getPrefs(context).edit().putInt(KEY_SCREEN_DURATION, v).apply()

    fun getScreenTitle(context: Context) = getPrefs(context).getString(KEY_SCREEN_TITLE, "Pause & Reflect") ?: "Pause & Reflect"
    fun setScreenTitle(context: Context, v: String) = getPrefs(context).edit().putString(KEY_SCREEN_TITLE, v).apply()

    fun getScreenQuote(context: Context) = getPrefs(context).getString(KEY_SCREEN_QUOTE,
        "Take a breath. Do you really need to check your phone right now?")
        ?: "Take a breath. Do you really need to check your phone right now?"
    fun setScreenQuote(context: Context, v: String) = getPrefs(context).edit().putString(KEY_SCREEN_QUOTE, v).apply()

    fun getScreenImageUri(context: Context): String? = getPrefs(context).getString(KEY_SCREEN_IMAGE_URI, null)
    fun setScreenImageUri(context: Context, v: String?) = getPrefs(context).edit().putString(KEY_SCREEN_IMAGE_URI, v).apply()

    fun getSelectedApps(context: Context): Set<String> = getPrefs(context).getStringSet(KEY_SELECTED_APPS, emptySet()) ?: emptySet()
    fun setSelectedApps(context: Context, v: Set<String>) = getPrefs(context).edit().putStringSet(KEY_SELECTED_APPS, v).apply()

    // Redirect app — the "healthy alternative" app shown as button on overlay
    fun getRedirectApp(context: Context): String? = getPrefs(context).getString(KEY_REDIRECT_APP, null)
    fun setRedirectApp(context: Context, pkg: String?) = getPrefs(context).edit().putString(KEY_REDIRECT_APP, pkg).apply()

    fun isFirstLaunch(context: Context) = getPrefs(context).getBoolean(KEY_FIRST_LAUNCH, true)
    fun setFirstLaunch(context: Context, v: Boolean) = getPrefs(context).edit().putBoolean(KEY_FIRST_LAUNCH, v).apply()

    fun arePermissionsGranted(context: Context) = getPrefs(context).getBoolean(KEY_PERMISSIONS_GRANTED, false)
    fun setPermissionsGranted(context: Context, v: Boolean) = getPrefs(context).edit().putBoolean(KEY_PERMISSIONS_GRANTED, v).apply()

    fun getThemeMode(context: Context) = getPrefs(context).getInt(KEY_THEME_MODE, -1)
    fun setThemeMode(context: Context, v: Int) = getPrefs(context).edit().putInt(KEY_THEME_MODE, v).apply()

    // Persisted across service restarts — survive Doze/idle kills
    fun getLastUnlockTime(context: Context) = getPrefs(context).getLong(KEY_LAST_UNLOCK_TIME, 0L)
    fun setLastUnlockTime(context: Context, v: Long) = getPrefs(context).edit().putLong(KEY_LAST_UNLOCK_TIME, v).apply()

    fun getLastForegroundApp(context: Context) = getPrefs(context).getString(KEY_LAST_FOREGROUND_APP, "") ?: ""
    fun setLastForegroundApp(context: Context, v: String) = getPrefs(context).edit().putString(KEY_LAST_FOREGROUND_APP, v).apply()

    fun getScreenContent(context: Context) = ScreenContent(
        title = getScreenTitle(context),
        quote = getScreenQuote(context),
        imageUri = getScreenImageUri(context),
        durationSeconds = getScreenDuration(context)
    )
}
