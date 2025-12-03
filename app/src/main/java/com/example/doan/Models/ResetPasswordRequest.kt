package com.example.doan.Models

data class ResetPasswordRequest(
    val email: String,
    val otp: String,
    val newPassword: String
)
