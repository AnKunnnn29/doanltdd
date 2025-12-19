package com.example.doan.Activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.doan.Fragments.AccountFragment
import com.example.doan.Fragments.HomeFragment
import com.example.doan.Fragments.MenuFragment
import com.example.doan.Fragments.StoreFragment
import com.example.doan.Network.AuthInterceptor
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView

class MainActivity : AppCompatActivity(), NavigationBarView.OnItemSelectedListener {

    // TÁC DỤNG: Activity chính của ứng dụng, quản lý BottomNavigationView và các Fragment chính.

    private var selectedItemId = R.id.nav_home
    private lateinit var bottomNavigationView: BottomNavigationView
    
    // FIX C4: BroadcastReceiver để xử lý token expired
    private val tokenExpiredReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.w(TAG, "Token expired broadcast received")
            Toast.makeText(this@MainActivity, "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show()
            navigateToLogin()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_main)

            val sessionManager = SessionManager(this)

            // YÊU CẦU: Chuyển hướng người dùng quản lý sang màn hình riêng.
            if (sessionManager.isLoggedIn() && sessionManager.isManager()) {
                Log.d(TAG, "Manager detected, redirecting to ManagerActivity")
                startActivity(Intent(this, ManagerActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                finish()
                return
            }

            bottomNavigationView = findViewById(R.id.bottom_navigation)
            bottomNavigationView.setOnItemSelectedListener(this)

            if (savedInstanceState == null) {
                // Tải HomeFragment làm màn hình mặc định khi ứng dụng khởi động.
                loadFragment(HomeFragment(), false)
            }
            
            handleIntent(intent)
            
            // FIX C4: Đăng ký broadcast receiver cho token expired
            LocalBroadcastManager.getInstance(this).registerReceiver(
                tokenExpiredReceiver,
                IntentFilter(AuthInterceptor.ACTION_TOKEN_EXPIRED)
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}")
            e.printStackTrace()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // FIX C4: Hủy đăng ký broadcast receiver
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(tokenExpiredReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver: ${e.message}")
        }
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Lưu lại ID của tab đang được chọn để khôi phục khi cần (ví dụ: xoay màn hình).
        outState.putInt("selectedItemId", selectedItemId)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        selectedItemId = savedInstanceState.getInt("selectedItemId", R.id.nav_home)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Xử lý intent được gửi đến MainActivity khi nó đã đang chạy.
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        // TÁC DỤNG: Điều hướng đến một tab cụ thể nếu có yêu cầu từ intent.
        val navToItem = intent?.getIntExtra("SELECTED_ITEM", -1)
        if (navToItem != null && navToItem != -1) {
            bottomNavigationView.selectedItemId = navToItem
        }
    }

    override fun onResume() {
        super.onResume()
        // Cập nhật số lượng sản phẩm trong giỏ hàng mỗi khi quay lại màn hình này.
        updateCartBadge()
    }

    private fun updateCartBadge() {
        // TÁC DỤNG: Lấy số lượng sản phẩm từ API và hiển thị trên icon giỏ hàng.
        val userId = SessionManager(this).getUserId()
        if (userId == -1) {
            bottomNavigationView.getBadge(R.id.nav_cart)?.isVisible = false
            return
        }

        RetrofitClient.getInstance(this).apiService.getCart(userId.toLong())
            .enqueue(object :
                retrofit2.Callback<com.example.doan.Models.ApiResponse<com.example.doan.Models.Cart>> {
                override fun onResponse(
                    call: retrofit2.Call<com.example.doan.Models.ApiResponse<com.example.doan.Models.Cart>>,
                    response: retrofit2.Response<com.example.doan.Models.ApiResponse<com.example.doan.Models.Cart>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val itemCount = response.body()?.data?.items?.size ?: 0
                        val badge = bottomNavigationView.getOrCreateBadge(R.id.nav_cart)
                        if (itemCount > 0) {
                            badge.isVisible = true
                            badge.number = itemCount
                        } else {
                            badge.isVisible = false
                        }
                    } else {
                        bottomNavigationView.getBadge(R.id.nav_cart)?.isVisible = false
                    }
                }

                override fun onFailure(
                    call: retrofit2.Call<com.example.doan.Models.ApiResponse<com.example.doan.Models.Cart>>,
                    t: Throwable
                ) {
                    Log.e(TAG, "Error loading cart badge: ${t.message}")
                    bottomNavigationView.getBadge(R.id.nav_cart)?.isVisible = false
                }
            })
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // TÁC DỤNG: Xử lý sự kiện khi người dùng nhấn vào một mục trên BottomNavigationView.
        
        // Ngăn không cho tải lại fragment nếu người dùng nhấn vào tab đang được chọn.
        if (item.itemId == selectedItemId && item.itemId != R.id.nav_cart) {
            return false
        }

        var fragment: Fragment? = null

        when (item.itemId) {
            R.id.nav_home -> fragment = HomeFragment()
            R.id.nav_order -> fragment = MenuFragment() // YÊU CẦU: Tab "Order" giờ sẽ hiển thị trang "Thực đơn".
            R.id.nav_store -> fragment = StoreFragment()
            R.id.nav_account -> fragment = AccountFragment()
            R.id.nav_cart -> {
                // Lấy orderType từ SharedPreferences
                val prefs = getSharedPreferences("UTETeaPrefs", Context.MODE_PRIVATE)
                val orderType = prefs.getString("orderType", "pickup") ?: "pickup"
                
                Log.d("MainActivity", "Opening cart with orderType: $orderType")
                
                startActivity(Intent(this, CartActivity::class.java).apply {
                    putExtra("orderType", orderType)
                })
                return false // Chỉ mở Activity, không thay đổi fragment.
            }
        }

        fragment?.let {
            loadFragment(it, true)
            selectedItemId = item.itemId
            return true
        }
        return false
    }

    private fun loadFragment(fragment: Fragment, animate: Boolean) {
        // TÁC DỤNG: Thay thế fragment hiện tại bằng một fragment mới.
        supportFragmentManager.beginTransaction().apply {
            if (animate) {
                setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            }
            replace(R.id.content_container, fragment)
            commit()
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
