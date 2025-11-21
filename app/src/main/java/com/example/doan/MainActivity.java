package com.example.doan;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(this);

        // Load the default fragment
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), false); // Don't animate the first fragment
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;
        int itemId = item.getItemId();

        if (itemId == R.id.nav_home) {
            fragment = new HomeFragment();
        } else if (itemId == R.id.nav_order) {
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

    private void loadFragment(Fragment fragment, boolean animate) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        if (animate) {
            fragmentTransaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
        }
        fragmentTransaction.replace(R.id.content_container, fragment);
        fragmentTransaction.commit();
    }
}
