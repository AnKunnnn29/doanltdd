package com.example.doan.Activities

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Adapters.OrderDetailItemAdapter
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.Order
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.google.android.material.chip.Chip
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
                        updateUi(order)
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
