package com.example.doan.Models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Product(
    @SerializedName("id")
    var id: Int = 0,
    
    @SerializedName("name")
    var name: String? = null,
    
    @SerializedName("description")
    var description: String? = null,
    
    @SerializedName("price")
    var price: Double = 0.0,
    
    @SerializedName("category")
    var category: String? = null,
    
    var categoryId: Int = 0,
    
    @SerializedName("image_url")
    var imageUrl: String? = null,
    
    @SerializedName("is_available")
    var isAvailable: Boolean = true,
    
    @SerializedName("sizes")
    var sizes: List<DrinkSize>? = null,
    
    @SerializedName("toppings")
    var toppings: List<DrinkTopping>? = null
) : Serializable {
    
    override fun toString(): String {
        return "Product(id=$id, name='$name', price=$price, category='$category', categoryId=$categoryId, isAvailable=$isAvailable)"
    }
}
