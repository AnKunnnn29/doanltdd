package com.example.doan.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doan.Models.Category
import com.example.doan.R

class CategoryAdapter(
    private var categoryList: List<Category>,
    private val listener: OnCategoryClickListener
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    interface OnCategoryClickListener {
        fun onCategoryClick(category: Category)
    }

    fun updateCategories(newCategories: List<Category>) {
        this.categoryList = newCategories
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categoryList[position]
        holder.bind(category)
    }

    override fun getItemCount(): Int = categoryList.size

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryImage: ImageView = itemView.findViewById(R.id.category_image)
        private val categoryName: TextView = itemView.findViewById(R.id.category_name)

        fun bind(category: Category) {
            categoryName.text = category.name
            Glide.with(itemView.context).load(category.image).placeholder(R.drawable.ic_image_placeholder).into(categoryImage)
            itemView.setOnClickListener { listener.onCategoryClick(category) }
        }
    }
}
