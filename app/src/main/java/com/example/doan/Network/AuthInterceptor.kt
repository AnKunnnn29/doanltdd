package com.example.doan.Network

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.doan.BuildConfig
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.JwtResponse
import com.example.doan.Models.RefreshTokenRequest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

/**
 * FIX High #8: Interceptor với auto token refresh
 * Khi nhận 401, sẽ thử refresh token trước khi logout
 */
class AuthInterceptor(private val context: Context) : Interceptor {
    
    // ✅ FIX: Sử dụng EncryptedSharedPreferences giống SessionManager
    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating encrypted prefs, fallback to normal", e)
            context.getSharedPreferences(PREF_NAME + "_fallback", Context.MODE_PRIVATE)
        }
    }
    
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // ✅ FIX: Đọc token từ EncryptedSharedPreferences
        val token = prefs.getString("jwt_token", null)
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        
        // ✅ SECURITY: Chỉ log trong debug mode
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Request URL: ${originalRequest.url}")
            Log.d(TAG, "Token exists: ${token != null}, Is logged in: $isLoggedIn")
        }
        
        // Chỉ thêm token nếu có và không rỗng
        val response = if (!token.isNullOrEmpty()) {
            val newRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .build()
            chain.proceed(newRequest)
        } else {
            // Không có token, gửi request bình thường (cho public endpoints)
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "No token available! User may need to login again.")
            }
            chain.proceed(originalRequest)
        }
        
        // FIX High #8: Xử lý token hết hạn với auto refresh
        if (response.code == 401 && !token.isNullOrEmpty()) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Token expired (401). Attempting to refresh...")
            }
            
            val refreshToken = prefs.getString("refresh_token", null)
            
            if (!refreshToken.isNullOrEmpty()) {
                // Thử refresh token
                val refreshed = tryRefreshToken(chain, refreshToken)
                
                if (refreshed) {
                    // Refresh thành công, retry request với token mới
                    response.close()
                    val newToken = prefs.getString("jwt_token", null)
                    val retryRequest = originalRequest.newBuilder()
                        .header("Authorization", "Bearer $newToken")
                        .build()
                    return chain.proceed(retryRequest)
                }
            }
            
            // Refresh thất bại hoặc không có refresh token
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Token refresh failed. Clearing session...")
            }
            prefs.edit().clear().apply()
            
            // Gửi broadcast để Activity xử lý chuyển về màn hình Login
            val intent = Intent(ACTION_TOKEN_EXPIRED)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }
        
        return response
    }
    
    /**
     * Thử refresh token synchronously
     * @return true nếu refresh thành công
     */
    private fun tryRefreshToken(
        chain: Interceptor.Chain,
        refreshToken: String
    ): Boolean {
        return try {
            // Tạo request refresh token
            val mediaType = "application/json".toMediaType()
            val refreshRequest = okhttp3.Request.Builder()
                .url("${getBaseUrl()}auth/refresh-token")
                .post(
                    """{"refreshToken":"$refreshToken"}""".toRequestBody(mediaType)
                )
                .build()
            
            val refreshResponse = chain.proceed(refreshRequest)
            
            if (refreshResponse.isSuccessful) {
                val responseBody = refreshResponse.body?.string()
                refreshResponse.close()
                
                // Parse response để lấy token mới
                if (responseBody != null) {
                    val gson = com.google.gson.Gson()
                    val type = object : com.google.gson.reflect.TypeToken<ApiResponse<JwtResponse>>() {}.type
                    val apiResponse: ApiResponse<JwtResponse> = gson.fromJson(responseBody, type)
                    
                    if (apiResponse.success == true && apiResponse.data != null) {
                        // Lưu token mới
                        prefs.edit()
                            .putString("jwt_token", apiResponse.data?.accessToken)
                            .putString("refresh_token", apiResponse.data?.refreshToken)
                            .apply()
                        
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "Token refreshed successfully")
                        }
                        return true
                    }
                }
            } else {
                refreshResponse.close()
            }
            
            false
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Error refreshing token: ${e.message}")
            }
            false
        }
    }
    
    private fun getBaseUrl(): String {
        // Sử dụng cùng base URL với RetrofitClient
        return RetrofitClient.getBaseUrl()
    }
    
    companion object {
        private const val TAG = "AuthInterceptor"
        private const val PREF_NAME = "UTETeaPrefs"
        const val ACTION_TOKEN_EXPIRED = "com.example.doan.TOKEN_EXPIRED"
    }
}
