package com.example.doan.Adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Models.PeakHourAnalysis
import com.example.doan.R
import com.google.android.material.card.MaterialCardView
import java.text.NumberFormat
import java.util.Locale

class PeakHourAdapter(
    private val context: Context,
    private val peakHours: List<PeakHourAnalysis>
) : RecyclerView.Adapter<PeakHourAdapter.ViewHolder>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: MaterialCardView = view.findViewById(R.id.card_peak_hour)
        val tvTimeRange: TextView = view.findViewById(R.id.tv_time_range)
        val tvAvgOrders: TextView = view.findViewById(R.id.tv_avg_orders)
        val tvAvgRevenue: TextView = view.findViewById(R.id.tv_avg_revenue)
        val tvPeakLevel: TextView = view.findViewById(R.id.tv_peak_level)
        val tvRecommendedStaff: TextView = view.findViewById(R.id.tv_recommended_staff)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_peak_hour, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = peakHours[position]

        holder.tvTimeRange.text = item.timeRange ?: "--:-- - --:--"
        holder.tvAvgOrders.text = "${item.avgOrders ?: 0} đơn/ngày"
        holder.tvAvgRevenue.text = currencyFormat.format(item.avgRevenue ?: 0)
        holder.tvRecommendedStaff.text = "👥 ${item.recommendedStaff ?: 1} nhân viên"

        // Peak level styling
        when (item.peakLevel) {
            "VERY_HIGH" -> {
                holder.tvPeakLevel.text = "🔥 Rất cao"
                holder.tvPeakLevel.setTextColor(Color.parseColor("#D32F2F"))
                holder.cardView.strokeColor = Color.parseColor("#D32F2F")
            }
            "HIGH" -> {
                holder.tvPeakLevel.text = "⚡ Cao"
                holder.tvPeakLevel.setTextColor(Color.parseColor("#FF9800"))
                holder.cardView.strokeColor = Color.parseColor("#FF9800")
            }
            "MEDIUM" -> {
                holder.tvPeakLevel.text = "📊 Trung bình"
                holder.tvPeakLevel.setTextColor(Color.parseColor("#2196F3"))
                holder.cardView.strokeColor = Color.parseColor("#2196F3")
            }
            else -> {
                holder.tvPeakLevel.text = "📉 Thấp"
                holder.tvPeakLevel.setTextColor(Color.parseColor("#4CAF50"))
                holder.cardView.strokeColor = Color.parseColor("#4CAF50")
            }
        }
    }

    override fun getItemCount() = peakHours.size
}
