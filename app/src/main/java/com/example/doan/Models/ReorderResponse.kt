package com.example.doan.Models

import com.google.gson.annotations.SerializedName

data class ReorderResponse(
    @SerializedName("cart")
    val cart: Cart?,
    
    @SerializedName("itemStatuses")
    val itemStatuses: List<ReorderItemStatus>?,
    
    @SerializedName("hasUnavailableItems")
    val hasUnavailableItems: Boolean = false,
    
    @SerializedName("message")
    val message: String?
)

data class ReorderItemStatus(
    @SerializedName("drinkName")
    val drinkName: String?,
    
    @SerializedName("sizeName")
    val sizeName: String?,
    
    @SerializedName("drinkAvailable")
    val drinkAvailable: Boolean = true,
    
    @SerializedName("sizeAvailable")
    val sizeAvailable: Boolean = true,
    
    @SerializedName("toppingStatuses")
    val toppingStatuses: List<ToppingStatus>?,
    
    @SerializedName("addedToCart")
    val addedToCart: Boolean = false,
    
    @SerializedName("reason")
    val reason: String?,
    
    @SerializedName("suggestions")
    val suggestions: List<SuggestionDto>?
)

data class ToppingStatus(
    @SerializedName("toppingName")
    val toppingName: String?,
    
    @SerializedName("available")
    val available: Boolean = true
)

data class SuggestionDto(
    @SerializedName("drinkId")
    val drinkId: Long,
    
    @SerializedName("drinkName")
    val drinkName: String?,
    
    @SerializedName("drinkImage")
    val drinkImage: String?,
    
    @SerializedName("basePrice")
    val basePrice: Double = 0.0,
    
    @SerializedName("reason")
    val reason: String?
)
