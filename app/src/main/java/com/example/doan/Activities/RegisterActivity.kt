package com.example.doan.Activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.doan.R

class RegisterActivity : AppCompatActivity() {

    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var registerButton: Button
    private lateinit var loginLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        usernameInput = findViewById(R.id.input_reg_username)
        passwordInput = findViewById(R.id.input_reg_password)
        confirmPasswordInput = findViewById(R.id.input_reg_confirm_password)
        emailInput = findViewById(R.id.input_reg_email)
        registerButton = findViewById(R.id.btn_register_submit)
        loginLink = findViewById(R.id.text_login_link)

        registerButton.setOnClickListener { attemptRegister() }
        loginLink.setOnClickListener { finish() }
    }

    private fun attemptRegister() {
        val username = usernameInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()
        val confirmPassword = confirmPasswordInput.text.toString().trim()
        val email = emailInput.text.toString().trim()

        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin bắt buộc.", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Mật khẩu xác nhận không khớp.", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự.", Toast.LENGTH_SHORT).show()
            return
        }

        // Chuyển sang OtpActivity ngay lập tức với dữ liệu đăng ký
        val intent = Intent(this, OtpActivity::class.java).apply {
            putExtra("USERNAME", username)
            putExtra("PASSWORD", password)
            putExtra("EMAIL", email)
        }
        Log.d("RegisterActivity", "Starting OtpActivity with data: $username, $password, $email")
        startActivity(intent)
//        finish()
    }
}
