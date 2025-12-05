package com.example.doan.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doan.Models.OrderItem
import com.example.doan.R
import java.text.NumberFormat
import java.util.Locale

class OrderDetailItemAdapter(
    private val context: Context,
    private var items: List<OrderItem>
) : RecyclerView.Adapter<OrderDetailItemAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_order_detail, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, position == items.size - 1)
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<OrderItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivDrinkImage: ImageView = itemView.findViewById(R.id.iv_drink_image)
        private val tvName: TextView = itemView.findViewById(R.id.tv_order_item_name)
        private val tvQuantity: TextView = itemView.findViewById(R.id.tv_order_item_quantity)
        private val tvPrice: TextView = itemView.findViewById(R.id.tv_order_item_price)
        private val tvSize: TextView = itemView.findViewById(R.id.tv_order_item_size)
        private val tvToppings: TextView = itemView.findViewById(R.id.tv_order_item_toppings)
        private val divider: View = itemView.findViewById(R.id.divider)

        fun bind(item: OrderItem, isLastItem: Boolean) {
            tvName.text = item.drinkName
            tvQuantity.text = "x${item.quantity}"

            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
            tvPrice.text = currencyFormat.format(item.price)

            // Load image using Glide
            Glide.with(context)
                .load(item.drinkImage)
                .placeholder(R.drawable.ic_launcher_background) // Optional placeholder
                .error(R.drawable.ic_launcher_background) // Optional error image
                .into(ivDrinkImage)

            // Size
            if (!item.sizeName.isNullOrEmpty()) {
                tvSize.text = "Size: ${item.sizeName}"
                tvSize.visibility = View.VISIBLE
            } else {
                tvSize.visibility = View.GONE
            }

            // Toppings
            val toppingsStr = item.toppings?.joinToString(", ") { it.name ?: "" } ?: ""
            if (toppingsStr.isNotEmpty()) {
                tvToppings.text = "Topping: $toppingsStr"
                tvToppings.visibility = View.VISIBLE
            } else {
                tvToppings.visibility = View.GONE
            }

            // Hide divider for the last item
            divider.visibility = if (isLastItem) View.GONE else View.VISIBLE
        }
    }
}
