package com.example.doan.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan.Models.Store;
import com.example.doan.R;
import com.example.doan.Utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.List;

public class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.StoreViewHolder> {

    private Context context;
    private List<Store> stores;
    private boolean isManager;

    public StoreAdapter(Context context, List<Store> stores) {
        this.context = context;
        this.stores = stores;
        
        // Kiểm tra quyền Manager ngay khi khởi tạo Adapter
        SessionManager sessionManager = new SessionManager(context);
        // Kiểm tra role là MANAGER hoặc ADMIN (tùy logic backend của bạn)
        String role = sessionManager.getRole();
        this.isManager = "MANAGER".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role);
    }

    @NonNull
    @Override
    public StoreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_store, parent, false);
        return new StoreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoreViewHolder holder, int position) {
        Store store = stores.get(position);
        holder.bind(store);
    }

    @Override
    public int getItemCount() {
        return stores.size();
    }

    public void updateStores(List<Store> newStores) {
        this.stores = newStores;
        notifyDataSetChanged();
    }

    class StoreViewHolder extends RecyclerView.ViewHolder {
        TextView tvStoreName, tvStoreAddress, tvStorePhone, tvStoreHours;
        Chip chipStoreStatus;
        MaterialButton btnEditStore, btnDeleteStore;

        public StoreViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStoreName = itemView.findViewById(R.id.tv_store_name);
            tvStoreAddress = itemView.findViewById(R.id.tv_store_address);
            tvStorePhone = itemView.findViewById(R.id.tv_store_phone);
            tvStoreHours = itemView.findViewById(R.id.tv_store_hours);
            chipStoreStatus = itemView.findViewById(R.id.chip_store_status);
            btnEditStore = itemView.findViewById(R.id.btn_edit_store);
            btnDeleteStore = itemView.findViewById(R.id.btn_delete_store);
        }

        public void bind(Store store) {
            tvStoreName.setText(store.getName());
            tvStoreAddress.setText(store.getAddress());
            tvStorePhone.setText(store.getPhone() != null ? store.getPhone() : "N/A");
            
            // Mock hours
            tvStoreHours.setText(store.getOpenTime() + " - " + store.getCloseTime());
            
            // Status
            chipStoreStatus.setText("Hoạt động");
            // chipStoreStatus.setChipBackgroundColorResource(R.color.success); // Comment out color if resource missing

            // PHÂN QUYỀN: Ẩn nút nếu không phải Manager
            if (isManager) {
                btnEditStore.setVisibility(View.VISIBLE);
                btnDeleteStore.setVisibility(View.VISIBLE);
                
                btnEditStore.setOnClickListener(v -> {
                    Toast.makeText(context, "Sửa: " + store.getName(), Toast.LENGTH_SHORT).show();
                    // TODO: Chuyển sang màn hình EditStoreActivity
                });

                btnDeleteStore.setOnClickListener(v -> {
                    Toast.makeText(context, "Xóa: " + store.getName(), Toast.LENGTH_SHORT).show();
                    // TODO: Gọi API xóa Store
                });
            } else {
                btnEditStore.setVisibility(View.GONE);
                btnDeleteStore.setVisibility(View.GONE);
            }
        }
    }
}