package com.example.doan.Models

import com.google.gson.annotations.SerializedName

// File: ChangePasswordRequest.kt
data class ChangePasswordRequest(
    @SerializedName("oldPassword")
    val oldPassword: String,

    @SerializedName("newPassword")
    val newPassword: String,

    @SerializedName("confirmPassword")
    val confirmPassword: String
)
