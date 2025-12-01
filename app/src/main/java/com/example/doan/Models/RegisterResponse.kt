package com.example.doan.Models

import com.google.gson.annotations.SerializedName

data class RegisterResponse(
    @SerializedName("userId")
    var userId: Int = 0,
    
    @SerializedName("username")
    var username: String? = null,
    
    @SerializedName("fullName")
    var fullName: String? = null,
    
    @SerializedName("phone")
    var phone: String? = null,
    
    @SerializedName("role")
    var role: String? = null,
    
    @SerializedName("memberTier")
    var memberTier: String? = null
)
