package com.example.doan.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Models.Store
import com.example.doan.R
import com.google.android.material.card.MaterialCardView

class StoreFilterAdapter(
    private val stores: List<Store?>,
    private var selectedStoreId: Long?,
    private val onStoreSelected: (Store?) -> Unit
) : RecyclerView.Adapter<StoreFilterAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: LinearLayout = view.findViewById(R.id.store_item_container)
        val cardIcon: MaterialCardView = view.findViewById(R.id.card_icon)
        val tvIcon: TextView = view.findViewById(R.id.tv_icon)
        val tvStoreName: TextView = view.findViewById(R.id.tv_store_name)
        val tvStoreAddress: TextView = view.findViewById(R.id.tv_store_address)
        val ivCheck: ImageView = view.findViewById(R.id.iv_check)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_store_filter, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val store = stores[position]
        
        if (store == null) {
            // "Tất cả chi nhánh" option
            holder.tvIcon.text = "🌐"
            holder.tvStoreName.text = "Tất cả chi nhánh"
            holder.tvStoreAddress.visibility = View.GONE
            
            val isSelected = selectedStoreId == null
            updateSelectionUI(holder, isSelected)
        } else {
            holder.tvIcon.text = "🏪"
            holder.tvStoreName.text = store.storeName ?: "Chi nhánh ${store.id}"
            
            if (!store.address.isNullOrEmpty()) {
                holder.tvStoreAddress.text = store.address
                holder.tvStoreAddress.visibility = View.VISIBLE
            } else {
                holder.tvStoreAddress.visibility = View.GONE
            }
            
            val isSelected = selectedStoreId == store.id.toLong()
            updateSelectionUI(holder, isSelected)
        }

        holder.container.setOnClickListener {
            val previousSelected = selectedStoreId
            selectedStoreId = store?.id?.toLong()
            
            // Update UI
            notifyDataSetChanged()
            
            // Callback
            onStoreSelected(store)
        }
    }

    private fun updateSelectionUI(holder: ViewHolder, isSelected: Boolean) {
        if (isSelected) {
            holder.ivCheck.visibility = View.VISIBLE
            holder.cardIcon.setCardBackgroundColor(
                holder.itemView.context.getColor(R.color.wine_primary)
            )
            holder.tvIcon.setTextColor(holder.itemView.context.getColor(R.color.white))
            holder.tvStoreName.setTextColor(holder.itemView.context.getColor(R.color.wine_primary))
        } else {
            holder.ivCheck.visibility = View.GONE
            holder.cardIcon.setCardBackgroundColor(
                holder.itemView.context.getColor(R.color.wine_neutral_light)
            )
            holder.tvIcon.setTextColor(holder.itemView.context.getColor(R.color.wine_dark_background))
            holder.tvStoreName.setTextColor(holder.itemView.context.getColor(R.color.wine_dark_background))
        }
    }

    override fun getItemCount() = stores.size
}
