package com.example.doan.Activities

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.UserProfileDto
import com.example.doan.Network.ApiService
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationBarView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AccountActivity : AppCompatActivity(), NavigationBarView.OnItemSelectedListener {

    private lateinit var sessionManager: SessionManager
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)

        sessionManager = SessionManager(this)
        apiService = RetrofitClient.getInstance(this).apiService

        val profileName = findViewById<TextView>(R.id.profile_name)
        val profileEmail = findViewById<TextView>(R.id.profile_email)

        profileName.text = sessionManager.getFullName()
        profileEmail.text = sessionManager.getEmail()

        findViewById<RelativeLayout>(R.id.user_detail_option).setOnClickListener {
            fetchAndShowUserDetails()
        }

        findViewById<RelativeLayout>(R.id.profile_option).setOnClickListener {
            startActivity(Intent(this, UserProfileActivity::class.java))
        }

        findViewById<RelativeLayout>(R.id.change_password_option).setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }

        findViewById<RelativeLayout>(R.id.settings_option).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.logout_button).setOnClickListener {
            sessionManager.logout()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }



        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener(this)
        bottomNavigationView.selectedItemId = R.id.nav_account
    }

    private fun fetchAndShowUserDetails() {
        // You can show a loading indicator here
        val call = apiService.getMyProfile()
        call.enqueue(object : Callback<ApiResponse<UserProfileDto>> {
            override fun onResponse(
                call: Call<ApiResponse<UserProfileDto>>,
                response: Response<ApiResponse<UserProfileDto>>
            ) {
                // Hide loading indicator
                if (response.isSuccessful && response.body()?.data != null) {
                    val userProfile = response.body()?.data!!
                    showUserDetailDialog(userProfile)
                    // Optionally, update session manager with fresh data
                    sessionManager.saveLoginSession(
                        userId = userProfile.id?.toInt() ?: -1,
                        username = userProfile.username,
                        email = userProfile.email,
                        fullName = userProfile.fullName,
                        phone = userProfile.phone,
                        role = sessionManager.getRole(), // Role is not in UserProfileDto, so we keep the old one
                        memberTier = userProfile.memberTier,
                        token = sessionManager.getToken()
                    )
                } else {
                    Toast.makeText(this@AccountActivity, "Lấy thông tin thất bại", Toast.LENGTH_SHORT).show()
                    Log.e("AccountActivity", "API call failed: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse<UserProfileDto>>, t: Throwable) {
                // Hide loading indicator
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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home, R.id.nav_order, R.id.nav_store -> {
                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra("SELECTED_ITEM", item.itemId)
                    addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(intent)
                return true
            }
            R.id.nav_account -> {
                // Already on the account screen
                return true
            }
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        // Refresh profile name and email on header in case it was changed
        val profileName = findViewById<TextView>(R.id.profile_name)
        val profileEmail = findViewById<TextView>(R.id.profile_email)
        profileName.text = sessionManager.getFullName()
        profileEmail.text = sessionManager.getEmail()
        
        bottomNavigationView.selectedItemId = R.id.nav_account
    }
}
