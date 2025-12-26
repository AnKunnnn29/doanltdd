package com.example.doan.Utils

import com.example.doan.Models.Branch
import com.example.doan.Models.Category
import com.example.doan.Models.Order
import com.example.doan.Models.Product
import com.example.doan.Models.Store
import com.example.doan.Models.UserProfileDto

/**
 * FIX Low #16: Thread-safe DataCache với @Volatile annotation
 * Đảm bảo visibility của các thay đổi giữa các threads
 */
object DataCache {
    @Volatile var branches: List<Branch>? = null
    @Volatile var products: List<Product>? = null
    @Volatile var categories: List<Category>? = null
    @Volatile var stores: List<Store>? = null
    @Volatile var userProfile: UserProfileDto? = null
    @Volatile var orderHistory: List<Order>? = null
    @Volatile var cartItemCount: Int? = null

    @Synchronized
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
