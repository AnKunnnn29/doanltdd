package com.example.doan.Activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.doan.Adapters.NotificationAdapter
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.NotificationDto
import com.example.doan.Models.NotificationType
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotificationActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var emptyView: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var btnMarkAllRead: ImageButton
    
    private lateinit var adapter: NotificationAdapter
    private val notifications = mutableListOf<NotificationDto>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)
        
        initViews()
        setupRecyclerView()
        setupListeners()
        loadNotifications()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewNotifications)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        emptyView = findViewById(R.id.tvEmpty)
        btnBack = findViewById(R.id.btnBack)
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead)
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(
            notifications,
            onItemClick = { notification -> handleNotificationClick(notification) },
            onDeleteClick = { notification -> deleteNotification(notification) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }
        
        btnMarkAllRead.setOnClickListener { markAllAsRead() }
        
        swipeRefresh.setOnRefreshListener { loadNotifications() }
    }

    private fun loadNotifications() {
        swipeRefresh.isRefreshing = true
        
        RetrofitClient.getInstance(this).apiService.getMyNotifications()
            .enqueue(object : Callback<ApiResponse<List<NotificationDto>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<NotificationDto>>>,
                    response: Response<ApiResponse<List<NotificationDto>>>
                ) {
                    swipeRefresh.isRefreshing = false
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        notifications.clear()
                        response.body()?.data?.let { notifications.addAll(it) }
                        adapter.notifyDataSetChanged()
                        
                        updateEmptyView()
                    } else {
                        Toast.makeText(this@NotificationActivity, 
                            "Không thể tải thông báo", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<NotificationDto>>>, t: Throwable) {
                    swipeRefresh.isRefreshing = false
                    Toast.makeText(this@NotificationActivity, 
                        "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun handleNotificationClick(notification: NotificationDto) {
        // Đánh dấu đã đọc
        if (!notification.isRead) {
            markAsRead(notification)
        }
        
        // Điều hướng dựa trên loại thông báo
        when (notification.type) {
            NotificationType.ORDER_NEW, NotificationType.ORDER_STATUS -> {
                notification.relatedId?.let { orderId ->
                    val intent = Intent(this, OrderDetailActivity::class.java)
                    intent.putExtra("orderId", orderId.toInt())
                    startActivity(intent)
                }
            }
            NotificationType.LIVE_CHAT -> {
                // Mở màn hình Live Chat
                notification.relatedId?.let { conversationId ->
                    val intent = Intent(this, LiveChatActivity::class.java)
                    intent.putExtra("conversationId", conversationId)
                    startActivity(intent)
                }
            }
            NotificationType.GROUP_CHAT -> {
                // Mở màn hình Group Chat
                notification.relatedId?.let { groupOrderId ->
                    val intent = Intent(this, GroupChatActivity::class.java)
                    intent.putExtra("groupOrderId", groupOrderId)
                    startActivity(intent)
                }
            }
            NotificationType.PROMOTION -> {
                // Có thể mở màn hình voucher
            }
            else -> {
                // Không làm gì thêm
            }
        }
    }

    private fun markAsRead(notification: NotificationDto) {
        RetrofitClient.getInstance(this).apiService.markNotificationAsRead(notification.id)
            .enqueue(object : Callback<ApiResponse<NotificationDto>> {
                override fun onResponse(
                    call: Call<ApiResponse<NotificationDto>>,
                    response: Response<ApiResponse<NotificationDto>>
                ) {
                    if (response.isSuccessful) {
                        // Cập nhật UI
                        val index = notifications.indexOfFirst { it.id == notification.id }
                        if (index >= 0) {
                            notifications[index] = notification.copy(isRead = true)
                            adapter.notifyItemChanged(index)
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<NotificationDto>>, t: Throwable) {
                    // Ignore
                }
            })
    }

    private fun markAllAsRead() {
        RetrofitClient.getInstance(this).apiService.markAllNotificationsAsRead()
            .enqueue(object : Callback<ApiResponse<Map<String, Int>>> {
                override fun onResponse(
                    call: Call<ApiResponse<Map<String, Int>>>,
                    response: Response<ApiResponse<Map<String, Int>>>
                ) {
                    if (response.isSuccessful) {
                        // Cập nhật tất cả thông báo thành đã đọc
                        notifications.forEachIndexed { index, notification ->
                            if (!notification.isRead) {
                                notifications[index] = notification.copy(isRead = true)
                            }
                        }
                        adapter.notifyDataSetChanged()
                        Toast.makeText(this@NotificationActivity, 
                            "Đã đánh dấu tất cả là đã đọc", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Map<String, Int>>>, t: Throwable) {
                    Toast.makeText(this@NotificationActivity, 
                        "Lỗi: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun deleteNotification(notification: NotificationDto) {
        RetrofitClient.getInstance(this).apiService.deleteNotification(notification.id)
            .enqueue(object : Callback<ApiResponse<String>> {
                override fun onResponse(
                    call: Call<ApiResponse<String>>,
                    response: Response<ApiResponse<String>>
                ) {
                    if (response.isSuccessful) {
                        val index = notifications.indexOfFirst { it.id == notification.id }
                        if (index >= 0) {
                            notifications.removeAt(index)
                            adapter.notifyItemRemoved(index)
                            updateEmptyView()
                        }
                        Toast.makeText(this@NotificationActivity, 
                            "Đã xóa thông báo", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<String>>, t: Throwable) {
                    Toast.makeText(this@NotificationActivity, 
                        "Lỗi: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateEmptyView() {
        if (notifications.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        loadNotifications()
    }
}
