package com.example.doan.Activities

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Adapters.CartAdapter
import com.example.doan.Models.*
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class CartActivity : AppCompatActivity() {

    private lateinit var recyclerCart: RecyclerView
    private lateinit var tvTotalCartPrice: TextView
    private lateinit var tvEmptyCart: TextView
    private lateinit var tvCountdown: TextView
    private lateinit var btnCheckout: MaterialButton
    private lateinit var btnBack: ImageView

    private var currentCart: Cart? = null
    private var cartAdapter: CartAdapter? = null
    private var countdownHandler: Handler? = null
    private var countdownRunnable: Runnable? = null
    private var countdownTimeLeft = 300000L // 5 phút

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        initViews()
        setupRecyclerView()
        loadCartFromServer()
        startCountdown()
    }

    private fun initViews() {
        recyclerCart = findViewById(R.id.recycler_cart)
        tvTotalCartPrice = findViewById(R.id.tv_total_cart_price)
        tvEmptyCart = findViewById(R.id.tv_empty_cart)
        tvCountdown = findViewById(R.id.tv_countdown)
        btnCheckout = findViewById(R.id.btn_checkout)
        btnBack = findViewById(R.id.btn_back)

        btnBack.setOnClickListener { finish() }
        btnCheckout.setOnClickListener { handleCheckout() }
    }

    private fun setupRecyclerView() {
        recyclerCart.layoutManager = LinearLayoutManager(this)
    }

    private fun loadCartFromServer() {
        val userId = getLoggedInUserId()
        if (userId == -1) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        RetrofitClient.getInstance(this).apiService.getCart(userId.toLong())
            .enqueue(object : Callback<ApiResponse<Cart>> {
                override fun onResponse(call: Call<ApiResponse<Cart>>, response: Response<ApiResponse<Cart>>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        currentCart = response.body()?.data
                        updateCartUI()
                    } else {
                        Toast.makeText(this@CartActivity, "Không thể tải giỏ hàng", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Cart>>, t: Throwable) {
                    Log.e(TAG, "Error loading cart: ${t.message}")
                    Toast.makeText(this@CartActivity, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateCartUI() {
        if (currentCart?.items.isNullOrEmpty()) {
            tvEmptyCart.visibility = View.VISIBLE
            recyclerCart.visibility = View.GONE
            tvCountdown.visibility = View.GONE
            btnCheckout.isEnabled = false
            stopCountdown()
        } else {
            tvEmptyCart.visibility = View.GONE
            recyclerCart.visibility = View.VISIBLE
            tvCountdown.visibility = View.VISIBLE
            btnCheckout.isEnabled = true
            
            tvTotalCartPrice.text = String.format(Locale.getDefault(), "%,.0f VNĐ", currentCart?.totalAmount)
            
            cartAdapter = CartAdapter(this, currentCart?.items ?: emptyList(), object : CartAdapter.OnCartChangeListener {
                override fun onCartChanged() {
                    loadCartFromServer()
                }
            })
            recyclerCart.adapter = cartAdapter
        }
    }

    private fun startCountdown() {
        countdownHandler = Handler(Looper.getMainLooper())
        countdownRunnable = object : Runnable {
            override fun run() {
                if (countdownTimeLeft > 0) {
                    countdownTimeLeft -= COUNTDOWN_INTERVAL
                    updateCountdownDisplay()
                    countdownHandler?.postDelayed(this, COUNTDOWN_INTERVAL)
                } else {
                    clearCartOnServer()
                }
            }
        }
        countdownHandler?.post(countdownRunnable!!)
    }

    private fun stopCountdown() {
        countdownHandler?.removeCallbacks(countdownRunnable!!)
    }

    private fun updateCountdownDisplay() {
        val seconds = countdownTimeLeft / 1000
        tvCountdown.text = String.format(Locale.getDefault(), "Giỏ hàng sẽ tự động xóa sau: %d giây", seconds)
    }

    private fun clearCartOnServer() {
        val userId = getLoggedInUserId()
        if (userId == -1) return

        RetrofitClient.getInstance(this).apiService.clearCart(userId.toLong())
            .enqueue(object : Callback<ApiResponse<Void>> {
                override fun onResponse(call: Call<ApiResponse<Void>>, response: Response<ApiResponse<Void>>) {
                    Toast.makeText(this@CartActivity, "Giỏ hàng đã bị xóa do hết thời gian", Toast.LENGTH_LONG).show()
                    finish()
                }

                override fun onFailure(call: Call<ApiResponse<Void>>, t: Throwable) {
                    Log.e(TAG, "Error clearing cart: ${t.message}")
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCountdown()
    }

    private fun handleCheckout() {
        if (currentCart?.items.isNullOrEmpty()) {
            Toast.makeText(this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = getLoggedInUserId()
        if (userId == -1) {
            Toast.makeText(this, "Vui lòng đăng nhập để thanh toán", Toast.LENGTH_SHORT).show()
            return
        }

        showPaymentMethodDialog()
    }

    private fun showPaymentMethodDialog() {
        val paymentMethods = arrayOf("💵 Thanh toán khi nhận hàng (COD)", "💳 Thanh toán VNPay")
        
        AlertDialog.Builder(this)
            .setTitle("Chọn phương thức thanh toán")
            .setItems(paymentMethods) { _, which ->
                val paymentMethod = if (which == 0) "COD" else "VNPAY"
                processOrder(paymentMethod)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun processOrder(paymentMethod: String) {
        stopCountdown()

        val userId = getLoggedInUserId()
        if (userId == -1 || currentCart?.items.isNullOrEmpty()) return

        val orderItems = currentCart?.items?.map { item ->
            val toppingIds = item.toppings?.map { it.id.toLong() }
            OrderItemRequest(
                item.drinkId ?: 0L,
                item.sizeName ?: "M",
                item.quantity ?: 1,
                item.note,
                toppingIds
            )
        } ?: emptyList()

        val request = CreateOrderRequest(1L, "PICKUP", "Tại cửa hàng", paymentMethod, null, null, orderItems)

        btnCheckout.isEnabled = false
        btnCheckout.text = "Đang xử lý..."

        RetrofitClient.getInstance(this).apiService.createOrder(request)
            .enqueue(object : Callback<ApiResponse<Order>> {
                override fun onResponse(call: Call<ApiResponse<Order>>, response: Response<ApiResponse<Order>>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val order = response.body()?.data
                        
                        if (paymentMethod == "COD") {
                            clearCartAfterOrder(userId)
                            showSuccessAndTrackOrder(order)
                        } else {
                            Toast.makeText(this@CartActivity, "Đang chuyển đến VNPay...", Toast.LENGTH_SHORT).show()
                            handleVNPayPayment(order)
                        }
                    } else {
                        btnCheckout.isEnabled = true
                        btnCheckout.text = "THANH TOÁN"
                        val message = response.body()?.message ?: "Lỗi đặt hàng"
                        Toast.makeText(this@CartActivity, message, Toast.LENGTH_LONG).show()
                        startCountdown()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Order>>, t: Throwable) {
                    btnCheckout.isEnabled = true
                    btnCheckout.text = "THANH TOÁN"
                    Toast.makeText(this@CartActivity, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                    startCountdown()
                }
            })
    }
    
    private fun handleVNPayPayment(order: Order?) {
        AlertDialog.Builder(this)
            .setTitle("VNPay Payment")
            .setMessage("Chức năng thanh toán VNPay đang được phát triển.\n\nMã đơn hàng: #${order?.id}")
            .setPositiveButton("OK") { _, _ ->
                clearCartAfterOrder(getLoggedInUserId())
            }
            .show()
    }

    private fun clearCartAfterOrder(userId: Int) {
        RetrofitClient.getInstance(this).apiService.clearCart(userId.toLong())
            .enqueue(object : Callback<ApiResponse<Void>> {
                override fun onResponse(call: Call<ApiResponse<Void>>, response: Response<ApiResponse<Void>>) {}
                override fun onFailure(call: Call<ApiResponse<Void>>, t: Throwable) {}
            })
    }
    
    private fun showSuccessAndTrackOrder(order: Order?) {
        AlertDialog.Builder(this)
            .setTitle("🎉 Cảm ơn bạn!")
            .setMessage("Đơn hàng #${order?.id} đã được đặt thành công!\n\n" +
                       "Phương thức: Thanh toán khi nhận hàng\n" +
                       "Tổng tiền: ${String.format(Locale.getDefault(), "%,.0f VNĐ", currentCart?.totalAmount)}\n\n" +
                       "Trạng thái: Đang chờ xử lý\n" +
                       "📱 Bạn có thể xem đơn hàng trong mục 'Đơn hàng của tôi'")
            .setPositiveButton("OK") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun getLoggedInUserId(): Int {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_USER_ID, -1)
    }

    companion object {
        private const val TAG = "CartActivity"
        private const val PREFS_NAME = "UserPrefs"
        private const val KEY_USER_ID = "userId"
        private const val COUNTDOWN_INTERVAL = 1000L
    }
}
