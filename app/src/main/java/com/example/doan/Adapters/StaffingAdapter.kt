package com.example.doan.Adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Models.StaffingRecommendation
import com.example.doan.R
import com.google.android.material.card.MaterialCardView

class StaffingAdapter(
    private val context: Context,
    private val staffingList: List<StaffingRecommendation>
) : RecyclerView.Adapter<StaffingAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: MaterialCardView = view.findViewById(R.id.card_staffing)
        val tvDayOfWeek: TextView = view.findViewById(R.id.tv_day_of_week)
        val tvDate: TextView = view.findViewById(R.id.tv_date)
        val tvRecommendedStaff: TextView = view.findViewById(R.id.tv_recommended_staff)
        val tvReason: TextView = view.findViewById(R.id.tv_reason)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_staffing, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = staffingList[position]

        holder.tvDayOfWeek.text = item.dayOfWeek ?: ""
        holder.tvDate.text = formatDate(item.date)
        holder.tvRecommendedStaff.text = "${item.recommendedStaff ?: 1}"
        holder.tvReason.text = item.reason ?: ""

        // Highlight weekends
        val isWeekend = item.dayOfWeek?.contains("Thứ bảy", ignoreCase = true) == true ||
                item.dayOfWeek?.contains("Chủ nhật", ignoreCase = true) == true

        if (isWeekend) {
            holder.cardView.setCardBackgroundColor(Color.parseColor("#FFF8E1"))
            holder.cardView.strokeColor = Color.parseColor("#FF9800")
        } else {
            holder.cardView.setCardBackgroundColor(Color.WHITE)
            holder.cardView.strokeColor = Color.parseColor("#E0E0E0")
        }

        // Highlight high staff needs
        val staffCount = item.recommendedStaff ?: 1
        holder.tvRecommendedStaff.setTextColor(
            when {
                staffCount >= 5 -> Color.parseColor("#D32F2F")
                staffCount >= 3 -> Color.parseColor("#FF9800")
                else -> Color.parseColor("#4CAF50")
            }
        )
    }

    private fun formatDate(dateStr: String?): String {
        if (dateStr.isNullOrEmpty()) return ""
        return try {
            val parts = dateStr.split("-")
            if (parts.size >= 3) "${parts[2]}/${parts[1]}" else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    override fun getItemCount() = staffingList.size
}
