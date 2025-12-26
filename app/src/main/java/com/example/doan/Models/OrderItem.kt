package com.example.doan.Models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

// FIX C4: Chuyển từ Serializable sang Parcelable
@Parcelize
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
) : Parcelable {
    // Backward compatibility
    val price: Double get() = itemPrice
}

// FIX C4: Chuyển từ Serializable sang Parcelable
@Parcelize
data class OrderItemTopping(
    @SerializedName("id")
    val id: Int = 0,
    
    @SerializedName("toppingName")
    val toppingName: String? = null,
    
    @SerializedName("price")
    val price: Double = 0.0
) : Parcelable {
    // Backward compatibility
    val name: String? get() = toppingName
}
