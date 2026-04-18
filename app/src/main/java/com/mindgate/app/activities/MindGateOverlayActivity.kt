package com.mindgate.app.activities

import android.animation.ObjectAnimator
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.mindgate.app.databinding.ActivityOverlayBinding
import com.mindgate.app.models.TriggerType
import com.mindgate.app.utils.AppPreferences

class MindGateOverlayActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TRIGGER_TYPE = "trigger_type"
        const val EXTRA_PACKAGE_NAME = "package_name"
        private const val TAG = "MindGateOverlay"
    }

    private lateinit var binding: ActivityOverlayBinding
    private var countdownHandler: Handler? = null
    private var progressAnimator: ObjectAnimator? = null
    private var remainingSeconds = 10
    private var triggerType = TriggerType.APP_LAUNCH
    private var isFromLockScreen = false
    private var targetPackage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding = ActivityOverlayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupFullScreen()
        readIntent(intent)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* blocked */ }
        })

        setupUI()
        startCountdown()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        progressAnimator?.cancel()
        countdownHandler?.removeCallbacksAndMessages(null)
        readIntent(intent)
        setupUI()
        startCountdown()
    }

    private fun readIntent(intent: Intent) {
        triggerType = TriggerType.valueOf(
            intent.getStringExtra(EXTRA_TRIGGER_TYPE) ?: TriggerType.APP_LAUNCH.name
        )
        targetPackage = intent.getStringExtra(EXTRA_PACKAGE_NAME)
        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        isFromLockScreen = triggerType == TriggerType.LOCK_SCREEN || km.isKeyguardLocked
    }

    private fun setupFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun setupUI() {
        val content = AppPreferences.getScreenContent(this)
        remainingSeconds = content.durationSeconds

        binding.tvTitle.text = content.title
        binding.tvQuote.text = content.quote
        updateTimerLabel(remainingSeconds)

        // Background image
        if (!content.imageUri.isNullOrEmpty()) {
            binding.ivBackground.visibility = View.VISIBLE
            try {
                Glide.with(this).load(Uri.parse(content.imageUri)).centerCrop()
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(e: GlideException?, model: Any?,
                            target: Target<Drawable>, isFirstResource: Boolean): Boolean {
                            binding.ivBackground.visibility = View.GONE; return false
                        }
                        override fun onResourceReady(resource: Drawable, model: Any,
                            target: Target<Drawable>?, dataSource: DataSource,
                            isFirstResource: Boolean): Boolean = false
                    }).into(binding.ivBackground)
            } catch (_: Exception) { binding.ivBackground.visibility = View.GONE }
        } else {
            binding.ivBackground.visibility = View.GONE
        }

        // Emergency button — lock screen only
        binding.btnEmergency.visibility = if (isFromLockScreen) View.VISIBLE else View.GONE
        binding.btnEmergency.setOnClickListener { vibrate(); dismissOverlay() }

        // Trigger badge
        binding.tvTriggerBadge.text = when (triggerType) {
            TriggerType.LOCK_SCREEN -> "📱 Phone Unlocked"
            TriggerType.APP_LAUNCH -> "🚀 Opening ${targetPackage?.let { getAppName(it) } ?: "App"}"
        }

        // Redirect button — shown only if user has picked a redirect/alternative app
        setupRedirectButton()

        // Progress bar
        binding.progressBar.max = content.durationSeconds * 100
        binding.progressBar.progress = content.durationSeconds * 100

        animateIn()
    }

    private fun setupRedirectButton() {
        val redirectPkg = AppPreferences.getRedirectApp(this)
        if (redirectPkg.isNullOrEmpty()) {
            binding.btnRedirect.visibility = View.GONE
            return
        }

        val redirectName = getAppName(redirectPkg)
        val redirectIcon = try { packageManager.getApplicationIcon(redirectPkg) } catch (_: Exception) { null }

        binding.btnRedirect.visibility = View.VISIBLE
        binding.btnRedirect.text = "Open $redirectName instead"
        if (redirectIcon != null) binding.btnRedirect.icon = redirectIcon

        binding.btnRedirect.setOnClickListener {
            vibrate()
            // Launch the redirect app
            val launchIntent = packageManager.getLaunchIntentForPackage(redirectPkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
            }
            dismissOverlay()
        }
    }

    private fun animateIn() {
        binding.cardContent.alpha = 0f
        binding.cardContent.translationY = 80f
        binding.tvTitle.alpha = 0f
        binding.tvQuote.alpha = 0f
        binding.cardContent.animate().alpha(1f).translationY(0f).setDuration(500)
            .setInterpolator(DecelerateInterpolator()).start()
        binding.tvTitle.animate().alpha(1f).setDuration(600).setStartDelay(200).start()
        binding.tvQuote.animate().alpha(1f).setDuration(600).setStartDelay(400).start()
    }

    private fun startCountdown() {
        progressAnimator = ObjectAnimator.ofInt(
            binding.progressBar, "progress", remainingSeconds * 100, 0
        ).apply {
            duration = remainingSeconds * 1000L
            interpolator = LinearInterpolator()
            start()
        }
        var secondsLeft = remainingSeconds
        val runnable = object : Runnable {
            override fun run() {
                secondsLeft--
                if (secondsLeft <= 0) closeScreen()
                else { updateTimerLabel(secondsLeft); countdownHandler?.postDelayed(this, 1000) }
            }
        }
        countdownHandler = Handler(Looper.getMainLooper())
        countdownHandler?.postDelayed(runnable, 1000)
    }

    private fun updateTimerLabel(seconds: Int) { binding.tvTimer.text = seconds.toString() }

    private fun closeScreen() {
        progressAnimator?.cancel()
        countdownHandler?.removeCallbacksAndMessages(null)
        binding.cardContent.animate()
            .alpha(0f).translationY(-40f).setDuration(300)
            .withEndAction { dismissOverlay() }.start()
    }

    private fun dismissOverlay() {
        if (!isFinishing) moveTaskToBack(true)
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION") getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun getAppName(pkg: String): String {
        return try { packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString() }
        catch (_: Exception) { pkg }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) setupFullScreen()
    }

    override fun onDestroy() {
        super.onDestroy()
        progressAnimator?.cancel()
        countdownHandler?.removeCallbacksAndMessages(null)
    }
}
