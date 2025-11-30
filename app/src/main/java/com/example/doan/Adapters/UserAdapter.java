package com.example.doan.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan.Models.User;
import com.example.doan.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private Context context;
    private List<User> users;

    public UserAdapter(Context context, List<User> users) {
        this.context = context;
        this.users = users;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public void updateUsers(List<User> newUsers) {
        this.users = newUsers;
        notifyDataSetChanged();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvUserPhone, tvUserEmail, tvUserCreated;
        Chip chipUserRole;
        MaterialButton btnViewUser, btnToggleStatus;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tv_user_name);
            tvUserPhone = itemView.findViewById(R.id.tv_user_phone);
            tvUserEmail = itemView.findViewById(R.id.tv_user_email);
            tvUserCreated = itemView.findViewById(R.id.tv_user_created);
            chipUserRole = itemView.findViewById(R.id.chip_user_role);
            btnViewUser = itemView.findViewById(R.id.btn_view_user);
            btnToggleStatus = itemView.findViewById(R.id.btn_toggle_status);
        }

        public void bind(User user) {
            tvUserName.setText(user.getFullName());
            tvUserPhone.setText("📞 " + user.getPhone());
            tvUserEmail.setText("📧 " + (user.getEmail() != null ? user.getEmail() : "N/A"));
            
            // Mock created date - replace with actual data
            tvUserCreated.setText("🕒 Tham gia: 01/01/2024");

            // Role chip
            chipUserRole.setText(user.getRole());
            if ("MANAGER".equals(user.getRole())) {
                chipUserRole.setChipBackgroundColorResource(R.color.success);
            } else {
                chipUserRole.setChipBackgroundColorResource(R.color.primary);
            }

            // View button
            btnViewUser.setOnClickListener(v -> {
                Toast.makeText(context, "Chi tiết: " + user.getFullName(), Toast.LENGTH_SHORT).show();
            });

            // Toggle status button
            btnToggleStatus.setOnClickListener(v -> {
                Toast.makeText(context, "Khóa/Mở khóa: " + user.getFullName(), Toast.LENGTH_SHORT).show();
            });
        }
    }
}
