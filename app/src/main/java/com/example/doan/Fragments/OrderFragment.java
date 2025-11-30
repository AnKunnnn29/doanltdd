package com.example.doan.Fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan.Models.ApiResponse;
import com.example.doan.Models.Order;
import com.example.doan.Adapters.OrderAdapter;
import com.example.doan.R;
import com.example.doan.Network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderFragment extends Fragment {

    private static final String TAG = "OrderFragment";
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_USER_ID = "userId";

    private RecyclerView ordersRecyclerView;
    private OrderAdapter orderAdapter;

    private final List<Order> orderList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order, container, false);

        ordersRecyclerView = view.findViewById(R.id.orders_recycler_view);
        ordersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        orderAdapter = new OrderAdapter(getContext(), orderList);
        ordersRecyclerView.setAdapter(orderAdapter);

        int userId = getLoggedInUserId();

        if (userId != -1) {
            loadOrders(userId);
        } else {
            Toast.makeText(getContext(), "Vui lòng đăng nhập để xem đơn hàng.", Toast.LENGTH_LONG).show();
        }

        return view;
    }

    private int getLoggedInUserId() {
        if (getContext() != null) {
            SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            return prefs.getInt(KEY_USER_ID, -1);
        }
        return -1;
    }

    private void loadOrders(int userId) {
        Log.d(TAG, "Tải đơn hàng cho User ID: " + userId);

        // Use getUserOrders which returns ApiResponse<List<Order>>
        RetrofitClient.getInstance(requireContext()).getApiService().getUserOrders(userId).enqueue(new Callback<ApiResponse<List<Order>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Order>>> call, @NonNull Response<ApiResponse<List<Order>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<Order>> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        orderList.clear();
                        orderList.addAll(apiResponse.getData());
                        orderAdapter.notifyDataSetChanged();

                        if (orderList.isEmpty()) {
                            Toast.makeText(getContext(), "Bạn chưa có đơn hàng nào.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                         Toast.makeText(getContext(), "Lỗi tải đơn hàng: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Lỗi Server: " + response.code(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Response Code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Order>>> call, @NonNull Throwable t) {
                Log.e(TAG, "Lỗi kết nối API đơn hàng: " + t.getMessage());
                Toast.makeText(getContext(), "Không thể kết nối đến máy chủ", Toast.LENGTH_LONG).show();
            }
        });
    }
}