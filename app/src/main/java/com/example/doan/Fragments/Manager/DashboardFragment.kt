package com.example.doan.Fragments.Manager

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.DashboardSummary
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class DashboardFragment : Fragment() {

    private lateinit var tvTotalRevenue: TextView
    private lateinit var tvTotalOrders: TextView
    private lateinit var tvPendingOrders: TextView
    private lateinit var rvTopSelling: RecyclerView
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        // Initialize views
        tvTotalRevenue = view.findViewById(R.id.tv_total_revenue)
        tvTotalOrders = view.findViewById(R.id.tv_total_orders)
        tvPendingOrders = view.findViewById(R.id.tv_pending_orders)
        rvTopSelling = view.findViewById(R.id.rv_top_selling)
        progressBar = view.findViewById(R.id.progress_bar)

        rvTopSelling.layoutManager = LinearLayoutManager(context)

        // Load dashboard data
        loadDashboardData()

        return view
    }

    private fun loadDashboardData() {
        progressBar.visibility = View.VISIBLE
        
        RetrofitClient.getInstance(requireContext()).apiService
            .getDashboardSummary()
            .enqueue(object : Callback<ApiResponse<DashboardSummary>> {
                override fun onResponse(
                    call: Call<ApiResponse<DashboardSummary>>,
                    response: Response<ApiResponse<DashboardSummary>>
                ) {
                    progressBar.visibility = View.GONE

                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!
                        if (apiResponse.success && apiResponse.data != null) {
                            displayDashboardData(apiResponse.data!!)
                        } else {
                            val message = apiResponse.message ?: "Không thể tải dữ liệu"
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Lỗi Server: ${response.code()}", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Error response code: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<ApiResponse<DashboardSummary>>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    Log.e(TAG, "Error loading dashboard: ${t.message}")
                    Toast.makeText(context, "Không thể kết nối Server", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun displayDashboardData(data: DashboardSummary) {
        // Format currency
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

        // Display revenue
        tvTotalRevenue.text = currencyFormat.format(data.totalRevenue)

        // Display orders
        tvTotalOrders.text = data.totalOrders.toString()
        tvPendingOrders.text = data.pendingOrders.toString()

        // Display top selling drinks
        data.topSellingDrinks?.takeIf { it.isNotEmpty() }?.let {
            Toast.makeText(context, "Top selling: ${it.size} món", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "DashboardFragment"
    }
}
