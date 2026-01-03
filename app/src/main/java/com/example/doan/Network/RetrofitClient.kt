package com.example.doan.Network

import android.content.Context
import com.example.doan.BuildConfig
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

class RetrofitClient private constructor(context: Context) {
    
    val apiService: ApiService
    private val baseUrl: String = BASE_URL
    
    /**
     * Lấy base URL để sử dụng cho WebSocket
     */
    fun getBaseUrl(): String = baseUrl
    
    init {
        // ✅ CHỈ BẬT LOGGING TRONG DEBUG MODE
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE  // Tắt hoàn toàn trong production
            }
        }
        
        val authInterceptor = AuthInterceptor(context)
        
        val client = OkHttpClient.Builder()
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(logging)  // Chỉ thêm logging trong debug
                }
            }
            .addInterceptor(authInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
        
        val gson = GsonBuilder()
            .setLenient()
            .create()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create()) // For plain text
            .addConverterFactory(GsonConverterFactory.create(gson)) // For JSON
            .build()
        
        apiService = retrofit.create(ApiService::class.java)
    }
    
    companion object {
        // ✅ URL Production trên Railway
        // private const val BASE_URL = "https://utetea-backend-production.up.railway.app/api/"
        private const val BASE_URL = "http://10.0.2.2:8080/api/"
        // private const val BASE_URL = "https://backend-app-ngwy.onrender.com/api/"  render

        @Volatile
        private var instance: RetrofitClient? = null
        
        fun getInstance(context: Context): RetrofitClient {
            return instance ?: synchronized(this) {
                instance ?: RetrofitClient(context.applicationContext).also { instance = it }
            }
        }
        
        fun getBaseUrl(): String = BASE_URL
    }
}
