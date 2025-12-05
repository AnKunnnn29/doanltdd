package com.example.doan.Utils

import com.example.doan.Models.Branch
import com.example.doan.Models.Category
import com.example.doan.Models.Product

object DataCache {
    var branches: List<Branch>? = null
    var products: List<Product>? = null
    var categories: List<Category>? = null

    fun clearAll() {
        branches = null
        products = null
        categories = null
    }
}
