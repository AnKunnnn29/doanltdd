package com.example.doan.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Models.UserActivityLog
import com.example.doan.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * 🛡️ Activity Log Adapter
 * Hiển thị danh sách log hoạt động của user
 * Giới hạn tối đa 100 items để tối ưu hiệu năng
 */
class ActivityLogAdapter(
    private val items: MutableList<UserActivityLog>,
    private val onItemClick: (UserActivityLog) -> Unit,
    private val onBlockIPClick: ((UserActivityLog) -> Unit)? = null,  // Callback block IP
    private val onWhitelistIPClick: ((UserActivityLog) -> Unit)? = null  // Callback whitelist IP
) : RecyclerView.Adapter<ActivityLogAdapter.ViewHolder>() {

    companion object {
        private const val MAX_ITEMS = 100
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: CardView = view.findViewById(R.id.cardView)
        val viewRiskIndicator: View = view.findViewById(R.id.viewRiskIndicator)
        val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
        val tvUsername: TextView = view.findViewById(R.id.tvUsername)
        val tvActivityType: TextView = view.findViewById(R.id.tvActivityType)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvRiskLevel: TextView = view.findViewById(R.id.tvRiskLevel)
        val tvIpAddress: TextView = view.findViewById(R.id.tvIpAddress)
        val btnBlockIP: TextView = view.findViewById(R.id.btnBlockIP)
        val btnWhitelistIP: TextView = view.findViewById(R.id.btnWhitelistIP)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_activity_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        // Hiển thị username với badge nếu là user đã xóa
        if (item.isDeletedUser == true) {
            holder.tvUsername.text = "🗑️ ${item.username ?: "Unknown"}"
            holder.tvUsername.setTextColor(android.graphics.Color.parseColor("#9E9E9E")) // Gray
        } else {
            holder.tvUsername.text = item.username ?: "Unknown"
            holder.tvUsername.setTextColor(android.graphics.Color.parseColor("#212121")) // Dark
        }
        
        holder.tvActivityType.text = item.activityTypeDisplay ?: item.activityType
        holder.tvDescription.text = item.description ?: ""
        holder.tvRiskLevel.text = item.riskLevelDisplay ?: item.riskLevel
        holder.tvIpAddress.text = "IP: ${item.ipAddress ?: "N/A"}"
        
        // Format time
        holder.tvTime.text = formatTime(item.createdAt)
        
        // Set risk color
        val riskColor = item.getRiskColor()
        holder.viewRiskIndicator.setBackgroundColor(riskColor)
        holder.tvRiskLevel.setTextColor(riskColor)
        
        // Set icon
        holder.ivIcon.setImageResource(item.getActivityIcon())
        
        // Hiển thị nút Block IP nếu có IP và có callback (không hiển thị cho user đã xóa)
        if (!item.ipAddress.isNullOrEmpty() && onBlockIPClick != null && item.isDeletedUser != true) {
            holder.btnBlockIP.visibility = View.VISIBLE
            holder.btnBlockIP.setOnClickListener { onBlockIPClick.invoke(item) }
        } else {
            holder.btnBlockIP.visibility = View.GONE
        }
        
        // Hiển thị nút Whitelist IP nếu có IP và có callback (không hiển thị cho user đã xóa)
        if (!item.ipAddress.isNullOrEmpty() && onWhitelistIPClick != null && item.isDeletedUser != true) {
            holder.btnWhitelistIP.visibility = View.VISIBLE
            holder.btnWhitelistIP.setOnClickListener { onWhitelistIPClick.invoke(item) }
        } else {
            holder.btnWhitelistIP.visibility = View.GONE
        }
        
        holder.cardView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = items.size

    fun addItems(newItems: List<UserActivityLog>) {
        val startPos = items.size
        items.addAll(newItems)
        notifyItemRangeInserted(startPos, newItems.size)
        
        // Xóa items cũ nếu vượt quá giới hạn
        trimToMaxSize()
    }
    
    /**
     * 🔄 Update toàn bộ items (cho silent refresh)
     */
    fun updateItems(newItems: List<UserActivityLog>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    /**
     * 🔌 Thêm item mới vào đầu danh sách (cho WebSocket realtime)
     * Tự động xóa item cũ nhất nếu vượt quá 100 items
     */
    fun addItemToTop(item: UserActivityLog) {
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
