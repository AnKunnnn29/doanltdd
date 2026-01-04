package com.example.doan.Activities

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
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
import com.example.doan.R
import com.google.android.material.button.MaterialButton
import kotlin.random.Random

class WelcomeActivity : AppCompatActivity() {

    private lateinit var logoCard: CardView
    private lateinit var logoOuterGlow: View
    private lateinit var logoRotatingRing: View
    private lateinit var logoShimmer: View
    private lateinit var tvAppName: TextView
    private lateinit var tvTagline: TextView
    private lateinit var buttonsContainer: View
    private lateinit var decorCircle1: View
    private lateinit var decorCircle2: View
    private lateinit var particlesContainer: FrameLayout
    
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_welcome)
            Log.d(TAG, "WelcomeActivity started")

            initViews()
            startAnimations()
            setupClickListeners()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun initViews() {
        logoCard = findViewById(R.id.logoCard)
        logoOuterGlow = findViewById(R.id.logoOuterGlow)
        logoRotatingRing = findViewById(R.id.logoRotatingRing)
        logoShimmer = findViewById(R.id.logoShimmer)
        tvAppName = findViewById(R.id.tvAppName)
        tvTagline = findViewById(R.id.tvTagline)
        buttonsContainer = findViewById(R.id.buttonsContainer)
        decorCircle1 = findViewById(R.id.decorCircle1)
        decorCircle2 = findViewById(R.id.decorCircle2)
        particlesContainer = findViewById(R.id.particlesContainer)
    }

    private fun startAnimations() {
        // Start particles
        startParticles()

        // Decor circles fade in
        handler.postDelayed({ animateDecorCircles() }, 100)

        // Logo entrance
        handler.postDelayed({ animateLogoEntrance() }, 200)

        // Glow effect
        handler.postDelayed({ animateGlow() }, 500)

        // Rotating ring
        handler.postDelayed({ animateRotatingRing() }, 600)

        // Shimmer effect
        handler.postDelayed({ startShimmerLoop() }, 800)

        // App name
        handler.postDelayed({ animateAppName() }, 700)

        // Tagline
        handler.postDelayed({ animateTagline() }, 900)

        // Buttons
        handler.postDelayed({ animateButtons() }, 1100)
    }

    private fun animateDecorCircles() {
        decorCircle1.animate()
            .alpha(0.15f)
            .setDuration(800)
            .start()

        decorCircle2.animate()
            .alpha(0.1f)
            .setDuration(800)
            .setStartDelay(200)
            .start()

        // Subtle floating animation
        val float1 = ObjectAnimator.ofFloat(decorCircle1, "translationY", 0f, -20f, 0f)
        float1.duration = 4000
        float1.repeatCount = ValueAnimator.INFINITE
        float1.interpolator = AccelerateDecelerateInterpolator()
        float1.start()

        val float2 = ObjectAnimator.ofFloat(decorCircle2, "translationY", 0f, 15f, 0f)
        float2.duration = 3500
        float2.repeatCount = ValueAnimator.INFINITE
        float2.interpolator = AccelerateDecelerateInterpolator()
        float2.start()
    }

    private fun animateLogoEntrance() {
        logoCard.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(600)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()
    }

    private fun animateGlow() {
        // Pulse glow
        val glowPulse = ObjectAnimator.ofFloat(logoOuterGlow, "alpha", 0f, 0.6f, 0.3f, 0.6f)
        glowPulse.duration = 2500
        glowPulse.repeatCount = ValueAnimator.INFINITE
        glowPulse.start()

        // Scale pulse
        val scaleX = ObjectAnimator.ofFloat(logoOuterGlow, "scaleX", 1f, 1.1f, 1f)
        val scaleY = ObjectAnimator.ofFloat(logoOuterGlow, "scaleY", 1f, 1.1f, 1f)
        scaleX.duration = 2500
        scaleY.duration = 2500
        scaleX.repeatCount = ValueAnimator.INFINITE
        scaleY.repeatCount = ValueAnimator.INFINITE
        scaleX.start()
        scaleY.start()
    }

    private fun animateRotatingRing() {
        logoRotatingRing.animate()
            .alpha(0.7f)
            .setDuration(400)
            .start()

        val rotation = ObjectAnimator.ofFloat(logoRotatingRing, "rotation", 0f, 360f)
        rotation.duration = 4000
        rotation.repeatCount = ValueAnimator.INFINITE
        rotation.interpolator = LinearInterpolator()
        rotation.start()
    }

    private fun startShimmerLoop() {
        animateShimmer()
    }

    private fun animateShimmer() {
        logoShimmer.alpha = 0.5f
        logoShimmer.translationX = -250f

        logoShimmer.animate()
            .translationX(450f)
            .setDuration(1200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                logoShimmer.alpha = 0f
                handler.postDelayed({ animateShimmer() }, 2500)
            }
            .start()
    }

    private fun animateAppName() {
        tvAppName.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun animateTagline() {
        tvTagline.animate()
            .alpha(0.9f)
            .translationY(0f)
            .setDuration(500)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun animateButtons() {
        buttonsContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun startParticles() {
        repeat(12) { i ->
            handler.postDelayed({
                if (!isFinishing) createParticle()
            }, (i * 300).toLong())
        }
    }

    private fun createParticle() {
        val particle = View(this).apply {
            val size = Random.nextInt(4, 10)
            layoutParams = FrameLayout.LayoutParams(size, size)
            setBackgroundResource(R.drawable.welcome_particle)
            alpha = Random.nextFloat() * 0.4f + 0.1f
            
            x = Random.nextFloat() * resources.displayMetrics.widthPixels
            y = resources.displayMetrics.heightPixels.toFloat()
        }

        particlesContainer.addView(particle)

        val duration = Random.nextLong(4000, 7000)

        particle.animate()
            .y(-50f)
            .alpha(0f)
            .setDuration(duration)
            .setInterpolator(LinearInterpolator())
            .withEndAction {
                particlesContainer.removeView(particle)
                if (!isFinishing) createParticle()
            }
            .start()
    }

    private fun setupClickListeners() {
        val loginButton = findViewById<MaterialButton>(R.id.btn_welcome_login)
        val registerButton = findViewById<MaterialButton>(R.id.btn_welcome_register)
        val guestMode = findViewById<TextView>(R.id.text_guest_mode)

        // Button press animation
        loginButton.setOnClickListener {
            animateButtonPress(it) {
                startActivity(Intent(this, LoginActivity::class.java))
                applyTransition()
            }
        }

        registerButton.setOnClickListener {
            animateButtonPress(it) {
                startActivity(Intent(this, RegisterActivity::class.java))
                applyTransition()
            }
        }

        guestMode.setOnClickListener {
            it.animate()
                .alpha(0.5f)
                .setDuration(100)
                .withEndAction {
                    startActivity(Intent(this, MainActivity::class.java))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.fade_in, R.anim.fade_out)
                    } else {
                        @Suppress("DEPRECATION")
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    }
                    finish()
                }
                .start()
        }
    }

    private fun animateButtonPress(view: View, onEnd: () -> Unit) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .withEndAction { onEnd() }
                    .start()
            }
            .start()
    }

    private fun applyTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.slide_in_right, R.anim.slide_out_left)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    companion object {
        private const val TAG = "WelcomeActivity"
    }
}
