package com.example.doan.Models

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("usernameOrPhone")
    var usernameOrPhone: String,
    
    @SerializedName("password")
    var password: String
)
