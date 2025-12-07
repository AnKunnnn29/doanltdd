package com.example.doan.Models

import com.google.gson.annotations.SerializedName

data class VNPayPaymentRequest(
    @SerializedName("orderId")
    val orderId: Long,
    
    @SerializedName("orderInfo")
    val orderInfo: String? = null,
    
    @SerializedName("ipAddress")
    val ipAddress: String? = null
)

data class VNPayPaymentResponse(
    @SerializedName("paymentUrl")
    val paymentUrl: String,
    
    @SerializedName("message")
    val message: String
)
