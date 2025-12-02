package com.example.doan.Models

data class AddToCartRequest(
    var drinkId: Long,
    var sizeId: Long,
    var quantity: Int,
    var toppingIds: List<Long>? = null,
    var note: String? = null
    // Note: branchId removed - backend doesn't support it
    // Branch selection should be done at checkout, not when adding to cart
)
