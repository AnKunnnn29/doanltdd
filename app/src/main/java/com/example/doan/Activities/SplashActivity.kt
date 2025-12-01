package com.example.doan.Activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.doan.R
import com.example.doan.Utils.SessionManager

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_splash)
            Log.d(TAG, "SplashActivity started")

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
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
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
        private const val SPLASH_DELAY = 2000L // 2 seconds
    }
}
