package com.example.doan.Models

import com.google.gson.annotations.SerializedName

data class JwtResponse(
    // Sửa lại "token" để khớp với server
    @SerializedName("token")
    val accessToken: String,

    @SerializedName("refreshToken")
    val refreshToken: String,

    @SerializedName("tokenType")
    val tokenType: String = "Bearer",

    @SerializedName("expiresIn")
    val expiresIn: Long
)
