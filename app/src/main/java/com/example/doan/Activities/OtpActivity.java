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
import com.example.doan.Models.VerifyOtpRequest;
import com.example.doan.Network.RetrofitClient;
import com.example.doan.R;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OtpActivity extends AppCompatActivity {

    private static final String TAG = "OtpActivity";

    private EditText otpInput;
    private Button verifyButton;
    private TextView resendOtpText;

    private String username, password, email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);

        otpInput = findViewById(R.id.input_otp);
        verifyButton = findViewById(R.id.btn_verify_otp);
        resendOtpText = findViewById(R.id.text_resend_otp);

        // Nhận dữ liệu từ RegisterActivity
        Intent intent = getIntent();
        if (intent.hasExtra("USERNAME") && intent.hasExtra("PASSWORD") && intent.hasExtra("EMAIL")) {
            username = intent.getStringExtra("USERNAME");
            password = intent.getStringExtra("PASSWORD");
            email = intent.getStringExtra("EMAIL");

            // Gọi API đăng ký để server gửi OTP
            requestRegistrationAndOtp();
        } else {
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin đăng ký.", Toast.LENGTH_LONG).show();
            finish(); // Quay lại trang trước
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

        resendOtpText.setOnClickListener(v -> resendOtp());
    }

    private void requestRegistrationAndOtp() {
        Toast.makeText(this, "Đang gửi yêu cầu đăng ký...", Toast.LENGTH_SHORT).show();
        RegisterRequest registerRequest = new RegisterRequest(username, email, password, username, "");

        RetrofitClient.getInstance(this).getApiService().registerWithOtp(registerRequest)
                .enqueue(new Callback<ApiResponse<String>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<String>> call, @NonNull Response<ApiResponse<String>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            Toast.makeText(OtpActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                        } else {
                            String errorMessage = "Lỗi khi gửi OTP.";
                            try {
                                if (response.body() != null) {
                                    errorMessage = response.body().getMessage();
                                } else if (response.errorBody() != null) {
                                    errorMessage = new JSONObject(response.errorBody().string()).getString("message");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing error response in requestRegistrationAndOtp", e);
                            }
                            Toast.makeText(OtpActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            // Cân nhắc finish() để quay về trang đăng ký nếu có lỗi
                            // finish();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<String>> call, @NonNull Throwable t) {
                        Log.e(TAG, "Lỗi mạng khi đăng ký: " + t.getMessage());
//                        Toast.makeText(OtpActivity.this, "Lỗi mạng, không thể kết nối server.", Toast.LENGTH_SHORT).show();
                        // Cân nhắc finish() để quay về trang đăng ký nếu có lỗi
                        // finish();
                    }
                });
    }

    private void verifyOtp(String otp) {
        VerifyOtpRequest request = new VerifyOtpRequest(email, otp);

        RetrofitClient.getInstance(this).getApiService().verifyOtp(request)
                .enqueue(new Callback<ApiResponse<String>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<String>> call, @NonNull Response<ApiResponse<String>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            Toast.makeText(OtpActivity.this, "Kích hoạt thành công! Vui lòng đăng nhập.", Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(OtpActivity.this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            String errorMessage = "Lỗi xác thực.";
                            try {
                                if (response.errorBody() != null) {
                                    String errorBodyStr = response.errorBody().string();
                                    JSONObject errorObj = new JSONObject(errorBodyStr);
                                    if (errorObj.has("message")) {
                                        errorMessage = errorObj.getString("message");
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing error body in verifyOtp", e);
                            }
                            Toast.makeText(OtpActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<String>> call, @NonNull Throwable t) {
                        Log.e(TAG, "Lỗi Retrofit khi xác thực OTP: " + t.getMessage());
                        Toast.makeText(OtpActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void resendOtp() {
        RetrofitClient.getInstance(this).getApiService().resendOtp(email)
                .enqueue(new Callback<ApiResponse<String>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<String>> call, @NonNull Response<ApiResponse<String>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            Toast.makeText(OtpActivity.this, "OTP đã được gửi lại!", Toast.LENGTH_SHORT).show();
                        } else {
                            String errorMessage = "Lỗi gửi lại OTP.";
                            if(response.body() != null) {
                                errorMessage = response.body().getMessage();
                            } else if (response.errorBody() != null) {
                                try {
                                    errorMessage = new JSONObject(response.errorBody().string()).getString("message");
                                } catch(Exception e) { /* Do nothing */ }
                            }
                            Toast.makeText(OtpActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<String>> call, @NonNull Throwable t) {
                        Log.e(TAG, "Network Error on resend: " + t.getMessage());
                        Toast.makeText(OtpActivity.this, "Không thể kết nối đến server.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
