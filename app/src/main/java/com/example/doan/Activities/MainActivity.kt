package com.example.doan.Activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
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

class MainActivity : AppCompatActivity(), NavigationBarView.OnItemSelectedListener {

    private var selectedItemId = R.id.nav_home
    private lateinit var fabCart: FloatingActionButton
    private lateinit var tvCartBadge: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check if user is Manager, redirect to ManagerActivity
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

        val userNameTextView = findViewById<TextView>(R.id.user_name)
        val fullName = sessionManager.getFullName()

        userNameTextView.text = if (sessionManager.isLoggedIn() && !fullName.isNullOrEmpty()) {
            "Hi, $fullName"
        } else {
            "Hi, Guest"
        }

        // Setup Cart FAB
        fabCart = findViewById(R.id.fab_cart)
        tvCartBadge = findViewById(R.id.tv_cart_badge)
        
        fabCart.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener(this)

        // Load the default fragment (HomeFragment)
        if (savedInstanceState == null) {
            loadFragment(HomeFragment(), false)
        } else {
            selectedItemId = savedInstanceState.getInt("selectedItemId", R.id.nav_home)
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateCartBadge()
    }
    
    private fun updateCartBadge() {
        val itemCount = CartManager.getInstance().getItemCount()
        if (itemCount > 0) {
            tvCartBadge.visibility = View.VISIBLE
            tvCartBadge.text = itemCount.toString()
        } else {
            tvCartBadge.visibility = View.GONE
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("selectedItemId", selectedItemId)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        selectedItemId = item.itemId

        val fragment: Fragment? = when (item.itemId) {
            R.id.nav_home -> HomeFragment()
            R.id.nav_order -> OrderFragment()
            R.id.nav_store -> StoreFragment()
            R.id.nav_account -> AccountFragment()
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
