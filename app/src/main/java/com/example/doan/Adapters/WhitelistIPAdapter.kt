package com.example.doan.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Models.WhitelistedIP
import com.example.doan.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * 🔓 Whitelist IP Adapter
 * Hiển thị danh sách IP được phép truy cập Admin/Manager
 */
class WhitelistIPAdapter(
    private val items: MutableList<WhitelistedIP>,
    private val onItemClick: (WhitelistedIP) -> Unit,
    private val onRemoveClick: (WhitelistedIP) -> Unit
) : RecyclerView.Adapter<WhitelistIPAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: CardView = view.findViewById(R.id.cardView)
        val tvIpAddress: TextView = view.findViewById(R.id.tvIpAddress)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val tvCreatedAt: TextView = view.findViewById(R.id.tvCreatedAt)
        val btnRemove: ImageButton = view.findViewById(R.id.btnRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_whitelist_ip, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        holder.tvIpAddress.text = "🔓 ${item.ipAddress}"
        holder.tvDescription.text = item.description ?: "Không có mô tả"
        holder.tvCreatedAt.text = formatTime(item.createdAt)
        
        holder.cardView.setOnClickListener { onItemClick(item) }
        holder.btnRemove.setOnClickListener { onRemoveClick(item) }
    }

    override fun getItemCount() = items.size

    fun removeItem(item: WhitelistedIP) {
        val index = items.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    private fun formatTime(timeStr: String?): String {
        if (timeStr == null) return ""
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val date = inputFormat.parse(timeStr)
            "Thêm: ${outputFormat.format(date!!)}"
        } catch (e: Exception) {
            "Thêm: ${timeStr.take(16).replace("T", " ")}"
        }
    }
}
