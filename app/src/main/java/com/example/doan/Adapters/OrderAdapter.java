package com.example.doan.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan.Models.Order;
import com.example.doan.R;
import com.google.android.material.chip.Chip;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {

    private Context context;
    private List<Order> orderList;
    private OnOrderClickListener listener;

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }

    public OrderAdapter(Context context, List<Order> orderList) {
        this.context = context;
        this.orderList = orderList;
    }

    public void setOnOrderClickListener(OnOrderClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = orderList.get(position);

        // Order ID
        holder.tvOrderId.setText("#" + order.getId());

        // Customer name or phone
        String customerInfo = order.getCustomerName() != null ? 
            order.getCustomerName() : 
            (order.getCustomerPhone() != null ? order.getCustomerPhone() : "Khách lẻ");
        holder.tvCustomer.setText(customerInfo);

        // Time
        if (order.getCreatedAt() != null) {
            try {
                // Parse ISO datetime string: "2025-12-01T14:48:42.365422"
                String dateTimeStr = order.getCreatedAt();
                if (dateTimeStr.contains("T")) {
                    String[] parts = dateTimeStr.split("T");
                    String datePart = parts[0]; // 2025-12-01
                    String timePart = parts[1].substring(0, 5); // 14:48
                    
                    // Format: HH:mm dd/MM
                    String[] dateParts = datePart.split("-");
                    holder.tvTime.setText(timePart + " " + dateParts[2] + "/" + dateParts[1]);
                } else {
                    holder.tvTime.setText(dateTimeStr);
                }
            } catch (Exception e) {
                holder.tvTime.setText(order.getCreatedAt());
            }
        }

        // Total amount
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        holder.tvTotal.setText(currencyFormat.format(order.getTotalAmount()));

        // Status chip
        String status = order.getStatus();
        holder.chipStatus.setText(getStatusText(status));
        
        // Set chip style based on status
        int bgColor, textColor;
        switch (status) {
            case "PENDING":
                bgColor = context.getColor(R.color.status_pending_bg);
                textColor = context.getColor(R.color.status_pending);
                break;
            case "MAKING":
                bgColor = context.getColor(R.color.status_making_bg);
                textColor = context.getColor(R.color.status_making);
                break;
            case "SHIPPING":
                bgColor = context.getColor(R.color.status_shipping_bg);
                textColor = context.getColor(R.color.status_shipping);
                break;
            case "DONE":
                bgColor = context.getColor(R.color.status_done_bg);
                textColor = context.getColor(R.color.status_done);
                break;
            case "CANCELED":
                bgColor = context.getColor(R.color.status_canceled_bg);
                textColor = context.getColor(R.color.status_canceled);
                break;
            default:
                bgColor = context.getColor(R.color.surface_variant);
                textColor = context.getColor(R.color.text_secondary);
        }
        
        holder.chipStatus.setChipBackgroundColorResource(android.R.color.transparent);
        holder.chipStatus.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(bgColor));
        holder.chipStatus.setTextColor(textColor);

        // Payment status
        if (order.getPaymentStatus() != null) {
            holder.tvPaymentStatus.setText(order.getPaymentStatus());
            holder.tvPaymentStatus.setVisibility(View.VISIBLE);
        } else {
            holder.tvPaymentStatus.setVisibility(View.GONE);
        }

        // Click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOrderClick(order);
            }
        });
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public void updateOrders(List<Order> newOrders) {
        this.orderList = newOrders;
        notifyDataSetChanged();
    }

    private String getStatusText(String status) {
        switch (status) {
            case "PENDING": return "Chờ xử lý";
            case "MAKING": return "Đang làm";
            case "SHIPPING": return "Đang giao";
            case "DONE": return "Hoàn thành";
            case "CANCELED": return "Đã hủy";
            default: return status;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvCustomer, tvTime, tvTotal, tvPaymentStatus;
        Chip chipStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tv_order_id);
            tvCustomer = itemView.findViewById(R.id.tv_customer);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvTotal = itemView.findViewById(R.id.tv_total);
            tvPaymentStatus = itemView.findViewById(R.id.tv_payment_status);
            chipStatus = itemView.findViewById(R.id.chip_status);
        }
    }
}
