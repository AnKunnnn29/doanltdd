package com.example.doan.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.doan.R;
import com.google.android.material.button.MaterialButton;

public class WelcomeActivity extends AppCompatActivity {

    private static final String TAG = "WelcomeActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_welcome);
            Log.d(TAG, "WelcomeActivity started");

            MaterialButton loginButton = findViewById(R.id.btn_welcome_login);
        MaterialButton registerButton = findViewById(R.id.btn_welcome_register);
        TextView guestMode = findViewById(R.id.text_guest_mode);

            // Login button
            loginButton.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                } catch (Exception e) {
                    Log.e(TAG, "Error starting LoginActivity: " + e.getMessage());
                }
            });

            // Register button
            registerButton.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(WelcomeActivity.this, RegisterActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                } catch (Exception e) {
                    Log.e(TAG, "Error starting RegisterActivity: " + e.getMessage());
                }
            });

            // Guest mode - go directly to MainActivity
            guestMode.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    finish();
                } catch (Exception e) {
                    Log.e(TAG, "Error starting MainActivity: " + e.getMessage());
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
