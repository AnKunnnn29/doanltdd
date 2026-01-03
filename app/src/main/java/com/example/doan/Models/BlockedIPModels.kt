package com.example.doan.Models

import com.google.gson.annotations.SerializedName

/**
 * 🚫 Blocked IP Model
 * Thông tin IP bị chặn
 */
data class BlockedIP(
    @SerializedName("id") val id: Long,
    @SerializedName("ipAddress") val ipAddress: String,
    @SerializedName("blockType") val blockType: String,
    @SerializedName("blockTypeDisplay") val blockTypeDisplay: String?,
    @SerializedName("reason") val reason: String?,
    
    @SerializedName("blockedById") val blockedById: Long?,
    @SerializedName("blockedByUsername") val blockedByUsername: String?,
    
    @SerializedName("blockedUntil") val blockedUntil: String?,
    @SerializedName("isActive") val isActive: Boolean?,
    @SerializedName("isCurrentlyBlocked") val isCurrentlyBlocked: Boolean?,
    
    @SerializedName("unblockedAt") val unblockedAt: String?,
    @SerializedName("unblockedById") val unblockedById: Long?,
    @SerializedName("unblockedByUsername") val unblockedByUsername: String?,
    @SerializedName("unblockReason") val unblockReason: String?,
    
    @SerializedName("alertId") val alertId: Long?,
    @SerializedName("relatedUserId") val relatedUserId: Long?,
    @SerializedName("relatedUsername") val relatedUsername: String?,
    
    @SerializedName("blockedRequestsCount") val blockedRequestsCount: Long?,
    @SerializedName("createdAt") val createdAt: String?
) {
    fun getBlockTypeEmoji(): String {
        return when (blockType) {
            "TEMPORARY" -> "⏰"
            "PERMANENT" -> "⛔"
            "AUTO" -> "🤖"
            else -> "🚫"
        }
    }
    
    fun getBlockTypeText(): String {
        return when (blockType) {
            "TEMPORARY" -> "Tạm thời"
            "PERMANENT" -> "Vĩnh viễn"
            "AUTO" -> "Tự động"
            else -> blockType
        }
    }
    
    fun getStatusText(): String {
        return if (isCurrentlyBlocked == true || isActive == true) {
            "🔴 Đang chặn"
        } else {
            "🟢 Đã gỡ"
        }
    }
    
    fun getStatusColor(): Int {
        return if (isCurrentlyBlocked == true || isActive == true) {
            android.graphics.Color.parseColor("#F44336") // Red
        } else {
            android.graphics.Color.parseColor("#4CAF50") // Green
        }
    }
}

/**
 * Request để block IP
 */
data class BlockIPRequest(
    @SerializedName("ipAddress") val ipAddress: String,
    @SerializedName("blockType") val blockType: String, // TEMPORARY, PERMANENT
    @SerializedName("reason") val reason: String?,
    @SerializedName("durationHours") val durationHours: Int?, // Cho TEMPORARY
    @SerializedName("relatedUserId") val relatedUserId: Long?,
    @SerializedName("alertId") val alertId: Long?
)

/**
 * Request để unblock IP
 */
data class UnblockIPRequest(
    @SerializedName("reason") val reason: String?
)

/**
 * Thống kê IP Blocking
 */
data class IPBlockingStats(
    @SerializedName("totalActive") val totalActive: Long?,
    @SerializedName("total") val total: Long?
)
