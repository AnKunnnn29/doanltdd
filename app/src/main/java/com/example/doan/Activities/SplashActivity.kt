package com.example.doan.Activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.doan.R
import com.example.doan.Utils.SessionManager

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_splash)
            Log.d(TAG, "SplashActivity started")

            // Start animations
            startAnimations()

            // Delay and navigate
            Handler(Looper.getMainLooper()).postDelayed({
                navigateToNextScreen()
            }, SPLASH_DELAY)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}")
            e.printStackTrace()
            // Fallback: go directly to WelcomeActivity
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }
    }

    private fun startAnimations() {
        try {
            // Find views
            val logoContainer = findViewById<FrameLayout>(R.id.logoContainer)
            val tvAppName = findViewById<TextView>(R.id.tvAppName)
            val tvTagline = findViewById<TextView>(R.id.tvTagline)
            val loadingCircle = findViewById<ImageView>(R.id.loadingCircle)
            val tvLoading = findViewById<TextView>(R.id.tvLoading)
            val tvCopyright = findViewById<TextView>(R.id.tvCopyright)
            val circle1 = findViewById<View>(R.id.circle1)
            val circle2 = findViewById<View>(R.id.circle2)
            val glowEffect = findViewById<View>(R.id.glowEffect)

            // Load animations
            val logoScaleIn = AnimationUtils.loadAnimation(this, R.anim.logo_scale_in)
            val slideUpFadeIn = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in)
            val rotateLoading = AnimationUtils.loadAnimation(this, R.anim.rotate_loading)
            val pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_animation)

            // Initially hide views
            logoContainer.alpha = 0f
            tvAppName.alpha = 0f
            tvTagline.alpha = 0f
            tvLoading.alpha = 0f
            tvCopyright.alpha = 0f

            // Start logo animation
            logoContainer.postDelayed({
                logoContainer.alpha = 1f
                logoContainer.startAnimation(logoScaleIn)
            }, 100)

            // Glow pulse effect
            glowEffect.startAnimation(pulseAnimation)

            // App name animation
            tvAppName.postDelayed({
                tvAppName.alpha = 1f
                tvAppName.startAnimation(slideUpFadeIn)
            }, 400)

            // Tagline animation
            tvTagline.postDelayed({
                tvTagline.alpha = 1f
                tvTagline.startAnimation(slideUpFadeIn)
            }, 600)

            // Loading animation
            loadingCircle.startAnimation(rotateLoading)
            tvLoading.postDelayed({
                tvLoading.alpha = 1f
                tvLoading.startAnimation(slideUpFadeIn)
            }, 800)

            // Copyright animation
            tvCopyright.postDelayed({
                tvCopyright.alpha = 1f
                tvCopyright.startAnimation(slideUpFadeIn)
            }, 1000)

            // Decorative circles pulse
            circle1.startAnimation(pulseAnimation)
            circle2.postDelayed({
                circle2.startAnimation(pulseAnimation)
            }, 300)

        } catch (e: Exception) {
            Log.e(TAG, "Error in animations: ${e.message}")
        }
    }

    private fun navigateToNextScreen() {
        try {
            // Sử dụng SessionManager để check session
            val sessionManager = SessionManager(this)
            val isLoggedIn = sessionManager.isLoggedIn()
            val isManager = sessionManager.isManager()
            val role = sessionManager.getRole()
            val username = sessionManager.getUsername()
            
            Log.d(TAG, "=== SESSION CHECK ===")
            Log.d(TAG, "User logged in: $isLoggedIn")
            Log.d(TAG, "Username: $username")
            Log.d(TAG, "Role: $role")
            Log.d(TAG, "Is Manager: $isManager")
            Log.d(TAG, "====================")

            val intent = when {
                isLoggedIn && isManager -> {
                    // Manager → ManagerActivity
                    Log.d(TAG, "✓ Redirecting to ManagerActivity")
                    Intent(this, ManagerActivity::class.java)
                }
                isLoggedIn -> {
                    // User → MainActivity
                    Log.d(TAG, "✓ Redirecting to MainActivity")
                    Intent(this, MainActivity::class.java)
                }
                else -> {
                    // Not logged in → WelcomeActivity
                    Log.d(TAG, "✓ Redirecting to WelcomeActivity")
                    Intent(this, WelcomeActivity::class.java)
                }
            }

            startActivity(intent)
            // FIX C2: Handle deprecated overridePendingTransition
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.fade_in, R.anim.fade_out)
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }
            finish()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating: ${e.message}")
            e.printStackTrace()
            // Fallback
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }
    }

    companion object {
        private const val TAG = "SplashActivity"
        private const val SPLASH_DELAY = 2500L // 2.5 seconds for better animation experience
    }
}
