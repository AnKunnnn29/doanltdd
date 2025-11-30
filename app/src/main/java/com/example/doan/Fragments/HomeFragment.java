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
    
    private int selectedCategoryId = -1; 

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Setup Product RecyclerView
        productRecyclerView = view.findViewById(R.id.product_recycler_view);
        // Sử dụng GridLayoutManager với 2 cột
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
                        
                        Category allCategory = new Category();
                        allCategory.setId(-1);
                        allCategory.setName("Tất cả");
                        categoryList.add(allCategory);
                        
                        categoryList.addAll(apiResponse.getData());
                        categoryAdapter.notifyDataSetChanged();
                        
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
                        
                        String baseUrl = RetrofitClient.getBaseUrl(); 
                        String rootUrl = baseUrl.replace("/api/", ""); 
                        if (rootUrl.endsWith("/")) rootUrl = rootUrl.substring(0, rootUrl.length() - 1);

                        for (Drink drink : apiResponse.getData()) {
                            String imageUrl = drink.getImageUrl();
                            if (imageUrl != null && !imageUrl.startsWith("http")) {
                                if (!imageUrl.startsWith("/")) imageUrl = "/" + imageUrl;
                                imageUrl = rootUrl + imageUrl;
                            }

                            int pCategoryId = drink.getCategoryId();
                            
                            // Log để kiểm tra ID từ Server trả về
                            Log.d("HomeFragment", "Sản phẩm: " + drink.getName() + " - Category ID: " + pCategoryId);

                            String categoryDisplayName = "";
                            for(Category c : categoryList) {
                                if(c.getId() == pCategoryId) {
                                    categoryDisplayName = c.getName();
                                    break;
                                }
                            }
                            if(categoryDisplayName.isEmpty()) categoryDisplayName = drink.getCategoryName();


                            Product product = new Product(
                                drink.getId(),
                                drink.getName(),
                                drink.getDescription() != null ? drink.getDescription() : "",
                                drink.getBasePrice(),
                                categoryDisplayName, 
                                pCategoryId,         
                                imageUrl,
                                drink.isActive()
                            );
                            product.setSizes(drink.getSizes());
                            
                            allProducts.add(product);
                        }

                        updateCategoryImages();
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
                cat.setImage(defaultImage);
            } else {
                for (Product p : allProducts) {
                    if (p.getCategoryId() == cat.getId()) {
                        cat.setImage(p.getImageUrl());
                        break; 
                    }
                }
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
            currentProductList.addAll(allProducts);
        } else {
            for (Product product : allProducts) {
                if (product.getCategoryId() == categoryId) {
                    currentProductList.add(product);
                }
            }
        }
        
        // DEBUG: Hiển thị số lượng tìm thấy để kiểm tra
        if (categoryId != -1) {
            if (currentProductList.isEmpty()) {
                Toast.makeText(getContext(), "Không tìm thấy sản phẩm nào cho danh mục ID: " + categoryId, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Tìm thấy " + currentProductList.size() + " sản phẩm", Toast.LENGTH_SHORT).show();
            }
        }
        
        currentProductList.sort((p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()));
        productAdapter.notifyDataSetChanged();
    }
}