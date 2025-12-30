package com.example.doan.Models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * Models cho tính năng Live Chat với Manager
 */

enum class ConversationStatus {
    WAITING, ACTIVE, RESOLVED, CLOSED
}

enum class SenderType {
    USER, MANAGER, SYSTEM
}

@Parcelize
data class LiveConversation(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("userId")
    val userId: Long,
    
    @SerializedName("userName")
    val userName: String?,
    
    @SerializedName("userAvatar")
    val userAvatar: String?,
    
    @SerializedName("managerId")
    val managerId: Long?,
    
    @SerializedName("managerName")
    val managerName: String?,
    
    @SerializedName("storeId")
    val storeId: Long?,
    
    @SerializedName("storeName")
    val storeName: String?,
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("subject")
    val subject: String?,
    
    @SerializedName("lastMessage")
    val lastMessage: String?,
    
    @SerializedName("lastMessageTime")
    val lastMessageTime: String?,
    
    @SerializedName("unreadUser")
    val unreadUser: Int = 0,
    
    @SerializedName("unreadManager")
    val unreadManager: Int = 0,
    
    @SerializedName("createdAt")
    val createdAt: String?,
    
    @SerializedName("messages")
    val messages: List<LiveMessage>? = null
) : Parcelable {
    
    fun getDisplayStatus(): String {
        return when (status) {
            "WAITING" -> "Đang chờ"
            "ACTIVE" -> "Đang chat"
            "RESOLVED" -> "Đã giải quyết"
            "CLOSED" -> "Đã đóng"
            else -> status
        }
    }
}

@Parcelize
data class LiveMessage(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("conversationId")
    val conversationId: Long,
    
    @SerializedName("senderId")
    val senderId: Long,
    
    @SerializedName("senderName")
    val senderName: String?,
    
    @SerializedName("senderAvatar")
    val senderAvatar: String?,
    
    @SerializedName("content")
    val content: String,
    
    @SerializedName("senderType")
    val senderType: String,
    
    @SerializedName("isRead")
    val isRead: Boolean = false,
    
    @SerializedName("createdAt")
    val createdAt: String?
) : Parcelable {
    
    fun isFromUser(): Boolean = senderType == "USER"
    fun isFromManager(): Boolean = senderType == "MANAGER"
    fun isSystem(): Boolean = senderType == "SYSTEM"
}

data class ConversationListItem(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("userId")
    val userId: Long,
    
    @SerializedName("userName")
    val userName: String?,
    
    @SerializedName("userAvatar")
    val userAvatar: String?,
    
    @SerializedName("storeId")
    val storeId: Long?,
    
    @SerializedName("storeName")
    val storeName: String?,
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("subject")
    val subject: String?,
    
    @SerializedName("lastMessage")
    val lastMessage: String?,
    
    @SerializedName("lastMessageTime")
    val lastMessageTime: String?,
    
    @SerializedName("unreadCount")
    val unreadCount: Int = 0,
    
    @SerializedName("createdAt")
    val createdAt: String?
)

// Request models
data class StartConversationRequest(
    @SerializedName("subject")
    val subject: String?,
    
    @SerializedName("initialMessage")
    val initialMessage: String?,
    
    @SerializedName("storeId")
    val storeId: Long? // Chi nhánh user chọn để tư vấn
)

data class SendLiveMessageRequest(
    @SerializedName("conversationId")
    val conversationId: Long,
    
    @SerializedName("content")
    val content: String
)
