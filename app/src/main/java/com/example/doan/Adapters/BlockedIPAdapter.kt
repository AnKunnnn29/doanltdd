package com.example.doan.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Models.BlockedIP
import com.example.doan.R
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 🚫 Adapter hiển thị danh sách IP bị chặn
 */
class BlockedIPAdapter(
    private var items: MutableList<BlockedIP>,
    private val onItemClick: (BlockedIP) -> Unit,
    private val onUnblockClick: (BlockedIP) -> Unit
) : RecyclerView.Adapter<BlockedIPAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_blocked_ip, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<BlockedIP>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun addItems(newItems: List<BlockedIP>) {
        val startPos = items.size
        items.addAll(newItems)
        notifyItemRangeInserted(startPos, newItems.size)
    }

    fun removeItem(blockedIP: BlockedIP) {
        val position = items.indexOfFirst { it.id == blockedIP.id }
        if (position != -1) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView.findViewById(R.id.cardBlockedIP)
        private val tvBlockTypeEmoji: TextView = itemView.findViewById(R.id.tvBlockTypeEmoji)
        private val tvIPAddress: TextView = itemView.findViewById(R.id.tvIPAddress)
        private val tvBlockType: TextView = itemView.findViewById(R.id.tvBlockType)
        private val tvReason: TextView = itemView.findViewById(R.id.tvReason)
        private val tvBlockedBy: TextView = itemView.findViewById(R.id.tvBlockedBy)
        private val tvBlockedTime: TextView = itemView.findViewById(R.id.tvBlockedTime)
        private val tvBlockedUntil: TextView = itemView.findViewById(R.id.tvBlockedUntil)
        private val tvBlockedCount: TextView = itemView.findViewById(R.id.tvBlockedCount)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val btnUnblock: ImageButton = itemView.findViewById(R.id.btnUnblock)
        private val layoutUnblockInfo: View = itemView.findViewById(R.id.layoutUnblockInfo)
        private val tvUnblockedBy: TextView = itemView.findViewById(R.id.tvUnblockedBy)
        private val tvUnblockReason: TextView = itemView.findViewById(R.id.tvUnblockReason)

        fun bind(item: BlockedIP) {
            // Block type emoji và text
            tvBlockTypeEmoji.text = item.getBlockTypeEmoji()
            tvBlockType.text = item.getBlockTypeText()
            
            // IP Address
            tvIPAddress.text = item.ipAddress
            
            // Reason
            tvReason.text = item.reason ?: "Không có lý do"
            
            // Blocked by
            tvBlockedBy.text = "👤 ${item.blockedByUsername ?: "System"}"
            
            // Blocked time
            item.createdAt?.let {
                tvBlockedTime.text = "🕐 ${formatDateTime(it)}"
                tvBlockedTime.visibility = View.VISIBLE
            } ?: run {
                tvBlockedTime.visibility = View.GONE
            }
            
            // Blocked until (cho TEMPORARY)
            if (item.blockType == "TEMPORARY" && item.blockedUntil != null) {
                tvBlockedUntil.text = "⏰ Hết hạn: ${formatDateTime(item.blockedUntil)}"
                tvBlockedUntil.visibility = View.VISIBLE
            } else if (item.blockType == "PERMANENT") {
                tvBlockedUntil.text = "⛔ Vĩnh viễn"
                tvBlockedUntil.visibility = View.VISIBLE
            } else {
                tvBlockedUntil.visibility = View.GONE
            }
            
            // Blocked requests count
            val count = item.blockedRequestsCount ?: 0
            if (count > 0) {
                tvBlockedCount.text = "🚫 $count requests bị chặn"
                tvBlockedCount.visibility = View.VISIBLE
            } else {
                tvBlockedCount.visibility = View.GONE
            }
            
            // Status
            tvStatus.text = item.getStatusText()
            tvStatus.setTextColor(item.getStatusColor())
            
            // Unblock button - chỉ hiện khi đang active
            val isActive = item.isCurrentlyBlocked == true || item.isActive == true
            btnUnblock.visibility = if (isActive) View.VISIBLE else View.GONE
            
            // Unblock info - hiện khi đã gỡ
            if (!isActive && item.unblockedAt != null) {
                layoutUnblockInfo.visibility = View.VISIBLE
                tvUnblockedBy.text = "Gỡ bởi: ${item.unblockedByUsername ?: "System"}"
                tvUnblockReason.text = "Lý do: ${item.unblockReason ?: "N/A"}"
            } else {
                layoutUnblockInfo.visibility = View.GONE
            }
            
            // Card background based on status
            if (isActive) {
                cardView.setCardBackgroundColor(itemView.context.getColor(R.color.blocked_ip_active_bg))
            } else {
                cardView.setCardBackgroundColor(itemView.context.getColor(R.color.blocked_ip_inactive_bg))
            }
            
            // Click listeners
            cardView.setOnClickListener { onItemClick(item) }
            btnUnblock.setOnClickListener { onUnblockClick(item) }
        }

        private fun formatDateTime(dateTimeStr: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val date = inputFormat.parse(dateTimeStr.substringBefore("."))
                date?.let { outputFormat.format(it) } ?: dateTimeStr
            } catch (e: Exception) {
                dateTimeStr.substringBefore("T")
            }
        }
    }
}
