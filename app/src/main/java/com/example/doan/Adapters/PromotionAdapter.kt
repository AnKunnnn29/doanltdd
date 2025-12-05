package com.example.doan.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doan.Models.Promotion
import com.example.doan.R

// YÊU CẦU: Tạo Adapter cho mục "Ưu đãi cho bạn".
// TÁC DỤNG: Cung cấp dữ liệu và quản lý các view cho danh sách ưu đãi trên màn hình Home.
class PromotionAdapter(private var promotions: List<Promotion>) : 
    RecyclerView.Adapter<PromotionAdapter.PromotionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PromotionViewHolder {
        // TÁC DỤNG: Tạo ra một ViewHolder mới bằng cách inflate layout item_promotion.
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_promotion, parent, false)
        return PromotionViewHolder(view)
    }

    override fun onBindViewHolder(holder: PromotionViewHolder, position: Int) {
        // TÁC DỤNG: Gán dữ liệu từ đối tượng Promotion vào các view trong ViewHolder.
        holder.bind(promotions[position])
    }

    override fun getItemCount(): Int = promotions.size

    fun updateData(newPromotions: List<Promotion>) {
        this.promotions = newPromotions
        notifyDataSetChanged() // Thông báo cho RecyclerView cập nhật lại toàn bộ danh sách
    }

    // TÁC DỤNG: Class này giữ các tham chiếu đến các view trong một item của RecyclerView.
    class PromotionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.iv_promotion_image)
        private val titleView: TextView = itemView.findViewById(R.id.tv_promotion_title)

        fun bind(promotion: Promotion) {
            // TÁC DỤNG: Gán dữ liệu cụ thể vào các view.
            titleView.text = promotion.title
            // Sử dụng Glide để tải hình ảnh từ URL.
            Glide.with(itemView.context)
                .load(promotion.imageUrl)
                .into(imageView)
        }
    }
}
