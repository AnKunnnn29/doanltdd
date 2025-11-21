package com.example.doan;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class OrderFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.orders_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        List<Order> orderList = new ArrayList<>();
        orderList.add(new Order("Order #12345", "October 26, 2023", "Status: Delivered"));
        orderList.add(new Order("Order #12346", "October 25, 2023", "Status: Canceled"));
        orderList.add(new Order("Order #12347", "October 24, 2023", "Status: Delivered"));

        OrderAdapter adapter = new OrderAdapter(orderList);
        recyclerView.setAdapter(adapter);

        return view;
    }
}
