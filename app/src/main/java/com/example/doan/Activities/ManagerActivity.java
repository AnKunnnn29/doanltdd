package com.example.doan.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.doan.Fragments.Manager.DashboardFragment;
import com.example.doan.Fragments.Manager.ManageCategoriesFragment;
import com.example.doan.Fragments.Manager.ManageDrinksFragment;
import com.example.doan.Fragments.Manager.ManageOrdersFragment;
import com.example.doan.Fragments.Manager.ManagerSettingsFragment;
import com.example.doan.R;
import com.example.doan.Utils.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class ManagerActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {

    private static final String TAG = "ManagerActivity";
    private SessionManager sessionManager;
    private int selectedItemId = R.id.nav_manager_dashboard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager);

        // Check if user is manager
        sessionManager = new SessionManager(this);
        if (!sessionManager.isManager()) {
            Toast.makeText(this, "Bạn không có quyền truy cập!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Setup UI
        TextView userNameTextView = findViewById(R.id.manager_user_name);
        String fullName = sessionManager.getFullName();
        if (fullName != null && !fullName.isEmpty()) {
            userNameTextView.setText("Hi, " + fullName + " (Manager)");
        } else {
            userNameTextView.setText("Hi, Manager");
        }

        // Setup Bottom Navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.manager_bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(this);

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new DashboardFragment(), false);
        } else {
            selectedItemId = savedInstanceState.getInt("selectedItemId", R.id.nav_manager_dashboard);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("selectedItemId", selectedItemId);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;
        int itemId = item.getItemId();

        selectedItemId = itemId;

        if (itemId == R.id.nav_manager_dashboard) {
            fragment = new DashboardFragment();
        } else if (itemId == R.id.nav_manager_drinks) {
            fragment = new ManageDrinksFragment();
        } else if (itemId == R.id.nav_manager_orders) {
            fragment = new ManageOrdersFragment();
        } else if (itemId == R.id.nav_manager_categories) {
            fragment = new ManageCategoriesFragment();
        } else if (itemId == R.id.nav_manager_settings) {
            fragment = new ManagerSettingsFragment();
        }

        if (fragment != null) {
            loadFragment(fragment, true);
            return true;
        }

        return false;
    }

    private static final int CONTENT_CONTAINER_ID = R.id.manager_content_container;

    private void loadFragment(Fragment fragment, boolean animate) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        if (animate) {
            fragmentTransaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
        }

        fragmentTransaction.replace(CONTENT_CONTAINER_ID, fragment);
        fragmentTransaction.commit();
    }

    @Override
    public void onBackPressed() {
        // Logout confirmation
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có muốn đăng xuất?")
                .setPositiveButton("Có", (dialog, which) -> {
                    sessionManager.logout();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .setNegativeButton("Không", null)
                .show();
    }
}
