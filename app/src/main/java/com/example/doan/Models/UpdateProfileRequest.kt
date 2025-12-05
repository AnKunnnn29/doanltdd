package com.example.doan.Models

data class UpdateProfileRequest(
    var fullName: String,
    var email: String,
    var phone: String,
    var address: String
)
