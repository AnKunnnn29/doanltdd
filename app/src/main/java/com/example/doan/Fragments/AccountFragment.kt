package com.example.doan.Fragments

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.doan.Activities.*
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.UserProfileDto
import com.example.doan.Network.ApiService
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.DataCache
import com.example.doan.Utils.LoadingDialog
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

class AccountFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private lateinit var apiService: ApiService
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var profileNameText: TextView
    private lateinit var profileEmailText: TextView
    private lateinit var userDetailOption: RelativeLayout
    private lateinit var orderHistoryOption: RelativeLayout
    private lateinit var profileOption: RelativeLayout
    private lateinit var changePasswordOption: RelativeLayout
    private lateinit var settingsOption: RelativeLayout
    private lateinit var logoutButton: MaterialButton
    private lateinit var profileImage: ShapeableImageView
    private lateinit var fabEditAvatar: FloatingActionButton
    private lateinit var deleteAccountOption: RelativeLayout

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
        private const val READ_MEDIA_IMAGES_REQUEST_CODE = 102
        private const val READ_EXTERNAL_STORAGE_REQUEST_CODE = 101
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.activity_account, container, false)

        sessionManager = SessionManager(requireContext())
        apiService = RetrofitClient.getInstance(requireContext()).apiService
        loadingDialog = LoadingDialog(requireContext())

        // Gán các view từ layout.
        profileNameText = view.findViewById(R.id.profile_name)
        profileEmailText = view.findViewById(R.id.profile_email)
        profileImage = view.findViewById(R.id.profile_image)
        fabEditAvatar = view.findViewById(R.id.fab_edit_avatar)
        userDetailOption = view.findViewById(R.id.user_detail_option)
        orderHistoryOption = view.findViewById(R.id.order_history_option)
        profileOption = view.findViewById(R.id.profile_option)
        changePasswordOption = view.findViewById(R.id.change_password_option)
        settingsOption = view.findViewById(R.id.settings_option)
        logoutButton = view.findViewById(R.id.logout_button)
        deleteAccountOption = view.findViewById(R.id.delete_account_option)

        // Thiết lập sự kiện click.
        fabEditAvatar.setOnClickListener { openGalleryWithPermission() }
        userDetailOption.setOnClickListener { fetchAndShowUserDetails() }
        orderHistoryOption.setOnClickListener {
            startActivity(Intent(requireContext(), OrderHistoryActivity::class.java))
        }
        profileOption.setOnClickListener {
            startActivity(Intent(requireContext(), UserProfileActivity::class.java))
        }
        changePasswordOption.setOnClickListener {
            startActivity(Intent(requireContext(), ChangePasswordActivity::class.java))
        }
        settingsOption.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
        deleteAccountOption.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        logoutButton.setOnClickListener {
            if (sessionManager.isLoggedIn()) {
                performLogout()
            } else {
                startActivity(Intent(requireContext(), LoginActivity::class.java))
            }
        }

        return view
    }
    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Xác nhận xóa tài khoản")
            .setMessage("Bạn có chắc chắn muốn xóa tài khoản không? Hành động này không thể hoàn tác.")
            .setPositiveButton("Xóa") { _, _ ->
                deleteAccount()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun deleteAccount() {
        loadingDialog.show("Đang xóa tài khoản...")
        
        apiService.deleteAccount().enqueue(object : Callback<ApiResponse<String>> {
            override fun onResponse(
                call: Call<ApiResponse<String>>,
                response: Response<ApiResponse<String>>
            ) {
                if (!isAdded) return
                loadingDialog.dismiss()
                
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Tài khoản đã được xóa thành công.", Toast.LENGTH_SHORT).show()
                    performLogout()
                } else {
                    Toast.makeText(requireContext(), "Xóa tài khoản thất bại.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<String>>, t: Throwable) {
                if (!isAdded) return
                loadingDialog.dismiss()
                Toast.makeText(requireContext(), "Lỗi mạng: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun openGalleryWithPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
            val requestCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                READ_MEDIA_IMAGES_REQUEST_CODE
            } else {
                READ_EXTERNAL_STORAGE_REQUEST_CODE
            }
            requestPermissions(arrayOf(permission), requestCode)
        } else {
            openGallery()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == READ_EXTERNAL_STORAGE_REQUEST_CODE || requestCode == READ_MEDIA_IMAGES_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery()
            } else {
                Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data?.data != null) {
            uploadAvatar(data.data)
        }
    }

    private fun uploadAvatar(imageUri: Uri?) {
        if (imageUri == null) return

        val file = getFileFromUri(imageUri)
        if (file == null) {
            Toast.makeText(requireContext(), "Failed to create temporary file", Toast.LENGTH_SHORT).show()
            return
        }

        loadingDialog.show("Đang tải ảnh lên...")

        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

        apiService.uploadAvatar(body).enqueue(object : Callback<ApiResponse<UserProfileDto>> {
            override fun onResponse(
                call: Call<ApiResponse<UserProfileDto>>,
                response: Response<ApiResponse<UserProfileDto>>
            ) {
                if (!isAdded) return
                loadingDialog.dismiss()
                
                if (response.isSuccessful && response.body()?.data != null) {
                    val userProfile = response.body()?.data!!
                    
                    // Cập nhật cache
                    DataCache.userProfile = userProfile
                    
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

                    Glide.with(this@AccountFragment)
                        .load(userProfile.avatar)
                        .into(profileImage)
                    Toast.makeText(requireContext(), "Cập nhật ảnh đại diện thành công", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Cập nhật ảnh đại diện thất bại", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<UserProfileDto>>, t: Throwable) {
                if (!isAdded) return
                loadingDialog.dismiss()
                Toast.makeText(requireContext(), "Lỗi mạng: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = requireActivity().contentResolver.openInputStream(uri) ?: return null
            val file = File(requireContext().cacheDir, "temp_avatar.jpg")
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

    override fun onResume() {
        super.onResume()
        profileNameText.text = sessionManager.getFullName()
        profileEmailText.text = sessionManager.getEmail()

        sessionManager.getAvatar()?.let {
            Glide.with(this)
                .load(it)
                .into(profileImage)
        }
    }

    private fun fetchAndShowUserDetails() {
        Log.d("AccountFragment", "Fetching user details")
        
        // Kiểm tra cache trước
        val cachedProfile = DataCache.userProfile
        if (cachedProfile != null) {
            showUserDetailDialog(cachedProfile)
            // Vẫn refresh data trong background
            refreshUserProfile()
            return
        }

        loadingDialog.show("Đang tải thông tin...")

        apiService.getMyProfile().enqueue(object : Callback<ApiResponse<UserProfileDto>> {
            override fun onResponse(
                call: Call<ApiResponse<UserProfileDto>>,
                response: Response<ApiResponse<UserProfileDto>>
            ) {
                if (!isAdded) return
                loadingDialog.dismiss()
                
                if (response.isSuccessful && response.body()?.data != null) {
                    val profile = response.body()!!.data!!
                    
                    // Lưu vào cache
                    DataCache.userProfile = profile
                    
                    showUserDetailDialog(profile)

                    sessionManager.saveLoginSession(
                        userId = profile.id?.toInt() ?: -1,
                        username = profile.username,
                        email = profile.email,
                        fullName = profile.fullName,
                        phone = profile.phone,
                        role = sessionManager.getRole(),
                        memberTier = profile.memberTier,
                        token = sessionManager.getToken(),
                        refreshToken = sessionManager.getRefreshToken(),
                        avatar = profile.avatar
                    )
                } else {
                    Toast.makeText(requireContext(), "Lấy thông tin thất bại", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<UserProfileDto>>, t: Throwable) {
                if (!isAdded) return
                loadingDialog.dismiss()
                Toast.makeText(requireContext(), "Lỗi mạng: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun refreshUserProfile() {
        apiService.getMyProfile().enqueue(object : Callback<ApiResponse<UserProfileDto>> {
            override fun onResponse(
                call: Call<ApiResponse<UserProfileDto>>,
                response: Response<ApiResponse<UserProfileDto>>
            ) {
                if (!isAdded) return
                
                if (response.isSuccessful && response.body()?.data != null) {
                    val profile = response.body()!!.data!!
                    DataCache.userProfile = profile
                    
                    sessionManager.saveLoginSession(
                        userId = profile.id?.toInt() ?: -1,
                        username = profile.username,
                        email = profile.email,
                        fullName = profile.fullName,
                        phone = profile.phone,
                        role = sessionManager.getRole(),
                        memberTier = profile.memberTier,
                        token = sessionManager.getToken(),
                        refreshToken = sessionManager.getRefreshToken(),
                        avatar = profile.avatar
                    )
                    
                    // Cập nhật UI
                    profileNameText.text = profile.fullName
                    profileEmailText.text = profile.email
                }
            }

            override fun onFailure(call: Call<ApiResponse<UserProfileDto>>, t: Throwable) {
                Log.e("AccountFragment", "Failed to refresh profile", t)
            }
        })
    }

    private fun showUserDetailDialog(user: UserProfileDto) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_user_profile_detail)

        dialog.findViewById<TextView>(R.id.tv_detail_full_name).text = user.fullName ?: "N/A"
        dialog.findViewById<TextView>(R.id.tv_detail_username).text = "@${user.username ?: "N/A"}"
        dialog.findViewById<TextView>(R.id.tv_detail_email).text = user.email ?: "N/A"
        dialog.findViewById<TextView>(R.id.tv_detail_phone).text = user.phone ?: "N/A"
        dialog.findViewById<TextView>(R.id.tv_detail_address).text = user.address ?: "N/A"
        dialog.findViewById<TextView>(R.id.tv_detail_tier).text = user.memberTier ?: "N/A"
        dialog.findViewById<TextView>(R.id.tv_detail_points).text = "${user.points ?: 0} điểm"

        dialog.findViewById<Button>(R.id.btn_close_dialog).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun performLogout() {
        sessionManager.logout()
        
        // Xóa cache khi logout
        DataCache.clearAll()

        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        requireActivity().finish()
    }
}
