package com.example.doan.Models

import com.google.gson.annotations.SerializedName

/**
 * 🛡️ User Activity Log Model
 * Hiển thị log hoạt động của user trong hệ thống giám sát
 */
data class UserActivityLog(
    @SerializedName("id") val id: Long,
    @SerializedName("userId") val userId: Long?,
    @SerializedName("username") val username: String?,
    @SerializedName("userEmail") val userEmail: String?,
    @SerializedName("userFullName") val userFullName: String?,
    @SerializedName("userAvatarUrl") val userAvatarUrl: String?,
    
    // Flag đánh dấu log của user đã xóa
    @SerializedName("isDeletedUser") val isDeletedUser: Boolean? = false,
    
    @SerializedName("activityType") val activityType: String,
    @SerializedName("activityTypeDisplay") val activityTypeDisplay: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("riskLevel") val riskLevel: String,
    @SerializedName("riskLevelDisplay") val riskLevelDisplay: String?,
    
    @SerializedName("ipAddress") val ipAddress: String?,
    @SerializedName("deviceInfo") val deviceInfo: String?,
    @SerializedName("userAgent") val userAgent: String?,
    @SerializedName("endpoint") val endpoint: String?,
    @SerializedName("requestMethod") val requestMethod: String?,
    @SerializedName("responseStatus") val responseStatus: Int?,
    
    @SerializedName("relatedId") val relatedId: Long?,
    @SerializedName("extraData") val extraData: String?,
    
    @SerializedName("createdAt") val createdAt: String?
) {
    // Helper để lấy màu theo risk level
    fun getRiskColor(): Int {
        return when (riskLevel) {
            "NORMAL" -> android.graphics.Color.parseColor("#4CAF50")     // Green
            "WARNING" -> android.graphics.Color.parseColor("#FF9800")    // Orange
            "SUSPICIOUS" -> android.graphics.Color.parseColor("#F44336") // Red
            "CRITICAL" -> android.graphics.Color.parseColor("#9C27B0")   // Purple
            else -> android.graphics.Color.parseColor("#9E9E9E")         // Gray
        }
    }
    
    // Helper để lấy icon theo activity type
    fun getActivityIcon(): Int {
        return when {
            activityType.contains("LOGIN") -> android.R.drawable.ic_menu_myplaces
            activityType.contains("ORDER") -> android.R.drawable.ic_menu_agenda
            activityType.contains("PAYMENT") -> android.R.drawable.ic_menu_send
            activityType.contains("PROMOTION") -> android.R.drawable.ic_menu_share
            activityType.contains("SPAM") || activityType.contains("BRUTE") -> android.R.drawable.ic_dialog_alert
            else -> android.R.drawable.ic_menu_info_details
        }
    }
}
