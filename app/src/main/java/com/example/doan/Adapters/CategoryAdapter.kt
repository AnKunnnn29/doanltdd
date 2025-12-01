package com.example.doan.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doan.Models.Category
import com.example.doan.Network.RetrofitClient
import com.example.doan.R

class CategoryAdapter(
    private val categoryList: MutableList<Category>,
    private val listener: OnCategoryClickListener
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    interface OnCategoryClickListener {
        fun onCategoryClick(category: Category)
    }

    fun updateCategories(newList: List<Category>) {
        categoryList.clear()
        categoryList.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categoryList[position]
        holder.bind(category, listener)
    }

    override fun getItemCount(): Int = categoryList.size

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryImage: ImageView = itemView.findViewById(R.id.category_image)
        private val categoryName: TextView = itemView.findViewById(R.id.category_name)

        fun bind(category: Category, listener: OnCategoryClickListener) {
            categoryName.text = category.name
            
            val imageUrl = if (category.image?.startsWith("http") == true) {
                category.image
            } else {
                "${RetrofitClient.getBaseUrl()}/images/categories/${category.image}"
            }
            
            Glide.with(itemView.context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .into(categoryImage)

            itemView.setOnClickListener { listener.onCategoryClick(category) }
        }
    }
}
