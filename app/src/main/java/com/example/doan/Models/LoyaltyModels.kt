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

// ============ MEMBER TIER BENEFITS ============

data class MemberTierBenefitsDto(
    @SerializedName("currentTier")
    val currentTier: String = "BRONZE",
    
    @SerializedName("tierName")
    val tierName: String = "Đồng",
    
    @SerializedName("tierColor")
    val tierColor: String = "#CD7F32",
    
    @SerializedName("currentPoints")
    val currentPoints: Int = 0,
    
    @SerializedName("pointsToNextTier")
    val pointsToNextTier: Int = 0,
    
    @SerializedName("nextTier")
    val nextTier: String? = null,
    
    @SerializedName("nextTierName")
    val nextTierName: String? = null,
    
    @SerializedName("progressPercent")
    val progressPercent: Double = 0.0,
    
    // Quyền lợi hiện tại
    @SerializedName("discountPercent")
    val discountPercent: Double = 0.0,
    
    @SerializedName("pointsMultiplier")
    val pointsMultiplier: Double = 1.0,
    
    @SerializedName("freeShipping")
    val freeShipping: Boolean = false,
    
    @SerializedName("freeShippingMinOrder")
    val freeShippingMinOrder: Double = 0.0,
    
    @SerializedName("birthdayVoucher")
    val birthdayVoucher: Boolean = false,
    
    @SerializedName("birthdayVoucherPercent")
    val birthdayVoucherPercent: Int = 0,
    
    @SerializedName("prioritySupport")
    val prioritySupport: Boolean = false,
    
    @SerializedName("exclusiveOffers")
    val exclusiveOffers: Boolean = false,
    
    @SerializedName("earlyAccess")
    val earlyAccess: Boolean = false,
    
    @SerializedName("description")
    val description: String? = null,
    
    @SerializedName("benefitsList")
    val benefitsList: List<String>? = null,
    
    @SerializedName("allTiers")
    val allTiers: List<TierInfoDto>? = null
) : Serializable

data class TierInfoDto(
    @SerializedName("tier")
    val tier: String = "BRONZE",
    
    @SerializedName("tierName")
    val tierName: String = "Đồng",
    
    @SerializedName("tierColor")
    val tierColor: String = "#CD7F32",
    
    @SerializedName("minPoints")
    val minPoints: Int = 0,
    
    @SerializedName("discountPercent")
    val discountPercent: Double = 0.0,
    
    @SerializedName("isCurrentTier")
    val isCurrentTier: Boolean = false,
    
    @SerializedName("isUnlocked")
    val isUnlocked: Boolean = false
) : Serializable

// ============ TIER DISCOUNT PREVIEW ============

data class TierDiscountPreview(
    @SerializedName("tier")
    val tier: String = "BRONZE",
    
    @SerializedName("tierName")
    val tierName: String = "Đồng",
    
    @SerializedName("discountPercent")
    val discountPercent: Double = 0.0,
    
    @SerializedName("orderTotal")
    val orderTotal: Double = 0.0,
    
    @SerializedName("tierDiscount")
    val tierDiscount: Double = 0.0,
    
    @SerializedName("finalTotal")
    val finalTotal: Double = 0.0,
    
    @SerializedName("message")
    val message: String? = null
) : Serializable
