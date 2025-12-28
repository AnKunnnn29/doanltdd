package com.example.doan.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doan.Models.ConversationListItem
import com.example.doan.R
import java.text.SimpleDateFormat
import java.util.*

class ConversationListAdapter(
    private val conversations: List<ConversationListItem>,
    private val onItemClick: (ConversationListItem) -> Unit
) : RecyclerView.Adapter<ConversationListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(conversations[position])
    }

    override fun getItemCount(): Int = conversations.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAvatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        private val tvLastMessage: TextView = itemView.findViewById(R.id.tvLastMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvUnreadCount: TextView = itemView.findViewById(R.id.tvUnreadCount)

        fun bind(conversation: ConversationListItem) {
            tvUserName.text = conversation.userName ?: "Khách hàng"
            tvLastMessage.text = conversation.lastMessage ?: conversation.subject ?: "Chưa có tin nhắn"
            tvTime.text = formatTime(conversation.lastMessageTime)
            
            // Status badge
            when (conversation.status) {
                "WAITING" -> {
                    tvStatus.text = "Chờ"
                    tvStatus.setBackgroundResource(R.drawable.bg_status_waiting)
                    tvStatus.setTextColor(itemView.context.getColor(android.R.color.white))
                }
                "ACTIVE" -> {
                    tvStatus.text = "Đang chat"
                    tvStatus.setBackgroundResource(R.drawable.bg_status_active)
                    tvStatus.setTextColor(itemView.context.getColor(android.R.color.white))
                }
                "CLOSED" -> {
                    tvStatus.text = "Đã đóng"
                    tvStatus.setBackgroundResource(R.drawable.bg_status_closed)
                    tvStatus.setTextColor(itemView.context.getColor(android.R.color.white))
                }
                else -> {
                    tvStatus.visibility = View.GONE
                }
            }
            tvStatus.visibility = View.VISIBLE
            
            // Unread count
            if (conversation.unreadCount > 0) {
                tvUnreadCount.visibility = View.VISIBLE
                tvUnreadCount.text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString()
            } else {
                tvUnreadCount.visibility = View.GONE
            }
            
            // Avatar
            if (!conversation.userAvatar.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(conversation.userAvatar)
                    .placeholder(R.drawable.ic_person)
                    .circleCrop()
                    .into(ivAvatar)
            } else {
                ivAvatar.setImageResource(R.drawable.ic_person)
            }
            
            itemView.setOnClickListener { onItemClick(conversation) }
        }

        private fun formatTime(timestamp: String?): String {
            if (timestamp == null) return ""
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val date = inputFormat.parse(timestamp) ?: return ""
                
                val now = Calendar.getInstance()
                val messageTime = Calendar.getInstance().apply { time = date }
                
                when {
                    now.get(Calendar.DATE) == messageTime.get(Calendar.DATE) -> {
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                    }
                    now.get(Calendar.DATE) - messageTime.get(Calendar.DATE) == 1 -> {
                        "Hôm qua"
                    }
                    else -> {
                        SimpleDateFormat("dd/MM", Locale.getDefault()).format(date)
                    }
                }
            } catch (e: Exception) {
                ""
            }
        }
    }
}
