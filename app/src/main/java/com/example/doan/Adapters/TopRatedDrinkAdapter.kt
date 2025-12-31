package com.example.doan.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doan.Models.DashboardSummary
import com.example.doan.R
import com.google.android.material.card.MaterialCardView

class TopRatedDrinkAdapter(
    private val context: Context,
    private val drinks: List<DashboardSummary.TopRatedDrink>
) : RecyclerView.Adapter<TopRatedDrinkAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_top_rated_drink, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(drinks[position], position + 1)
    }

    override fun getItemCount(): Int = drinks.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.card_drink)
        private val tvRank: TextView = itemView.findViewById(R.id.tv_rank)
        private val ivDrink: ImageView = itemView.findViewById(R.id.iv_drink)
        private val tvDrinkName: TextView = itemView.findViewById(R.id.tv_drink_name)
        private val ratingBar: RatingBar = itemView.findViewById(R.id.rating_bar)
        private val tvRating: TextView = itemView.findViewById(R.id.tv_rating)
        private val tvReviewCount: TextView = itemView.findViewById(R.id.tv_review_count)

        fun bind(drink: DashboardSummary.TopRatedDrink, rank: Int) {
            // Rank badge
            tvRank.text = "#$rank"
            when (rank) {
                1 -> {
                    tvRank.setBackgroundResource(R.drawable.bg_rank_gold)
                    tvRank.setTextColor(context.getColor(R.color.white))
                }
                2 -> {
                    tvRank.setBackgroundResource(R.drawable.bg_rank_silver)
                    tvRank.setTextColor(context.getColor(R.color.white))
                }
                3 -> {
                    tvRank.setBackgroundResource(R.drawable.bg_rank_bronze)
                    tvRank.setTextColor(context.getColor(R.color.white))
                }
                else -> {
                    tvRank.setBackgroundResource(R.drawable.bg_rank_default)
                    tvRank.setTextColor(context.getColor(R.color.wine_neutral_dark))
                }
            }

            // Drink image
            if (!drink.drinkImage.isNullOrEmpty()) {
                Glide.with(context)
                    .load(drink.drinkImage)
                    .placeholder(R.drawable.placeholder_drink)
                    .error(R.drawable.placeholder_drink)
                    .centerCrop()
                    .into(ivDrink)
            } else {
                ivDrink.setImageResource(R.drawable.placeholder_drink)
            }

            // Drink name
            tvDrinkName.text = drink.drinkName ?: "N/A"

            // Rating
            ratingBar.rating = drink.averageRating.toFloat()
            tvRating.text = String.format("%.1f", drink.averageRating)

            // Review count
            tvReviewCount.text = "(${drink.totalReviews} đánh giá)"
        }
    }
}
