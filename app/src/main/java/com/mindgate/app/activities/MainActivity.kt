package com.mindgate.app.activities

import android.app.AppOpsManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationManagerCompat
import com.mindgate.app.R
import com.mindgate.app.databinding.ActivityMainBinding
import com.mindgate.app.services.AppMonitorService
import com.mindgate.app.services.LockScreenService
import com.mindgate.app.utils.AppPreferences

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var ignoreToggleChange = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyTheme()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "FocusGuard"
        setupUI()
        if (AppPreferences.isFirstLaunch(this)) {
            AppPreferences.setFirstLaunch(this, false)
            startActivity(Intent(this, PermissionSetupActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUI()
    }

    private fun applyTheme() {
        when (AppPreferences.getThemeMode(this)) {
            AppCompatDelegate.MODE_NIGHT_YES ->
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            AppCompatDelegate.MODE_NIGHT_NO ->
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            else ->
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun setupUI() {
        binding.switchMainToggle.setOnCheckedChangeListener { _, isChecked ->
            if (ignoreToggleChange) return@setOnCheckedChangeListener
            AppPreferences.setServiceEnabled(this, isChecked)
            if (isChecked) startServices() else stopServices()
            updateServiceStatus(isChecked)
        }
        binding.switchLockScreen.setOnCheckedChangeListener { _, isChecked ->
            if (ignoreToggleChange) return@setOnCheckedChangeListener
            AppPreferences.setLockScreenEnabled(this, isChecked)
            if (AppPreferences.isServiceEnabled(this)) {
                if (isChecked) LockScreenService.start(this) else LockScreenService.stop(this)
            }
        }
        binding.switchAppMonitor.setOnCheckedChangeListener { _, isChecked ->
            if (ignoreToggleChange) return@setOnCheckedChangeListener
            AppPreferences.setAppMonitorEnabled(this, isChecked)
            if (AppPreferences.isServiceEnabled(this)) {
                if (isChecked) AppMonitorService.start(this) else AppMonitorService.stop(this)
            }
        }
        binding.cardCustomize.setOnClickListener { startActivity(Intent(this, CustomizeScreenActivity::class.java)) }
        binding.cardAppSelection.setOnClickListener { startActivity(Intent(this, AppSelectionActivity::class.java)) }
        binding.cardPermissions.setOnClickListener { startActivity(Intent(this, PermissionSetupActivity::class.java)) }

        // Redirect app picker — opens the same app list but for single selection
        binding.cardRedirectApp.setOnClickListener {
            startActivity(Intent(this, RedirectAppPickerActivity::class.java))
        }

        binding.btnPreview.setOnClickListener {
            startActivity(Intent(this, MindGateOverlayActivity::class.java).apply {
                putExtra(MindGateOverlayActivity.EXTRA_TRIGGER_TYPE, "APP_LAUNCH")
            })
        }
        binding.btnThemeLight.setOnClickListener {
            AppPreferences.setThemeMode(this, AppCompatDelegate.MODE_NIGHT_NO)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        binding.btnThemeDark.setOnClickListener {
            AppPreferences.setThemeMode(this, AppCompatDelegate.MODE_NIGHT_YES)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
        binding.btnThemeSystem.setOnClickListener {
            AppPreferences.setThemeMode(this, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun refreshUI() {
        ignoreToggleChange = true
        binding.switchMainToggle.isChecked = AppPreferences.isServiceEnabled(this)
        binding.switchLockScreen.isChecked = AppPreferences.isLockScreenEnabled(this)
        binding.switchAppMonitor.isChecked = AppPreferences.isAppMonitorEnabled(this)
        ignoreToggleChange = false

        updateServiceStatus(AppPreferences.isServiceEnabled(this))

        val selectedCount = AppPreferences.getSelectedApps(this).size
        binding.tvSelectedAppsCount.text =
            if (selectedCount == 0) "No apps selected — tap to choose"
            else "$selectedCount app${if (selectedCount == 1) "" else "s"} monitored"

        val content = AppPreferences.getScreenContent(this)
        val preview = content.quote.take(60) + if (content.quote.length > 60) "..." else ""
        binding.tvScreenPreview.text = "\"$preview\""
        binding.tvDurationPreview.text = "${content.durationSeconds}s display duration"

        // Redirect app label
        val redirectPkg = AppPreferences.getRedirectApp(this)
        binding.tvRedirectAppName.text = if (redirectPkg.isNullOrEmpty()) {
            "Not set — tap to choose"
        } else {
            try {
                val info = packageManager.getApplicationInfo(redirectPkg, 0)
                val name = packageManager.getApplicationLabel(info).toString()
                val icon = packageManager.getApplicationIcon(redirectPkg)
                binding.ivRedirectIcon.setImageDrawable(icon)
                "→ $name"
            } catch (_: Exception) { "Not set — tap to choose" }
        }

        val hasUsageAccess = hasUsageStatsPermission()
        val hasOverlay = Settings.canDrawOverlays(this)
        binding.tvPermissionsStatus.text = when {
            hasUsageAccess && hasOverlay -> "✓ All permissions granted"
            !hasUsageAccess && !hasOverlay -> "⚠ Usage access & overlay needed"
            !hasUsageAccess -> "⚠ Usage access needed"
            else -> "⚠ Overlay permission needed"
        }
        binding.tvPermissionsStatus.setTextColor(
            getColor(if (hasUsageAccess && hasOverlay) R.color.color_success else R.color.color_warning)
        )
    }

    private fun updateServiceStatus(enabled: Boolean) {
        binding.tvServiceStatus.text = if (enabled) "🟢 Active" else "⚪ Inactive"
        binding.tvServiceStatus.setTextColor(
            getColor(if (enabled) R.color.color_success else R.color.color_on_surface_variant)
        )
        binding.cardControls.alpha = if (enabled) 1f else 0.6f
    }

    private fun startServices() {
        if (!hasUsageStatsPermission() || !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please grant required permissions first", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, PermissionSetupActivity::class.java))
            ignoreToggleChange = true
            binding.switchMainToggle.isChecked = false
            ignoreToggleChange = false
            AppPreferences.setServiceEnabled(this, false)
            return
        }
        if (AppPreferences.isLockScreenEnabled(this)) LockScreenService.start(this)
        if (AppPreferences.isAppMonitorEnabled(this)) AppMonitorService.start(this)
        Toast.makeText(this, "FocusGuard is now active", Toast.LENGTH_SHORT).show()
    }

    private fun stopServices() {
        LockScreenService.stop(this)
        AppMonitorService.stop(this)
        Toast.makeText(this, "FocusGuard paused", Toast.LENGTH_SHORT).show()
    }

    private fun hasUsageStatsPermission(): Boolean {
        return try {
            val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName
            ) == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) { false }
    }
}
