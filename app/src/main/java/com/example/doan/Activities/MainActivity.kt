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
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sessionManager = SessionManager(this)
        if (sessionManager.isLoggedIn() && sessionManager.isManager()) {
            Log.d(TAG, "Manager detected, redirecting to ManagerActivity")
            startActivity(Intent(this, ManagerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            finish()
            return
        }

        setupViews(sessionManager)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun setupViews(sessionManager: SessionManager) {
        val userNameTextView = findViewById<TextView>(R.id.user_name)
        userNameTextView.text = if (sessionManager.isLoggedIn() && !sessionManager.getFullName().isNullOrEmpty()) {
            "Hi, ${sessionManager.getFullName()}"
        } else {
            "Hi, Guest"
        }

        findViewById<ImageView>(R.id.profile_image).setOnClickListener {
            startActivity(Intent(this, AccountActivity::class.java))
        }

        fabCart = findViewById(R.id.fab_cart)
        tvCartBadge = findViewById(R.id.tv_cart_badge)
        fabCart.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener(this)
    }

    private fun handleIntent(intent: Intent?) {
        val selectedItem = intent?.getIntExtra("SELECTED_ITEM", R.id.nav_home) ?: R.id.nav_home
        if (bottomNavigationView.selectedItemId != selectedItem) {
            bottomNavigationView.selectedItemId = selectedItem
            loadFragmentForItem(selectedItem)
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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.itemId == selectedItemId) return false // Do not reload if the same item is selected
        
        if (item.itemId == R.id.nav_account) {
            startActivity(Intent(this, AccountActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            })
            return false // Prevent selection change in this activity
        }

        loadFragmentForItem(item.itemId)
        return true
    }

    private fun loadFragmentForItem(itemId: Int) {
        selectedItemId = itemId
        val fragment: Fragment? = when (itemId) {
            R.id.nav_home -> HomeFragment()
            R.id.nav_order -> OrderFragment()
            R.id.nav_store -> StoreFragment()
            else -> null
        }

        fragment?.let { loadFragment(it, true) }
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
