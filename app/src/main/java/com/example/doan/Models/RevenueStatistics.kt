package com.example.doan.Models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class RevenueStatistics(
    @SerializedName("totalRevenue")
    val totalRevenue: Double = 0.0,
    
    @SerializedName("dailyRevenues")
    val dailyRevenues: List<DailyRevenue>? = null,
    
    @SerializedName("monthlyRevenues")
    val monthlyRevenues: List<MonthlyRevenue>? = null,
    
    @SerializedName("topSellingDrinks")
    val topSellingDrinks: List<TopSellingDrink>? = null
) : Serializable {
    
    data class DailyRevenue(
        @SerializedName("date")
        val date: String,
        
        @SerializedName("revenue")
        val revenue: Double,
        
        @SerializedName("orderCount")
        val orderCount: Long
    ) : Serializable
    
    data class MonthlyRevenue(
        @SerializedName("year")
        val year: Int,
        
        @SerializedName("month")
        val month: Int,
        
        @SerializedName("revenue")
        val revenue: Double,
        
        @SerializedName("orderCount")
        val orderCount: Long
    ) : Serializable
    
    data class TopSellingDrink(
        @SerializedName("drinkId")
        val drinkId: Long,
        
        @SerializedName("drinkName")
        val drinkName: String,
        
        @SerializedName("imageUrl")
        val imageUrl: String?,
        
        @SerializedName("totalQuantity")
        val totalQuantity: Long,
        
        @SerializedName("totalRevenue")
        val totalRevenue: Double
    ) : Serializable
}
