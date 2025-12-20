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
    var orderCount: Int? = null
)
