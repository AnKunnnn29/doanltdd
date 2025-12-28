package com.example.doan.Models

/**
 * Models cho Group Chat trong Group Order
 */

// Enum cho loại tin nhắn
enum class GroupChatMessageType {
    TEXT,           // Tin nhắn văn bản thường
    SYSTEM,         // Thông báo hệ thống (ai vào/ra, khóa đơn...)
    ITEM_ADDED,     // Thông báo thêm món
    ITEM_REMOVED    // Thông báo xóa món
}

// DTO cho tin nhắn chat nhóm
data class GroupChatMessageDto(
    val id: Long?,
    val groupOrderId: Long?,
    val senderId: Long?,
    val senderName: String?,
    val senderAvatar: String?,
    val content: String?,
    val messageType: GroupChatMessageType?,
    val createdAt: String?
)

// Request gửi tin nhắn
data class SendGroupChatRequest(
    val content: String
)
