package com.example.doan.Activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
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

    private lateinit var tvOrderId: TextView
    private lateinit var tvOrderDate: TextView
    private lateinit var chipStatus: Chip
    private lateinit var tvTotal: TextView
    private lateinit var tvPaymentMethod: TextView
    private lateinit var rvOrderItems: RecyclerView
    private lateinit var orderDetailItemAdapter: OrderDetailItemAdapter
    private lateinit var btnReorder: MaterialButton
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var sessionManager: SessionManager
    
    // Manager/Admin views
    private lateinit var cardCustomerInfo: MaterialCardView
    private lateinit var cardStatusControl: MaterialCardView
    private lateinit var tvCustomerName: TextView
    private lateinit var tvCustomerAddress: TextView
    private lateinit var btnStatusMaking: MaterialButton
    private lateinit var btnStatusShipping: MaterialButton
    private lateinit var btnStatusDone: MaterialButton
    private lateinit var btnStatusCancel: MaterialButton
    
    private var currentOrder: Order? = null
    private val reviewedItemIds = mutableSetOf<Long>()
    private var isManagerOrAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_detail)

        // FIX: Sử dụng getParcelableExtra thay vì getSerializableExtra vì Order đã đổi sang Parcelable
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
        tvOrderId = findViewById(R.id.tv_order_detail_id)
        tvOrderDate = findViewById(R.id.tv_order_detail_date)
        chipStatus = findViewById(R.id.chip_order_detail_status)
        tvTotal = findViewById(R.id.tv_order_detail_total)
        tvPaymentMethod = findViewById(R.id.tv_payment_method)
        rvOrderItems = findViewById(R.id.rv_order_detail_items)
        btnReorder = findViewById(R.id.btn_reorder)
        
        // Manager/Admin views
        cardCustomerInfo = findViewById(R.id.card_customer_info)
        cardStatusControl = findViewById(R.id.card_status_control)
        tvCustomerName = findViewById(R.id.tv_customer_name)
        tvCustomerAddress = findViewById(R.id.tv_customer_address)
        btnStatusMaking = findViewById(R.id.btn_status_making)
        btnStatusShipping = findViewById(R.id.btn_status_shipping)
        btnStatusDone = findViewById(R.id.btn_status_done)
        btnStatusCancel = findViewById(R.id.btn_status_cancel)
        
        // Ẩn nút đặt lại đơn hàng nếu là Manager/Admin, hiện các control quản lý
        if (isManagerOrAdmin) {
            btnReorder.visibility = View.GONE
            cardCustomerInfo.visibility = View.VISIBLE
            cardStatusControl.visibility = View.VISIBLE
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
    }
    
    private fun setupManagerControls() {
        if (!isManagerOrAdmin) return
        
        btnStatusMaking.setOnClickListener { updateOrderStatus("MAKING") }
        btnStatusShipping.setOnClickListener { updateOrderStatus("SHIPPING") }
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
                            
                            // Gửi result về để refresh list
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
        // Flow hợp lý: PENDING → MAKING → SHIPPING → DONE
        // Có thể hủy bất kỳ lúc nào trước khi DONE
        
        val isDone = currentStatus == "DONE"
        val isCanceled = currentStatus == "CANCELED"
        val isFinished = isDone || isCanceled
        
        when (currentStatus) {
            "PENDING" -> {
                // Từ PENDING: chỉ có thể chuyển sang MAKING hoặc hủy
                btnStatusMaking.visibility = View.VISIBLE
                btnStatusShipping.visibility = View.GONE
                btnStatusDone.visibility = View.GONE
                btnStatusCancel.visibility = View.VISIBLE
                
                btnStatusMaking.isEnabled = true
                btnStatusCancel.isEnabled = true
            }
            "MAKING" -> {
                // Từ MAKING: chỉ có thể chuyển sang SHIPPING hoặc hủy
                btnStatusMaking.visibility = View.GONE
                btnStatusShipping.visibility = View.VISIBLE
                btnStatusDone.visibility = View.GONE
                btnStatusCancel.visibility = View.VISIBLE
                
                btnStatusShipping.isEnabled = true
                btnStatusCancel.isEnabled = true
            }
            "SHIPPING" -> {
                // Từ SHIPPING: chỉ có thể chuyển sang DONE hoặc hủy
                btnStatusMaking.visibility = View.GONE
                btnStatusShipping.visibility = View.GONE
                btnStatusDone.visibility = View.VISIBLE
                btnStatusCancel.visibility = View.VISIBLE
                
                btnStatusDone.isEnabled = true
                btnStatusCancel.isEnabled = true
            }
            "DONE", "CANCELED" -> {
                // Đã hoàn thành hoặc đã hủy: ẩn tất cả nút
                btnStatusMaking.visibility = View.GONE
                btnStatusShipping.visibility = View.GONE
                btnStatusDone.visibility = View.GONE
                btnStatusCancel.visibility = View.GONE
                
                // Ẩn luôn card điều khiển
                cardStatusControl.visibility = View.GONE
            }
            else -> {
                // Trạng thái không xác định: hiện tất cả
                btnStatusMaking.visibility = View.VISIBLE
                btnStatusShipping.visibility = View.VISIBLE
                btnStatusDone.visibility = View.VISIBLE
                btnStatusCancel.visibility = View.VISIBLE
            }
        }
        
        // Đổi alpha để hiển thị trạng thái
        btnStatusMaking.alpha = if (btnStatusMaking.isEnabled) 1f else 0.5f
        btnStatusShipping.alpha = if (btnStatusShipping.isEnabled) 1f else 0.5f
        btnStatusDone.alpha = if (btnStatusDone.isEnabled) 1f else 0.5f
        btnStatusCancel.alpha = if (btnStatusCancel.isEnabled) 1f else 0.5f
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
            // Hiển thị dialog thông báo có món không còn bán
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
                
                // Hiển thị gợi ý thay thế
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
        
        // Kiểm tra các món có topping không còn bán
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
        // Manager/Admin không cần hiển thị nút đánh giá
        if (isManagerOrAdmin) {
            orderDetailItemAdapter.setShowReviewButton(false)
            return
        }
        
        // Chỉ hiển thị nút đánh giá khi đơn hàng đã hoàn thành
        if (order.status != "DONE") {
            orderDetailItemAdapter.setShowReviewButton(false)
            return
        }
        
        orderDetailItemAdapter.setShowReviewButton(true)
        
        // Kiểm tra từng item đã được đánh giá chưa
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
                        // Hiển thị thông báo đánh giá thành công + cộng điểm
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
        tvOrderId.text = "Đơn hàng #${order.getDisplayOrderNumber()}"
        tvOrderDate.text = formatDateTime(order.createdAt)
        
        // Status Chip
        val statusInfo = getStatusInfo(order.status)
        chipStatus.text = statusInfo.first
        chipStatus.setChipBackgroundColorResource(statusInfo.second)
        chipStatus.setTextColor(ContextCompat.getColor(this, statusInfo.third))

        // Payment
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        tvTotal.text = currencyFormat.format(order.totalAmount)
        tvPaymentMethod.text = "Thanh toán bằng ${order.paymentMethod}"

        // Items
        order.items?.let {
            orderDetailItemAdapter.updateItems(it)
        }
        
        // Manager/Admin: Hiển thị thông tin khách hàng và cập nhật nút trạng thái
        if (isManagerOrAdmin) {
            tvCustomerName.text = "Khách hàng: ${order.userName ?: "Không rõ"}"
            tvCustomerAddress.text = "Địa chỉ: ${order.address ?: "Nhận tại cửa hàng"}"
            updateStatusButtons(order.status)
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
            "DONE" -> Triple("Hoàn thành", R.color.status_done_bg, R.color.status_done)
            "CANCELED" -> Triple("Đã hủy", R.color.status_canceled_bg, R.color.status_canceled)
            else -> Triple(status ?: "Không rõ", R.color.surface_variant, R.color.text_secondary)
        }
    }
}
