package com.example.doan.Models

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("success")
    var success: Boolean = false,
    
    @SerializedName("message")
    var message: String? = null,
    
    @SerializedName("data")
    var data: T? = null
)
