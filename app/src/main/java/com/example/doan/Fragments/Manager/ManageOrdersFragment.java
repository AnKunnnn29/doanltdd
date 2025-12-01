package com.example.doan.Fragments.Manager;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan.Adapters.OrderAdapter;
import com.example.doan.Models.ApiResponse;
import com.example.doan.Models.Order;
import com.example.doan.Network.RetrofitClient;
import com.example.doan.R;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageOrdersFragment extends Fragment {

    private static final String TAG = "ManageOrdersFragment";

    private RecyclerView rvOrders;
    private ProgressBar progressBar;
    private View emptyState;
    private TabLayout tabLayout;

    private OrderAdapter adapter;
    private List<Order> orderList = new ArrayList<>();
    private String currentStatus = null; // null = all orders

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_orders, container, false);

        // Initialize views
        rvOrders = view.findViewById(R.id.rv_orders);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyState = view.findViewById(R.id.empty_state);
        tabLayout = view.findViewById(R.id.tab_layout);

        // Setup RecyclerView
        rvOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new OrderAdapter(getContext(), orderList);
        rvOrders.setAdapter(adapter);

        // Setup TabLayout
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: // Tất cả
                        currentStatus = null;
                        break;
                    case 1: // Chờ xử lý
                        currentStatus = "PENDING";
                        break;
                    case 2: // Đang làm
                        currentStatus = "MAKING";
                        break;
                    case 3: // Đang giao
                        currentStatus = "SHIPPING";
                        break;
                    case 4: // Hoàn thành
                        currentStatus = "DONE";
                        break;
                    case 5: // Đã hủy
                        currentStatus = "CANCELED";
                        break;
                }
                loadOrders();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Load orders
        loadOrders();

        return view;
    }

    private void loadOrders() {
        progressBar.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        
        Log.d(TAG, "Loading orders with status: " + currentStatus);

        RetrofitClient.getInstance(requireContext()).getApiService()
                .getManagerOrders(currentStatus, 0, 100)
                .enqueue(new Callback<ApiResponse<com.example.doan.Models.PageResponse<Order>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<com.example.doan.Models.PageResponse<Order>>> call,
                                           @NonNull Response<ApiResponse<com.example.doan.Models.PageResponse<Order>>> response) {
                        progressBar.setVisibility(View.GONE);
                        
                        Log.d(TAG, "Response code: " + response.code());
                        Log.d(TAG, "Response successful: " + response.isSuccessful());

                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<com.example.doan.Models.PageResponse<Order>> apiResponse = response.body();
                            Log.d(TAG, "ApiResponse success: " + apiResponse.isSuccess());
                            Log.d(TAG, "ApiResponse data null: " + (apiResponse.getData() == null));
                            
                            if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                                com.example.doan.Models.PageResponse<Order> pageResponse = apiResponse.getData();
                                orderList = pageResponse.getContent();
                                
                                Log.d(TAG, "Orders loaded: " + (orderList != null ? orderList.size() : 0));
                                
                                adapter.updateOrders(orderList);

                                if (orderList == null || orderList.isEmpty()) {
                                    emptyState.setVisibility(View.VISIBLE);
                                    Log.d(TAG, "No orders found");
                                } else {
                                    Log.d(TAG, "Displaying " + orderList.size() + " orders");
                                }
                            } else {
                                String msg = apiResponse.getMessage() != null ? apiResponse.getMessage() : "Không thể tải đơn hàng";
                                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "API Error: " + msg);
                            }
                        } else {
                            Toast.makeText(getContext(), "Lỗi Server: " + response.code(), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error code: " + response.code());
                            try {
                                if (response.errorBody() != null) {
                                    Log.e(TAG, "Error body: " + response.errorBody().string());
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error reading error body", e);
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<com.example.doan.Models.PageResponse<Order>>> call, @NonNull Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        emptyState.setVisibility(View.VISIBLE);
                        
                        String errorMsg = "Không thể kết nối Server";
                        if (t != null) {
                            errorMsg += "\n" + t.getClass().getSimpleName() + ": " + t.getMessage();
                            Log.e(TAG, "Connection error: " + t.getMessage(), t);
                        }
                        
                        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
