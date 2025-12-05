package com.example.doan.Activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.doan.Fragments.Manager.DashboardFragment
import com.example.doan.Fragments.Manager.ManageCategoriesFragment
import com.example.doan.Fragments.Manager.ManageDrinksFragment
import com.example.doan.Fragments.Manager.ManageOrdersFragment
import com.example.doan.Fragments.Manager.ManagerSettingsFragment
import com.example.doan.R
import com.example.doan.Utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView

class ManagerActivity : AppCompatActivity(), NavigationBarView.OnItemSelectedListener {

    private lateinit var sessionManager: SessionManager
    private var selectedItemId = R.id.nav_manager_dashboard

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manager)

        // Check if user is manager
        sessionManager = SessionManager(this)
        if (!sessionManager.isManager()) {
            Toast.makeText(this, "Bạn không có quyền truy cập!", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Setup UI
        val userNameTextView = findViewById<TextView>(R.id.manager_user_name)
        val fullName = sessionManager.getFullName()
        userNameTextView.text = if (!fullName.isNullOrEmpty()) {
            "Hi, $fullName (Manager)"
        } else {
            "Hi, Manager"
        }

        // Setup Bottom Navigation
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.manager_bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener(this)

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(DashboardFragment(), false)
        } else {
            selectedItemId = savedInstanceState.getInt("selectedItemId", R.id.nav_manager_dashboard)
        }
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
            R.id.nav_manager_categories -> ManageCategoriesFragment()
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
                setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            }
            replace(R.id.manager_content_container, fragment)
            commit()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Logout confirmation
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
                super.onBackPressed()
            }
            .show()
    }



    companion object {
        private const val TAG = "ManagerActivity"
    }
}
