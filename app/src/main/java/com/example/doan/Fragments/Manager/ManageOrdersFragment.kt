package com.example.doan.Fragments.Manager

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.doan.Activities.OrderDetailActivity
import com.example.doan.Adapters.ManagerOrderAdapter
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.Order
import com.example.doan.Models.PageResponse
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
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

    private lateinit var adapter: ManagerOrderAdapter
    private var orderList = mutableListOf<Order>()
    private var currentStatus: String? = null // null = all orders

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_manage_orders, container, false)

        // Initialize views
        rvOrders = view.findViewById(R.id.rv_orders)
        progressBar = view.findViewById(R.id.progress_bar)
        emptyState = view.findViewById(R.id.empty_state)
        tabLayout = view.findViewById(R.id.tab_layout)
        swipeRefresh = view.findViewById(R.id.swipe_refresh)

        // Setup RecyclerView
        rvOrders.layoutManager = LinearLayoutManager(context)
        adapter = ManagerOrderAdapter(requireContext(), orderList)
        adapter.setOnOrderActionListener(this)
        rvOrders.adapter = adapter

        // Setup SwipeRefresh
        swipeRefresh.setOnRefreshListener {
            loadOrders()
        }

        // Setup TabLayout
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentStatus = when (tab.position) {
                    0 -> null // Tất cả
                    1 -> "PENDING" // Chờ xử lý
                    2 -> "MAKING" // Đang làm
                    3 -> "SHIPPING" // Đang giao
                    4 -> "READY" // Sẵn sàng (Pickup)
                    5 -> "DONE" // Hoàn thành
                    6 -> "CANCELED" // Đã hủy
                    else -> null
                }
                loadOrders()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {
                // Double tap to refresh
                loadOrders()
            }
        })

        // Load orders
        loadOrders()

        return view
    }

    override fun onOrderClick(order: Order) {
        // Mở màn hình chi tiết đơn hàng
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
                    
                    Log.d(TAG, "Response code: ${response.code()}")

                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!
                        
                        if (apiResponse.success && apiResponse.data != null) {
                            val pageResponse = apiResponse.data!!
                            orderList = pageResponse.content?.toMutableList() ?: mutableListOf()
                            
                            Log.d(TAG, "Orders loaded: ${orderList.size}")
                            
                            adapter.updateOrders(orderList)

                            if (orderList.isEmpty()) {
                                emptyState.visibility = View.VISIBLE
                            }
                        } else {
                            val msg = apiResponse.message ?: "Không thể tải đơn hàng"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            Log.e(TAG, "API Error: $msg")
                        }
                    } else {
                        Toast.makeText(context, "Lỗi Server: ${response.code()}", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Error code: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<ApiResponse<PageResponse<Order>>>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                    emptyState.visibility = View.VISIBLE
                    
                    val errorMsg = "Không thể kết nối Server"
                    Log.e(TAG, "Connection error: ${t.message}", t)
                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                }
            })
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
                        // Reload orders to reflect changes
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
}
