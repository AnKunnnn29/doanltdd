package com.example.doan.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Models.Store
import com.example.doan.R

class StoreSelectAdapter(
    private val context: Context,
    private var stores: List<Store>,
    private val onStoreSelected: (Store) -> Unit
) : RecyclerView.Adapter<StoreSelectAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_store_select, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val store = stores[position]
        holder.bind(store)
    }

    override fun getItemCount(): Int = stores.size

    fun updateStores(newStores: List<Store>) {
        this.stores = newStores
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvStoreName: TextView = itemView.findViewById(R.id.tv_store_name)
        private val tvStoreAddress: TextView = itemView.findViewById(R.id.tv_store_address)
        private val tvStoreHours: TextView = itemView.findViewById(R.id.tv_store_hours)

        fun bind(store: Store) {
            tvStoreName.text = store.storeName ?: "Chi nhánh"
            tvStoreAddress.text = store.address ?: "Địa chỉ không xác định"
            
            val openTime = store.openTime ?: "8:00"
            val closeTime = store.closeTime ?: "22:00"
            tvStoreHours.text = "$openTime - $closeTime"
            
            itemView.setOnClickListener {
                onStoreSelected(store)
            }
        }
    }
}
