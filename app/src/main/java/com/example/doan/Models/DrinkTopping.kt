package com.example.doan.Models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class DrinkTopping(
    @SerializedName("id")
    var id: Int = 0,
    
    @SerializedName("toppingName")
    var toppingName: String? = null,
    
    @SerializedName("price")
    var price: Double = 0.0,
    
    @SerializedName("isActive")
    var isActive: Boolean = true
) : Serializable
