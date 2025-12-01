package com.example.doan.Fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.doan.Activities.LoginActivity
import com.example.doan.R
import com.example.doan.Utils.SessionManager

class AccountFragment : Fragment() {

    private lateinit var profileOption: LinearLayout
    private lateinit var settingsOption: LinearLayout
    private lateinit var logoutOption: LinearLayout
    private lateinit var profileNameText: TextView
    private var logoutText: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_account, container, false)

        profileOption = view.findViewById(R.id.profile_option)
        settingsOption = view.findViewById(R.id.settings_option)
        logoutOption = view.findViewById(R.id.logout_option)
        profileNameText = view.findViewById(R.id.profile_name)

        if (logoutOption.childCount > 2 && logoutOption.getChildAt(2) is TextView) {
            logoutText = logoutOption.getChildAt(2) as TextView
        }

        profileOption.setOnClickListener { handleProfileClick() }
        settingsOption.setOnClickListener { handleSettingsClick() }
        logoutOption.setOnClickListener { handleLogoutClick() }

        return view
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun isUserLoggedIn(): Boolean {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    private fun updateUI() {
        if (isUserLoggedIn()) {
            val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val userName = prefs.getString(KEY_USER_NAME, "Người dùng")

            profileNameText.text = "Xin chào, $userName"
            logoutText?.text = "Đăng xuất"
        } else {
            profileNameText.text = "Đăng nhập / Đăng ký"
            logoutText?.text = "Đăng nhập"
        }
    }

    private fun handleProfileClick() {
        if (isUserLoggedIn()) {
            Toast.makeText(context, "Chuyển đến Hồ sơ (Profile)", Toast.LENGTH_SHORT).show()
        } else {
            startActivity(Intent(context, LoginActivity::class.java))
        }
    }

    private fun handleSettingsClick() {
        Toast.makeText(context, "Chuyển đến Cài đặt (Settings)", Toast.LENGTH_SHORT).show()
    }

    private fun handleLogoutClick() {
        if (isUserLoggedIn()) {
            performLogout()
        } else {
            startActivity(Intent(context, LoginActivity::class.java))
        }
    }

    private fun performLogout() {
        handleLogoutSuccess()
    }

    private fun handleLogoutSuccess() {
        // Xóa session bằng SessionManager
        val sessionManager = SessionManager(requireContext())
        sessionManager.logout()
        
        // Xóa SharedPreferences cũ
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            clear()
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }

        Toast.makeText(context, "Đã đăng xuất thành công!", Toast.LENGTH_LONG).show()
        updateUI()
    }

    companion object {
        private const val PREFS_NAME = "UserPrefs"
        private const val KEY_USER_ID = "userId"
        private const val KEY_USER_NAME = "userName"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
    }
}
