package com.example.doan.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Models.User
import com.example.doan.R

class UserSelectableAdapter(
    private val users: MutableList<User>,
    private val selectedUsers: MutableSet<User>
) : RecyclerView.Adapter<UserSelectableAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_selectable, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.bind(user)
    }

    override fun getItemCount(): Int = users.size

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userName: TextView = itemView.findViewById(R.id.tv_user_name)
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkbox_user)

        fun bind(user: User) {
            userName.text = user.fullName ?: user.username
            checkBox.isChecked = selectedUsers.contains(user)

            itemView.setOnClickListener {
                checkBox.isChecked = !checkBox.isChecked
            }

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedUsers.add(user)
                } else {
                    selectedUsers.remove(user)
                }
            }
        }
    }
    
    fun updateData(newUsers: List<User>) {
        users.clear()
        users.addAll(newUsers)
        notifyDataSetChanged()
    }
}
