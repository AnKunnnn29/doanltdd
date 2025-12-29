package com.example.doan.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doan.Models.GroupChatMessageDto
import com.example.doan.Models.GroupChatMessageType
import com.example.doan.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter cho Group Chat trong Group Order
 * Hiển thị tin nhắn của các thành viên trong nhóm
 */
class GroupChatAdapter(
    private val currentUserId: Long
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val messages = mutableListOf<GroupChatMessageDto>()

    companion object {
        private const val VIEW_TYPE_SENT = 1      // Tin nhắn của mình
        private const val VIEW_TYPE_RECEIVED = 2  // Tin nhắn của người khác
        private const val VIEW_TYPE_SYSTEM = 3    // Thông báo hệ thống
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return when {
            message.messageType == GroupChatMessageType.SYSTEM ||
            message.messageType == GroupChatMessageType.ITEM_ADDED ||
            message.messageType == GroupChatMessageType.ITEM_REMOVED -> VIEW_TYPE_SYSTEM
            message.senderId == currentUserId -> VIEW_TYPE_SENT
            else -> VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_group_chat_sent, parent, false)
                SentMessageViewHolder(view)
            }
            VIEW_TYPE_SYSTEM -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_group_chat_system, parent, false)
                SystemMessageViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_group_chat_received, parent, false)
                ReceivedMessageViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
            is SystemMessageViewHolder -> holder.bind(message)
        }
    }

    override fun getItemCount(): Int = messages.size

    fun setMessages(newMessages: List<GroupChatMessageDto>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    fun addMessage(message: GroupChatMessageDto) {
        // Kiểm tra trùng lặp
        if (messages.none { it.id == message.id }) {
            messages.add(message)
            notifyItemInserted(messages.size - 1)
        }
    }

    fun getLastPosition(): Int = messages.size - 1

    // ViewHolder cho tin nhắn gửi đi (của mình)
    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)

        fun bind(message: GroupChatMessageDto) {
            tvMessage.text = message.content
            tvTime.text = formatTime(message.createdAt)
        }
    }

    // ViewHolder cho tin nhắn nhận được (của người khác)
    inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAvatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        private val tvSenderName: TextView = itemView.findViewById(R.id.tvSenderName)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)

        fun bind(message: GroupChatMessageDto) {
            tvSenderName.text = message.senderName ?: "Unknown"
            tvMessage.text = message.content
            tvTime.text = formatTime(message.createdAt)
            
            // Load avatar
            if (!message.senderAvatar.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(message.senderAvatar)
                    .placeholder(R.drawable.ic_person)
                    .circleCrop()
                    .into(ivAvatar)
            } else {
                ivAvatar.setImageResource(R.drawable.ic_person)
            }
        }
    }

    // ViewHolder cho thông báo hệ thống
    inner class SystemMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvSystemMessage)

        fun bind(message: GroupChatMessageDto) {
            tvMessage.text = message.content
        }
    }

    private fun formatTime(timestamp: String?): String {
        if (timestamp == null) return ""
        return try {
            // Thử nhiều format khác nhau
            val formats = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'"
            )
            val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            
            for (format in formats) {
                try {
                    val inputFormat = SimpleDateFormat(format, Locale.getDefault())
                    val date = inputFormat.parse(timestamp)
                    if (date != null) {
                        return outputFormat.format(date)
                    }
                } catch (e: Exception) {
                    continue
                }
            }
            ""
        } catch (e: Exception) {
            ""
        }
    }
}
