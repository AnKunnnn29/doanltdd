package com.example.doan.Adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doan.Models.LowStockWarning
import com.example.doan.R
import com.google.android.material.card.MaterialCardView

class LowStockAdapter(
    private val context: Context,
    private val warnings: List<LowStockWarning>
) : RecyclerView.Adapter<LowStockAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: MaterialCardView = view.findViewById(R.id.card_low_stock)
        val ivDrink: ImageView = view.findViewById(R.id.iv_drink)
        val tvDrinkName: TextView = view.findViewById(R.id.tv_drink_name)
        val tvSoldToday: TextView = view.findViewById(R.id.tv_sold_today)
        val tvAvgDaily: TextView = view.findViewById(R.id.tv_avg_daily)
        val tvVelocity: TextView = view.findViewById(R.id.tv_velocity)
        val tvWarningLevel: TextView = view.findViewById(R.id.tv_warning_level)
        val tvMessage: TextView = view.findViewById(R.id.tv_message)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_low_stock, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = warnings[position]

        holder.tvDrinkName.text = item.drinkName ?: "Unknown"
        holder.tvSoldToday.text = "Hôm nay: ${item.soldToday ?: 0}"
        holder.tvAvgDaily.text = "TB/ngày: ${item.avgDailySales ?: 0}"
        holder.tvVelocity.text = String.format("%.1f đơn/giờ", item.salesVelocity ?: 0.0)
        holder.tvMessage.text = item.message ?: ""

        // Load image
        Glide.with(context)
            .load(item.imageUrl)
            .placeholder(R.drawable.placeholder_drink)
            .error(R.drawable.placeholder_drink)
            .into(holder.ivDrink)

        // Warning level styling
        when (item.warningLevel) {
            "CRITICAL" -> {
                holder.tvWarningLevel.text = "🚨 NGUY CẤP"
                holder.tvWarningLevel.setTextColor(Color.parseColor("#D32F2F"))
                holder.cardView.strokeColor = Color.parseColor("#D32F2F")
                holder.cardView.setCardBackgroundColor(Color.parseColor("#FFEBEE"))
            }
            else -> {
                holder.tvWarningLevel.text = "⚠️ Cảnh báo"
                holder.tvWarningLevel.setTextColor(Color.parseColor("#FF9800"))
                holder.cardView.strokeColor = Color.parseColor("#FF9800")
                holder.cardView.setCardBackgroundColor(Color.parseColor("#FFF3E0"))
            }
        }
    }

    override fun getItemCount() = warnings.size
}
