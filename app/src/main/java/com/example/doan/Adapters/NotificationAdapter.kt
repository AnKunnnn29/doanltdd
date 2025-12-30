package com.example.doan.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Models.NotificationDto
import com.example.doan.Models.NotificationType
import com.example.doan.R
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(
    private val notifications: List<NotificationDto>,
    private val onItemClick: (NotificationDto) -> Unit,
    private val onDeleteClick: (NotificationDto) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: CardView = view.findViewById(R.id.cardNotification)
        val ivIcon: ImageView = view.findViewById(R.id.ivNotificationIcon)
        val tvTitle: TextView = view.findViewById(R.id.tvNotificationTitle)
        val tvContent: TextView = view.findViewById(R.id.tvNotificationContent)
        val tvTime: TextView = view.findViewById(R.id.tvNotificationTime)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteNotification)
        val viewUnread: View = view.findViewById(R.id.viewUnreadIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = notifications[position]
        val context = holder.itemView.context
        
        holder.tvTitle.text = notification.title
        holder.tvContent.text = notification.content
        holder.tvTime.text = formatTime(notification.createdAt)
        
        // Icon theo loại thông báo
        val iconRes = when (notification.type) {
            NotificationType.ORDER_NEW -> R.drawable.ic_order_new
            NotificationType.ORDER_STATUS -> R.drawable.ic_order_status
            NotificationType.PROMOTION -> R.drawable.ic_promotion
            NotificationType.SYSTEM -> R.drawable.ic_system_notification
            NotificationType.CUSTOM -> R.drawable.ic_notification
        }
        holder.ivIcon.setImageResource(iconRes)
        
        // Hiển thị indicator chưa đọc
        holder.viewUnread.visibility = if (notification.isRead) View.GONE else View.VISIBLE
        
        // Background khác nhau cho đã đọc/chưa đọc
        val bgColor = if (notification.isRead) {
            ContextCompat.getColor(context, R.color.notification_read_bg)
        } else {
            ContextCompat.getColor(context, R.color.notification_unread_bg)
        }
        holder.cardView.setCardBackgroundColor(bgColor)
        
        // Click listeners
        holder.cardView.setOnClickListener { onItemClick(notification) }
        holder.btnDelete.setOnClickListener { onDeleteClick(notification) }
    }

    override fun getItemCount() = notifications.size

    private fun formatTime(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(dateString) ?: return dateString
            
            val now = Date()
            val diff = now.time - date.time
            val minutes = diff / (1000 * 60)
            val hours = minutes / 60
            val days = hours / 24
            
            when {
                minutes < 1 -> "Vừa xong"
                minutes < 60 -> "$minutes phút trước"
                hours < 24 -> "$hours giờ trước"
                days < 7 -> "$days ngày trước"
                else -> {
                    val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    outputFormat.format(date)
                }
            }
        } catch (e: Exception) {
            dateString
        }
    }
}
