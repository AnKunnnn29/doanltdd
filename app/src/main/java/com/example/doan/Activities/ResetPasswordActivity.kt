package com.example.doan.Activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.doan.Models.ResetPasswordRequest
import com.example.doan.Network.ApiService
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var tokenInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var confirmPasswordInput: TextInputEditText
    private lateinit var submitButton: MaterialButton
    private lateinit var apiService: ApiService
    private var userEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        apiService = RetrofitClient.getInstance(this).apiService
        userEmail = intent.getStringExtra("USER_EMAIL")

        if (userEmail == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy email người dùng.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        tokenInput = findViewById(R.id.input_reset_token)
        passwordInput = findViewById(R.id.input_reset_password)
        confirmPasswordInput = findViewById(R.id.input_reset_confirm_password)
        submitButton = findViewById(R.id.btn_reset_submit)

        submitButton.setOnClickListener { attemptPasswordReset() }
    }

    private fun attemptPasswordReset() {
        val token = tokenInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()
        val confirmPassword = confirmPasswordInput.text.toString().trim()

        if (token.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show()
            return
        }

        submitButton.isEnabled = false
        submitButton.text = "ĐANG XỬ LÝ..."

        val request = ResetPasswordRequest(email = userEmail!!, otp = token, newPassword = password)

        apiService.resetPassword(request).enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ResetPasswordActivity, response.body() ?: "Đặt lại mật khẩu thành công.", Toast.LENGTH_LONG).show()
                    val intent = Intent(this@ResetPasswordActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@ResetPasswordActivity, errorBody ?: "Đặt lại mật khẩu thất bại.", Toast.LENGTH_SHORT).show()
                    resetButtonState()
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                Toast.makeText(this@ResetPasswordActivity, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("ResetPassword", "Network Failure", t)
                resetButtonState()
            }
        })
    }

    private fun resetButtonState() {
        submitButton.isEnabled = true
        submitButton.text = "ĐẶT LẠI MẬT KHẨU"
    }
}
