package com.example.doan.Models

import com.google.gson.annotations.SerializedName

/**
 * DTO cho thông báo từ server
 */
data class NotificationDto(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("title")
    val title: String?,
    
    @SerializedName("content")
    val content: String?,
    
    @SerializedName("type")
    val type: NotificationType?,  // Nullable để xử lý trường hợp server trả về null hoặc giá trị không hợp lệ
    
    @SerializedName("isRead")
    val isRead: Boolean = false,
    
    @SerializedName("relatedId")
    val relatedId: Long?,
    
    @SerializedName("createdAt")
    val createdAt: String = ""
)

/**
 * Loại thông báo
 */
enum class NotificationType {
    @SerializedName("ORDER_NEW")
    ORDER_NEW,           // Đơn hàng mới (cho Manager)
    
    @SerializedName("ORDER_STATUS")
    ORDER_STATUS,        // Cập nhật trạng thái đơn (cho User)
    
    @SerializedName("PROMOTION")
    PROMOTION,           // Thông báo khuyến mãi
    
    @SerializedName("SYSTEM")
    SYSTEM,              // Thông báo hệ thống
    
    @SerializedName("CUSTOM")
    CUSTOM,              // Thông báo tùy chỉnh từ Manager
    
    @SerializedName("LIVE_CHAT")
    LIVE_CHAT,           // Tin nhắn tư vấn mới
    
    @SerializedName("GROUP_CHAT")
    GROUP_CHAT           // Tin nhắn nhóm mới
}
