package com.example.doan.Fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan.Adapters.CategoryAdapter;
import com.example.doan.Models.ApiResponse;
import com.example.doan.Models.Category;
import com.example.doan.Models.Drink;
import com.example.doan.Models.Product;
import com.example.doan.Adapters.ProductAdapter;
import com.example.doan.R;
import com.example.doan.Network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment implements CategoryAdapter.OnCategoryClickListener {

    private RecyclerView productRecyclerView;
    private RecyclerView categoryRecyclerView;
    private ProductAdapter productAdapter;
    private CategoryAdapter categoryAdapter;

    private final List<Product> currentProductList = new ArrayList<>();
    private final List<Product> allProducts = new ArrayList<>(); 
    private final List<Category> categoryList = new ArrayList<>();
    
    // Lưu lại category đang chọn để khi reload data vẫn giữ đúng tab
    private int selectedCategoryId = -1; 

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Setup Product RecyclerView
        productRecyclerView = view.findViewById(R.id.product_recycler_view);
        productRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        productAdapter = new ProductAdapter(currentProductList);
        productRecyclerView.setAdapter(productAdapter);

        // Setup Category RecyclerView
        categoryRecyclerView = view.findViewById(R.id.category_recycler_view);
        categoryRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        categoryAdapter = new CategoryAdapter(categoryList, this);
        categoryRecyclerView.setAdapter(categoryAdapter);

        loadCategories();

        return view;
    }

    private void loadCategories() {
        RetrofitClient.getInstance(requireContext()).getApiService().getCategories().enqueue(new Callback<ApiResponse<List<Category>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Category>>> call, @NonNull Response<ApiResponse<List<Category>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<Category>> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        categoryList.clear();
                        
                        // Thêm danh mục "Tất cả"
                        Category allCategory = new Category();
                        allCategory.setId(-1);
                        allCategory.setName("Tất cả");
                        // Hình ảnh sẽ được cập nhật sau khi load sản phẩm
                        categoryList.add(allCategory);
                        
                        categoryList.addAll(apiResponse.getData());
                        categoryAdapter.notifyDataSetChanged();
                        
                        // Sau khi có danh mục thì tải sản phẩm
                        loadAllProducts();
                    }
                } else {
                    Log.e("HomeFragment", "Lỗi tải danh mục: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Category>>> call, @NonNull Throwable t) {
                Log.e("HomeFragment", "Lỗi kết nối danh mục: " + t.getMessage());
            }
        });
    }

    private void loadAllProducts() {
        RetrofitClient.getInstance(requireContext()).getApiService().getDrinks().enqueue(new Callback<ApiResponse<List<Drink>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Drink>>> call, @NonNull Response<ApiResponse<List<Drink>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<Drink>> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        allProducts.clear();
                        
                        // Xử lý URL hình ảnh
                        String baseUrl = RetrofitClient.getBaseUrl(); 
                        String rootUrl = baseUrl.replace("/api/", ""); 
                        if (rootUrl.endsWith("/")) rootUrl = rootUrl.substring(0, rootUrl.length() - 1);

                        for (Drink drink : apiResponse.getData()) {
                            String imageUrl = drink.getImageUrl();
                            if (imageUrl != null && !imageUrl.startsWith("http")) {
                                if (!imageUrl.startsWith("/")) imageUrl = "/" + imageUrl;
                                imageUrl = rootUrl + imageUrl;
                            }

                            // Chuyển đổi Drink -> Product
                            Product product = new Product(
                                drink.getId(),
                                drink.getName(),
                                drink.getDescription() != null ? drink.getDescription() : "",
                                drink.getBasePrice(),
                                drink.getCategoryName() != null ? drink.getCategoryName() : "",
                                drink.getCategoryId(), // QUAN TRỌNG: Lấy ID danh mục từ Drink
                                imageUrl,
                                drink.isActive()
                            );
                            // Thêm Size nếu cần thiết (Product model mới đã hỗ trợ list size)
                            product.setSizes(drink.getSizes());
                            
                            allProducts.add(product);
                        }

                        // Cập nhật hình ảnh cho Category (Lấy hình của sản phẩm đầu tiên thuộc Category đó)
                        updateCategoryImages();
                        
                        // Hiển thị sản phẩm theo category đang chọn (mặc định là -1: Tất cả)
                        filterProductsByCategory(selectedCategoryId);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Drink>>> call, @NonNull Throwable t) {
                Log.e("HomeFragment", "Lỗi kết nối sản phẩm: " + t.getMessage());
                Toast.makeText(getContext(), "Không thể tải thực đơn.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCategoryImages() {
        if (allProducts.isEmpty()) return;

        String defaultImage = allProducts.get(0).getImageUrl();

        for (Category cat : categoryList) {
            if (cat.getId() == -1) {
                // Category "Tất cả" lấy hình đầu tiên
                cat.setImage(defaultImage);
            } else {
                // Tìm sản phẩm đầu tiên có categoryId trùng khớp
                for (Product p : allProducts) {
                    if (p.getCategoryId() == cat.getId()) {
                        cat.setImage(p.getImageUrl());
                        break; 
                    }
                }
                // Fallback: nếu chưa có hình, dùng hình mặc định
                if (cat.getImage() == null) {
                    cat.setImage(defaultImage);
                }
            }
        }
        categoryAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCategoryClick(Category category) {
        selectedCategoryId = category.getId();
        filterProductsByCategory(selectedCategoryId);
    }

    private void filterProductsByCategory(int categoryId) {
        currentProductList.clear();
        
        if (categoryId == -1) {
            // Hiển thị tất cả
            currentProductList.addAll(allProducts);
        } else {
            // Lọc theo ID
            for (Product product : allProducts) {
                if (product.getCategoryId() == categoryId) {
                    currentProductList.add(product);
                }
            }
        }
        
        // Sắp xếp theo tên (A-Z)
        currentProductList.sort((p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()));
        
        productAdapter.notifyDataSetChanged();
    }
}