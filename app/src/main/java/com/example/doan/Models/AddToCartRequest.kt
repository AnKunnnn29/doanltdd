package com.example.doan.Models

data class AddToCartRequest(
    var drinkId: Long,
    var sizeId: Long,
    var quantity: Int,
    var toppingIds: List<Long>? = null,
    var note: String? = null,
    var branchId: Long
)
