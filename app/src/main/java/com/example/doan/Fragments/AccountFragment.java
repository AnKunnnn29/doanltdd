package com.example.doan.Fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
import com.example.doan.Activities.UserProfileActivity; // Đảm bảo file này tồn tại
import com.example.doan.Activities.SettingsActivity; // Đảm bảo file này tồn tại
import com.example.doan.R;
import com.example.doan.Utils.SessionManager;

public class AccountFragment extends Fragment {

    private LinearLayout profileOption;
    private LinearLayout settingsOption;
    private LinearLayout logoutOption;
    private TextView profileNameText;
    private TextView logoutText;

    private SessionManager sessionManager;
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

        sessionManager = new SessionManager(mContext);

        profileOption = view.findViewById(R.id.profile_option);
        settingsOption = view.findViewById(R.id.settings_option);
        logoutOption = view.findViewById(R.id.logout_option);
        profileNameText = view.findViewById(R.id.profile_name);

        // Tìm TextView trong logoutOption (thường là child thứ 1 hoặc 2 tùy layout)
        // Kiểm tra layout để chắc chắn vị trí
        if (logoutOption.getChildCount() > 1 && logoutOption.getChildAt(1) instanceof TextView) {
             logoutText = (TextView) logoutOption.getChildAt(1);
        } else if (logoutOption.getChildCount() > 2 && logoutOption.getChildAt(2) instanceof TextView) {
             logoutText = (TextView) logoutOption.getChildAt(2);
        }

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

    private void updateUI() {
        if (sessionManager.isLoggedIn()) {
            String fullName = sessionManager.getFullName();
            if (fullName == null || fullName.isEmpty()) {
                fullName = sessionManager.getUsername();
            }
            profileNameText.setText("Xin chào, " + fullName);
            if (logoutText != null) logoutText.setText("Đăng xuất");
        } else {
            profileNameText.setText("Đăng nhập / Đăng ký");
            if (logoutText != null) logoutText.setText("Đăng nhập");
        }
    }

    private void handleProfileClick() {
        if (sessionManager.isLoggedIn()) {
            Intent intent = new Intent(mContext, UserProfileActivity.class);
            startActivity(intent);
        } else {
            // Khách: Chuyển đến trang đăng nhập
            startActivity(new Intent(mContext, LoginActivity.class));
        }
    }

    private void handleSettingsClick() {
        startActivity(new Intent(mContext, SettingsActivity.class));
    }

    private void handleLogoutClick() {
        if (sessionManager.isLoggedIn()) {
            sessionManager.logout();
            Toast.makeText(mContext, "Đã đăng xuất thành công!", Toast.LENGTH_SHORT).show();
            updateUI();
            // Có thể chuyển về màn hình chính hoặc login nếu cần
             startActivity(new Intent(mContext, LoginActivity.class));
        } else {
            startActivity(new Intent(mContext, LoginActivity.class));
        }
    }
}