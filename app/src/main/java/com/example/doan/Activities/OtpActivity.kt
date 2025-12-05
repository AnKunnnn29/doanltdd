package com.example.doan.Activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.RegisterRequest
import com.example.doan.Models.VerifyOtpRequest
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OtpActivity : AppCompatActivity() {

    private lateinit var otpInput: EditText
    private lateinit var verifyButton: Button
    private lateinit var resendOtpText: TextView

    private var username: String = ""
    private var password: String = ""
    private var email: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp)

        otpInput = findViewById(R.id.input_otp)
        verifyButton = findViewById(R.id.btn_verify_otp)
        resendOtpText = findViewById(R.id.text_resend_otp)

        // Nhận dữ liệu từ RegisterActivity
        intent?.let {
            if (it.hasExtra("USERNAME") && it.hasExtra("PASSWORD") && it.hasExtra("EMAIL")) {
                username = it.getStringExtra("USERNAME") ?: ""
                password = it.getStringExtra("PASSWORD") ?: ""
                email = it.getStringExtra("EMAIL") ?: ""

                // Gọi API đăng ký để server gửi OTP
                requestRegistrationAndOtp()
            } else {
                Toast.makeText(this, "Lỗi: Không tìm thấy thông tin đăng ký.", Toast.LENGTH_LONG).show()
                finish()
                return
            }
        }

        verifyButton.setOnClickListener {
            val otp = otpInput.text.toString().trim()
            if (otp.length != 6) {
                Toast.makeText(this, "Mã OTP phải có 6 chữ số.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            verifyOtp(otp)
        }

        resendOtpText.setOnClickListener { resendOtp() }
    }

    private fun requestRegistrationAndOtp() {
        Toast.makeText(this, "Đang gửi yêu cầu đăng ký...", Toast.LENGTH_SHORT).show()
        val registerRequest = RegisterRequest(username, email, password, username, "")

        RetrofitClient.getInstance(this).apiService.registerWithOtp(registerRequest)
            .enqueue(object : Callback<ApiResponse<String>> {
                override fun onResponse(call: Call<ApiResponse<String>>, response: Response<ApiResponse<String>>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@OtpActivity, response.body()?.message, Toast.LENGTH_SHORT).show()
                    } else {
                        var errorMessage = "Lỗi khi gửi OTP."
                        try {
                            errorMessage = response.body()?.message 
                                ?: response.errorBody()?.string()?.let { JSONObject(it).getString("message") }
                                ?: errorMessage
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing error response in requestRegistrationAndOtp", e)
                        }
                        Toast.makeText(this@OtpActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<String>>, t: Throwable) {
                    Log.e(TAG, "Lỗi mạng khi đăng ký: ${t.message}")
                }
            })
    }

    private fun verifyOtp(otp: String) {
        val request = VerifyOtpRequest(email, otp)

        RetrofitClient.getInstance(this).apiService.verifyOtp(request)
            .enqueue(object : Callback<ApiResponse<String>> {
                override fun onResponse(call: Call<ApiResponse<String>>, response: Response<ApiResponse<String>>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@OtpActivity, "Kích hoạt thành công! Vui lòng đăng nhập.", Toast.LENGTH_LONG).show()
                        val intent = Intent(this@OtpActivity, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        var errorMessage = "Lỗi xác thực."
                        try {
                            response.errorBody()?.string()?.let { errorBodyStr ->
                                val errorObj = JSONObject(errorBodyStr)
                                if (errorObj.has("message")) {
                                    errorMessage = errorObj.getString("message")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing error body in verifyOtp", e)
                        }
                        Toast.makeText(this@OtpActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<String>>, t: Throwable) {
                    Log.e(TAG, "Lỗi Retrofit khi xác thực OTP: ${t.message}")
                    Toast.makeText(this@OtpActivity, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun resendOtp() {
        RetrofitClient.getInstance(this).apiService.resendOtp(email)
            .enqueue(object : Callback<ApiResponse<String>> {
                override fun onResponse(call: Call<ApiResponse<String>>, response: Response<ApiResponse<String>>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@OtpActivity, "OTP đã được gửi lại!", Toast.LENGTH_SHORT).show()
                    } else {
                        var errorMessage = "Lỗi gửi lại OTP."
                        errorMessage = response.body()?.message 
                            ?: try {
                                response.errorBody()?.string()?.let { JSONObject(it).getString("message") }
                            } catch (e: Exception) { null }
                            ?: errorMessage
                        Toast.makeText(this@OtpActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<String>>, t: Throwable) {
                    Log.e(TAG, "Network Error on resend: ${t.message}")
                    Toast.makeText(this@OtpActivity, "Không thể kết nối đến server.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    companion object {
        private const val TAG = "OtpActivity"
    }
}
