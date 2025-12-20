package com.example.doan.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.R

class BannerAdapter(
    private val banners: List<Int>,
    private val onBannerClick: ((Int) -> Unit)? = null
) : RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {

    class BannerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivBanner: ImageView = view.findViewById(R.id.iv_banner)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_banner, parent, false)
        return BannerViewHolder(view)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        val actualPosition = position % banners.size
        holder.ivBanner.setImageResource(banners[actualPosition])
        
        holder.itemView.setOnClickListener {
            onBannerClick?.invoke(actualPosition)
        }
    }

    override fun getItemCount(): Int = if (banners.isEmpty()) 0 else Int.MAX_VALUE
    
    fun getRealCount(): Int = banners.size
}
