package com.example.doan.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doan.Models.GroupOrderMemberDto
import com.example.doan.R

class GroupOrderMemberAdapter(
    private var members: List<GroupOrderMemberDto>,
    private val onKickMember: ((GroupOrderMemberDto) -> Unit)? = null,
    private val isHost: Boolean = false
) : RecyclerView.Adapter<GroupOrderMemberAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAvatar: ImageView = view.findViewById(R.id.iv_member_avatar)
        val tvName: TextView = view.findViewById(R.id.tv_member_name)
        val tvItemCount: TextView = view.findViewById(R.id.tv_item_count)
        val ivHostBadge: ImageView = view.findViewById(R.id.iv_host_badge)
        val btnKick: ImageView = view.findViewById(R.id.btn_kick_member)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group_order_member, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val member = members[position]
        
        holder.tvName.text = member.userName ?: "Thành viên"
        holder.tvItemCount.text = "${member.itemCount ?: 0} món"
        
        // Avatar
        if (!member.userAvatar.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(member.userAvatar)
                .placeholder(R.drawable.ic_person)
                .circleCrop()
                .into(holder.ivAvatar)
        } else {
            holder.ivAvatar.setImageResource(R.drawable.ic_person)
        }
        
        // Host badge
        holder.ivHostBadge.visibility = if (member.isHost == true) View.VISIBLE else View.GONE
        
        // Kick button (only show for host and not for self)
        holder.btnKick.visibility = if (isHost && member.isHost != true) View.VISIBLE else View.GONE
        holder.btnKick.setOnClickListener {
            onKickMember?.invoke(member)
        }
    }

    override fun getItemCount() = members.size

    fun updateMembers(newMembers: List<GroupOrderMemberDto>) {
        members = newMembers
        notifyDataSetChanged()
    }
}
