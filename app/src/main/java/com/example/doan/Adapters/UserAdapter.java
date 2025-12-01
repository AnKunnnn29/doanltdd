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

    private static final String TAG = "UserAdapter";
    
    private Context context;
    private List<User> users;
    private OnUserActionListener listener;

    public interface OnUserActionListener {
        void onViewUser(User user);
        void onToggleUserStatus(User user);
    }

    public UserAdapter(Context context, List<User> users) {
        this.context = context;
        this.users = users;
    }

    public void setOnUserActionListener(OnUserActionListener listener) {
        this.listener = listener;
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
            tvUserName.setText(user.getFullName() != null ? user.getFullName() : user.getUsername());
            tvUserPhone.setText("📞 " + (user.getPhone() != null ? user.getPhone() : "N/A"));
            tvUserEmail.setText("📧 " + (user.getEmail() != null ? user.getEmail() : "N/A"));
            
            // Format created date
            if (user.getCreatedAt() != null && !user.getCreatedAt().isEmpty()) {
                try {
                    String date = user.getCreatedAt().substring(0, 10);
                    tvUserCreated.setText("🕒 Tham gia: " + date);
                } catch (Exception e) {
                    tvUserCreated.setText("🕒 Tham gia: N/A");
                }
            } else {
                tvUserCreated.setText("🕒 Tham gia: N/A");
            }

            // Role chip
            chipUserRole.setText(user.getRole());
            if ("MANAGER".equals(user.getRole())) {
                chipUserRole.setChipBackgroundColorResource(R.color.success);
            } else {
                chipUserRole.setChipBackgroundColorResource(R.color.primary);
            }

            // Status button text
            if (user.isBlocked()) {
                btnToggleStatus.setText("Mở khóa");
                btnToggleStatus.setIconResource(R.drawable.ic_lock_open);
                itemView.setAlpha(0.6f);
            } else {
                btnToggleStatus.setText("Khóa");
                btnToggleStatus.setIconResource(R.drawable.ic_lock);
                itemView.setAlpha(1.0f);
            }

            // View button
            btnViewUser.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewUser(user);
                }
            });

            // Toggle status button
            btnToggleStatus.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onToggleUserStatus(user);
                }
            });
        }
    }
}
