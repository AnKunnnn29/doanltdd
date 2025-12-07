package com.example.doan.Models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

import java.math.BigDecimal

data class DashboardSummary(
    @SerializedName("totalRevenue")
    var totalRevenue: BigDecimal = BigDecimal.ZERO,
    
    @SerializedName("totalOrders")
    var totalOrders: Long = 0,
    
    @SerializedName("pendingOrders")
    var pendingOrders: Long = 0,
    
    @SerializedName("completedOrders")
    var completedOrders: Long = 0,
    
    @SerializedName("canceledOrders")
    var canceledOrders: Long = 0,
    
    @SerializedName("topSellingDrinks")
    var topSellingDrinks: List<TopSellingDrink>? = null
) : Serializable {
    
    data class TopSellingDrink(
        @SerializedName("drinkName")
        val drinkName: String? = null,
        
        @SerializedName("totalSold")
        val totalSold: Long = 0,
        
        @SerializedName("revenue")
        val revenue: BigDecimal = BigDecimal.ZERO
    ) : Serializable
}
