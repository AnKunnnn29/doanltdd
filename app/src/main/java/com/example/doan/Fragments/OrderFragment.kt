package com.example.doan.Fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Activities.MainActivity
import com.example.doan.Activities.OrderDetailActivity
import com.example.doan.Adapters.OrderAdapter
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.Order
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.DataCache
import com.example.doan.Utils.LoadingDialog
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OrderFragment : Fragment(), OrderAdapter.OnOrderClickListener {

    private lateinit var ordersRecyclerView: RecyclerView
    private lateinit var orderAdapter: OrderAdapter
    private lateinit var tvLoginPrompt: TextView
    private lateinit var toolbar: Toolbar
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var emptyStateContainer: LinearLayout
    private lateinit var btnOrderNow: MaterialButton
    private lateinit var btnLoadMore: MaterialButton
    
    // Stats views
    private lateinit var tvTotalOrders: TextView
    private lateinit var tvPendingOrders: TextView
    private lateinit var tvCompletedOrders: TextView

    private val orderList = mutableListOf<Order>()
    private val displayedOrders = mutableListOf<Order>()
    
    // Pagination
    private val PAGE_SIZE = 10
    private var currentPage = 0
    private var hasMoreData = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_order, container, false)

        loadingDialog = LoadingDialog(requireContext())
        
        initViews(view)
        setupRecyclerView()
        setupListeners()

        return view
    }
    
    private fun initViews(view: View) {
        toolbar = view.findViewById(R.id.toolbar_order)
        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)

        ordersRecyclerView = view.findViewById(R.id.orders_recycler_view)
        tvLoginPrompt = view.findViewById(R.id.tv_login_prompt_order)
        emptyStateContainer = view.findViewById(R.id.empty_state_container)
        btnOrderNow = view.findViewById(R.id.btn_order_now)
        btnLoadMore = view.findViewById(R.id.btn_load_more)
        
        // Stats
        tvTotalOrders = view.findViewById(R.id.tv_total_orders)
        tvPendingOrders = view.findViewById(R.id.tv_pending_orders)
        tvCompletedOrders = view.findViewById(R.id.tv_completed_orders)
    }
    
    private fun setupRecyclerView() {
        ordersRecyclerView.layoutManager = LinearLayoutManager(context)
        orderAdapter = OrderAdapter(requireContext(), displayedOrders)
        orderAdapter.setOnOrderClickListener(this)
        ordersRecyclerView.adapter = orderAdapter
        
        // Add item animation
        ordersRecyclerView.layoutAnimation = AnimationUtils.loadLayoutAnimation(
            context, R.anim.layout_animation_fall_down
        )
    }
    
    private fun setupListeners() {
        btnOrderNow.setOnClickListener {
            // Navigate to Menu
            val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
            bottomNav?.selectedItemId = R.id.nav_order
        }
        
        btnLoadMore.setOnClickListener {
            loadMoreOrders()
        }
    }

    override fun onResume() {
        super.onResume()
        checkLoginAndLoadOrders()
    }

    private fun checkLoginAndLoadOrders() {
        val userId = getLoggedInUserId()
        if (userId != -1) {
            emptyStateContainer.visibility = View.GONE
            ordersRecyclerView.visibility = View.VISIBLE
            loadOrders(userId)
        } else {
            showEmptyState("Vui lòng đăng nhập để xem đơn hàng")
            updateStats(0, 0, 0)
        }
    }
    
    private fun showEmptyState(message: String) {
        emptyStateContainer.visibility = View.VISIBLE
        ordersRecyclerView.visibility = View.GONE
        tvLoginPrompt.text = message
        orderList.clear()
        orderAdapter.notifyDataSetChanged()
    }

    private fun getLoggedInUserId(): Int {
        // FIX: Sử dụng SessionManager thay vì đọc trực tiếp từ SharedPreferences
        // vì SessionManager sử dụng EncryptedSharedPreferences
        val sessionManager = com.example.doan.Utils.SessionManager(requireContext())
        return sessionManager.getUserId()
    }

    private fun loadOrders(userId: Int) {
        Log.d(TAG, "Tải đơn hàng cho User ID: $userId")
        
        // Kiểm tra cache trước
        val cachedOrders = DataCache.orderHistory
        if (!cachedOrders.isNullOrEmpty()) {
            displayOrders(cachedOrders)
            refreshOrders(userId)
            return
        }

        loadingDialog.show("Đang tải đơn hàng...")

        RetrofitClient.getInstance(requireContext()).apiService.getUserOrders(userId)
            .enqueue(object : Callback<ApiResponse<List<Order>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<Order>>>,
                    response: Response<ApiResponse<List<Order>>>
                ) {
                    if (!isAdded) return
                    loadingDialog.dismiss()
                    
                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!
                        if (apiResponse.success && apiResponse.data != null) {
                            DataCache.orderHistory = apiResponse.data
                            displayOrders(apiResponse.data!!)
                        } else {
                            Toast.makeText(context, "Lỗi tải đơn hàng: ${apiResponse.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Lỗi Server: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<Order>>>, t: Throwable) {
                    if (!isAdded) return
                    loadingDialog.dismiss()
                    Log.e(TAG, "Lỗi kết nối API đơn hàng: ${t.message}")
                    Toast.makeText(context, "Không thể kết nối đến máy chủ", Toast.LENGTH_LONG).show()
                }
            })
    }
    
    private fun displayOrders(orders: List<Order>) {
        orderList.clear()
        
        // Sắp xếp thông minh:
        // - Đơn đang xử lý (PENDING, MAKING, SHIPPING, READY): đơn đặt TRƯỚC lên đầu
        // - Đơn đã hoàn thành/hủy (DONE, CANCELED): đơn MỚI NHẤT lên đầu
        val processingStatuses = listOf("PENDING", "MAKING", "SHIPPING", "READY")
        
        val processingOrders = orders
            .filter { it.status in processingStatuses }
            .sortedBy { it.createdAt } // Đơn cũ lên trước
            
        val completedOrders = orders
            .filter { it.status !in processingStatuses }
            .sortedByDescending { it.createdAt } // Đơn mới lên trước
        
        // Đơn đang xử lý hiển thị trước
        orderList.addAll(processingOrders)
        orderList.addAll(completedOrders)
        
        // Reset pagination và clear displayed orders
        currentPage = 0
        displayedOrders.clear()
        orderAdapter.notifyDataSetChanged() // Clear adapter first
        hasMoreData = true
        
        // Load first page
        loadMoreOrders()
        
        if (orderList.isEmpty()) {
            showEmptyState("Bạn chưa có đơn hàng nào")
            btnLoadMore.visibility = View.GONE
        } else {
            emptyStateContainer.visibility = View.GONE
            ordersRecyclerView.visibility = View.VISIBLE
        }
        
        // FIX Medium #14: Update stats với đúng status từ Backend
        // Backend statuses: PENDING, MAKING, SHIPPING, READY, DONE, CANCELED
        val pending = orders.count { it.status in processingStatuses }
        val completed = orders.count { it.status == "DONE" }
        updateStats(orders.size, pending, completed)
    }
    
    private fun loadMoreOrders() {
        val startIndex = currentPage * PAGE_SIZE
        val endIndex = minOf(startIndex + PAGE_SIZE, orderList.size)
        
        if (startIndex >= orderList.size) {
            hasMoreData = false
            btnLoadMore.visibility = View.GONE
            return
        }
        
        // Copy items to avoid subList issues
        val newOrders = orderList.subList(startIndex, endIndex).toList()
        displayedOrders.addAll(newOrders)
        orderAdapter.notifyDataSetChanged()
        
        currentPage++
        
        // Check if there's more data
        hasMoreData = endIndex < orderList.size
        btnLoadMore.visibility = if (hasMoreData) View.VISIBLE else View.GONE
        
        // Play animation only on first load
        if (currentPage == 1) {
            ordersRecyclerView.scheduleLayoutAnimation()
        }
    }
    
    private fun updateStats(total: Int, pending: Int, completed: Int) {
        tvTotalOrders.text = total.toString()
        tvPendingOrders.text = pending.toString()
        tvCompletedOrders.text = completed.toString()
        
        // Animate stats
        animateTextView(tvTotalOrders)
        animateTextView(tvPendingOrders)
        animateTextView(tvCompletedOrders)
    }
    
    private fun animateTextView(textView: TextView) {
        textView.alpha = 0f
        textView.animate()
            .alpha(1f)
            .setDuration(500)
            .start()
    }
    
    private fun refreshOrders(userId: Int) {
        RetrofitClient.getInstance(requireContext()).apiService.getUserOrders(userId)
            .enqueue(object : Callback<ApiResponse<List<Order>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<Order>>>,
                    response: Response<ApiResponse<List<Order>>>
                ) {
                    if (!isAdded) return
                    
                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!
                        if (apiResponse.success && apiResponse.data != null) {
                            DataCache.orderHistory = apiResponse.data
                            displayOrders(apiResponse.data!!)
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<Order>>>, t: Throwable) {
                    Log.e(TAG, "Failed to refresh orders: ${t.message}")
                }
            })
    }

    override fun onOrderClick(order: Order) {
        val intent = Intent(requireContext(), OrderDetailActivity::class.java)
        intent.putExtra("order", order)
        startActivity(intent)
    }

    companion object {
        private const val TAG = "OrderFragment"
    }
}
