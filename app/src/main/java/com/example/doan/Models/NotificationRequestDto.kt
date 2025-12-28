package com.example.doan.Models

import com.google.gson.annotations.SerializedName

data class NotificationRequestDto(
    @SerializedName("title")
    val title: String,

    @SerializedName("content")
    val content: String,

    @SerializedName("sendAll")
    val sendAll: Boolean,

    @SerializedName("userIds")
    val userIds: List<String>? = null
)
