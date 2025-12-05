package com.example.doan.Fragments

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.doan.Activities.*
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.UserProfileDto
import com.example.doan.Network.ApiService
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.SessionManager
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AccountFragment : Fragment() {

    // TÁC DỤNG: Màn hình này hiển thị thông tin tài khoản và các tùy chọn liên quan.

    private lateinit var sessionManager: SessionManager
    private lateinit var apiService: ApiService
    private lateinit var profileNameText: TextView
    private lateinit var profileEmailText: TextView
    private lateinit var userDetailOption: RelativeLayout
    private lateinit var orderHistoryOption: RelativeLayout // YÊU CẦU: Thêm mục "Lịch sử đơn hàng".
    private lateinit var profileOption: RelativeLayout
    private lateinit var changePasswordOption: RelativeLayout
    private lateinit var settingsOption: RelativeLayout
    private lateinit var logoutButton: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.activity_account, container, false)

        sessionManager = SessionManager(requireContext())
        apiService = RetrofitClient.getInstance(requireContext()).apiService

        // Gán các view từ layout.
        profileNameText = view.findViewById(R.id.profile_name)
        profileEmailText = view.findViewById(R.id.profile_email)
        userDetailOption = view.findViewById(R.id.user_detail_option)
        orderHistoryOption = view.findViewById(R.id.order_history_option) // YÊU CẦU: Gán view cho mục mới.
        profileOption = view.findViewById(R.id.profile_option)
        changePasswordOption = view.findViewById(R.id.change_password_option)
        settingsOption = view.findViewById(R.id.settings_option)
        logoutButton = view.findViewById(R.id.logout_button)

        // Thiết lập sự kiện click.
        userDetailOption.setOnClickListener { fetchAndShowUserDetails() }
        orderHistoryOption.setOnClickListener { 
            // YÊU CẦU: Khi nhấn, mở màn hình Lịch sử đơn hàng.
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

        logoutButton.setOnClickListener {
            if (sessionManager.isLoggedIn()) {
                performLogout()
            } else {
                startActivity(Intent(requireContext(), LoginActivity::class.java))
            }
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        // Làm mới thông tin header mỗi khi quay lại màn hình.
        profileNameText.text = sessionManager.getFullName()
        profileEmailText.text = sessionManager.getEmail()
    }

    private fun fetchAndShowUserDetails() {
        // TÁC DỤNG: Lấy và hiển thị chi tiết thông tin người dùng trong một dialog.
        Log.d("AccountFragment", "Fetching user details")

        apiService.getMyProfile().enqueue(object : Callback<ApiResponse<UserProfileDto>> {
            override fun onResponse(
                call: Call<ApiResponse<UserProfileDto>>,
                response: Response<ApiResponse<UserProfileDto>>
            ) {
                if (response.isSuccessful && response.body()?.data != null) {
                    val profile = response.body()!!.data
                    showUserDetailDialog(profile!!)

                    // Cập nhật session với dữ liệu mới nhất.
                    sessionManager.saveLoginSession(
                        userId = profile.id?.toInt() ?: -1,
                        username = profile.username,
                        email = profile.email,
                        fullName = profile.fullName,
                        phone = profile.phone,
                        role = sessionManager.getRole(),
                        memberTier = profile.memberTier,
                        token = sessionManager.getToken()
                    )

                } else {
                    Toast.makeText(requireContext(), "Lấy thông tin thất bại", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<UserProfileDto>>, t: Throwable) {
                Toast.makeText(requireContext(), "Lỗi mạng: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showUserDetailDialog(user: UserProfileDto) {
        // TÁC DỤNG: Hiển thị một dialog với thông tin chi tiết của người dùng.
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
        // TÁC DỤNG: Xóa session và điều hướng người dùng về màn hình đăng nhập.
        sessionManager.logout()

        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        requireActivity().finish()
    }
}
