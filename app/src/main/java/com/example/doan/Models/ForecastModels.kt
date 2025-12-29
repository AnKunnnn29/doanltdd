package com.example.doan.Models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

/**
 * Models cho tính năng Dự báo doanh thu & Nguy cơ quá tải
 */

data class ForecastDto(
    @SerializedName("revenueForecast") val revenueForecast: RevenueForecast?,
    @SerializedName("peakHours") val peakHours: List<PeakHourAnalysis>?,
    @SerializedName("lowStockWarnings") val lowStockWarnings: List<LowStockWarning>?,
    @SerializedName("staffingRecommendations") val staffingRecommendations: List<StaffingRecommendation>?,
    @SerializedName("overloadWarnings") val overloadWarnings: List<OverloadWarning>?
)

data class RevenueForecast(
    @SerializedName("todayForecast") val todayForecast: Double?,
    @SerializedName("tomorrowForecast") val tomorrowForecast: Double?,
    @SerializedName("weekForecast") val weekForecast: Double?,
    @SerializedName("monthForecast") val monthForecast: Double?,
    @SerializedName("growthRate") val growthRate: Double?,
    @SerializedName("trend") val trend: String?,
    @SerializedName("dailyForecasts") val dailyForecasts: List<DailyForecast>?
)

data class DailyForecast(
    @SerializedName("date") val date: String?,
    @SerializedName("dayOfWeek") val dayOfWeek: String?,
    @SerializedName("forecastRevenue") val forecastRevenue: Double?,
    @SerializedName("forecastOrders") val forecastOrders: Long?,
    @SerializedName("confidence") val confidence: Double?
)

data class PeakHourAnalysis(
    @SerializedName("hour") val hour: Int?,
    @SerializedName("timeRange") val timeRange: String?,
    @SerializedName("avgOrders") val avgOrders: Long?,
    @SerializedName("avgRevenue") val avgRevenue: Double?,
    @SerializedName("peakLevel") val peakLevel: String?,
    @SerializedName("recommendedStaff") val recommendedStaff: Int?
)

data class LowStockWarning(
    @SerializedName("drinkId") val drinkId: Long?,
    @SerializedName("drinkName") val drinkName: String?,
    @SerializedName("imageUrl") val imageUrl: String?,
    @SerializedName("soldToday") val soldToday: Long?,
    @SerializedName("avgDailySales") val avgDailySales: Long?,
    @SerializedName("salesVelocity") val salesVelocity: Double?,
    @SerializedName("warningLevel") val warningLevel: String?,
    @SerializedName("message") val message: String?
)

data class StaffingRecommendation(
    @SerializedName("date") val date: String?,
    @SerializedName("dayOfWeek") val dayOfWeek: String?,
    @SerializedName("currentStaff") val currentStaff: Int?,
    @SerializedName("recommendedStaff") val recommendedStaff: Int?,
    @SerializedName("additionalNeeded") val additionalNeeded: Int?,
    @SerializedName("reason") val reason: String?,
    @SerializedName("hourlyBreakdown") val hourlyBreakdown: List<HourlyStaffing>?
)

data class HourlyStaffing(
    @SerializedName("hour") val hour: Int?,
    @SerializedName("timeRange") val timeRange: String?,
    @SerializedName("recommendedStaff") val recommendedStaff: Int?,
    @SerializedName("expectedOrders") val expectedOrders: Long?,
    @SerializedName("loadLevel") val loadLevel: String?
)

data class OverloadWarning(
    @SerializedName("date") val date: String?,
    @SerializedName("hour") val hour: Int?,
    @SerializedName("timeRange") val timeRange: String?,
    @SerializedName("expectedOrders") val expectedOrders: Long?,
    @SerializedName("maxCapacity") val maxCapacity: Long?,
    @SerializedName("overloadPercent") val overloadPercent: Double?,
    @SerializedName("severity") val severity: String?,
    @SerializedName("recommendation") val recommendation: String?
)
