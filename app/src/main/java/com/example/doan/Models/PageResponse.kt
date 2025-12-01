package com.example.doan.Models

import com.google.gson.annotations.SerializedName

data class PageResponse<T>(
    @SerializedName("content")
    var content: List<T>? = null,
    
    @SerializedName("totalElements")
    var totalElements: Long = 0,
    
    @SerializedName("totalPages")
    var totalPages: Int = 0,
    
    @SerializedName("size")
    var size: Int = 0,
    
    @SerializedName("number")
    var number: Int = 0,
    
    @SerializedName("first")
    var isFirst: Boolean = false,
    
    @SerializedName("last")
    var isLast: Boolean = false,
    
    @SerializedName("empty")
    var isEmpty: Boolean = false
)
