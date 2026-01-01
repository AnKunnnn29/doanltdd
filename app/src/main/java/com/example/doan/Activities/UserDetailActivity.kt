package com.example.doan.Activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.UserProfileDto
import com.example.doan.Network.ApiService
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.DataCache
import com.example.doan.Utils.SessionManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class UserDetailActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var apiService: ApiService

    // Views
    private lateinit var toolbar: MaterialToolbar
    private lateinit var progressLoading: ProgressBar
    private lateinit var layoutError: LinearLayout
    private lateinit var tvErrorMessage: TextView
    private lateinit var btnRetry: MaterialButton
    private lateinit var layoutContent: LinearLayout

    // Profile views
    private lateinit var imgAvatar: ShapeableImageView
    private lateinit var tvFullName: TextView
    private lateinit var tvUsername: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvAddress: TextView
    private lateinit var imgTierIcon: ImageView
    private lateinit var tvTier: TextView
    private lateinit var tvPoints: TextView
    private lateinit var btnEditProfile: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_detail)

        sessionManager = SessionManager(this)
        apiService = RetrofitClient.getInstance(this).apiService

        initViews()
        setupToolbar()
        setupClickListeners()
        loadUserProfile()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar_user_detail)
        progressLoading = findViewById(R.id.progress_loading)
        layoutError = findViewById(R.id.layout_error)
        tvErrorMessage = findViewById(R.id.tv_error_message)
        btnRetry = findViewById(R.id.btn_retry)
        layoutContent = findViewById(R.id.layout_content)

        imgAvatar = findViewById(R.id.img_avatar)
        tvFullName = findViewById(R.id.tv_full_name)
        tvUsername = findViewById(R.id.tv_username)
        tvEmail = findViewById(R.id.tv_email)
        tvPhone = findViewById(R.id.tv_phone)
        tvAddress = findViewById(R.id.tv_address)
        imgTierIcon = findViewById(R.id.img_tier_icon)
        tvTier = findViewById(R.id.tv_tier)
        tvPoints = findViewById(R.id.tv_points)
        btnEditProfile = findViewById(R.id.btn_edit_profile)
    }

    private fun setupToolbar() {
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupClickListeners() {
        btnRetry.setOnClickListener { loadUserProfile() }
        btnEditProfile.setOnClickListener {
            startActivity(Intent(this, UserProfileActivity::class.java))
        }
    }

    private fun loadUserProfile() {
        // Kiểm tra cache trước
        val cachedProfile = DataCache.userProfile
        if (cachedProfile != null) {
            Log.d("UserDetailActivity", "Using cached profile")
            displayUserProfile(cachedProfile)
            refreshUserProfile() // Refresh trong background
            return
        }

        // Nếu không có cache, thử dùng dữ liệu từ SessionManager
        val sessionProfile = getProfileFromSession()
        if (sessionProfile != null) {
            Log.d("UserDetailActivity", "Using session profile")
            displayUserProfile(sessionProfile)
            refreshUserProfile() // Refresh trong background
            return
        }

        showLoading()

        Log.d("UserDetailActivity", "Fetching profile from API")
        apiService.getMyProfile().enqueue(object : Callback<ApiResponse<UserProfileDto>> {
            override fun onResponse(
                call: Call<ApiResponse<UserProfileDto>>,
                response: Response<ApiResponse<UserProfileDto>>
            ) {
                Log.d("UserDetailActivity", "API Response: ${response.code()}")
                if (response.isSuccessful && response.body()?.data != null) {
                    val profile = response.body()!!.data!!
                    DataCache.userProfile = profile
                    displayUserProfile(profile)
                    updateSessionManager(profile)
                } else {
                    Log.e("UserDetailActivity", "API Error: ${response.code()} - ${response.errorBody()?.string()}")
                    // Fallback to session data
                    val fallbackProfile = getProfileFromSession()
                    if (fallbackProfile != null) {
                        displayUserProfile(fallbackProfile)
                    } else {
                        showError("Không thể tải thông tin người dùng (${response.code()})")
                    }
                }
            }

            override fun onFailure(call: Call<ApiResponse<UserProfileDto>>, t: Throwable) {
                Log.e("UserDetailActivity", "API Failure: ${t.message}", t)
                // Fallback to session data
                val fallbackProfile = getProfileFromSession()
                if (fallbackProfile != null) {
                    displayUserProfile(fallbackProfile)
                } else {
                    showError("Lỗi mạng: ${t.message}")
                }
            }
        })
    }

    private fun getProfileFromSession(): UserProfileDto? {
        val fullName = sessionManager.getFullName()
        val email = sessionManager.getEmail()
        
        // Nếu có dữ liệu cơ bản từ session, tạo profile
        if (!fullName.isNullOrEmpty() || !email.isNullOrEmpty()) {
            return UserProfileDto(
                id = sessionManager.getUserId().toLong(),
                username = sessionManager.getUsername(),
                fullName = fullName,
                email = email,
                phone = sessionManager.getPhoneNumber(),
                address = null,
                memberTier = sessionManager.getMemberTier(),
                points = 0,
                avatar = sessionManager.getAvatar(),
                role = sessionManager.getRole()
            )
        }
        return null
    }

    private fun refreshUserProfile() {
        apiService.getMyProfile().enqueue(object : Callback<ApiResponse<UserProfileDto>> {
            override fun onResponse(
                call: Call<ApiResponse<UserProfileDto>>,
                response: Response<ApiResponse<UserProfileDto>>
            ) {
                if (response.isSuccessful && response.body()?.data != null) {
                    val profile = response.body()!!.data!!
                    DataCache.userProfile = profile
                    displayUserProfile(profile)
                    updateSessionManager(profile)
                }
            }

            override fun onFailure(call: Call<ApiResponse<UserProfileDto>>, t: Throwable) {
                // Silent fail for background refresh
            }
        })
    }

    private fun displayUserProfile(profile: UserProfileDto) {
        showContent()

        // Avatar
        profile.avatar?.let {
            Glide.with(this).load(it).into(imgAvatar)
        }

        // Basic info
        tvFullName.text = profile.fullName ?: "N/A"
        tvUsername.text = "@${profile.username ?: "N/A"}"
        tvEmail.text = profile.email ?: "N/A"
        tvPhone.text = profile.phone ?: "Chưa cập nhật"
        tvAddress.text = profile.address ?: "Chưa cập nhật"

        // Member tier
        val tier = profile.memberTier ?: "BRONZE"
        tvTier.text = tier
        setTierStyle(tier)

        // Points
        val points = profile.points ?: 0
        tvPoints.text = NumberFormat.getNumberInstance(Locale("vi", "VN")).format(points)
    }

    private fun setTierStyle(tier: String) {
        val (iconRes, colorRes) = when (tier.uppercase()) {
            "GOLD" -> Pair(R.drawable.ic_tier_gold, R.color.wine_accent_gold)
            "SILVER" -> Pair(R.drawable.ic_tier_silver, R.color.wine_neutral_dark)
            "PLATINUM", "DIAMOND" -> Pair(R.drawable.ic_tier_platinum, R.color.wine_primary)
            else -> Pair(R.drawable.ic_tier_bronze, R.color.wine_accent_bronze)
        }
        imgTierIcon.setImageResource(iconRes)
        tvTier.setTextColor(getColor(colorRes))
    }

    private fun updateSessionManager(profile: UserProfileDto) {
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
    }

    private fun showLoading() {
        progressLoading.visibility = View.VISIBLE
        layoutError.visibility = View.GONE
        layoutContent.visibility = View.GONE
    }

    private fun showError(message: String) {
        progressLoading.visibility = View.GONE
        layoutError.visibility = View.VISIBLE
        layoutContent.visibility = View.GONE
        tvErrorMessage.text = message
    }

    private fun showContent() {
        progressLoading.visibility = View.GONE
        layoutError.visibility = View.GONE
        layoutContent.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        // Refresh khi quay lại từ UserProfileActivity
        refreshUserProfile()
    }
}
