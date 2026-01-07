package com.example.doan.Activities

import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.doan.Adapters.ActivityLogAdapter
import com.example.doan.Adapters.BlockedIPAdapter
import com.example.doan.Adapters.MonitoringAlertAdapter
import com.example.doan.Adapters.RiskScoreAdapter
import com.example.doan.Adapters.WhitelistIPAdapter
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.BlockIPRequest
import com.example.doan.Models.BlockedIP
import com.example.doan.Models.HandleAlertRequest
import com.example.doan.Models.MonitoringAlert
import com.example.doan.Models.MonitoringDashboard
import com.example.doan.Models.PageResponse
import com.example.doan.Models.User
import com.example.doan.Models.UserActivityLog
import com.example.doan.Models.UserRiskScore
import com.example.doan.Models.WhitelistedIP
import com.example.doan.Network.ApiService
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserMonitoringActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var btnBlockIP: ImageButton
    private lateinit var btnRefresh: ImageButton
    private lateinit var tabLayout: TabLayout
    private lateinit var scrollFilter: HorizontalScrollView
    private lateinit var chipGroupFilter: ChipGroup
    private lateinit var scrollDashboard: ScrollView
    private lateinit var frameContent: FrameLayout
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var tvEmpty: TextView
    private lateinit var fabAction: FloatingActionButton

    private lateinit var tvPendingAlerts: TextView
    private lateinit var tvCriticalAlerts: TextView
    private lateinit var tvSuspiciousUsers: TextView
    private lateinit var tvBlockedIPs: TextView
    private lateinit var tvActivities24h: TextView
    private lateinit var cardBlockedIPs: View
    private lateinit var rvRecentAlerts: RecyclerView
    private lateinit var rvTopRiskyUsers: RecyclerView
    private lateinit var rvRecentActivities: RecyclerView

    private lateinit var apiService: ApiService

    private var activityLogAdapter: ActivityLogAdapter? = null
    private var alertAdapter: MonitoringAlertAdapter? = null
    private var riskScoreAdapter: RiskScoreAdapter? = null
    private var blockedIPAdapter: BlockedIPAdapter? = null

    private var currentTab = 0
    private var currentPage = 0
    private var isLoading = false
    private var hasMoreData = true
    private var currentRiskFilter: String? = null
    private var currentStatusFilter: String? = null
    
    // 🔄 Auto-refresh cho real-time monitoring
    private val refreshHandler = Handler(Looper.getMainLooper())
    private var autoRefreshRunnable: Runnable? = null
    private val AUTO_REFRESH_INTERVAL = 10000L // 10 seconds
    private var isAutoRefreshEnabled = true
    
    // 🔊 Sound & Vibration cho critical alerts
    private var soundPool: SoundPool? = null
    private var alertSoundId: Int = 0
    private var vibrator: Vibrator? = null
    
    // 📊 Tracking để detect new alerts - Theo dõi số lượng cảnh báo để phát hiện cảnh báo mới
    private var lastCriticalAlertCount: Long = 0
    private var lastPendingAlertCount: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_monitoring)

        apiService = RetrofitClient.getInstance(this).apiService

        initViews()
        setupTabLayout()
        setupRecyclerView()
        setupSwipeRefresh()
        setupClickListeners()
        setupSoundAndVibration()

        loadDashboard()
        startAutoRefresh()
    }
    
    // 🔊 Setup Sound & Vibration
    private fun setupSoundAndVibration() {
        // Sound Pool cho alert sound
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttributes)
            .build()
        
        // Load alert sound (sử dụng system sound)
        try {
            alertSoundId = soundPool?.load(this, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI.hashCode(), 1) ?: 0
        } catch (e: Exception) {
            // Fallback nếu không load được
        }
        
        // Vibrator
        vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
    }
    
    // 🔄 Start Auto-Refresh
    private fun startAutoRefresh() {
        autoRefreshRunnable = object : Runnable {
            override fun run() {
                if (isAutoRefreshEnabled && !isLoading && !swipeRefresh.isRefreshing) {
                    silentRefresh()
                }
                refreshHandler.postDelayed(this, AUTO_REFRESH_INTERVAL)
            }
        }
        refreshHandler.postDelayed(autoRefreshRunnable!!, AUTO_REFRESH_INTERVAL)
    }
    
    // 🔄 Stop Auto-Refresh
    private fun stopAutoRefresh() {
        autoRefreshRunnable?.let { refreshHandler.removeCallbacks(it) }
    }
    
    // 🔄 Silent Refresh (không show loading)
    private fun silentRefresh() {
        when (currentTab) {
            0 -> loadDashboardSilent()
            1 -> loadAlertsSilent()
            2 -> loadActivitiesSilent()
            3 -> loadRiskScoresSilent()
            // Tab 4, 5 không cần auto-refresh
        }
    }
    
    // 📊 Load Dashboard Silent (không show loading, check new alerts)
    private fun loadDashboardSilent() {
        apiService.getMonitoringDashboard().enqueue(object : Callback<ApiResponse<MonitoringDashboard>> {
            override fun onResponse(call: Call<ApiResponse<MonitoringDashboard>>, response: Response<ApiResponse<MonitoringDashboard>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    response.body()?.data?.let { dashboard ->
                        // Check for new critical alerts
                        val newCritical = dashboard.criticalAlerts ?: 0
                        val newPending = dashboard.totalPendingAlerts ?: 0
                        
                        if (newCritical > lastCriticalAlertCount) {
                            // 🚨 New critical alert detected!
                            playCriticalAlert()
                            Toast.makeText(this@UserMonitoringActivity, 
                                "🚨 ${newCritical - lastCriticalAlertCount} cảnh báo nghiêm trọng mới!", 
                                Toast.LENGTH_LONG).show()
                        } else if (newPending > lastPendingAlertCount) {
                            // ⚠️ New pending alert
                            playWarningVibration()
                        }
                        
                        lastCriticalAlertCount = newCritical
                        lastPendingAlertCount = newPending
                        
                        updateDashboard(dashboard)
                    }
                }
            }
            override fun onFailure(call: Call<ApiResponse<MonitoringDashboard>>, t: Throwable) {
                // Silent fail - không show error
            }
        })
        loadBlockedIPCount()
    }
    
    // 📋 Load Alerts Silent
    private fun loadAlertsSilent() {
        if (currentPage != 0) return // Chỉ refresh page đầu
        
        apiService.getMonitoringAlerts(status = currentStatusFilter, page = 0, size = 20)
            .enqueue(object : Callback<ApiResponse<PageResponse<MonitoringAlert>>> {
                override fun onResponse(call: Call<ApiResponse<PageResponse<MonitoringAlert>>>, response: Response<ApiResponse<PageResponse<MonitoringAlert>>>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val alerts = response.body()?.data?.content ?: emptyList()
                        alertAdapter?.updateItems(alerts)
                    }
                }
                override fun onFailure(call: Call<ApiResponse<PageResponse<MonitoringAlert>>>, t: Throwable) {}
            })
    }
    
    // 📋 Load Activities Silent
    private fun loadActivitiesSilent() {
        if (currentPage != 0) return
        
        apiService.getActivityLogs(riskLevel = currentRiskFilter, page = 0, size = 20)
            .enqueue(object : Callback<ApiResponse<PageResponse<UserActivityLog>>> {
                override fun onResponse(call: Call<ApiResponse<PageResponse<UserActivityLog>>>, response: Response<ApiResponse<PageResponse<UserActivityLog>>>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val activities = response.body()?.data?.content ?: emptyList()
                        activityLogAdapter?.updateItems(activities)
                    }
                }
                override fun onFailure(call: Call<ApiResponse<PageResponse<UserActivityLog>>>, t: Throwable) {}
            })
    }
    
    // 📋 Load Risk Scores Silent
    private fun loadRiskScoresSilent() {
        if (currentPage != 0) return
        
        apiService.getRiskScores(riskLevel = currentRiskFilter, page = 0, size = 20)
            .enqueue(object : Callback<ApiResponse<PageResponse<UserRiskScore>>> {
                override fun onResponse(call: Call<ApiResponse<PageResponse<UserRiskScore>>>, response: Response<ApiResponse<PageResponse<UserRiskScore>>>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val scores = response.body()?.data?.content ?: emptyList()
                        riskScoreAdapter?.updateItems(scores)
                    }
                }
                override fun onFailure(call: Call<ApiResponse<PageResponse<UserRiskScore>>>, t: Throwable) {}
            })
    }
    
    // 🔊 Play Critical Alert Sound + Vibration
    private fun playCriticalAlert() {
        // Vibration pattern: long-short-long
        vibrator?.let {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500), -1))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(longArrayOf(0, 500, 200, 500), -1)
            }
        }
        
        // Play notification sound
        try {
            val notification = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = android.media.RingtoneManager.getRingtone(this, notification)
            ringtone?.play()
        } catch (e: Exception) {
            // Ignore sound errors
        }
    }
    
    // 📳 Play Warning Vibration only
    private fun playWarningVibration() {
        vibrator?.let {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(200)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        isAutoRefreshEnabled = true
        startAutoRefresh()
    }
    
    override fun onPause() {
        super.onPause()
        isAutoRefreshEnabled = false
        stopAutoRefresh()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopAutoRefresh()
        soundPool?.release()
        soundPool = null
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnBlockIP = findViewById(R.id.btnBlockIP)
        btnRefresh = findViewById(R.id.btnRefresh)
        tabLayout = findViewById(R.id.tabLayout)
        scrollFilter = findViewById(R.id.scrollFilter)
        chipGroupFilter = findViewById(R.id.chipGroupFilter)
        scrollDashboard = findViewById(R.id.scrollDashboard)
        frameContent = findViewById(R.id.frameContent)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        tvEmpty = findViewById(R.id.tvEmpty)
        fabAction = findViewById(R.id.fabAction)

        tvPendingAlerts = findViewById(R.id.tvPendingAlerts)
        tvCriticalAlerts = findViewById(R.id.tvCriticalAlerts)
        tvSuspiciousUsers = findViewById(R.id.tvSuspiciousUsers)
        tvBlockedIPs = findViewById(R.id.tvBlockedIPs)
        tvActivities24h = findViewById(R.id.tvActivities24h)
        cardBlockedIPs = findViewById(R.id.cardBlockedIPs)
        rvRecentAlerts = findViewById(R.id.rvRecentAlerts)
        rvTopRiskyUsers = findViewById(R.id.rvTopRiskyUsers)
        rvRecentActivities = findViewById(R.id.rvRecentActivities)

        rvRecentAlerts.layoutManager = LinearLayoutManager(this)
        rvTopRiskyUsers.layoutManager = LinearLayoutManager(this)
        rvRecentActivities.layoutManager = LinearLayoutManager(this)
    }

    private fun setupTabLayout() {
        tabLayout.addTab(tabLayout.newTab().setText("Tổng quan"))
        tabLayout.addTab(tabLayout.newTab().setText("Cảnh báo"))
        tabLayout.addTab(tabLayout.newTab().setText("Hoạt động"))
        tabLayout.addTab(tabLayout.newTab().setText("Rủi ro"))
        tabLayout.addTab(tabLayout.newTab().setText("Block IP"))
        tabLayout.addTab(tabLayout.newTab().setText("Whitelist"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                currentPage = 0
                hasMoreData = true
                onTabChanged()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {
                currentPage = 0
                hasMoreData = true
                onTabChanged()
            }
        })
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)
                val lm = rv.layoutManager as LinearLayoutManager
                val visibleCount = lm.childCount
                val totalCount = lm.itemCount
                val firstVisible = lm.findFirstVisibleItemPosition()
                if (!isLoading && hasMoreData && (visibleCount + firstVisible) >= totalCount - 5) {
                    loadMoreData()
                }
            }
        })
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.orange)
        swipeRefresh.setOnRefreshListener {
            currentPage = 0
            hasMoreData = true
            onTabChanged()
        }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }
        btnBlockIP.setOnClickListener { showBlockIPMenu() }
        btnRefresh.setOnClickListener { currentPage = 0; hasMoreData = true; onTabChanged() }
        cardBlockedIPs.setOnClickListener { tabLayout.getTabAt(4)?.select() }
        fabAction.setOnClickListener { 
            when (currentTab) {
                4 -> showAddBlockIPDialog()
                5 -> showAddWhitelistDialog()
            }
        }
    }

    private fun onTabChanged() {
        when (currentTab) {
            0 -> {
                scrollDashboard.visibility = View.VISIBLE
                frameContent.visibility = View.GONE
                scrollFilter.visibility = View.GONE
                fabAction.visibility = View.GONE
                loadDashboard()
            }
            1 -> {
                scrollDashboard.visibility = View.GONE
                frameContent.visibility = View.VISIBLE
                scrollFilter.visibility = View.VISIBLE
                fabAction.visibility = View.GONE
                setupAlertFilters()
                loadAlerts()
            }
            2 -> {
                scrollDashboard.visibility = View.GONE
                frameContent.visibility = View.VISIBLE
                scrollFilter.visibility = View.VISIBLE
                fabAction.visibility = View.GONE
                setupActivityFilters()
                loadActivities()
            }
            3 -> {
                scrollDashboard.visibility = View.GONE
                frameContent.visibility = View.VISIBLE
                scrollFilter.visibility = View.VISIBLE
                fabAction.visibility = View.GONE
                setupRiskFilters()
                loadRiskScores()
            }
            4 -> {
                scrollDashboard.visibility = View.GONE
                frameContent.visibility = View.VISIBLE
                scrollFilter.visibility = View.VISIBLE
                fabAction.visibility = View.VISIBLE
                fabAction.setImageResource(R.drawable.ic_add)
                setupBlockedIPFilters()
                loadBlockedIPs()
            }
            5 -> {
                scrollDashboard.visibility = View.GONE
                frameContent.visibility = View.VISIBLE
                scrollFilter.visibility = View.GONE
                fabAction.visibility = View.VISIBLE
                fabAction.setImageResource(R.drawable.ic_add)
                loadWhitelistIPs()
            }
        }
    }

    private fun setupAlertFilters() {
        chipGroupFilter.removeAllViews()
        addFilterChip("Tất cả", null, true)
        addFilterChip("Chờ xử lý", "PENDING", false)
        addFilterChip("Đã xử lý", "RESOLVED", false)
        chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            currentStatusFilter = if (checkedIds.isEmpty()) null
            else (findViewById<Chip>(checkedIds.first())?.tag as? String)
            currentPage = 0; hasMoreData = true; loadAlerts()
        }
    }

    private fun setupActivityFilters() {
        chipGroupFilter.removeAllViews()
        addFilterChip("Tất cả", null, true)
        addFilterChip("Bình thường", "NORMAL", false)
        addFilterChip("Cảnh báo", "WARNING", false)
        addFilterChip("Đáng ngờ", "SUSPICIOUS", false)
        chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            currentRiskFilter = if (checkedIds.isEmpty()) null
            else (findViewById<Chip>(checkedIds.first())?.tag as? String)
            currentPage = 0; hasMoreData = true; loadActivities()
        }
    }

    private fun setupRiskFilters() {
        chipGroupFilter.removeAllViews()
        addFilterChip("Tất cả", null, true)
        addFilterChip("Bình thường", "NORMAL", false)
        addFilterChip("Cảnh báo", "WARNING", false)
        addFilterChip("Đáng ngờ", "SUSPICIOUS", false)
        chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            currentRiskFilter = if (checkedIds.isEmpty()) null
            else (findViewById<Chip>(checkedIds.first())?.tag as? String)
            currentPage = 0; hasMoreData = true; loadRiskScores()
        }
    }

    private fun setupBlockedIPFilters() {
        chipGroupFilter.removeAllViews()
        addFilterChip("Đang chặn", "ACTIVE", true)
        addFilterChip("Tất cả", "ALL", false)
        chipGroupFilter.setOnCheckedStateChangeListener { _, _ ->
            currentPage = 0; hasMoreData = true; loadBlockedIPs()
        }
    }

    private fun addFilterChip(text: String, tag: String?, checked: Boolean) {
        val chip = Chip(this).apply {
            this.text = text
            this.tag = tag
            isCheckable = true
            isChecked = checked
            setChipBackgroundColorResource(R.color.chip_background)
        }
        chipGroupFilter.addView(chip)
    }

    private fun showBlockIPMenu() {
        val options = arrayOf("Block IP mới", "Xem danh sách IP", "Tìm kiếm IP", "Thống kê")
        AlertDialog.Builder(this)
            .setTitle("Quản lý Block IP")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAddBlockIPDialog()
                    1 -> tabLayout.getTabAt(4)?.select()
                    2 -> showSearchIPDialog()
                    3 -> showIPStatistics()
                }
            }
            .setNegativeButton("Đóng", null)
            .show()
    }

    private fun showAddBlockIPDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }
        val etIP = EditText(this).apply { hint = "Nhập địa chỉ IP" }
        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@UserMonitoringActivity, android.R.layout.simple_spinner_dropdown_item,
                arrayOf("Tạm thời (1 giờ)", "Tạm thời (24 giờ)", "Vĩnh viễn"))
        }
        val etReason = EditText(this).apply { hint = "Lý do block"; minLines = 2 }

        layout.addView(etIP)
        layout.addView(TextView(this).apply { text = "Loại:"; setPadding(0, 16, 0, 8) })
        layout.addView(spinner)
        layout.addView(TextView(this).apply { text = "Lý do:"; setPadding(0, 16, 0, 8) })
        layout.addView(etReason)

        AlertDialog.Builder(this)
            .setTitle("Block IP mới")
            .setView(layout)
            .setPositiveButton("Block") { _, _ ->
                val ip = etIP.text.toString().trim()
                if (ip.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập IP", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val (type, hours) = when (spinner.selectedItemPosition) {
                    0 -> "TEMPORARY" to 1
                    1 -> "TEMPORARY" to 24
                    else -> "PERMANENT" to null
                }
                executeBlockIP(ip, type, etReason.text.toString().ifEmpty { "Admin block" }, hours)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showSearchIPDialog() {
        val etIP = EditText(this).apply { hint = "Nhập IP cần tìm"; setPadding(48, 32, 48, 32) }
        AlertDialog.Builder(this)
            .setTitle("Tìm kiếm IP")
            .setView(etIP)
            .setPositiveButton("Tìm") { _, _ ->
                val ip = etIP.text.toString().trim()
                if (ip.isNotEmpty()) searchBlockedIP(ip)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showIPStatistics() {
        apiService.getBlockedIPStatistics().enqueue(object : Callback<ApiResponse<Map<String, Any>>> {
            override fun onResponse(call: Call<ApiResponse<Map<String, Any>>>, response: Response<ApiResponse<Map<String, Any>>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val stats = response.body()?.data
                    val active = (stats?.get("totalActive") as? Number)?.toLong() ?: 0
                    val total = (stats?.get("total") as? Number)?.toLong() ?: 0
                    AlertDialog.Builder(this@UserMonitoringActivity)
                        .setTitle("Thống kê IP Blocking")
                        .setMessage("IP đang bị chặn: $active\nTổng số lượt block: $total")
                        .setPositiveButton("Đóng", null).show()
                }
            }
            override fun onFailure(call: Call<ApiResponse<Map<String, Any>>>, t: Throwable) {
                Toast.makeText(this@UserMonitoringActivity, "Lỗi: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun searchBlockedIP(ip: String) {
        apiService.searchBlockedIP(ip).enqueue(object : Callback<ApiResponse<List<BlockedIP>>> {
            override fun onResponse(call: Call<ApiResponse<List<BlockedIP>>>, response: Response<ApiResponse<List<BlockedIP>>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val results = response.body()?.data ?: emptyList()
                    if (results.isEmpty()) {
                        Toast.makeText(this@UserMonitoringActivity, "Không tìm thấy IP", Toast.LENGTH_SHORT).show()
                    } else {
                        val sb = StringBuilder()
                        results.forEach { sb.append("${it.ipAddress} - ${it.getStatusText()}\n") }
                        AlertDialog.Builder(this@UserMonitoringActivity)
                            .setTitle("Kết quả (${results.size})")
                            .setMessage(sb.toString())
                            .setPositiveButton("Đóng", null).show()
                    }
                }
            }
            override fun onFailure(call: Call<ApiResponse<List<BlockedIP>>>, t: Throwable) {
                Toast.makeText(this@UserMonitoringActivity, "Lỗi: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun executeBlockIP(ip: String, blockType: String, reason: String, hours: Int?) {
        val request = BlockIPRequest(ip, blockType, reason, hours, null, null)
        apiService.blockIP(request).enqueue(object : Callback<ApiResponse<BlockedIP>> {
            override fun onResponse(call: Call<ApiResponse<BlockedIP>>, response: Response<ApiResponse<BlockedIP>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@UserMonitoringActivity, "Đã block IP: $ip", Toast.LENGTH_SHORT).show()
                    if (currentTab == 4) { currentPage = 0; loadBlockedIPs() }
                    loadBlockedIPCount()
                } else {
                    Toast.makeText(this@UserMonitoringActivity, response.body()?.message ?: "Lỗi", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ApiResponse<BlockedIP>>, t: Throwable) {
                Toast.makeText(this@UserMonitoringActivity, "Lỗi: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun executeUnblockIP(blockedIP: BlockedIP) {
        AlertDialog.Builder(this)
            .setTitle("Gỡ chặn IP")
            .setMessage("Gỡ chặn IP: ${blockedIP.ipAddress}?")
            .setPositiveButton("Gỡ chặn") { _, _ ->
                apiService.unblockIP(blockedIP.id, mapOf("reason" to "Admin unblock")).enqueue(object : Callback<ApiResponse<BlockedIP>> {
                    override fun onResponse(call: Call<ApiResponse<BlockedIP>>, response: Response<ApiResponse<BlockedIP>>) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            Toast.makeText(this@UserMonitoringActivity, "Đã gỡ chặn", Toast.LENGTH_SHORT).show()
                            blockedIPAdapter?.removeItem(blockedIP)
                            loadBlockedIPCount()
                        }
                    }
                    override fun onFailure(call: Call<ApiResponse<BlockedIP>>, t: Throwable) {
                        Toast.makeText(this@UserMonitoringActivity, "Lỗi: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }
            .setNegativeButton("Hủy", null).show()
    }


    private fun loadDashboard() {
        showLoading(true)
        apiService.getMonitoringDashboard().enqueue(object : Callback<ApiResponse<MonitoringDashboard>> {
            override fun onResponse(call: Call<ApiResponse<MonitoringDashboard>>, response: Response<ApiResponse<MonitoringDashboard>>) {
                showLoading(false)
                if (response.isSuccessful && response.body()?.success == true) {
                    response.body()?.data?.let { updateDashboard(it) }
                }
            }
            override fun onFailure(call: Call<ApiResponse<MonitoringDashboard>>, t: Throwable) {
                showLoading(false)
                showError("Lỗi: ${t.message}")
            }
        })
        loadBlockedIPCount()
    }

    private fun loadBlockedIPCount() {
        apiService.getBlockedIPStatistics().enqueue(object : Callback<ApiResponse<Map<String, Any>>> {
            override fun onResponse(call: Call<ApiResponse<Map<String, Any>>>, response: Response<ApiResponse<Map<String, Any>>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val active = (response.body()?.data?.get("totalActive") as? Number)?.toLong() ?: 0
                    tvBlockedIPs.text = "$active"
                }
            }
            override fun onFailure(call: Call<ApiResponse<Map<String, Any>>>, t: Throwable) {}
        })
    }

    private fun updateDashboard(dashboard: MonitoringDashboard) {
        tvPendingAlerts.text = "${dashboard.totalPendingAlerts ?: 0}"
        tvCriticalAlerts.text = "${dashboard.criticalAlerts ?: 0}"
        tvSuspiciousUsers.text = "${(dashboard.suspiciousUsers ?: 0) + (dashboard.criticalUsers ?: 0)}"
        tvActivities24h.text = "${dashboard.totalActivities24h ?: 0} hoạt động"

        if ((dashboard.criticalAlerts ?: 0) > 0) tvCriticalAlerts.setTextColor(getColor(R.color.red))
        if ((dashboard.suspiciousUsers ?: 0) > 0) tvSuspiciousUsers.setTextColor(getColor(R.color.warning))

        dashboard.recentAlerts?.let { alerts ->
            rvRecentAlerts.adapter = MonitoringAlertAdapter(alerts.take(5).toMutableList(), { showAlertDetail(it) }, null)
        }
        dashboard.topRiskyUsers?.let { users ->
            rvTopRiskyUsers.adapter = RiskScoreAdapter(users.take(5).toMutableList()) { showRiskScoreDetail(it) }
        }
        dashboard.recentActivities?.let { activities ->
            rvRecentActivities.adapter = ActivityLogAdapter(activities.take(5).toMutableList(), { showActivityDetail(it) }, null)
        }
    }

    private fun loadAlerts() {
        if (isLoading) return
        isLoading = true
        showLoading(currentPage == 0)

        apiService.getMonitoringAlerts(status = currentStatusFilter, page = currentPage, size = 20)
            .enqueue(object : Callback<ApiResponse<PageResponse<MonitoringAlert>>> {
                override fun onResponse(call: Call<ApiResponse<PageResponse<MonitoringAlert>>>, response: Response<ApiResponse<PageResponse<MonitoringAlert>>>) {
                    isLoading = false
                    showLoading(false)
                    swipeRefresh.isRefreshing = false
                    if (response.isSuccessful && response.body()?.success == true) {
                        val pageData = response.body()?.data
                        val alerts = pageData?.content ?: emptyList()
                        if (currentPage == 0) {
                            alertAdapter = MonitoringAlertAdapter(alerts.toMutableList(), { showAlertDetail(it) }, { alert, action -> handleQuickAction(alert, action) })
                            recyclerView.adapter = alertAdapter
                        } else alertAdapter?.addItems(alerts)
                        hasMoreData = pageData?.isLast != true
                        showEmpty(alerts.isEmpty() && currentPage == 0, "Không có cảnh báo")
                    }
                }
                override fun onFailure(call: Call<ApiResponse<PageResponse<MonitoringAlert>>>, t: Throwable) {
                    isLoading = false; showLoading(false); swipeRefresh.isRefreshing = false
                    showError("Lỗi: ${t.message}")
                }
            })
    }

    private fun loadActivities() {
        if (isLoading) return
        isLoading = true
        showLoading(currentPage == 0)

        apiService.getActivityLogs(riskLevel = currentRiskFilter, page = currentPage, size = 20)
            .enqueue(object : Callback<ApiResponse<PageResponse<UserActivityLog>>> {
                override fun onResponse(call: Call<ApiResponse<PageResponse<UserActivityLog>>>, response: Response<ApiResponse<PageResponse<UserActivityLog>>>) {
                    isLoading = false; showLoading(false); swipeRefresh.isRefreshing = false
                    if (response.isSuccessful && response.body()?.success == true) {
                        val pageData = response.body()?.data
                        val activities = pageData?.content ?: emptyList()
                        if (currentPage == 0) {
                            activityLogAdapter = ActivityLogAdapter(
                                activities.toMutableList(), 
                                { showActivityDetail(it) },
                                { log -> 
                                    // Block IP nhanh từ nút trong item
                                    if (!log.ipAddress.isNullOrEmpty()) {
                                        quickBlockIP(log.ipAddress, log.userId, log.username)
                                    }
                                },
                                { log ->
                                    // Whitelist IP nhanh từ nút trong item
                                    if (!log.ipAddress.isNullOrEmpty()) {
                                        quickWhitelistIP(log.ipAddress, log.userId, log.username)
                                    }
                                }
                            )
                            recyclerView.adapter = activityLogAdapter
                        } else activityLogAdapter?.addItems(activities)
                        hasMoreData = pageData?.isLast != true
                        showEmpty(activities.isEmpty() && currentPage == 0, "Không có hoạt động")
                    }
                }
                override fun onFailure(call: Call<ApiResponse<PageResponse<UserActivityLog>>>, t: Throwable) {
                    isLoading = false; showLoading(false); swipeRefresh.isRefreshing = false
                    showError("Lỗi: ${t.message}")
                }
            })
    }

    private fun loadRiskScores() {
        if (isLoading) return
        isLoading = true
        showLoading(currentPage == 0)

        apiService.getRiskScores(riskLevel = currentRiskFilter, page = currentPage, size = 20)
            .enqueue(object : Callback<ApiResponse<PageResponse<UserRiskScore>>> {
                override fun onResponse(call: Call<ApiResponse<PageResponse<UserRiskScore>>>, response: Response<ApiResponse<PageResponse<UserRiskScore>>>) {
                    isLoading = false; showLoading(false); swipeRefresh.isRefreshing = false
                    if (response.isSuccessful && response.body()?.success == true) {
                        val pageData = response.body()?.data
                        val scores = pageData?.content ?: emptyList()
                        if (currentPage == 0) {
                            riskScoreAdapter = RiskScoreAdapter(scores.toMutableList()) { showRiskScoreDetail(it) }
                            recyclerView.adapter = riskScoreAdapter
                        } else riskScoreAdapter?.addItems(scores)
                        hasMoreData = pageData?.isLast != true
                        showEmpty(scores.isEmpty() && currentPage == 0, "Không có dữ liệu")
                    }
                }
                override fun onFailure(call: Call<ApiResponse<PageResponse<UserRiskScore>>>, t: Throwable) {
                    isLoading = false; showLoading(false); swipeRefresh.isRefreshing = false
                    showError("Lỗi: ${t.message}")
                }
            })
    }

    private fun loadBlockedIPs() {
        if (isLoading) return
        isLoading = true
        showLoading(currentPage == 0)

        val checkedChipId = chipGroupFilter.checkedChipId
        val showAll = if (checkedChipId != View.NO_ID) findViewById<Chip>(checkedChipId)?.tag == "ALL" else false
        val call = if (showAll) apiService.getAllBlockedIPs(currentPage, 20) else apiService.getActiveBlockedIPs(currentPage, 20)

        call.enqueue(object : Callback<ApiResponse<PageResponse<BlockedIP>>> {
            override fun onResponse(call: Call<ApiResponse<PageResponse<BlockedIP>>>, response: Response<ApiResponse<PageResponse<BlockedIP>>>) {
                isLoading = false; showLoading(false); swipeRefresh.isRefreshing = false
                if (response.isSuccessful && response.body()?.success == true) {
                    val pageData = response.body()?.data
                    val ips = pageData?.content ?: emptyList()
                    if (currentPage == 0) {
                        blockedIPAdapter = BlockedIPAdapter(ips.toMutableList(), { showBlockedIPDetail(it) }, { executeUnblockIP(it) })
                        recyclerView.adapter = blockedIPAdapter
                    } else blockedIPAdapter?.addItems(ips)
                    hasMoreData = pageData?.isLast != true
                    showEmpty(ips.isEmpty() && currentPage == 0, "Không có IP bị chặn")
                }
            }
            override fun onFailure(call: Call<ApiResponse<PageResponse<BlockedIP>>>, t: Throwable) {
                isLoading = false; showLoading(false); swipeRefresh.isRefreshing = false
                showError("Lỗi: ${t.message}")
            }
        })
    }

    private fun loadMoreData() {
        currentPage++
        when (currentTab) {
            1 -> loadAlerts()
            2 -> loadActivities()
            3 -> loadRiskScores()
            4 -> loadBlockedIPs()
        }
    }

    private fun showAlertDetail(alert: MonitoringAlert) {
        val options = arrayOf(
            "✅ Đánh dấu đã xử lý",
            "🚫 Block User",
            "⚡ Block IP ngay (24h)",
            "🔒 Block IP (chọn thời gian)",
            "🗑️ Xóa User",
            "❌ Bỏ qua"
        )
        
        AlertDialog.Builder(this)
            .setTitle(alert.title)
            .setMessage("👤 User: ${alert.targetUsername}\n📧 Email: ${alert.targetUserEmail ?: "N/A"}\n🌐 IP: ${alert.ipAddress ?: "N/A"}\n\n${alert.message}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> handleAlert(alert.id, "RESOLVED", "NONE")
                    1 -> alert.targetUserId?.let { showBlockUserDialog(it, alert.targetUsername ?: "User") }
                    2 -> {
                        // Block IP ngay 24h
                        val ip = alert.ipAddress
                        if (!ip.isNullOrEmpty()) {
                            quickBlockIP(ip, alert.targetUserId, alert.targetUsername)
                        } else {
                            Toast.makeText(this, "Không có IP để block", Toast.LENGTH_SHORT).show()
                        }
                    }
                    3 -> showBlockIPDialogForUser(alert.targetUserId, alert.targetUsername)
                    4 -> alert.targetUserId?.let { showDeleteUserDialog(it, alert.targetUsername ?: "User") }
                    5 -> handleAlert(alert.id, "DISMISSED", "FALSE_POSITIVE")
                }
            }
            .setNegativeButton("Đóng", null)
            .show()
    }

    private fun showActivityDetail(log: UserActivityLog) {
        val options = mutableListOf<String>()
        if (log.userId != null) {
            options.add("🚫 Block User")
            options.add("🗑️ Xóa User")
        }
        if (!log.ipAddress.isNullOrEmpty()) {
            options.add("⚡ Block IP ngay (24h)")  // Block nhanh 24h
            options.add("🔒 Block IP (chọn thời gian)")  // Chọn thời gian
        }
        options.add("📋 Xem chi tiết đầy đủ")
        
        val message = "👤 User: ${log.userFullName ?: log.username ?: "N/A"}\n" +
            "📧 Email: ${log.userEmail ?: "N/A"}\n" +
            "📋 Loại: ${log.activityTypeDisplay ?: log.activityType}\n" +
            "📝 Mô tả: ${log.description ?: "N/A"}\n" +
            "⚠️ Mức rủi ro: ${log.riskLevelDisplay ?: log.riskLevel}\n" +
            "🌐 IP: ${log.ipAddress ?: "N/A"}"
        
        AlertDialog.Builder(this)
            .setTitle("Chi tiết hoạt động")
            .setMessage(message)
            .setItems(options.toTypedArray()) { _, which ->
                var idx = 0
                if (log.userId != null) {
                    if (which == idx) { showBlockUserDialog(log.userId, log.username ?: "User"); return@setItems }
                    idx++
                    if (which == idx) { showDeleteUserDialog(log.userId, log.username ?: "User"); return@setItems }
                    idx++
                }
                if (!log.ipAddress.isNullOrEmpty()) {
                    if (which == idx) { 
                        // Block IP ngay 24h - không cần confirm
                        quickBlockIP(log.ipAddress, log.userId, log.username)
                        return@setItems 
                    }
                    idx++
                    if (which == idx) { 
                        showBlockIPDialogWithIP(log.ipAddress, log.userId, log.username)
                        return@setItems 
                    }
                    idx++
                }
                if (which == idx) showFullActivityDetail(log)
            }
            .setNegativeButton("Đóng", null)
            .show()
    }
    
    /**
     * ⚡ Block IP nhanh 24h - không cần confirm
     */
    private fun quickBlockIP(ip: String, userId: Long?, username: String?) {
        val reason = "Block nhanh từ Activity Log - User: ${username ?: "N/A"}"
        executeBlockIP(ip, "TEMPORARY", reason, 24)
    }

    /**
     * ✅ Whitelist IP nhanh - không cần confirm
     */
    private fun quickWhitelistIP(ip: String, userId: Long?, username: String?) {
        val description = "Whitelist nhanh từ Activity Log - User: ${username ?: "N/A"}"
        addToWhitelist(ip, description)
    }

    private fun showRiskScoreDetail(score: UserRiskScore) {
        val options = arrayOf("🚫 Block User", "⚡ Block IP ngay (24h)", "🔒 Block IP (chọn thời gian)", "🔄 Reset điểm rủi ro", "🗑️ Xóa User")
        
        val message = "👤 User: ${score.userFullName ?: score.username ?: "N/A"}\n" +
            "📧 Email: ${score.userEmail ?: "N/A"}\n" +
            "🌐 IP gần nhất: ${score.lastIpAddress ?: "N/A"}\n" +
            "📊 Điểm rủi ro: ${score.totalScore}/100\n" +
            "⚠️ Mức độ: ${score.riskLevelDisplay ?: score.riskLevel}\n\n" +
            "📈 Chi tiết:\n" +
            "• Đăng nhập thất bại: ${score.loginFailedCount ?: 0}\n" +
            "• Hủy đơn: ${score.orderCancelCount ?: 0}\n" +
            "• Thanh toán thất bại: ${score.paymentFailedCount ?: 0}\n" +
            "• Spam request: ${score.spamRequestCount ?: 0}"
        
        AlertDialog.Builder(this)
            .setTitle("${score.getRiskEmoji()} Điểm rủi ro: ${score.totalScore}/100")
            .setMessage(message)
            .setItems(options) { _, which ->
                score.userId?.let { userId ->
                    when (which) {
                        0 -> showBlockUserDialog(userId, score.username ?: "User")
                        1 -> {
                            // Block IP ngay 24h
                            val ip = score.lastIpAddress
                            if (!ip.isNullOrEmpty()) {
                                quickBlockIP(ip, userId, score.username)
                            } else {
                                Toast.makeText(this, "Không có IP để block", Toast.LENGTH_SHORT).show()
                            }
                        }
                        2 -> showBlockIPDialogForUser(userId, score.username)
                        3 -> resetUserRiskScore(userId)
                        4 -> showDeleteUserDialog(userId, score.username ?: "User")
                    }
                }
            }
            .setNegativeButton("Đóng", null)
            .show()
    }

    private fun showFullActivityDetail(log: UserActivityLog) {
        val message = "👤 User: ${log.userFullName ?: log.username ?: "N/A"}\n" +
            "📧 Email: ${log.userEmail ?: "N/A"}\n" +
            "🆔 User ID: ${log.userId ?: "N/A"}\n\n" +
            "📋 Loại: ${log.activityTypeDisplay ?: log.activityType}\n" +
            "📝 Mô tả: ${log.description ?: "N/A"}\n" +
            "⚠️ Mức rủi ro: ${log.riskLevelDisplay ?: log.riskLevel}\n\n" +
            "🌐 IP: ${log.ipAddress ?: "N/A"}\n" +
            "📱 Thiết bị: ${log.deviceInfo ?: "N/A"}\n" +
            "🔗 Endpoint: ${log.endpoint ?: "N/A"}\n" +
            "🕐 Thời gian: ${log.createdAt?.replace("T", " ")?.take(19) ?: "N/A"}"
        
        AlertDialog.Builder(this)
            .setTitle("Chi tiết đầy đủ")
            .setMessage(message)
            .setPositiveButton("Đóng", null)
            .show()
    }

    private fun showBlockUserDialog(userId: Long, username: String) {
        AlertDialog.Builder(this)
            .setTitle("🚫 Block User")
            .setMessage("Bạn có chắc muốn khóa tài khoản: $username?")
            .setPositiveButton("Block") { _, _ -> blockUser(userId) }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showDeleteUserDialog(userId: Long, username: String) {
        AlertDialog.Builder(this)
            .setTitle("🗑️ Xóa User")
            .setMessage("⚠️ CẢNH BÁO: Không thể hoàn tác!\n\nXóa user: $username?")
            .setPositiveButton("Xóa") { _, _ -> deleteUser(userId) }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showBlockIPDialogForUser(userId: Long?, username: String?) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }
        val etIP = EditText(this).apply { hint = "Nhập địa chỉ IP cần block" }
        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@UserMonitoringActivity, android.R.layout.simple_spinner_dropdown_item,
                arrayOf("Tạm thời (1 giờ)", "Tạm thời (24 giờ)", "Vĩnh viễn"))
        }
        layout.addView(TextView(this).apply { text = "User: ${username ?: "N/A"}"; setPadding(0, 0, 0, 16) })
        layout.addView(etIP)
        layout.addView(TextView(this).apply { text = "Loại:"; setPadding(0, 16, 0, 8) })
        layout.addView(spinner)
        
        AlertDialog.Builder(this)
            .setTitle("🔒 Block IP")
            .setView(layout)
            .setPositiveButton("Block") { _, _ ->
                val ip = etIP.text.toString().trim()
                if (ip.isEmpty()) { Toast.makeText(this, "Vui lòng nhập IP", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                val (type, hours) = when (spinner.selectedItemPosition) { 0 -> "TEMPORARY" to 1; 1 -> "TEMPORARY" to 24; else -> "PERMANENT" to null }
                executeBlockIP(ip, type, "Block từ user: $username", hours)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showBlockIPDialogWithIP(ip: String, userId: Long?, username: String?) {
        val options = arrayOf("Tạm thời (1 giờ)", "Tạm thời (24 giờ)", "Vĩnh viễn")
        AlertDialog.Builder(this)
            .setTitle("🔒 Block IP: $ip")
            .setMessage("User: ${username ?: "N/A"}\n\nChọn thời gian block:")
            .setItems(options) { _, which ->
                val (type, hours) = when (which) { 0 -> "TEMPORARY" to 1; 1 -> "TEMPORARY" to 24; else -> "PERMANENT" to null }
                executeBlockIP(ip, type, "Block từ activity - User: $username", hours)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun blockUser(userId: Long) {
        apiService.toggleUserBlock(userId.toInt(), true).enqueue(object : Callback<ApiResponse<User>> {
            override fun onResponse(call: Call<ApiResponse<User>>, response: Response<ApiResponse<User>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@UserMonitoringActivity, "✅ Đã khóa user", Toast.LENGTH_SHORT).show()
                    refreshCurrentTab()
                } else Toast.makeText(this@UserMonitoringActivity, response.body()?.message ?: "Lỗi", Toast.LENGTH_SHORT).show()
            }
            override fun onFailure(call: Call<ApiResponse<User>>, t: Throwable) {
                Toast.makeText(this@UserMonitoringActivity, "Lỗi: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun deleteUser(userId: Long) {
        apiService.deleteUser(userId.toInt()).enqueue(object : Callback<ApiResponse<String>> {
            override fun onResponse(call: Call<ApiResponse<String>>, response: Response<ApiResponse<String>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@UserMonitoringActivity, "✅ Đã xóa user", Toast.LENGTH_SHORT).show()
                    refreshCurrentTab()
                } else Toast.makeText(this@UserMonitoringActivity, response.body()?.message ?: "Lỗi", Toast.LENGTH_SHORT).show()
            }
            override fun onFailure(call: Call<ApiResponse<String>>, t: Throwable) {
                Toast.makeText(this@UserMonitoringActivity, "Lỗi: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun resetUserRiskScore(userId: Long) {
        apiService.resetRiskScore(userId).enqueue(object : Callback<ApiResponse<UserRiskScore>> {
            override fun onResponse(call: Call<ApiResponse<UserRiskScore>>, response: Response<ApiResponse<UserRiskScore>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@UserMonitoringActivity, "✅ Đã reset điểm rủi ro", Toast.LENGTH_SHORT).show()
                    refreshCurrentTab()
                } else Toast.makeText(this@UserMonitoringActivity, response.body()?.message ?: "Lỗi", Toast.LENGTH_SHORT).show()
            }
            override fun onFailure(call: Call<ApiResponse<UserRiskScore>>, t: Throwable) {
                Toast.makeText(this@UserMonitoringActivity, "Lỗi: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun refreshCurrentTab() {
        currentPage = 0
        hasMoreData = true
        onTabChanged()
    }

    private fun showBlockedIPDetail(ip: BlockedIP) {
        val builder = AlertDialog.Builder(this)
            .setTitle(ip.ipAddress)
            .setMessage("Loại: ${ip.getBlockTypeText()}\nLý do: ${ip.reason}\nBlock bởi: ${ip.blockedByUsername}\n${ip.getStatusText()}")
            .setNegativeButton("Đóng", null)
        if (ip.isActive == true) builder.setPositiveButton("Gỡ chặn") { _, _ -> executeUnblockIP(ip) }
        builder.show()
    }

    private fun handleAlert(alertId: Long, status: String, action: String) {
        apiService.handleAlert(alertId, HandleAlertRequest(status, action, null))
            .enqueue(object : Callback<ApiResponse<MonitoringAlert>> {
                override fun onResponse(call: Call<ApiResponse<MonitoringAlert>>, response: Response<ApiResponse<MonitoringAlert>>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@UserMonitoringActivity, "Đã xử lý", Toast.LENGTH_SHORT).show()
                        if (currentTab == 1) { currentPage = 0; loadAlerts() }
                    }
                }
                override fun onFailure(call: Call<ApiResponse<MonitoringAlert>>, t: Throwable) {
                    Toast.makeText(this@UserMonitoringActivity, "Lỗi: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun handleQuickAction(alert: MonitoringAlert, action: String) {
        handleAlert(alert.id, "RESOLVED", action)
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showEmpty(show: Boolean, msg: String = "Không có dữ liệu") {
        layoutEmpty.visibility = if (show) View.VISIBLE else View.GONE
        tvEmpty.text = msg
    }

    private fun showError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    // ==================== WHITELIST IP FUNCTIONS ====================
    
    private var whitelistAdapter: WhitelistIPAdapter? = null
    
    /**
     * 🔓 Load danh sách Whitelist IP
     */
    private fun loadWhitelistIPs() {
        showLoading(true)
        
        apiService.getActiveWhitelistIPs().enqueue(object : Callback<ApiResponse<List<WhitelistedIP>>> {
            override fun onResponse(
                call: Call<ApiResponse<List<WhitelistedIP>>>,
                response: Response<ApiResponse<List<WhitelistedIP>>>
            ) {
                showLoading(false)
                swipeRefresh.isRefreshing = false
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val whitelist = response.body()?.data ?: emptyList()
                    
                    whitelistAdapter = WhitelistIPAdapter(
                        whitelist.toMutableList(),
                        { showWhitelistDetail(it) },
                        { removeFromWhitelist(it) }
                    )
                    recyclerView.adapter = whitelistAdapter
                    
                    showEmpty(whitelist.isEmpty(), "Chưa có IP trong whitelist\n\nThêm IP để Admin/Manager có thể truy cập")
                } else {
                    showError(response.body()?.message ?: "Lỗi tải whitelist")
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<WhitelistedIP>>>, t: Throwable) {
                showLoading(false)
                swipeRefresh.isRefreshing = false
                showError("Lỗi: ${t.message}")
            }
        })
    }
    
    /**
     * 🔓 Hiển thị dialog thêm IP vào whitelist
     */
    private fun showAddWhitelistDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }
        
        val etIP = EditText(this).apply { 
            hint = "Nhập địa chỉ IP (VD: 123.21.109.117)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }
        val etDescription = EditText(this).apply { 
            hint = "Mô tả (VD: IP văn phòng, IP nhà...)"
            minLines = 2
        }
        
        // Thêm nút lấy IP hiện tại
        val btnGetMyIP = TextView(this).apply {
            text = "📍 Lấy IP hiện tại của tôi"
            setTextColor(getColor(R.color.wine_primary))
            setPadding(0, 16, 0, 16)
            setOnClickListener {
                getMyCurrentIP { ip ->
                    etIP.setText(ip)
                    Toast.makeText(this@UserMonitoringActivity, "IP của bạn: $ip", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        layout.addView(etIP)
        layout.addView(btnGetMyIP)
        layout.addView(TextView(this).apply { text = "Mô tả:"; setPadding(0, 8, 0, 8) })
        layout.addView(etDescription)
        
        AlertDialog.Builder(this)
            .setTitle("🔓 Thêm IP vào Whitelist")
            .setMessage("IP trong whitelist sẽ được phép truy cập với quyền Admin/Manager")
            .setView(layout)
            .setPositiveButton("Thêm") { _, _ ->
                val ip = etIP.text.toString().trim()
                val description = etDescription.text.toString().trim()
                
                if (ip.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập IP", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                addToWhitelist(ip, description.ifEmpty { "Thêm từ app" })
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
    
    /**
     * 🌐 Lấy IP hiện tại của thiết bị
     */
    private fun getMyCurrentIP(callback: (String) -> Unit) {
        apiService.getMyIP().enqueue(object : Callback<ApiResponse<Map<String, Any>>> {
            override fun onResponse(
                call: Call<ApiResponse<Map<String, Any>>>,
                response: Response<ApiResponse<Map<String, Any>>>
            ) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val ip = response.body()?.data?.get("yourIP") as? String ?: "N/A"
                    callback(ip)
                } else {
                    Toast.makeText(this@UserMonitoringActivity, "Không thể lấy IP", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<Map<String, Any>>>, t: Throwable) {
                Toast.makeText(this@UserMonitoringActivity, "Lỗi: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    /**
     * ➕ Thêm IP vào whitelist
     */
    private fun addToWhitelist(ipAddress: String, description: String) {
        val request = mapOf("ipAddress" to ipAddress, "description" to description)
        
        apiService.addToWhitelist(request).enqueue(object : Callback<ApiResponse<WhitelistedIP>> {
            override fun onResponse(
                call: Call<ApiResponse<WhitelistedIP>>,
                response: Response<ApiResponse<WhitelistedIP>>
            ) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@UserMonitoringActivity, "✅ Đã thêm IP vào whitelist", Toast.LENGTH_SHORT).show()
                    loadWhitelistIPs()
                } else {
                    Toast.makeText(this@UserMonitoringActivity, response.body()?.message ?: "Lỗi", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<WhitelistedIP>>, t: Throwable) {
                Toast.makeText(this@UserMonitoringActivity, "Lỗi: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    /**
     * ➖ Xóa IP khỏi whitelist
     */
    private fun removeFromWhitelist(whitelistedIP: WhitelistedIP) {
        AlertDialog.Builder(this)
            .setTitle("🔒 Xóa khỏi Whitelist")
            .setMessage("Xóa IP: ${whitelistedIP.ipAddress}?\n\nAdmin/Manager sẽ không thể truy cập từ IP này nữa.")
            .setPositiveButton("Xóa") { _, _ ->
                apiService.removeFromWhitelist(whitelistedIP.id).enqueue(object : Callback<ApiResponse<String>> {
                    override fun onResponse(
                        call: Call<ApiResponse<String>>,
                        response: Response<ApiResponse<String>>
                    ) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            Toast.makeText(this@UserMonitoringActivity, "✅ Đã xóa khỏi whitelist", Toast.LENGTH_SHORT).show()
                            whitelistAdapter?.removeItem(whitelistedIP)
                        } else {
                            Toast.makeText(this@UserMonitoringActivity, response.body()?.message ?: "Lỗi", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<String>>, t: Throwable) {
                        Toast.makeText(this@UserMonitoringActivity, "Lỗi: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
    
    /**
     * 📋 Hiển thị chi tiết whitelist IP
     */
    private fun showWhitelistDetail(whitelistedIP: WhitelistedIP) {
        val message = "🌐 IP: ${whitelistedIP.ipAddress}\n" +
            "📝 Mô tả: ${whitelistedIP.description ?: "N/A"}\n" +
            "🕐 Thêm lúc: ${whitelistedIP.createdAt?.replace("T", " ")?.take(19) ?: "N/A"}"
        
        AlertDialog.Builder(this)
            .setTitle("🔓 Chi tiết Whitelist IP")
            .setMessage(message)
            .setPositiveButton("Đóng", null)
            .setNegativeButton("Xóa") { _, _ -> removeFromWhitelist(whitelistedIP) }
            .show()
    }
}
