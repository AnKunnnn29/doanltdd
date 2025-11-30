package com.example.doan.Fragments.Manager;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan.Activities.AddEditDrinkActivity;
import com.example.doan.Adapters.ManagerDrinkAdapter;
import com.example.doan.Models.ApiResponse;
import com.example.doan.Models.Category;
import com.example.doan.Models.Drink;
import com.example.doan.Network.RetrofitClient;
import com.example.doan.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageDrinksFragmentNew extends Fragment implements ManagerDrinkAdapter.OnDrinkActionListener {

    private static final String TAG = "ManageDrinksFragmentNew";

    // Views
    private RecyclerView rvDrinks;
    private ProgressBar progressBar;
    private View emptyState;
    private ExtendedFloatingActionButton fabAddDrink;
    private TextInputEditText editSearch;
    private TextView tvTotalDrinks, tvActiveDrinks, tvCategoriesCount, tvResultsCount;
    private MaterialButton btnViewMode;
    
    // Filter Chips
    private Chip chipFilterCategory, chipFilterPrice, chipFilterStatus, chipClearFilter;
    
    // Sort Chips
    private ChipGroup chipGroupSort;
    private Chip chipSortNameAsc, chipSortNameDesc, chipSortPriceAsc, chipSortPriceDesc, chipSortNewest;

    // Data
    private ManagerDrinkAdapter adapter;
    private List<Drink> allDrinks = new ArrayList<>();
    private List<Drink> filteredDrinks = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();
    
    // Filter State
    private int selectedCategoryId = -1;
    private double minPrice = 0;
    private double maxPrice = Double.MAX_VALUE;
    private Boolean filterActive = null; // null = all, true = active, false = inactive
    private String sortMode = "name_asc";
    private boolean isGridView = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_drinks_new, container, false);

        initViews(view);
        setupRecyclerView();
        setupListeners();
        loadCategories();
        loadDrinks();

        return view;
    }

    private void initViews(View view) {
        rvDrinks = view.findViewById(R.id.rv_drinks);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyState = view.findViewById(R.id.empty_state);
        fabAddDrink = view.findViewById(R.id.fab_add_drink);
        editSearch = view.findViewById(R.id.edit_search);
        
        // Stats
        tvTotalDrinks = view.findViewById(R.id.tv_total_drinks);
        tvActiveDrinks = view.findViewById(R.id.tv_active_drinks);
        tvCategoriesCount = view.findViewById(R.id.tv_categories_count);
        tvResultsCount = view.findViewById(R.id.tv_results_count);
        btnViewMode = view.findViewById(R.id.btn_view_mode);
        
        // Filter Chips
        chipFilterCategory = view.findViewById(R.id.chip_filter_category);
        chipFilterPrice = view.findViewById(R.id.chip_filter_price);
        chipFilterStatus = view.findViewById(R.id.chip_filter_status);
        chipClearFilter = view.findViewById(R.id.chip_clear_filter);
        
        // Sort Chips
        chipGroupSort = view.findViewById(R.id.chip_group_sort);
        chipSortNameAsc = view.findViewById(R.id.chip_sort_name_asc);
        chipSortNameDesc = view.findViewById(R.id.chip_sort_name_desc);
        chipSortPriceAsc = view.findViewById(R.id.chip_sort_price_asc);
        chipSortPriceDesc = view.findViewById(R.id.chip_sort_price_desc);
        chipSortNewest = view.findViewById(R.id.chip_sort_newest);
    }

    private void setupRecyclerView() {
        adapter = new ManagerDrinkAdapter(getContext(), filteredDrinks, this);
        rvDrinks.setLayoutManager(new LinearLayoutManager(getContext()));
        rvDrinks.setAdapter(adapter);
    }

    private void setupListeners() {
        // FAB Add
        fabAddDrink.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddEditDrinkActivity.class);
            startActivity(intent);
        });

        // Search
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFiltersAndSort();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Filter Chips
        chipFilterCategory.setOnClickListener(v -> showCategoryFilterDialog());
        chipFilterPrice.setOnClickListener(v -> showPriceFilterDialog());
        chipFilterStatus.setOnClickListener(v -> showStatusFilterDialog());
        chipClearFilter.setOnClickListener(v -> clearAllFilters());

        // Sort Chips
        chipGroupSort.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chip_sort_name_asc) sortMode = "name_asc";
                else if (checkedId == R.id.chip_sort_name_desc) sortMode = "name_desc";
                else if (checkedId == R.id.chip_sort_price_asc) sortMode = "price_asc";
                else if (checkedId == R.id.chip_sort_price_desc) sortMode = "price_desc";
                else if (checkedId == R.id.chip_sort_newest) sortMode = "newest";
                
                applyFiltersAndSort();
            }
        });

        // View Mode Toggle
        btnViewMode.setOnClickListener(v -> toggleViewMode());
    }

    private void loadCategories() {
        RetrofitClient.getInstance(requireContext()).getApiService()
                .getCategories()
                .enqueue(new Callback<ApiResponse<List<Category>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<Category>>> call,
                                           @NonNull Response<ApiResponse<List<Category>>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            categories = response.body().getData();
                            tvCategoriesCount.setText(String.valueOf(categories.size()));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<Category>>> call, @NonNull Throwable t) {
                        Log.e(TAG, "Failed to load categories: " + t.getMessage());
                    }
                });
    }

    private void loadDrinks() {
        progressBar.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);

        RetrofitClient.getInstance(requireContext()).getApiService()
                .getAllDrinks()
                .enqueue(new Callback<ApiResponse<List<Drink>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<Drink>>> call,
                                           @NonNull Response<ApiResponse<List<Drink>>> response) {
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            allDrinks = response.body().getData();
                            updateStats();
                            applyFiltersAndSort();
                        } else {
                            Toast.makeText(getContext(), "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<Drink>>> call, @NonNull Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Không thể kết nối", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void applyFiltersAndSort() {
        String searchQuery = editSearch.getText().toString().trim().toLowerCase();
        
        filteredDrinks.clear();
        
        for (Drink drink : allDrinks) {
            // Search filter
            if (!searchQuery.isEmpty()) {
                if (!drink.getName().toLowerCase().contains(searchQuery) &&
                    (drink.getCategoryName() == null || !drink.getCategoryName().toLowerCase().contains(searchQuery))) {
                    continue;
                }
            }
            
            // Category filter
            if (selectedCategoryId != -1 && drink.getCategoryId() != selectedCategoryId) {
                continue;
            }
            
            // Price filter
            if (drink.getBasePrice() < minPrice || drink.getBasePrice() > maxPrice) {
                continue;
            }
            
            // Status filter
            if (filterActive != null && drink.isActive() != filterActive) {
                continue;
            }
            
            filteredDrinks.add(drink);
        }
        
        // Sort
        sortDrinks();
        
        // Update UI
        adapter.updateList(filteredDrinks);
        updateResultsCount();
        
        if (filteredDrinks.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
        } else {
            emptyState.setVisibility(View.GONE);
        }
        
        // Show/hide clear filter button
        boolean hasFilters = selectedCategoryId != -1 || minPrice > 0 || 
                            maxPrice < Double.MAX_VALUE || filterActive != null;
        chipClearFilter.setVisibility(hasFilters ? View.VISIBLE : View.GONE);
    }

    private void sortDrinks() {
        switch (sortMode) {
            case "name_asc":
                Collections.sort(filteredDrinks, (d1, d2) -> d1.getName().compareToIgnoreCase(d2.getName()));
                break;
            case "name_desc":
                Collections.sort(filteredDrinks, (d1, d2) -> d2.getName().compareToIgnoreCase(d1.getName()));
                break;
            case "price_asc":
                Collections.sort(filteredDrinks, (d1, d2) -> Double.compare(d1.getBasePrice(), d2.getBasePrice()));
                break;
            case "price_desc":
                Collections.sort(filteredDrinks, (d1, d2) -> Double.compare(d2.getBasePrice(), d1.getBasePrice()));
                break;
            case "newest":
                Collections.sort(filteredDrinks, (d1, d2) -> Integer.compare(d2.getId(), d1.getId()));
                break;
        }
    }

    private void updateStats() {
        tvTotalDrinks.setText(String.valueOf(allDrinks.size()));
        
        int activeCount = 0;
        for (Drink drink : allDrinks) {
            if (drink.isActive()) activeCount++;
        }
        tvActiveDrinks.setText(String.valueOf(activeCount));
    }

    private void updateResultsCount() {
        tvResultsCount.setText("Hiển thị " + filteredDrinks.size() + " món");
    }

    private void toggleViewMode() {
        isGridView = !isGridView;
        if (isGridView) {
            rvDrinks.setLayoutManager(new GridLayoutManager(getContext(), 2));
            btnViewMode.setText("List");
        } else {
            rvDrinks.setLayoutManager(new LinearLayoutManager(getContext()));
            btnViewMode.setText("Grid");
        }
    }

    private void showCategoryFilterDialog() {
        // TODO: Implement category filter dialog
        Toast.makeText(getContext(), "Category filter - Coming soon", Toast.LENGTH_SHORT).show();
    }

    private void showPriceFilterDialog() {
        // TODO: Implement price filter dialog
        Toast.makeText(getContext(), "Price filter - Coming soon", Toast.LENGTH_SHORT).show();
    }

    private void showStatusFilterDialog() {
        // TODO: Implement status filter dialog
        Toast.makeText(getContext(), "Status filter - Coming soon", Toast.LENGTH_SHORT).show();
    }

    private void clearAllFilters() {
        selectedCategoryId = -1;
        minPrice = 0;
        maxPrice = Double.MAX_VALUE;
        filterActive = null;
        editSearch.setText("");
        applyFiltersAndSort();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDrinks();
    }

    @Override
    public void onEditClick(Drink drink) {
        Intent intent = new Intent(getActivity(), AddEditDrinkActivity.class);
        intent.putExtra("drink_id", drink.getId());
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(Drink drink) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc muốn xóa món '" + drink.getName() + "'?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteDrink(drink))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteDrink(Drink drink) {
        progressBar.setVisibility(View.VISIBLE);

        RetrofitClient.getInstance(requireContext()).getApiService()
                .deleteDrink(drink.getId())
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Void>> call,
                                           @NonNull Response<ApiResponse<Void>> response) {
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(), "Đã xóa món thành công", Toast.LENGTH_SHORT).show();
                            loadDrinks();
                        } else {
                            Toast.makeText(getContext(), "Không thể xóa món", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
