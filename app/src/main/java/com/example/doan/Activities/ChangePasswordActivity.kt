package com.example.doan.Activities

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.doan.Models.ChangePasswordRequest
import com.example.doan.Network.ApiService
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var currentPasswordEditText: TextInputEditText
    private lateinit var newPasswordEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText
    private lateinit var saveButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        apiService = RetrofitClient.getInstance(this).apiService

        currentPasswordEditText = findViewById(R.id.input_current_password)
        newPasswordEditText = findViewById(R.id.input_new_password)
        confirmPasswordEditText = findViewById(R.id.input_confirm_password)
        saveButton = findViewById(R.id.btn_save_password)

        saveButton.setOnClickListener {
            handleChangePassword()
        }
    }

    private fun handleChangePassword() {
        val currentPassword = currentPasswordEditText.text.toString().trim()
        val newPassword = newPasswordEditText.text.toString().trim()
        val confirmPassword = confirmPasswordEditText.text.toString().trim()

        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(this, "Mật khẩu mới không khớp", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (newPassword.length < 6) {
            Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show()
            return
        }

        saveButton.isEnabled = false
        saveButton.text = "ĐANG LƯU..."

        val request = ChangePasswordRequest(
            oldPassword = currentPassword,
            newPassword = newPassword,
            confirmPassword = confirmPassword
        )

        apiService.changePassword(request).enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ChangePasswordActivity, response.body() ?: "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@ChangePasswordActivity, errorBody ?: "Mật khẩu cũ không chính xác.", Toast.LENGTH_LONG).show()
                    Log.e("ChangePassword", "API Error: ${response.code()} - $errorBody")
                    resetButtonState()
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                Toast.makeText(this@ChangePasswordActivity, "Lỗi mạng: ${t.message}", Toast.LENGTH_LONG).show()
                Log.e("ChangePassword", "Network Failure", t)
                resetButtonState()
            }
        })
    }

    private fun resetButtonState() {
        saveButton.isEnabled = true
        saveButton.text = "LƯU THAY ĐỔI"
    }
}
