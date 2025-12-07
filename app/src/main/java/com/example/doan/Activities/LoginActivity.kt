package com.example.doan.Activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.doan.Models.*
import com.example.doan.Network.ApiService
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.KeyStoreManager
import com.example.doan.Utils.SessionManager
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.Executor

class LoginActivity : AppCompatActivity() {

    private lateinit var usernameInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var loginButton: Button
    private lateinit var registerLink: TextView
    private lateinit var forgotPasswordLink: TextView
    private lateinit var sessionManager: SessionManager
    private lateinit var apiService: ApiService
    private lateinit var biometricLoginButton: ImageButton

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

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
        biometricLoginButton = findViewById(R.id.btn_biometric_login)

        loginButton.setOnClickListener { attemptLogin() }
        registerLink.setOnClickListener { navigateToRegister() }
        forgotPasswordLink.setOnClickListener { navigateToForgotPassword() }

        // Biometric Login
        if (KeyStoreManager.isBiometricEnrolled(this)) {
            biometricLoginButton.visibility = View.VISIBLE
            biometricLoginButton.setOnClickListener {
                startBiometricLogin()
            }
        }
    }

    private fun startBiometricLogin() {
        executor = ContextCompat.getMainExecutor(this)
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Đăng nhập bằng vân tay")
            .setSubtitle("Sử dụng vân tay của bạn để đăng nhập")
            .setNegativeButtonText("Hủy")
            .build()

        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                try {
                    val refreshToken = KeyStoreManager.decryptToken(this@LoginActivity, result.cryptoObject!!)
                    callRefreshTokenApi(refreshToken)
                } catch (e: Exception) {
                    Toast.makeText(applicationContext, "Lỗi giải mã token: " + e.message, Toast.LENGTH_LONG).show()
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(applicationContext, "Lỗi xác thực: $errString", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(applicationContext, "Xác thực thất bại", Toast.LENGTH_SHORT).show()
            }
        })

        try {
            val cryptoObject = KeyStoreManager.getDecryptionCryptoObject(this)
            biometricPrompt.authenticate(promptInfo, cryptoObject)
        } catch (e: Exception) {
            Toast.makeText(applicationContext, "Vân tay chưa được thiết lập: " + e.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun callRefreshTokenApi(refreshToken: String) {
        apiService.refreshToken(RefreshTokenRequest(refreshToken)).enqueue(object : Callback<ApiResponse<JwtResponse>> {
            override fun onResponse(call: Call<ApiResponse<JwtResponse>>, response: Response<ApiResponse<JwtResponse>>) {
                if (response.isSuccessful && response.body()?.data != null) {
                    val jwtResponse = response.body()!!.data!!
                    sessionManager.updateTokens(jwtResponse)
                    fetchProfileAndLogin()
                } else {
                    Toast.makeText(this@LoginActivity, "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show()
                    KeyStoreManager.clearBiometricData(this@LoginActivity)
                    biometricLoginButton.visibility = View.GONE
                }
            }

            override fun onFailure(call: Call<ApiResponse<JwtResponse>>, t: Throwable) {
                Toast.makeText(this@LoginActivity, "Lỗi mạng: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchProfileAndLogin() {
        // Khởi tạo lại ApiService để nó dùng Retrofit client mới
        val newApiService = RetrofitClient.getInstance(this).apiService
        newApiService.getMyProfile().enqueue(object : Callback<ApiResponse<UserProfileDto>> {
            override fun onResponse(call: Call<ApiResponse<UserProfileDto>>, response: Response<ApiResponse<UserProfileDto>>) {
                if (response.isSuccessful && response.body()?.data != null) {
                    val profile = response.body()?.data!!
                    sessionManager.saveLoginSession(
                        userId = profile.id?.toInt() ?: -1,
                        username = profile.username,
                        email = profile.email,
                        fullName = profile.fullName,
                        phone = profile.phone,
                        role = profile.role,
                        memberTier = profile.memberTier,
                        token = sessionManager.getToken(), // Lấy token mới từ session
                        refreshToken = sessionManager.getRefreshToken(), // Lấy refresh token mới từ session
                        avatar = profile.avatar
                    )
                    navigateToMain()
                } else {
                    Toast.makeText(this@LoginActivity, "Không thể lấy thông tin người dùng. Mã lỗi: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<UserProfileDto>>, t: Throwable) {
                Toast.makeText(this@LoginActivity, "Lỗi mạng: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
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
                            token = loginResponse.token,
                            refreshToken = loginResponse.refreshToken,
                            avatar = loginResponse.avatar
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
