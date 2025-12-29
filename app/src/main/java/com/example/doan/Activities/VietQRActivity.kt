package com.example.doan.Activities

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.doan.Adapters.OrderSummaryAdapter
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.BankInfo
import com.example.doan.Models.CreateOrderRequest
import com.example.doan.Models.Order
import com.example.doan.Models.OrderSummaryItem
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.SessionManager
import com.example.doan.Utils.VietQRService
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VietQRActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "VietQRActivity"
    }

    private lateinit var imgQrCode: ImageView
    private lateinit var progressQrLoading: ProgressBar
    private lateinit var tvOrderId: TextView
    private lateinit var tvTotalAmount: TextView
    private lateinit var tvBankName: TextView
    private lateinit var tvAccountNumber: TextView
    private lateinit var tvAccountName: TextView
    private lateinit var tvTransferContent: TextView
    private lateinit var rvOrderItems: RecyclerView
    private lateinit var btnBackToCart: Button
    private lateinit var btnPaymentCompleted: Button
    
    private var orderId: Long = 0
    private var totalAmount: Double = 0.0
    private var orderItems: List<OrderSummaryItem> = emptyList()
    private lateinit var orderSummaryAdapter: OrderSummaryAdapter
    private var orderRequestJson: String? = null
    
    // Track API calls for cancellation
    private var createOrderCall: Call<ApiResponse<Order>>? = null
    private var clearCartCall: Call<ApiResponse<Void>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vietqr_payment)

        initViews()
        getIntentData()
        setupOrderSummary()
        generateQRCode()
        setupListeners()
        setupBackPressHandler()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Cancel pending API calls
        createOrderCall?.cancel()
        clearCartCall?.cancel()
        Log.d(TAG, "Cancelled pending API calls")
    }

    private fun initViews() {
        imgQrCode = findViewById(R.id.img_qr_code)
        progressQrLoading = findViewById(R.id.progress_qr_loading)
        tvOrderId = findViewById(R.id.tv_order_id)
        tvTotalAmount = findViewById(R.id.tv_total_amount)
        tvBankName = findViewById(R.id.tv_bank_name)
        tvAccountNumber = findViewById(R.id.tv_account_number)
        tvAccountName = findViewById(R.id.tv_account_name)
        tvTransferContent = findViewById(R.id.tv_transfer_content)
        rvOrderItems = findViewById(R.id.rv_order_items)
        btnBackToCart = findViewById(R.id.btn_back_to_cart)
        btnPaymentCompleted = findViewById(R.id.btn_payment_completed)
        
        // Setup toolbar with cancel confirmation
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_vietqr).setNavigationOnClickListener { 
            showCancelPaymentDialog()
        }
        
        // Setup RecyclerView
        orderSummaryAdapter = OrderSummaryAdapter(orderItems)
        rvOrderItems.layoutManager = LinearLayoutManager(this)
        rvOrderItems.adapter = orderSummaryAdapter
    }
    
    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showCancelPaymentDialog()
            }
        })
    }
    
    private fun getIntentData() {
        orderId = intent.getLongExtra("ORDER_ID", System.currentTimeMillis())
        totalAmount = intent.getDoubleExtra("TOTAL_AMOUNT", 0.0)
        orderRequestJson = intent.getStringExtra("ORDER_REQUEST")
        
        // Parse order items from intent
        val itemsJson = intent.getStringExtra("ORDER_ITEMS")
        if (!itemsJson.isNullOrEmpty()) {
            try {
                val type = object : com.google.gson.reflect.TypeToken<List<OrderSummaryItem>>() {}.type
                orderItems = Gson().fromJson(itemsJson, type)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing order items", e)
            }
        }
    }

    private fun setupOrderSummary() {
        orderSummaryAdapter.updateItems(orderItems)
        
        // Update UI
        tvOrderId.text = "Mã đơn hàng: #$orderId"
        tvTotalAmount.text = VietQRService.formatCurrency(totalAmount)
        
        // Setup banking information
        val bankInfo = BankInfo()
        tvBankName.text = "MB Bank"
        tvAccountNumber.text = bankInfo.accountNumber
        tvAccountName.text = bankInfo.accountName
        tvTransferContent.text = VietQRService.generateTransferContent(orderId)
    }

    private fun generateQRCode() {
        if (!VietQRService.validatePaymentData(totalAmount, orderId)) {
            Toast.makeText(this, "Thông tin thanh toán không hợp lệ", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val orderInfo = VietQRService.generateTransferContent(orderId)
        val qrUrl = VietQRService.generateQRUrl(totalAmount, orderInfo)
        
        Log.d(TAG, "Generated QR URL: $qrUrl")
        
        // Load QR image using Glide
        Glide.with(this)
            .load(qrUrl)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.stat_notify_error)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    progressQrLoading.visibility = View.GONE
                    Toast.makeText(this@VietQRActivity, "Không thể tải mã QR. Vui lòng thử lại.", Toast.LENGTH_SHORT).show()
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    progressQrLoading.visibility = View.GONE
                    return false
                }
            })
            .into(imgQrCode)
    }

    private fun setupListeners() {
        btnBackToCart.setOnClickListener {
            showCancelPaymentDialog()
        }
        
        btnPaymentCompleted.setOnClickListener {
            showConfirmPaymentDialog()
        }
    }
    
    /**
     * Hiển thị dialog xác nhận hủy thanh toán
     */
    private fun showCancelPaymentDialog() {
        AlertDialog.Builder(this)
            .setTitle("Hủy thanh toán")
            .setMessage("Bạn có chắc muốn hủy thanh toán?\n\nĐơn hàng sẽ không được tạo và bạn sẽ quay lại giỏ hàng.")
            .setPositiveButton("Hủy thanh toán") { _, _ ->
                handlePaymentCancellation()
            }
            .setNegativeButton("Tiếp tục thanh toán", null)
            .setCancelable(true)
            .show()
    }
    
    /**
     * Hiển thị dialog xác nhận đã thanh toán
     */
    private fun showConfirmPaymentDialog() {
        AlertDialog.Builder(this)
            .setTitle("Xác nhận thanh toán")
            .setMessage("Bạn đã chuyển khoản thành công?\n\nVui lòng đảm bảo đã chuyển đúng số tiền ${VietQRService.formatCurrency(totalAmount)} với nội dung \"${VietQRService.generateTransferContent(orderId)}\"")
            .setPositiveButton("Đã thanh toán") { _, _ ->
                handlePaymentConfirmation()
            }
            .setNegativeButton("Chưa thanh toán", null)
            .setCancelable(true)
            .show()
    }
    
    /**
     * Xử lý khi user hủy thanh toán
     */
    private fun handlePaymentCancellation() {
        Toast.makeText(this, "Đã hủy thanh toán", Toast.LENGTH_SHORT).show()
        // Chỉ cần finish() vì CartActivity vẫn còn trong stack
        finish()
    }
    
    /**
     * Quay lại giỏ hàng (không sử dụng nữa, giữ lại để backup)
     */
    @Suppress("unused")
    private fun navigateBackToCart() {
        // Lấy orderType từ SharedPreferences
        val prefs = getSharedPreferences("UTETeaPrefs", MODE_PRIVATE)
        val orderType = prefs.getString("orderType", "pickup") ?: "pickup"
        
        val intent = Intent(this, CartActivity::class.java).apply {
            putExtra("orderType", orderType)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    /**
     * Xử lý khi user xác nhận đã thanh toán
     */
    private fun handlePaymentConfirmation() {
        if (!orderRequestJson.isNullOrEmpty()) {
            createOrderAfterPayment()
        } else {
            Toast.makeText(this, "Cảm ơn bạn đã thanh toán!", Toast.LENGTH_SHORT).show()
            navigateToOrderHistory()
        }
    }
    
    private fun createOrderAfterPayment() {
        progressQrLoading.visibility = View.VISIBLE
        btnPaymentCompleted.isEnabled = false
        btnBackToCart.isEnabled = false
        
        try {
            val orderRequest = Gson().fromJson(orderRequestJson, CreateOrderRequest::class.java)
            
            Log.d(TAG, "Creating order with request: $orderRequestJson")
            Log.d(TAG, "Payment method: ${orderRequest.paymentMethod}")
            
            // Sử dụng endpoint /api/orders thay vì /api/vnpay/create-order-after-payment
            // vì endpoint orders đã được cấu hình đúng trong SecurityConfig
            createOrderCall = RetrofitClient.getInstance(this).apiService.createOrder(orderRequest)
            createOrderCall?.enqueue(object : Callback<ApiResponse<Order>> {
                override fun onResponse(call: Call<ApiResponse<Order>>, response: Response<ApiResponse<Order>>) {
                    progressQrLoading.visibility = View.GONE
                    btnPaymentCompleted.isEnabled = true
                    btnBackToCart.isEnabled = true
                    
                    // Check if activity is still valid
                    if (isFinishing || isDestroyed) return
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        // Clear cart
                        clearCartOnServer()
                        
                        Toast.makeText(this@VietQRActivity, "Thanh toán và đặt hàng thành công!", Toast.LENGTH_LONG).show()
                        navigateToOrderHistory()
                    } else {
                        val errorMsg = response.body()?.message ?: "Tạo đơn thất bại"
                        Toast.makeText(this@VietQRActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Order>>, t: Throwable) {
                    progressQrLoading.visibility = View.GONE
                    btnPaymentCompleted.isEnabled = true
                    btnBackToCart.isEnabled = true
                    
                    // Check if activity is still valid
                    if (isFinishing || isDestroyed) return
                    
                    if (!call.isCanceled) {
                        Log.e(TAG, "Error creating order after payment", t)
                        val errorMsg = when {
                            t is java.net.UnknownHostException -> "Không có kết nối mạng"
                            t is java.net.SocketTimeoutException -> "Kết nối quá thời gian chờ"
                            else -> "Lỗi tạo đơn: ${t.message}"
                        }
                        Toast.makeText(this@VietQRActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }
            })
        } catch (e: Exception) {
            progressQrLoading.visibility = View.GONE
            btnPaymentCompleted.isEnabled = true
            btnBackToCart.isEnabled = true
            Log.e(TAG, "Error parsing order request", e)
            Toast.makeText(this, "Lỗi xử lý đơn hàng", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun clearCartOnServer() {
        val userId = SessionManager(this).getUserId()
        if (userId == -1) return
        
        clearCartCall = RetrofitClient.getInstance(this).apiService.clearCart(userId.toLong())
        clearCartCall?.enqueue(object : Callback<ApiResponse<Void>> {
            override fun onResponse(call: Call<ApiResponse<Void>>, response: Response<ApiResponse<Void>>) {
                Log.d(TAG, "Cart cleared: ${response.isSuccessful}")
            }
            override fun onFailure(call: Call<ApiResponse<Void>>, t: Throwable) {
                if (!call.isCanceled) {
                    Log.e(TAG, "Error clearing cart", t)
                }
            }
        })
    }

    private fun navigateToOrderHistory() {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("SELECTED_ITEM", R.id.nav_order)
            putExtra("PAYMENT_SUCCESS", true)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }
}
