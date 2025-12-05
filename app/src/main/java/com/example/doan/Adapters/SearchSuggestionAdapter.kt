package com.example.doan.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doan.Models.Product
import com.example.doan.R

class SearchSuggestionAdapter(
    private var suggestions: List<Product>,
    private val onSuggestionClick: (Product) -> Unit
) : RecyclerView.Adapter<SearchSuggestionAdapter.SuggestionViewHolder>() {

    inner class SuggestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.suggestion_icon)
        val text: TextView = itemView.findViewById(android.R.id.text1)

        fun bind(product: Product) {
            text.text = product.name
            
            Glide.with(itemView.context)
                .load(product.imageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .centerCrop()
                .into(icon)

            itemView.setOnClickListener {
                onSuggestionClick(product)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_suggestion, parent, false)
        return SuggestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
        holder.bind(suggestions[position])
    }

    override fun getItemCount(): Int = suggestions.size

    fun updateSuggestions(newSuggestions: List<Product>) {
        suggestions = newSuggestions
        notifyDataSetChanged()
    }
}
