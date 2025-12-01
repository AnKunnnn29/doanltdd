package com.example.doan.Models

import com.google.gson.annotations.SerializedName

data class OrderItemRequest(
    @SerializedName("drinkId")
    var drinkId: Long,
    
    @SerializedName("sizeName")
    var sizeName: String,
    
    @SerializedName("quantity")
    var quantity: Int,
    
    @SerializedName("note")
    var note: String? = null,
    
    @SerializedName("toppingIds")
    var toppingIds: List<Long>? = null
)
