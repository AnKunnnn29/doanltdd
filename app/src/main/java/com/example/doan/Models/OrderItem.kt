package com.example.doan.Models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class OrderItem(
    @SerializedName("id")
    val id: Int,

    @SerializedName("drink_name")
    val drinkName: String? = null,

    @SerializedName("drink_image")
    val drinkImage: String? = null,

    @SerializedName("quantity")
    val quantity: Int = 0,

    @SerializedName("price")
    val price: Double = 0.0,

    @SerializedName("size_name")
    val sizeName: String? = null,

    @SerializedName("toppings")
    val toppings: List<Topping>? = null
) : Serializable
