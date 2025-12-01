package com.example.doan.Models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Store(
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
    
    @SerializedName("imageUrl")
    var imageUrl: String? = null
) : Serializable {
    val name: String?
        get() = storeName
}
