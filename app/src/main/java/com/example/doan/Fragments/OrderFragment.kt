package com.example.doan.Fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Adapters.OrderAdapter
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.Order
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OrderFragment : Fragment() {

    private lateinit var ordersRecyclerView: RecyclerView
    private lateinit var orderAdapter: OrderAdapter

    private val orderList = mutableListOf<Order>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return try {
            val view = inflater.inflate(R.layout.fragment_order, container, false)

            ordersRecyclerView = view.findViewById(R.id.orders_recycler_view)
            ordersRecyclerView.layoutManager = LinearLayoutManager(context)

            orderAdapter = OrderAdapter(requireContext(), orderList)
            ordersRecyclerView.adapter = orderAdapter

            val userId = getLoggedInUserId()

            if (userId != -1) {
                loadOrders(userId)
            } else {
                Toast.makeText(context, "Vui lòng đăng nhập để xem đơn hàng.", Toast.LENGTH_LONG).show()
            }

            view
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreateView: ${e.message}")
            e.printStackTrace()
            Toast.makeText(context, "Lỗi tải đơn hàng", Toast.LENGTH_SHORT).show()
            inflater.inflate(R.layout.fragment_order, container, false)
        }
    }

    private fun getLoggedInUserId(): Int {
        context?.let {
            val prefs = it.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getInt(KEY_USER_ID, -1)
        }
        return -1
    }

    private fun loadOrders(userId: Int) {
        Log.d(TAG, "Tải đơn hàng cho User ID: $userId")

        RetrofitClient.getInstance(requireContext()).apiService.getUserOrders(userId)
            .enqueue(object : Callback<ApiResponse<List<Order>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<Order>>>,
                    response: Response<ApiResponse<List<Order>>>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!
                        if (apiResponse.success && apiResponse.data != null) {
                            orderList.clear()
                            orderList.addAll(apiResponse.data!!)
                            orderAdapter.notifyDataSetChanged()

                            if (orderList.isEmpty()) {
                                Toast.makeText(context, "Bạn chưa có đơn hàng nào.", Toast.LENGTH_LONG).show()
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
                    Log.e(TAG, "Lỗi kết nối API đơn hàng: ${t.message}")
                    Toast.makeText(context, "Không thể kết nối đến máy chủ", Toast.LENGTH_LONG).show()
                }
            })
    }

    companion object {
        private const val TAG = "OrderFragment"
        private const val PREFS_NAME = "UserPrefs"
        private const val KEY_USER_ID = "userId"
    }
}
