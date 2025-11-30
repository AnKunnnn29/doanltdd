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
import androidx.appcompat.app.AppCompatActivity;

import com.example.doan.Models.ApiResponse;
import com.example.doan.Models.LoginRequest;
import com.example.doan.Models.LoginResponse;
import com.example.doan.Network.ApiService;
import com.example.doan.R;
import com.example.doan.Network.RetrofitClient;
import com.example.doan.Utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_AUTH_TOKEN = "authToken";

    private EditText usernameInput;
    private EditText passwordInput;
    private Button loginButton;
    private TextView registerLink;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        apiService = RetrofitClient.getInstance(this).getApiService();
        usernameInput = findViewById(R.id.input_login_username);
        passwordInput = findViewById(R.id.input_login_password);
        loginButton = findViewById(R.id.btn_login);
        registerLink = findViewById(R.id.text_register_link);

        loginButton.setOnClickListener(v -> attemptLogin());

        registerLink.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void attemptLogin() {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên đăng nhập và mật khẩu.", Toast.LENGTH_SHORT).show();
            return;
        }

        LoginRequest loginRequest = new LoginRequest(username, password);

        apiService.login(loginRequest).enqueue(new Callback<ApiResponse<LoginResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<LoginResponse>> call, @NonNull Response<ApiResponse<LoginResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    handleLoginSuccess(response.body().getData());
                } else {
                    String message = (response.body() != null) ? response.body().getMessage() : "Sai tên đăng nhập hoặc mật khẩu.";
                    Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Login failed: " + message);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<LoginResponse>> call, @NonNull Throwable t) {
                Toast.makeText(LoginActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Connection failure: ", t);
            }
        });
    }

    private void handleLoginSuccess(LoginResponse loginResponse) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, loginResponse.getUserId());
        editor.putString(KEY_USER_NAME, loginResponse.getFullName());
        editor.putString(KEY_AUTH_TOKEN, loginResponse.getToken());
        editor.commit();

        // VERIFY WRITE: Log the value immediately after saving
        boolean isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false);
        Log.d(TAG, "VERIFY WRITE - isLoggedIn saved as: " + isLoggedIn);

        SessionManager sessionManager = new SessionManager(this);
        sessionManager.saveLoginSession(
                loginResponse.getUserId(),
                loginResponse.getUsername(),
                loginResponse.getFullName(),
                loginResponse.getPhone(),
                loginResponse.getRole(),
                loginResponse.getMemberTier(),
                loginResponse.getToken()
        );

        Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
