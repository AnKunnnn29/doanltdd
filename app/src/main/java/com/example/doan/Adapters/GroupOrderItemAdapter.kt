package com.example.doan.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doan.Models.GroupOrderItemDto
import com.example.doan.R
import java.util.Locale

class GroupOrderItemAdapter(
    private var items: List<GroupOrderItemDto>,
    private val currentUserId: Long,
    private val isHost: Boolean = false,
    private val canModifyItems: Boolean = true, // Chỉ cho phép sửa/xóa khi phiên OPEN
    private val onEditItem: ((GroupOrderItemDto) -> Unit)? = null,
    private val onDeleteItem: ((GroupOrderItemDto) -> Unit)? = null
) : RecyclerView.Adapter<GroupOrderItemAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivDrinkImage: ImageView = view.findViewById(R.id.iv_drink_image)
        val tvDrinkName: TextView = view.findViewById(R.id.tv_drink_name)
        val tvUserName: TextView = view.findViewById(R.id.tv_user_name)
        val tvSize: TextView = view.findViewById(R.id.tv_size)
        val tvToppings: TextView = view.findViewById(R.id.tv_toppings)
        val tvQuantity: TextView = view.findViewById(R.id.tv_quantity)
        val tvPrice: TextView = view.findViewById(R.id.tv_price)
        val tvNote: TextView = view.findViewById(R.id.tv_note)
        val btnEdit: ImageView = view.findViewById(R.id.btn_edit_item)
        val btnDelete: ImageView = view.findViewById(R.id.btn_delete_item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group_order_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        holder.tvDrinkName.text = item.drinkName ?: "Đồ uống"
        holder.tvUserName.text = item.userName ?: "Thành viên"
        holder.tvSize.text = "Size: ${item.sizeName ?: "M"}"
        holder.tvQuantity.text = "x${item.quantity ?: 1}"
        holder.tvPrice.text = String.format(Locale.getDefault(), "%,.0f VNĐ", item.itemPrice ?: 0.0)
        
        // Toppings
        if (!item.toppingsSnapshot.isNullOrEmpty()) {
            holder.tvToppings.visibility = View.VISIBLE
            holder.tvToppings.text = "Topping: ${item.toppingsSnapshot}"
        } else {
            holder.tvToppings.visibility = View.GONE
        }
        
        // Note
        if (!item.note.isNullOrEmpty()) {
            holder.tvNote.visibility = View.VISIBLE
            holder.tvNote.text = "Ghi chú: ${item.note}"
        } else {
            holder.tvNote.visibility = View.GONE
        }

        // Drink image
        if (!item.drinkImage.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(item.drinkImage)
                .placeholder(R.drawable.placeholder_drink)
                .into(holder.ivDrinkImage)
        } else {
            holder.ivDrinkImage.setImageResource(R.drawable.placeholder_drink)
        }
        
        // Show edit/delete buttons only for own items or if host, and only when session is OPEN
        val isOwnItem = item.userId == currentUserId
        val canDelete = canModifyItems && (isOwnItem || isHost)
        val canEdit = canModifyItems && isOwnItem
        
        holder.btnEdit.visibility = if (canEdit) View.VISIBLE else View.GONE
        holder.btnDelete.visibility = if (canDelete) View.VISIBLE else View.GONE
        
        holder.btnEdit.setOnClickListener { onEditItem?.invoke(item) }
        holder.btnDelete.setOnClickListener { onDeleteItem?.invoke(item) }
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<GroupOrderItemDto>) {
        items = newItems
        notifyDataSetChanged()
    }
    
    fun getItemsByUser(userId: Long): List<GroupOrderItemDto> {
        return items.filter { it.userId == userId }
    }
    
    fun getTotalPrice(): Double {
        return items.sumOf { it.itemPrice ?: 0.0 }
    }
}
