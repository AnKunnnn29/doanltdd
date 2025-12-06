package com.example.doan.Network

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Interceptor để tự động thêm JWT token vào header và xử lý token expired
 */
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
        val response = if (!token.isNullOrEmpty()) {
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
        
        // FIX C4: Xử lý token hết hạn (401 Unauthorized)
        if (response.code == 401 && !token.isNullOrEmpty()) {
            Log.w(TAG, "Token expired or invalid (401). Clearing session...")
            
            // Xóa session
            prefs.edit().clear().apply()
            
            // Gửi broadcast để Activity xử lý chuyển về màn hình Login
            val intent = Intent(ACTION_TOKEN_EXPIRED)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
            
            Log.w(TAG, "Broadcast sent: User needs to login again")
        }
        
        return response
    }
    
    companion object {
        private const val TAG = "AuthInterceptor"
        const val ACTION_TOKEN_EXPIRED = "com.example.doan.TOKEN_EXPIRED"
    }
}
