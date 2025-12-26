package com.example.doan.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Models.SpinRewardDto
import com.example.doan.R
import com.google.android.material.button.MaterialButton

class SpinVoucherAdapter(
    private var vouchers: List<SpinRewardDto> = emptyList(),
    private val onCopyClick: (SpinRewardDto) -> Unit
) : RecyclerView.Adapter<SpinVoucherAdapter.VoucherViewHolder>() {

    fun updateVouchers(newVouchers: List<SpinRewardDto>) {
        vouchers = newVouchers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoucherViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_spin_voucher, parent, false)
        return VoucherViewHolder(view)
    }

    override fun onBindViewHolder(holder: VoucherViewHolder, position: Int) {
        holder.bind(vouchers[position])
    }

    override fun getItemCount(): Int = vouchers.size

    inner class VoucherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDiscount: TextView = itemView.findViewById(R.id.tvVoucherDiscount)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvVoucherDescription)
        private val tvVoucherCode: TextView = itemView.findViewById(R.id.tvVoucherCode)
        private val btnCopy: MaterialButton = itemView.findViewById(R.id.btnUseVoucher)

        fun bind(voucher: SpinRewardDto) {
            tvDiscount.text = "${voucher.discountPercent}%"
            
            tvDescription.text = when (voucher.discountPercent) {
                100 -> "Miễn phí 1 đơn hàng"
                else -> "Giảm ${voucher.discountPercent}% đơn hàng"
            }
            
            tvVoucherCode.text = voucher.voucherCode ?: ""
            
            btnCopy.text = "Copy"
            btnCopy.setOnClickListener {
                onCopyClick(voucher)
            }
        }
    }
}
