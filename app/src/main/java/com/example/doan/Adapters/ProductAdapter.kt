package com.example.doan.Adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doan.Activities.ProductDetailActivity
import com.example.doan.Models.Product
import com.example.doan.R
import com.google.gson.Gson
import java.util.Locale

class ProductAdapter(private val productList: List<Product>) : 
    RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]
        
        holder.productName.text = product.name
        holder.productPrice.text = String.format(Locale.getDefault(), "%,.0f VNĐ", product.price)
        
        Glide.with(holder.productImage.context)
            .load(product.imageUrl)
            .placeholder(R.drawable.ic_image_placeholder)
            .error(R.drawable.ic_broken_image)
            .into(holder.productImage)
        
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, ProductDetailActivity::class.java)
            val gson = Gson()
            val productJson = gson.toJson(product)
            android.util.Log.d("ProductAdapter", "Sending product JSON: $productJson")
            intent.putExtra("product", productJson)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = productList.size

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImage: ImageView = itemView.findViewById(R.id.product_image)
        val productName: TextView = itemView.findViewById(R.id.product_name)
        val productPrice: TextView = itemView.findViewById(R.id.product_price)
    }
}
