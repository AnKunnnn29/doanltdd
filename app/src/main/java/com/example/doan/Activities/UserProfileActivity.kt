package com.example.doan.Activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.UpdateProfileRequest
import com.example.doan.Models.UserProfileDto
import com.example.doan.Network.ApiService
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.SessionManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserProfileActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var apiService: ApiService
    private lateinit var nameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var phoneEditText: TextInputEditText
    private lateinit var addressEditText: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar_user_profile)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        sessionManager = SessionManager(this)
        apiService = RetrofitClient.getInstance(this).apiService

        nameEditText = findViewById(R.id.input_profile_name)
        emailEditText = findViewById(R.id.input_profile_email)
        phoneEditText = findViewById(R.id.input_profile_phone)
        addressEditText = findViewById(R.id.input_profile_address)

        loadUserProfile()

        findViewById<MaterialButton>(R.id.btn_save_profile).setOnClickListener {
            updateUserProfile()
        }
    }

    private fun loadUserProfile() {
        apiService.getMyProfile().enqueue(object : Callback<ApiResponse<UserProfileDto>> {
            override fun onResponse(call: Call<ApiResponse<UserProfileDto>>, response: Response<ApiResponse<UserProfileDto>>) {
                if (response.isSuccessful && response.body()?.data != null) {
                    val profile = response.body()?.data!!
                    nameEditText.setText(profile.fullName)
                    emailEditText.setText(profile.email)
                    phoneEditText.setText(profile.phone)
                    addressEditText.setText(profile.address)
                } else {
                    Toast.makeText(this@UserProfileActivity, "Không thể tải hồ sơ", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<UserProfileDto>>, t: Throwable) {
                Toast.makeText(this@UserProfileActivity, "Lỗi mạng: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateUserProfile() {
        val request = UpdateProfileRequest(
            fullName = nameEditText.text.toString(),
            email = emailEditText.text.toString(),
            phone = phoneEditText.text.toString(),
            address = addressEditText.text.toString()
        )

        apiService.updateProfile(request).enqueue(object : Callback<ApiResponse<UserProfileDto>> {
            override fun onResponse(call: Call<ApiResponse<UserProfileDto>>, response: Response<ApiResponse<UserProfileDto>>) {
                if (response.isSuccessful && response.body()?.data != null) {
                    val updatedProfile = response.body()?.data!!
                    // Update SessionManager with new data, ensuring refreshToken is preserved
                    sessionManager.saveLoginSession(
                        userId = sessionManager.getUserId(),
                        username = updatedProfile.username,
                        email = updatedProfile.email,
                        fullName = updatedProfile.fullName,
                        phone = updatedProfile.phone,
                        role = sessionManager.getRole(),
                        memberTier = updatedProfile.memberTier,
                        token = sessionManager.getToken(),
                        refreshToken = sessionManager.getRefreshToken(), // Giữ lại refresh token
                        avatar = updatedProfile.avatar
                    )
                    Toast.makeText(this@UserProfileActivity, "Hồ sơ đã được cập nhật", Toast.LENGTH_SHORT).show()
                    finish() // Close activity after successful update
                } else {
                    Toast.makeText(this@UserProfileActivity, "Cập nhật thất bại", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<UserProfileDto>>, t: Throwable) {
                Toast.makeText(this@UserProfileActivity, "Lỗi mạng: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
