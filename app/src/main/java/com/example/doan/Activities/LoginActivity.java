package com.example.doan.Activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


import com.example.doan.Models.ApiResponse;
import com.example.doan.Models.LoginRequest;
import com.example.doan.Models.LoginResponse;
import com.example.doan.R;
import com.example.doan.Network.RetrofitClient;
// Imports cho các Activity cần chuyển hướng
import com.example.doan.Activities.MainActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_IS_ADMIN = "isAdmin";

    private EditText usernameInput;
    private EditText passwordInput;
    private Button loginButton;
    private TextView registerLink; // TextView dùng làm liên kết Đăng ký

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 1. Ánh xạ View (ĐÃ ĐỒNG BỘ 100% VỚI XML)
        usernameInput = findViewById(R.id.input_login_username);
        passwordInput = findViewById(R.id.input_login_password);
        loginButton = findViewById(R.id.btn_login);
        registerLink = findViewById(R.id.text_register_link);

        // 2. Xử lý sự kiện Đăng nhập
        loginButton.setOnClickListener(v -> attemptLogin());

        // 3. Xử lý sự kiện Đăng ký (Chuyển Activity)
        registerLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }

    private void attemptLogin() {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        // 1. Kiểm tra Validation cơ bản
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên đăng nhập và mật khẩu.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Tạo Request Body
        LoginRequest loginRequest = new LoginRequest(username, password);

        // 3. Gọi API Đăng nhập
        RetrofitClient.getInstance(this).getApiService().login(loginRequest).enqueue(new Callback<ApiResponse<LoginResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<LoginResponse>> call, @NonNull Response<ApiResponse<LoginResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<LoginResponse> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        handleLoginSuccess(apiResponse.getData());
                    } else {
                        String message = apiResponse.getMessage() != null ? apiResponse.getMessage() : "Sai tên đăng nhập hoặc mật khẩu.";
                        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Login failed: " + message);
                    }
                } else {
                    String message = "Sai tên đăng nhập hoặc mật khẩu. Vui lòng thử lại.";
                    if (response.code() == 401) { // 401 Unauthorized
                        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(LoginActivity.this, "Lỗi Server: " + response.code(), Toast.LENGTH_LONG).show();
                    }
                    Log.e(TAG, "Login failed, Code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<LoginResponse>> call, @NonNull Throwable t) {
                Toast.makeText(LoginActivity.this, "Không thể kết nối Server: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Connection failure: " + t.getMessage());
            }
        });
    }

    private void handleLoginSuccess(LoginResponse response) {
        // 1. Lưu Session sử dụng SessionManager
        com.example.doan.Utils.SessionManager sessionManager = new com.example.doan.Utils.SessionManager(this);
        sessionManager.saveLoginSession(
            response.getUserId(),
            response.getUsername(),
            response.getFullName(),
            response.getPhone(),
            response.getRole(),
            response.getMemberTier(),
            response.getToken()
        );

        // 2. Lưu thêm vào SharedPreferences cũ (để tương thích với code cũ)
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_USER_ID, response.getUserId());
        editor.putString(KEY_USER_NAME, response.getUsername());
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putBoolean(KEY_IS_ADMIN, response.isManager());
        editor.apply();

        Toast.makeText(this, "Xin chào, " + response.getFullName(), Toast.LENGTH_SHORT).show();

        // 3. Chuyển hướng về MainActivity
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }
}