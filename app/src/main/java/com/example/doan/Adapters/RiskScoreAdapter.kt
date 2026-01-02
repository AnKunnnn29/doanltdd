package com.example.doan.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Models.UserRiskScore
import com.example.doan.R

/**
 * ⚠️ Risk Score Adapter
 * Hiển thị danh sách điểm rủi ro của users
 */
class RiskScoreAdapter(
    private val items: MutableList<UserRiskScore>,
    private val onItemClick: (UserRiskScore) -> Unit
) : RecyclerView.Adapter<RiskScoreAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: CardView = view.findViewById(R.id.cardView)
        val viewRiskIndicator: View = view.findViewById(R.id.viewRiskIndicator)
        val tvRiskEmoji: TextView = view.findViewById(R.id.tvRiskEmoji)
        val tvUsername: TextView = view.findViewById(R.id.tvUsername)
        val tvEmail: TextView = view.findViewById(R.id.tvEmail)
        val tvRiskScore: TextView = view.findViewById(R.id.tvRiskScore)
        val tvRiskLevel: TextView = view.findViewById(R.id.tvRiskLevel)
        val progressRisk: ProgressBar = view.findViewById(R.id.progressRisk)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvStats: TextView = view.findViewById(R.id.tvStats)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_risk_score, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        holder.tvRiskEmoji.text = item.getRiskEmoji()
        holder.tvUsername.text = item.username ?: "Unknown"
        holder.tvEmail.text = item.userEmail ?: ""
        holder.tvRiskScore.text = "${item.totalScore}/100"
        holder.tvRiskLevel.text = item.riskLevelDisplay ?: item.riskLevel
        
        // Progress bar
        holder.progressRisk.progress = item.totalScore
        
        // Set colors
        val riskColor = item.getRiskColor()
        holder.viewRiskIndicator.setBackgroundColor(riskColor)
        holder.tvRiskScore.setTextColor(riskColor)
        holder.tvRiskLevel.setTextColor(riskColor)
        
        // Status
        holder.tvStatus.text = when {
            item.userBlocked == true -> "🔒 Đã khóa"
            item.autoBlocked == true -> "⚠️ Tự động khóa"
            else -> "✅ Hoạt động"
        }
        
        // Stats summary
        val stats = buildString {
            if ((item.loginFailedCount ?: 0) > 0) append("🔐${item.loginFailedCount} ")
            if ((item.orderCancelCount ?: 0) > 0) append("❌${item.orderCancelCount} ")
            if ((item.paymentFailedCount ?: 0) > 0) append("💳${item.paymentFailedCount} ")
            if ((item.spamRequestCount ?: 0) > 0) append("🚫${item.spamRequestCount}")
        }
        holder.tvStats.text = stats.ifEmpty { "Không có vi phạm" }
        
        // Highlight high risk
        if (item.totalScore >= 60) {
            holder.cardView.setCardBackgroundColor(
                holder.itemView.context.getColor(R.color.risk_high_bg)
            )
        } else {
            holder.cardView.setCardBackgroundColor(
                holder.itemView.context.getColor(R.color.white)
            )
        }
        
        holder.cardView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = items.size

    fun addItems(newItems: List<UserRiskScore>) {
        val startPos = items.size
        items.addAll(newItems)
        notifyItemRangeInserted(startPos, newItems.size)
    }
}
