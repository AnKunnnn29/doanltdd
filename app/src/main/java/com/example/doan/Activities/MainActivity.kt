package com.example.doan.Activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.doan.Fragments.AccountFragment
import com.example.doan.Fragments.HomeFragment
import com.example.doan.Fragments.OrderFragment
import com.example.doan.Fragments.StoreFragment
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView

class MainActivity : AppCompatActivity(), NavigationBarView.OnItemSelectedListener {

    private var selectedItemId = R.id.nav_home
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var userNameTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_main)

            val sessionManager = SessionManager(this)

            // Manager redirection
            if (sessionManager.isLoggedIn() && sessionManager.isManager()) {
                Log.d(TAG, "Manager detected in MainActivity, redirecting to ManagerActivity")
                val intent = Intent(this, ManagerActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                finish()
                return
            }

            // Username
            userNameTextView = findViewById(R.id.user_name)
            val fullName = sessionManager.getFullName()

            userNameTextView.text = if (sessionManager.isLoggedIn() && !fullName.isNullOrEmpty()) {
                "Hi, $fullName"
            } else {
                "Hi, Guest"
            }

            // --- Restore LEFT feature: profile_image click ---
            val profileImage = findViewById<ImageView>(R.id.profile_image)
            profileImage?.setOnClickListener {
                startActivity(Intent(this, AccountActivity::class.java))
            }

            // Bottom Navigation
            bottomNavigationView = findViewById(R.id.bottom_navigation)
            bottomNavigationView.setOnItemSelectedListener(this)

            // First load
            if (savedInstanceState == null) {
                loadFragment(HomeFragment(), false)
                updateHeaderVisibility(true)
            } else {
                selectedItemId = savedInstanceState.getInt("selectedItemId", R.id.nav_home)
                updateHeaderVisibility(selectedItemId == R.id.nav_home)
            }

            // Handle intent from notifications or other screens
            handleIntent(intent)

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}")
            e.printStackTrace()
        }
    }

    // Restore LEFT feature: handle SELECTED_ITEM
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val selectedItem = intent?.getIntExtra("SELECTED_ITEM", selectedItemId) ?: selectedItemId
        if (bottomNavigationView.selectedItemId != selectedItem) {
            bottomNavigationView.selectedItemId = selectedItem
        }
    }

    override fun onResume() {
        super.onResume()
        updateCartBadge()

        if (bottomNavigationView.selectedItemId != selectedItemId) {
            bottomNavigationView.selectedItemId = selectedItemId
        }
    }

    // Cart badge from API
    private fun updateCartBadge() {
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

        var fragment: Fragment? = null
        var showHeader = false

        when (item.itemId) {
            R.id.nav_home -> {
                fragment = HomeFragment()
                showHeader = true
                selectedItemId = item.itemId
            }
            R.id.nav_order -> {
                fragment = OrderFragment()
                selectedItemId = item.itemId
            }
            R.id.nav_cart -> {
                startActivity(Intent(this, CartActivity::class.java))
                return false
            }
            R.id.nav_store -> {
                fragment = StoreFragment()
                selectedItemId = item.itemId
            }
            R.id.nav_account -> {
                fragment = AccountFragment()
                selectedItemId = item.itemId
            }
        }

        updateHeaderVisibility(showHeader)

        fragment?.let {
            loadFragment(it, true)
            return true
        }
        return false
    }

    private fun updateHeaderVisibility(show: Boolean) {
        val headerLayout = findViewById<View>(R.id.app_bar)
        headerLayout?.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun loadFragment(fragment: Fragment, animate: Boolean) {
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
