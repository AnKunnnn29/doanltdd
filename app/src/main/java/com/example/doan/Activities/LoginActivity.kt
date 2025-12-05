package com.example.doan.Activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.LoginRequest
import com.example.doan.Models.LoginResponse
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var registerLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Ánh xạ View
        usernameInput = findViewById(R.id.input_login_username)
        passwordInput = findViewById(R.id.input_login_password)
        loginButton = findViewById(R.id.btn_login)
        registerLink = findViewById(R.id.text_register_link)

        // Xử lý sự kiện Đăng nhập
        loginButton.setOnClickListener { attemptLogin() }

        // Xử lý sự kiện Đăng ký
        registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    private fun attemptLogin() {
        val username = usernameInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        // Kiểm tra Validation cơ bản
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên đăng nhập và mật khẩu.", Toast.LENGTH_SHORT).show()
            return
        }

        // Tạo Request Body
        val loginRequest = LoginRequest(username, password)

        // Gọi API Đăng nhập
        RetrofitClient.getInstance(this).apiService.login(loginRequest)
            .enqueue(object : Callback<ApiResponse<LoginResponse>> {
                override fun onResponse(
                    call: Call<ApiResponse<LoginResponse>>,
                    response: Response<ApiResponse<LoginResponse>>
                ) {
                    Log.e(TAG, "========================================")
                    Log.e(TAG, "=== API RESPONSE RECEIVED ===")
                    Log.e(TAG, "Response code: ${response.code()}")
                    Log.e(TAG, "Response successful: ${response.isSuccessful}")
                    
                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!
                        Log.e(TAG, "ApiResponse success: ${apiResponse.success}")
                        Log.e(TAG, "ApiResponse message: ${apiResponse.message}")
                        Log.e(TAG, "ApiResponse data is null: ${apiResponse.data == null}")
                        
                        if (apiResponse.success && apiResponse.data != null) {
                            val loginResponse = apiResponse.data!!
                            Log.e(TAG, "LoginResponse object: $loginResponse")
                            Log.e(TAG, "LoginResponse.role: ${loginResponse.role}")
                            Log.e(TAG, "LoginResponse.username: ${loginResponse.username}")
                            Log.e(TAG, "========================================")
                            handleLoginSuccess(loginResponse)
                        } else {
                            val message = apiResponse.message ?: "Sai tên đăng nhập hoặc mật khẩu."
                            Toast.makeText(this@LoginActivity, message, Toast.LENGTH_LONG).show()
                            Log.e(TAG, "Login failed: $message")
                        }
                    } else {
                        val message = if (response.code() == 401) {
                            "Sai tên đăng nhập hoặc mật khẩu. Vui lòng thử lại."
                        } else {
                            "Lỗi Server: ${response.code()}"
                        }
                        Toast.makeText(this@LoginActivity, message, Toast.LENGTH_LONG).show()
                        Log.e(TAG, "Login failed, Code: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<ApiResponse<LoginResponse>>, t: Throwable) {
                    Toast.makeText(this@LoginActivity, "Không thể kết nối Server: ${t.message}", Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Connection failure: ${t.message}")
                }
            })
    }

    private fun handleLoginSuccess(response: LoginResponse) {
        // DEBUG: Log thông tin response
        Log.e(TAG, "=== LOGIN SUCCESS DEBUG ===")
        Log.e(TAG, "Response object: $response")
        Log.e(TAG, "User ID: ${response.userId}")
        Log.e(TAG, "Username: ${response.username}")
        Log.e(TAG, "FullName: ${response.fullName}")
        Log.e(TAG, "Phone: ${response.phone}")
        Log.e(TAG, "Token: ${if (response.token != null) "EXISTS" else "NULL"}")
        Log.e(TAG, "Role (raw): '${response.role}'")
        Log.e(TAG, "Role is null: ${response.role == null}")
        Log.e(TAG, "Role length: ${response.role?.length ?: "N/A"}")
        Log.e(TAG, "Role equals MANAGER: ${"MANAGER" == response.role}")
        Log.e(TAG, "Role equals USER: ${"USER" == response.role}")
        Log.e(TAG, "Is Manager: ${response.isManager()}")
        Log.e(TAG, "========================")
        
        // Lưu Session sử dụng SessionManager
        val sessionManager = SessionManager(this)
        sessionManager.saveLoginSession(
            response.userId,
            response.username,
            response.fullName,
            response.phone,
            response.role,
            response.memberTier,
            response.token
        )

        // Lưu thêm vào SharedPreferences cũ (để tương thích với code cũ)
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt(KEY_USER_ID, response.userId)
            putString(KEY_USER_NAME, response.username)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putBoolean(KEY_IS_ADMIN, response.isManager())
            apply()
        }

        // Check role và chuyển hướng
        val isManager = response.isManager()
        
        Log.e(TAG, "Checking role for redirect...")
        Log.e(TAG, "isManager() result: $isManager")
        
        val intent = if (isManager) {
            // Manager → ManagerActivity
            Log.e(TAG, "✓✓✓ REDIRECTING TO MANAGER ACTIVITY ✓✓✓")
            Toast.makeText(this, "Chào Manager: ${response.fullName}", Toast.LENGTH_SHORT).show()
            Intent(this, ManagerActivity::class.java)
        } else {
            // User → MainActivity
            Log.e(TAG, "✗✗✗ REDIRECTING TO MAIN ACTIVITY ✗✗✗")
            Toast.makeText(this, "Xin chào, ${response.fullName}", Toast.LENGTH_SHORT).show()
            Intent(this, MainActivity::class.java)
        }
        
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }

    companion object {
        private const val TAG = "LoginActivity"
        private const val PREFS_NAME = "UserPrefs"
        private const val KEY_USER_ID = "userId"
        private const val KEY_USER_NAME = "userName"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_IS_ADMIN = "isAdmin"
    }
}
