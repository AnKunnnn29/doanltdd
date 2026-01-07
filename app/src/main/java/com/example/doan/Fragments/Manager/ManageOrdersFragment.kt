package com.example.doan.Fragments.Manager

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.doan.Activities.OrderDetailActivity
import com.example.doan.Adapters.ManagerOrderAdapter
import com.example.doan.Adapters.StoreFilterAdapter
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.Order
import com.example.doan.Models.PageResponse
import com.example.doan.Models.Store
import com.example.doan.Network.OrderWebSocketManager
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.SessionManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ManageOrdersFragment : Fragment(), ManagerOrderAdapter.OnOrderActionListener {

    private lateinit var rvOrders: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: View
    private lateinit var tabLayout: TabLayout
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var headerLayout: View
    private lateinit var cardStoreFilter: MaterialCardView
    private lateinit var tvSelectedStore: TextView
    private lateinit var tvPendingCount: TextView
    private lateinit var tvMakingCount: TextView
    private lateinit var tvDoneCount: TextView
    private lateinit var btnRefresh: MaterialButton
    private var tvConnectionStatus: TextView? = null
    private var tvHeaderText: TextView? = null

    private lateinit var adapter: ManagerOrderAdapter
    private val allOrders = mutableListOf<Order>()
    private val displayedOrders = mutableListOf<Order>()
    private val storeList = mutableListOf<Store>()
    private val managedStoreIds = mutableListOf<Long>() // Chi nhánh được gán cho manager
    private var currentStatus: String? = null
    private var selectedStoreId: Long? = null
    private var isAdmin = false
    
    // WebSocket Manager
    private lateinit var webSocketManager: OrderWebSocketManager
    private var notificationSound: MediaPlayer? = null
    
    // Pagination - gọi API thực sự (5 đơn/trang như Shopee Partner)
    private val PAGE_SIZE = 5
    private var currentPage = 0
    private var totalPages = 0
    private var totalElements = 0L
    private var hasMoreData = true
    private var isLoading = false
    private var btnLoadMore: MaterialButton? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_manage_orders, container, false)

        initViews(view)
        setupRecyclerView()
        setupListeners()
        setupWebSocket()
        animateViewsIn()
        
        // Check if admin
        val sessionManager = SessionManager(requireContext())
        isAdmin = sessionManager.isAdmin()
        
        // Load managed stores first, then load orders
        loadManagedStores()
        loadStores()
        loadOrders()

        return view
    }
    
    private fun setupWebSocket() {
        webSocketManager = OrderWebSocketManager.getInstance()
        
        // Set listener for new orders
        webSocketManager.setOnNewOrderListener { newOrder ->
            activity?.runOnUiThread {
                handleNewOrder(newOrder)
            }
        }
        
        // Set listener for status updates
        webSocketManager.setOnStatusUpdateListener { updatedOrder ->
            activity?.runOnUiThread {
                handleOrderStatusUpdate(updatedOrder)
            }
        }
        
        // Set listener for connection state
        webSocketManager.setOnConnectionListener { connected ->
            activity?.runOnUiThread {
                updateConnectionStatus(connected)
            }
        }
        
        // Connect to WebSocket
        val baseUrl = RetrofitClient.getBaseUrl()
        webSocketManager.connect(baseUrl)
    }
    
    private fun handleNewOrder(newOrder: Order) {
        Log.d(TAG, "New order received via WebSocket: #${newOrder.id}")
        
        // Check if order matches current filter (store)
        if (selectedStoreId != null && newOrder.storeId != selectedStoreId) {
            Log.d(TAG, "Order doesn't match selected store, ignoring")
            return
        }
        
        // Check if order already exists
        val existingIndex = allOrders.indexOfFirst { it.id == newOrder.id }
        if (existingIndex >= 0) {
            Log.d(TAG, "Order already exists, updating")
            allOrders[existingIndex] = newOrder
            val displayIndex = displayedOrders.indexOfFirst { it.id == newOrder.id }
            if (displayIndex >= 0) {
                displayedOrders[displayIndex] = newOrder
                adapter.notifyItemChanged(displayIndex)
            }
        } else {
            // Add new order và sắp xếp lại theo priority
            allOrders.add(newOrder)
            sortOrdersByStatusPriority(allOrders)
            
            // Cập nhật displayedOrders
            displayedOrders.clear()
            val filteredOrders = if (selectedStoreId != null) {
                allOrders.filter { it.storeId == selectedStoreId }
            } else {
                allOrders
            }
            displayedOrders.addAll(filteredOrders)
            adapter.notifyDataSetChanged()
            
            // Scroll đến vị trí đơn mới
            val newOrderIndex = displayedOrders.indexOfFirst { it.id == newOrder.id }
            if (newOrderIndex >= 0) {
                rvOrders.scrollToPosition(newOrderIndex)
            }
            
            // Play notification sound
            playNotificationSound()
            
            // Show toast
            Toast.makeText(
                context,
                "🔔 Đơn hàng mới #${newOrder.id}",
                Toast.LENGTH_SHORT
            ).show()
        }
        
        updateStats()
        
        // Ẩn empty state nếu có đơn
        if (displayedOrders.isNotEmpty()) {
            emptyState.visibility = View.GONE
        }
    }
    
    /**
     * Sắp xếp đơn hàng theo thứ tự trạng thái ưu tiên:
     * PENDING -> MAKING -> SHIPPING -> READY -> DONE -> CANCELED
     * Trong mỗi trạng thái: sắp xếp từ cũ đến mới (createdAt ASC)
     */
    private fun sortOrdersByStatusPriority(orders: MutableList<Order>) {
        val statusPriority = mapOf(
            "PENDING" to 1,
            "MAKING" to 2,
            "SHIPPING" to 3,
            "READY" to 4,
            "DONE" to 5,
            "CANCELED" to 6
        )
        
        orders.sortWith(compareBy(
            { statusPriority[it.status] ?: 7 },  // Sắp xếp theo priority status
            { it.createdAt }                      // Trong cùng status: cũ đến mới (ASC)
        ))
    }
    
    private fun handleOrderStatusUpdate(updatedOrder: Order) {
        Log.d(TAG, "Order status update via WebSocket: #${updatedOrder.id} -> ${updatedOrder.status}")
        
        // Cập nhật order trong allOrders
        val allIndex = allOrders.indexOfFirst { it.id == updatedOrder.id }
        if (allIndex >= 0 && allIndex < allOrders.size) {
            allOrders[allIndex] = updatedOrder
        }
        
        // Sắp xếp lại theo priority status
        sortOrdersByStatusPriority(allOrders)
        
        // Cập nhật displayedOrders
        displayedOrders.clear()
        val filteredOrders = if (selectedStoreId != null) {
            allOrders.filter { it.storeId == selectedStoreId }
        } else {
            allOrders
        }
        displayedOrders.addAll(filteredOrders)
        
        try {
            adapter.notifyDataSetChanged()
        } catch (e: Exception) {
            Log.e(TAG, "Error notifying WebSocket update: ${e.message}")
        }
        
        updateStats()
    }
    
    private fun updateConnectionStatus(connected: Boolean) {
        tvConnectionStatus?.apply {
            if (connected) {
                text = "● Realtime"
                setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            } else {
                text = "○ Offline"
                setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
            }
        }
    }
    
    private fun playNotificationSound() {
        try {
            notificationSound?.release()
            notificationSound = MediaPlayer.create(context, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
            notificationSound?.start()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing notification sound: ${e.message}")
        }
    }

    private fun initViews(view: View) {
        rvOrders = view.findViewById(R.id.rv_orders)
        progressBar = view.findViewById(R.id.progress_bar)
        emptyState = view.findViewById(R.id.empty_state)
        tabLayout = view.findViewById(R.id.tab_layout)
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        headerLayout = view.findViewById(R.id.header_layout)
        cardStoreFilter = view.findViewById(R.id.card_store_filter)
        tvSelectedStore = view.findViewById(R.id.tv_selected_store)
        tvPendingCount = view.findViewById(R.id.tv_pending_count)
        tvMakingCount = view.findViewById(R.id.tv_making_count)
        tvDoneCount = view.findViewById(R.id.tv_done_count)
        btnRefresh = view.findViewById(R.id.btn_refresh)
        tvConnectionStatus = view.findViewById(R.id.tv_connection_status)
        btnLoadMore = view.findViewById(R.id.btn_load_more)
        tvHeaderText = view.findViewById(R.id.header_text)
    }
    
    private fun loadManagedStores() {
        // Admin quản lý tất cả
        if (isAdmin) {
            tvHeaderText?.text = "Quản Lý Đơn Hàng\n(Tất cả chi nhánh)"
            return
        }
        
        RetrofitClient.getInstance(requireContext()).apiService
            .getMyManagedStores()
            .enqueue(object : Callback<ApiResponse<List<Store>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<Store>>>,
                    response: Response<ApiResponse<List<Store>>>
                ) {
                    if (!isAdded) return
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        val stores = response.body()?.data ?: emptyList()
                        managedStoreIds.clear()
                        managedStoreIds.addAll(stores.mapNotNull { it.id?.toLong() })
                        
                        // Update header với chi nhánh quản lý
                        if (stores.isEmpty()) {
                            tvHeaderText?.text = "Quản Lý Đơn Hàng\n⚠️ Chưa được gán chi nhánh"
                        } else if (stores.size == 1) {
                            tvHeaderText?.text = "Quản Lý Đơn Hàng\n🏪 ${stores[0].storeName}"
                        } else {
                            val storeNames = stores.take(2).joinToString(", ") { it.storeName ?: "N/A" }
                            val suffix = if (stores.size > 2) " +${stores.size - 2}" else ""
                            tvHeaderText?.text = "Quản Lý Đơn Hàng\n🏪 $storeNames$suffix"
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<Store>>>, t: Throwable) {
                    Log.e(TAG, "Error loading managed stores: ${t.message}")
                }
            })
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(context)
        rvOrders.layoutManager = layoutManager
        rvOrders.layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_fall_down)
        adapter = ManagerOrderAdapter(requireContext(), displayedOrders)
        adapter.setOnOrderActionListener(this)
        rvOrders.adapter = adapter
        
        // Infinite scroll như Shopee Partner - tự động load khi cuộn
        rvOrders.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                // Chỉ load khi cuộn xuống
                if (dy <= 0) return
                
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                
                // Khi còn 2 item nữa là hết → load thêm
                if (!isLoading && hasMoreData) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 2) {
                        loadMoreOrders()
                    }
                }
            }
        })
    }

    private fun setupListeners() {
        swipeRefresh.setColorSchemeResources(R.color.wine_primary)
        swipeRefresh.setOnRefreshListener {
            loadOrders()
        }

        btnRefresh.setOnClickListener {
            loadOrders()
        }

        cardStoreFilter.setOnClickListener {
            showStoreFilterDialog()
        }
        
        // Nút xem thêm
        btnLoadMore?.setOnClickListener {
            loadMoreOrders()
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentStatus = when (tab.position) {
                    0 -> null
                    1 -> "PENDING"
                    2 -> "MAKING"
                    3 -> "SHIPPING"
                    4 -> "READY"
                    5 -> "DONE"
                    6 -> "CANCELED"
                    else -> null
                }
                loadOrders()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {
                loadOrders()
            }
        })
    }

    private fun animateViewsIn() {
        headerLayout.alpha = 0f
        headerLayout.translationY = -30f

        headerLayout.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(350)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun loadStores() {
        RetrofitClient.getInstance(requireContext()).apiService
            .getStores()
            .enqueue(object : Callback<ApiResponse<List<Store>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<Store>>>,
                    response: Response<ApiResponse<List<Store>>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        storeList.clear()
                        response.body()?.data?.let { stores ->
                            storeList.addAll(stores)
                        }
                        Log.d(TAG, "Stores loaded: ${storeList.size}")
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<Store>>>, t: Throwable) {
                    Log.e(TAG, "Error loading stores: ${t.message}")
                }
            })
    }

    private fun showStoreFilterDialog() {
        val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_store_filter, null)
        
        val rvStores = sheetView.findViewById<RecyclerView>(R.id.rv_stores)
        val btnClose = sheetView.findViewById<ImageView>(R.id.btn_close)
        
        // Tạo danh sách với "Tất cả chi nhánh" ở đầu (null)
        val storesWithAll = mutableListOf<Store?>()
        storesWithAll.add(null) // "Tất cả chi nhánh"
        storesWithAll.addAll(storeList)
        
        val storeAdapter = StoreFilterAdapter(storesWithAll, selectedStoreId) { selectedStore ->
            selectedStoreId = selectedStore?.id?.toLong()
            tvSelectedStore.text = selectedStore?.storeName ?: "Tất cả chi nhánh"
            applyFilters()
            bottomSheetDialog.dismiss()
        }
        
        rvStores.layoutManager = LinearLayoutManager(context)
        rvStores.adapter = storeAdapter
        
        btnClose.setOnClickListener {
            bottomSheetDialog.dismiss()
        }
        
        bottomSheetDialog.setContentView(sheetView)
        bottomSheetDialog.show()
    }

    private fun loadOrders() {
        // Reset pagination khi load mới
        currentPage = 0
        hasMoreData = true
        allOrders.clear()
        displayedOrders.clear()
        adapter.notifyDataSetChanged()
        
        // Load trang đầu tiên
        loadOrdersPage(0, isRefresh = true)
    }
    
    private fun loadOrdersPage(page: Int, isRefresh: Boolean = false) {
        if (isLoading) return
        isLoading = true
        
        if (isRefresh && !swipeRefresh.isRefreshing) {
            progressBar.visibility = View.VISIBLE
        }
        emptyState.visibility = View.GONE
        btnLoadMore?.visibility = View.GONE

        Log.d(TAG, "Loading orders page $page with status: $currentStatus, size: $PAGE_SIZE")

        RetrofitClient.getInstance(requireContext()).apiService
            .getManagerOrders(currentStatus, page, PAGE_SIZE)
            .enqueue(object : Callback<ApiResponse<PageResponse<Order>>> {
                override fun onResponse(
                    call: Call<ApiResponse<PageResponse<Order>>>,
                    response: Response<ApiResponse<PageResponse<Order>>>
                ) {
                    progressBar.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                    isLoading = false

                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!

                        if (apiResponse.success && apiResponse.data != null) {
                            val pageResponse = apiResponse.data!!
                            
                            // Cập nhật thông tin pagination
                            totalPages = pageResponse.totalPages ?: 0
                            totalElements = pageResponse.totalElements ?: 0
                            currentPage = page
                            hasMoreData = !pageResponse.isLast
                            
                            // Thêm orders vào danh sách
                            pageResponse.content?.let { newOrders ->
                                allOrders.addAll(newOrders)
                                
                                // Filter theo store nếu cần
                                val filteredNewOrders = if (selectedStoreId != null) {
                                    newOrders.filter { it.storeId == selectedStoreId }
                                } else {
                                    newOrders
                                }
                                displayedOrders.addAll(filteredNewOrders)
                            }

                            Log.d(TAG, "Orders loaded: page=$page, total=${allOrders.size}, hasMore=$hasMoreData")

                            adapter.notifyDataSetChanged()
                            updateStats()
                            updateLoadMoreButton()
                            
                            // Animation chỉ cho lần load đầu
                            if (page == 0) {
                                rvOrders.scheduleLayoutAnimation()
                            }
                            
                            // Hiển thị empty state nếu không có đơn
                            if (displayedOrders.isEmpty()) {
                                emptyState.visibility = View.VISIBLE
                                view?.findViewById<TextView>(R.id.tv_empty_message)?.text =
                                    if (selectedStoreId != null) "Không có đơn hàng tại chi nhánh này"
                                    else "Đơn hàng mới sẽ xuất hiện ở đây"
                            } else {
                                emptyState.visibility = View.GONE
                            }
                        } else {
                            val msg = apiResponse.message ?: "Không thể tải đơn hàng"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Lỗi Server: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<PageResponse<Order>>>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                    isLoading = false
                    
                    if (displayedOrders.isEmpty()) {
                        emptyState.visibility = View.VISIBLE
                    }

                    Log.e(TAG, "Connection error: ${t.message}", t)
                    Toast.makeText(context, "Không thể kết nối Server", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateStats() {
        // Đếm từ danh sách đã load (có thể không chính xác 100% nếu chưa load hết)
        val ordersToCount = if (selectedStoreId != null) {
            allOrders.filter { it.storeId == selectedStoreId }
        } else {
            allOrders
        }

        val pendingCount = ordersToCount.count { it.status == "PENDING" }
        val makingCount = ordersToCount.count { it.status == "MAKING" }
        val doneCount = ordersToCount.count { it.status == "DONE" }

        tvPendingCount.text = pendingCount.toString()
        tvMakingCount.text = makingCount.toString()
        tvDoneCount.text = doneCount.toString()
    }

    private fun applyFilters() {
        // Khi thay đổi filter store, cần reload từ đầu
        loadOrders()
    }
    
    private fun loadMoreOrders() {
        if (isLoading || !hasMoreData) return
        
        // Load trang tiếp theo từ API
        loadOrdersPage(currentPage + 1)
    }
    
    private fun updateLoadMoreButton() {
        // Ẩn nút vì đã có infinite scroll
        btnLoadMore?.visibility = View.GONE
    }

    override fun onOrderClick(order: Order) {
        val intent = Intent(requireContext(), OrderDetailActivity::class.java)
        intent.putExtra("order", order)
        startActivity(intent)
    }

    override fun onUpdateStatus(order: Order, newStatus: String) {
        val statusText = getStatusText(newStatus)
        val message = "Chuyển đơn hàng #${order.id} sang trạng thái \"$statusText\"?"

        AlertDialog.Builder(requireContext())
            .setTitle("Xác nhận")
            .setMessage(message)
            .setPositiveButton("Xác nhận") { _, _ ->
                updateOrderStatus(order.id, newStatus)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    override fun onCancelOrder(order: Order) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hủy đơn hàng")
            .setMessage("Bạn có chắc muốn hủy đơn hàng #${order.id}?")
            .setPositiveButton("Hủy đơn") { _, _ ->
                updateOrderStatus(order.id, "CANCELED")
            }
            .setNegativeButton("Không", null)
            .show()
    }

    private fun getStatusText(status: String): String {
        return when (status) {
            "PENDING" -> "Chờ xử lý"
            "MAKING" -> "Đang làm"
            "SHIPPING" -> "Đang giao"
            "READY" -> "Sẵn sàng"
            "DONE" -> "Hoàn thành"
            "CANCELED" -> "Đã hủy"
            else -> status
        }
    }

    private fun updateOrderStatus(orderId: Int, newStatus: String) {
        // Tìm index trong cả 2 list
        val allIndex = allOrders.indexOfFirst { it.id == orderId }
        val displayIndex = displayedOrders.indexOfFirst { it.id == orderId }
        val oldStatus = if (allIndex >= 0) allOrders[allIndex].status else null
        
        // Optimistic update - cập nhật UI trước
        if (allIndex >= 0 && allIndex < allOrders.size) {
            allOrders[allIndex].status = newStatus
        }
        if (displayIndex >= 0 && displayIndex < displayedOrders.size) {
            displayedOrders[displayIndex].status = newStatus
            try {
                adapter.notifyItemChanged(displayIndex)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying item change: ${e.message}")
            }
        }
        updateStats()

        RetrofitClient.getInstance(requireContext()).apiService
            .updateOrderStatus(orderId, newStatus)
            .enqueue(object : Callback<ApiResponse<Order>> {
                override fun onResponse(
                    call: Call<ApiResponse<Order>>,
                    response: Response<ApiResponse<Order>>
                ) {
                    if (!isAdded) return
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        val updatedOrder = response.body()?.data
                        if (updatedOrder != null) {
                            // Cập nhật với data từ server
                            val newAllIndex = allOrders.indexOfFirst { it.id == orderId }
                            val newDisplayIndex = displayedOrders.indexOfFirst { it.id == orderId }
                            
                            if (newAllIndex >= 0 && newAllIndex < allOrders.size) {
                                allOrders[newAllIndex] = updatedOrder
                            }
                            if (newDisplayIndex >= 0 && newDisplayIndex < displayedOrders.size) {
                                displayedOrders[newDisplayIndex] = updatedOrder
                                try {
                                    adapter.notifyItemChanged(newDisplayIndex)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error notifying: ${e.message}")
                                }
                            }
                        }
                        Toast.makeText(context, "✓ Đã cập nhật", Toast.LENGTH_SHORT).show()
                    } else {
                        // Rollback nếu thất bại
                        rollbackStatus(orderId, oldStatus)
                        val errorMsg = response.body()?.message ?: "Không thể cập nhật"
                        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Order>>, t: Throwable) {
                    if (!isAdded) return
                    rollbackStatus(orderId, oldStatus)
                    Toast.makeText(context, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                }
            })
    }
    
    private fun rollbackStatus(orderId: Int, oldStatus: String?) {
        if (oldStatus == null) return
        
        val allIndex = allOrders.indexOfFirst { it.id == orderId }
        val displayIndex = displayedOrders.indexOfFirst { it.id == orderId }
        
        if (allIndex >= 0 && allIndex < allOrders.size) {
            allOrders[allIndex].status = oldStatus
        }
        if (displayIndex >= 0 && displayIndex < displayedOrders.size) {
            displayedOrders[displayIndex].status = oldStatus
            try {
                adapter.notifyItemChanged(displayIndex)
            } catch (e: Exception) {
                Log.e(TAG, "Error rollback: ${e.message}")
            }
        }
        updateStats()
    }

    companion object {
        private const val TAG = "ManageOrdersFragment"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear lists to prevent memory leak
        allOrders.clear()
        displayedOrders.clear()
        storeList.clear()
        
        // Release notification sound
        notificationSound?.release()
        notificationSound = null
    }
    
    override fun onResume() {
        super.onResume()
        // Reconnect WebSocket if needed
        val baseUrl = RetrofitClient.getBaseUrl()
        webSocketManager.reconnectIfNeeded(baseUrl)
    }
}
