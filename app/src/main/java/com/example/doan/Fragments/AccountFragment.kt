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

    private lateinit var sessionManager: SessionManager
    private lateinit var apiService: ApiService
    private lateinit var profileNameText: TextView
    private lateinit var profileEmailText: TextView
    private lateinit var userDetailOption: RelativeLayout
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

        // Header info
        profileNameText = view.findViewById(R.id.profile_name)
        profileEmailText = view.findViewById(R.id.profile_email)

        // Options
        userDetailOption = view.findViewById(R.id.user_detail_option)
        profileOption = view.findViewById(R.id.profile_option)
        changePasswordOption = view.findViewById(R.id.change_password_option)
        settingsOption = view.findViewById(R.id.settings_option)
        logoutButton = view.findViewById(R.id.logout_button)

        // Set Click Events
        userDetailOption.setOnClickListener { fetchAndShowUserDetails() }
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

        // Refresh header
        profileNameText.text = sessionManager.getFullName()
        profileEmailText.text = sessionManager.getEmail()
    }

    //          API CALL
    private fun fetchAndShowUserDetails() {
        Log.d("AccountFragment", "Fetching user details")

        apiService.getMyProfile().enqueue(object : Callback<ApiResponse<UserProfileDto>> {
            override fun onResponse(
                call: Call<ApiResponse<UserProfileDto>>,
                response: Response<ApiResponse<UserProfileDto>>
            ) {
                if (response.isSuccessful && response.body()?.data != null) {
                    val profile = response.body()!!.data
                    showUserDetailDialog(profile!!)

                    // cập nhật session với data mới
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

    //        DIALOG DETAIL
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

    //            LOGOUT
    private fun performLogout() {
        sessionManager.logout()

        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        requireActivity().finish()
    }
}
