package com.example.doan.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doan.Models.RevenueStatistics
import com.example.doan.R
import java.util.Locale

class TopSellingDrinkAdapter(
    private val context: Context,
    private val drinks: List<RevenueStatistics.TopSellingDrink>
) : RecyclerView.Adapter<TopSellingDrinkAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_top_selling_drink, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val drink = drinks[position]
        
        holder.tvRank.text = "#${position + 1}"
        holder.tvDrinkName.text = drink.drinkName
        holder.tvQuantity.text = "${drink.totalQuantity} ly"
        holder.tvRevenue.text = String.format(Locale.getDefault(), "%,.0f VNĐ", drink.totalRevenue)
        
        Glide.with(context)
            .load(drink.imageUrl)
            .placeholder(R.drawable.ic_image_placeholder)
            .into(holder.ivDrink)
    }

    override fun getItemCount(): Int = drinks.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvRank: TextView = itemView.findViewById(R.id.tv_rank)
        val ivDrink: ImageView = itemView.findViewById(R.id.iv_drink)
        val tvDrinkName: TextView = itemView.findViewById(R.id.tv_drink_name)
        val tvQuantity: TextView = itemView.findViewById(R.id.tv_quantity)
        val tvRevenue: TextView = itemView.findViewById(R.id.tv_revenue)
    }
}
