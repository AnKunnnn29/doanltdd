package com.example.doan.Fragments

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
    private lateinit var memberTierOption: RelativeLayout

    // Avatar animation views
    private lateinit var avatarContainer: FrameLayout
    private lateinit var avatarGlowOuter: View
    private lateinit var avatarGlowRing: View
    private lateinit var avatarBorder: View

    private var tempImageUri: Uri? = null

    // FIX C1: Use ActivityResultLauncher instead of deprecated startActivityForResult
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { uploadAvatar(it) }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            tempImageUri?.let { uploadAvatar(it) }
        }
    }

    // FIX C1: Use ActivityResultLauncher for permission request
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
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
        memberTierOption = view.findViewById(R.id.member_tier_option)

        // Avatar animation views
        avatarContainer = view.findViewById(R.id.avatar_container)
        avatarGlowOuter = view.findViewById(R.id.avatar_glow_outer)
        avatarGlowRing = view.findViewById(R.id.avatar_glow_ring)
        avatarBorder = view.findViewById(R.id.avatar_border)

        // Khởi động animation cho avatar
        startAvatarAnimations()

        // Thiết lập sự kiện click.
        fabEditAvatar.setOnClickListener { showImageSourceDialog() }
        userDetailOption.setOnClickListener {
            startActivity(Intent(requireContext(), UserDetailActivity::class.java))
        }
        orderHistoryOption.setOnClickListener {
            startActivity(Intent(requireContext(), OrderHistoryActivity::class.java))
        }
        memberTierOption.setOnClickListener {
            startActivity(Intent(requireContext(), MemberTierActivity::class.java))
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

    private fun showImageSourceDialog() {
        val options = arrayOf("Chụp ảnh", "Chọn từ thư viện")
        AlertDialog.Builder(requireContext())
            .setTitle("Thay đổi ảnh đại diện")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCameraWithPermission()
                    1 -> openGalleryWithPermission()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun openCameraWithPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            openCamera()
        }
    }

    private fun openCamera() {
        tempImageUri = createImageFileUri()
        // Make sure tempImageUri is not null
        tempImageUri?.let {
             takePictureLauncher.launch(it)
        }
    }

    private fun createImageFileUri(): Uri? {
        val imageFile = File(requireContext().cacheDir, "temp_avatar_camera.jpg")
        return try {
            FileProvider.getUriForFile(
                requireContext(),
                // Use your application's package name + .provider
                "com.example.doan.provider",
                imageFile
            )
        } catch (e: Exception) {
            Log.e("AccountFragment", "Error creating file URI", e)
            Toast.makeText(requireContext(), "Lỗi tạo file ảnh. Vui lòng cấu hình FileProvider.", Toast.LENGTH_LONG).show()
            null
        }
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

    /**
     * Khởi động các animation đẹp cho avatar
     */
    private fun startAvatarAnimations() {
        // Animation bounce cho avatar khi xuất hiện
        val bounceAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.avatar_bounce_in)
        profileImage.startAnimation(bounceAnim)

        // Animation xoay cho vòng glow bên ngoài
        val rotateAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.avatar_glow_rotate)
        avatarGlowRing.startAnimation(rotateAnim)

        // Animation pulse cho border
        val pulseAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.avatar_pulse)
        avatarBorder.startAnimation(pulseAnim)

        // Animation scale cho FAB
        val fabAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.fab_scale_in)
        fabEditAvatar.startAnimation(fabAnim)

        // Animation fade cho outer glow
        avatarGlowOuter.alpha = 0f
        avatarGlowOuter.animate()
            .alpha(0.6f)
            .setDuration(1000)
            .setStartDelay(200)
            .start()
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
            requestPermissionLauncher.launch(permission)
        } else {
            openGallery()
        }
    }

    private fun openGallery() {
        pickImageLauncher.launch("image/*")
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