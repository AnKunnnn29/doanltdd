package com.example.doan.Models

import com.google.gson.annotations.SerializedName

/**
 * Model cho dữ liệu thời tiết từ API
 */
data class WeatherResponse(
    val city: String?,
    val country: String?,
    val temperature: Double?,
    val feelsLike: Double?,
    val tempMin: Double?,
    val tempMax: Double?,
    val humidity: Int?,
    val condition: String?,
    val description: String?,
    val icon: String?,
    val iconUrl: String?,
    val windSpeed: Double?,
    val pressure: Int?,
    val visibility: Int?,
    val sunrise: Long?,
    val sunset: Long?,
    val timestamp: Long?,
    val businessInsight: WeatherBusinessInsight?
)

data class WeatherBusinessInsight(
    val weatherType: String?,           // HOT, COLD, RAINY, NORMAL
    val recommendation: String?,        // Gợi ý cho manager
    val suggestedDrinks: List<String>?, // Các loại đồ uống nên đẩy mạnh
    val expectedImpact: Double?         // Dự kiến ảnh hưởng doanh thu (%)
)
