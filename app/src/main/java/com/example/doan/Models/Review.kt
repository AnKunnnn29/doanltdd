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

// ==================== MANAGER REVIEW MANAGEMENT ====================

/**
 * Model cho quản lý đánh giá (Admin/Manager)
 * Bao gồm cả review hiện tại và backup từ user đã xóa
 */
data class ReviewManagement(
    @SerializedName("id")
    val id: Long = 0,
    
    @SerializedName("userId")
    val userId: Long? = null,
    
    @SerializedName("userName")
    val userName: String? = null,
    
    @SerializedName("userFullName")
    val userFullName: String? = null,
    
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
    val createdAt: String? = null,
    
    // Thông tin bổ sung cho admin
    @SerializedName("isFromDeletedUser")
    val isFromDeletedUser: Boolean = false,  // true nếu là backup từ user đã xóa
    
    @SerializedName("isHidden")
    val isHidden: Boolean = false            // true nếu review bị ẩn
) : Serializable

/**
 * Thống kê đánh giá tổng hợp cho Admin/Manager
 */
data class ReviewStatistics(
    @SerializedName("totalReviews")
    val totalReviews: Long = 0,
    
    @SerializedName("activeReviews")
    val activeReviews: Long = 0,      // Reviews từ user còn hoạt động
    
    @SerializedName("backupReviews")
    val backupReviews: Long = 0,      // Reviews từ user đã xóa
    
    @SerializedName("averageRating")
    val averageRating: Double = 0.0,
    
    @SerializedName("fiveStarCount")
    val fiveStarCount: Long = 0,
    
    @SerializedName("fourStarCount")
    val fourStarCount: Long = 0,
    
    @SerializedName("threeStarCount")
    val threeStarCount: Long = 0,
    
    @SerializedName("twoStarCount")
    val twoStarCount: Long = 0,
    
    @SerializedName("oneStarCount")
    val oneStarCount: Long = 0
) : Serializable
