package com.example.doan.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Models.LiveMessage
import com.example.doan.R
import java.text.SimpleDateFormat
import java.util.*

class LiveChatAdapter(
    private val currentUserId: Long,
    private val isManager: Boolean = false
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val messages = mutableListOf<LiveMessage>()

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
        private const val VIEW_TYPE_SYSTEM = 3
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return when {
            message.isSystem() -> VIEW_TYPE_SYSTEM
            // Nếu là Manager: tin nhắn từ MANAGER là sent, từ USER là received
            // Nếu là User: tin nhắn từ USER là sent, từ MANAGER là received
            isManager -> if (message.isFromManager()) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
            else -> if (message.isFromUser()) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_user, parent, false)
                SentMessageViewHolder(view)
            }
            VIEW_TYPE_SYSTEM -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_system, parent, false)
                SystemMessageViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_bot, parent, false)
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

    fun setMessages(newMessages: List<LiveMessage>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    fun addMessage(message: LiveMessage) {
        // Kiểm tra trùng lặp - dựa vào id hoặc (senderId + content + senderType)
        val isDuplicate = messages.any { existing ->
            existing.id == message.id || 
            (existing.senderId == message.senderId && 
             existing.content == message.content &&
             existing.senderType == message.senderType)
        }
        
        if (!isDuplicate) {
            messages.add(message)
            notifyItemInserted(messages.size - 1)
        }
    }

    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvUserMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvUserTime)

        fun bind(message: LiveMessage) {
            tvMessage.text = message.content
            tvTime.text = formatTime(message.createdAt)
        }
    }

    inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvBotMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvBotTime)

        fun bind(message: LiveMessage) {
            tvMessage.text = message.content
            tvTime.text = formatTime(message.createdAt)
        }
    }

    inner class SystemMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvSystemMessage)

        fun bind(message: LiveMessage) {
            tvMessage.text = message.content
        }
    }

    private fun formatTime(timestamp: String?): String {
        if (timestamp == null) return ""
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val date = inputFormat.parse(timestamp)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            ""
        }
    }
}
