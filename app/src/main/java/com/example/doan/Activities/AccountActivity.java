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
                startActivity(new Intent(AccountActivity.this, UserProfileActivity.class));
            } else {
                Log.e(TAG, "User not logged in, redirecting to LoginActivity.");
                Toast.makeText(AccountActivity.this, "Vui lòng đăng nhập để xem hồ sơ.", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(AccountActivity.this, LoginActivity.class));
            }
        });

        settingsOption.setOnClickListener(v -> {
            startActivity(new Intent(AccountActivity.this, SettingsActivity.class));
        });

        logoutOption.setOnClickListener(v -> {
            if (isUserLoggedIn()) {
                performLogout();
            } else {
                startActivity(new Intent(AccountActivity.this, LoginActivity.class));
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
        boolean isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false);
        // VERIFY READ: Log the value being read
        Log.d(TAG, "VERIFY READ - isLoggedIn read as: " + isLoggedIn);
        return isLoggedIn;
    }

    private void updateUI() {
        TextView logoutText = (logoutOption.getChildCount() > 1 && logoutOption.getChildAt(1) instanceof TextView)
                ? (TextView) logoutOption.getChildAt(1) : null;

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
        editor.commit(); // Use commit() for synchronous save


        Toast.makeText(AccountActivity.this, "Đã đăng xuất thành công!", Toast.LENGTH_LONG).show();


        updateUI();


        Intent intent = new Intent(AccountActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
