package com.example.doan.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doan.Models.BillItem
import com.example.doan.R
import java.util.Locale

class BillItemAdapter(
    private val items: List<BillItem>
) : RecyclerView.Adapter<BillItemAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivDrinkImage: ImageView = view.findViewById(R.id.iv_drink_image)
        val tvDrinkName: TextView = view.findViewById(R.id.tv_drink_name)
        val tvSizeName: TextView = view.findViewById(R.id.tv_size_name)
        val tvToppings: TextView = view.findViewById(R.id.tv_toppings)
        val tvNote: TextView = view.findViewById(R.id.tv_note)
        val tvQuantity: TextView = view.findViewById(R.id.tv_quantity)
        val tvItemPrice: TextView = view.findViewById(R.id.tv_item_price)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bill_preview, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        // Load image
        if (!item.drinkImage.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(item.drinkImage)
                .placeholder(R.drawable.placeholder_drink)
                .error(R.drawable.placeholder_drink)
                .into(holder.ivDrinkImage)
        } else {
            holder.ivDrinkImage.setImageResource(R.drawable.placeholder_drink)
        }

        // Drink name
        holder.tvDrinkName.text = item.drinkName ?: "Đồ uống"

        // Size
        holder.tvSizeName.text = "Size: ${item.sizeName ?: "M"}"

        // Toppings
        if (!item.toppings.isNullOrEmpty()) {
            holder.tvToppings.visibility = View.VISIBLE
            holder.tvToppings.text = "+ ${item.toppings.joinToString(", ")}"
        } else {
            holder.tvToppings.visibility = View.GONE
        }

        // Note
        if (!item.note.isNullOrEmpty()) {
            holder.tvNote.visibility = View.VISIBLE
            holder.tvNote.text = "Ghi chú: ${item.note}"
        } else {
            holder.tvNote.visibility = View.GONE
        }

        // Quantity
        holder.tvQuantity.text = "x${item.quantity ?: 1}"

        // Price
        val price = item.totalPrice ?: 0.0
        holder.tvItemPrice.text = String.format(Locale.getDefault(), "%,.0fđ", price)
    }

    override fun getItemCount(): Int = items.size
}
