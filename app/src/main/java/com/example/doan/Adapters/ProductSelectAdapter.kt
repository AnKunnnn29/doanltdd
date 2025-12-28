package com.example.doan.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doan.Models.Product
import com.example.doan.R
import java.util.Locale

class ProductSelectAdapter(
    private val productList: List<Product>,
    private val onProductClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductSelectAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_product, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = productList[position]
        
        holder.productName.text = product.name
        holder.productPrice.text = String.format(Locale.getDefault(), "%,.0f VNĐ", product.price)
        
        Glide.with(holder.productImage.context)
            .load(product.imageUrl)
            .placeholder(R.drawable.ic_image_placeholder)
            .error(R.drawable.ic_broken_image)
            .into(holder.productImage)
        
        holder.itemView.setOnClickListener {
            onProductClick(product)
        }
    }

    override fun getItemCount(): Int = productList.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImage: ImageView = itemView.findViewById(R.id.product_image)
        val productName: TextView = itemView.findViewById(R.id.product_name)
        val productPrice: TextView = itemView.findViewById(R.id.product_price)
    }
}
