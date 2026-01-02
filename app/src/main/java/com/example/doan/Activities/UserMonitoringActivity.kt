package com.example.doan.Activities

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.doan.Adapters.ActivityLogAdapter
import com.example.doan.Adapters.MonitoringAlertAdapter
import com.example.doan.Adapters.RiskScoreAdapter
import com.example.doan.Models.*
import com.example.doan.Network.ApiService
import com.example.doan.Network.MonitoringWebSocketManager
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * 🛡️ USER MONITORING ACTIVITY
 * Màn hình giám sát hành vi người dùng cho Admin/Manager
 * Hỗ trợ WebSocket realtime để nhận cảnh báo tức thì
 */
class UserMonitoringActivity : AppCompatActivity() {

    // Views
    private lateinit var btnBack: ImageButton
    private lateinit var tabLayout: TabLayout
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var layoutDashboard: View
    private lateinit var chipGroupFilter: ChipGroup
    private var tvConnectionStatus: TextView? = null
    
    // Dashboard views
    private lateinit var tvPendingAlerts: TextView
    private lateinit var tvCriticalAlerts: TextView
    private lateinit var tvSuspiciousUsers: TextView
    private lateinit var tvActivities24h: TextView

    // API Service
    private lateinit var apiService: ApiService
    
    // 🔌 WebSocket Manager
    private lateinit var webSocketManager: MonitoringWebSocketManager
    private var isWebSocketConnected = false

    // Adapters
    private var activityLogAdapter: ActivityLogAdapter? = null
    private var alertAdapter: MonitoringAlertAdapter? = null
    private var riskScoreAdapter: RiskScoreAdapter? = null
    
    // Data
    private var currentTab = 0
    private var currentPage = 0
    private var isLoading = false
    private var hasMoreData = true
    private var currentRiskFilter: String? = null
    private var currentStatusFilter: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_monitoring)
        
        // Initialize API Service
        apiService = RetrofitClient.getInstance(this).apiService
        
        // 🔌 Initialize WebSocket Manager
        webSocketManager = MonitoringWebSocketManager.getInstance()
        
        initViews()
        setupTabLayout()
        setupRecyclerView()
        setupSwipeRefresh()
        setupFilters()
        
        // 🔌 Setup WebSocket
        setupWebSocket()
        
        loadDashboard()
    }
    
    override fun onResume() {
        super.onResume()
        // Reconnect WebSocket khi quay lại Activity
        connectWebSocket()
    }
    
    override fun onPause() {
        super.onPause()
        // Không disconnect khi pause để vẫn nhận notification
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Disconnect WebSocket khi đóng Activity
        webSocketManager.clearListeners()
        webSocketManager.disconnect()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tabLayout = findViewById(R.id.tabLayout)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)
        layoutDashboard = findViewById(R.id.layoutDashboard)
        chipGroupFilter = findViewById(R.id.chipGroupFilter)
        
        // Dashboard views
        tvPendingAlerts = findViewById(R.id.tvPendingAlerts)
        tvCriticalAlerts = findViewById(R.id.tvCriticalAlerts)
        tvSuspiciousUsers = findViewById(R.id.tvSuspiciousUsers)
        tvActivities24h = findViewById(R.id.tvActivities24h)
        
        btnBack.setOnClickListener { finish() }
    }

    private fun setupTabLayout() {
        tabLayout.addTab(tabLayout.newTab().setText("📊 Tổng quan"))
        tabLayout.addTab(tabLayout.newTab().setText("🚨 Cảnh báo"))
        tabLayout.addTab(tabLayout.newTab().setText("📋 Hoạt động"))
        tabLayout.addTab(tabLayout.newTab().setText("⚠️ Rủi ro"))
        
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
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                
                if (!isLoading && hasMoreData) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5
                        && firstVisibleItemPosition >= 0) {
                        loadMoreData()
                    }
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

    private fun setupFilters() {
        chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) {
                currentRiskFilter = null
                currentStatusFilter = null
            } else {
                val chip = findViewById<Chip>(checkedIds.first())
                when (chip?.tag) {
                    "NORMAL", "WARNING", "SUSPICIOUS", "CRITICAL" -> {
                        currentRiskFilter = chip.tag as String
                        currentStatusFilter = null
                    }
                    "PENDING", "RESOLVED" -> {
                        currentStatusFilter = chip.tag as String
                        currentRiskFilter = null
                    }
                }
            }
            currentPage = 0
            hasMoreData = true
            onTabChanged()
        }
    }

    private fun onTabChanged() {
        when (currentTab) {
            0 -> {
                layoutDashboard.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                chipGroupFilter.visibility = View.GONE
                loadDashboard()
            }
            1 -> {
                layoutDashboard.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                chipGroupFilter.visibility = View.VISIBLE
                setupAlertFilters()
                loadAlerts()
            }
            2 -> {
                layoutDashboard.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                chipGroupFilter.visibility = View.VISIBLE
                setupActivityFilters()
                loadActivities()
            }
            3 -> {
                layoutDashboard.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                chipGroupFilter.visibility = View.VISIBLE
                setupRiskFilters()
                loadRiskScores()
            }
        }
    }

    private fun setupAlertFilters() {
        chipGroupFilter.removeAllViews()
        addFilterChip("Chờ xử lý", "PENDING")
        addFilterChip("Đã xử lý", "RESOLVED")
    }

    private fun setupActivityFilters() {
        chipGroupFilter.removeAllViews()
        addFilterChip("🟢 Bình thường", "NORMAL")
        addFilterChip("🟡 Cảnh báo", "WARNING")
        addFilterChip("🔴 Đáng ngờ", "SUSPICIOUS")
        addFilterChip("⚫ Nghiêm trọng", "CRITICAL")
    }

    private fun setupRiskFilters() {
        chipGroupFilter.removeAllViews()
        addFilterChip("🟢 Bình thường", "NORMAL")
        addFilterChip("🟡 Cảnh báo", "WARNING")
        addFilterChip("🔴 Đáng ngờ", "SUSPICIOUS")
        addFilterChip("⚫ Nghiêm trọng", "CRITICAL")
    }

    private fun addFilterChip(text: String, tag: String) {
        val chip = Chip(this).apply {
            this.text = text
            this.tag = tag
            isCheckable = true
            setChipBackgroundColorResource(R.color.chip_background)
        }
        chipGroupFilter.addView(chip)
    }

    // ==================== LOAD DATA ====================

    private fun loadDashboard() {
        showLoading(true)
        
        apiService.getMonitoringDashboard()
            .enqueue(object : Callback<ApiResponse<MonitoringDashboard>> {
                override fun onResponse(
                    call: Call<ApiResponse<MonitoringDashboard>>,
                    response: Response<ApiResponse<MonitoringDashboard>>
                ) {
                    showLoading(false)
                    swipeRefresh.isRefreshing = false
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        response.body()?.data?.let { updateDashboard(it) }
                    } else {
                        showError("Không thể tải dữ liệu")
                    }
                }

                override fun onFailure(call: Call<ApiResponse<MonitoringDashboard>>, t: Throwable) {
                    showLoading(false)
                    swipeRefresh.isRefreshing = false
                    showError("Lỗi kết nối: ${t.message}")
                }
            })
    }

    private fun updateDashboard(dashboard: MonitoringDashboard) {
        tvPendingAlerts.text = "${dashboard.totalPendingAlerts ?: 0}"
        tvCriticalAlerts.text = "${dashboard.criticalAlerts ?: 0}"
        tvSuspiciousUsers.text = "${(dashboard.suspiciousUsers ?: 0) + (dashboard.criticalUsers ?: 0)}"
        tvActivities24h.text = "${dashboard.totalActivities24h ?: 0}"
        
        if ((dashboard.criticalAlerts ?: 0) > 0) {
            tvCriticalAlerts.setTextColor(getColor(R.color.red))
        }
        if ((dashboard.suspiciousUsers ?: 0) > 0 || (dashboard.criticalUsers ?: 0) > 0) {
            tvSuspiciousUsers.setTextColor(getColor(R.color.orange))
        }
    }

    private fun loadAlerts() {
        if (isLoading) return
        isLoading = true
        showLoading(currentPage == 0)
        
        apiService.getMonitoringAlerts(
            status = currentStatusFilter,
            page = currentPage,
            size = 20
        ).enqueue(object : Callback<ApiResponse<PageResponse<MonitoringAlert>>> {
            override fun onResponse(
                call: Call<ApiResponse<PageResponse<MonitoringAlert>>>,
                response: Response<ApiResponse<PageResponse<MonitoringAlert>>>
            ) {
                isLoading = false
                showLoading(false)
                swipeRefresh.isRefreshing = false
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val pageData = response.body()?.data
                    val alerts = pageData?.content ?: emptyList()
                    
                    if (currentPage == 0) {
                        alertAdapter = MonitoringAlertAdapter(alerts.toMutableList()) { alert ->
                            showAlertActionDialog(alert)
                        }
                        recyclerView.adapter = alertAdapter
                    } else {
                        alertAdapter?.addItems(alerts)
                    }
                    
                    hasMoreData = pageData?.isLast != true
                    showEmpty(alerts.isEmpty() && currentPage == 0)
                }
            }

            override fun onFailure(call: Call<ApiResponse<PageResponse<MonitoringAlert>>>, t: Throwable) {
                isLoading = false
                showLoading(false)
                swipeRefresh.isRefreshing = false
                showError("Lỗi: ${t.message}")
            }
        })
    }

    private fun loadActivities() {
        if (isLoading) return
        isLoading = true
        showLoading(currentPage == 0)
        
        apiService.getActivityLogs(
            riskLevel = currentRiskFilter,
            page = currentPage,
            size = 20
        ).enqueue(object : Callback<ApiResponse<PageResponse<UserActivityLog>>> {
            override fun onResponse(
                call: Call<ApiResponse<PageResponse<UserActivityLog>>>,
                response: Response<ApiResponse<PageResponse<UserActivityLog>>>
            ) {
                isLoading = false
                showLoading(false)
                swipeRefresh.isRefreshing = false
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val pageData = response.body()?.data
                    val activities = pageData?.content ?: emptyList()
                    
                    if (currentPage == 0) {
                        activityLogAdapter = ActivityLogAdapter(activities.toMutableList()) { log ->
                            showActivityDetail(log)
                        }
                        recyclerView.adapter = activityLogAdapter
                    } else {
                        activityLogAdapter?.addItems(activities)
                    }
                    
                    hasMoreData = pageData?.isLast != true
                    showEmpty(activities.isEmpty() && currentPage == 0)
                }
            }

            override fun onFailure(call: Call<ApiResponse<PageResponse<UserActivityLog>>>, t: Throwable) {
                isLoading = false
                showLoading(false)
                swipeRefresh.isRefreshing = false
                showError("Lỗi: ${t.message}")
            }
        })
    }

    private fun loadRiskScores() {
        if (isLoading) return
        isLoading = true
        showLoading(currentPage == 0)
        
        apiService.getRiskScores(
            riskLevel = currentRiskFilter,
            page = currentPage,
            size = 20
        ).enqueue(object : Callback<ApiResponse<PageResponse<UserRiskScore>>> {
            override fun onResponse(
                call: Call<ApiResponse<PageResponse<UserRiskScore>>>,
                response: Response<ApiResponse<PageResponse<UserRiskScore>>>
            ) {
                isLoading = false
                showLoading(false)
                swipeRefresh.isRefreshing = false
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val pageData = response.body()?.data
                    val scores = pageData?.content ?: emptyList()
                    
                    if (currentPage == 0) {
                        riskScoreAdapter = RiskScoreAdapter(scores.toMutableList()) { score ->
                            showRiskScoreActions(score)
                        }
                        recyclerView.adapter = riskScoreAdapter
                    } else {
                        riskScoreAdapter?.addItems(scores)
                    }
                    
                    hasMoreData = pageData?.isLast != true
                    showEmpty(scores.isEmpty() && currentPage == 0)
                }
            }

            override fun onFailure(call: Call<ApiResponse<PageResponse<UserRiskScore>>>, t: Throwable) {
                isLoading = false
                showLoading(false)
                swipeRefresh.isRefreshing = false
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
        }
    }

    // ==================== DIALOGS ====================

    private fun showAlertActionDialog(alert: MonitoringAlert) {
        val options = arrayOf(
            "✅ Đánh dấu đã xử lý",
            "🚫 Tạm khóa user",
            "⛔ Khóa vĩnh viễn",
            "👁️ Theo dõi",
            "❌ Bỏ qua (False positive)"
        )
        
        AlertDialog.Builder(this)
            .setTitle("Xử lý cảnh báo")
            .setMessage("User: ${alert.targetUsername}\n${alert.title}")
            .setItems(options) { _, which ->
                val (status, action) = when (which) {
                    0 -> "RESOLVED" to "NONE"
                    1 -> "RESOLVED" to "TEMP_BLOCKED"
                    2 -> "RESOLVED" to "PERM_BLOCKED"
                    3 -> "RESOLVED" to "MONITORED"
                    4 -> "DISMISSED" to "FALSE_POSITIVE"
                    else -> return@setItems
                }
                handleAlert(alert.id, status, action)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun handleAlert(alertId: Long, status: String, action: String) {
        val request = HandleAlertRequest(status, action, null)
        
        apiService.handleAlert(alertId, request)
            .enqueue(object : Callback<ApiResponse<MonitoringAlert>> {
                override fun onResponse(
                    call: Call<ApiResponse<MonitoringAlert>>,
                    response: Response<ApiResponse<MonitoringAlert>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@UserMonitoringActivity, 
                            "Đã xử lý cảnh báo", Toast.LENGTH_SHORT).show()
                        currentPage = 0
                        loadAlerts()
                    } else {
                        Toast.makeText(this@UserMonitoringActivity, 
                            "Lỗi xử lý", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<MonitoringAlert>>, t: Throwable) {
                    Toast.makeText(this@UserMonitoringActivity, 
                        "Lỗi: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showActivityDetail(log: UserActivityLog) {
        val message = """
            |👤 User: ${log.username ?: "Unknown"}
            |📧 Email: ${log.userEmail ?: "N/A"}
            |
            |📋 Hoạt động: ${log.activityTypeDisplay ?: log.activityType}
            |📝 Mô tả: ${log.description ?: "N/A"}
            |
            |⚠️ Mức độ: ${log.riskLevelDisplay ?: log.riskLevel}
            |
            |🌐 IP: ${log.ipAddress ?: "N/A"}
            |📱 Thiết bị: ${log.deviceInfo ?: "N/A"}
            |🔗 Endpoint: ${log.endpoint ?: "N/A"}
            |
            |🕐 Thời gian: ${log.createdAt ?: "N/A"}
        """.trimMargin()
        
        AlertDialog.Builder(this)
            .setTitle("Chi tiết hoạt động")
            .setMessage(message)
            .setPositiveButton("Đóng", null)
            .setNeutralButton("Xem user") { _, _ ->
                log.userId?.let { showUserRiskScore(it) }
            }
            .show()
    }

    private fun showRiskScoreActions(score: UserRiskScore) {
        val options = mutableListOf<String>()
        options.add("📊 Xem chi tiết")
        options.add("📝 Thêm ghi chú")
        
        if (score.userBlocked == true) {
            options.add("🔓 Mở khóa user")
        } else if (score.totalScore >= 60) {
            options.add("🔒 Khóa user")
        }
        
        options.add("🔄 Reset điểm rủi ro")
        
        AlertDialog.Builder(this)
            .setTitle("${score.getRiskEmoji()} ${score.username}")
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    "📊 Xem chi tiết" -> showRiskScoreDetail(score)
                    "📝 Thêm ghi chú" -> showAddNoteDialog(score)
                    "🔓 Mở khóa user" -> unblockUser(score.userId!!)
                    "🔒 Khóa user" -> blockUser(score.userId!!)
                    "🔄 Reset điểm rủi ro" -> resetRiskScore(score.userId!!)
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showRiskScoreDetail(score: UserRiskScore) {
        val autoBlockInfo = if (score.autoBlocked == true) 
            "\n⚠️ Tự động khóa: ${score.autoBlockedReason}" else ""
        
        val message = """
            |👤 User: ${score.username}
            |📧 Email: ${score.userEmail ?: "N/A"}
            |
            |📊 ĐIỂM RỦI RO: ${score.totalScore}/100
            |⚠️ Mức độ: ${score.riskLevelDisplay}
            |
            |📈 CHI TIẾT:
            |• Đăng nhập thất bại: ${score.loginFailedCount ?: 0}
            |• Hủy đơn hàng: ${score.orderCancelCount ?: 0}
            |• Thanh toán thất bại: ${score.paymentFailedCount ?: 0}
            |• Vượt rate limit: ${score.rateLimitHitCount ?: 0}
            |• Lạm dụng khuyến mãi: ${score.promotionAbuseCount ?: 0}
            |• Spam request: ${score.spamRequestCount ?: 0}
            |
            |🔒 Trạng thái: ${if (score.userBlocked == true) "Đã khóa" else "Hoạt động"}$autoBlockInfo
            |
            |📝 Ghi chú Admin: ${score.adminNote ?: "Chưa có"}
        """.trimMargin()
        
        AlertDialog.Builder(this)
            .setTitle("${score.getRiskEmoji()} Chi tiết rủi ro")
            .setMessage(message)
            .setPositiveButton("Đóng", null)
            .show()
    }

    private fun showAddNoteDialog(score: UserRiskScore) {
        val editText = EditText(this).apply {
            hint = "Nhập ghi chú..."
            setText(score.adminNote ?: "")
            setPadding(48, 32, 48, 32)
        }
        
        AlertDialog.Builder(this)
            .setTitle("Thêm ghi chú cho ${score.username}")
            .setView(editText)
            .setPositiveButton("Lưu") { _, _ ->
                val note = editText.text.toString()
                if (note.isNotBlank()) {
                    addAdminNote(score.userId!!, note)
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showUserRiskScore(userId: Long) {
        apiService.getUserRiskScore(userId)
            .enqueue(object : Callback<ApiResponse<UserRiskScore>> {
                override fun onResponse(
                    call: Call<ApiResponse<UserRiskScore>>,
                    response: Response<ApiResponse<UserRiskScore>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        response.body()?.data?.let { showRiskScoreDetail(it) }
                    }
                }
                override fun onFailure(call: Call<ApiResponse<UserRiskScore>>, t: Throwable) {}
            })
    }

    // ==================== API ACTIONS ====================

    private fun addAdminNote(userId: Long, note: String) {
        apiService.addAdminNote(userId, mapOf("note" to note))
            .enqueue(object : Callback<ApiResponse<UserRiskScore>> {
                override fun onResponse(
                    call: Call<ApiResponse<UserRiskScore>>,
                    response: Response<ApiResponse<UserRiskScore>>
                ) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@UserMonitoringActivity, 
                            "Đã lưu ghi chú", Toast.LENGTH_SHORT).show()
                        currentPage = 0
                        loadRiskScores()
                    }
                }
                override fun onFailure(call: Call<ApiResponse<UserRiskScore>>, t: Throwable) {
                    Toast.makeText(this@UserMonitoringActivity, 
                        "Lỗi: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun resetRiskScore(userId: Long) {
        AlertDialog.Builder(this)
            .setTitle("Reset điểm rủi ro")
            .setMessage("Bạn có chắc muốn reset điểm rủi ro của user này về 0?")
            .setPositiveButton("Reset") { _, _ ->
                apiService.resetRiskScore(userId)
                    .enqueue(object : Callback<ApiResponse<UserRiskScore>> {
                        override fun onResponse(
                            call: Call<ApiResponse<UserRiskScore>>,
                            response: Response<ApiResponse<UserRiskScore>>
                        ) {
                            if (response.isSuccessful) {
                                Toast.makeText(this@UserMonitoringActivity, 
                                    "Đã reset điểm rủi ro", Toast.LENGTH_SHORT).show()
                                currentPage = 0
                                loadRiskScores()
                            }
                        }
                        override fun onFailure(call: Call<ApiResponse<UserRiskScore>>, t: Throwable) {
                            Toast.makeText(this@UserMonitoringActivity, 
                                "Lỗi: ${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun unblockUser(userId: Long) {
        apiService.unblockUser(userId, mapOf("reason" to "Admin unblock"))
            .enqueue(object : Callback<ApiResponse<String>> {
                override fun onResponse(
                    call: Call<ApiResponse<String>>,
                    response: Response<ApiResponse<String>>
                ) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@UserMonitoringActivity, 
                            "Đã mở khóa user", Toast.LENGTH_SHORT).show()
                        currentPage = 0
                        loadRiskScores()
                    }
                }
                override fun onFailure(call: Call<ApiResponse<String>>, t: Throwable) {
                    Toast.makeText(this@UserMonitoringActivity, 
                        "Lỗi: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun blockUser(userId: Long) {
        apiService.toggleUserBlock(userId.toInt(), true)
            .enqueue(object : Callback<ApiResponse<User>> {
                override fun onResponse(
                    call: Call<ApiResponse<User>>,
                    response: Response<ApiResponse<User>>
                ) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@UserMonitoringActivity, 
                            "Đã khóa user", Toast.LENGTH_SHORT).show()
                        currentPage = 0
                        loadRiskScores()
                    }
                }
                override fun onFailure(call: Call<ApiResponse<User>>, t: Throwable) {
                    Toast.makeText(this@UserMonitoringActivity, 
                        "Lỗi: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // ==================== UI HELPERS ====================

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showEmpty(show: Boolean) {
        tvEmpty.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // ==================== 🔌 WEBSOCKET REALTIME ====================

    /**
     * Setup WebSocket listeners
     */
    private fun setupWebSocket() {
        // 🔌 Connection listener
        webSocketManager.setOnConnectionListener { connected ->
            runOnUiThread {
                isWebSocketConnected = connected
                updateConnectionStatus(connected)
            }
        }

        // 🚨 Alert listener - Nhận cảnh báo mới realtime
        webSocketManager.setOnAlertListener { alert ->
            runOnUiThread {
                handleNewAlertRealtime(alert)
            }
        }

        // 📋 Activity listener - Nhận activity mới realtime
        webSocketManager.setOnActivityListener { activity ->
            runOnUiThread {
                handleNewActivityRealtime(activity)
            }
        }

        // ⚠️ Risk score listener - Nhận cập nhật risk score realtime
        webSocketManager.setOnRiskScoreListener { riskScore ->
            runOnUiThread {
                handleRiskScoreUpdateRealtime(riskScore)
            }
        }

        // 📊 Dashboard listener - Nhận cập nhật dashboard realtime
        webSocketManager.setOnDashboardListener { dashboard ->
            runOnUiThread {
                updateDashboard(dashboard)
            }
        }
    }

    /**
     * Kết nối WebSocket
     */
    private fun connectWebSocket() {
        val baseUrl = RetrofitClient.getInstance(this).getBaseUrl()
        webSocketManager.reconnectIfNeeded(baseUrl)
    }

    /**
     * Cập nhật trạng thái kết nối trên UI
     */
    private fun updateConnectionStatus(connected: Boolean) {
        tvConnectionStatus?.let { tv ->
            if (connected) {
                tv.text = "🟢 Realtime"
                tv.setTextColor(getColor(R.color.green))
            } else {
                tv.text = "🔴 Offline"
                tv.setTextColor(getColor(R.color.red))
            }
        }
    }

    /**
     * 🚨 Xử lý khi nhận alert mới qua WebSocket
     */
    private fun handleNewAlertRealtime(alert: MonitoringAlert) {
        // Phát âm thanh cảnh báo
        playAlertSound(alert.severity)

        // Hiển thị Snackbar thông báo
        val emoji = when (alert.severity) {
            "CRITICAL" -> "🚨"
            "HIGH" -> "⚠️"
            "MEDIUM" -> "🟡"
            else -> "🔵"
        }
        
        val snackbar = Snackbar.make(
            findViewById(android.R.id.content),
            "$emoji ${alert.title}",
            Snackbar.LENGTH_LONG
        )
        snackbar.setAction("Xem") {
            // Chuyển sang tab Cảnh báo
            tabLayout.getTabAt(1)?.select()
        }
        snackbar.show()

        // Nếu đang ở tab Cảnh báo, thêm alert vào đầu danh sách
        if (currentTab == 1) {
            alertAdapter?.addItemToTop(alert)
            recyclerView.scrollToPosition(0)
        }

        // Cập nhật dashboard stats
        updateDashboardAlertCount(1)
    }

    /**
     * 📋 Xử lý khi nhận activity mới qua WebSocket
     */
    private fun handleNewActivityRealtime(activity: UserActivityLog) {
        // Nếu đang ở tab Hoạt động, thêm activity vào đầu danh sách
        if (currentTab == 2) {
            activityLogAdapter?.addItemToTop(activity)
            recyclerView.scrollToPosition(0)
        }

        // Hiển thị toast cho TẤT CẢ activity
        val emoji = when (activity.riskLevel) {
            "CRITICAL" -> "🔴"
            "SUSPICIOUS" -> "🟠"
            "WARNING" -> "🟡"
            else -> "🟢"
        }
        
        // Chỉ hiển thị toast ngắn cho activity bình thường
        val message = "${activity.username ?: "User"}: ${activity.activityTypeDisplay ?: activity.activityType}"
        Toast.makeText(this, "$emoji $message", Toast.LENGTH_SHORT).show()
        
        // Phát âm thanh nếu activity nghiêm trọng
        if (activity.riskLevel == "SUSPICIOUS" || activity.riskLevel == "CRITICAL") {
            playAlertSound(activity.riskLevel)
        }
    }

    /**
     * ⚠️ Xử lý khi nhận cập nhật risk score qua WebSocket
     */
    private fun handleRiskScoreUpdateRealtime(riskScore: UserRiskScore) {
        // Nếu đang ở tab Rủi ro, cập nhật item trong danh sách
        if (currentTab == 3) {
            riskScoreAdapter?.updateItem(riskScore)
        }

        // Hiển thị thông báo nếu user bị auto-block
        if (riskScore.autoBlocked == true) {
            playAlertSound("CRITICAL")
            
            Snackbar.make(
                findViewById(android.R.id.content),
                "🚨 ${riskScore.username} đã bị tự động khóa!",
                Snackbar.LENGTH_LONG
            ).setAction("Xem") {
                tabLayout.getTabAt(3)?.select()
            }.show()
        }
    }

    /**
     * 🔊 Phát âm thanh cảnh báo (sử dụng system notification sound)
     */
    private fun playAlertSound(severity: String?) {
        try {
            // Sử dụng system notification sound thay vì custom sound
            val notification = android.media.RingtoneManager.getDefaultUri(
                android.media.RingtoneManager.TYPE_NOTIFICATION
            )
            val ringtone = android.media.RingtoneManager.getRingtone(this, notification)
            ringtone?.play()
        } catch (e: Exception) {
            // Ignore sound errors
        }
    }

    /**
     * Cập nhật số lượng alert trên dashboard
     */
    private fun updateDashboardAlertCount(increment: Int) {
        try {
            val current = tvPendingAlerts.text.toString().toIntOrNull() ?: 0
            tvPendingAlerts.text = "${current + increment}"
        } catch (e: Exception) {
            // Ignore
        }
    }
}
