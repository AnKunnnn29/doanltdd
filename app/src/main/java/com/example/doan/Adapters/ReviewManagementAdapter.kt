package com.example.doan.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doan.Models.ReviewManagement
import com.example.doan.R
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ReviewManagementAdapter(
    private val onDeleteClick: (ReviewManagement) -> Unit
) : RecyclerView.Adapter<ReviewManagementAdapter.ViewHolder>() {

    private val reviews = mutableListOf<ReviewManagement>()

    fun setData(newReviews: List<ReviewManagement>) {
        reviews.clear()
        reviews.addAll(newReviews)
        notifyDataSetChanged()
    }

    fun addData(newReviews: List<ReviewManagement>) {
        val startPosition = reviews.size
        reviews.addAll(newReviews)
        notifyItemRangeInserted(startPosition, newReviews.size)
    }

    fun clearData() {
        reviews.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review_management, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(reviews[position])
    }

    override fun getItemCount(): Int = reviews.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.card_review)
        private val ivAvatar: ImageView = itemView.findViewById(R.id.iv_avatar)
        private val tvUserName: TextView = itemView.findViewById(R.id.tv_user_name)
        private val tvUserFullName: TextView = itemView.findViewById(R.id.tv_user_fullname)
        private val ratingBar: RatingBar = itemView.findViewById(R.id.rating_bar)
        private val tvDrinkName: TextView = itemView.findViewById(R.id.tv_drink_name)
        private val tvComment: TextView = itemView.findViewById(R.id.tv_comment)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        private val chipBackup: Chip = itemView.findViewById(R.id.chip_backup)
        private val chipAnonymous: Chip = itemView.findViewById(R.id.chip_anonymous)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)

        fun bind(review: ReviewManagement) {
            // Avatar
            if (!review.userAvatar.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(review.userAvatar)
                    .placeholder(R.drawable.ic_person)
                    .circleCrop()
                    .into(ivAvatar)
            } else {
                ivAvatar.setImageResource(R.drawable.ic_person)
            }

            // User info
            tvUserName.text = review.userName ?: "Unknown"
            tvUserFullName.text = review.userFullName ?: ""
            tvUserFullName.visibility = if (review.userFullName.isNullOrEmpty()) View.GONE else View.VISIBLE

            // Rating
            ratingBar.rating = review.rating.toFloat()

            // Drink name
            tvDrinkName.text = "🍵 ${review.drinkName ?: "N/A"}"

            // Comment
            if (!review.comment.isNullOrEmpty()) {
                tvComment.text = review.comment
                tvComment.visibility = View.VISIBLE
            } else {
                tvComment.visibility = View.GONE
            }

            // Date
            tvDate.text = formatDate(review.createdAt)

            // Backup chip
            if (review.isFromDeletedUser) {
                chipBackup.visibility = View.VISIBLE
                cardView.strokeColor = itemView.context.getColor(R.color.warning)
                cardView.strokeWidth = 2
                btnDelete.visibility = View.GONE // Không cho xóa backup
            } else {
                chipBackup.visibility = View.GONE
                cardView.strokeWidth = 0
                btnDelete.visibility = View.VISIBLE
            }

            // Anonymous chip
            chipAnonymous.visibility = if (review.isAnonymous) View.VISIBLE else View.GONE

            // Delete button
            btnDelete.setOnClickListener {
                onDeleteClick(review)
            }
        }

        private fun formatDate(dateString: String?): String {
            if (dateString.isNullOrEmpty()) return "N/A"
            
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                val date = inputFormat.parse(dateString)
                
                val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                outputFormat.format(date!!)
            } catch (e: Exception) {
                dateString.take(16).replace("T", " ")
            }
        }
    }
}
