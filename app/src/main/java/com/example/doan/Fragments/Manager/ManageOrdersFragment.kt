package com.example.doan.Fragments.Manager

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Adapters.OrderAdapter
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.Order
import com.example.doan.Models.PageResponse
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.google.android.material.tabs.TabLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ManageOrdersFragment : Fragment() {

    private lateinit var rvOrders: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: View
    private lateinit var tabLayout: TabLayout

    private lateinit var adapter: OrderAdapter
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

        // Setup RecyclerView
        rvOrders.layoutManager = LinearLayoutManager(context)
        adapter = OrderAdapter(requireContext(), orderList)
        rvOrders.adapter = adapter

        // Setup TabLayout
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentStatus = when (tab.position) {
                    0 -> null // Tất cả
                    1 -> "PENDING" // Chờ xử lý
                    2 -> "MAKING" // Đang làm
                    3 -> "SHIPPING" // Đang giao
                    4 -> "DONE" // Hoàn thành
                    5 -> "CANCELED" // Đã hủy
                    else -> null
                }
                loadOrders()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // Load orders
        loadOrders()

        return view
    }

    private fun loadOrders() {
        progressBar.visibility = View.VISIBLE
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
                    
                    Log.d(TAG, "Response code: ${response.code()}")
                    Log.d(TAG, "Response successful: ${response.isSuccessful}")

                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!
                        Log.d(TAG, "ApiResponse success: ${apiResponse.success}")
                        Log.d(TAG, "ApiResponse data null: ${apiResponse.data == null}")
                        
                        if (apiResponse.success && apiResponse.data != null) {
                            val pageResponse = apiResponse.data!!
                            orderList = pageResponse.content?.toMutableList() ?: mutableListOf()
                            
                            Log.d(TAG, "Orders loaded: ${orderList.size}")
                            
                            adapter.updateOrders(orderList)

                            if (orderList.isEmpty()) {
                                emptyState.visibility = View.VISIBLE
                                Log.d(TAG, "No orders found")
                            } else {
                                Log.d(TAG, "Displaying ${orderList.size} orders")
                            }
                        } else {
                            val msg = apiResponse.message ?: "Không thể tải đơn hàng"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            Log.e(TAG, "API Error: $msg")
                        }
                    } else {
                        Toast.makeText(context, "Lỗi Server: ${response.code()}", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Error code: ${response.code()}")
                        try {
                            response.errorBody()?.let {
                                Log.e(TAG, "Error body: ${it.string()}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error reading error body", e)
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<PageResponse<Order>>>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    emptyState.visibility = View.VISIBLE
                    
                    val errorMsg = "Không thể kết nối Server\n${t.javaClass.simpleName}: ${t.message}"
                    Log.e(TAG, "Connection error: ${t.message}", t)
                    
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                }
            })
    }

    companion object {
        private const val TAG = "ManageOrdersFragment"
    }
}
