package com.example.doan.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doan.Models.Category
import com.example.doan.R

class ManagerCategoryAdapter(
    private val context: Context,
    private var categoryList: List<Category>,
    private val listener: OnCategoryActionListener
) : RecyclerView.Adapter<ManagerCategoryAdapter.ViewHolder>() {

    interface OnCategoryActionListener {
        fun onEditClick(category: Category)
        fun onDeleteClick(category: Category)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_manager_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categoryList[position]

        holder.tvName.text = category.name
        holder.tvDescription.text = category.description ?: "Không có mô tả"

        // Load image
        if (!category.image.isNullOrEmpty()) {
            Glide.with(context)
                .load(category.image)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_broken_image)
                .into(holder.imgCategory)
        } else {
            holder.imgCategory.setImageResource(R.drawable.ic_image_placeholder)
        }

        // Edit button
        holder.btnEdit.setOnClickListener {
            listener.onEditClick(category)
        }

        // Delete button
        holder.btnDelete.setOnClickListener {
            listener.onDeleteClick(category)
        }
    }

    override fun getItemCount(): Int = categoryList.size

    fun updateList(newList: List<Category>) {
        this.categoryList = newList
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgCategory: ImageView = itemView.findViewById(R.id.img_category)
        val tvName: TextView = itemView.findViewById(R.id.tv_category_name)
        val tvDescription: TextView = itemView.findViewById(R.id.tv_category_description)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btn_edit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)
    }
}
