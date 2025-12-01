package com.example.doan.Adapters

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Models.Order
import com.example.doan.R
import com.google.android.material.chip.Chip
import java.text.NumberFormat
import java.util.Locale

class OrderAdapter(
    private val context: Context,
    private var orderList: List<Order>
) : RecyclerView.Adapter<OrderAdapter.ViewHolder>() {

    private var listener: OnOrderClickListener? = null

    interface OnOrderClickListener {
        fun onOrderClick(order: Order)
    }

    fun setOnOrderClickListener(listener: OnOrderClickListener) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = orderList[position]

        // Order ID
        holder.tvOrderId.text = "#${order.id}"

        // Customer name or phone
        val customerInfo = order.customerName 
            ?: order.customerPhone 
            ?: "Khách lẻ"
        holder.tvCustomer.text = customerInfo

        // Time
        order.createdAt?.let { dateTimeStr ->
            try {
                if (dateTimeStr.contains("T")) {
                    val parts = dateTimeStr.split("T")
                    val datePart = parts[0] // 2025-12-01
                    val timePart = parts[1].substring(0, 5) // 14:48
                    
                    val dateParts = datePart.split("-")
                    holder.tvTime.text = "$timePart ${dateParts[2]}/${dateParts[1]}"
                } else {
                    holder.tvTime.text = dateTimeStr
                }
            } catch (e: Exception) {
                holder.tvTime.text = dateTimeStr
            }
        }

        // Total amount
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        holder.tvTotal.text = currencyFormat.format(order.totalAmount)

        // Status chip
        val status = order.status ?: ""
        holder.chipStatus.text = getStatusText(status)
        
        // Set chip style based on status
        val (bgColor, textColor) = when (status) {
            "PENDING" -> Pair(R.color.status_pending_bg, R.color.status_pending)
            "MAKING" -> Pair(R.color.status_making_bg, R.color.status_making)
            "SHIPPING" -> Pair(R.color.status_shipping_bg, R.color.status_shipping)
            "DONE" -> Pair(R.color.status_done_bg, R.color.status_done)
            "CANCELED" -> Pair(R.color.status_canceled_bg, R.color.status_canceled)
            else -> Pair(R.color.surface_variant, R.color.text_secondary)
        }
        
        holder.chipStatus.chipBackgroundColor = ColorStateList.valueOf(context.getColor(bgColor))
        holder.chipStatus.setTextColor(context.getColor(textColor))

        // Payment status
        order.paymentStatus?.let {
            holder.tvPaymentStatus.text = it
            holder.tvPaymentStatus.visibility = View.VISIBLE
        } ?: run {
            holder.tvPaymentStatus.visibility = View.GONE
        }

        // Click listener
        holder.itemView.setOnClickListener {
            listener?.onOrderClick(order)
        }
    }

    override fun getItemCount(): Int = orderList.size

    fun updateOrders(newOrders: List<Order>) {
        this.orderList = newOrders
        notifyDataSetChanged()
    }

    private fun getStatusText(status: String): String {
        return when (status) {
            "PENDING" -> "Chờ xử lý"
            "MAKING" -> "Đang làm"
            "SHIPPING" -> "Đang giao"
            "DONE" -> "Hoàn thành"
            "CANCELED" -> "Đã hủy"
            else -> status
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvOrderId: TextView = itemView.findViewById(R.id.tv_order_id)
        val tvCustomer: TextView = itemView.findViewById(R.id.tv_customer)
        val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        val tvTotal: TextView = itemView.findViewById(R.id.tv_total)
        val tvPaymentStatus: TextView = itemView.findViewById(R.id.tv_payment_status)
        val chipStatus: Chip = itemView.findViewById(R.id.chip_status)
    }
}
