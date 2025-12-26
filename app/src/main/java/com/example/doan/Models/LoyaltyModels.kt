package com.example.doan.Models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class UserPointsDto(
    @SerializedName("currentPoints")
    val currentPoints: Int = 0,
    
    @SerializedName("pointsToSpin")
    val pointsToSpin: Int = 5,
    
    @SerializedName("canSpin")
    val canSpin: Boolean = false,
    
    @SerializedName("availableRewards")
    val availableRewards: List<SpinRewardDto>? = null
) : Serializable

data class SpinRewardDto(
    @SerializedName("id")
    val id: Long = 0,
    
    @SerializedName("voucherCode")
    val voucherCode: String? = null,
    
    @SerializedName("discountPercent")
    val discountPercent: Int = 0,
    
    @SerializedName("discountLabel")
    val discountLabel: String? = null,
    
    @SerializedName("isUsed")
    val isUsed: Boolean = false,
    
    @SerializedName("createdAt")
    val createdAt: String? = null
) : Serializable

data class SpinWheelResponse(
    @SerializedName("rewardId")
    val rewardId: Long = 0,
    
    @SerializedName("voucherCode")
    val voucherCode: String? = null,
    
    @SerializedName("discountPercent")
    val discountPercent: Int = 0,
    
    @SerializedName("discountLabel")
    val discountLabel: String? = null,
    
    @SerializedName("winIndex")
    val winIndex: Int = 0,
    
    @SerializedName("wheelItems")
    val wheelItems: List<Int>? = null,
    
    @SerializedName("remainingPoints")
    val remainingPoints: Int = 0,
    
    @SerializedName("message")
    val message: String? = null
) : Serializable
