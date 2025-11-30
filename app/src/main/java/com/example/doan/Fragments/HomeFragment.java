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
    private final List<Product> allProducts = new ArrayList<>(); // To store all products
    private final List<Category> categoryList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Product RecyclerView
        productRecyclerView = view.findViewById(R.id.product_recycler_view);
        productRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        productAdapter = new ProductAdapter(currentProductList);
        productRecyclerView.setAdapter(productAdapter);

        // Category RecyclerView
        categoryRecyclerView = view.findViewById(R.id.category_recycler_view);
        categoryRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        categoryAdapter = new CategoryAdapter(categoryList, this);
        categoryRecyclerView.setAdapter(categoryAdapter);

        loadCategories();
        // loadAllProducts() will be called after categories are loaded

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
                        // Add an "All" category
                        Category allCategory = new Category();
                        allCategory.setId(-1); // -1 for All
                        allCategory.setName("Tất cả");
                        allCategory.setImage("all_icon"); // Placeholder
                        categoryList.add(allCategory);
                        categoryList.addAll(apiResponse.getData());
                        categoryAdapter.notifyDataSetChanged();
                        
                        // Load products after categories are ready
                        loadAllProducts();
                    } else {
                        Toast.makeText(getContext(), "Lỗi tải danh mục: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Lỗi Server: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Category>>> call, @NonNull Throwable t) {
                Log.e("HomeFragment", "Lỗi kết nối API: " + t.getMessage());
                Toast.makeText(getContext(), "Không thể kết nối Server để tải danh mục.", Toast.LENGTH_LONG).show();
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
                        
                        // Prepare Base URL for images
                        String baseUrl = RetrofitClient.getBaseUrl(); 
                        String rootUrl = baseUrl.replace("/api/", ""); 
                        if (rootUrl.endsWith("/")) {
                             rootUrl = rootUrl.substring(0, rootUrl.length() - 1);
                        }

                        for (Drink drink : apiResponse.getData()) {
                            String imageUrl = drink.getImageUrl();
                            if (imageUrl != null && !imageUrl.startsWith("http")) {
                                if (!imageUrl.startsWith("/")) {
                                    imageUrl = "/" + imageUrl;
                                }
                                imageUrl = rootUrl + imageUrl;
                            }

                            int pCategoryId = drink.getCategoryId();
                            // Fallback if categoryId is 0 (missing from API), try to match by name
                            if (pCategoryId == 0 && drink.getCategoryName() != null) {
                                for (Category cat : categoryList) {
                                    if (cat.getName().equalsIgnoreCase(drink.getCategoryName())) {
                                        pCategoryId = cat.getId();
                                        break;
                                    }
                                }
                            }

                            Product product = new Product(
                                drink.getId(),
                                drink.getName(),
                                drink.getDescription() != null ? drink.getDescription() : "",
                                drink.getBasePrice(),
                                drink.getCategoryName() != null ? drink.getCategoryName() : "",
                                pCategoryId,
                                imageUrl,
                                drink.isActive()
                            );
                            allProducts.add(product);
                        }
                        
                        // --- Logic to Update Category Images ---
                        if (!allProducts.isEmpty()) {
                            String defaultImage = allProducts.get(0).getImageUrl();
                            
                            for (Category cat : categoryList) {
                                if (cat.getId() == -1) {
                                    // For "All", use any product image (e.g., the first one)
                                    cat.setImage(defaultImage);
                                } else {
                                    // For other categories, prioritize product image
                                    // Try to find first product in this category
                                    for (Product p : allProducts) {
                                        if (p.getCategoryId() == cat.getId()) {
                                            cat.setImage(p.getImageUrl());
                                            break; // Found a representative, move to next category
                                        }
                                        // Fallback logic: if ID match failed, try name match
                                        if (p.getCategory() != null && p.getCategory().equalsIgnoreCase(cat.getName())) {
                                             cat.setImage(p.getImageUrl());
                                             break;
                                        }
                                    }
                                }
                            }
                            categoryAdapter.notifyDataSetChanged(); // Refresh category images
                        }

                        // Initially, show all products
                        filterProductsByCategory(-1, null);
                    } else {
                        Toast.makeText(getContext(), "Lỗi: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Lỗi tải thực đơn: " + response.code(), Toast.LENGTH_SHORT).show();
                    Log.e("HomeFragment", "Lỗi tải sản phẩm, Code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Drink>>> call, @NonNull Throwable t) {
                Log.e("HomeFragment", "Lỗi kết nối API: " + t.getMessage());
                Toast.makeText(getContext(), "Không thể kết nối Server để tải thực đơn.", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onCategoryClick(Category category) {
        // Check category ID, fallback to name if ID seems invalid (though now it should be valid)
        if (category.getName().equals("Tất cả") || category.getId() == -1) {
            filterProductsByCategory(-1, null); // Pass -1 to show all
        } else {
            filterProductsByCategory(category.getId(), category.getName());
        }
    }

    private void filterProductsByCategory(int categoryId, String categoryName) {
        currentProductList.clear();
        if (categoryId == -1) {
            currentProductList.addAll(allProducts);
        } else {
            for (Product product : allProducts) {
                // Filter by ID
                boolean matchId = (product.getCategoryId() == categoryId);
                // Filter by Name (Fallback)
                boolean matchName = (categoryName != null && product.getCategory() != null && product.getCategory().equalsIgnoreCase(categoryName));
                
                if (matchId || matchName) {
                    currentProductList.add(product);
                }
            }
        }
        // Sort products by name in ascending order
        currentProductList.sort((p1, p2) -> p1.getName().compareTo(p2.getName()));
        productAdapter.notifyDataSetChanged();
    }
}
