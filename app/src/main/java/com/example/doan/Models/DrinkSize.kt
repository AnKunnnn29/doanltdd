package com.example.doan.Models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class DrinkSize(
    @SerializedName("id")
    var id: Int = 0,
    
    @SerializedName("sizeName")
    var sizeName: String? = null,
    
    @SerializedName("extraPrice")
    var extraPrice: Double = 0.0
) : Serializable
