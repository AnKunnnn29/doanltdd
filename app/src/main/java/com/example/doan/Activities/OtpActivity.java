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
import com.example.doan.Models.VerifyOtpRequest;
import com.example.doan.Network.RetrofitClient;
import com.example.doan.R;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OtpActivity extends AppCompatActivity {

    private static final String TAG = "OtpActivity";

    private EditText otpInput;
    private Button verifyButton;
    private TextView resendOtpText;
    private String userIdentifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);

        otpInput = findViewById(R.id.input_otp);
        verifyButton = findViewById(R.id.btn_verify_otp);
        resendOtpText = findViewById(R.id.text_resend_otp);

        if (getIntent().hasExtra("USER_IDENTIFIER")) {
            userIdentifier = getIntent().getStringExtra("USER_IDENTIFIER");
        } else {
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin người dùng.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        verifyButton.setOnClickListener(v -> {
            String otp = otpInput.getText().toString().trim();
            if (otp.length() != 6) {
                Toast.makeText(this, "Mã OTP phải có 6 chữ số.", Toast.LENGTH_SHORT).show();
                return;
            }
            verifyOtp(otp);
        });

        resendOtpText.setOnClickListener(v -> {
            // TODO: Implement API to resend OTP
            Toast.makeText(this, "Đã gửi lại mã OTP.", Toast.LENGTH_SHORT).show();
        });
    }

    private void verifyOtp(String otp) {
        VerifyOtpRequest request = new VerifyOtpRequest(userIdentifier, otp);

        RetrofitClient.getInstance(this).getApiService().verifyOtp(request).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Void> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Toast.makeText(OtpActivity.this, "Xác thực thành công! Vui lòng đăng nhập.", Toast.LENGTH_LONG).show();

                        Intent intent = new Intent(OtpActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        String message = apiResponse.getMessage() != null ? apiResponse.getMessage() : "Mã OTP không hợp lệ.";
                        Toast.makeText(OtpActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(OtpActivity.this, "Xác thực thất bại. Lỗi Server: " + response.code(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "OTP verification failed, Code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                Log.e(TAG, "Connection error: " + t.getMessage());
                Toast.makeText(OtpActivity.this, "Không thể kết nối đến server.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
