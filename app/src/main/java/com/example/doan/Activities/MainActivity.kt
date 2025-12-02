package com.example.doan.Activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.doan.Fragments.AccountFragment
import com.example.doan.Fragments.HomeFragment
import com.example.doan.Fragments.OrderFragment
import com.example.doan.Fragments.StoreFragment
import com.example.doan.R
import com.example.doan.Utils.CartManager
import com.example.doan.Utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationBarView
import kotlin.math.abs

class MainActivity : AppCompatActivity(), NavigationBarView.OnItemSelectedListener {

    private var selectedItemId = R.id.nav_home
    private lateinit var fabCart: FloatingActionButton
    private lateinit var tvCartBadge: TextView
    private lateinit var fabCartContainer: View
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var userNameTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)

            val sessionManager = SessionManager(this)
            if (sessionManager.isLoggedIn() && sessionManager.isManager()) {
                Log.d(TAG, "Manager detected in MainActivity, redirecting to ManagerActivity")
                val intent = Intent(this, ManagerActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                finish()
                return
            }

            userNameTextView = findViewById(R.id.user_name)
            val fullName = sessionManager.getFullName()

            userNameTextView.text = if (sessionManager.isLoggedIn() && !fullName.isNullOrEmpty()) {
                "Hi, $fullName"
            } else {
                "Hi, Guest"
            }

            // Remove setup of Floating Action Button as it was removed from layout
            // fabCart = findViewById(R.id.fab_cart)
            // tvCartBadge = findViewById(R.id.tv_cart_badge)
            // fabCartContainer = findViewById(R.id.fab_cart_container)
            
            // setupDraggableFab()

            bottomNavigationView = findViewById(R.id.bottom_navigation)
            bottomNavigationView.setOnItemSelectedListener(this)

            if (savedInstanceState == null) {
                loadFragment(HomeFragment(), false)
                updateHeaderVisibility(true)
            } else {
                selectedItemId = savedInstanceState.getInt("selectedItemId", R.id.nav_home)
                // We need to restore the fragment state or just let the system handle it,
                // but ensuring header visibility is correct is tricky without knowing which frag is active.
                // For now, let's default based on selected ID.
                if (selectedItemId == R.id.nav_home) {
                    updateHeaderVisibility(true)
                } else {
                    updateHeaderVisibility(false)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}")
            e.printStackTrace()
        }
    }
    
    // setupDraggableFab removed

    override fun onResume() {
        super.onResume()
        updateCartBadge()
        // Re-select the item based on current state, but prevent loop if setting it triggers listener
        if (bottomNavigationView.selectedItemId != selectedItemId) {
            bottomNavigationView.selectedItemId = selectedItemId
        }
    }
    
    private fun updateCartBadge() {
        val itemCount = CartManager.getInstance().getItemCount()
        val badge = bottomNavigationView.getOrCreateBadge(R.id.nav_cart)
        if (itemCount > 0) {
            badge.isVisible = true
            badge.number = itemCount
        } else {
            badge.isVisible = false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("selectedItemId", selectedItemId)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // If clicking the same item, do nothing (or scroll to top)
        // if (item.itemId == selectedItemId && item.itemId != R.id.nav_cart) return true

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
                return false // Return false to not select the item visually
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
        if (show) {
            headerLayout.visibility = View.VISIBLE
        } else {
            headerLayout.visibility = View.GONE
        }
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
