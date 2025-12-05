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
import java.text.NumberFormat
import java.util.Locale

// YÊU CẦU: Tạo Adapter cho các mục trượt ngang trên trang Home.
// TÁC DỤNG: Cung cấp dữ liệu và quản lý các view cho danh sách sản phẩm kiểu carousel.
class ProductCarouselAdapter(private var products: List<Product>) :
    RecyclerView.Adapter<ProductCarouselAdapter.ProductCarouselViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductCarouselViewHolder {
        // TÁC DỤNG: Tạo ra một ViewHolder mới bằng cách inflate layout item_product_carousel.
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_carousel, parent, false)
        return ProductCarouselViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductCarouselViewHolder, position: Int) {
        // TÁC DỤNG: Gán dữ liệu từ đối tượng Product vào các view trong ViewHolder.
        holder.bind(products[position])
    }

    override fun getItemCount(): Int = products.size

    // TÁC DỤNG: Class này giữ các tham chiếu đến các view trong một item của RecyclerView.
    class ProductCarouselViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.iv_product_image_carousel)
        private val nameView: TextView = itemView.findViewById(R.id.tv_product_name_carousel)
        private val priceView: TextView = itemView.findViewById(R.id.tv_product_price_carousel)

        fun bind(product: Product) {
            // TÁC DỤNG: Gán dữ liệu cụ thể vào các view.
            nameView.text = product.name
            
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
            priceView.text = currencyFormat.format(product.price)

            // Sử dụng Glide để tải hình ảnh từ URL.
            Glide.with(itemView.context)
                .load(product.imageUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(imageView)
        }
    }
}
