package com.example.doan.Fragments.Manager;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.doan.Adapters.StoreAdapter;
import com.example.doan.Models.ApiResponse;
import com.example.doan.Models.Store;
import com.example.doan.Network.ApiService;
import com.example.doan.Network.RetrofitClient;
import com.example.doan.R;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageStoresFragment extends Fragment {

    private RecyclerView rvStores;
    private StoreAdapter storeAdapter;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private EditText etSearchStore;
    private MaterialButton btnAddStore;

    private ApiService apiService;
    private List<Store> allStores = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_stores, container, false);

        // Initialize views
        rvStores = view.findViewById(R.id.rv_stores);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyState = view.findViewById(R.id.empty_state);
        etSearchStore = view.findViewById(R.id.et_search_store);
        btnAddStore = view.findViewById(R.id.btn_add_store);

        // Setup RecyclerView
        rvStores.setLayoutManager(new LinearLayoutManager(getContext()));
        storeAdapter = new StoreAdapter(getContext(), new ArrayList<>());
        rvStores.setAdapter(storeAdapter);

        // Setup API
        apiService = RetrofitClient.getInstance(requireContext()).getApiService();

        // Setup listeners
        setupListeners();

        // Load data
        loadStores();

        return view;
    }

    private void setupListeners() {
        // Swipe refresh
        swipeRefresh.setOnRefreshListener(this::loadStores);

        // Search
        etSearchStore.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStores(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Add store
        btnAddStore.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Thêm cửa hàng - Coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadStores() {
        showLoading(true);

        apiService.getStores().enqueue(new Callback<ApiResponse<List<Store>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Store>>> call, Response<ApiResponse<List<Store>>> response) {
                showLoading(false);
                swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<Store>> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        allStores = apiResponse.getData();
                        storeAdapter.updateStores(allStores);
                        updateEmptyState();
                    } else {
                        Toast.makeText(getContext(), "Lỗi: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Không thể tải danh sách cửa hàng", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Store>>> call, Throwable t) {
                showLoading(false);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterStores(String query) {
        if (query.isEmpty()) {
            storeAdapter.updateStores(allStores);
        } else {
            List<Store> filtered = new ArrayList<>();
            for (Store store : allStores) {
                if (store.getName().toLowerCase().contains(query.toLowerCase()) ||
                    store.getAddress().toLowerCase().contains(query.toLowerCase())) {
                    filtered.add(store);
                }
            }
            storeAdapter.updateStores(filtered);
        }
        updateEmptyState();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        rvStores.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void updateEmptyState() {
        emptyState.setVisibility(storeAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }
}
