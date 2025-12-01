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
import com.google.android.material.button.MaterialButton

class AccountFragment : Fragment() {

    private lateinit var profileOption: LinearLayout
    private lateinit var settingsOption: LinearLayout
    private lateinit var logoutButton: MaterialButton
    private lateinit var profileNameText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_account, container, false)

        profileOption = view.findViewById(R.id.profile_option)
        settingsOption = view.findViewById(R.id.settings_option)
        logoutButton = view.findViewById(R.id.logout_button)
        profileNameText = view.findViewById(R.id.profile_name)

        profileOption.setOnClickListener { handleProfileClick() }
        settingsOption.setOnClickListener { handleSettingsClick() }
        logoutButton.setOnClickListener { handleLogoutClick() }

        return view
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun isUserLoggedIn(): Boolean {
        val sessionManager = SessionManager(requireContext())
        return sessionManager.isLoggedIn()
    }

    private fun updateUI() {
        if (isUserLoggedIn()) {
            val sessionManager = SessionManager(requireContext())
            val userName = sessionManager.getUsername() // Assuming you have this method

            profileNameText.text = "Xin chào, $userName"
            logoutButton.text = "Đăng xuất"
        } else {
            profileNameText.text = "Đăng nhập / Đăng ký"
            logoutButton.text = "Đăng nhập"
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
        val sessionManager = SessionManager(requireContext())
        sessionManager.logout()

        Toast.makeText(context, "Đã đăng xuất thành công!", Toast.LENGTH_LONG).show()
        updateUI()
    }
}
