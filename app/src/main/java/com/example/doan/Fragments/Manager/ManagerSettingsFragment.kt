package com.example.doan.Fragments.Manager

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.doan.Activities.LoginActivity
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.Store
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.SessionManager
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ManagerSettingsFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private lateinit var tvManagerName: TextView
    private lateinit var tvManagerPhone: TextView
    private lateinit var tvManagerRole: Chip
    private lateinit var tvManagedStoresInfo: TextView
    private lateinit var cardStore: MaterialCardView
    private lateinit var cardUsers: MaterialCardView
    private lateinit var cardVouchers: MaterialCardView
    private lateinit var cardNotifications: MaterialCardView
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
        tvManagedStoresInfo = view.findViewById(R.id.tv_managed_stores_info)

        cardStore = view.findViewById(R.id.card_store)
        cardUsers = view.findViewById(R.id.card_users)
        cardVouchers = view.findViewById(R.id.card_vouchers)
        cardLogout = view.findViewById(R.id.card_logout)
        cardNotifications = view.findViewById(R.id.card_notifications)

        // Load manager info
        loadManagerInfo()
        
        // Load managed stores
        loadManagedStores()

        // Setup listeners
        setupListeners()

        // Animate cards
        animateCardsIn()

        return view
    }

    private fun animateCardsIn() {
        val cards = listOf(cardStore, cardUsers, cardVouchers, cardNotifications, cardLogout)
        cards.forEachIndexed { index, card ->
            card.alpha = 0f
            card.translationX = -50f
            card.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(350)
                .setStartDelay((index * 60).toLong())
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun loadManagerInfo() {
        val fullName = sessionManager.getFullName()
        val phone = sessionManager.getPhoneNumber()
        val role = sessionManager.getRole()

        tvManagerName.text = fullName ?: "Manager"
        tvManagerPhone.text = phone ?: "N/A"
        tvManagerRole.text = role
    }
    
    private fun loadManagedStores() {
        // Admin quản lý tất cả
        if (sessionManager.isAdmin()) {
            tvManagedStoresInfo.text = "Quản lý tất cả chi nhánh"
            tvManagedStoresInfo.visibility = View.VISIBLE
            return
        }
        
        RetrofitClient.getInstance(requireContext()).apiService
            .getMyManagedStores()
            .enqueue(object : Callback<ApiResponse<List<Store>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<Store>>>,
                    response: Response<ApiResponse<List<Store>>>
                ) {
                    if (!isAdded) return
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        val stores = response.body()?.data ?: emptyList()
                        tvManagedStoresInfo.visibility = View.VISIBLE
                        
                        if (stores.isEmpty()) {
                            tvManagedStoresInfo.text = "⚠️ Chưa được gán chi nhánh nào"
                            tvManagedStoresInfo.setTextColor(resources.getColor(R.color.warning, null))
                        } else {
                            val storeNames = stores.joinToString("\n") { "• ${it.storeName}" }
                            tvManagedStoresInfo.text = "Chi nhánh quản lý:\n$storeNames"
                            tvManagedStoresInfo.setTextColor(resources.getColor(R.color.wine_light_surface, null))
                        }
                    } else {
                        tvManagedStoresInfo.text = "Không thể tải thông tin chi nhánh"
                        tvManagedStoresInfo.visibility = View.VISIBLE
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<Store>>>, t: Throwable) {
                    if (!isAdded) return
                    Log.e("ManagerSettings", "Error loading stores: ${t.message}")
                    tvManagedStoresInfo.text = "Lỗi kết nối"
                    tvManagedStoresInfo.visibility = View.VISIBLE
                }
            })
    }

    private fun setupListeners() {
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

        // Notifications management card
        cardNotifications.setOnClickListener {
            navigateToFragment(NotificationManagerFragment())
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
