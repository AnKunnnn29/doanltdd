package com.example.doan.Models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class OrderItem(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("drinkName")
    val drinkName: String? = null,

    @SerializedName("drinkImage")
    val drinkImage: String? = null,

    @SerializedName("quantity")
    val quantity: Int = 0,

    @SerializedName("itemPrice")
    val itemPrice: Double = 0.0,

    @SerializedName("sizeName")
    val sizeName: String? = null,

    @SerializedName("note")
    val note: String? = null,

    @SerializedName("toppings")
    val toppings: List<OrderItemTopping>? = null
) : Serializable {
    // Backward compatibility
    val price: Double get() = itemPrice
}

data class OrderItemTopping(
    @SerializedName("id")
    val id: Int = 0,
    
    @SerializedName("toppingName")
    val toppingName: String? = null,
    
    @SerializedName("price")
    val price: Double = 0.0
) : Serializable {
    // Backward compatibility
    val name: String? get() = toppingName
}
