package com.example.doan.Models

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    @SerializedName("username")
    var username: String,
    
    @SerializedName("phone")
    var phone: String,
    
    @SerializedName("password")
    var password: String,
    
    @SerializedName("fullName")
    var fullName: String,
    
    @SerializedName("address")
    var address: String? = null
)
