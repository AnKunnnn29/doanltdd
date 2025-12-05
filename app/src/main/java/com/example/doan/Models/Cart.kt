package com.example.doan.Models

data class Cart(
    var id: Long? = null,
    var userId: Long? = null,
    var items: List<CartItem>? = null,
    var totalAmount: Double? = null
)
