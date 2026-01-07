package com.example.doan.Models

import com.google.gson.annotations.SerializedName

/**
 * 📦 Backup Activity Log của user đã xóa
 */
data class DeletedUserActivityLog(
    @SerializedName("id") val id: Long,
    @SerializedName("deletedUserId") val deletedUserId: Long?,
    @SerializedName("deletedUsername") val deletedUsername: String?,
    @SerializedName("originalLogId") val originalLogId: Long?,
    @SerializedName("activityType") val activityType: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("riskLevel") val riskLevel: String?,
    @SerializedName("ipAddress") val ipAddress: String?,
    @SerializedName("deviceInfo") val deviceInfo: String?,
    @SerializedName("userAgent") val userAgent: String?,
    @SerializedName("endpoint") val endpoint: String?,
    @SerializedName("requestMethod") val requestMethod: String?,
    @SerializedName("responseStatus") val responseStatus: Int?,
    @SerializedName("relatedId") val relatedId: Long?,
    @SerializedName("extraData") val extraData: String?,
    @SerializedName("activityCreatedAt") val activityCreatedAt: String?,
    @SerializedName("backupCreatedAt") val backupCreatedAt: String?,
    @SerializedName("note") val note: String?
)

/**
 * 📦 Backup Monitoring Alert của user đã xóa
 */
data class DeletedUserMonitoringAlert(
    @SerializedName("id") val id: Long,
    @SerializedName("deletedUserId") val deletedUserId: Long?,
    @SerializedName("deletedUsername") val deletedUsername: String?,
    @SerializedName("originalAlertId") val originalAlertId: Long?,
    @SerializedName("alertType") val alertType: String?,
    @SerializedName("severity") val severity: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("handledByUserId") val handledByUserId: Long?,
    @SerializedName("handledByUsername") val handledByUsername: String?,
    @SerializedName("handledAt") val handledAt: String?,
    @SerializedName("handlerNote") val handlerNote: String?,
    @SerializedName("actionTaken") val actionTaken: String?,
    @SerializedName("activityLogId") val activityLogId: Long?,
    @SerializedName("ipAddress") val ipAddress: String?,
    @SerializedName("notificationSent") val notificationSent: Boolean?,
    @SerializedName("alertCreatedAt") val alertCreatedAt: String?,
    @SerializedName("backupCreatedAt") val backupCreatedAt: String?,
    @SerializedName("note") val note: String?
)

/**
 * 📦 Backup Risk Score của user đã xóa
 */
data class DeletedUserRiskScore(
    @SerializedName("id") val id: Long,
    @SerializedName("deletedUserId") val deletedUserId: Long?,
    @SerializedName("deletedUsername") val deletedUsername: String?,
    @SerializedName("originalRiskScoreId") val originalRiskScoreId: Long?,
    @SerializedName("totalScore") val totalScore: Int?,
    @SerializedName("riskLevel") val riskLevel: String?,
    @SerializedName("loginFailedCount") val loginFailedCount: Int?,
    @SerializedName("orderCancelCount") val orderCancelCount: Int?,
    @SerializedName("paymentFailedCount") val paymentFailedCount: Int?,
    @SerializedName("rateLimitHitCount") val rateLimitHitCount: Int?,
    @SerializedName("promotionAbuseCount") val promotionAbuseCount: Int?,
    @SerializedName("spamRequestCount") val spamRequestCount: Int?,
    @SerializedName("lastIpAddress") val lastIpAddress: String?,
    @SerializedName("lastScoreReset") val lastScoreReset: String?,
    @SerializedName("adminNote") val adminNote: String?,
    @SerializedName("notedBy") val notedBy: String?,
    @SerializedName("notedAt") val notedAt: String?,
    @SerializedName("autoBlocked") val autoBlocked: Boolean?,
    @SerializedName("autoBlockedAt") val autoBlockedAt: String?,
    @SerializedName("autoBlockedReason") val autoBlockedReason: String?,
    @SerializedName("riskScoreCreatedAt") val riskScoreCreatedAt: String?,
    @SerializedName("riskScoreUpdatedAt") val riskScoreUpdatedAt: String?,
    @SerializedName("backupCreatedAt") val backupCreatedAt: String?,
    @SerializedName("note") val note: String?
)
