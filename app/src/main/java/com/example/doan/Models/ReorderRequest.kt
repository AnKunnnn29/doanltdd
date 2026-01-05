package com.example.doan.Models

import com.google.gson.annotations.SerializedName

data class ReorderRequest(
    @SerializedName("orderId")
    val orderId: Long
)
