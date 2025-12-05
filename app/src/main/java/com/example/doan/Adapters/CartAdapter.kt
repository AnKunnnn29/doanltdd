package com.example.doan.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doan.Models.CartItem
import com.example.doan.R
import java.util.Locale

class CartAdapter(
    private val context: Context,
    private var cartItems: MutableList<CartItem>,
    private val listener: OnCartItemChangeListener
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    interface OnCartItemChangeListener {
        fun onItemSelectedChanged()
        fun onItemDeleted(item: CartItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = cartItems[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = cartItems.size

    fun getSelectedItems(): List<CartItem> {
        return cartItems.filter { it.isSelected }
    }

    fun selectAll(isSelected: Boolean) {
        cartItems.forEach { it.isSelected = isSelected }
        notifyDataSetChanged()
        listener.onItemSelectedChanged()
    }

    fun deleteSelectedItems() {
        val itemsToRemove = cartItems.filter { it.isSelected }
        itemsToRemove.forEach { listener.onItemDeleted(it) }
    }
    
    fun updateItems(newItems: List<CartItem>){
        cartItems.clear()
        cartItems.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivImage: ImageView = itemView.findViewById(R.id.iv_cart_item_image)
        private val tvName: TextView = itemView.findViewById(R.id.tv_cart_item_name)
        private val tvDetails: TextView = itemView.findViewById(R.id.tv_cart_item_details)
        private val tvPrice: TextView = itemView.findViewById(R.id.tv_cart_item_price)
        private val tvQuantity: TextView = itemView.findViewById(R.id.tv_cart_item_quantity)
        private val btnDeleteItem: ImageButton = itemView.findViewById(R.id.btn_delete_item)
        private val cbSelectItem: CheckBox = itemView.findViewById(R.id.cb_select_item)

        fun bind(item: CartItem) {
            tvName.text = item.drinkName
            val details = mutableListOf<String>()
            item.sizeName?.let { details.add("Size: $it") }
            item.toppings?.let { toppings ->
                if (toppings.isNotEmpty()) {
                    details.add(toppings.joinToString { it.toppingName ?: "" })
                }
            }
            tvDetails.text = details.joinToString(", ")

            // Use unitPrice which is the price for one item including options
            val singleItemPrice = item.unitPrice ?: 0.0
            tvPrice.text = String.format(Locale.getDefault(), "%,.0f VNĐ", singleItemPrice)

            tvQuantity.text = "x${item.quantity}"

            Glide.with(context).load(item.drinkImage).placeholder(R.drawable.ic_image_placeholder).into(ivImage)

            cbSelectItem.setOnCheckedChangeListener(null) 
            cbSelectItem.isChecked = item.isSelected
            cbSelectItem.setOnCheckedChangeListener { _, isChecked ->
                item.isSelected = isChecked
                listener.onItemSelectedChanged()
            }

            btnDeleteItem.setOnClickListener {
                listener.onItemDeleted(item)
            }
        }
    }
}
