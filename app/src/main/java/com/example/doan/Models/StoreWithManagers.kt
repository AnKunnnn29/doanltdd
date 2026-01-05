package com.example.doan.Models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class StoreWithManagers(
    @SerializedName("id")
    var id: Int = 0,
    
    @SerializedName("storeName")
    var storeName: String? = null,
    
    @SerializedName("address")
    var address: String? = null,
    
    @SerializedName("latitude")
    var latitude: Double = 0.0,
    
    @SerializedName("longitude")
    var longitude: Double = 0.0,
    
    @SerializedName("openTime")
    var openTime: String? = null,
    
    @SerializedName("closeTime")
    var closeTime: String? = null,
    
    @SerializedName("phone")
    var phone: String? = null,
    
    @SerializedName("isActive")
    var isActive: Boolean? = true,
    
    // Danh sách managers quản lý store này
    @SerializedName("managers")
    var managers: List<User>? = null,
    
    // Danh sách admins (để liên hệ)
    @SerializedName("admins")
    var admins: List<User>? = null
) : Serializable
