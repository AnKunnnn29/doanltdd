package com.example.doan.Models

import com.google.gson.annotations.SerializedName

/**
 * 🛡️ User Risk Score Model
 * Điểm rủi ro tổng hợp của user
 */
data class UserRiskScore(
    @SerializedName("id") val id: Long,
    @SerializedName("userId") val userId: Long?,
    @SerializedName("username") val username: String?,
    @SerializedName("userEmail") val userEmail: String?,
    @SerializedName("userFullName") val userFullName: String?,
    @SerializedName("userAvatarUrl") val userAvatarUrl: String?,
    @SerializedName("userBlocked") val userBlocked: Boolean?,
    @SerializedName("userActive") val userActive: Boolean?,
    
    @SerializedName("totalScore") val totalScore: Int,
    @SerializedName("riskLevel") val riskLevel: String,
    @SerializedName("riskLevelDisplay") val riskLevelDisplay: String?,
    
    // Chi tiết các chỉ số
    @SerializedName("loginFailedCount") val loginFailedCount: Int?,
    @SerializedName("orderCancelCount") val orderCancelCount: Int?,
    @SerializedName("paymentFailedCount") val paymentFailedCount: Int?,
    @SerializedName("rateLimitHitCount") val rateLimitHitCount: Int?,
    @SerializedName("promotionAbuseCount") val promotionAbuseCount: Int?,
    @SerializedName("spamRequestCount") val spamRequestCount: Int?,
    
    // IP gần nhất của user
    @SerializedName("lastIpAddress") val lastIpAddress: String?,
    
    @SerializedName("lastScoreReset") val lastScoreReset: String?,
    
    // Admin note
    @SerializedName("adminNote") val adminNote: String?,
    @SerializedName("notedBy") val notedBy: Long?,
    @SerializedName("notedAt") val notedAt: String?,
    
    // Auto-block info
    @SerializedName("autoBlocked") val autoBlocked: Boolean?,
    @SerializedName("autoBlockedAt") val autoBlockedAt: String?,
    @SerializedName("autoBlockedReason") val autoBlockedReason: String?,
    
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("updatedAt") val updatedAt: String?
) {
    fun getRiskColor(): Int {
        return when (riskLevel) {
            "NORMAL" -> android.graphics.Color.parseColor("#4CAF50")
            "WARNING" -> android.graphics.Color.parseColor("#FF9800")
            "SUSPICIOUS" -> android.graphics.Color.parseColor("#F44336")
            "CRITICAL" -> android.graphics.Color.parseColor("#9C27B0")
            else -> android.graphics.Color.parseColor("#9E9E9E")
        }
    }
    
    fun getRiskEmoji(): String {
        return when (riskLevel) {
            "NORMAL" -> "🟢"
            "WARNING" -> "🟡"
            "SUSPICIOUS" -> "🔴"
            "CRITICAL" -> "⚫"
            else -> "⚪"
        }
    }
}
