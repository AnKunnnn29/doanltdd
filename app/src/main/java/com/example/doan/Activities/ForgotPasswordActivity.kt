package com.example.doan.Activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.doan.Models.ForgotPasswordRequest
import com.example.doan.Network.ApiService
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var emailInput: TextInputEditText
    private lateinit var submitButton: MaterialButton
    private lateinit var backToLoginLink: TextView
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        apiService = RetrofitClient.getInstance(this).apiService

        emailInput = findViewById(R.id.input_forgot_email)
        submitButton = findViewById(R.id.btn_forgot_submit)
        backToLoginLink = findViewById(R.id.text_back_to_login)

        submitButton.setOnClickListener { sendPasswordResetRequest() }
        backToLoginLink.setOnClickListener { finish() }
    }

    private fun sendPasswordResetRequest() {
        val email = emailInput.text.toString().trim()

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Vui lòng nhập một địa chỉ email hợp lệ", Toast.LENGTH_SHORT).show()
            return
        }

        submitButton.isEnabled = false
        submitButton.text = "ĐANG GỬI..."

        val request = ForgotPasswordRequest(email = email)

        apiService.forgotPassword(request).enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ForgotPasswordActivity, response.body() ?: "Yêu cầu đã được gửi đi.", Toast.LENGTH_LONG).show()
                    val intent = Intent(this@ForgotPasswordActivity, ResetPasswordActivity::class.java)
                    intent.putExtra("USER_EMAIL", email)
                    startActivity(intent)
                    finish()
                } else {
                    // Handle non-2xx responses here, you might get an error message in the body
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@ForgotPasswordActivity, errorBody ?: "Email không tồn tại trong hệ thống.", Toast.LENGTH_SHORT).show()
                    resetButtonState()
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                Toast.makeText(this@ForgotPasswordActivity, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("ForgotPassword", "Network Failure", t)
                resetButtonState()
            }
        })
    }

    private fun resetButtonState() {
        submitButton.isEnabled = true
        submitButton.text = "GỬI YÊU CẦU"
    }
}
