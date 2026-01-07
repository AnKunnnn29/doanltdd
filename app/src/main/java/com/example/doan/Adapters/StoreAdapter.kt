package com.example.doan.Adapters

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Models.Store
import com.example.doan.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip

class StoreAdapter(
    private val context: Context,
    private var stores: List<Store>,
    private val onStoreClick: (Store) -> Unit // Click listener
) : RecyclerView.Adapter<StoreAdapter.StoreViewHolder>() {

    private var distanceMap: Map<Int, String> = emptyMap()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoreViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_store, parent, false)
        return StoreViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoreViewHolder, position: Int) {
        val store = stores[position]
        val distanceInfo = distanceMap[store.id]
        holder.bind(store, distanceInfo, position == 0 && distanceMap.isNotEmpty())
    }

    override fun getItemCount(): Int = stores.size

    fun updateStores(newStores: List<Store>) {
        this.stores = newStores
        notifyDataSetChanged()
    }
    

    fun updateStoresWithDistance(newStores: List<Store>, distances: Map<Int, String>) {
        this.stores = newStores
        this.distanceMap = distances
        notifyDataSetChanged()
    }

    inner class StoreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvStoreName: TextView = itemView.findViewById(R.id.tv_store_name)
        private val tvStoreAddress: TextView = itemView.findViewById(R.id.tv_store_address)
        private val tvStorePhone: TextView = itemView.findViewById(R.id.tv_store_phone)
        private val tvStoreHours: TextView = itemView.findViewById(R.id.tv_store_hours)
        private val chipStoreStatus: Chip = itemView.findViewById(R.id.chip_store_status)
        private val tvDistance: TextView? = itemView.findViewById(R.id.tv_store_distance)
        private val btnViewOnGoogleMaps: MaterialButton = itemView.findViewById(R.id.btn_view_on_google_maps)

        fun bind(store: Store, distanceInfo: String?, isNearest: Boolean) {
            tvStoreName.text = store.storeName
            tvStoreAddress.text = store.address
            tvStorePhone.text = store.phone ?: "N/A"
            
            // Hiển thị giờ mở cửa
            val openTime = store.openTime ?: "8:00"
            val closeTime = store.closeTime ?: "22:00"
            tvStoreHours.text = "$openTime - $closeTime"
            
            // Hiển thị khoảng cách nếu có
            tvDistance?.let { tv ->
                if (distanceInfo != null) {
                    tv.visibility = View.VISIBLE
                    tv.text = "📍 $distanceInfo"
                } else {
                    tv.visibility = View.GONE
                }
            }
            
            // Status - highlight quán gần nhất
            if (isNearest) {
                chipStoreStatus.text = "Gần nhất ⭐"
                chipStoreStatus.setChipBackgroundColorResource(R.color.primary)
            } else {
                chipStoreStatus.text = "Hoạt động"
                chipStoreStatus.setChipBackgroundColorResource(R.color.success)
            }
            
            // Xử lý nút xem trên Google Maps
            btnViewOnGoogleMaps.setOnClickListener {
                openGoogleMaps(store)
            }
            
            itemView.setOnClickListener {
                onStoreClick(store)
            }
        }
        
        private fun openGoogleMaps(store: Store) {
            // Ưu tiên sử dụng tọa độ nếu có
            val lat = store.latitude
            val lng = store.longitude
            val storeName = store.storeName ?: "Cửa hàng"
            val address = store.address ?: ""
            
            val uri = if (lat != 0.0 && lng != 0.0) {
                // Mở Google Maps với tọa độ chính xác
                Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
            } else if (address.isNotEmpty()) {
                // Fallback: tìm kiếm theo địa chỉ
                val encodedAddress = Uri.encode("$storeName, $address")
                Uri.parse("https://www.google.com/maps/search/?api=1&query=$encodedAddress")
            } else {
                // Fallback cuối: tìm theo tên
                val encodedName = Uri.encode(storeName)
                Uri.parse("https://www.google.com/maps/search/?api=1&query=$encodedName")
            }
            
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        }
    }
}
