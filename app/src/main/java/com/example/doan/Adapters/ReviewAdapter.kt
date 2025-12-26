package com.example.doan.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doan.Models.Review
import com.example.doan.R
import java.text.SimpleDateFormat
import java.util.*

class ReviewAdapter(
    private var reviews: List<Review> = emptyList()
) : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    fun updateReviews(newReviews: List<Review>) {
        reviews = newReviews
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(reviews[position])
    }

    override fun getItemCount(): Int = reviews.size

    class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgAvatar: ImageView = itemView.findViewById(R.id.imgReviewAvatar)
        private val tvUserName: TextView = itemView.findViewById(R.id.tvReviewUserName)
        private val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBarReview)
        private val tvComment: TextView = itemView.findViewById(R.id.tvReviewComment)
        private val tvDate: TextView = itemView.findViewById(R.id.tvReviewDate)

        fun bind(review: Review) {
            tvUserName.text = review.userName ?: "Ẩn danh"
            ratingBar.rating = review.rating.toFloat()
            
            if (review.comment.isNullOrEmpty()) {
                tvComment.visibility = View.GONE
            } else {
                tvComment.visibility = View.VISIBLE
                tvComment.text = review.comment
            }
            
            // Format date
            review.createdAt?.let { dateStr ->
                try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val date = inputFormat.parse(dateStr)
                    tvDate.text = date?.let { outputFormat.format(it) } ?: dateStr
                } catch (e: Exception) {
                    tvDate.text = dateStr.take(10)
                }
            }
            
            // Load avatar
            if (!review.userAvatar.isNullOrEmpty() && !review.isAnonymous) {
                Glide.with(itemView.context)
                    .load(review.userAvatar)
                    .placeholder(R.drawable.ic_person)
                    .circleCrop()
                    .into(imgAvatar)
            } else {
                imgAvatar.setImageResource(R.drawable.ic_person)
            }
        }
    }
}
