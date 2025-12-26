package com.example.doan.Models

import com.google.gson.annotations.SerializedName

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val message: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "TEXT", // TEXT, DRINKS, VOUCHERS, STORES, ORDER
    val data: List<Any>? = null
)

data class ChatRequest(
    val message: String,
    val userId: Long?
)

data class ChatResponse(
    @SerializedName("message")
    val message: String,
    
    @SerializedName("type")
    val type: String,
    
    @SerializedName("data")
    val data: List<Any>? = null
)
