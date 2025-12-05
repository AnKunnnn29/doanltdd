package com.example.doan.Models

import com.google.gson.annotations.SerializedName

data class VerifyOtpRequest(
    @SerializedName("email")
    var email: String,
    
    var otp: String
)
