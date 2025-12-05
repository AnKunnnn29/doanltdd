package com.example.doan.Adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doan.Models.Category
import com.example.doan.R

class CategoryAdapter(
    private var categoryList: List<Category>,
    private val listener: OnCategoryClickListener
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var selectedPosition = 0 // Default "All" selected

    interface OnCategoryClickListener {
        fun onCategoryClick(category: Category)
    }

    fun updateCategories(newCategories: List<Category>) {
        this.categoryList = newCategories
        notifyDataSetChanged()
    }
    
    fun setSelectedCategory(categoryId: Int) {
        val index = categoryList.indexOfFirst { it.id == categoryId }
        if (index != -1) {
            val previousPosition = selectedPosition
            selectedPosition = index
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categoryList[position]
        holder.bind(category, position == selectedPosition)
    }

    override fun getItemCount(): Int = categoryList.size

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryImage: ImageView = itemView.findViewById(R.id.category_image)
        private val categoryName: TextView = itemView.findViewById(R.id.category_name)
        // Assuming root element is a CardView or Layout we can change background of, 
        // or we can traverse up if needed.
        // If item_category.xml root is ConstraintLayout/LinearLayout, we might need to change its background.
        // For now, let's assume we can change background of itemView or add a border/color change.

        fun bind(category: Category, isSelected: Boolean) {
            categoryName.text = category.name
            Glide.with(itemView.context)
                .load(category.image)
                .placeholder(R.drawable.ic_image_placeholder)
                .into(categoryImage)
                
            if (isSelected) {
                categoryName.setTextColor(itemView.context.getColor(R.color.wine_primary))
                categoryName.typeface = android.graphics.Typeface.DEFAULT_BOLD
                // Optional: Add visual indicator like border or background tint
                itemView.alpha = 1.0f
            } else {
                categoryName.setTextColor(itemView.context.getColor(R.color.wine_dark_background))
                categoryName.typeface = android.graphics.Typeface.DEFAULT
                itemView.alpha = 0.7f
            }

            itemView.setOnClickListener { 
                val previous = selectedPosition
                selectedPosition = bindingAdapterPosition
                notifyItemChanged(previous)
                notifyItemChanged(selectedPosition)
                listener.onCategoryClick(category) 
            }
        }
    }
}
