package com.example.doan.Fragments.Manager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan.Activities.AddEditCategoryActivity;
import com.example.doan.Adapters.ManagerCategoryAdapter;
import com.example.doan.Models.ApiResponse;
import com.example.doan.Models.Category;
import com.example.doan.Network.RetrofitClient;
import com.example.doan.R;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageCategoriesFragment extends Fragment implements ManagerCategoryAdapter.OnCategoryActionListener {

    private static final String TAG = "ManageCategoriesFragment";

    private RecyclerView rvCategories;
    private ProgressBar progressBar;
    private View emptyState;
    private MaterialButton btnAddCategory;

    private ManagerCategoryAdapter adapter;
    private List<Category> categoryList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_categories, container, false);

        // Initialize views
        rvCategories = view.findViewById(R.id.rv_categories);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyState = view.findViewById(R.id.empty_state);
        btnAddCategory = view.findViewById(R.id.btn_add_category);

        // Setup RecyclerView
        rvCategories.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new ManagerCategoryAdapter(getContext(), categoryList, this);
        rvCategories.setAdapter(adapter);

        // Add category button
        btnAddCategory.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddEditCategoryActivity.class);
            startActivity(intent);
        });

        // Load categories
        loadCategories();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCategories();
    }

    private void loadCategories() {
        progressBar.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);

        RetrofitClient.getInstance(requireContext()).getApiService()
                .getCategories()
                .enqueue(new Callback<ApiResponse<List<Category>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<Category>>> call,
                                           @NonNull Response<ApiResponse<List<Category>>> response) {
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<List<Category>> apiResponse = response.body();
                            if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                                categoryList = apiResponse.getData();
                                adapter.updateList(categoryList);

                                if (categoryList.isEmpty()) {
                                    emptyState.setVisibility(View.VISIBLE);
                                }
                            } else {
                                Toast.makeText(getContext(), "Không thể tải danh mục", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getContext(), "Lỗi Server: " + response.code(), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error code: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<Category>>> call, @NonNull Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        emptyState.setVisibility(View.VISIBLE);
                        Toast.makeText(getContext(), "Không thể kết nối Server", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Connection error: " + t.getMessage());
                    }
                });
    }

    @Override
    public void onEditClick(Category category) {
        Intent intent = new Intent(getActivity(), AddEditCategoryActivity.class);
        intent.putExtra("category_id", category.getId());
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(Category category) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc muốn xóa danh mục '" + category.getName() + "'?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteCategory(category))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteCategory(Category category) {
        progressBar.setVisibility(View.VISIBLE);

        RetrofitClient.getInstance(requireContext()).getApiService()
                .deleteCategory(category.getId())
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Void>> call,
                                           @NonNull Response<ApiResponse<Void>> response) {
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(), "Đã xóa danh mục thành công", Toast.LENGTH_SHORT).show();
                            loadCategories();
                        } else {
                            Toast.makeText(getContext(), "Không thể xóa danh mục: " + response.code(), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Delete error code: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Delete connection error: " + t.getMessage());
                    }
                });
    }
}
