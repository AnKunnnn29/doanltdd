package com.example.doan.Fragments.Manager

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.doan.Activities.LoginActivity
import com.example.doan.R
import com.example.doan.Utils.SessionManager
import com.google.android.material.card.MaterialCardView

class ManagerSettingsFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private lateinit var tvManagerName: TextView
    private lateinit var tvManagerPhone: TextView
    private lateinit var tvManagerRole: TextView
    private lateinit var cardProfile: MaterialCardView
    private lateinit var cardStore: MaterialCardView
    private lateinit var cardUsers: MaterialCardView
    private lateinit var cardVouchers: MaterialCardView
    private lateinit var cardLogout: MaterialCardView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_manager_settings, container, false)

        sessionManager = SessionManager(requireContext())

        // Initialize views
        tvManagerName = view.findViewById(R.id.tv_manager_name)
        tvManagerPhone = view.findViewById(R.id.tv_manager_phone)
        tvManagerRole = view.findViewById(R.id.tv_manager_role)
        
        cardProfile = view.findViewById(R.id.card_profile)
        cardStore = view.findViewById(R.id.card_store)
        cardUsers = view.findViewById(R.id.card_users)
        cardVouchers = view.findViewById(R.id.card_vouchers)
        cardLogout = view.findViewById(R.id.card_logout)

        // Load manager info
        loadManagerInfo()

        // Setup listeners
        setupListeners()

        return view
    }

    private fun loadManagerInfo() {
        val fullName = sessionManager.getFullName()
        val phone = sessionManager.getPhoneNumber()
        val role = sessionManager.getRole()

        tvManagerName.text = fullName ?: "Manager"
        tvManagerPhone.text = phone ?: "N/A"
        tvManagerRole.text = role
    }

    private fun setupListeners() {
        // Profile card
        cardProfile.setOnClickListener {
            Toast.makeText(context, "Chỉnh sửa profile - Coming soon", Toast.LENGTH_SHORT).show()
        }

        // Store management card
        cardStore.setOnClickListener {
            navigateToFragment(ManageStoresFragment())
        }

        // Users management card
        cardUsers.setOnClickListener {
            navigateToFragment(ManageUsersFragment())
        }

        // Vouchers management card
        cardVouchers.setOnClickListener {
            navigateToFragment(ManageVouchersFragment())
        }

        // Logout card
        cardLogout.setOnClickListener { showLogoutDialog() }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Đăng xuất")
            .setMessage("Bạn có chắc muốn đăng xuất?")
            .setPositiveButton("Đăng xuất") { _, _ ->
                // Logout
                sessionManager.logout()
                
                // Go to login
                val intent = Intent(activity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                
                activity?.finish()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun navigateToFragment(fragment: Fragment) {
        activity?.supportFragmentManager
            ?.beginTransaction()
            ?.replace(R.id.manager_content_container, fragment)
            ?.addToBackStack(null)
            ?.commit()
    }
}
