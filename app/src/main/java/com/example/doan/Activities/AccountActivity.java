package com.example.doan.Activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.doan.R;
import com.example.doan.Network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountActivity extends AppCompatActivity {

    private static final String TAG = "AccountActivity";
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";

    private LinearLayout profileOption;
    private LinearLayout settingsOption;
    private LinearLayout logoutOption;
    private TextView profileNameText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);


        profileOption = findViewById(R.id.profile_option);
        settingsOption = findViewById(R.id.settings_option);
        logoutOption = findViewById(R.id.logout_option);


        profileNameText = findViewById(R.id.profile_name);

        updateUI();


        profileOption.setOnClickListener(v -> {

            if (isUserLoggedIn()) {

                Toast.makeText(AccountActivity.this, "Chuyển đến Hồ sơ.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(AccountActivity.this, "Vui lòng đăng nhập để xem hồ sơ.", Toast.LENGTH_SHORT).show();
            }
        });

        settingsOption.setOnClickListener(v -> {
            Toast.makeText(AccountActivity.this, "Chuyển đến Cài đặt.", Toast.LENGTH_SHORT).show();
        });

        logoutOption.setOnClickListener(v -> {
            if (isUserLoggedIn()) {
                performLogout();
            } else {
                Toast.makeText(AccountActivity.this, "Chuyển đến Đăng nhập.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateUI();
    }

    private boolean isUserLoggedIn() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    private void updateUI() {
        TextView logoutText = (logoutOption.getChildCount() > 2 && logoutOption.getChildAt(2) instanceof TextView)
                ? (TextView) logoutOption.getChildAt(2) : null;

        if (isUserLoggedIn()) {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String userName = prefs.getString(KEY_USER_NAME, "Người dùng");

            profileNameText.setText("Xin chào, " + userName);
            if (logoutText != null) {
                logoutText.setText("Đăng xuất");
            }
        } else {
            profileNameText.setText("Đăng nhập / Đăng ký");
            if (logoutText != null) {
                logoutText.setText("Đăng nhập");
            }
        }
    }


    private void performLogout() {
        // Call logout API (optional - backend doesn't have this endpoint yet)
        // Just clear local session
        handleLogoutSuccess();
    }


    private void handleLogoutSuccess() {

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.apply();


        Toast.makeText(AccountActivity.this, "Đã đăng xuất thành công!", Toast.LENGTH_LONG).show();


        updateUI();


        Intent intent = new Intent(AccountActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}