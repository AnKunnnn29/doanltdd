package com.example.doan.Activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan.Adapters.CartAdapter;
import com.example.doan.Models.ApiResponse;
import com.example.doan.Models.CartItem;
import com.example.doan.Models.CreateOrderRequest;
import com.example.doan.Models.OrderItemRequest;
import com.example.doan.Models.Order;
import com.example.doan.Network.RetrofitClient;
import com.example.doan.R;
import com.example.doan.Utils.CartManager;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CartActivity extends AppCompatActivity implements CartAdapter.OnCartChangeListener {

    private RecyclerView recyclerCart;
    private CartAdapter cartAdapter;
    private TextView tvTotalCartPrice, tvEmptyCart;
    private MaterialButton btnCheckout;
    private ImageView btnBack;

    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_USER_ID = "userId";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        initViews();
        setupRecyclerView();
        updateCartUI();
    }

    private void initViews() {
        recyclerCart = findViewById(R.id.recycler_cart);
        tvTotalCartPrice = findViewById(R.id.tv_total_cart_price);
        tvEmptyCart = findViewById(R.id.tv_empty_cart);
        btnCheckout = findViewById(R.id.btn_checkout);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());
        btnCheckout.setOnClickListener(v -> handleCheckout());
    }

    private void setupRecyclerView() {
        recyclerCart.setLayoutManager(new LinearLayoutManager(this));
        cartAdapter = new CartAdapter(this, CartManager.getInstance().getCartItems(), this);
        recyclerCart.setAdapter(cartAdapter);
    }

    private void updateCartUI() {
        double total = CartManager.getInstance().getTotalPrice();
        tvTotalCartPrice.setText(String.format(Locale.getDefault(), "%,.0f VNĐ", total));

        if (CartManager.getInstance().getItemCount() == 0) {
            tvEmptyCart.setVisibility(View.VISIBLE);
            recyclerCart.setVisibility(View.GONE);
            btnCheckout.setEnabled(false);
        } else {
            tvEmptyCart.setVisibility(View.GONE);
            recyclerCart.setVisibility(View.VISIBLE);
            btnCheckout.setEnabled(true);
        }
    }

    @Override
    public void onCartChanged() {
        updateCartUI();
    }

    private void handleCheckout() {
        List<CartItem> cartItems = CartManager.getInstance().getCartItems();
        if (cartItems.isEmpty()) return;

        int userId = getLoggedInUserId();
        if (userId == -1) {
            Toast.makeText(this, "Vui lòng đăng nhập để thanh toán", Toast.LENGTH_SHORT).show();
            return;
        }

        // Chuẩn bị dữ liệu gửi lên Server
        List<OrderItemRequest> orderItems = new ArrayList<>();
        for (CartItem item : cartItems) {
            orderItems.add(new OrderItemRequest(
                    item.getProduct().getId(),
                    item.getSizeName(),
                    item.getQuantity(),
                    item.getUnitPrice()
            ));
        }

        // Giả sử storeId là 1 (Mặc định) hoặc cần logic chọn Store trước khi Checkout
        // Để đơn giản, ta lấy storeId = 1 tạm thời, hoặc cần thêm UI chọn Store trong CartActivity
        int storeId = 1; 
        double totalPrice = CartManager.getInstance().getTotalPrice();

        CreateOrderRequest request = new CreateOrderRequest(userId, storeId, totalPrice, orderItems);

        btnCheckout.setEnabled(false);
        btnCheckout.setText("Đang xử lý...");

        RetrofitClient.getInstance(this).getApiService().createOrder(request).enqueue(new Callback<ApiResponse<Order>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Order>> call, @NonNull Response<ApiResponse<Order>> response) {
                btnCheckout.setEnabled(true);
                btnCheckout.setText("THANH TOÁN");

                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isSuccess()) {
                        Toast.makeText(CartActivity.this, "Đặt hàng thành công!", Toast.LENGTH_LONG).show();
                        CartManager.getInstance().clearCart();
                        finish();
                    } else {
                        Toast.makeText(CartActivity.this, "Lỗi: " + response.body().getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(CartActivity.this, "Lỗi Server: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Order>> call, @NonNull Throwable t) {
                btnCheckout.setEnabled(true);
                btnCheckout.setText("THANH TOÁN");
                Toast.makeText(CartActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int getLoggedInUserId() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_USER_ID, -1);
    }
}
