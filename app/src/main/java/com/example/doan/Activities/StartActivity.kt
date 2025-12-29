package com.example.doan.Activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.doan.R
import com.google.android.material.button.MaterialButton

class StartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.acitivity_start)

        // Find views
        val logoContainer = findViewById<FrameLayout>(R.id.logoContainer)
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val tvSubtitle = findViewById<TextView>(R.id.tvSubtitle)
        val tvTagline = findViewById<TextView>(R.id.tvTagline)
        val btnStart = findViewById<MaterialButton>(R.id.btn_start)
        val circle1 = findViewById<View>(R.id.circle1)
        val circle2 = findViewById<View>(R.id.circle2)

        // Load animations
        val logoScaleIn = AnimationUtils.loadAnimation(this, R.anim.logo_scale_in)
        val slideUpFadeIn = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in)
        val pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_animation)

        // Initially hide views
        logoContainer.alpha = 0f
        tvWelcome.alpha = 0f
        tvSubtitle.alpha = 0f
        tvTagline.alpha = 0f
        btnStart.alpha = 0f

        // Start animations with delays
        logoContainer.postDelayed({
            logoContainer.alpha = 1f
            logoContainer.startAnimation(logoScaleIn)
        }, 200)

        tvWelcome.postDelayed({
            tvWelcome.alpha = 1f
            tvWelcome.startAnimation(slideUpFadeIn)
        }, 600)

        tvSubtitle.postDelayed({
            tvSubtitle.alpha = 1f
            tvSubtitle.startAnimation(slideUpFadeIn)
        }, 800)

        tvTagline.postDelayed({
            tvTagline.alpha = 1f
            tvTagline.startAnimation(slideUpFadeIn)
        }, 1000)

        btnStart.postDelayed({
            btnStart.alpha = 1f
            btnStart.startAnimation(slideUpFadeIn)
        }, 1200)

        // Pulse animation for decorative circles
        circle1.startAnimation(pulseAnimation)
        circle2.postDelayed({
            circle2.startAnimation(pulseAnimation)
        }, 500)

        btnStart.setOnClickListener {
            // Button press animation
            btnStart.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    btnStart.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .withEndAction {
                            // Navigate to SplashActivity
                            startActivity(Intent(this, SplashActivity::class.java))
                            // Smooth transition
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
                .start()
        }
    }
}
