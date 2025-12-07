package com.example.doan.Fragments.Manager

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Adapters.TopSellingDrinkAdapter
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.DashboardSummary
import com.example.doan.Models.RevenueStatistics
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class DashboardFragment : Fragment() {

    private lateinit var tvTotalRevenue: TextView
    private lateinit var tvTotalOrders: TextView
    private lateinit var tvPendingOrders: TextView
    private lateinit var chartRevenue: BarChart
    private lateinit var rgChartType: RadioGroup
    private lateinit var rbDaily: RadioButton
    private lateinit var rbMonthly: RadioButton
    private lateinit var rvTopSelling: RecyclerView
    private lateinit var tvNoData: TextView
    private lateinit var progressBar: ProgressBar

    private var statistics: RevenueStatistics? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        initViews(view)
        setupChart()
        setupListeners()
        loadData()

        return view
    }

    private fun initViews(view: View) {
        tvTotalRevenue = view.findViewById(R.id.tv_total_revenue)
        tvTotalOrders = view.findViewById(R.id.tv_total_orders)
        tvPendingOrders = view.findViewById(R.id.tv_pending_orders)
        chartRevenue = view.findViewById(R.id.chart_revenue)
        rgChartType = view.findViewById(R.id.rg_chart_type)
        rbDaily = view.findViewById(R.id.rb_daily)
        rbMonthly = view.findViewById(R.id.rb_monthly)
        rvTopSelling = view.findViewById(R.id.rv_top_selling)
        tvNoData = view.findViewById(R.id.tv_no_data)
        progressBar = view.findViewById(R.id.progress_bar)

        rvTopSelling.layoutManager = LinearLayoutManager(context)
    }

    private fun setupChart() {
        chartRevenue.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            setPinchZoom(false)
            setScaleEnabled(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = Color.parseColor("#5D4037")
                textSize = 10f
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E0E0E0")
                textColor = Color.parseColor("#5D4037")
                axisMinimum = 0f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return when {
                            value >= 1000000 -> String.format("%.1fM", value / 1000000)
                            value >= 1000 -> String.format("%.0fK", value / 1000)
                            else -> String.format("%.0f", value)
                        }
                    }
                }
            }

            axisRight.isEnabled = false
            legend.isEnabled = false
            animateY(800)
        }
    }

    private fun setupListeners() {
        rgChartType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_daily -> displayDailyChart()
                R.id.rb_monthly -> displayMonthlyChart()
            }
        }
    }

    private fun loadData() {
        progressBar.visibility = View.VISIBLE

        // Load dashboard summary (for order counts)
        loadDashboardSummary()

        // Load revenue statistics (for chart and top selling)
        loadRevenueStatistics()
    }

    private fun loadDashboardSummary() {
        RetrofitClient.getInstance(requireContext()).apiService
            .getDashboardSummary()
            .enqueue(object : Callback<ApiResponse<DashboardSummary>> {
                override fun onResponse(
                    call: Call<ApiResponse<DashboardSummary>>,
                    response: Response<ApiResponse<DashboardSummary>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        response.body()?.data?.let { data ->
                            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
                            tvTotalRevenue.text = currencyFormat.format(data.totalRevenue)
                            tvTotalOrders.text = data.totalOrders.toString()
                            tvPendingOrders.text = data.pendingOrders.toString()
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<DashboardSummary>>, t: Throwable) {
                    Log.e(TAG, "Error loading dashboard summary", t)
                }
            })
    }

    private fun loadRevenueStatistics() {
        RetrofitClient.getInstance(requireContext()).apiService
            .getRevenueStatistics(days = 7, months = 6)
            .enqueue(object : Callback<ApiResponse<RevenueStatistics>> {
                override fun onResponse(
                    call: Call<ApiResponse<RevenueStatistics>>,
                    response: Response<ApiResponse<RevenueStatistics>>
                ) {
                    progressBar.visibility = View.GONE

                    if (response.isSuccessful && response.body()?.success == true) {
                        statistics = response.body()?.data
                        displayStatistics()
                    } else {
                        Log.e(TAG, "Error: ${response.code()} - ${response.message()}")
                        showNoData()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<RevenueStatistics>>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    Log.e(TAG, "Error loading statistics", t)
                    Toast.makeText(context, "Không thể kết nối Server", Toast.LENGTH_SHORT).show()
                    showNoData()
                }
            })
    }

    private fun displayStatistics() {
        statistics?.let { stats ->
            // Display chart
            if (rbDaily.isChecked) {
                displayDailyChart()
            } else {
                displayMonthlyChart()
            }

            // Display top selling drinks
            displayTopSellingDrinks(stats.topSellingDrinks)
        }
    }

    private fun displayDailyChart() {
        val dailyRevenues = statistics?.dailyRevenues

        if (dailyRevenues.isNullOrEmpty()) {
            chartRevenue.clear()
            chartRevenue.setNoDataText("Chưa có dữ liệu doanh thu")
            return
        }

        val entries = dailyRevenues.mapIndexed { index, daily ->
            BarEntry(index.toFloat(), daily.revenue.toFloat())
        }

        val labels = dailyRevenues.map { daily ->
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
                val date = inputFormat.parse(daily.date)
                date?.let { outputFormat.format(it) } ?: daily.date.takeLast(5)
            } catch (e: Exception) {
                daily.date.takeLast(5)
            }
        }

        val dataSet = BarDataSet(entries, "Doanh thu").apply {
            color = Color.parseColor("#8B4513")
            valueTextColor = Color.parseColor("#5D4037")
            valueTextSize = 9f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return when {
                        value >= 1000000 -> String.format("%.1fM", value / 1000000)
                        value >= 1000 -> String.format("%.0fK", value / 1000)
                        else -> ""
                    }
                }
            }
        }

        chartRevenue.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        chartRevenue.xAxis.labelCount = labels.size
        chartRevenue.data = BarData(dataSet).apply { barWidth = 0.6f }
        chartRevenue.invalidate()
    }

    private fun displayMonthlyChart() {
        val monthlyRevenues = statistics?.monthlyRevenues

        if (monthlyRevenues.isNullOrEmpty()) {
            chartRevenue.clear()
            chartRevenue.setNoDataText("Chưa có dữ liệu doanh thu")
            return
        }

        val entries = monthlyRevenues.mapIndexed { index, monthly ->
            BarEntry(index.toFloat(), monthly.revenue.toFloat())
        }

        val labels = monthlyRevenues.map { monthly ->
            "T${monthly.month}"
        }

        val dataSet = BarDataSet(entries, "Doanh thu").apply {
            color = Color.parseColor("#8B4513")
            valueTextColor = Color.parseColor("#5D4037")
            valueTextSize = 9f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return when {
                        value >= 1000000 -> String.format("%.1fM", value / 1000000)
                        value >= 1000 -> String.format("%.0fK", value / 1000)
                        else -> ""
                    }
                }
            }
        }

        chartRevenue.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        chartRevenue.xAxis.labelCount = labels.size
        chartRevenue.data = BarData(dataSet).apply { barWidth = 0.6f }
        chartRevenue.invalidate()
    }

    private fun displayTopSellingDrinks(drinks: List<RevenueStatistics.TopSellingDrink>?) {
        if (drinks.isNullOrEmpty()) {
            rvTopSelling.visibility = View.GONE
            tvNoData.visibility = View.VISIBLE
            return
        }

        rvTopSelling.visibility = View.VISIBLE
        tvNoData.visibility = View.GONE

        val adapter = TopSellingDrinkAdapter(requireContext(), drinks)
        rvTopSelling.adapter = adapter
    }

    private fun showNoData() {
        chartRevenue.clear()
        chartRevenue.setNoDataText("Không thể tải dữ liệu")
        rvTopSelling.visibility = View.GONE
        tvNoData.visibility = View.VISIBLE
    }

    companion object {
        private const val TAG = "DashboardFragment"
    }
}
