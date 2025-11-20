package com.example.doan;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        NestedScrollView nestedScrollView = findViewById(R.id.content_container);
        MaterialCardView bannerCard = findViewById(R.id.banner_card);

        nestedScrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            // Fade out the banner as it scrolls up.
            float maxScroll = bannerCard.getTop();
            if (maxScroll > 0) { 
                if (scrollY < maxScroll) {
                    float alpha = 1.0f - ((float) scrollY / maxScroll);
                    bannerCard.setAlpha(alpha);
                } else {
                    bannerCard.setAlpha(0.0f);
                }
            }
        });

        // Set listener for bottom navigation item selection
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                String message = "";
                // Using if-else because the project might not be configured for view binding's R.id syntax in switch
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    message = "Home";
                } else if (itemId == R.id.nav_order) {
                    message = "Order";
                } else if (itemId == R.id.nav_qr) {
                    message = "QR Code";
                } else if (itemId == R.id.nav_store) {
                    message = "Store";
                } else if (itemId == R.id.nav_account) {
                    message = "Account";
                }

                if (!message.isEmpty()) {
                    Toast.makeText(MainActivity.this, message + " selected", Toast.LENGTH_SHORT).show();
                }
                return true; // Return true to display the item as the selected item
            }
        });
    }
}
