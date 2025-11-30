package com.example.doan.Activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.doan.Models.ApiResponse;
import com.example.doan.Models.CreateOrderRequest;
import com.example.doan.Models.OrderItemRequest;
import com.example.doan.Models.Order;
import com.example.doan.Models.Product;
import com.example.doan.Models.Store;
import com.example.doan.Network.RetrofitClient;
import com.example.doan.R;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductDetailActivity extends AppCompatActivity {

    private static final String TAG = "ProductDetailActivity";
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_USER_ID = "userId";

    private ImageView productImage;
    private TextView productName, productPrice, productDescription, tvQuantity, tvTotalPrice;
    private Spinner spinnerBranch;
    private MaterialButton btnDecrease, btnIncrease, btnConfirmOrder;

    private Product product;
    private int quantity = 1;
    private List<Store> storeList = new ArrayList<>();
    private ArrayAdapter<String> storeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        // Retrieve product from Intent
        if (getIntent().hasExtra("product")) {
            product = (Product) getIntent().getSerializableExtra("product");
        }

        if (product == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin sản phẩm", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupData();
        loadStores();
        setupListeners();
    }

    private void initViews() {
        productImage = findViewById(R.id.detail_product_image);
        productName = findViewById(R.id.detail_product_name);
        productPrice = findViewById(R.id.detail_product_price);
        productDescription = findViewById(R.id.detail_product_description);
        spinnerBranch = findViewById(R.id.spinner_branch);
        tvQuantity = findViewById(R.id.tv_quantity);
        tvTotalPrice = findViewById(R.id.tv_total_price);
        btnDecrease = findViewById(R.id.btn_decrease_qty);
        btnIncrease = findViewById(R.id.btn_increase_qty);
        btnConfirmOrder = findViewById(R.id.btn_confirm_order);
    }

    private void setupData() {
        productName.setText(product.getName());
        productPrice.setText(String.format(Locale.getDefault(), "%,.0f VNĐ", product.getPrice()));
        productDescription.setText(product.getDescription());
        
        Glide.with(this)
                .load(product.getImageUrl())
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_broken_image)
                .into(productImage);

        updateTotalPrice();
    }

    private void loadStores() {
        RetrofitClient.getInstance(this).getApiService().getStores().enqueue(new Callback<ApiResponse<List<Store>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Store>>> call, @NonNull Response<ApiResponse<List<Store>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    storeList = response.body().getData();
                    List<String> storeNames = new ArrayList<>();
                    for (Store store : storeList) {
                        storeNames.add(store.getStoreName() + " - " + store.getAddress());
                    }
                    
                    storeAdapter = new ArrayAdapter<>(ProductDetailActivity.this, android.R.layout.simple_spinner_item, storeNames);
                    storeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerBranch.setAdapter(storeAdapter);
                } else {
                    Toast.makeText(ProductDetailActivity.this, "Không thể tải danh sách cửa hàng", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Store>>> call, @NonNull Throwable t) {
                Log.e(TAG, "Lỗi tải danh sách cửa hàng: " + t.getMessage());
                Toast.makeText(ProductDetailActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        btnDecrease.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                tvQuantity.setText(String.valueOf(quantity));
                updateTotalPrice();
            }
        });

        btnIncrease.setOnClickListener(v -> {
            quantity++;
            tvQuantity.setText(String.valueOf(quantity));
            updateTotalPrice();
        });

        btnConfirmOrder.setOnClickListener(v -> placeOrder());
    }

    private void updateTotalPrice() {
        double total = quantity * product.getPrice();
        tvTotalPrice.setText(String.format(Locale.getDefault(), "%,.0f VNĐ", total));
    }

    private void placeOrder() {
        if (storeList.isEmpty()) {
            Toast.makeText(this, "Đang tải danh sách cửa hàng, vui lòng đợi...", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedStoreIndex = spinnerBranch.getSelectedItemPosition();
        if (selectedStoreIndex == -1) {
            Toast.makeText(this, "Vui lòng chọn cửa hàng", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int storeId = storeList.get(selectedStoreIndex).getId();
        int userId = getLoggedInUserId();

        if (userId == -1) {
            Toast.makeText(this, "Vui lòng đăng nhập để đặt hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        double totalAmount = quantity * product.getPrice();
        
        OrderItemRequest item = new OrderItemRequest(product.getId(), quantity, product.getPrice());
        CreateOrderRequest orderRequest = new CreateOrderRequest(userId, storeId, totalAmount, Collections.singletonList(item));

        btnConfirmOrder.setEnabled(false);
        btnConfirmOrder.setText("Đang xử lý...");

        RetrofitClient.getInstance(this).getApiService().createOrder(orderRequest).enqueue(new Callback<ApiResponse<Order>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Order>> call, @NonNull Response<ApiResponse<Order>> response) {
                btnConfirmOrder.setEnabled(true);
                btnConfirmOrder.setText("XÁC NHẬN ĐẶT HÀNG");

                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isSuccess()) {
                        Toast.makeText(ProductDetailActivity.this, "Đặt hàng thành công!", Toast.LENGTH_LONG).show();
                        finish(); 
                    } else {
                        String message = response.body().getMessage();
                        Toast.makeText(ProductDetailActivity.this, "Đặt hàng thất bại: " + message, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Đặt hàng thất bại: " + message);
                    }
                } else {
                    Toast.makeText(ProductDetailActivity.this, "Lỗi Server: " + response.code(), Toast.LENGTH_SHORT).show();
                    try {
                        Log.e(TAG, "Error Body: " + response.errorBody().string());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Order>> call, @NonNull Throwable t) {
                btnConfirmOrder.setEnabled(true);
                btnConfirmOrder.setText("XÁC NHẬN ĐẶT HÀNG");
                Log.e(TAG, "Lỗi đặt hàng: " + t.getMessage());
                Toast.makeText(ProductDetailActivity.this, "Không thể kết nối đến máy chủ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int getLoggedInUserId() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_USER_ID, -1);
    }
}