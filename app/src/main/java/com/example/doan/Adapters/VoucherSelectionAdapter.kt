package com.example.doan.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Models.Voucher
import com.example.doan.R
import java.text.SimpleDateFormat
import java.util.*

class VoucherSelectionAdapter(
    private val vouchers: List<Voucher>,
    private val onVoucherSelected: (Voucher) -> Unit
) : RecyclerView.Adapter<VoucherSelectionAdapter.VoucherViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoucherViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_voucher_selection, parent, false)
        return VoucherViewHolder(view)
    }

    override fun onBindViewHolder(holder: VoucherViewHolder, position: Int) {
        holder.bind(vouchers[position])
    }

    override fun getItemCount() = vouchers.size

    inner class VoucherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView.findViewById(R.id.card_voucher)
        private val tvCode: TextView = itemView.findViewById(R.id.tv_voucher_code)
        private val tvDescription: TextView = itemView.findViewById(R.id.tv_voucher_description)
        private val tvDiscount: TextView = itemView.findViewById(R.id.tv_discount_value)
        private val tvMinOrder: TextView = itemView.findViewById(R.id.tv_min_order)
        private val tvExpiry: TextView = itemView.findViewById(R.id.tv_expiry_date)

        fun bind(voucher: Voucher) {
            tvCode.text = voucher.code
            tvDescription.text = voucher.description ?: "Voucher giảm giá"
            
            // Discount value - handle null
            val discountText = if (voucher.discountType == "PERCENT") {
                "Giảm ${voucher.discountValue?.toDouble() ?: 0.0}%"
            } else {
                val value = voucher.discountValue?.toDouble() ?: 0.0
                "Giảm ${String.format("%,.0f", value)} VNĐ"
            }
            tvDiscount.text = discountText
            
            // Min order - handle null
            val minOrderValue = voucher.minOrderValue?.toDouble() ?: 0.0
            tvMinOrder.text = if (minOrderValue > 0) {
                "Đơn tối thiểu: ${String.format("%,.0f", minOrderValue)} VNĐ"
            } else {
                "Không yêu cầu tối thiểu"
            }
            
            // Expiry date
            try {
                val endDate = inputFormat.parse(voucher.endDate)
                tvExpiry.text = "HSD: ${dateFormat.format(endDate)}"
            } catch (e: Exception) {
                tvExpiry.text = "HSD: ${voucher.endDate}"
            }
            
            cardView.setOnClickListener {
                onVoucherSelected(voucher)
            }
        }
    }
}
