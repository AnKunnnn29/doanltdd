package com.example.doan.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan.Models.Order;
import com.example.doan.R;

import java.util.List;
import java.util.Locale; // Cần thiết cho việc định dạng tiền tệ

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private final List<Order> orderList;

    public OrderAdapter(List<Order> orderList) {
        this.orderList = orderList;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);


        holder.orderNumber.setText(String.format("Mã Đơn: #%s", order.getOrderNumber()));


        holder.orderDate.setText(String.format("Ngày: %s", order.getDate()));


        holder.orderStatus.setText(String.format("Trạng thái: %s", order.getStatus()));


        holder.totalAmount.setText(String.format(Locale.getDefault(), "Tổng tiền: %,.0f VNĐ", order.getTotalAmount()));
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView orderNumber;
        TextView orderDate;
        TextView orderStatus;
        TextView totalAmount;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);

            orderNumber = itemView.findViewById(R.id.order_number);
            orderDate = itemView.findViewById(R.id.order_date);
            orderStatus = itemView.findViewById(R.id.order_status);


            totalAmount = itemView.findViewById(R.id.total_amount);

        }
    }
}