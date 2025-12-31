package com.example.doan.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Models.StoreWithManagers
import com.example.doan.Models.User
import com.example.doan.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip

class StoreWithManagersAdapter(
    private val context: Context,
    private var stores: List<StoreWithManagers>,
    private val onContactClick: (User, StoreWithManagers) -> Unit // Truyền cả store
) : RecyclerView.Adapter<StoreWithManagersAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_store_with_managers, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val store = stores[position]
        holder.bind(store)
    }

    override fun getItemCount(): Int = stores.size

    fun updateStores(newStores: List<StoreWithManagers>) {
        this.stores = newStores
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvStoreName: TextView = itemView.findViewById(R.id.tv_store_name)
        private val tvStoreAddress: TextView = itemView.findViewById(R.id.tv_store_address)
        private val tvStorePhone: TextView = itemView.findViewById(R.id.tv_store_phone)
        private val tvStoreHours: TextView = itemView.findViewById(R.id.tv_store_hours)
        private val chipStoreStatus: Chip = itemView.findViewById(R.id.chip_store_status)
        private val layoutManagers: LinearLayout = itemView.findViewById(R.id.layout_managers)
        private val tvNoManager: TextView = itemView.findViewById(R.id.tv_no_manager)

        fun bind(store: StoreWithManagers) {
            tvStoreName.text = store.storeName
            tvStoreAddress.text = store.address
            tvStorePhone.text = store.phone ?: "N/A"
            
            val openTime = store.openTime ?: "8:00"
            val closeTime = store.closeTime ?: "22:00"
            tvStoreHours.text = "$openTime - $closeTime"
            
            chipStoreStatus.text = "Hoạt động"
            chipStoreStatus.setChipBackgroundColorResource(R.color.success)
            
            // Hiển thị managers
            layoutManagers.removeAllViews()
            
            val managers = store.managers ?: emptyList()
            val admins = store.admins ?: emptyList()
            
            if (managers.isEmpty() && admins.isEmpty()) {
                tvNoManager.visibility = View.VISIBLE
                tvNoManager.text = "Chưa có người quản lý"
            } else {
                tvNoManager.visibility = View.GONE
                
                // Thêm managers
                managers.forEach { manager ->
                    addContactItem(manager, "Manager", store)
                }
                
                // Thêm admins
                admins.forEach { admin ->
                    addContactItem(admin, "Admin", store)
                }
            }
        }
        
        private fun addContactItem(user: User, role: String, store: StoreWithManagers) {
            val contactView = LayoutInflater.from(context)
                .inflate(R.layout.item_manager_contact, layoutManagers, false)
            
            val tvName = contactView.findViewById<TextView>(R.id.tv_manager_name)
            val tvRole = contactView.findViewById<Chip>(R.id.tv_manager_role)
            val tvPhone = contactView.findViewById<TextView>(R.id.tv_manager_phone)
            val btnContact = contactView.findViewById<MaterialButton>(R.id.btn_contact)
            
            tvName.text = user.fullName ?: user.username ?: "N/A"
            tvRole.text = role
            tvPhone.text = user.phone ?: "N/A"
            
            btnContact.setOnClickListener {
                onContactClick(user, store)
            }
            
            layoutManagers.addView(contactView)
        }
    }
}
