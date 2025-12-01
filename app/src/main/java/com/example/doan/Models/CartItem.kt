package com.example.doan.Models

data class CartItem(
    var id: Long? = null,
    var drinkId: Long? = null,
    var drinkName: String? = null,
    var drinkImage: String? = null,
    var sizeId: Long? = null,
    var sizeName: String? = null,
    var quantity: Int? = null,
    var unitPrice: Double? = null,
    var totalPrice: Double? = null,
    var toppings: List<DrinkTopping>? = null,
    var note: String? = null
) {
    val product: Product
        get() = Product(
            id = drinkId?.toInt() ?: 0,
            name = drinkName,
            imageUrl = drinkImage,
            price = unitPrice ?: 0.0
        )
    
    fun setProduct(product: Product) {
        this.drinkId = product.id.toLong()
        this.drinkName = product.name
        this.drinkImage = product.imageUrl
        this.unitPrice = product.price
    }
}
