package com.example.doan.Activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.example.doan.R;
import com.example.doan.Utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DELAY = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_splash);
            Log.d(TAG, "SplashActivity started");

            // Delay and navigate
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                navigateToNextScreen();
            }, SPLASH_DELAY);
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage());
            e.printStackTrace();
            // Fallback: go directly to WelcomeActivity
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
        }
    }

    private void navigateToNextScreen() {
        try {
            // Sử dụng SessionManager để check session
            SessionManager sessionManager = new SessionManager(this);
            boolean isLoggedIn = sessionManager.isLoggedIn();
            boolean isManager = sessionManager.isManager();
            String role = sessionManager.getRole();
            String username = sessionManager.getUsername();
            
            Log.d(TAG, "=== SESSION CHECK ===");
            Log.d(TAG, "User logged in: " + isLoggedIn);
            Log.d(TAG, "Username: " + username);
            Log.d(TAG, "Role: " + role);
            Log.d(TAG, "Is Manager: " + isManager);
            Log.d(TAG, "====================");

            Intent intent;
            if (isLoggedIn) {
                // Check role để redirect đúng
                if (isManager) {
                    // Manager → ManagerActivity
                    intent = new Intent(SplashActivity.this, ManagerActivity.class);
                    Log.d(TAG, "✓ Redirecting to ManagerActivity");
                } else {
                    // User → MainActivity
                    intent = new Intent(SplashActivity.this, MainActivity.class);
                    Log.d(TAG, "✓ Redirecting to MainActivity");
                }
            } else {
                // Not logged in → WelcomeActivity
                intent = new Intent(SplashActivity.this, WelcomeActivity.class);
                Log.d(TAG, "✓ Redirecting to WelcomeActivity");
            }

            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
            
        } catch (Exception e) {
            Log.e(TAG, "Error navigating: " + e.getMessage());
            e.printStackTrace();
            // Fallback
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
        }
    }
}
