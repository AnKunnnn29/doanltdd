package com.example.doan.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doan.Models.Drink;
import com.example.doan.R;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ManagerDrinkAdapter extends RecyclerView.Adapter<ManagerDrinkAdapter.ViewHolder> {

    private Context context;
    private List<Drink> drinkList;
    private List<Drink> drinkListFull; // For search functionality
    private OnDrinkActionListener listener;

    public interface OnDrinkActionListener {
        void onEditClick(Drink drink);
        void onDeleteClick(Drink drink);
    }

    public ManagerDrinkAdapter(Context context, List<Drink> drinkList, OnDrinkActionListener listener) {
        this.context = context;
        this.drinkList = drinkList;
        this.drinkListFull = new ArrayList<>(drinkList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_manager_drink, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Drink drink = drinkList.get(position);

        holder.tvName.setText(drink.getName());
        holder.tvCategory.setText(drink.getCategoryName() != null ? drink.getCategoryName() : "N/A");

        // Format price - use basePrice from Drink model
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        holder.tvPrice.setText(currencyFormat.format(drink.getBasePrice()));

        // Load image
        if (drink.getImageUrl() != null && !drink.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(drink.getImageUrl())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_broken_image)
                    .into(holder.imgDrink);
        } else {
            holder.imgDrink.setImageResource(R.drawable.ic_image_placeholder);
        }

        // Edit button
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(drink);
            }
        });

        // Delete button
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(drink);
            }
        });
    }

    @Override
    public int getItemCount() {
        return drinkList.size();
    }

    public void updateList(List<Drink> newList) {
        this.drinkList = newList;
        this.drinkListFull = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        drinkList.clear();
        if (query.isEmpty()) {
            drinkList.addAll(drinkListFull);
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();
            for (Drink drink : drinkListFull) {
                if (drink.getName().toLowerCase().contains(lowerCaseQuery) ||
                    (drink.getCategoryName() != null && drink.getCategoryName().toLowerCase().contains(lowerCaseQuery))) {
                    drinkList.add(drink);
                }
            }
        }
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgDrink;
        TextView tvName, tvCategory, tvPrice;
        ImageButton btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgDrink = itemView.findViewById(R.id.img_drink);
            tvName = itemView.findViewById(R.id.tv_drink_name);
            tvCategory = itemView.findViewById(R.id.tv_drink_category);
            tvPrice = itemView.findViewById(R.id.tv_drink_price);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
