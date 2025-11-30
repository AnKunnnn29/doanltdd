package com.example.doan.Fragments.Manager;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan.Models.ApiResponse;
import com.example.doan.Models.DashboardSummary;
import com.example.doan.Network.RetrofitClient;
import com.example.doan.R;

import java.text.NumberFormat;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardFragment extends Fragment {

    private static final String TAG = "DashboardFragment";

    private TextView tvTotalRevenue, tvTotalOrders, tvPendingOrders;
    private RecyclerView rvTopSelling;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Initialize views
        tvTotalRevenue = view.findViewById(R.id.tv_total_revenue);
        tvTotalOrders = view.findViewById(R.id.tv_total_orders);
        tvPendingOrders = view.findViewById(R.id.tv_pending_orders);
        rvTopSelling = view.findViewById(R.id.rv_top_selling);
        progressBar = view.findViewById(R.id.progress_bar);

        rvTopSelling.setLayoutManager(new LinearLayoutManager(getContext()));

        // Load dashboard data
        loadDashboardData();

        return view;
    }

    private void loadDashboardData() {
        progressBar.setVisibility(View.VISIBLE);
        
        RetrofitClient.getInstance(requireContext()).getApiService()
                .getDashboardSummary()
                .enqueue(new Callback<ApiResponse<DashboardSummary>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<DashboardSummary>> call,
                                           @NonNull Response<ApiResponse<DashboardSummary>> response) {
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<DashboardSummary> apiResponse = response.body();
                            if (apiResponse != null && apiResponse.isSuccess() && apiResponse.getData() != null) {
                                displayDashboardData(apiResponse.getData());
                            } else {
                                String message = (apiResponse != null && apiResponse.getMessage() != null)
                                        ? apiResponse.getMessage()
                                        : "Không thể tải dữ liệu";
                                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getContext(), "Lỗi Server: " + response.code(), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error response code: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<DashboardSummary>> call, @NonNull Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "Error loading dashboard: " + t.getMessage());
                        Toast.makeText(getContext(), "Không thể kết nối Server", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void displayDashboardData(DashboardSummary data) {
        // Format currency
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        // Display revenue
        tvTotalRevenue.setText(currencyFormat.format(data.getTotalRevenue()));

        // Display orders
        tvTotalOrders.setText(String.valueOf(data.getTotalOrders()));
        tvPendingOrders.setText(String.valueOf(data.getPendingOrders()));

        // Display top selling drinks
        if (data.getTopSellingDrinks() != null && !data.getTopSellingDrinks().isEmpty()) {
            // TODO: Create adapter for top selling drinks
            // For now, just show count
            Toast.makeText(getContext(), 
                "Top selling: " + data.getTopSellingDrinks().size() + " món", 
                Toast.LENGTH_SHORT).show();
        }
    }
}
