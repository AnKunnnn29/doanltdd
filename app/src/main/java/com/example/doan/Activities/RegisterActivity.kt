package com.example.doan.Activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
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

    companion object {
        private const val TAG = "RegisterActivity"
        // FIX C6: Regex patterns cho validation
        private val USERNAME_PATTERN = Regex("^[a-zA-Z0-9_]{3,50}$")
        private const val MIN_PASSWORD_LENGTH = 6
        private const val MAX_PASSWORD_LENGTH = 100
    }

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
        // Clear previous errors
        usernameInput.error = null
        passwordInput.error = null
        confirmPasswordInput.error = null
        emailInput.error = null
        
        val username = usernameInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()
        val confirmPassword = confirmPasswordInput.text.toString().trim()
        val email = emailInput.text.toString().trim()

        // FIX C6: Validate tất cả input trước khi gửi API
        if (!validateInputs(username, password, confirmPassword, email)) {
            return
        }

        // Chuyển sang OtpActivity ngay lập tức với dữ liệu đăng ký
        val intent = Intent(this, OtpActivity::class.java).apply {
            putExtra("USERNAME", username)
            putExtra("PASSWORD", password)
            putExtra("EMAIL", email)
        }
        Log.d(TAG, "Starting OtpActivity with data: $username, [password hidden], $email")
        startActivity(intent)
    }
    
    /**
     * FIX C6: Validate tất cả input fields
     * @return true nếu tất cả input hợp lệ, false nếu có lỗi
     */
    private fun validateInputs(username: String, password: String, confirmPassword: String, email: String): Boolean {
        var isValid = true
        
        // Validate username
        when {
            username.isEmpty() -> {
                usernameInput.error = "Vui lòng nhập tên đăng nhập"
                usernameInput.requestFocus()
                isValid = false
            }
            username.length < 3 -> {
                usernameInput.error = "Tên đăng nhập phải có ít nhất 3 ký tự"
                if (isValid) usernameInput.requestFocus()
                isValid = false
            }
            username.length > 50 -> {
                usernameInput.error = "Tên đăng nhập không được quá 50 ký tự"
                if (isValid) usernameInput.requestFocus()
                isValid = false
            }
            !USERNAME_PATTERN.matches(username) -> {
                usernameInput.error = "Tên đăng nhập chỉ được chứa chữ cái, số và dấu gạch dưới"
                if (isValid) usernameInput.requestFocus()
                isValid = false
            }
        }
        
        // Validate email
        when {
            email.isEmpty() -> {
                emailInput.error = "Vui lòng nhập email"
                if (isValid) emailInput.requestFocus()
                isValid = false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                emailInput.error = "Email không hợp lệ"
                if (isValid) emailInput.requestFocus()
                isValid = false
            }
        }
        
        // Validate password
        when {
            password.isEmpty() -> {
                passwordInput.error = "Vui lòng nhập mật khẩu"
                if (isValid) passwordInput.requestFocus()
                isValid = false
            }
            password.length < MIN_PASSWORD_LENGTH -> {
                passwordInput.error = "Mật khẩu phải có ít nhất $MIN_PASSWORD_LENGTH ký tự"
                if (isValid) passwordInput.requestFocus()
                isValid = false
            }
            password.length > MAX_PASSWORD_LENGTH -> {
                passwordInput.error = "Mật khẩu không được quá $MAX_PASSWORD_LENGTH ký tự"
                if (isValid) passwordInput.requestFocus()
                isValid = false
            }
        }
        
        // Validate confirm password
        when {
            confirmPassword.isEmpty() -> {
                confirmPasswordInput.error = "Vui lòng xác nhận mật khẩu"
                if (isValid) confirmPasswordInput.requestFocus()
                isValid = false
            }
            password != confirmPassword -> {
                confirmPasswordInput.error = "Mật khẩu xác nhận không khớp"
                if (isValid) confirmPasswordInput.requestFocus()
                isValid = false
            }
        }
        
        return isValid
    }
}
