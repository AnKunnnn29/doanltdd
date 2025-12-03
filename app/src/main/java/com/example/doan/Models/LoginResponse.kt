package com.example.doan.Models

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("id")
    var userId: Int = 0,
    
    @SerializedName("username")
    var username: String? = null,

    @SerializedName("email")
    var email: String? = null,
    
    @SerializedName("fullName")
    var fullName: String? = null,
    
    @SerializedName("phone")
    var phone: String? = null,
    
    @SerializedName("role")
    var role: String? = null,
    
    @SerializedName("memberTier")
    var memberTier: String? = null,

    @SerializedName("token")
    var token: String? = null
) {
    fun isManager(): Boolean = role == "MANAGER"
}
