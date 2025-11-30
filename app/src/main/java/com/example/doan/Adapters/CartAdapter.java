package com.example.doan.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doan.Models.CartItem;
import com.example.doan.R;
import com.example.doan.Utils.CartManager;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private Context context;
    private List<CartItem> cartItems;
    private OnCartChangeListener listener;

    public interface OnCartChangeListener {
        void onCartChanged();
    }

    public CartAdapter(Context context, List<CartItem> cartItems, OnCartChangeListener listener) {
        this.context = context;
        this.cartItems = cartItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        holder.bind(item, position);
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView tvName, tvSize, tvPrice, tvQuantity;
        MaterialButton btnMinus, btnPlus, btnRemove;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.img_product);
            tvName = itemView.findViewById(R.id.tv_product_name);
            tvSize = itemView.findViewById(R.id.tv_product_size);
            tvPrice = itemView.findViewById(R.id.tv_product_price);
            tvQuantity = itemView.findViewById(R.id.tv_quantity);
            btnMinus = itemView.findViewById(R.id.btn_minus);
            btnPlus = itemView.findViewById(R.id.btn_plus);
            btnRemove = itemView.findViewById(R.id.btn_remove);
        }

        public void bind(CartItem item, int position) {
            Glide.with(context).load(item.getProduct().getImageUrl()).into(imgProduct);
            tvName.setText(item.getProduct().getName());
            tvSize.setText("Size: " + item.getSizeName());
            tvPrice.setText(String.format(Locale.getDefault(), "%,.0f VNĐ", item.getTotalPrice()));
            tvQuantity.setText(String.valueOf(item.getQuantity()));

            btnMinus.setOnClickListener(v -> {
                if (item.getQuantity() > 1) {
                    CartManager.getInstance().updateQuantity(position, item.getQuantity() - 1);
                    notifyItemChanged(position);
                    listener.onCartChanged();
                }
            });

            btnPlus.setOnClickListener(v -> {
                CartManager.getInstance().updateQuantity(position, item.getQuantity() + 1);
                notifyItemChanged(position);
                listener.onCartChanged();
            });

            btnRemove.setOnClickListener(v -> {
                CartManager.getInstance().removeItem(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, cartItems.size());
                listener.onCartChanged();
            });
        }
    }
}