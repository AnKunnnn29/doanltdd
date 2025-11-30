package com.example.doan.Fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.doan.Activities.LoginActivity;
import com.example.doan.Activities.SettingsActivity;
import com.example.doan.Activities.UserProfileActivity;
import com.example.doan.R;

public class AccountFragment extends Fragment {

    private static final String TAG = "AccountFragment";
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";

    private LinearLayout profileOption;
    private LinearLayout settingsOption;
    private LinearLayout logoutOption;
    private TextView profileNameText;
    private TextView logoutText;

    private Context mContext;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_account, container, false);

        profileOption = view.findViewById(R.id.profile_option);
        settingsOption = view.findViewById(R.id.settings_option);
        logoutOption = view.findViewById(R.id.logout_option);
        profileNameText = view.findViewById(R.id.profile_name);
        logoutText = view.findViewById(R.id.logout_text);

        profileOption.setOnClickListener(v -> handleProfileClick());
        settingsOption.setOnClickListener(v -> handleSettingsClick());
        logoutOption.setOnClickListener(v -> handleLogoutClick());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    private boolean isUserLoggedIn() {
        if (mContext == null) return false;
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false);
        Log.d(TAG, "VERIFY READ - isLoggedIn in Fragment read as: " + isLoggedIn);
        return isLoggedIn;
    }

    private void updateUI() {
        if (!isAdded() || mContext == null) return; // Ensure fragment is attached

        if (isUserLoggedIn()) {
            SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String userName = prefs.getString(KEY_USER_NAME, "Người dùng");
            profileNameText.setText("Xin chào, " + userName);
            if(logoutText != null) logoutText.setText("Đăng xuất");
        } else {
            profileNameText.setText("Đăng nhập / Đăng ký");
            if(logoutText != null) logoutText.setText("Đăng nhập");
        }
    }

    private void handleProfileClick() {
        if (isUserLoggedIn()) {
            startActivity(new Intent(mContext, UserProfileActivity.class));
        } else {
            Toast.makeText(mContext, "Vui lòng đăng nhập để xem hồ sơ.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(mContext, LoginActivity.class));
        }
    }

    private void handleSettingsClick() {
        startActivity(new Intent(mContext, SettingsActivity.class));
    }

    private void handleLogoutClick() {
        if (isUserLoggedIn()) {
            performLogout();
        } else {
            startActivity(new Intent(mContext, LoginActivity.class));
        }
    }

    private void performLogout() {
        if (mContext == null) return;
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.commit(); // Use commit for synchronous save in critical operations

        Toast.makeText(mContext, "Đã đăng xuất thành công!", Toast.LENGTH_LONG).show();
        updateUI();
    }
}
