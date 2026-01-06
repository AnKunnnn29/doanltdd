package com.example.doan.Models

import com.google.gson.annotations.SerializedName

/**
 * 🚨 Monitoring Alert Model
 * Cảnh báo bảo mật cho Admin/Manager
 */
data class MonitoringAlert(
    @SerializedName("id") val id: Long,
    
    // Target user info
    @SerializedName("targetUserId") val targetUserId: Long?,
    @SerializedName("targetUsername") val targetUsername: String?,
    @SerializedName("targetUserEmail") val targetUserEmail: String?,
    @SerializedName("targetUserFullName") val targetUserFullName: String?,
    @SerializedName("targetUserAvatarUrl") val targetUserAvatarUrl: String?,
    
    // IP address liên quan đến alert
    @SerializedName("ipAddress") val ipAddress: String?,
    
    @SerializedName("alertType") val alertType: String,
    @SerializedName("alertTypeDisplay") val alertTypeDisplay: String?,
    @SerializedName("severity") val severity: String,
    @SerializedName("severityDisplay") val severityDisplay: String?,
    
    @SerializedName("title") val title: String,
    @SerializedName("message") val message: String,
    
    @SerializedName("status") val status: String,
    @SerializedName("statusDisplay") val statusDisplay: String?,
    
    // Handler info
    @SerializedName("handledById") val handledById: Long?,
    @SerializedName("handledByUsername") val handledByUsername: String?,
    @SerializedName("handledAt") val handledAt: String?,
    @SerializedName("handlerNote") val handlerNote: String?,
    @SerializedName("actionTaken") val actionTaken: String?,
    @SerializedName("actionTakenDisplay") val actionTakenDisplay: String?,
    
    @SerializedName("activityLogId") val activityLogId: Long?,
    @SerializedName("notificationSent") val notificationSent: Boolean?,
    
    @SerializedName("createdAt") val createdAt: String?
) {
    fun getSeverityColor(): Int {
        return when (severity) {
            "LOW" -> android.graphics.Color.parseColor("#2196F3")      // Blue
            "MEDIUM" -> android.graphics.Color.parseColor("#FF9800")   // Orange
            "HIGH" -> android.graphics.Color.parseColor("#FF5722")     // Deep Orange
            "CRITICAL" -> android.graphics.Color.parseColor("#F44336") // Red
            else -> android.graphics.Color.parseColor("#9E9E9E")
        }
    }
    
    fun getSeverityEmoji(): String {
        return when (severity) {
            "LOW" -> "🔵"
            "MEDIUM" -> "🟡"
            "HIGH" -> "🟠"
            "CRITICAL" -> "🔴"
            else -> "⚪"
        }
    }
    
    fun getStatusColor(): Int {
        return when (status) {
            "PENDING" -> android.graphics.Color.parseColor("#FF9800")
            "REVIEWING" -> android.graphics.Color.parseColor("#2196F3")
            "RESOLVED" -> android.graphics.Color.parseColor("#4CAF50")
            "DISMISSED" -> android.graphics.Color.parseColor("#9E9E9E")
            else -> android.graphics.Color.parseColor("#9E9E9E")
        }
    }
}
