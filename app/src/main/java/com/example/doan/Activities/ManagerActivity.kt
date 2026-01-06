package com.example.doan.Activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.doan.Fragments.Manager.DashboardFragment
import com.example.doan.Fragments.Manager.ForecastFragment
import com.example.doan.Fragments.Manager.ManageCategoriesFragment
import com.example.doan.Fragments.Manager.ManageChatsFragment
import com.example.doan.Fragments.Manager.ManageDrinksFragment
import com.example.doan.Fragments.Manager.ManageOrdersFragment
import com.example.doan.Fragments.Manager.ManagerSettingsFragment
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.Store
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.navigation.NavigationBarView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ManagerActivity : AppCompatActivity(), NavigationBarView.OnItemSelectedListener {

    private lateinit var sessionManager: SessionManager
    private var selectedItemId = R.id.nav_manager_dashboard
    private lateinit var btnForecast: MaterialCardView
    private lateinit var badgeWarning: TextView
    private lateinit var tvManagedStores: TextView
    private lateinit var btnNotifications: View
    private lateinit var badgeMonitoring: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manager)

        // Check if user is manager or admin
        sessionManager = SessionManager(this)
        if (!sessionManager.isManager() && !sessionManager.isAdmin()) {
            Toast.makeText(this, "Bạn không có quyền truy cập!", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Setup UI - hiển thị role phù hợp
        val userNameTextView = findViewById<TextView>(R.id.manager_user_name)
        val fullName = sessionManager.getFullName()
        val roleLabel = if (sessionManager.isAdmin()) "Admin" else "Manager"
        userNameTextView.text = if (!fullName.isNullOrEmpty()) {
            "Hi, $fullName ($roleLabel)"
        } else {
            "Hi, $roleLabel"
        }

        // Setup Forecast/Warning Button với animation
        btnForecast = findViewById(R.id.btn_forecast)
        badgeWarning = findViewById(R.id.badge_warning)
        tvManagedStores = findViewById(R.id.tv_managed_stores)
        btnNotifications = findViewById(R.id.btn_notifications)
        badgeMonitoring = findViewById(R.id.badge_monitoring)
        
        // 🛡️ Setup User Monitoring Button
        btnNotifications.setOnClickListener {
            startActivity(Intent(this, UserMonitoringActivity::class.java))
        }
        
        // Load số cảnh báo chờ xử lý
        loadPendingAlertsCount()
        
        // Load chi nhánh quản lý
        loadManagedStores()
        
        // Bắt đầu animation nhấp nháy cho badge
        startWarningAnimation()
        
        btnForecast.setOnClickListener {
            // Dừng animation khi click
            badgeWarning.clearAnimation()
            badgeWarning.visibility = View.GONE
            
            loadFragment(ForecastFragment(), true)
            // Deselect bottom nav items
            val bottomNav = findViewById<BottomNavigationView>(R.id.manager_bottom_navigation)
            bottomNav.menu.setGroupCheckable(0, true, false)
            for (i in 0 until bottomNav.menu.size()) {
                bottomNav.menu.getItem(i).isChecked = false
            }
            bottomNav.menu.setGroupCheckable(0, true, true)
        }

        // Setup Bottom Navigation
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.manager_bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener(this)

        // Setup back press handler using OnBackPressedCallback (API 33+)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showLogoutConfirmation()
            }
        })

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(DashboardFragment(), false)
        } else {
            selectedItemId = savedInstanceState.getInt("selectedItemId", R.id.nav_manager_dashboard)
        }
    }
    
    private fun startWarningAnimation() {
        val pulseAnim = AnimationUtils.loadAnimation(this, R.anim.pulse_warning)
        badgeWarning.startAnimation(pulseAnim)
        btnForecast.startAnimation(pulseAnim)
    }
    
    private fun loadManagedStores() {
        // Admin quản lý tất cả
        if (sessionManager.isAdmin()) {
            tvManagedStores.text = "🏪 Quản lý tất cả chi nhánh"
            return
        }
        
        RetrofitClient.getInstance(this).apiService
            .getMyManagedStores()
            .enqueue(object : Callback<ApiResponse<List<Store>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<Store>>>,
                    response: Response<ApiResponse<List<Store>>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val stores = response.body()?.data ?: emptyList()
                        if (stores.isEmpty()) {
                            tvManagedStores.text = "⚠️ Chưa được gán chi nhánh"
                        } else if (stores.size == 1) {
                            tvManagedStores.text = "🏪 ${stores[0].storeName}"
                        } else {
                            val storeNames = stores.take(2).joinToString(", ") { it.storeName ?: "N/A" }
                            val suffix = if (stores.size > 2) " +${stores.size - 2}" else ""
                            tvManagedStores.text = "🏪 $storeNames$suffix"
                        }
                    } else {
                        tvManagedStores.text = "🏪 Không thể tải chi nhánh"
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<Store>>>, t: Throwable) {
                    Log.e(TAG, "Error loading managed stores: ${t.message}")
                    tvManagedStores.text = "🏪 Lỗi kết nối"
                }
            })
    }
    
    override fun onResume() {
        super.onResume()
        // Restart animation khi quay lại activity
        if (badgeWarning.visibility == View.VISIBLE) {
            startWarningAnimation()
        }
        // Reload số cảnh báo
        loadPendingAlertsCount()
    }
    
    /**
     * 🛡️ Load số cảnh báo chờ xử lý để hiển thị badge
     */
    private fun loadPendingAlertsCount() {
        // Chỉ Admin mới xem được monitoring
        if (!sessionManager.isAdmin()) {
            badgeMonitoring.visibility = View.GONE
            return
        }
        
        RetrofitClient.getInstance(this).apiService
            .getMonitoringDashboard()
            .enqueue(object : Callback<ApiResponse<com.example.doan.Models.MonitoringDashboard>> {
                override fun onResponse(
                    call: Call<ApiResponse<com.example.doan.Models.MonitoringDashboard>>,
                    response: Response<ApiResponse<com.example.doan.Models.MonitoringDashboard>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val dashboard = response.body()?.data
                        val pendingCount = dashboard?.totalPendingAlerts ?: 0
                        
                        if (pendingCount > 0) {
                            badgeMonitoring.visibility = View.VISIBLE
                            badgeMonitoring.text = if (pendingCount > 99) "99+" else pendingCount.toString()
                            
                            // Animation nhấp nháy nếu có cảnh báo critical
                            val criticalCount = dashboard?.criticalAlerts ?: 0
                            if (criticalCount > 0) {
                                val pulseAnim = AnimationUtils.loadAnimation(this@ManagerActivity, R.anim.pulse_warning)
                                badgeMonitoring.startAnimation(pulseAnim)
                            }
                        } else {
                            badgeMonitoring.visibility = View.GONE
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<com.example.doan.Models.MonitoringDashboard>>, t: Throwable) {
                    Log.e(TAG, "Error loading pending alerts: ${t.message}")
                }
            })
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Đăng xuất")
            .setMessage("Bạn có muốn đăng xuất?")
            .setPositiveButton("Có") { _, _ ->
                sessionManager.logout()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .setNegativeButton("Không") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("selectedItemId", selectedItemId)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        selectedItemId = item.itemId

        val fragment: Fragment? = when (item.itemId) {
            R.id.nav_manager_dashboard -> DashboardFragment()
            R.id.nav_manager_drinks -> ManageDrinksFragment()
            R.id.nav_manager_orders -> ManageOrdersFragment()
            R.id.nav_manager_chats -> ManageChatsFragment()
            R.id.nav_manager_settings -> ManagerSettingsFragment()
            else -> null
        }

        fragment?.let {
            loadFragment(it, true)
            return true
        }

        return false
    }

    private fun loadFragment(fragment: Fragment, animate: Boolean) {
        supportFragmentManager.beginTransaction().apply {
            if (animate) {
                setCustomAnimations(
                    R.anim.fade_in,
                    R.anim.fade_out,
                    R.anim.fade_in,
                    R.anim.fade_out
                )
            }
            replace(R.id.manager_content_container, fragment)
            commit()
        }
    }

    companion object {
        private const val TAG = "ManagerActivity"
    }
}
