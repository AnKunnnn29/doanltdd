package com.example.doan.Models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

// Enums
enum class GroupOrderStatus {
    OPEN, LOCKED, COMPLETED, CANCELLED, EXPIRED
}

// DTOs
data class GroupOrderDto(
    val id: Long?,
    val inviteCode: String?,
    val name: String?,
    val status: GroupOrderStatus?,
    val hostUserId: Long?,
    val hostUserName: String?,
    val storeId: Long?,
    val storeName: String?,
    val orderType: String?,
    val deliveryAddress: String?,
    val expiresAt: String?,
    val remainingSeconds: Long?, // Số giây còn lại từ server
    val maxMembers: Int?,
    val currentMemberCount: Int?,
    val totalPrice: Double?,
    val finalOrderId: Long?,
    val members: List<GroupOrderMemberDto>?,
    val items: List<GroupOrderItemDto>?,
    val createdAt: String?,
    val updatedAt: String?,
    /**
     * Flag để phân biệt phiên mới tạo (true) hay phiên cũ được trả về (false)
     * Dùng khi gọi API createGroupOrder
     */
    val isNewSession: Boolean? = null
)

data class GroupOrderMemberDto(
    val id: Long?,
    val userId: Long?,
    val userName: String?,
    val userAvatar: String?,
    val isHost: Boolean?,
    val joinedAt: String?,
    val itemCount: Int?
)

data class GroupOrderItemDto(
    val id: Long?,
    val userId: Long?,
    val userName: String?,
    val drinkId: Long?,
    val drinkName: String?,
    val drinkImage: String?,
    val sizeName: String?,
    val quantity: Int?,
    val unitPrice: Double?,
    val itemPrice: Double?,
    val note: String?,
    val toppingIds: List<Long>?,
    val toppingsSnapshot: String?
)


// Request DTOs
data class CreateGroupOrderRequest(
    val name: String? = null,
    val storeId: Long? = null,
    val orderType: String? = null,
    val deliveryAddress: String? = null,
    val maxMembers: Int? = 10,
    val expirationMinutes: Int? = 60
)

data class JoinGroupOrderRequest(
    val inviteCode: String
)

data class UpdateGroupOrderRequest(
    val name: String? = null,
    val storeId: Long? = null,
    val orderType: String? = null,
    val deliveryAddress: String? = null
)

data class AddGroupOrderItemRequest(
    val drinkId: Long,
    val quantity: Int,
    val sizeName: String? = null,
    val toppingIds: List<Long>? = null,
    val note: String? = null,
    val promotionCode: String? = null,
    val spinVoucherCode: String? = null
)

data class CheckoutGroupOrderRequest(
    val paymentMethod: String,
    val promotionCode: String? = null,
    val spinVoucherCode: String? = null
)

data class PreviewGroupOrderBillRequest(
    val paymentMethod: String? = null,
    val promotionCode: String? = null,
    val spinVoucherCode: String? = null,
    val shippingFee: Int? = null
)
