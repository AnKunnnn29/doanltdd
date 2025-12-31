package com.example.doan.Fragments.Manager

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class ForecastFragment : Fragment() {

    // Weather Views
    private lateinit var cardWeather: MaterialCardView
    private lateinit var ivWeatherIcon: ImageView
    private lateinit var tvTemperature: TextView
    private lateinit var tvWeatherCondition: TextView
    private lateinit var tvWeatherCity: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var tvRainProb: TextView
    private lateinit var tvWeatherImpact: TextView
    private lateinit var tvWeatherRecommendation: TextView
    private lateinit var tvSuggestedDrinks: TextView
    private lateinit var btnWeatherAlerts: MaterialButton
    private lateinit var layoutBusinessImpact: LinearLayout

    // Weather data
    private var weatherData: WeatherResponse? = null

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
        // Weather Forecast
        cardWeather = view.findViewById(R.id.card_weather)
        ivWeatherIcon = view.findViewById(R.id.iv_weather_icon)
        tvTemperature = view.findViewById(R.id.tv_temperature)
        tvWeatherCondition = view.findViewById(R.id.tv_weather_condition)
        tvWeatherCity = view.findViewById(R.id.tv_weather_city)
        tvHumidity = view.findViewById(R.id.tv_humidity)
        tvRainProb = view.findViewById(R.id.tv_rain_prob)
        tvWeatherImpact = view.findViewById(R.id.tv_weather_impact)
        tvWeatherRecommendation = view.findViewById(R.id.tv_weather_recommendation)
        tvSuggestedDrinks = view.findViewById(R.id.tv_suggested_drinks)
        btnWeatherAlerts = view.findViewById(R.id.btn_weather_alerts)
        layoutBusinessImpact = view.findViewById(R.id.layout_business_impact)

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
        listOf(cardWeather, cardRevenueForecast, cardPeakHours, cardLowStock, cardStaffing, cardOverload).forEach {
            it.alpha = 0f
            it.translationY = 50f
        }

        // Weather alerts button click
        btnWeatherAlerts.setOnClickListener {
            showWeatherAlertsDialog()
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
        val cards = listOf(cardWeather, cardRevenueForecast, cardPeakHours, cardLowStock, cardStaffing, cardOverload)
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

        // Load weather forecast
        loadWeatherForecast()

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

        // Hiển thị ngày + độ tin cậy
        val labels = dailyForecasts.map { daily ->
            val dayName = daily.dayOfWeek?.take(3) ?: ""
            val confidence = daily.confidence?.toInt() ?: 0
            "$dayName\n($confidence%)"
        }

        val dataSet = BarDataSet(entries, "Dự báo doanh thu").apply {
            // Màu sắc theo độ tin cậy
            colors = dailyForecasts.map { daily ->
                val confidence = daily.confidence ?: 85.0
                when {
                    confidence >= 80 -> Color.parseColor("#4CAF50") // Xanh - tin cậy cao
                    confidence >= 65 -> Color.parseColor("#FF9800") // Cam - trung bình
                    else -> Color.parseColor("#F44336") // Đỏ - thấp
                }
            }
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

    // ==================== WEATHER FORECAST ====================

    private fun loadWeatherForecast() {
        RetrofitClient.getInstance(requireContext()).apiService
            .getCurrentWeather()
            .enqueue(object : Callback<ApiResponse<WeatherResponse>> {
                override fun onResponse(
                    call: Call<ApiResponse<WeatherResponse>>,
                    response: Response<ApiResponse<WeatherResponse>>
                ) {
                    if (!isAdded) return

                    if (response.isSuccessful && response.body()?.success == true) {
                        response.body()?.data?.let { weather ->
                            weatherData = weather
                            displayWeatherForecast(weather)
                        }
                    } else {
                        Log.e(TAG, "Weather error: ${response.code()}")
                        showDefaultWeather()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<WeatherResponse>>, t: Throwable) {
                    if (!isAdded) return
                    Log.e(TAG, "Weather load failed", t)
                    showDefaultWeather()
                }
            })
    }

    private fun displayWeatherForecast(weather: WeatherResponse) {
        // Current Weather
        tvTemperature.text = "${weather.temperature?.toInt() ?: 30}°C"
        tvWeatherCondition.text = weather.description ?: getConditionText(weather.condition)
        tvWeatherCity.text = "${weather.city ?: "TP. Hồ Chí Minh"}, ${weather.country ?: "VN"}"
        tvHumidity.text = "💧 ${weather.humidity ?: 70}%"
        
        // Estimate rain probability from condition
        val rainProb = estimateRainProbability(weather.condition)
        tvRainProb.text = "🌧️ ${rainProb}%"

        // Load weather icon
        weather.iconUrl?.let { url ->
            Glide.with(this)
                .load(url)
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(ivWeatherIcon)
        }

        // Temperature color
        val temp = weather.temperature ?: 30.0
        tvTemperature.setTextColor(
            when {
                temp >= 35 -> Color.parseColor("#D32F2F") // Đỏ - rất nóng
                temp >= 32 -> Color.parseColor("#FF5722") // Cam - nóng
                temp >= 28 -> Color.parseColor("#FF9800") // Vàng cam - ấm
                temp < 22 -> Color.parseColor("#2196F3") // Xanh - lạnh
                else -> Color.parseColor("#4CAF50") // Xanh lá - dễ chịu
            }
        )

        // Business Insight
        weather.businessInsight?.let { insight ->
            val impactPercent = insight.expectedImpact ?: 0.0
            val impactText = if (impactPercent >= 0) "+${impactPercent.toInt()}%" else "${impactPercent.toInt()}%"
            val impactIcon = if (impactPercent >= 0) "📈" else "📉"

            tvWeatherImpact.text = "$impactIcon Dự kiến doanh thu $impactText"
            tvWeatherImpact.setTextColor(
                if (impactPercent >= 0) Color.parseColor("#4CAF50")
                else Color.parseColor("#F44336")
            )

            tvWeatherRecommendation.text = insight.recommendation ?: "Thời tiết bình thường"

            // Suggested drinks
            val drinks = insight.suggestedDrinks?.take(5)
            tvSuggestedDrinks.text = drinks?.joinToString(", ") ?: "Trà sữa, Trà đào"

            // Background color based on weather type
            val bgColor = when (insight.weatherType) {
                "HOT" -> "#FFF3E0" // Cam nhạt
                "RAINY" -> "#E3F2FD" // Xanh dương nhạt
                "COLD" -> "#E8EAF6" // Tím nhạt
                else -> "#E8F5E9" // Xanh lá nhạt
            }
            layoutBusinessImpact.setBackgroundColor(Color.parseColor(bgColor))
        }

        // Update alerts button based on weather condition
        val hasAlert = weather.condition in listOf("Rain", "Thunderstorm") || (weather.temperature ?: 30.0) >= 35
        btnWeatherAlerts.text = if (hasAlert) {
            "⚠️ Xem chi tiết cảnh báo"
        } else {
            "ℹ️ Xem gợi ý kinh doanh"
        }

        if (hasAlert) {
            btnWeatherAlerts.setBackgroundColor(Color.parseColor("#FFCDD2"))
            btnWeatherAlerts.setTextColor(Color.parseColor("#D32F2F"))
        }
    }

    private fun estimateRainProbability(condition: String?): Int {
        return when (condition) {
            "Clear" -> 5
            "Clouds" -> 20
            "Rain" -> 80
            "Thunderstorm" -> 95
            "Drizzle" -> 60
            else -> 15
        }
    }

    private fun showDefaultWeather() {
        tvTemperature.text = "30°C"
        tvWeatherCondition.text = "Không thể tải"
        tvWeatherCity.text = "TP. Hồ Chí Minh"
        tvHumidity.text = "💧 70%"
        tvRainProb.text = "🌧️ 20%"
        tvWeatherImpact.text = "📊 Không có dữ liệu"
        tvWeatherRecommendation.text = "Vui lòng kiểm tra kết nối mạng"
        tvSuggestedDrinks.text = "Trà sữa, Trà đào"
        btnWeatherAlerts.text = "Xem gợi ý kinh doanh"
    }

    private fun getConditionText(condition: String?): String {
        return when (condition) {
            "Clear" -> "Trời quang đãng"
            "Clouds" -> "Có mây"
            "Rain" -> "Có mưa"
            "Thunderstorm" -> "Có giông bão"
            "Drizzle" -> "Mưa phùn"
            "Snow" -> "Có tuyết"
            "Mist", "Fog" -> "Sương mù"
            else -> condition ?: "Bình thường"
        }
    }

    private fun showWeatherAlertsDialog() {
        val weather = weatherData
        if (weather == null) {
            Toast.makeText(context, "Chưa có dữ liệu thời tiết", Toast.LENGTH_SHORT).show()
            return
        }

        val insight = weather.businessInsight
        val temp = weather.temperature ?: 30.0
        val condition = weather.condition ?: "Normal"

        val message = buildString {
            appendLine("🌡️ Nhiệt độ: ${temp.toInt()}°C")
            appendLine("☁️ Điều kiện: ${getConditionText(condition)}")
            appendLine("💧 Độ ẩm: ${weather.humidity ?: 70}%")
            appendLine("💨 Gió: ${weather.windSpeed ?: 0} m/s")
            appendLine()
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine()
            
            insight?.let {
                appendLine("📊 Phân tích kinh doanh:")
                appendLine()
                appendLine("• Loại thời tiết: ${it.weatherType ?: "NORMAL"}")
                appendLine("• Ảnh hưởng doanh thu: ${if ((it.expectedImpact ?: 0.0) >= 0) "+" else ""}${it.expectedImpact?.toInt() ?: 0}%")
                appendLine()
                appendLine("💡 Gợi ý:")
                appendLine(it.recommendation ?: "Duy trì hoạt động bình thường")
                appendLine()
                appendLine("🍹 Đồ uống nên đẩy mạnh:")
                appendLine(it.suggestedDrinks?.joinToString(", ") ?: "Trà sữa, Trà đào")
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("🌤️ Chi tiết thời tiết & Gợi ý")
            .setMessage(message)
            .setPositiveButton("Đã hiểu", null)
            .show()
    }
}
