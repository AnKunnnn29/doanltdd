package com.example.doan.Activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.LoginRequest
import com.example.doan.Models.LoginResponse
import com.example.doan.Network.ApiService
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.SessionManager
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var usernameInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var loginButton: Button
    private lateinit var registerLink: TextView
    private lateinit var forgotPasswordLink: TextView
    private lateinit var sessionManager: SessionManager
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        sessionManager = SessionManager(this)
        apiService = RetrofitClient.getInstance(this).apiService

        if (sessionManager.isLoggedIn()) {
            navigateToMain()
            return
        }

        usernameInput = findViewById(R.id.input_login_username)
        passwordInput = findViewById(R.id.input_login_password)
        loginButton = findViewById(R.id.btn_login)
        registerLink = findViewById(R.id.text_register_link)
        forgotPasswordLink = findViewById(R.id.text_forgot_password)

        loginButton.setOnClickListener { attemptLogin() }
        registerLink.setOnClickListener { navigateToRegister() }
        forgotPasswordLink.setOnClickListener { navigateToForgotPassword() }
    }

    private fun attemptLogin() {
        val username = usernameInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên đăng nhập và mật khẩu", Toast.LENGTH_SHORT).show()
            return
        }

        val loginRequest = LoginRequest(username, password)
        apiService.login(loginRequest).enqueue(object : Callback<ApiResponse<LoginResponse>> {
            override fun onResponse(call: Call<ApiResponse<LoginResponse>>, response: Response<ApiResponse<LoginResponse>>) {
                if (response.isSuccessful) {
                    val loginResponse = response.body()?.data
                    if (loginResponse != null) {
                        sessionManager.saveLoginSession(
                            userId = loginResponse.userId,
                            username = loginResponse.username,
                            email = loginResponse.email,
                            fullName = loginResponse.fullName,
                            phone = loginResponse.phone,
                            role = loginResponse.role,
                            memberTier = loginResponse.memberTier,
                            token = loginResponse.token
                        )
                        navigateToMain()
                    } else {
                        Toast.makeText(this@LoginActivity, response.body()?.message ?: "Đã có lỗi xảy ra", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@LoginActivity, "Sai tên đăng nhập hoặc mật khẩu", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<LoginResponse>>, t: Throwable) {
                Toast.makeText(this@LoginActivity, "Lỗi mạng: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToForgotPassword() {
        val intent = Intent(this, ForgotPasswordActivity::class.java)
        startActivity(intent)
    }
}
