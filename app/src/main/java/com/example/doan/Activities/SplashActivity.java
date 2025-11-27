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

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
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
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            boolean isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false);
            
            Log.d(TAG, "User logged in: " + isLoggedIn);

            Intent intent;
            if (isLoggedIn) {
                // User is logged in, go to MainActivity
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                // User is not logged in, go to WelcomeActivity
                intent = new Intent(SplashActivity.this, WelcomeActivity.class);
            }

            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
            
        } catch (Exception e) {
            Log.e(TAG, "Error navigating: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
