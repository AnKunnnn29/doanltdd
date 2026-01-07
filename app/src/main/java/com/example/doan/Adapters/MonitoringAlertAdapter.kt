package com.example.doan.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Models.MonitoringAlert
import com.example.doan.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * 🚨 Monitoring Alert Adapter
 * Hiển thị danh sách cảnh báo bảo mật
 * Giới hạn tối đa 100 items để tối ưu hiệu năng
 */
class MonitoringAlertAdapter(
    private val items: MutableList<MonitoringAlert>,
    private val onItemClick: (MonitoringAlert) -> Unit,
    private val onQuickAction: ((MonitoringAlert, String) -> Unit)? = null
) : RecyclerView.Adapter<MonitoringAlertAdapter.ViewHolder>() {

    companion object {
        private const val MAX_ITEMS = 100
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: CardView = view.findViewById(R.id.cardView)
        val viewSeverityIndicator: View = view.findViewById(R.id.viewSeverityIndicator)
        val tvSeverityEmoji: TextView = view.findViewById(R.id.tvSeverityEmoji)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvUsername: TextView = view.findViewById(R.id.tvUsername)
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvAlertType: TextView = view.findViewById(R.id.tvAlertType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_monitoring_alert, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        holder.tvSeverityEmoji.text = item.getSeverityEmoji()
        holder.tvTitle.text = item.title
        holder.tvUsername.text = "👤 ${item.targetUsername ?: "Unknown"}"
        holder.tvMessage.text = item.message
        holder.tvAlertType.text = item.alertTypeDisplay ?: item.alertType
        holder.tvStatus.text = item.statusDisplay ?: item.status
        holder.tvTime.text = formatTime(item.createdAt)
        
        // Set colors
        val severityColor = item.getSeverityColor()
        holder.viewSeverityIndicator.setBackgroundColor(severityColor)
        
        val statusColor = item.getStatusColor()
        holder.tvStatus.setTextColor(statusColor)
        
        // Highlight pending alerts
        if (item.status == "PENDING") {
            holder.cardView.setCardBackgroundColor(
                holder.itemView.context.getColor(R.color.alert_pending_bg)
            )
        } else {
            holder.cardView.setCardBackgroundColor(
                holder.itemView.context.getColor(R.color.white)
            )
        }
        
        holder.cardView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = items.size

    fun addItems(newItems: List<MonitoringAlert>) {
        val startPos = items.size
        items.addAll(newItems)
        notifyItemRangeInserted(startPos, newItems.size)
        trimToMaxSize()
    }
    
    /**
     * 🔄 Update toàn bộ items (cho silent refresh)
     */
    fun updateItems(newItems: List<MonitoringAlert>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    /**
     * 🔌 Thêm item mới vào đầu danh sách (cho WebSocket realtime)
     * Tự động xóa item cũ nhất nếu vượt quá 100 items
     */
    fun addItemToTop(item: MonitoringAlert) {
        items.add(0, item)
        notifyItemInserted(0)
        
        // Xóa item cuối nếu vượt quá giới hạn
        if (items.size > MAX_ITEMS) {
            val removeIndex = items.size - 1
            items.removeAt(removeIndex)
            notifyItemRemoved(removeIndex)
        }
    }

    /**
     * 🔌 Cập nhật item trong danh sách (cho WebSocket realtime)
     */
    fun updateItem(updatedItem: MonitoringAlert) {
        val index = items.indexOfFirst { it.id == updatedItem.id }
        if (index >= 0) {
            items[index] = updatedItem
            notifyItemChanged(index)
        }
    }

    /**
     * Xóa bớt items nếu vượt quá giới hạn MAX_ITEMS
     */
    private fun trimToMaxSize() {
        if (items.size > MAX_ITEMS) {
            val removeCount = items.size - MAX_ITEMS
            val startIndex = MAX_ITEMS
            for (i in 0 until removeCount) {
                items.removeAt(startIndex)
            }
            notifyItemRangeRemoved(startIndex, removeCount)
        }
    }

    /**
     * Xóa tất cả items
     */
    fun clearAll() {
        val size = items.size
        items.clear()
        notifyItemRangeRemoved(0, size)
    }

    private fun formatTime(timeStr: String?): String {
        if (timeStr == null) return ""
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
            val date = inputFormat.parse(timeStr)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            timeStr.take(16).replace("T", " ")
        }
    }
}
