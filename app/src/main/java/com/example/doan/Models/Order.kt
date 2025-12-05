package com.example.doan.Models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Order(
    @SerializedName("id")
    var id: Int = 0,
    
    @SerializedName("order_number")
    var orderNumber: String? = null,
    
    @SerializedName("created_at")
    var date: String? = null,
    
    @SerializedName("status")
    var status: String? = null,
    
    @SerializedName("total_price")
    var totalAmount: Double = 0.0,
    
    @SerializedName("final_price")
    var finalPrice: Double = 0.0,
    
    @SerializedName("address")
    var address: String? = null,
    
    @SerializedName("customer_name")
    var customerName: String? = null,
    
    @SerializedName("customer_phone")
    var customerPhone: String? = null,
    
    @SerializedName("payment_status")
    var paymentStatus: String? = null,
    
    @SerializedName("payment_method")
    var paymentMethod: String? = null,
    
    @SerializedName("createdAt")
    var createdAt: String? = null,
    
    @SerializedName("updatedAt")
    var updatedAt: String? = null,
    
    @SerializedName("items")
    var items: List<OrderItem>? = null
) : Serializable {
    
    fun getDisplayOrderNumber(): String {
        return if (orderNumber.isNullOrEmpty()) {
            id.toString()
        } else {
            orderNumber!!
        }
    }
}
