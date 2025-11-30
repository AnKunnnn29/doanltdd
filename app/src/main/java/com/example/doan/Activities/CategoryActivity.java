package com.example.doan.Activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan.Models.ApiResponse;
import com.example.doan.Models.Drink;
import com.example.doan.Models.Product;
import com.example.doan.Adapters.ProductAdapter;
import com.example.doan.Network.ApiService;
import com.example.doan.Network.RetrofitClient;
import com.example.doan.R;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;

    private int categoryId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        categoryId = getIntent().getIntExtra("category_id", -1);
        if (categoryId == -1) {
            Toast.makeText(this, "Invalid Category ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        recyclerView = findViewById(R.id.recycler_view_products);
        progressBar = findViewById(R.id.progress_bar);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        loadProducts();
    }

    private void loadProducts() {
        progressBar.setVisibility(View.VISIBLE);
        ApiService apiService = RetrofitClient.getInstance(this).getApiService();
        Call<ApiResponse<List<Drink>>> call = apiService.getProductsByCategory(categoryId);

        call.enqueue(new Callback<ApiResponse<List<Drink>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Drink>>> call, Response<ApiResponse<List<Drink>>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Drink> newDrinks = response.body().getData();
                    if (newDrinks != null && !newDrinks.isEmpty()) {
                        List<Product> productList = new ArrayList<>();
                        for (Drink drink : newDrinks) {
                            if (drink == null) continue;

                            // Reverted to direct assignment as the getters return primitive types
                            int id = drink.getId();
                            String name = drink.getName() != null ? drink.getName() : "";
                            String description = drink.getDescription() != null ? drink.getDescription() : "";
                            double price = drink.getBasePrice();
                            String categoryName = drink.getCategoryName() != null ? drink.getCategoryName() : "";
                            String imageUrl = drink.getImageUrl() != null ? drink.getImageUrl() : "";
                            boolean isActive = drink.isActive();

                            productList.add(new Product(id, name, description, price, categoryName, imageUrl, isActive));
                        }
                        ProductAdapter productAdapter = new ProductAdapter(productList);
                        recyclerView.setAdapter(productAdapter);
                    } else {
                        Toast.makeText(CategoryActivity.this, "No products found in this category.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                     Toast.makeText(CategoryActivity.this, "Failed to load products for this category", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Drink>>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Log.e("CategoryActivity", "API call failed: " + t.getMessage());
                Toast.makeText(CategoryActivity.this, "An error occurred: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
