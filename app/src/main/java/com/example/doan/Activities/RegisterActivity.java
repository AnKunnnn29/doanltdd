package com.example.doan.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.doan.Models.ApiResponse;
import com.example.doan.Models.RegisterRequest;
import com.example.doan.Models.RegisterResponse;
import com.example.doan.Network.RetrofitClient;
import com.example.doan.R;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private EditText usernameInput, passwordInput, confirmPasswordInput, phoneInput;
    private Button registerButton;
    private TextView loginLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        usernameInput = findViewById(R.id.input_reg_username);
        passwordInput = findViewById(R.id.input_reg_password);
        confirmPasswordInput = findViewById(R.id.input_reg_confirm_password);
        phoneInput = findViewById(R.id.input_reg_email);
        registerButton = findViewById(R.id.btn_register_submit);
        loginLink = findViewById(R.id.text_login_link);

        registerButton.setOnClickListener(v -> attemptRegister());
        loginLink.setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin bắt buộc.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Mật khẩu xác nhận không khớp.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự.", Toast.LENGTH_SHORT).show();
            return;
        }

        RegisterRequest registerRequest = new RegisterRequest(username, phone, password, username, "");

        RetrofitClient.getInstance(this).getApiService().register(registerRequest).enqueue(new Callback<ApiResponse<RegisterResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<RegisterResponse>> call, @NonNull Response<ApiResponse<RegisterResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<RegisterResponse> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Toast.makeText(RegisterActivity.this, "Đăng ký thành công! Vui lòng xác thực OTP.", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(RegisterActivity.this, OtpActivity.class);
                        intent.putExtra("USER_IDENTIFIER", username);
                        startActivity(intent);
                        finish();
                    } else {
                        String message = apiResponse.getMessage() != null ? apiResponse.getMessage() : "Đăng ký thất bại.";
                        Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Register failed: " + message);
                    }
                } else {
                    Toast.makeText(RegisterActivity.this, "Đăng ký thất bại. Lỗi Server.", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Register failed, Code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<RegisterResponse>> call, @NonNull Throwable t) {
                Toast.makeText(RegisterActivity.this, "Không thể kết nối Server để đăng ký.", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Lỗi kết nối: " + t.getMessage());
            }
        });
    }
}
