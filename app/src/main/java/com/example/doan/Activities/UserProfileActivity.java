package com.example.doan.Activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.doan.Models.ApiResponse;
import com.example.doan.Models.UpdateProfileRequest;
import com.example.doan.Models.UserProfileDto;
import com.example.doan.Network.ApiService;
import com.example.doan.Network.RetrofitClient;
import com.example.doan.R;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserProfileActivity extends AppCompatActivity {

    private static final String TAG = "UserProfileActivity";
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_AUTH_TOKEN = "authToken";

    private EditText nameInput, emailInput, phoneInput, addressInput;
    private Button saveButton;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        apiService = RetrofitClient.getInstance(this).getApiService();

        nameInput = findViewById(R.id.input_profile_name);
        emailInput = findViewById(R.id.input_profile_email);
        phoneInput = findViewById(R.id.input_profile_phone);
        addressInput = findViewById(R.id.input_profile_address);
        saveButton = findViewById(R.id.btn_save_profile);

        loadUserProfile();

        saveButton.setOnClickListener(v -> saveProfileChanges());
    }

    private String getAuthToken() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_AUTH_TOKEN, null);
    }

    private void loadUserProfile() {
        String authToken = getAuthToken();
        if (authToken == null) {
            Toast.makeText(this, "Vui lòng đăng nhập.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Call<ApiResponse<UserProfileDto>> call = apiService.getProfile("Bearer " + authToken);
        call.enqueue(new Callback<ApiResponse<UserProfileDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserProfileDto>> call, Response<ApiResponse<UserProfileDto>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    UserProfileDto profile = response.body().getData();
                    nameInput.setText(profile.getFullName());
                    emailInput.setText(profile.getEmail());
                    phoneInput.setText(profile.getPhone());
                    addressInput.setText(profile.getAddress());
                } else {
                    Toast.makeText(UserProfileActivity.this, "Không thể tải hồ sơ", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error loading profile: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserProfileDto>> call, Throwable t) {
                Toast.makeText(UserProfileActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Network failure: ", t);
            }
        });
    }

    private void saveProfileChanges() {
        String authToken = getAuthToken();
        if (authToken == null) {
            Toast.makeText(this, "Phiên đăng nhập hết hạn.", Toast.LENGTH_LONG).show();
            return;
        }

        String fullName = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String address = addressInput.getText().toString().trim();


        // --- CLIENT-SIDE VALIDATION ---
        if (phone.isEmpty() || !Patterns.PHONE.matcher(phone).matches() || phone.length() < 10 || phone.length() > 15) {
            phoneInput.setError("Số điện thoại không hợp lệ (phải có 10-15 chữ số).");
            phoneInput.requestFocus();
            return;
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Địa chỉ email không hợp lệ.");
            emailInput.requestFocus();
            return;
        }
        if (fullName.isEmpty()) {
            nameInput.setError("Tên không được để trống.");
            nameInput.requestFocus();
            return;
        }
        // --- END VALIDATION ---

        UpdateProfileRequest request = new UpdateProfileRequest(fullName, email, phone, address);

        Call<ApiResponse<UserProfileDto>> call = apiService.updateProfile("Bearer " + authToken, request);
        call.enqueue(new Callback<ApiResponse<UserProfileDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserProfileDto>> call, Response<ApiResponse<UserProfileDto>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(UserProfileActivity.this, "Hồ sơ đã được cập nhật!", Toast.LENGTH_SHORT).show();
                    // Update the local user name in SharedPreferences
                    SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                    editor.putString("userName", response.body().getData().getFullName());
                    editor.apply();
                } else {
                     // Try to get a more specific error message
                    String errorMessage = "Cập nhật thất bại.";
                    if (response.code() == 400) {
                        errorMessage = "Dữ liệu không hợp lệ. Vui lòng kiểm tra lại.";
                    } else if (response.message() != null) {
                        errorMessage += " Lỗi: " + response.message();
                    }
                    Toast.makeText(UserProfileActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserProfileDto>> call, Throwable t) {
                Toast.makeText(UserProfileActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
