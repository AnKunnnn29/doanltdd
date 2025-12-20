package com.example.doan.Utils

import com.example.doan.Models.Branch
import com.example.doan.Models.Category
import com.example.doan.Models.Order
import com.example.doan.Models.Product
import com.example.doan.Models.Store
import com.example.doan.Models.UserProfileDto

object DataCache {
    var branches: List<Branch>? = null
    var products: List<Product>? = null
    var categories: List<Category>? = null
    var stores: List<Store>? = null
    var userProfile: UserProfileDto? = null
    var orderHistory: List<Order>? = null
    var cartItemCount: Int? = null

    fun clearAll() {
        branches = null
        products = null
        categories = null
        stores = null
        userProfile = null
        orderHistory = null
        cartItemCount = null
    }
}
