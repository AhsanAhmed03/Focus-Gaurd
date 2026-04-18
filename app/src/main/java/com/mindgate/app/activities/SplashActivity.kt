package com.mindgate.app.activities

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.mindgate.app.databinding.ActivitySplashBinding
import com.mindgate.app.utils.AppPreferences

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val SPLASH_DURATION = 5000L   // 5 seconds total

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply theme before setContentView
        when (AppPreferences.getThemeMode(this)) {
            AppCompatDelegate.MODE_NIGHT_YES ->
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            AppCompatDelegate.MODE_NIGHT_NO ->
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            else ->
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Full screen immersive
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        startSplashAnimations()
    }

    private fun startSplashAnimations() {
        // Phase 1 (0–600ms): Fade + scale in the logo + title
        binding.splashContent.apply {
            alpha = 0f
            scaleX = 0.7f
            scaleY = 0.7f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(600)
                .setInterpolator(OvershootInterpolator(1.2f))
                .start()
        }

        // Phase 2 (700ms): Fade in hint text and progress bar
        Handler(Looper.getMainLooper()).postDelayed({
            binding.tvSplashHint.animate().alpha(1f).setDuration(400).start()
            binding.splashProgress.animate().alpha(1f).setDuration(400).start()
        }, 700)

        // Phase 3 (800ms – SPLASH_DURATION): Animate progress bar from 0 to 100
        Handler(Looper.getMainLooper()).postDelayed({
            ObjectAnimator.ofInt(binding.splashProgress, "progress", 0, 100).apply {
                duration = SPLASH_DURATION - 900L   // leave 100ms buffer
                interpolator = DecelerateInterpolator(0.8f)
                start()
            }
        }, 800)

        // Cycle through motivational hints
        val hints = listOf(
            "Preparing your mindful experience...",
            "Building healthy habits takes time ✨",
            "You're taking control of your screen time 💪",
            "Every pause is a step toward balance 🧘",
            "Ready to be more intentional today?"
        )
        var hintIndex = 0
        val hintHandler = Handler(Looper.getMainLooper())
        val hintRunnable = object : Runnable {
            override fun run() {
                if (hintIndex < hints.size - 1) {
                    hintIndex++
                    binding.tvSplashHint.animate()
                        .alpha(0f).setDuration(300)
                        .withEndAction {
                            binding.tvSplashHint.text = hints[hintIndex]
                            binding.tvSplashHint.animate().alpha(1f).setDuration(300).start()
                        }.start()
                    hintHandler.postDelayed(this, 900)
                }
            }
        }
        hintHandler.postDelayed(hintRunnable, 1000)

        // Navigate to MainActivity after splash duration
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToMain()
        }, SPLASH_DURATION)
    }

    private fun navigateToMain() {
        // Fade out the whole splash
        binding.root.animate()
            .alpha(0f)
            .setDuration(400)
            .withEndAction {
                startActivity(Intent(this, MainActivity::class.java))
                // No slide animation — crossfade feel
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }.start()
    }

    // Prevent back press from exiting the splash — user must wait
    @Deprecated("Use OnBackPressedDispatcher")
    override fun onBackPressed() {
        // Do nothing during splash
    }
}
