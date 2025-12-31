package com.example.doan.Utils

import android.content.Context
import android.util.Log
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.WeatherResponse
import com.example.doan.Network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Helper class để lấy và xử lý dữ liệu thời tiết
 */
object WeatherHelper {
    
    private const val TAG = "WeatherHelper"
    
    /**
     * Lấy thời tiết hiện tại (mặc định TP.HCM)
     */
    fun getCurrentWeather(
        context: Context,
        onSuccess: (WeatherResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        RetrofitClient.getInstance(context).apiService.getCurrentWeather()
            .enqueue(object : Callback<ApiResponse<WeatherResponse>> {
                override fun onResponse(
                    call: Call<ApiResponse<WeatherResponse>>,
                    response: Response<ApiResponse<WeatherResponse>>
                ) {
                    if (response.isSuccessful && response.body()?.data != null) {
                        Log.d(TAG, "Weather loaded: ${response.body()?.data}")
                        onSuccess(response.body()!!.data!!)
                    } else {
                        val error = "Không thể tải dữ liệu thời tiết"
                        Log.e(TAG, error)
                        onError(error)
                    }
                }
                
                override fun onFailure(call: Call<ApiResponse<WeatherResponse>>, t: Throwable) {
                    Log.e(TAG, "Weather API error: ${t.message}")
                    onError(t.message ?: "Lỗi kết nối")
                }
            })
    }
    
    /**
     * Lấy thời tiết theo thành phố
     */
    fun getWeatherByCity(
        context: Context,
        city: String,
        country: String = "VN",
        onSuccess: (WeatherResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        RetrofitClient.getInstance(context).apiService.getWeatherByCity(city, country)
            .enqueue(object : Callback<ApiResponse<WeatherResponse>> {
                override fun onResponse(
                    call: Call<ApiResponse<WeatherResponse>>,
                    response: Response<ApiResponse<WeatherResponse>>
                ) {
                    if (response.isSuccessful && response.body()?.data != null) {
                        onSuccess(response.body()!!.data!!)
                    } else {
                        onError("Không thể tải dữ liệu thời tiết cho $city")
                    }
                }
                
                override fun onFailure(call: Call<ApiResponse<WeatherResponse>>, t: Throwable) {
                    onError(t.message ?: "Lỗi kết nối")
                }
            })
    }
    
    /**
     * Lấy icon emoji dựa trên condition
     */
    fun getWeatherEmoji(condition: String?): String {
        return when (condition?.lowercase()) {
            "clear" -> "☀️"
            "clouds" -> "☁️"
            "rain", "drizzle" -> "🌧️"
            "thunderstorm" -> "⛈️"
            "snow" -> "❄️"
            "mist", "fog", "haze" -> "🌫️"
            else -> "🌤️"
        }
    }
    
    /**
     * Lấy màu background dựa trên weatherType
     */
    fun getWeatherColor(weatherType: String?): Int {
        return when (weatherType?.uppercase()) {
            "HOT" -> 0xFFFF6B6B.toInt()      // Đỏ cam
            "WARM" -> 0xFFFFB347.toInt()     // Cam
            "COLD" -> 0xFF74B9FF.toInt()     // Xanh dương nhạt
            "RAINY" -> 0xFF636E72.toInt()    // Xám
            else -> 0xFF00CEC9.toInt()       // Xanh ngọc
        }
    }
    
    /**
     * Format nhiệt độ
     */
    fun formatTemperature(temp: Double?): String {
        return if (temp != null) "${temp.toInt()}°C" else "--°C"
    }
    
    /**
     * Format impact percentage
     */
    fun formatImpact(impact: Double?): String {
        return when {
            impact == null -> "0%"
            impact > 0 -> "+${impact.toInt()}%"
            else -> "${impact.toInt()}%"
        }
    }
}
