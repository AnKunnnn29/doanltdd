package com.example.doan.Models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

/**
 * Models cho tính năng Predictive Order - Dự đoán món khách hàng muốn đặt
 */

data class PredictiveOrderResponse(
    @SerializedName("hasPrediction")
    val hasPrediction: Boolean,
    
    @SerializedName("message")
    val message: String?,
    
    @SerializedName("predictedDrink")
    val predictedDrink: PredictedDrink?,
    
    @SerializedName("triggerReasons")
    val triggerReasons: List<String>?,
    
    @SerializedName("confidenceScore")
    val confidenceScore: Double
)

data class PredictedDrink(
    @SerializedName("drinkId")
    val drinkId: Long,
    
    @SerializedName("drinkName")
    val drinkName: String,
    
    @SerializedName("drinkImage")
    val drinkImage: String?,
    
    @SerializedName("sizeName")
    val sizeName: String?,
    
    @SerializedName("sizeId")
    val sizeId: Long?,
    
    @SerializedName("price")
    val price: BigDecimal?,
    
    @SerializedName("orderCount")
    val orderCount: Int,
    
    @SerializedName("lastOrderTime")
    val lastOrderTime: String?,
    
    @SerializedName("toppings")
    val toppings: List<PredictedTopping>?,
    
    @SerializedName("note")
    val note: String?
)

data class PredictedTopping(
    @SerializedName("toppingId")
    val toppingId: Long,
    
    @SerializedName("toppingName")
    val toppingName: String,
    
    @SerializedName("price")
    val price: BigDecimal?
)
