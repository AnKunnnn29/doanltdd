package com.example.doan.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Models.OrderSummaryItem
import com.example.doan.R
import java.text.NumberFormat
import java.util.Locale

class OrderSummaryAdapter(
    private var items: List<OrderSummaryItem>
) : RecyclerView.Adapter<OrderSummaryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDrinkName: TextView = view.findViewById(R.id.tv_item_name)
        val tvSize: TextView = view.findViewById(R.id.tv_item_size)
        val tvQuantity: TextView = view.findViewById(R.id.tv_item_quantity)
        val tvPrice: TextView = view.findViewById(R.id.tv_item_price)
        val tvToppings: TextView = view.findViewById(R.id.tv_item_toppings)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_summary, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        
        holder.tvDrinkName.text = item.drinkName
        holder.tvSize.text = "Size: ${item.sizeName}"
        holder.tvQuantity.text = "x${item.quantity}"
        
        val totalPrice = item.unitPrice * item.quantity
        holder.tvPrice.text = "${formatter.format(totalPrice)} VNĐ"
        
        if (item.toppings.isNotEmpty()) {
            holder.tvToppings.visibility = View.VISIBLE
            holder.tvToppings.text = "Topping: ${item.toppings.joinToString(", ")}"
        } else {
            holder.tvToppings.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<OrderSummaryItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
