package com.example.doan.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Models.Voucher
import com.example.doan.R
import com.google.android.material.materialswitch.MaterialSwitch
import java.text.SimpleDateFormat
import java.util.*

class VoucherManagerAdapter(
    private val vouchers: List<Voucher>,
    private val onEditClick: (Voucher) -> Unit,
    private val onDeleteClick: (Voucher) -> Unit,
    private val onToggleClick: (Voucher) -> Unit
) : RecyclerView.Adapter<VoucherManagerAdapter.VoucherViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoucherViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_voucher_manager, parent, false)
        return VoucherViewHolder(view)
    }

    override fun onBindViewHolder(holder: VoucherViewHolder, position: Int) {
        holder.bind(vouchers[position])
    }

    override fun getItemCount() = vouchers.size

    inner class VoucherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCode: TextView = itemView.findViewById(R.id.tv_voucher_code)
        private val tvDescription: TextView = itemView.findViewById(R.id.tv_voucher_description)
        private val tvDiscount: TextView = itemView.findViewById(R.id.tv_discount_info)
        private val tvMinOrder: TextView = itemView.findViewById(R.id.tv_min_order)
        private val tvUsage: TextView = itemView.findViewById(R.id.tv_usage_info)
        private val tvDates: TextView = itemView.findViewById(R.id.tv_dates)
        // FIX: Đổi từ Switch sang MaterialSwitch
        private val switchActive: MaterialSwitch = itemView.findViewById(R.id.switch_voucher_active)
        private val btnEdit: Button = itemView.findViewById(R.id.btn_edit_voucher)
        private val btnDelete: Button = itemView.findViewById(R.id.btn_delete_voucher)

        fun bind(voucher: Voucher) {
            tvCode.text = voucher.code
            tvDescription.text = voucher.description ?: "Không có mô tả"
            
            // Discount info - handle null
            val discountText = if (voucher.discountType == "PERCENT") {
                "Giảm ${voucher.discountValue?.toDouble() ?: 0.0}%"
            } else {
                val value = voucher.discountValue?.toDouble() ?: 0.0
                "Giảm ${String.format("%,.0f", value)} VNĐ"
            }
            tvDiscount.text = discountText
            
            // Min order - handle null
            val minOrderValue = voucher.minOrderValue?.toDouble() ?: 0.0
            tvMinOrder.text = "Đơn tối thiểu: ${String.format("%,.0f", minOrderValue)} VNĐ"
            
            // Usage info
            val usageText = if (voucher.usageLimit != null) {
                "Đã dùng: ${voucher.usedCount}/${voucher.usageLimit}"
            } else {
                "Đã dùng: ${voucher.usedCount} (Không giới hạn)"
            }
            tvUsage.text = usageText
            
            // Dates
            try {
                val startDate = inputFormat.parse(voucher.startDate)
                val endDate = inputFormat.parse(voucher.endDate)
                tvDates.text = "Từ ${dateFormat.format(startDate)} đến ${dateFormat.format(endDate)}"
            } catch (e: Exception) {
                tvDates.text = "Từ ${voucher.startDate} đến ${voucher.endDate}"
            }
            
            // Active status - remove listener first to avoid triggering on bind
            switchActive.setOnCheckedChangeListener(null)
            switchActive.isChecked = voucher.isActive
            switchActive.setOnCheckedChangeListener { _, isChecked ->
                // Only trigger if actually changed
                if (isChecked != voucher.isActive) {
                    onToggleClick(voucher)
                }
            }
            
            // Style mờ nếu inactive
            itemView.alpha = if (voucher.isActive) 1.0f else 0.5f
            
            // Buttons
            btnEdit.setOnClickListener { onEditClick(voucher) }
            btnDelete.setOnClickListener { onDeleteClick(voucher) }
        }
    }
}
