package com.example.doan.Fragments.Manager

import android.content.Intent
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
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
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

    private lateinit var adapter: ManagerOrderAdapter
    private val allOrders = mutableListOf<Order>()
    private val filteredOrders = mutableListOf<Order>()
    private val storeList = mutableListOf<Store>()
    private var currentStatus: String? = null
    private var selectedStoreId: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_manage_orders, container, false)

        initViews(view)
        setupRecyclerView()
        setupListeners()
        animateViewsIn()
        loadStores()
        loadOrders()

        return view
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
    }

    private fun setupRecyclerView() {
        rvOrders.layoutManager = LinearLayoutManager(context)
        rvOrders.layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_fall_down)
        adapter = ManagerOrderAdapter(requireContext(), filteredOrders)
        adapter.setOnOrderActionListener(this)
        rvOrders.adapter = adapter
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
        if (!swipeRefresh.isRefreshing) {
            progressBar.visibility = View.VISIBLE
        }
        emptyState.visibility = View.GONE

        Log.d(TAG, "Loading orders with status: $currentStatus")

        RetrofitClient.getInstance(requireContext()).apiService
            .getManagerOrders(currentStatus, 0, 100)
            .enqueue(object : Callback<ApiResponse<PageResponse<Order>>> {
                override fun onResponse(
                    call: Call<ApiResponse<PageResponse<Order>>>,
                    response: Response<ApiResponse<PageResponse<Order>>>
                ) {
                    progressBar.visibility = View.GONE
                    swipeRefresh.isRefreshing = false

                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!

                        if (apiResponse.success && apiResponse.data != null) {
                            val pageResponse = apiResponse.data!!
                            allOrders.clear()
                            pageResponse.content?.let { allOrders.addAll(it) }

                            Log.d(TAG, "Orders loaded: ${allOrders.size}")

                            updateStats()
                            applyFilters()
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
                    emptyState.visibility = View.VISIBLE

                    Log.e(TAG, "Connection error: ${t.message}", t)
                    Toast.makeText(context, "Không thể kết nối Server", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateStats() {
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
        filteredOrders.clear()

        filteredOrders.addAll(
            if (selectedStoreId != null) {
                allOrders.filter { it.storeId == selectedStoreId }
            } else {
                allOrders
            }
        )

        adapter.updateOrders(filteredOrders)
        rvOrders.scheduleLayoutAnimation()

        updateStats()

        if (filteredOrders.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            view?.findViewById<TextView>(R.id.tv_empty_message)?.text =
                if (selectedStoreId != null) "Không có đơn hàng tại chi nhánh này"
                else "Đơn hàng mới sẽ xuất hiện ở đây"
        } else {
            emptyState.visibility = View.GONE
        }
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
        progressBar.visibility = View.VISIBLE

        RetrofitClient.getInstance(requireContext()).apiService
            .updateOrderStatus(orderId, newStatus)
            .enqueue(object : Callback<ApiResponse<Order>> {
                override fun onResponse(
                    call: Call<ApiResponse<Order>>,
                    response: Response<ApiResponse<Order>>
                ) {
                    progressBar.visibility = View.GONE

                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(context, "Cập nhật trạng thái thành công", Toast.LENGTH_SHORT).show()
                        loadOrders()
                    } else {
                        val errorMsg = response.body()?.message ?: "Không thể cập nhật trạng thái"
                        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Order>>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(context, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    companion object {
        private const val TAG = "ManageOrdersFragment"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear lists to prevent memory leak
        allOrders.clear()
        filteredOrders.clear()
        storeList.clear()
    }
}
