package com.example.doan.Activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import com.example.doan.R
import com.example.doan.Utils.SessionManager
import kotlin.random.Random

class SplashActivity : AppCompatActivity() {

    private lateinit var logoCard: CardView
    private lateinit var rotatingRing: View
    private lateinit var innerGlow: View
    private lateinit var outerGlow: View
    private lateinit var shimmerOverlay: View
    private lateinit var tvAppName: TextView
    private lateinit var tvTagline: TextView
    private lateinit var tvCopyright: TextView
    private lateinit var loadingDots: View
    private lateinit var dot1: View
    private lateinit var dot2: View
    private lateinit var dot3: View
    private lateinit var ripple1: View
    private lateinit var ripple2: View
    private lateinit var ripple3: View
    private lateinit var particlesContainer: FrameLayout

    private val handler = Handler(Looper.getMainLooper())
    private val appName = "Houjicha"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_splash)
            Log.d(TAG, "SplashActivity started")

            initViews()
            startAnimationSequence()

            handler.postDelayed({
                navigateToNextScreen()
            }, SPLASH_DELAY)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}")
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }
    }

    private fun initViews() {
        logoCard = findViewById(R.id.logoCard)
        rotatingRing = findViewById(R.id.rotatingRing)
        innerGlow = findViewById(R.id.innerGlow)
        outerGlow = findViewById(R.id.outerGlow)
        shimmerOverlay = findViewById(R.id.shimmerOverlay)
        tvAppName = findViewById(R.id.tvAppName)
        tvTagline = findViewById(R.id.tvTagline)
        tvCopyright = findViewById(R.id.tvCopyright)
        loadingDots = findViewById(R.id.loadingDots)
        dot1 = findViewById(R.id.dot1)
        dot2 = findViewById(R.id.dot2)
        dot3 = findViewById(R.id.dot3)
        ripple1 = findViewById(R.id.ripple1)
        ripple2 = findViewById(R.id.ripple2)
        ripple3 = findViewById(R.id.ripple3)
        particlesContainer = findViewById(R.id.particlesContainer)
    }

    private fun startAnimationSequence() {
        // 1. Start particles
        startParticles()

        // 2. Logo entrance (0ms)
        handler.postDelayed({ animateLogoEntrance() }, 200)

        // 3. Glow effects (400ms)
        handler.postDelayed({ animateGlowEffects() }, 400)

        // 4. Rotating ring (600ms)
        handler.postDelayed({ animateRotatingRing() }, 600)

        // 5. Ripple effects (800ms)
        handler.postDelayed({ startRippleAnimation() }, 800)

        // 6. Shimmer effect (1000ms)
        handler.postDelayed({ animateShimmer() }, 1000)

        // 7. Typing effect (1200ms)
        handler.postDelayed({ startTypingAnimation() }, 1200)

        // 8. Tagline (2000ms)
        handler.postDelayed({ animateTagline() }, 2000)

        // 9. Loading dots (2200ms)
        handler.postDelayed({ animateLoadingDots() }, 2200)

        // 10. Copyright (2400ms)
        handler.postDelayed({ animateCopyright() }, 2400)
    }

    private fun animateLogoEntrance() {
        logoCard.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(600)
            .setInterpolator(OvershootInterpolator(1.5f))
            .start()
    }

    private fun animateGlowEffects() {
        // Inner glow pulse
        innerGlow.alpha = 0f
        val innerPulse = ObjectAnimator.ofFloat(innerGlow, "alpha", 0f, 0.8f, 0.4f, 0.8f)
        innerPulse.duration = 2000
        innerPulse.repeatCount = ValueAnimator.INFINITE
        innerPulse.start()

        // Outer glow pulse (offset)
        outerGlow.alpha = 0f
        handler.postDelayed({
            val outerPulse = ObjectAnimator.ofFloat(outerGlow, "alpha", 0f, 0.6f, 0.3f, 0.6f)
            outerPulse.duration = 2000
            outerPulse.repeatCount = ValueAnimator.INFINITE
            outerPulse.start()
        }, 500)
    }

    private fun animateRotatingRing() {
        rotatingRing.alpha = 0f
        rotatingRing.animate()
            .alpha(0.8f)
            .setDuration(400)
            .start()

        val rotation = ObjectAnimator.ofFloat(rotatingRing, "rotation", 0f, 360f)
        rotation.duration = 3000
        rotation.repeatCount = ValueAnimator.INFINITE
        rotation.interpolator = LinearInterpolator()
        rotation.start()
    }

    private fun startRippleAnimation() {
        animateRipple(ripple1, 0)
        animateRipple(ripple2, 400)
        animateRipple(ripple3, 800)
    }

    private fun animateRipple(ripple: View, delay: Long) {
        handler.postDelayed({
            ripple.scaleX = 0.5f
            ripple.scaleY = 0.5f
            ripple.alpha = 0.8f

            val scaleX = ObjectAnimator.ofFloat(ripple, "scaleX", 0.5f, 2f)
            val scaleY = ObjectAnimator.ofFloat(ripple, "scaleY", 0.5f, 2f)
            val alpha = ObjectAnimator.ofFloat(ripple, "alpha", 0.8f, 0f)

            val set = AnimatorSet()
            set.playTogether(scaleX, scaleY, alpha)
            set.duration = 1500
            set.interpolator = DecelerateInterpolator()
            set.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    animateRipple(ripple, 0)
                }
            })
            set.start()
        }, delay)
    }

    private fun animateShimmer() {
        shimmerOverlay.alpha = 0.6f
        shimmerOverlay.translationX = -200f

        val shimmer = ObjectAnimator.ofFloat(shimmerOverlay, "translationX", -200f, 400f)
        shimmer.duration = 1000
        shimmer.interpolator = AccelerateDecelerateInterpolator()
        shimmer.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                shimmerOverlay.alpha = 0f
                // Repeat shimmer
                handler.postDelayed({ animateShimmer() }, 2000)
            }
        })
        shimmer.start()
    }

    private fun startTypingAnimation() {
        var index = 0
        val typingRunnable = object : Runnable {
            override fun run() {
                if (index <= appName.length) {
                    tvAppName.text = appName.substring(0, index)
                    index++
                    handler.postDelayed(this, 80)
                }
            }
        }
        handler.post(typingRunnable)
    }

    private fun animateTagline() {
        tvTagline.animate()
            .alpha(0.9f)
            .translationY(0f)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun animateLoadingDots() {
        loadingDots.animate()
            .alpha(1f)
            .setDuration(300)
            .start()

        // Bouncing dots animation
        animateDot(dot1, 0)
        animateDot(dot2, 150)
        animateDot(dot3, 300)
    }

    private fun animateDot(dot: View, delay: Long) {
        handler.postDelayed({
            val bounce = ObjectAnimator.ofFloat(dot, "translationY", 0f, -15f, 0f)
            bounce.duration = 600
            bounce.repeatCount = ValueAnimator.INFINITE
            bounce.interpolator = AccelerateDecelerateInterpolator()
            bounce.start()
        }, delay)
    }

    private fun animateCopyright() {
        tvCopyright.animate()
            .alpha(0.7f)
            .setDuration(400)
            .start()
    }

    private fun startParticles() {
        repeat(15) { i ->
            handler.postDelayed({
                createParticle()
            }, (i * 200).toLong())
        }
    }

    private fun createParticle() {
        val particle = View(this).apply {
            val size = Random.nextInt(4, 12)
            layoutParams = FrameLayout.LayoutParams(size, size)
            setBackgroundResource(R.drawable.splash_dot)
            alpha = Random.nextFloat() * 0.5f + 0.2f
            
            x = Random.nextFloat() * resources.displayMetrics.widthPixels
            y = resources.displayMetrics.heightPixels.toFloat()
        }

        particlesContainer.addView(particle)

        val duration = Random.nextLong(3000, 6000)
        val targetY = -100f

        particle.animate()
            .y(targetY)
            .alpha(0f)
            .setDuration(duration)
            .setInterpolator(LinearInterpolator())
            .withEndAction {
                particlesContainer.removeView(particle)
                if (!isFinishing) createParticle()
            }
            .start()
    }

    private fun navigateToNextScreen() {
        try {
            val sessionManager = SessionManager(this)
            val isLoggedIn = sessionManager.isLoggedIn()
            val isManager = sessionManager.isManager()

            Log.d(TAG, "User logged in: $isLoggedIn, Is Manager: $isManager")

            val intent = when {
                isLoggedIn && isManager -> Intent(this, ManagerActivity::class.java)
                isLoggedIn -> Intent(this, MainActivity::class.java)
                else -> Intent(this, WelcomeActivity::class.java)
            }

            startActivity(intent)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.fade_in, R.anim.fade_out)
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }
            finish()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating: ${e.message}")
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    companion object {
        private const val TAG = "SplashActivity"
        private const val SPLASH_DELAY = 3500L
    }
}
