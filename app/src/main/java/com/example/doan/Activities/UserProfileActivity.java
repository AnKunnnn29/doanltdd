package com.example.doan.Activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.example.doan.R;

public class UserProfileActivity extends AppCompatActivity {

    // ID mô tả được sử dụng:
    private EditText nameInput, emailInput;
    private Button saveButton;
    private int currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);


        nameInput = findViewById(R.id.input_profile_name);
        emailInput = findViewById(R.id.input_profile_email);
        saveButton = findViewById(R.id.btn_save_profile);

        currentUserId = getLoggedInUserId();

        if (currentUserId != -1) {
            loadUserProfile(currentUserId);
            saveButton.setOnClickListener(v -> saveProfileChanges());
        } else {
            Toast.makeText(this, "Vui lòng đăng nhập.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private int getLoggedInUserId() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        return prefs.getInt("userId", -1);
    }

    private void loadUserProfile(int userId) {

        Toast.makeText(this, "Tải hồ sơ người dùng...", Toast.LENGTH_SHORT).show();
    }

    private void saveProfileChanges() {
        String newName = nameInput.getText().toString().trim();
        String newEmail = emailInput.getText().toString().trim();

        Toast.makeText(this, "Lưu thay đổi hồ sơ thành công (Chưa có API Backend)", Toast.LENGTH_SHORT).show();
    }
}