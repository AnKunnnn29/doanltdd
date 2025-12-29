package com.example.doan.Models

import com.google.gson.annotations.SerializedName

data class CreateOrderRequest(
    @SerializedName("storeId")
    var storeId: Long,
    
    @SerializedName("type")
    var type: String,
    
    @SerializedName("address")
    var address: String? = null,
    
    @SerializedName("paymentMethod")
    var paymentMethod: String,
    
    @SerializedName("pickupTime")
    var pickupTime: String? = null,
    
    @SerializedName("promotionCode")
    var promotionCode: String? = null,
    
    @SerializedName("spinVoucherCode")
    var spinVoucherCode: String? = null,
    
    @SerializedName("items")
    var items: List<OrderItemRequest>
)
