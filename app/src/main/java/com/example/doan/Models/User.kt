package com.example.doan.Models

data class User(
    var id: Int = 0,
    var username: String? = null,
    var fullName: String? = null,
    var phone: String? = null,
    var email: String? = null,
    var address: String? = null,
    var role: String? = null,
    var memberTier: String? = null,
    var points: Int = 0,
    var active: Boolean = true,
    var isBlocked: Boolean = false,
    var createdAt: String? = null,
    var updatedAt: String? = null,
    var orderCount: Int? = null,
    // Thông tin về stores được quản lý (chỉ cho Manager)
    var managedStores: List<ManagedStoreInfo>? = null,
    var isSuperManager: Boolean? = null
)

data class ManagedStoreInfo(
    var id: Long = 0,
    var storeName: String? = null
)
