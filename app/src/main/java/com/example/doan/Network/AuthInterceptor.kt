package com.example.doan.Network

import android.content.Context
import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class AuthInterceptor(private val context: Context) : Interceptor {
    
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Lấy token từ SharedPreferences
        val prefs = context.getSharedPreferences("UTETeaPrefs", Context.MODE_PRIVATE)
        val token = prefs.getString("jwt_token", null)
        
        // Log để debug
        Log.d(TAG, "Request URL: ${originalRequest.url}")
        Log.d(TAG, "Token: ${if (token != null) "exists" else "null"}")
        
        // Chỉ thêm token nếu có và không rỗng
        return if (!token.isNullOrEmpty()) {
            val newRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            Log.d(TAG, "Added Authorization header")
            chain.proceed(newRequest)
        } else {
            // Không có token, gửi request bình thường (cho public endpoints)
            Log.d(TAG, "No token, sending request without Authorization header")
            chain.proceed(originalRequest)
        }
    }
    
    companion object {
        private const val TAG = "AuthInterceptor"
    }
}
