package com.example.doan.Models

import com.google.gson.annotations.SerializedName

/**
 * 🔓 Whitelist IP Model
 * IP được phép truy cập với quyền Admin/Manager
 */
data class WhitelistedIP(
    @SerializedName("id") val id: Long,
    @SerializedName("ipAddress") val ipAddress: String,
    @SerializedName("description") val description: String?,
    @SerializedName("addedById") val addedById: Long?,
    @SerializedName("isActive") val isActive: Boolean?,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("updatedAt") val updatedAt: String?
)
