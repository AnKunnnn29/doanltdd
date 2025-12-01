package com.example.doan.Network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;

public class AuthInterceptor implements Interceptor {
    
    private static final String TAG = "AuthInterceptor";
    private Context context;
    
    public AuthInterceptor(Context context) {
        this.context = context;
    }
    
    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request originalRequest = chain.request();
        
        // Lấy token từ SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences("UTETeaPrefs", Context.MODE_PRIVATE);
        String token = prefs.getString("jwt_token", null);
        
        // Log để debug
        Log.d(TAG, "Request URL: " + originalRequest.url());
        Log.d(TAG, "Token: " + (token != null ? "exists" : "null"));
        
        // Chỉ thêm token nếu có và không rỗng
        if (token != null && !token.isEmpty()) {
            Request newRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer " + token)
                    .build();
            Log.d(TAG, "Added Authorization header");
            return chain.proceed(newRequest);
        }
        
        // Không có token, gửi request bình thường (cho public endpoints)
        Log.d(TAG, "No token, sending request without Authorization header");
        return chain.proceed(originalRequest);
    }
}
