package com.mindgate.app.activities

import android.app.AppOpsManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.mindgate.app.R
import com.mindgate.app.databinding.ActivityPermissionSetupBinding

class PermissionSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionSetupBinding

    // Use ActivityResultLauncher for settings navigation (catches result on return)
    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { updatePermissionStatus() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Setup Permissions"
        setupUI()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun setupUI() {
        binding.btnGrantUsage.setOnClickListener {
            settingsLauncher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        binding.btnGrantOverlay.setOnClickListener {
            settingsLauncher.launch(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName"))
            )
        }
        binding.btnGrantBattery.setOnClickListener {
            try {
                settingsLauncher.launch(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:$packageName"))
                )
            } catch (_: Exception) {
                settingsLauncher.launch(
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                )
            }
        }
        binding.btnGrantNotification.setOnClickListener {
            settingsLauncher.launch(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                }
            )
        }
        binding.btnDone.setOnClickListener { finish() }
    }

    private fun updatePermissionStatus() {
        val hasUsage = hasUsageStatsPermission()
        val hasOverlay = Settings.canDrawOverlays(this)
        val hasBattery = isIgnoringBatteryOptimizations()
        val hasNotification = NotificationManagerCompat.from(this).areNotificationsEnabled()

        updateRow(hasUsage, binding.ivUsageStatus, binding.tvUsageStatus, binding.btnGrantUsage,
            "Granted — App monitoring active",
            "Required to detect which app is in foreground")

        updateRow(hasOverlay, binding.ivOverlayStatus, binding.tvOverlayStatus, binding.btnGrantOverlay,
            "Granted — Overlay screens enabled",
            "Required to show mindfulness screen over apps")

        updateRow(hasBattery, binding.ivBatteryStatus, binding.tvBatteryStatus, binding.btnGrantBattery,
            "Granted — Services run reliably in background",
            "Recommended to keep services alive on all OEMs")

        updateRow(hasNotification, binding.ivNotificationStatus, binding.tvNotificationStatus,
            binding.btnGrantNotification,
            "Granted — Service status notifications shown",
            "Needed to show the persistent service notification")

        val allRequired = hasUsage && hasOverlay
        binding.tvAllGranted.visibility = if (allRequired) View.VISIBLE else View.GONE
        binding.btnDone.text = if (allRequired) "All Set! Continue →" else "Continue Anyway"
    }

    private fun updateRow(
        granted: Boolean,
        icon: ImageView, status: TextView, button: Button,
        grantedText: String, notGrantedText: String
    ) {
        if (granted) {
            icon.setImageResource(R.drawable.ic_check_circle)
            icon.setColorFilter(getColor(R.color.color_success))
            status.text = grantedText
            status.setTextColor(getColor(R.color.color_success))
            button.isEnabled = false
            button.text = "Granted ✓"
        } else {
            icon.setImageResource(R.drawable.ic_warning)
            icon.setColorFilter(getColor(R.color.color_warning))
            status.text = notGrantedText
            status.setTextColor(getColor(R.color.color_on_surface_variant))
            button.isEnabled = true
            button.text = "Grant Permission"
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        return try {
            val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName
            ) == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) { false }
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
