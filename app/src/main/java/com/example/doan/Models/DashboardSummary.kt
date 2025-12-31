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
    var topSellingDrinks: List<TopSellingDrink>? = null,
    
    @SerializedName("topRatedDrinks")
    var topRatedDrinks: List<TopRatedDrink>? = null,
    
    // Thông tin stores được quản lý (cho Manager)
    @SerializedName("managedStores")
    var managedStores: List<ManagedStoreInfo>? = null,
    
    @SerializedName("isAdmin")
    var isAdmin: Boolean? = false
) : Serializable {
    
    data class TopSellingDrink(
        @SerializedName("drinkName")
        val drinkName: String? = null,
        
        @SerializedName("totalSold")
        val totalSold: Long = 0,
        
        @SerializedName("revenue")
        val revenue: BigDecimal = BigDecimal.ZERO
    ) : Serializable
    
    data class TopRatedDrink(
        @SerializedName("drinkId")
        val drinkId: Long = 0,
        
        @SerializedName("drinkName")
        val drinkName: String? = null,
        
        @SerializedName("drinkImage")
        val drinkImage: String? = null,
        
        @SerializedName("averageRating")
        val averageRating: Double = 0.0,
        
        @SerializedName("totalReviews")
        val totalReviews: Long = 0
    ) : Serializable
    
    data class ManagedStoreInfo(
        @SerializedName("id")
        val id: Long = 0,
        
        @SerializedName("storeName")
        val storeName: String? = null,
        
        @SerializedName("address")
        val address: String? = null
    ) : Serializable
}
