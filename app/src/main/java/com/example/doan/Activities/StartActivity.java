package com.example.doan.Activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.doan.R;
import com.google.android.material.button.MaterialButton;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acitivity_start);

        MaterialButton btnStart = findViewById(R.id.btn_start);
        
        btnStart.setOnClickListener(v -> {
            // Khi bấm Bắt đầu, chuyển sang SplashActivity
            Intent intent = new Intent(StartActivity.this, SplashActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
