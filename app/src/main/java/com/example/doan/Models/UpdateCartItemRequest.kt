package com.example.doan.Models

data class UpdateCartItemRequest(
    val quantity: Int,
    val sizeId: Long? = null,
    val toppingIds: List<Long>? = null,
    val note: String? = null
)
