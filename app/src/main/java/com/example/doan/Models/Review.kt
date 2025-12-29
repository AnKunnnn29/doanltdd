package com.example.doan.Models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Review(
    @SerializedName("id")
    val id: Long = 0,
    
    @SerializedName("userId")
    val userId: Long? = null,
    
    @SerializedName("userName")
    val userName: String? = null,
    
    @SerializedName("userAvatar")
    val userAvatar: String? = null,
    
    @SerializedName("drinkId")
    val drinkId: Long = 0,
    
    @SerializedName("drinkName")
    val drinkName: String? = null,
    
    @SerializedName("orderId")
    val orderId: Long = 0,
    
    @SerializedName("orderItemId")
    val orderItemId: Long = 0,
    
    @SerializedName("rating")
    val rating: Int = 0,
    
    @SerializedName("comment")
    val comment: String? = null,
    
    @SerializedName("isAnonymous")
    val isAnonymous: Boolean = false,
    
    @SerializedName("createdAt")
    val createdAt: String? = null
) : Serializable

data class CreateReviewRequest(
    @SerializedName("orderId")
    val orderId: Long,
    
    @SerializedName("orderItemId")
    val orderItemId: Long,
    
    @SerializedName("rating")
    val rating: Int,
    
    @SerializedName("comment")
    val comment: String? = null,
    
    @SerializedName("isAnonymous")
    val isAnonymous: Boolean = false
)

data class DrinkRatingSummary(
    @SerializedName("drinkId")
    val drinkId: Long = 0,
    
    @SerializedName("averageRating")
    val averageRating: Double = 0.0,
    
    @SerializedName("totalReviews")
    val totalReviews: Long = 0,
    
    @SerializedName("ratingDistribution")
    val ratingDistribution: Map<Int, Long>? = null
)
