package com.example.doan.Activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
import com.example.doan.Models.Cart;
import com.example.doan.Models.CartItem;
import com.example.doan.Models.CreateOrderRequest;
import com.example.doan.Models.OrderItemRequest;
import com.example.doan.Models.Order;
import com.example.doan.Network.RetrofitClient;
import com.example.doan.R;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CartActivity extends AppCompatActivity {

    private static final String TAG = "CartActivity";
    private RecyclerView recyclerCart;
    private TextView tvTotalCartPrice, tvEmptyCart, tvCountdown;
    private MaterialButton btnCheckout;
    private ImageView btnBack;

    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_USER_ID = "userId";
    
    private Cart currentCart;
    private CartAdapter cartAdapter;
    private Handler countdownHandler;
    private Runnable countdownRunnable;
    private long countdownTimeLeft = 300000; // 5 phút = 300000ms
    private static final long COUNTDOWN_INTERVAL = 1000; // Update mỗi giây

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        initViews();
        setupRecyclerView();
        loadCartFromServer();
        startCountdown();
    }

    private void initViews() {
        recyclerCart = findViewById(R.id.recycler_cart);
        tvTotalCartPrice = findViewById(R.id.tv_total_cart_price);
        tvEmptyCart = findViewById(R.id.tv_empty_cart);
        tvCountdown = findViewById(R.id.tv_countdown);
        btnCheckout = findViewById(R.id.btn_checkout);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());
        btnCheckout.setOnClickListener(v -> handleCheckout());
    }

    private void setupRecyclerView() {
        recyclerCart.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadCartFromServer() {
        int userId = getLoggedInUserId();
        if (userId == -1) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        RetrofitClient.getInstance(this).getApiService().getCart(userId)
            .enqueue(new Callback<ApiResponse<Cart>>() {
                @Override
                public void onResponse(@NonNull Call<ApiResponse<Cart>> call, 
                                     @NonNull Response<ApiResponse<Cart>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        currentCart = response.body().getData();
                        updateCartUI();
                    } else {
                        Toast.makeText(CartActivity.this, "Không thể tải giỏ hàng", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ApiResponse<Cart>> call, @NonNull Throwable t) {
                    Log.e(TAG, "Error loading cart: " + t.getMessage());
                    Toast.makeText(CartActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void updateCartUI() {
        if (currentCart == null || currentCart.getItems() == null || currentCart.getItems().isEmpty()) {
            tvEmptyCart.setVisibility(View.VISIBLE);
            recyclerCart.setVisibility(View.GONE);
            tvCountdown.setVisibility(View.GONE);
            btnCheckout.setEnabled(false);
            stopCountdown();
        } else {
            tvEmptyCart.setVisibility(View.GONE);
            recyclerCart.setVisibility(View.VISIBLE);
            tvCountdown.setVisibility(View.VISIBLE);
            btnCheckout.setEnabled(true);
            
            tvTotalCartPrice.setText(String.format(Locale.getDefault(), "%,.0f VNĐ", currentCart.getTotalAmount()));
            
            // Cập nhật adapter với cart items từ server
            cartAdapter = new CartAdapter(this, currentCart.getItems(), () -> {
                // Callback khi cart thay đổi
                loadCartFromServer();
            });
            recyclerCart.setAdapter(cartAdapter);
        }
    }

    private void startCountdown() {
        countdownHandler = new Handler(Looper.getMainLooper());
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (countdownTimeLeft > 0) {
                    countdownTimeLeft -= COUNTDOWN_INTERVAL;
                    updateCountdownDisplay();
                    countdownHandler.postDelayed(this, COUNTDOWN_INTERVAL);
                } else {
                    // Hết thời gian - xóa giỏ hàng
                    clearCartOnServer();
                }
            }
        };
        countdownHandler.post(countdownRunnable);
    }

    private void stopCountdown() {
        if (countdownHandler != null && countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
        }
    }

    private void updateCountdownDisplay() {
        long seconds = countdownTimeLeft / 1000;
        tvCountdown.setText(String.format(Locale.getDefault(), 
            "Giỏ hàng sẽ tự động xóa sau: %d giây", seconds));
    }

    private void clearCartOnServer() {
        int userId = getLoggedInUserId();
        if (userId == -1) return;

        RetrofitClient.getInstance(this).getApiService().clearCart(userId)
            .enqueue(new Callback<ApiResponse<Void>>() {
                @Override
                public void onResponse(@NonNull Call<ApiResponse<Void>> call, 
                                     @NonNull Response<ApiResponse<Void>> response) {
                    Toast.makeText(CartActivity.this, 
                        "Giỏ hàng đã bị xóa do hết thời gian", Toast.LENGTH_LONG).show();
                    finish();
                }

                @Override
                public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                    Log.e(TAG, "Error clearing cart: " + t.getMessage());
                }
            });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCountdown();
    }

    private void handleCheckout() {
        if (currentCart == null || currentCart.getItems() == null || currentCart.getItems().isEmpty()) {
            Toast.makeText(this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
            return;
        }

        int userId = getLoggedInUserId();
        if (userId == -1) {
            Toast.makeText(this, "Vui lòng đăng nhập để thanh toán", Toast.LENGTH_SHORT).show();
            return;
        }

        // Hiển thị dialog chọn phương thức thanh toán
        showPaymentMethodDialog();
    }

    private void showPaymentMethodDialog() {
        String[] paymentMethods = {"💵 Thanh toán khi nhận hàng (COD)", "💳 Thanh toán VNPay"};
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Chọn phương thức thanh toán")
            .setItems(paymentMethods, (dialog, which) -> {
                if (which == 0) {
                    // COD - Thanh toán khi nhận hàng
                    processOrder("COD");
                } else {
                    // VNPay - Thanh toán online
                    processOrder("VNPAY");
                }
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void processOrder(String paymentMethod) {
        // Dừng countdown khi đang checkout
        stopCountdown();

        int userId = getLoggedInUserId();
        if (userId == -1) {
            Toast.makeText(this, "Vui lòng đăng nhập để thanh toán", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentCart == null || currentCart.getItems() == null || currentCart.getItems().isEmpty()) {
            Toast.makeText(this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
            return;
        }

        // Chuyển cart items thành order items
        List<OrderItemRequest> orderItems = new ArrayList<>();
        for (CartItem item : currentCart.getItems()) {
            List<Long> toppingIds = new ArrayList<>();
            if (item.getToppings() != null) {
                for (com.example.doan.Models.DrinkTopping topping : item.getToppings()) {
                    toppingIds.add((long) topping.getId());
                }
            }
            
            orderItems.add(new OrderItemRequest(
                item.getDrinkId(),
                item.getSizeName(),
                item.getQuantity(),
                item.getNote(),
                toppingIds.isEmpty() ? null : toppingIds
            ));
        }

        // Set default values
        Long storeId = 1L;
        String type = "PICKUP";
        String address = "Tại cửa hàng";

        CreateOrderRequest request = new CreateOrderRequest(storeId, type, address, paymentMethod, orderItems);

        btnCheckout.setEnabled(false);
        btnCheckout.setText("Đang xử lý...");

        RetrofitClient.getInstance(this).getApiService().createOrder(request)
            .enqueue(new Callback<ApiResponse<Order>>() {
                @Override
                public void onResponse(@NonNull Call<ApiResponse<Order>> call, 
                                     @NonNull Response<ApiResponse<Order>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        Order order = response.body().getData();
                        
                        if ("COD".equals(paymentMethod)) {
                            // COD - Thanh toán khi nhận hàng → Hoàn thành đơn luôn
                            clearCartAfterOrder(userId);
                            showSuccessAndTrackOrder(order);
                        } else {
                            // VNPay - Chuyển sang màn hình thanh toán
                            Toast.makeText(CartActivity.this, 
                                "Đang chuyển đến VNPay...", 
                                Toast.LENGTH_SHORT).show();
                            handleVNPayPayment(order);
                        }
                    } else {
                        btnCheckout.setEnabled(true);
                        btnCheckout.setText("THANH TOÁN");
                        String message = response.body() != null ? response.body().getMessage() : "Lỗi đặt hàng";
                        Toast.makeText(CartActivity.this, message, Toast.LENGTH_LONG).show();
                        startCountdown();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ApiResponse<Order>> call, @NonNull Throwable t) {
                    btnCheckout.setEnabled(true);
                    btnCheckout.setText("THANH TOÁN");
                    Toast.makeText(CartActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    startCountdown();
                }
            });
    }
    
    private void handleVNPayPayment(Order order) {
        // TODO: Implement VNPay payment integration
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("VNPay Payment")
            .setMessage("Chức năng thanh toán VNPay đang được phát triển.\n\nMã đơn hàng: #" + order.getId())
            .setPositiveButton("OK", (dialog, which) -> {
                clearCartAfterOrder(getLoggedInUserId());
            })
            .show();
    }

    private void clearCartAfterOrder(int userId) {
        RetrofitClient.getInstance(this).getApiService().clearCart(userId)
            .enqueue(new Callback<ApiResponse<Void>>() {
                @Override
                public void onResponse(@NonNull Call<ApiResponse<Void>> call, 
                                     @NonNull Response<ApiResponse<Void>> response) {
                    // Cart cleared successfully
                }

                @Override
                public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                    // Ignore error - order already created
                }
            });
    }
    
    private void showSuccessAndTrackOrder(Order order) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("🎉 Cảm ơn bạn!")
            .setMessage("Đơn hàng #" + order.getId() + " đã được đặt thành công!\n\n" +
                       "Phương thức: Thanh toán khi nhận hàng\n" +
                       "Tổng tiền: " + String.format(Locale.getDefault(), "%,.0f VNĐ", currentCart.getTotalAmount()) + "\n\n" +
                       "Trạng thái: Đang chờ xử lý\n" +
                       "📱 Bạn có thể xem đơn hàng trong mục 'Đơn hàng của tôi'")
            .setPositiveButton("OK", (dialog, which) -> {
                finish();
            })
            .setCancelable(false)
            .show();
    }

    private int getLoggedInUserId() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_USER_ID, -1);
    }
}
