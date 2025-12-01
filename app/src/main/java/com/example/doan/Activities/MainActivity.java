package com.example.doan.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.doan.Fragments.AccountFragment;
import com.example.doan.Fragments.HomeFragment;
import com.example.doan.Fragments.OrderFragment;
import com.example.doan.R;
import com.example.doan.Fragments.StoreFragment;
import com.example.doan.Utils.CartManager;
import com.example.doan.Utils.SessionManager;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {

    private final String TAG = "MainActivity";
    private int selectedItemId = R.id.nav_home;
    private FloatingActionButton fabCart;
    private TextView tvCartBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check if user is Manager, redirect to ManagerActivity
        SessionManager sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn() && sessionManager.isManager()) {
            Log.d(TAG, "Manager detected in MainActivity, redirecting to ManagerActivity");
            Intent intent = new Intent(this, ManagerActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

        TextView userNameTextView = findViewById(R.id.user_name);
        String fullName = sessionManager.getFullName();

        if(sessionManager.isLoggedIn() && fullName != null && !fullName.isEmpty()){
            userNameTextView.setText("Hi, " + fullName);
        } else {
            userNameTextView.setText("Hi, Guest");
        }

        // Setup Cart FAB
        fabCart = findViewById(R.id.fab_cart);
        tvCartBadge = findViewById(R.id.tv_cart_badge);
        
        fabCart.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CartActivity.class);
            startActivity(intent);
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(this);

        // Load the default fragment (HomeFragment)
        if (savedInstanceState == null) {
            // Không cần animate lần đầu
            loadFragment(new HomeFragment(), false);
        } else {
            // Lấy lại ID đang chọn khi Activity được khôi phục (xoay màn hình, v.v.)
            selectedItemId = savedInstanceState.getInt("selectedItemId", R.id.nav_home);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateCartBadge();
    }
    
    private void updateCartBadge() {
        int itemCount = CartManager.getInstance().getItemCount();
        if (itemCount > 0) {
            tvCartBadge.setVisibility(View.VISIBLE);
            tvCartBadge.setText(String.valueOf(itemCount));
        } else {
            tvCartBadge.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Lưu lại trạng thái của mục đã chọn
        outState.putInt("selectedItemId", selectedItemId);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;
        int itemId = item.getItemId();


        selectedItemId = itemId;

        if (itemId == R.id.nav_home) {

            fragment = new HomeFragment();
        } else if (itemId == R.id.nav_order) {
            // Nếu chưa Login, có thể chuyển hướng đến LoginFragment thay vì OrderFragment
            fragment = new OrderFragment();
        } else if (itemId == R.id.nav_store) {
            fragment = new StoreFragment();
        } else if (itemId == R.id.nav_account) {
            fragment = new AccountFragment();
        }

        if (fragment != null) {
            loadFragment(fragment, true);
            return true;
        }

        return false;
    }


    private static final int CONTENT_CONTAINER_ID = R.id.content_container;

    private void loadFragment(Fragment fragment, boolean animate) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        if (animate) {

            fragmentTransaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
        }


        fragmentTransaction.replace(CONTENT_CONTAINER_ID, fragment);

        fragmentTransaction.commit();
    }
}