package com.example.doan.Fragments;

import android.os.Bundle;
import android.util.Log; // Cần thiết cho Log.e
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast; // Cần thiết cho Toast

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager; // Cần thiết cho setLayoutManager
import androidx.recyclerview.widget.RecyclerView; // Lớp bị lỗi

import com.example.doan.R;
import com.example.doan.Network.RetrofitClient;
import com.example.doan.Models.ApiResponse;
import com.example.doan.Models.Store;
import com.example.doan.Adapters.StoreAdapter;

import java.util.ArrayList; // Lớp bị lỗi
import java.util.List; // Lớp bị lỗi

import retrofit2.Call; // Cần thiết cho Retrofit
import retrofit2.Callback; // Cần thiết cho Retrofit
import retrofit2.Response; // Cần thiết cho Retrofit

public class StoreFragment extends Fragment {

    private RecyclerView storesRecyclerView;
    private StoreAdapter storeAdapter;
    private List<Store> storeList = new ArrayList<>(); // Đã sửa lỗi List và ArrayList

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_store, container, false);

        // Ánh xạ View và thiết lập LayoutManager
        storesRecyclerView = view.findViewById(R.id.stores_recycler_view);
        storesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        loadStores();

        return view;
    }

    private void loadStores() {

        // Gọi API qua RetrofitClient
        RetrofitClient.getInstance(requireContext()).getApiService().getStores().enqueue(new Callback<ApiResponse<List<Store>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Store>>> call, @NonNull Response<ApiResponse<List<Store>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<Store>> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        // Cập nhật adapter
                        storeList = apiResponse.getData();
                        storeAdapter = new StoreAdapter(storeList);
                        storesRecyclerView.setAdapter(storeAdapter);
                    } else {
                        String message = apiResponse.getMessage() != null ? apiResponse.getMessage() : "Lỗi tải dữ liệu cửa hàng";
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    }
                } else {

                    Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Store>>> call, @NonNull Throwable t) {

                Log.e("StoreFragment", "Lỗi kết nối API: " + t.getMessage());
                Toast.makeText(getContext(), "Không thể kết nối đến máy chủ", Toast.LENGTH_LONG).show();
            }
        });
    }
}