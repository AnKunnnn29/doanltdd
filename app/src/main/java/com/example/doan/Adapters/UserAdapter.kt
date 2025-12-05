package com.example.doan.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Models.User
import com.example.doan.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip

class UserAdapter(
    private val context: Context,
    private var users: List<User>
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private var listener: OnUserActionListener? = null

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

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvUserName: TextView = itemView.findViewById(R.id.tv_user_name)
        private val tvUserPhone: TextView = itemView.findViewById(R.id.tv_user_phone)
        private val tvUserEmail: TextView = itemView.findViewById(R.id.tv_user_email)
        private val tvUserCreated: TextView = itemView.findViewById(R.id.tv_user_created)
        private val chipUserRole: Chip = itemView.findViewById(R.id.chip_user_role)
        private val btnViewUser: MaterialButton = itemView.findViewById(R.id.btn_view_user)
        private val btnToggleStatus: MaterialButton = itemView.findViewById(R.id.btn_toggle_status)

        fun bind(user: User) {
            tvUserName.text = user.fullName ?: user.username
            tvUserPhone.text = "📞 ${user.phone ?: "N/A"}"
            tvUserEmail.text = "📧 ${user.email ?: "N/A"}"
            
            // Format created date
            user.createdAt?.takeIf { it.isNotEmpty() }?.let {
                try {
                    val date = it.substring(0, 10)
                    tvUserCreated.text = "🕒 Tham gia: $date"
                } catch (e: Exception) {
                    tvUserCreated.text = "🕒 Tham gia: N/A"
                }
            } ?: run {
                tvUserCreated.text = "🕒 Tham gia: N/A"
            }

            // Role chip
            chipUserRole.text = user.role
            if (user.role == "MANAGER") {
                chipUserRole.setChipBackgroundColorResource(R.color.success)
            } else {
                chipUserRole.setChipBackgroundColorResource(R.color.primary)
            }

            // Status button text
            if (user.isBlocked) {
                btnToggleStatus.text = "Mở khóa"
                btnToggleStatus.setIconResource(R.drawable.ic_lock_open)
                itemView.alpha = 0.6f
            } else {
                btnToggleStatus.text = "Khóa"
                btnToggleStatus.setIconResource(R.drawable.ic_lock)
                itemView.alpha = 1.0f
            }

            // View button
            btnViewUser.setOnClickListener {
                listener?.onViewUser(user)
            }

            // Toggle status button
            btnToggleStatus.setOnClickListener {
                listener?.onToggleUserStatus(user)
            }
        }
    }
}
