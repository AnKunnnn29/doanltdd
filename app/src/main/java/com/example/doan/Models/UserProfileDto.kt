package com.example.doan.Models

import com.google.gson.annotations.SerializedName

data class UserProfileDto(
    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("username")
    val username: String? = null,

    @SerializedName("fullName")
    val fullName: String? = null,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("phone")
    val phone: String? = null,

    @SerializedName("address")
    val address: String? = null,

    @SerializedName("memberTier")
    val memberTier: String? = null,

    @SerializedName("points")
    val points: Int? = null,

    @SerializedName("avatarUrl")
    val avatar: String? = null,

    @SerializedName("role")
    val role: String? = null
)
