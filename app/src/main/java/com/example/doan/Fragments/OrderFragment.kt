package com.example.doan.Fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Activities.OrderDetailActivity
import com.example.doan.Adapters.OrderAdapter
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.Order
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.DataCache
import com.example.doan.Utils.LoadingDialog
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OrderFragment : Fragment(), OrderAdapter.OnOrderClickListener {

    private lateinit var ordersRecyclerView: RecyclerView
    private lateinit var orderAdapter: OrderAdapter
    private lateinit var tvLoginPrompt: TextView
    private lateinit var toolbar: Toolbar
    private lateinit var loadingDialog: LoadingDialog

    private val orderList = mutableListOf<Order>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_order, container, false)

        loadingDialog = LoadingDialog(requireContext())
        
        toolbar = view.findViewById(R.id.toolbar_order)
        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)

        ordersRecyclerView = view.findViewById(R.id.orders_recycler_view)
        tvLoginPrompt = view.findViewById(R.id.tv_login_prompt_order)
        ordersRecyclerView.layoutManager = LinearLayoutManager(context)

        orderAdapter = OrderAdapter(requireContext(), orderList)
        orderAdapter.setOnOrderClickListener(this)
        ordersRecyclerView.adapter = orderAdapter

        return view
    }

    override fun onResume() {
        super.onResume()
        checkLoginAndLoadOrders()
    }

    private fun checkLoginAndLoadOrders() {
        val userId = getLoggedInUserId()
        if (userId != -1) {
            tvLoginPrompt.visibility = View.GONE
            ordersRecyclerView.visibility = View.VISIBLE
            loadOrders(userId)
        } else {
            tvLoginPrompt.visibility = View.VISIBLE
            ordersRecyclerView.visibility = View.GONE
            orderList.clear()
            orderAdapter.notifyDataSetChanged()
        }
    }

    private fun getLoggedInUserId(): Int {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_USER_ID, -1)
    }

    private fun loadOrders(userId: Int) {
        Log.d(TAG, "Tải đơn hàng cho User ID: $userId")
        
        // Kiểm tra cache trước
        val cachedOrders = DataCache.orderHistory
        if (!cachedOrders.isNullOrEmpty()) {
            orderList.clear()
            orderList.addAll(cachedOrders)
            orderAdapter.notifyDataSetChanged()
            
            // Refresh trong background
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
                            orderList.clear()
                            orderList.addAll(apiResponse.data!!)
                            orderAdapter.notifyDataSetChanged()
                            
                            // Lưu vào cache
                            DataCache.orderHistory = apiResponse.data

                            if (orderList.isEmpty()) {
                                tvLoginPrompt.text = "Bạn chưa có đơn hàng nào."
                                tvLoginPrompt.visibility = View.VISIBLE
                            }
                        } else {
                            Toast.makeText(context, "Lỗi tải đơn hàng: ${apiResponse.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Lỗi Server: ${response.code()}", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Response Code: ${response.code()}")
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
                            orderList.clear()
                            orderList.addAll(apiResponse.data!!)
                            orderAdapter.notifyDataSetChanged()
                            DataCache.orderHistory = apiResponse.data
                            
                            if (orderList.isEmpty()) {
                                tvLoginPrompt.text = "Bạn chưa có đơn hàng nào."
                                tvLoginPrompt.visibility = View.VISIBLE
                            }
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
        private const val PREFS_NAME = "UTETeaPrefs"
        private const val KEY_USER_ID = "user_id"
    }
}
