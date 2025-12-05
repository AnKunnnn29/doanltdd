package com.example.doan.Models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Topping(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("price")
    val price: Double = 0.0
) : Serializable
