package com.example.doan.Activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_main)

            val sessionManager = SessionManager(this)

            // Manager redirection
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
                // First time creation, load home fragment
                loadFragment(HomeFragment(), false)
            }
            
            handleIntent(intent)

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("selectedItemId", selectedItemId)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        selectedItemId = savedInstanceState.getInt("selectedItemId", R.id.nav_home)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val navToItem = intent?.getIntExtra("SELECTED_ITEM", -1)
        if (navToItem != null && navToItem != -1) {
            bottomNavigationView.selectedItemId = navToItem
        }
    }

    override fun onResume() {
        super.onResume()
        updateCartBadge()
    }

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
        // Prevent reloading the same fragment
        if (item.itemId == selectedItemId && item.itemId != R.id.nav_cart) {
            return false
        }

        var fragment: Fragment? = null

        when (item.itemId) {
            R.id.nav_home -> fragment = HomeFragment()
            R.id.nav_order -> fragment = OrderFragment()
            R.id.nav_store -> fragment = StoreFragment()
            R.id.nav_account -> fragment = AccountFragment()
            R.id.nav_cart -> {
                startActivity(Intent(this, CartActivity::class.java))
                return false // Do not change fragment, just launch the activity
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
