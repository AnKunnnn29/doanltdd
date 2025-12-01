package com.example.doan.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doan.Models.CartItem
import com.example.doan.R
import com.example.doan.Utils.CartManager
import com.google.android.material.button.MaterialButton
import java.util.Locale

class CartAdapter(
    private val context: Context,
    private val cartItems: List<CartItem>,
    private val listener: OnCartChangeListener
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    interface OnCartChangeListener {
        fun onCartChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = cartItems[position]
        holder.bind(item, position)
    }

    override fun getItemCount(): Int = cartItems.size

    inner class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgProduct: ImageView = itemView.findViewById(R.id.img_product)
        private val tvName: TextView = itemView.findViewById(R.id.tv_product_name)
        private val tvSize: TextView = itemView.findViewById(R.id.tv_product_size)
        private val tvPrice: TextView = itemView.findViewById(R.id.tv_product_price)
        private val tvQuantity: TextView = itemView.findViewById(R.id.tv_quantity)
        private val btnMinus: MaterialButton = itemView.findViewById(R.id.btn_minus)
        private val btnPlus: MaterialButton = itemView.findViewById(R.id.btn_plus)
        private val btnRemove: MaterialButton = itemView.findViewById(R.id.btn_remove)

        fun bind(item: CartItem, position: Int) {
            Glide.with(context).load(item.product.imageUrl).into(imgProduct)
            tvName.text = item.product.name
            tvSize.text = "Size: ${item.sizeName}"
            tvPrice.text = String.format(Locale.getDefault(), "%,.0f VNĐ", item.totalPrice)
            tvQuantity.text = item.quantity.toString()

            btnMinus.setOnClickListener {
                if ((item.quantity ?: 0) > 1) {
                    CartManager.getInstance().updateQuantity(position, (item.quantity ?: 0) - 1)
                    notifyItemChanged(position)
                    listener.onCartChanged()
                }
            }

            btnPlus.setOnClickListener {
                CartManager.getInstance().updateQuantity(position, (item.quantity ?: 0) + 1)
                notifyItemChanged(position)
                listener.onCartChanged()
            }

            btnRemove.setOnClickListener {
                CartManager.getInstance().removeItem(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, cartItems.size)
                listener.onCartChanged()
            }
        }
    }
}
