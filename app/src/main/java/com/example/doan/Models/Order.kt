package com.example.doan.Models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * FIX C7: Cập nhật SerializedName để khớp với Backend OrderDto
 * FIX C4: Chuyển từ Serializable sang Parcelable để tránh crash trên một số thiết bị
 * Backend trả về: id, userId, userName, storeId, storeName, type, address, 
 *                 pickupTime, status, totalPrice, discount, finalPrice, 
 *                 paymentMethod, promotionCode, items, createdAt, updatedAt
 */
@Parcelize
data class Order(
    @SerializedName("id")
    var id: Int = 0,
    
    @SerializedName("userId")
    var userId: Long? = null,
    
    @SerializedName("userName")
    var userName: String? = null,
    
    @SerializedName("storeId")
    var storeId: Long? = null,
    
    @SerializedName("storeName")
    var storeName: String? = null,
    
    @SerializedName("type")
    var type: String? = null,
    
    @SerializedName("status")
    var status: String? = null,
    
    // FIX C7: Backend trả về "totalPrice" không phải "total_price"
    @SerializedName("totalPrice")
    var totalPrice: Double = 0.0,
    
    @SerializedName("discount")
    var discount: Double = 0.0,
    
    // FIX C7: Backend trả về "finalPrice" không phải "final_price"
    @SerializedName("finalPrice")
    var finalPrice: Double = 0.0,
    
    @SerializedName("address")
    var address: String? = null,
    
    @SerializedName("pickupTime")
    var pickupTime: String? = null,
    
    // FIX C7: Backend trả về "paymentMethod" không phải "payment_method"
    @SerializedName("paymentMethod")
    var paymentMethod: String? = null,
    
    @SerializedName("promotionCode")
    var promotionCode: String? = null,
    
    // FIX C7: Backend trả về "createdAt" (đã đúng)
    @SerializedName("createdAt")
    var createdAt: String? = null,
    
    @SerializedName("updatedAt")
    var updatedAt: String? = null,
    
    @SerializedName("items")
    var items: List<OrderItem>? = null
) : Parcelable {
    
    // Backward compatibility - giữ lại các property cũ
    val totalAmount: Double get() = totalPrice
    val customerName: String? get() = userName
    
    fun getDisplayOrderNumber(): String {
        return "#${id}"
    }
    
    fun getDisplayStatus(): String {
        return when (status) {
            "PENDING" -> "Chờ xử lý"
            "MAKING" -> "Đang pha chế"
            "SHIPPING" -> "Đang giao"
            "READY" -> "Sẵn sàng"
            "DONE" -> "Hoàn thành"
            "CANCELED" -> "Đã hủy"
            else -> status ?: "Không xác định"
        }
    }
}
