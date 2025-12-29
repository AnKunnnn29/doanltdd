package com.example.doan.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Models.SpinRewardDto
import com.example.doan.Models.Voucher
import com.example.doan.R
import java.text.SimpleDateFormat
import java.util.*

class CombinedVoucherAdapter(
    private val items: List<Any>,
    private val onItemSelected: (Any) -> Unit
) : RecyclerView.Adapter<CombinedVoucherAdapter.VoucherViewHolder>() {

    companion object {
        private const val TYPE_SPIN = 0
        private const val TYPE_NORMAL = 1
    }

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is SpinRewardDto -> TYPE_SPIN
            else -> TYPE_NORMAL
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoucherViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_voucher_selection, parent, false)
        return VoucherViewHolder(view)
    }

    override fun onBindViewHolder(holder: VoucherViewHolder, position: Int) {
        when (val item = items[position]) {
            is SpinRewardDto -> holder.bindSpinVoucher(item)
            is Voucher -> holder.bindNormalVoucher(item)
        }
    }

    override fun getItemCount() = items.size

    inner class VoucherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView.findViewById(R.id.card_voucher)
        private val tvCode: TextView = itemView.findViewById(R.id.tv_voucher_code)
        private val tvDescription: TextView = itemView.findViewById(R.id.tv_voucher_description)
        private val tvDiscount: TextView = itemView.findViewById(R.id.tv_discount_value)
        private val tvMinOrder: TextView = itemView.findViewById(R.id.tv_min_order)
        private val tvExpiry: TextView = itemView.findViewById(R.id.tv_expiry_date)

        fun bindSpinVoucher(voucher: SpinRewardDto) {
            tvCode.text = voucher.voucherCode
            tvDescription.text = "🎡 Voucher từ vòng quay may mắn"
            tvDiscount.text = "Giảm ${voucher.discountPercent}%"
            tvMinOrder.text = "Không yêu cầu tối thiểu"
            
            // Parse created date
            try {
                val createdDate = inputFormat.parse(voucher.createdAt ?: "")
                tvExpiry.text = "Nhận: ${dateFormat.format(createdDate)}"
            } catch (e: Exception) {
                tvExpiry.text = "Voucher spin"
            }
            
            // Highlight spin voucher
            cardView.setCardBackgroundColor(itemView.context.getColor(R.color.wine_neutral_light))
            
            cardView.setOnClickListener {
                onItemSelected(voucher)
            }
        }

        fun bindNormalVoucher(voucher: Voucher) {
            tvCode.text = voucher.code
            tvDescription.text = voucher.description ?: "Voucher giảm giá"
            
            // Discount value
            val discountText = if (voucher.discountType == "PERCENT") {
                "Giảm ${voucher.discountValue?.toDouble() ?: 0.0}%"
            } else {
                val value = voucher.discountValue?.toDouble() ?: 0.0
                "Giảm ${String.format("%,.0f", value)} VNĐ"
            }
            tvDiscount.text = discountText
            
            // Min order
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
            
            // Normal background
            cardView.setCardBackgroundColor(itemView.context.getColor(android.R.color.white))
            
            cardView.setOnClickListener {
                onItemSelected(voucher)
            }
        }
    }
}
