package com.example.doan.Models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Drink(
    @SerializedName("id")
    var id: Int = 0,
    
    @SerializedName("name")
    var name: String? = null,
    
    @SerializedName("description")
    var description: String? = null,
    
    @SerializedName(value = "imageUrl", alternate = ["image_url"])
    var imageUrl: String? = null,
    
    @SerializedName(value = "basePrice", alternate = ["price"])
    var basePrice: Double = 0.0,
    
    @SerializedName("isActive")
    var isActive: Boolean = true,
    
    @SerializedName(value = "categoryId", alternate = ["category_id"])
    var categoryId: Int = 0,
    
    @SerializedName("categoryName")
    var categoryName: String? = null,
    
    @SerializedName("sizes")
    var sizes: List<DrinkSize>? = null,
    
    @SerializedName("toppings")
    var toppings: List<DrinkTopping>? = null
) : Serializable
