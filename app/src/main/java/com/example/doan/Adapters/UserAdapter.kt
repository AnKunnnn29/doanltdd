package com.example.doan.Adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Models.User
import com.example.doan.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip

class UserAdapter(
    private val context: Context,
    private var users: List<User>
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private var listener: OnUserActionListener? = null

    // Màu avatar dựa trên tên
    private val avatarColors = listOf(
        "#931923", // Wine primary
        "#E91E63", // Pink
        "#9C27B0", // Purple
        "#673AB7", // Deep Purple
        "#3F51B5", // Indigo
        "#2196F3", // Blue
        "#00BCD4", // Cyan
        "#009688", // Teal
        "#4CAF50", // Green
        "#FF9800", // Orange
        "#FF5722"  // Deep Orange
    )

    interface OnUserActionListener {
        fun onViewUser(user: User)
        fun onToggleUserStatus(user: User)
    }

    fun setOnUserActionListener(listener: OnUserActionListener) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.bind(user)
    }

    override fun getItemCount(): Int = users.size

    fun updateUsers(newUsers: List<User>) {
        this.users = newUsers
        notifyDataSetChanged()
    }

    private fun getAvatarColor(name: String?): Int {
        val index = (name?.hashCode()?.let { Math.abs(it) } ?: 0) % avatarColors.size
        return Color.parseColor(avatarColors[index])
    }

    private fun getInitials(name: String?): String {
        if (name.isNullOrBlank()) return "U"
        val parts = name.trim().split(" ")
        return when {
            parts.size >= 2 -> "${parts.first().firstOrNull()?.uppercaseChar() ?: ""}${parts.last().firstOrNull()?.uppercaseChar() ?: ""}"
            parts.size == 1 -> parts.first().take(2).uppercase()
            else -> "U"
        }
    }

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardAvatar: MaterialCardView = itemView.findViewById(R.id.card_avatar)
        private val tvAvatarInitial: TextView = itemView.findViewById(R.id.tv_avatar_initial)
        private val statusIndicator: View = itemView.findViewById(R.id.status_indicator)
        private val tvUserName: TextView = itemView.findViewById(R.id.tv_user_name)
        private val tvUserPhone: TextView = itemView.findViewById(R.id.tv_user_phone)
        private val tvUserEmail: TextView = itemView.findViewById(R.id.tv_user_email)
        private val tvUserCreated: TextView = itemView.findViewById(R.id.tv_user_created)
        private val tvOrderCount: TextView = itemView.findViewById(R.id.tv_order_count)
        private val chipUserRole: Chip = itemView.findViewById(R.id.chip_user_role)
        private val btnViewUser: MaterialButton = itemView.findViewById(R.id.btn_view_user)
        private val btnToggleStatus: MaterialButton = itemView.findViewById(R.id.btn_toggle_status)

        fun bind(user: User) {
            val displayName = user.fullName ?: user.username ?: "User"
            
            // Avatar
            tvAvatarInitial.text = getInitials(displayName)
            cardAvatar.setCardBackgroundColor(getAvatarColor(displayName))
            
            // Status indicator
            statusIndicator.setBackgroundResource(
                if (user.isBlocked) R.drawable.bg_status_blocked 
                else R.drawable.bg_status_indicator
            )
            
            // User info
            tvUserName.text = displayName
            tvUserPhone.text = user.phone ?: "Chưa cập nhật"
            tvUserEmail.text = user.email ?: "Chưa cập nhật"
            
            // Format created date
            user.createdAt?.takeIf { it.isNotEmpty() }?.let {
                try {
                    val date = it.substring(0, 10)
                    tvUserCreated.text = date
                } catch (e: Exception) {
                    tvUserCreated.text = "N/A"
                }
            } ?: run {
                tvUserCreated.text = "N/A"
            }

            // Order count (nếu có)
            tvOrderCount.text = "${user.orderCount ?: 0} đơn"

            // Role chip với màu sắc
            chipUserRole.text = when (user.role) {
                "MANAGER" -> "Quản lý"
                "ADMIN" -> "Admin"
                else -> "Khách hàng"
            }
            
            when (user.role) {
                "MANAGER" -> {
                    chipUserRole.setChipBackgroundColorResource(R.color.success)
                    chipUserRole.setTextColor(context.getColor(R.color.white))
                }
                "ADMIN" -> {
                    chipUserRole.setChipBackgroundColorResource(R.color.wine_primary)
                    chipUserRole.setTextColor(context.getColor(R.color.white))
                }
                else -> {
                    chipUserRole.setChipBackgroundColorResource(R.color.wine_neutral_light)
                    chipUserRole.setTextColor(context.getColor(R.color.wine_dark_background))
                }
            }

            // Status button
            if (user.isBlocked) {
                btnToggleStatus.text = "Mở khóa"
                btnToggleStatus.setIconResource(R.drawable.ic_lock_open)
                btnToggleStatus.setTextColor(context.getColor(R.color.success))
                btnToggleStatus.setIconTintResource(R.color.success)
                btnToggleStatus.setStrokeColorResource(R.color.success)
                itemView.alpha = 0.7f
            } else {
                btnToggleStatus.text = "Khóa tài khoản"
                btnToggleStatus.setIconResource(R.drawable.ic_lock)
                btnToggleStatus.setTextColor(context.getColor(R.color.error))
                btnToggleStatus.setIconTintResource(R.color.error)
                btnToggleStatus.setStrokeColorResource(R.color.error)
                itemView.alpha = 1.0f
            }

            // Click listeners
            btnViewUser.setOnClickListener {
                listener?.onViewUser(user)
            }

            btnToggleStatus.setOnClickListener {
                listener?.onToggleUserStatus(user)
            }
            
            // Card click
            itemView.setOnClickListener {
                listener?.onViewUser(user)
            }
        }
    }
}
