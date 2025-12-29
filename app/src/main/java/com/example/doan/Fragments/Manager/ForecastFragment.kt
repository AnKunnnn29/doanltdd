package com.example.doan.Fragments.Manager

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Adapters.LowStockAdapter
import com.example.doan.Adapters.OverloadWarningAdapter
import com.example.doan.Adapters.PeakHourAdapter
import com.example.doan.Adapters.StaffingAdapter
import com.example.doan.Models.*
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.card.MaterialCardView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class ForecastFragment : Fragment() {

    // Revenue Forecast Views
    private lateinit var tvTodayForecast: TextView
    private lateinit var tvTomorrowForecast: TextView
    private lateinit var tvWeekForecast: TextView
    private lateinit var tvGrowthRate: TextView
    private lateinit var tvTrend: TextView
    private lateinit var chartForecast: BarChart

    // Peak Hours Views
    private lateinit var rvPeakHours: RecyclerView

    // Low Stock Views
    private lateinit var rvLowStock: RecyclerView
    private lateinit var tvNoLowStock: TextView

    // Staffing Views
    private lateinit var rvStaffing: RecyclerView

    // Overload Warnings Views
    private lateinit var rvOverload: RecyclerView
    private lateinit var tvNoOverload: TextView

    // Cards for animation
    private lateinit var cardRevenueForecast: MaterialCardView
    private lateinit var cardPeakHours: MaterialCardView
    private lateinit var cardLowStock: MaterialCardView
    private lateinit var cardStaffing: MaterialCardView
    private lateinit var cardOverload: MaterialCardView

    private lateinit var progressBar: ProgressBar

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_forecast, container, false)
        initViews(view)
        setupChart()
        loadForecastData()
        return view
    }

    private fun initViews(view: View) {
        // Revenue Forecast
        tvTodayForecast = view.findViewById(R.id.tv_today_forecast)
        tvTomorrowForecast = view.findViewById(R.id.tv_tomorrow_forecast)
        tvWeekForecast = view.findViewById(R.id.tv_week_forecast)
        tvGrowthRate = view.findViewById(R.id.tv_growth_rate)
        tvTrend = view.findViewById(R.id.tv_trend)
        chartForecast = view.findViewById(R.id.chart_forecast)

        // Peak Hours
        rvPeakHours = view.findViewById(R.id.rv_peak_hours)
        rvPeakHours.layoutManager = LinearLayoutManager(context)

        // Low Stock
        rvLowStock = view.findViewById(R.id.rv_low_stock)
        rvLowStock.layoutManager = LinearLayoutManager(context)
        tvNoLowStock = view.findViewById(R.id.tv_no_low_stock)

        // Staffing
        rvStaffing = view.findViewById(R.id.rv_staffing)
        rvStaffing.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        // Overload
        rvOverload = view.findViewById(R.id.rv_overload)
        rvOverload.layoutManager = LinearLayoutManager(context)
        tvNoOverload = view.findViewById(R.id.tv_no_overload)

        // Cards
        cardRevenueForecast = view.findViewById(R.id.card_revenue_forecast)
        cardPeakHours = view.findViewById(R.id.card_peak_hours)
        cardLowStock = view.findViewById(R.id.card_low_stock)
        cardStaffing = view.findViewById(R.id.card_staffing)
        cardOverload = view.findViewById(R.id.card_overload)

        progressBar = view.findViewById(R.id.progress_bar)

        // Initial state for animations
        listOf(cardRevenueForecast, cardPeakHours, cardLowStock, cardStaffing, cardOverload).forEach {
            it.alpha = 0f
            it.translationY = 50f
        }
    }


    private fun setupChart() {
        chartForecast.apply {
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

    private fun animateCardsIn() {
        val cards = listOf(cardRevenueForecast, cardPeakHours, cardLowStock, cardStaffing, cardOverload)
        cards.forEachIndexed { index, card ->
            card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay((index * 100).toLong())
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun loadForecastData() {
        progressBar.visibility = View.VISIBLE

        RetrofitClient.getInstance(requireContext()).apiService
            .getFullForecast()
            .enqueue(object : Callback<ApiResponse<ForecastDto>> {
                override fun onResponse(
                    call: Call<ApiResponse<ForecastDto>>,
                    response: Response<ApiResponse<ForecastDto>>
                ) {
                    if (!isAdded) return
                    progressBar.visibility = View.GONE

                    if (response.isSuccessful && response.body()?.success == true) {
                        response.body()?.data?.let { forecast ->
                            displayForecast(forecast)
                            animateCardsIn()
                        }
                    } else {
                        Log.e(TAG, "Error: ${response.code()} - ${response.message()}")
                        showError("Không thể tải dữ liệu dự báo")
                        animateCardsIn()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<ForecastDto>>, t: Throwable) {
                    if (!isAdded) return
                    progressBar.visibility = View.GONE
                    Log.e(TAG, "Error loading forecast", t)
                    showError("Không thể kết nối Server")
                    animateCardsIn()
                }
            })
    }

    private fun displayForecast(forecast: ForecastDto) {
        // Revenue Forecast
        forecast.revenueForecast?.let { revenue ->
            tvTodayForecast.text = currencyFormat.format(revenue.todayForecast ?: 0)
            tvTomorrowForecast.text = currencyFormat.format(revenue.tomorrowForecast ?: 0)
            tvWeekForecast.text = currencyFormat.format(revenue.weekForecast ?: 0)

            val growthRate = revenue.growthRate ?: 0.0
            tvGrowthRate.text = String.format("%+.1f%%", growthRate)
            tvGrowthRate.setTextColor(
                if (growthRate >= 0) Color.parseColor("#4CAF50")
                else Color.parseColor("#F44336")
            )

            tvTrend.text = when (revenue.trend) {
                "UP" -> "📈 Tăng trưởng"
                "DOWN" -> "📉 Giảm"
                else -> "➡️ Ổn định"
            }

            // Display forecast chart
            displayForecastChart(revenue.dailyForecasts)
        }

        // Peak Hours
        forecast.peakHours?.let { peakHours ->
            if (peakHours.isNotEmpty()) {
                rvPeakHours.adapter = PeakHourAdapter(requireContext(), peakHours.take(5))
            }
        }

        // Low Stock Warnings
        forecast.lowStockWarnings?.let { warnings ->
            if (warnings.isNotEmpty()) {
                rvLowStock.visibility = View.VISIBLE
                tvNoLowStock.visibility = View.GONE
                rvLowStock.adapter = LowStockAdapter(requireContext(), warnings)
            } else {
                rvLowStock.visibility = View.GONE
                tvNoLowStock.visibility = View.VISIBLE
            }
        } ?: run {
            rvLowStock.visibility = View.GONE
            tvNoLowStock.visibility = View.VISIBLE
        }

        // Staffing Recommendations
        forecast.staffingRecommendations?.let { staffing ->
            if (staffing.isNotEmpty()) {
                rvStaffing.adapter = StaffingAdapter(requireContext(), staffing)
            }
        }

        // Overload Warnings
        forecast.overloadWarnings?.let { warnings ->
            if (warnings.isNotEmpty()) {
                rvOverload.visibility = View.VISIBLE
                tvNoOverload.visibility = View.GONE
                rvOverload.adapter = OverloadWarningAdapter(requireContext(), warnings)
            } else {
                rvOverload.visibility = View.GONE
                tvNoOverload.visibility = View.VISIBLE
            }
        } ?: run {
            rvOverload.visibility = View.GONE
            tvNoOverload.visibility = View.VISIBLE
        }
    }

    private fun displayForecastChart(dailyForecasts: List<DailyForecast>?) {
        if (dailyForecasts.isNullOrEmpty()) {
            chartForecast.clear()
            chartForecast.setNoDataText("Chưa có dữ liệu dự báo")
            return
        }

        val entries = dailyForecasts.mapIndexed { index, daily ->
            BarEntry(index.toFloat(), (daily.forecastRevenue ?: 0.0).toFloat())
        }

        val labels = dailyForecasts.map { it.dayOfWeek?.take(3) ?: "" }

        val dataSet = BarDataSet(entries, "Dự báo doanh thu").apply {
            color = Color.parseColor("#FF9800")
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

        chartForecast.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        chartForecast.xAxis.labelCount = labels.size
        chartForecast.data = BarData(dataSet).apply { barWidth = 0.6f }
        chartForecast.invalidate()
    }

    private fun showError(message: String) {
        context?.let {
            Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "ForecastFragment"
    }
}
