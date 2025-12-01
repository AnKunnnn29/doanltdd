package com.example.doan.Activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.doan.R
import com.google.android.material.button.MaterialButton

class StartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.acitivity_start)

        val btnStart = findViewById<MaterialButton>(R.id.btn_start)
        
        btnStart.setOnClickListener {
            // Khi bấm Bắt đầu, chuyển sang SplashActivity
            startActivity(Intent(this, SplashActivity::class.java))
            finish()
        }
    }
}
