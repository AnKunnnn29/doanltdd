package com.example.doan.Activities

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
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
import com.google.android.material.button.MaterialButton
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
    
    private var currentOrder: Order? = null
    private val reviewedItemIds = mutableSetOf<Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_detail)

        val orderFromIntent = intent.getSerializableExtra("order") as? Order
        if (orderFromIntent == null) {
            Toast.makeText(this, "Không có thông tin đơn hàng", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupRecyclerView()
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
    }

    private fun setupRecyclerView() {
        orderDetailItemAdapter = OrderDetailItemAdapter(this, mutableListOf())
        orderDetailItemAdapter.setOnReviewClickListener { orderItem ->
            showReviewDialog(orderItem)
        }
        rvOrderItems.layoutManager = LinearLayoutManager(this)
        rvOrderItems.adapter = orderDetailItemAdapter
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
                        Toast.makeText(this@OrderDetailActivity, "Đánh giá thành công!", Toast.LENGTH_SHORT).show()
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
