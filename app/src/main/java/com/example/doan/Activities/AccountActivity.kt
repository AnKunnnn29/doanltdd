package com.example.doan.Activities

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.UserProfileDto
import com.example.doan.Network.ApiService
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.imageview.ShapeableImageView
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class AccountActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var apiService: ApiService
    private lateinit var profileImage: ShapeableImageView
    private lateinit var editAvatarButton: FloatingActionButton

    // FIX C1: Use ActivityResultLauncher instead of deprecated startActivityForResult
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { uploadAvatar(it) }
    }

    // FIX C1: Use ActivityResultLauncher for permission request
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(this, "Permission denied to read external storage", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)

        sessionManager = SessionManager(this)
        apiService = RetrofitClient.getInstance(this).apiService

        profileImage = findViewById(R.id.profile_image)
        editAvatarButton = findViewById(R.id.fab_edit_avatar)
        val profileName = findViewById<TextView>(R.id.profile_name)
        val profileEmail = findViewById<TextView>(R.id.profile_email)

        profileName.text = sessionManager.getFullName()
        profileEmail.text = sessionManager.getEmail()

        editAvatarButton.setOnClickListener {
            Log.d("AccountActivity", "Edit avatar button clicked")
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(permission)
            } else {
                openGallery()
            }
        }

        findViewById<RelativeLayout>(R.id.user_detail_option).setOnClickListener {
            Log.d("AccountActivity", "User detail option clicked")
            fetchAndShowUserDetails()
        }

        findViewById<RelativeLayout>(R.id.profile_option).setOnClickListener {
            startActivity(Intent(this, UserProfileActivity::class.java))
        }

        findViewById<RelativeLayout>(R.id.change_password_option).setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }

        findViewById<RelativeLayout>(R.id.settings_option).setOnClickListener {
            Log.d("AccountActivity", "SETTING user details")
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.logout_button).setOnClickListener {
            sessionManager.logout()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun openGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun uploadAvatar(imageUri: Uri?) {
        if (imageUri == null) return

        val file = getFileFromUri(imageUri)
        if (file == null) {
            Toast.makeText(this, "Failed to create temporary file", Toast.LENGTH_SHORT).show()
            return
        }

        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

        apiService.uploadAvatar(body).enqueue(object : Callback<ApiResponse<UserProfileDto>> {
            override fun onResponse(
                call: Call<ApiResponse<UserProfileDto>>,
                response: Response<ApiResponse<UserProfileDto>>
            ) {
                if (isFinishing) return
                
                if (response.isSuccessful && response.body()?.data != null) {
                    val userProfile = response.body()?.data!!
                    sessionManager.saveLoginSession(
                        userId = userProfile.id?.toInt() ?: -1,
                        username = userProfile.username,
                        email = userProfile.email,
                        fullName = userProfile.fullName,
                        phone = userProfile.phone,
                        role = sessionManager.getRole(),
                        memberTier = userProfile.memberTier,
                        token = sessionManager.getToken(),
                        refreshToken = sessionManager.getRefreshToken(),
                        avatar = userProfile.avatar
                    )
                    Glide.with(this@AccountActivity)
                        .load(userProfile.avatar)
                        .into(profileImage)
                    Toast.makeText(this@AccountActivity, "Avatar updated successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@AccountActivity, "Failed to upload avatar", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<UserProfileDto>>, t: Throwable) {
                if (isFinishing) return
                Toast.makeText(this@AccountActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val file = File(cacheDir, "temp_avatar.jpg")
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    private fun fetchAndShowUserDetails() {
        Log.d("AccountActivity", "Fetching user details")
        val call = apiService.getMyProfile()
        call.enqueue(object : Callback<ApiResponse<UserProfileDto>> {
            override fun onResponse(
                call: Call<ApiResponse<UserProfileDto>>,
                response: Response<ApiResponse<UserProfileDto>>
            ) {
                if (isFinishing) return
                
                if (response.isSuccessful && response.body()?.data != null) {
                    val userProfile = response.body()?.data!!
                    showUserDetailDialog(userProfile)
                    sessionManager.saveLoginSession(
                        userId = userProfile.id?.toInt() ?: -1,
                        username = userProfile.username,
                        email = userProfile.email,
                        fullName = userProfile.fullName,
                        phone = userProfile.phone,
                        role = sessionManager.getRole(),
                        memberTier = userProfile.memberTier,
                        token = sessionManager.getToken(),
                        refreshToken = sessionManager.getRefreshToken(),
                        avatar = userProfile.avatar
                    )
                } else {
                    Toast.makeText(this@AccountActivity, "Lấy thông tin thất bại", Toast.LENGTH_SHORT).show()
                    Log.e("AccountActivity", "API call failed: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse<UserProfileDto>>, t: Throwable) {
                if (isFinishing) return
                Toast.makeText(this@AccountActivity, "Lỗi mạng: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("AccountActivity", "API call failed", t)
            }
        })
    }

    private fun showUserDetailDialog(userProfile: UserProfileDto) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_user_profile_detail)

        val fullName = dialog.findViewById<TextView>(R.id.tv_detail_full_name)
        val username = dialog.findViewById<TextView>(R.id.tv_detail_username)
        val email = dialog.findViewById<TextView>(R.id.tv_detail_email)
        val phone = dialog.findViewById<TextView>(R.id.tv_detail_phone)
        val address = dialog.findViewById<TextView>(R.id.tv_detail_address)
        val tier = dialog.findViewById<TextView>(R.id.tv_detail_tier)
        val points = dialog.findViewById<TextView>(R.id.tv_detail_points)
        val closeButton = dialog.findViewById<Button>(R.id.btn_close_dialog)

        fullName.text = userProfile.fullName ?: "N/A"
        username.text = "@${userProfile.username ?: "N/A"}"
        email.text = userProfile.email ?: "N/A"
        phone.text = userProfile.phone ?: "N/A"
        address.text = userProfile.address ?: "N/A"
        tier.text = userProfile.memberTier ?: "N/A"
        points.text = "${userProfile.points ?: 0} điểm"

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        val profileName = findViewById<TextView>(R.id.profile_name)
        val profileEmail = findViewById<TextView>(R.id.profile_email)
        profileName.text = sessionManager.getFullName()
        profileEmail.text = sessionManager.getEmail()
    }
}
