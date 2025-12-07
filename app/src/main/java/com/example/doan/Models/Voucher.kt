package com.example.doan.Models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class Voucher(
    @SerializedName("id")
    val id: Long? = null,
    
    @SerializedName("code")
    val code: String = "",
    
    @SerializedName("description")
    val description: String? = null,
    
    @SerializedName("discountType")
    val discountType: String = "FIXED", // "PERCENT" hoặc "FIXED"
    
    @SerializedName("discountValue")
    val discountValue: BigDecimal? = null,
    
    @SerializedName("startDate")
    val startDate: String = "",
    
    @SerializedName("endDate")
    val endDate: String = "",
    
    @SerializedName("minOrderValue")
    val minOrderValue: BigDecimal? = null,
    
    @SerializedName("maxDiscountAmount")
    val maxDiscountAmount: BigDecimal? = null,
    
    @SerializedName("minOrderAmount")
    val minOrderAmount: BigDecimal? = null,
    
    @SerializedName("usageLimit")
    val usageLimit: Int? = null,
    
    @SerializedName("usedCount")
    val usedCount: Int = 0,
    
    @SerializedName("isActive")
    val isActive: Boolean = true,
    
    @SerializedName("createdAt")
    val createdAt: String? = null,
    
    @SerializedName("updatedAt")
    val updatedAt: String? = null
)

data class CreateVoucherRequest(
    val code: String,
    val description: String?,
    val discountType: String,
    val discountValue: BigDecimal,
    val startDate: String,
    val endDate: String,
    val minOrderValue: BigDecimal,
    val maxDiscountAmount: BigDecimal?,
    val usageLimit: Int?,
    val isActive: Boolean
)

data class UpdateVoucherRequest(
    val description: String?,
    val discountType: String?,
    val discountValue: BigDecimal?,
    val startDate: String?,
    val endDate: String?,
    val minOrderValue: BigDecimal?,
    val maxDiscountAmount: BigDecimal?,
    val usageLimit: Int?,
    val isActive: Boolean?
)
