package com.example.doan.Models

import com.google.gson.annotations.SerializedName

/**
 * 🛡️ Monitoring Dashboard Model
 * Tổng quan dashboard giám sát
 */
data class MonitoringDashboard(
    // Alert counts
    @SerializedName("totalPendingAlerts") val totalPendingAlerts: Long?,
    @SerializedName("criticalAlerts") val criticalAlerts: Long?,
    @SerializedName("highAlerts") val highAlerts: Long?,
    @SerializedName("mediumAlerts") val mediumAlerts: Long?,
    @SerializedName("lowAlerts") val lowAlerts: Long?,
    
    // User risk counts
    @SerializedName("normalUsers") val normalUsers: Long?,
    @SerializedName("warningUsers") val warningUsers: Long?,
    @SerializedName("suspiciousUsers") val suspiciousUsers: Long?,
    @SerializedName("criticalUsers") val criticalUsers: Long?,
    
    // Activity stats
    @SerializedName("totalActivities24h") val totalActivities24h: Long?,
    @SerializedName("suspiciousActivities24h") val suspiciousActivities24h: Long?,
    @SerializedName("blockedUsers24h") val blockedUsers24h: Long?,
    
    // Top risky users
    @SerializedName("topRiskyUsers") val topRiskyUsers: List<UserRiskScore>?,
    
    // Recent alerts
    @SerializedName("recentAlerts") val recentAlerts: List<MonitoringAlert>?,
    
    // Recent activities
    @SerializedName("recentActivities") val recentActivities: List<UserActivityLog>?,
    
    // Stats maps
    @SerializedName("activityTypeStats") val activityTypeStats: Map<String, Long>?,
    @SerializedName("riskLevelStats") val riskLevelStats: Map<String, Long>?
)

/**
 * Request để xử lý alert
 */
data class HandleAlertRequest(
    @SerializedName("status") val status: String,
    @SerializedName("actionTaken") val actionTaken: String?,
    @SerializedName("note") val note: String?
)
