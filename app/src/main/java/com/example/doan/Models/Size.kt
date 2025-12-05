package com.example.doan.Models

import com.google.gson.annotations.SerializedName

data class Size(
    @SerializedName("id")
    var id: Int = 0,
    
    @SerializedName("sizeName")
    var sizeName: String? = null,
    
    @SerializedName("extraPrice")
    var extraPrice: Double = 0.0
)
