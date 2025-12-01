package com.example.doan.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Models.Store
import com.example.doan.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip

class StoreAdapter(
    private val context: Context,
    private var stores: List<Store>
) : RecyclerView.Adapter<StoreAdapter.StoreViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoreViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_store, parent, false)
        return StoreViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoreViewHolder, position: Int) {
        val store = stores[position]
        holder.bind(store)
    }

    override fun getItemCount(): Int = stores.size

    fun updateStores(newStores: List<Store>) {
        this.stores = newStores
        notifyDataSetChanged()
    }

    inner class StoreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvStoreName: TextView = itemView.findViewById(R.id.tv_store_name)
        private val tvStoreAddress: TextView = itemView.findViewById(R.id.tv_store_address)
        private val tvStorePhone: TextView = itemView.findViewById(R.id.tv_store_phone)
        private val tvStoreHours: TextView = itemView.findViewById(R.id.tv_store_hours)
        private val chipStoreStatus: Chip = itemView.findViewById(R.id.chip_store_status)
        private val btnEditStore: MaterialButton = itemView.findViewById(R.id.btn_edit_store)
        private val btnDeleteStore: MaterialButton = itemView.findViewById(R.id.btn_delete_store)

        fun bind(store: Store) {
            tvStoreName.text = store.name
            tvStoreAddress.text = store.address
            tvStorePhone.text = store.phone ?: "N/A"
            
            // Mock hours - replace with actual data if available
            tvStoreHours.text = "8:00 - 22:00"
            
            // Status
            chipStoreStatus.text = "Hoạt động"
            chipStoreStatus.setChipBackgroundColorResource(R.color.success)

            // Edit button
            btnEditStore.setOnClickListener {
                Toast.makeText(context, "Sửa: ${store.name}", Toast.LENGTH_SHORT).show()
            }

            // Delete button
            btnDeleteStore.setOnClickListener {
                Toast.makeText(context, "Xóa: ${store.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
