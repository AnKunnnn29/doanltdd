package com.example.doan.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.doan.Models.AddToCartRequest;
import com.example.doan.Models.ApiResponse;
import com.example.doan.Models.CreateOrderRequest;
import com.example.doan.Models.Drink;
import com.example.doan.Models.DrinkSize;
import com.example.doan.Models.DrinkTopping;
import com.example.doan.Models.Order;
import com.example.doan.Models.OrderItemRequest;
import com.example.doan.Models.Store;
import com.example.doan.R;
import com.example.doan.Network.RetrofitClient;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductDetailActivity extends AppCompatActivity {
    private static final String TAG = "ProductDetailActivity";

    private ImageView ivProductImage;
    private TextView tvProductName, tvProductPrice, tvProductDescription;
    private TextView tvQuantity, tvTotalPrice;
    private com.google.android.material.button.MaterialButton btnDecrease, btnIncrease;
    private com.google.android.material.button.MaterialButton btnAddToCart, btnConfirmOrder;
    private Spinner spinnerSize, spinnerBranch;
    private LinearLayout layoutToppings;
    private ImageButton btnBack;

    private Drink product;
    private int quantity = 1;
    private DrinkSize selectedSize;
    private final Set<DrinkTopping> selectedToppings = new HashSet<>();
    private List<Store> storeList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        initViews();
        getProductDetails();
        loadStores();
        setupListeners();
    }

    private int getLoggedInUserId() {
        // Thử SessionManager trước
        com.example.doan.Utils.SessionManager sessionManager = new com.example.doan.Utils.SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            return sessionManager.getUserId();
        }
        
        // Fallback: Thử SharedPreferences cũ
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        return sharedPreferences.getInt("userId", -1);
    }

    private void initViews() {
        ivProductImage = findViewById(R.id.ivProductImage);
        tvProductName = findViewById(R.id.tvProductName);
        tvProductPrice = findViewById(R.id.tvProductPrice);
        tvProductDescription = findViewById(R.id.tvProductDescription);
        tvQuantity = findViewById(R.id.tvQuantity);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        btnDecrease = findViewById(R.id.btnDecrease);
        btnIncrease = findViewById(R.id.btnIncrease);
        btnAddToCart = findViewById(R.id.btnAddToCart);
        btnConfirmOrder = findViewById(R.id.btnConfirmOrder);
        spinnerSize = findViewById(R.id.spinnerSize);
        spinnerBranch = findViewById(R.id.spinnerBranch);
        layoutToppings = findViewById(R.id.layoutToppings);

        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    private void getProductDetails() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("product")) {
            String productJson = intent.getStringExtra("product");
            Log.d(TAG, "Product JSON: " + productJson);
            try {
                // Try parsing as Drink first
                product = new Gson().fromJson(productJson, Drink.class);
                if (product != null) {
                    Log.d(TAG, "Product parsed successfully: " + product.getName());
                    Log.d(TAG, "Product ID: " + product.getId());
                    Log.d(TAG, "Product basePrice: " + product.getBasePrice());
                    Log.d(TAG, "Product imageUrl: " + product.getImageUrl());
                    displayProductDetails();
                } else {
                    Log.e(TAG, "Product is null after parsing");
                    Toast.makeText(this, "Không thể tải chi tiết sản phẩm", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing product: " + e.getMessage());
                e.printStackTrace();
                Toast.makeText(this, "Lỗi tải sản phẩm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Log.e(TAG, "No product extra in intent");
            Toast.makeText(this, "Không có thông tin sản phẩm", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void displayProductDetails() {
        try {
            Log.d(TAG, "Displaying product details");
            
            if (product.getName() != null) {
                tvProductName.setText(product.getName());
            } else {
                tvProductName.setText("Tên sản phẩm");
            }
            
            if (product.getDescription() != null) {
                tvProductDescription.setText(product.getDescription());
            } else {
                tvProductDescription.setText("Không có mô tả");
            }
            
            tvProductPrice.setText(String.format(Locale.getDefault(), "%,.0f VNĐ", product.getBasePrice()));

            if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
                Log.d(TAG, "Loading image: " + product.getImageUrl());
                Glide.with(this)
                    .load(product.getImageUrl())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .into(ivProductImage);
            }

            setupSizeSpinner();
            setupToppingCheckboxes();
            updateTotalPrice();
            
            Log.d(TAG, "Product details displayed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error displaying product details: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Lỗi hiển thị sản phẩm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadStores() {
        RetrofitClient.getInstance(this).getApiService().getStores().enqueue(new Callback<ApiResponse<List<Store>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Store>>> call, @NonNull Response<ApiResponse<List<Store>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    storeList = response.body().getData();
                    if (storeList != null && !storeList.isEmpty()) {
                        List<String> storeNames = new ArrayList<>();
                        for (Store store : storeList) {
                            storeNames.add(store.getStoreName());
                        }
                        ArrayAdapter<String> storeAdapter = new ArrayAdapter<>(ProductDetailActivity.this, android.R.layout.simple_spinner_item, storeNames);
                        storeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerBranch.setAdapter(storeAdapter);
                    }
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
        
        btnAddToCart.setOnClickListener(v -> addToCart());
    }

    private void setupSizeSpinner() {
        if (product.getSizes() == null || product.getSizes().isEmpty()) {
            spinnerSize.setVisibility(View.GONE);
            // Không set selectedSize, sẽ gửi null lên backend
            selectedSize = null;
            return;
        }
        
        List<String> sizeOptions = new ArrayList<>();
        for (DrinkSize size : product.getSizes()) {
            String option = size.getSizeName();
            if (size.getExtraPrice() > 0) {
                option += " (+" + String.format(Locale.getDefault(), "%,.0f VNĐ", size.getExtraPrice()) + ")";
            }
            sizeOptions.add(option);
        }
        
        ArrayAdapter<String> sizeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sizeOptions);
        sizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSize.setAdapter(sizeAdapter);
        
        // Set default selection
        selectedSize = product.getSizes().get(0);
        
        spinnerSize.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedSize = product.getSizes().get(position);
                updateTotalPrice();
                Log.d(TAG, "Size selected: " + selectedSize.getSizeName() + ", Extra: " + selectedSize.getExtraPrice());
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }
    
    private void setupToppingCheckboxes() {
        if (product.getToppings() == null || product.getToppings().isEmpty()) {
            layoutToppings.setVisibility(View.GONE);
            return;
        }
        
        layoutToppings.removeAllViews();
        
        for (DrinkTopping topping : product.getToppings()) {
            if (!topping.isActive()) continue;
            
            android.widget.CheckBox checkBox = new android.widget.CheckBox(this);
            checkBox.setText(topping.getToppingName() + " (+" + String.format(Locale.getDefault(), "%,.0f VNĐ", topping.getPrice()) + ")");
            checkBox.setTextSize(14);
            checkBox.setPadding(8, 8, 8, 8);
            
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedToppings.add(topping);
                    Log.d(TAG, "Topping added: " + topping.getToppingName());
                } else {
                    selectedToppings.remove(topping);
                    Log.d(TAG, "Topping removed: " + topping.getToppingName());
                }
                updateTotalPrice();
            });
            
            layoutToppings.addView(checkBox);
        }
    }
    
    private void updateTotalPrice() {
        double basePrice = product.getBasePrice();
        double sizeExtra = selectedSize != null ? selectedSize.getExtraPrice() : 0;
        double toppingTotal = 0;
        
        for (DrinkTopping topping : selectedToppings) {
            toppingTotal += topping.getPrice();
        }
        
        double itemPrice = basePrice + sizeExtra + toppingTotal;
        double total = itemPrice * quantity;
        
        tvTotalPrice.setText(String.format(Locale.getDefault(), "%,.0f VNĐ", total));
        
        Log.d(TAG, "Price breakdown - Base: " + basePrice + ", Size: " + sizeExtra + ", Toppings: " + toppingTotal + ", Total: " + total);
    }

    private void addToCart() {
        int userId = getLoggedInUserId();
        if (userId == -1) {
            Toast.makeText(this, "Vui lòng đăng nhập để thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Tạo danh sách topping IDs
        List<Long> toppingIds = new ArrayList<>();
        for (DrinkTopping topping : selectedToppings) {
            toppingIds.add((long) topping.getId());
        }
        
        // Tạo note với thông tin topping
        StringBuilder note = new StringBuilder();
        if (!selectedToppings.isEmpty()) {
            note.append("Topping: ");
            for (int i = 0; i < selectedToppings.size(); i++) {
                if (i > 0) note.append(", ");
                note.append(selectedToppings.toArray(new DrinkTopping[0])[i].getToppingName());
            }
        }
        
        // Nếu không có size, gửi null
        Long sizeId = selectedSize != null ? (long) selectedSize.getId() : null;
        
        com.example.doan.Models.AddToCartRequest request = new com.example.doan.Models.AddToCartRequest(
            (long) product.getId(),
            sizeId,
            quantity,
            toppingIds.isEmpty() ? null : toppingIds,
            note.toString()
        );
        
        btnAddToCart.setEnabled(false);
        btnAddToCart.setText("Đang thêm...");
        
        RetrofitClient.getInstance(this).getApiService().addToCart(userId, request)
            .enqueue(new Callback<ApiResponse<com.example.doan.Models.Cart>>() {
                @Override
                public void onResponse(@NonNull Call<ApiResponse<com.example.doan.Models.Cart>> call, 
                                     @NonNull Response<ApiResponse<com.example.doan.Models.Cart>> response) {
                    btnAddToCart.setEnabled(true);
                    btnAddToCart.setText("THÊM VÀO GIỎ");
                    
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        // Hiển thị dialog hỏi user
                        new androidx.appcompat.app.AlertDialog.Builder(ProductDetailActivity.this)
                            .setTitle("Thêm vào giỏ hàng thành công!")
                            .setMessage("Bạn có muốn xem giỏ hàng ngay không?\n\n⚠️ Lưu ý: Giỏ hàng sẽ tự động xóa sau 5 phút nếu không đặt hàng.")
                            .setPositiveButton("Xem giỏ hàng", (dialog, which) -> {
                                startActivity(new Intent(ProductDetailActivity.this, CartActivity.class));
                            })
                            .setNegativeButton("Tiếp tục mua", (dialog, which) -> {
                                dialog.dismiss();
                            })
                            .show();
                    } else {
                        String message = response.body() != null ? response.body().getMessage() : "Lỗi thêm vào giỏ hàng";
                        Toast.makeText(ProductDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                }
                
                @Override
                public void onFailure(@NonNull Call<ApiResponse<com.example.doan.Models.Cart>> call, @NonNull Throwable t) {
                    btnAddToCart.setEnabled(true);
                    btnAddToCart.setText("THÊM VÀO GIỎ");
                    Log.e(TAG, "Lỗi thêm vào giỏ hàng: " + t.getMessage());
                    Toast.makeText(ProductDetailActivity.this, 
                        "Không thể kết nối đến máy chủ", Toast.LENGTH_SHORT).show();
                }
            });
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
        
        long storeId = storeList.get(selectedStoreIndex).getId();
        
        int userId = getLoggedInUserId();
        if (userId == -1) {
            Toast.makeText(this, "Vui lòng đăng nhập để đặt hàng", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Tạo danh sách topping IDs
        List<Long> toppingIds = new ArrayList<>();
        for (DrinkTopping topping : selectedToppings) {
            toppingIds.add((long) topping.getId());
        }
        
        // Tạo note với thông tin topping
        StringBuilder note = new StringBuilder();
        if (!selectedToppings.isEmpty()) {
            note.append("Topping: ");
            for (int i = 0; i < selectedToppings.size(); i++) {
                if (i > 0) note.append(", ");
                note.append(selectedToppings.toArray(new DrinkTopping[0])[i].getToppingName());
            }
        }
        
        // Nếu không có size, dùng "M" làm mặc định
        String sizeName = selectedSize != null ? selectedSize.getSizeName() : "M";
        
        OrderItemRequest item = new OrderItemRequest(
            (long) product.getId(), 
            sizeName, 
            quantity, 
            note.toString(),
            toppingIds.isEmpty() ? null : toppingIds
        );

        CreateOrderRequest orderRequest = new CreateOrderRequest(storeId, "PICKUP", "Tại cửa hàng", "COD", Collections.singletonList(item));

        btnConfirmOrder.setEnabled(false);
        btnConfirmOrder.setText("Đang xử lý...");

        RetrofitClient.getInstance(this).getApiService().createOrder(orderRequest).enqueue(new Callback<ApiResponse<Order>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Order>> call, @NonNull Response<ApiResponse<Order>> response) {
                btnConfirmOrder.setEnabled(true);
                btnConfirmOrder.setText("MUA NGAY");

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
                btnConfirmOrder.setText("MUA NGAY");
                Log.e(TAG, "Đặt hàng thất bại: " + t.getMessage());
                Toast.makeText(ProductDetailActivity.this, "Không thể kết nối đến máy chủ", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
