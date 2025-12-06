package com.example.doan.Adapters

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Models.Order
import com.example.doan.R
import com.google.android.material.chip.Chip
import java.text.NumberFormat
import java.util.Locale

/**
 * Adapter cho Manager để quản lý đơn hàng với các nút action
 */
class ManagerOrderAdapter(
    private val context: Context,
    private var orderList: List<Order>
) : RecyclerView.Adapter<ManagerOrderAdapter.ViewHolder>() {

    private var listener: OnOrderActionListener? = null

    interface OnOrderActionListener {
        fun onOrderClick(order: Order)
        fun onUpdateStatus(order: Order, newStatus: String)
        fun onCancelOrder(order: Order)
    }

    fun setOnOrderActionListener(listener: OnOrderActionListener) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_manager_order, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = orderList[position]

        // Order info
        holder.tvOrderId.text = "Đơn hàng #${order.id}"
        holder.tvCustomerName.text = order.userName ?: "Khách hàng"
        holder.tvStoreName.text = order.storeName ?: "Cửa hàng"

        // Date
        order.createdAt?.let { dateTimeStr ->
            try {
                if (dateTimeStr.contains("T")) {
                    val parts = dateTimeStr.split("T")
                    val datePart = parts[0]
                    val timePart = parts[1].substring(0, 5)
                    val dateParts = datePart.split("-")
                    holder.tvOrderDate.text = "${dateParts[2]}/${dateParts[1]}/${dateParts[0]} ${timePart}"
                } else {
                    holder.tvOrderDate.text = dateTimeStr
                }
            } catch (e: Exception) {
                holder.tvOrderDate.text = dateTimeStr
            }
        }

        // Total amount
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        holder.tvTotal.text = currencyFormat.format(order.finalPrice)

        // Order type
        holder.tvOrderType.text = when (order.type) {
            "DELIVERY" -> "Giao hàng"
            "PICKUP" -> "Lấy tại quầy"
            else -> order.type ?: ""
        }

        // Status chip
        val status = order.status ?: ""
        holder.chipStatus.text = order.getDisplayStatus()

        val (bgColor, textColor) = getStatusColors(status)
        holder.chipStatus.chipBackgroundColor = ColorStateList.valueOf(context.getColor(bgColor))
        holder.chipStatus.setTextColor(context.getColor(textColor))

        // Setup action buttons based on current status
        setupActionButtons(holder, order, status)

        // Click listener for details
        holder.itemView.setOnClickListener {
            listener?.onOrderClick(order)
        }
    }

    private fun setupActionButtons(holder: ViewHolder, order: Order, status: String) {
        holder.layoutActions.removeAllViews()

        when (status) {
            "PENDING" -> {
                // Chờ xử lý → có thể chuyển sang "Đang làm" hoặc "Hủy"
                addActionButton(holder, "Bắt đầu làm", R.color.status_making) {
                    listener?.onUpdateStatus(order, "MAKING")
                }
                addActionButton(holder, "Hủy đơn", R.color.status_canceled) {
                    listener?.onCancelOrder(order)
                }
            }
            "MAKING" -> {
                // Đang làm → có thể chuyển sang "Đang giao" (nếu delivery) hoặc "Sẵn sàng" (nếu pickup)
                if (order.type == "DELIVERY") {
                    addActionButton(holder, "Giao hàng", R.color.status_shipping) {
                        listener?.onUpdateStatus(order, "SHIPPING")
                    }
                } else {
                    addActionButton(holder, "Sẵn sàng", R.color.status_done) {
                        listener?.onUpdateStatus(order, "READY")
                    }
                }
                addActionButton(holder, "Hủy đơn", R.color.status_canceled) {
                    listener?.onCancelOrder(order)
                }
            }
            "SHIPPING" -> {
                // Đang giao → có thể chuyển sang "Hoàn thành"
                addActionButton(holder, "Hoàn thành", R.color.status_done) {
                    listener?.onUpdateStatus(order, "DONE")
                }
            }
            "READY" -> {
                // Sẵn sàng → có thể chuyển sang "Hoàn thành"
                addActionButton(holder, "Hoàn thành", R.color.status_done) {
                    listener?.onUpdateStatus(order, "DONE")
                }
            }
            "DONE", "CANCELED" -> {
                // Đã hoàn thành hoặc đã hủy → không có action
                holder.layoutActions.visibility = View.GONE
            }
        }
    }

    private fun addActionButton(holder: ViewHolder, text: String, colorRes: Int, onClick: () -> Unit) {
        val button = Button(context).apply {
            this.text = text
            setBackgroundColor(context.getColor(colorRes))
            setTextColor(context.getColor(android.R.color.white))
            setPadding(32, 16, 32, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 16
            }
            setOnClickListener { onClick() }
        }
        holder.layoutActions.addView(button)
        holder.layoutActions.visibility = View.VISIBLE
    }

    private fun getStatusColors(status: String): Pair<Int, Int> {
        return when (status) {
            "PENDING" -> Pair(R.color.status_pending_bg, R.color.status_pending)
            "MAKING" -> Pair(R.color.status_making_bg, R.color.status_making)
            "SHIPPING" -> Pair(R.color.status_shipping_bg, R.color.status_shipping)
            "READY" -> Pair(R.color.status_done_bg, R.color.status_done)
            "DONE" -> Pair(R.color.status_done_bg, R.color.status_done)
            "CANCELED" -> Pair(R.color.status_canceled_bg, R.color.status_canceled)
            else -> Pair(R.color.surface_variant, R.color.text_secondary)
        }
    }

    override fun getItemCount(): Int = orderList.size

    fun updateOrders(newOrders: List<Order>) {
        this.orderList = newOrders
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvOrderId: TextView = itemView.findViewById(R.id.tv_order_id)
        val tvCustomerName: TextView = itemView.findViewById(R.id.tv_customer_name)
        val tvStoreName: TextView = itemView.findViewById(R.id.tv_store_name)
        val tvOrderDate: TextView = itemView.findViewById(R.id.tv_order_date)
        val tvOrderType: TextView = itemView.findViewById(R.id.tv_order_type)
        val tvTotal: TextView = itemView.findViewById(R.id.tv_total)
        val chipStatus: Chip = itemView.findViewById(R.id.chip_status)
        val layoutActions: LinearLayout = itemView.findViewById(R.id.layout_actions)
    }
}
