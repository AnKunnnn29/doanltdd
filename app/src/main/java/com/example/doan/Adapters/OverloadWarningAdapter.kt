package com.example.doan.Adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Models.OverloadWarning
import com.example.doan.R
import com.google.android.material.card.MaterialCardView

class OverloadWarningAdapter(
    private val context: Context,
    private val warnings: List<OverloadWarning>
) : RecyclerView.Adapter<OverloadWarningAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: MaterialCardView = view.findViewById(R.id.card_overload)
        val tvDate: TextView = view.findViewById(R.id.tv_date)
        val tvTimeRange: TextView = view.findViewById(R.id.tv_time_range)
        val tvExpectedOrders: TextView = view.findViewById(R.id.tv_expected_orders)
        val tvOverloadPercent: TextView = view.findViewById(R.id.tv_overload_percent)
        val tvSeverity: TextView = view.findViewById(R.id.tv_severity)
        val tvRecommendation: TextView = view.findViewById(R.id.tv_recommendation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_overload_warning, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = warnings[position]

        holder.tvDate.text = formatDate(item.date)
        holder.tvTimeRange.text = item.timeRange ?: "--:-- - --:--"
        holder.tvExpectedOrders.text = "Dự kiến: ${item.expectedOrders ?: 0} đơn"
        holder.tvOverloadPercent.text = String.format("+%.0f%% công suất", item.overloadPercent ?: 0.0)
        holder.tvRecommendation.text = item.recommendation ?: ""

        // Severity styling
        when (item.severity) {
            "CRITICAL" -> {
                holder.tvSeverity.text = "🚨 NGHIÊM TRỌNG"
                holder.tvSeverity.setTextColor(Color.parseColor("#D32F2F"))
                holder.cardView.strokeColor = Color.parseColor("#D32F2F")
                holder.cardView.setCardBackgroundColor(Color.parseColor("#FFEBEE"))
                holder.tvOverloadPercent.setTextColor(Color.parseColor("#D32F2F"))
            }
            else -> {
                holder.tvSeverity.text = "⚠️ Cảnh báo"
                holder.tvSeverity.setTextColor(Color.parseColor("#FF9800"))
                holder.cardView.strokeColor = Color.parseColor("#FF9800")
                holder.cardView.setCardBackgroundColor(Color.parseColor("#FFF3E0"))
                holder.tvOverloadPercent.setTextColor(Color.parseColor("#FF9800"))
            }
        }
    }

    private fun formatDate(dateStr: String?): String {
        if (dateStr.isNullOrEmpty()) return ""
        return try {
            val parts = dateStr.split("-")
            if (parts.size >= 3) "${parts[2]}/${parts[1]}/${parts[0]}" else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    override fun getItemCount() = warnings.size
}
