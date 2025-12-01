package com.example.doan.Activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.doan.R
import com.google.android.material.button.MaterialButton

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_welcome)
            Log.d(TAG, "WelcomeActivity started")

            val loginButton = findViewById<MaterialButton>(R.id.btn_welcome_login)
            val registerButton = findViewById<MaterialButton>(R.id.btn_welcome_register)
            val guestMode = findViewById<TextView>(R.id.text_guest_mode)

            // Login button
            loginButton.setOnClickListener {
                try {
                    startActivity(Intent(this, LoginActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting LoginActivity: ${e.message}")
                }
            }

            // Register button
            registerButton.setOnClickListener {
                try {
                    startActivity(Intent(this, RegisterActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting RegisterActivity: ${e.message}")
                }
            }

            // Guest mode - go directly to MainActivity
            guestMode.setOnClickListener {
                try {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    finish()
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting MainActivity: ${e.message}")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}")
            e.printStackTrace()
        }
    }

    companion object {
        private const val TAG = "WelcomeActivity"
    }
}
