package com.example.doan.Models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Category(
    @SerializedName("id")
    var id: Int = 0,
    
    @SerializedName("name")
    var name: String? = null,
    
    @SerializedName("description")
    var description: String? = null,
    
    @SerializedName("imageUrl")
    var image: String? = null,
    
    @SerializedName("displayOrder")
    var displayOrder: Int = 0,
    
    @SerializedName("isActive")
    var isActive: Boolean = true
) : Serializable
