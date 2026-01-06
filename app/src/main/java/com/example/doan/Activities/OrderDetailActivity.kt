package com.example.doan.Activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Adapters.OrderDetailItemAdapter
import com.example.doan.Models.*
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.LoadingDialog
import com.example.doan.Utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class OrderDetailActivity : AppCompatActivity() {

    // Header views
    private lateinit var tvOrderId: TextView
    private lateinit var tvOrderDate: TextView
    private lateinit var chipStatus: Chip
    
    // Customer info views (Manager/Admin)
    private lateinit var cardCustomerInfo: MaterialCardView
    private lateinit var tvCustomerName: TextView
    private lateinit var tvCustomerPhone: TextView
    private lateinit var tvCustomerEmail: TextView
    
    // Order info views
    private lateinit var tvStoreName: TextView
    private lateinit var tvOrderType: TextView
    private lateinit var tvCustomerAddress: TextView
    private lateinit var tvPaymentMethod: TextView
    
    // Items
    private lateinit var rvOrderItems: RecyclerView
    private lateinit var orderDetailItemAdapter: OrderDetailItemAdapter
    
    // Price summary views
    private lateinit var tvSubtotal: TextView
    private lateinit var llShippingFee: LinearLayout
    private lateinit var tvShippingFee: TextView
    private lateinit var llDiscount: LinearLayout
    private lateinit var tvDiscount: TextView
    private lateinit var tvTotal: TextView
    
    // Manager/Admin status control
    private lateinit var cardStatusControl: MaterialCardView
    private lateinit var btnStatusMaking: MaterialButton
    private lateinit var btnStatusShipping: MaterialButton
    private lateinit var btnStatusDone: MaterialButton
    private lateinit var btnStatusCancel: MaterialButton
    
    // User actions
    private lateinit var llUserActions: LinearLayout
    private lateinit var btnReorder: MaterialButton
    private lateinit var btnCancelOrder: MaterialButton
    
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var sessionManager: SessionManager
    
    private var currentOrder: Order? = null
    private val reviewedItemIds = mutableSetOf<Long>()
    private var isManagerOrAdmin = false
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_detail)

        val orderFromIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("order", Order::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("order")
        }
        
        if (orderFromIntent == null) {
            Toast.makeText(this, "Không có thông tin đơn hàng", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadingDialog = LoadingDialog(this)
        sessionManager = SessionManager(this)
        isManagerOrAdmin = sessionManager.isManager() || sessionManager.isAdmin()
        
        initViews()
        setupRecyclerView()
        setupReorderButton()
        setupManagerControls()
        loadOrderDetail(orderFromIntent.id)
    }

    private fun initViews() {
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_order_detail).setNavigationOnClickListener { finish() }
        
        // Header
        tvOrderId = findViewById(R.id.tv_order_detail_id)
        tvOrderDate = findViewById(R.id.tv_order_detail_date)
        chipStatus = findViewById(R.id.chip_order_detail_status)
        
        // Customer info (Manager/Admin)
        cardCustomerInfo = findViewById(R.id.card_customer_info)
        tvCustomerName = findViewById(R.id.tv_customer_name)
        tvCustomerPhone = findViewById(R.id.tv_customer_phone)
        tvCustomerEmail = findViewById(R.id.tv_customer_email)
        
        // Order info
        tvStoreName = findViewById(R.id.tv_store_name)
        tvOrderType = findViewById(R.id.tv_order_type)
        tvCustomerAddress = findViewById(R.id.tv_customer_address)
        tvPaymentMethod = findViewById(R.id.tv_payment_method)
        
        // Items
        rvOrderItems = findViewById(R.id.rv_order_detail_items)
        
        // Price summary
        tvSubtotal = findViewById(R.id.tv_subtotal)
        llShippingFee = findViewById(R.id.ll_shipping_fee)
        tvShippingFee = findViewById(R.id.tv_shipping_fee)
        llDiscount = findViewById(R.id.ll_discount)
        tvDiscount = findViewById(R.id.tv_discount)
        tvTotal = findViewById(R.id.tv_order_detail_total)
        
        // Manager/Admin controls
        cardStatusControl = findViewById(R.id.card_status_control)
        btnStatusMaking = findViewById(R.id.btn_status_making)
        btnStatusShipping = findViewById(R.id.btn_status_shipping)
        btnStatusDone = findViewById(R.id.btn_status_done)
        btnStatusCancel = findViewById(R.id.btn_status_cancel)
        
        // User actions
        llUserActions = findViewById(R.id.ll_user_actions)
        btnReorder = findViewById(R.id.btn_reorder)
        btnCancelOrder = findViewById(R.id.btn_cancel_order)
        
        // Show/hide based on role
        if (isManagerOrAdmin) {
            llUserActions.visibility = View.GONE
            cardCustomerInfo.visibility = View.VISIBLE
            cardStatusControl.visibility = View.VISIBLE
        } else {
            llUserActions.visibility = View.VISIBLE
            cardCustomerInfo.visibility = View.GONE
            cardStatusControl.visibility = View.GONE
        }
    }

    private fun setupRecyclerView() {
        orderDetailItemAdapter = OrderDetailItemAdapter(this, mutableListOf())
        orderDetailItemAdapter.setOnReviewClickListener { orderItem ->
            showReviewDialog(orderItem)
        }
        rvOrderItems.layoutManager = LinearLayoutManager(this)
        rvOrderItems.adapter = orderDetailItemAdapter
    }
    
    private fun setupReorderButton() {
        btnReorder.setOnClickListener {
            currentOrder?.let { order ->
                reorderFromHistory(order.id.toLong())
            }
        }
        
        btnCancelOrder.setOnClickListener {
            currentOrder?.let { order ->
                showUserCancelOrderDialog(order)
            }
        }
    }
    
    private fun showUserCancelOrderDialog(order: Order) {
        AlertDialog.Builder(this)
            .setTitle("Xác nhận hủy đơn")
            .setMessage("Bạn có chắc muốn hủy đơn hàng #${order.getDisplayOrderNumber()}?\n\nĐơn hàng đang ở trạng thái chờ xử lý và có thể hủy được.")
            .setPositiveButton("Hủy đơn") { _, _ -> 
                cancelOrderByUser(order.id)
            }
            .setNegativeButton("Không", null)
            .show()
    }
    
    private fun cancelOrderByUser(orderId: Int) {
        loadingDialog.show("Đang hủy đơn hàng...")
        
        RetrofitClient.getInstance(this).apiService.cancelOrder(orderId)
            .enqueue(object : Callback<ApiResponse<String>> {
                override fun onResponse(call: Call<ApiResponse<String>>, response: Response<ApiResponse<String>>) {
                    loadingDialog.dismiss()
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@OrderDetailActivity, "✅ Đã hủy đơn hàng thành công", Toast.LENGTH_SHORT).show()
                        
                        currentOrder?.let { order ->
                            val updatedOrder = order.copy(status = "CANCELED")
                            currentOrder = updatedOrder
                            updateUi(updatedOrder)
                            updateUserCancelButton(updatedOrder.status)
                        }
                        
                        com.example.doan.Utils.DataCache.orderHistory = null
                        setResult(RESULT_OK)
                    } else {
                        val errorMsg = if (response.body() != null) {
                            response.body()?.message ?: "Không thể hủy đơn hàng"
                        } else {
                            try {
                                val errorBody = response.errorBody()?.string()
                                val errorResponse = Gson().fromJson(errorBody, ApiResponse::class.java)
                                errorResponse?.message ?: "Không thể hủy đơn hàng"
                            } catch (e: Exception) {
                                "Không thể hủy đơn hàng"
                            }
                        }
                        Toast.makeText(this@OrderDetailActivity, errorMsg, Toast.LENGTH_SHORT).show()
                        currentOrder?.let { loadOrderDetail(it.id) }
                    }
                }
                
                override fun onFailure(call: Call<ApiResponse<String>>, t: Throwable) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@OrderDetailActivity, "Lỗi mạng: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
    
    private fun updateUserCancelButton(status: String?) {
        if (isManagerOrAdmin) {
            btnCancelOrder.visibility = View.GONE
            return
        }
        btnCancelOrder.visibility = if (status == "PENDING") View.VISIBLE else View.GONE
    }
    
    private fun setupManagerControls() {
        if (!isManagerOrAdmin) return
        
        btnStatusMaking.setOnClickListener { updateOrderStatus("MAKING") }
        btnStatusShipping.setOnClickListener { 
            val orderType = currentOrder?.type
            if (orderType == "PICKUP") {
                updateOrderStatus("READY")
            } else {
                updateOrderStatus("SHIPPING")
            }
        }
        btnStatusDone.setOnClickListener { updateOrderStatus("DONE") }
        btnStatusCancel.setOnClickListener { 
            AlertDialog.Builder(this)
                .setTitle("Xác nhận hủy đơn")
                .setMessage("Bạn có chắc muốn hủy đơn hàng này?")
                .setPositiveButton("Hủy đơn") { _, _ -> updateOrderStatus("CANCELED") }
                .setNegativeButton("Không", null)
                .show()
        }
    }
    
    private fun updateOrderStatus(newStatus: String) {
        val orderId = currentOrder?.id ?: return
        
        loadingDialog.show("Đang cập nhật...")
        
        RetrofitClient.getInstance(this).apiService.updateOrderStatus(orderId, newStatus)
            .enqueue(object : Callback<ApiResponse<Order>> {
                override fun onResponse(call: Call<ApiResponse<Order>>, response: Response<ApiResponse<Order>>) {
                    loadingDialog.dismiss()
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        val updatedOrder = response.body()?.data
                        if (updatedOrder != null) {
                            currentOrder = updatedOrder
                            updateUi(updatedOrder)
                            updateStatusButtons(updatedOrder.status)
                            Toast.makeText(this@OrderDetailActivity, "✅ Đã cập nhật trạng thái", Toast.LENGTH_SHORT).show()
                            setResult(RESULT_OK)
                        }
                    } else {
                        val errorMsg = response.body()?.message ?: "Không thể cập nhật trạng thái"
                        Toast.makeText(this@OrderDetailActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }
                
                override fun onFailure(call: Call<ApiResponse<Order>>, t: Throwable) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@OrderDetailActivity, "Lỗi mạng: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
    
    private fun updateStatusButtons(currentStatus: String?) {
        val orderType = currentOrder?.type
        
        when (currentStatus) {
            "PENDING" -> {
                btnStatusMaking.visibility = View.VISIBLE
                btnStatusShipping.visibility = View.GONE
                btnStatusDone.visibility = View.GONE
                btnStatusCancel.visibility = View.VISIBLE
                
                btnStatusMaking.isEnabled = true
                btnStatusCancel.isEnabled = true
                btnStatusCancel.alpha = 1f
            }
            "MAKING" -> {
                btnStatusMaking.visibility = View.GONE
                
                if (orderType == "DELIVERY") {
                    btnStatusShipping.visibility = View.VISIBLE
                    btnStatusShipping.text = "Giao hàng"
                    btnStatusDone.visibility = View.GONE
                } else {
                    btnStatusShipping.visibility = View.VISIBLE
                    btnStatusShipping.text = "Sẵn sàng"
                    btnStatusDone.visibility = View.GONE
                }
                
                btnStatusCancel.visibility = View.VISIBLE
                btnStatusCancel.isEnabled = false
                btnStatusCancel.alpha = 0.4f
                
                btnStatusShipping.isEnabled = true
            }
            "SHIPPING" -> {
                btnStatusMaking.visibility = View.GONE
                btnStatusShipping.visibility = View.GONE
                btnStatusDone.visibility = View.VISIBLE
                
                btnStatusCancel.visibility = View.VISIBLE
                btnStatusCancel.isEnabled = false
                btnStatusCancel.alpha = 0.4f
                
                btnStatusDone.isEnabled = true
            }
            "READY" -> {
                btnStatusMaking.visibility = View.GONE
                btnStatusShipping.visibility = View.GONE
                btnStatusDone.visibility = View.VISIBLE
                
                btnStatusCancel.visibility = View.VISIBLE
                btnStatusCancel.isEnabled = false
                btnStatusCancel.alpha = 0.4f
                
                btnStatusDone.isEnabled = true
            }
            "DONE", "CANCELED" -> {
                btnStatusMaking.visibility = View.GONE
                btnStatusShipping.visibility = View.GONE
                btnStatusDone.visibility = View.GONE
                btnStatusCancel.visibility = View.GONE
                cardStatusControl.visibility = View.GONE
            }
            else -> {
                btnStatusMaking.visibility = View.VISIBLE
                btnStatusShipping.visibility = View.VISIBLE
                btnStatusDone.visibility = View.VISIBLE
                btnStatusCancel.visibility = View.VISIBLE
            }
        }
        
        btnStatusMaking.alpha = if (btnStatusMaking.isEnabled) 1f else 0.4f
        btnStatusShipping.alpha = if (btnStatusShipping.isEnabled) 1f else 0.4f
        btnStatusDone.alpha = if (btnStatusDone.isEnabled) 1f else 0.4f
    }

    
    private fun reorderFromHistory(orderId: Long) {
        loadingDialog.show("Đang thêm vào giỏ hàng...")
        
        val request = ReorderRequest(orderId)
        RetrofitClient.getInstance(this).apiService.reorderFromHistory(request)
            .enqueue(object : Callback<ApiResponse<ReorderResponse>> {
                override fun onResponse(
                    call: Call<ApiResponse<ReorderResponse>>,
                    response: Response<ApiResponse<ReorderResponse>>
                ) {
                    loadingDialog.dismiss()
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        val reorderResponse = response.body()?.data
                        if (reorderResponse != null) {
                            handleReorderResponse(reorderResponse)
                        } else {
                            Toast.makeText(this@OrderDetailActivity, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show()
                            navigateToCart()
                        }
                    } else {
                        val errorMsg = response.body()?.message ?: "Không thể đặt lại đơn hàng"
                        Toast.makeText(this@OrderDetailActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }
                
                override fun onFailure(call: Call<ApiResponse<ReorderResponse>>, t: Throwable) {
                    loadingDialog.dismiss()
                    Log.e("OrderDetailActivity", "Reorder failed", t)
                    Toast.makeText(this@OrderDetailActivity, "Lỗi mạng: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
    
    private fun handleReorderResponse(response: ReorderResponse) {
        if (response.hasUnavailableItems) {
            showUnavailableItemsDialog(response)
        } else {
            Toast.makeText(this, "✅ ${response.message}", Toast.LENGTH_SHORT).show()
            navigateToCart()
        }
    }
    
    private fun showUnavailableItemsDialog(response: ReorderResponse) {
        val unavailableItems = response.itemStatuses?.filter { !it.addedToCart } ?: emptyList()
        val addedItems = response.itemStatuses?.filter { it.addedToCart } ?: emptyList()
        
        val messageBuilder = StringBuilder()
        messageBuilder.append("✅ Đã thêm ${addedItems.size} món vào giỏ hàng\n\n")
        
        if (unavailableItems.isNotEmpty()) {
            messageBuilder.append("⚠️ Các món không còn bán:\n")
            unavailableItems.forEach { item ->
                messageBuilder.append("• ${item.drinkName}")
                item.reason?.let { messageBuilder.append("\n  → $it") }
                
                item.suggestions?.takeIf { it.isNotEmpty() }?.let { suggestions ->
                    messageBuilder.append("\n  💡 Gợi ý: ")
                    suggestions.take(2).forEachIndexed { index, suggestion ->
                        if (index > 0) messageBuilder.append(", ")
                        messageBuilder.append(suggestion.drinkName)
                    }
                }
                messageBuilder.append("\n\n")
            }
        }
        
        val itemsWithUnavailableToppings = addedItems.filter { item ->
            item.toppingStatuses?.any { !it.available } == true
        }
        
        if (itemsWithUnavailableToppings.isNotEmpty()) {
            messageBuilder.append("ℹ️ Một số topping không còn bán:\n")
            itemsWithUnavailableToppings.forEach { item ->
                val unavailableToppings = item.toppingStatuses?.filter { !it.available }
                unavailableToppings?.forEach { topping ->
                    messageBuilder.append("• ${item.drinkName}: ${topping.toppingName}\n")
                }
            }
        }
        
        AlertDialog.Builder(this)
            .setTitle("Kết quả đặt lại")
            .setMessage(messageBuilder.toString().trim())
            .setPositiveButton("Xem giỏ hàng") { _, _ ->
                navigateToCart()
            }
            .setNegativeButton("Đóng", null)
            .show()
    }
    
    private fun navigateToCart() {
        val intent = Intent(this, CartActivity::class.java)
        startActivity(intent)
    }

    private fun loadOrderDetail(orderId: Int) {
        RetrofitClient.getInstance(this).apiService.getOrderById(orderId).enqueue(object : Callback<ApiResponse<Order>> {
            override fun onResponse(call: Call<ApiResponse<Order>>, response: Response<ApiResponse<Order>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val order = response.body()?.data
                    if (order != null) {
                        Log.d("OrderDetailActivity", "Order data received: ${Gson().toJson(order)}")
                        currentOrder = order
                        updateUi(order)
                        checkReviewedItems(order)
                    } else {
                        Log.e("OrderDetailActivity", "Order data is null")
                        Toast.makeText(this@OrderDetailActivity, "Không tìm thấy chi tiết đơn hàng", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("OrderDetailActivity", "Failed to load order details. Code: ${response.code()}, Message: ${response.message()}")
                    Toast.makeText(this@OrderDetailActivity, "Lỗi: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<Order>>, t: Throwable) {
                Log.e("OrderDetailActivity", "API call failed", t)
                Toast.makeText(this@OrderDetailActivity, "Lỗi mạng: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun checkReviewedItems(order: Order) {
        if (isManagerOrAdmin) {
            orderDetailItemAdapter.setShowReviewButton(false)
            return
        }
        
        if (order.status != "DONE") {
            orderDetailItemAdapter.setShowReviewButton(false)
            return
        }
        
        orderDetailItemAdapter.setShowReviewButton(true)
        
        order.items?.forEach { item ->
            val itemId = item.id.toLong()
            RetrofitClient.getInstance(this).apiService.canReviewOrderItem(itemId)
                .enqueue(object : Callback<ApiResponse<Boolean>> {
                    override fun onResponse(call: Call<ApiResponse<Boolean>>, response: Response<ApiResponse<Boolean>>) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            val canReview = response.body()?.data ?: false
                            if (!canReview) {
                                reviewedItemIds.add(itemId)
                                orderDetailItemAdapter.setReviewedItemIds(reviewedItemIds)
                            }
                        }
                    }
                    override fun onFailure(call: Call<ApiResponse<Boolean>>, t: Throwable) {
                        Log.e("OrderDetailActivity", "Failed to check review status", t)
                    }
                })
        }
    }
    
    private fun showReviewDialog(orderItem: OrderItem) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_review, null)
        val tvDrinkName = dialogView.findViewById<TextView>(R.id.tvReviewDrinkName)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBarInput)
        val tvRatingHint = dialogView.findViewById<TextView>(R.id.tvRatingHint)
        val etComment = dialogView.findViewById<TextInputEditText>(R.id.etReviewComment)
        val cbAnonymous = dialogView.findViewById<CheckBox>(R.id.cbAnonymous)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancelReview)
        val btnSubmit = dialogView.findViewById<MaterialButton>(R.id.btnSubmitReview)
        
        tvDrinkName.text = orderItem.drinkName
        
        ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            tvRatingHint.text = when (rating.toInt()) {
                1 -> "Rất tệ"
                2 -> "Tệ"
                3 -> "Bình thường"
                4 -> "Tốt"
                5 -> "Tuyệt vời"
                else -> ""
            }
        }
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        btnCancel.setOnClickListener { dialog.dismiss() }
        
        btnSubmit.setOnClickListener {
            val rating = ratingBar.rating.toInt()
            if (rating == 0) {
                Toast.makeText(this, "Vui lòng chọn số sao đánh giá", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val request = CreateReviewRequest(
                orderId = currentOrder?.id?.toLong() ?: 0,
                orderItemId = orderItem.id.toLong(),
                rating = rating,
                comment = etComment.text?.toString()?.trim(),
                isAnonymous = cbAnonymous.isChecked
            )
            
            submitReview(request, dialog, orderItem.id.toLong())
        }
        
        dialog.show()
    }
    
    private fun submitReview(request: CreateReviewRequest, dialog: AlertDialog, orderItemId: Long) {
        RetrofitClient.getInstance(this).apiService.createReview(request)
            .enqueue(object : Callback<ApiResponse<Review>> {
                override fun onResponse(call: Call<ApiResponse<Review>>, response: Response<ApiResponse<Review>>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@OrderDetailActivity, "🎉 Đánh giá thành công! +1 điểm vòng quay", Toast.LENGTH_LONG).show()
                        reviewedItemIds.add(orderItemId)
                        orderDetailItemAdapter.setReviewedItemIds(reviewedItemIds)
                        dialog.dismiss()
                    } else {
                        val errorMsg = response.body()?.message ?: "Không thể gửi đánh giá"
                        Toast.makeText(this@OrderDetailActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }
                
                override fun onFailure(call: Call<ApiResponse<Review>>, t: Throwable) {
                    Toast.makeText(this@OrderDetailActivity, "Lỗi mạng: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateUi(order: Order) {
        // Header
        tvOrderId.text = "Đơn hàng ${order.getDisplayOrderNumber()}"
        tvOrderDate.text = formatDateTime(order.createdAt)
        
        // Status Chip
        val statusInfo = getStatusInfo(order.status)
        chipStatus.text = statusInfo.first
        chipStatus.setChipBackgroundColorResource(statusInfo.second)
        chipStatus.setTextColor(ContextCompat.getColor(this, statusInfo.third))
        
        // Customer info (Manager/Admin only)
        if (isManagerOrAdmin) {
            tvCustomerName.text = "Họ tên: ${order.userName ?: "Không rõ"}"
            // Phone và email có thể thêm sau nếu backend trả về
        }
        
        // Order info
        tvStoreName.text = "🏪 Cửa hàng: ${order.storeName ?: "Chưa xác định"}"
        tvOrderType.text = when (order.type) {
            "DELIVERY" -> "🚚 Loại đơn: Giao hàng"
            "PICKUP" -> "🏪 Loại đơn: Lấy tại quầy"
            else -> "Loại đơn: ${order.type ?: "Không rõ"}"
        }
        
        if (order.type == "DELIVERY" && !order.address.isNullOrEmpty()) {
            tvCustomerAddress.visibility = View.VISIBLE
            tvCustomerAddress.text = "📍 Địa chỉ: ${order.address}"
        } else if (order.type == "PICKUP") {
            tvCustomerAddress.visibility = View.VISIBLE
            tvCustomerAddress.text = "📍 Nhận tại: ${order.storeName ?: "Cửa hàng"}"
        } else {
            tvCustomerAddress.visibility = View.GONE
        }
        
        tvPaymentMethod.text = "💳 Thanh toán: ${getPaymentMethodDisplay(order.paymentMethod)}"
        
        // Items
        order.items?.let {
            orderDetailItemAdapter.updateItems(it)
        }
        
        // Price summary
        tvSubtotal.text = currencyFormat.format(order.totalPrice)
        
        // Shipping fee
        if (order.shippingFee > 0) {
            llShippingFee.visibility = View.VISIBLE
            tvShippingFee.text = currencyFormat.format(order.shippingFee)
        } else {
            llShippingFee.visibility = View.GONE
        }
        
        // Discount
        if (order.discount > 0) {
            llDiscount.visibility = View.VISIBLE
            tvDiscount.text = "-${currencyFormat.format(order.discount)}"
        } else {
            llDiscount.visibility = View.GONE
        }
        
        // Final price
        tvTotal.text = currencyFormat.format(order.finalPrice)
        
        // User: Update cancel button
        if (!isManagerOrAdmin) {
            updateUserCancelButton(order.status)
        }
        
        // Manager/Admin: Update status buttons
        if (isManagerOrAdmin) {
            updateStatusButtons(order.status)
        }
    }
    
    private fun getPaymentMethodDisplay(method: String?): String {
        return when (method) {
            "COD" -> "Tiền mặt khi nhận hàng"
            "VNPAY" -> "VNPay"
            "MOMO" -> "MoMo"
            "BANK_TRANSFER" -> "Chuyển khoản"
            else -> method ?: "Không rõ"
        }
    }
    
    private fun formatDateTime(dateTimeStr: String?): String {
        if (dateTimeStr.isNullOrEmpty()) return "N/A"
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val outputFormat = SimpleDateFormat("HH:mm, dd/MM/yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateTimeStr)
            if (date != null) outputFormat.format(date) else "N/A"
        } catch (e: Exception) {
            dateTimeStr
        }
    }

    private fun getStatusInfo(status: String?): Triple<String, Int, Int> {
        return when (status) {
            "PENDING" -> Triple("Chờ xử lý", R.color.status_pending_bg, R.color.status_pending)
            "MAKING" -> Triple("Đang làm", R.color.status_making_bg, R.color.status_making)
            "SHIPPING" -> Triple("Đang giao", R.color.status_shipping_bg, R.color.status_shipping)
            "READY" -> Triple("Sẵn sàng", R.color.status_done_bg, R.color.status_done)
            "DONE" -> Triple("Hoàn thành", R.color.status_done_bg, R.color.status_done)
            "CANCELED" -> Triple("Đã hủy", R.color.status_canceled_bg, R.color.status_canceled)
            else -> Triple(status ?: "Không rõ", R.color.surface_variant, R.color.text_secondary)
        }
    }
}
