package com.example.doan;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AccountActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        LinearLayout profileOption = findViewById(R.id.profile_option);
        LinearLayout settingsOption = findViewById(R.id.settings_option);
        LinearLayout logoutOption = findViewById(R.id.logout_option);

        profileOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(AccountActivity.this, "Profile clicked", Toast.LENGTH_SHORT).show();
            }
        });

        settingsOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(AccountActivity.this, "Settings clicked", Toast.LENGTH_SHORT).show();
            }
        });

        logoutOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(AccountActivity.this, "Logout clicked", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
