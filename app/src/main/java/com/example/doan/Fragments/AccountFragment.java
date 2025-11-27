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
// Cần đảm bảo các file này tồn tại
// import com.example.doan.Activities.UserProfileActivity;
// import com.example.doan.Activities.SettingsActivity;
import com.example.doan.R;
import com.example.doan.Network.RetrofitClient; // Sử dụng tên lớp RetrofitClient

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountFragment extends Fragment {

    private static final String TAG = "AccountFragment";
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";

    private LinearLayout profileOption;
    private LinearLayout settingsOption;
    private LinearLayout logoutOption;
    private TextView profileNameText; // TextView hiển thị trạng thái/tên người dùng
    private TextView logoutText; // Để truy cập trực tiếp text Đăng xuất/Đăng nhập

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


        if (logoutOption.getChildCount() > 2 && logoutOption.getChildAt(2) instanceof TextView) {
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


    private boolean isUserLoggedIn() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    private void updateUI() {
        if (isUserLoggedIn()) {
            SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
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


    private void handleProfileClick() {
        if (isUserLoggedIn()) {
            Toast.makeText(mContext, "Chuyển đến Hồ sơ (Profile)", Toast.LENGTH_SHORT).show();

        } else {
            startActivity(new Intent(mContext, LoginActivity.class));
        }
    }

    private void handleSettingsClick() {
        Toast.makeText(mContext, "Chuyển đến Cài đặt (Settings)", Toast.LENGTH_SHORT).show();

    }

    private void handleLogoutClick() {
        if (isUserLoggedIn()) {
            performLogout();
        } else {
            startActivity(new Intent(mContext, LoginActivity.class));
        }
    }


    private void performLogout() {
        // Đăng xuất local, không cần gọi API
        handleLogoutSuccess();
    }

    private void handleLogoutSuccess() {
        // Xóa session bằng SessionManager
        com.example.doan.Utils.SessionManager sessionManager = new com.example.doan.Utils.SessionManager(mContext);
        sessionManager.logout();
        
        // Xóa SharedPreferences cũ
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.apply();

        Toast.makeText(mContext, "Đã đăng xuất thành công!", Toast.LENGTH_LONG).show();
        updateUI();
    }
}