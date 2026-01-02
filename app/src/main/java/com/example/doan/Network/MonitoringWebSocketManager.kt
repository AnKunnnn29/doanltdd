package com.example.doan.Network

import android.util.Log
import com.example.doan.Models.MonitoringAlert
import com.example.doan.Models.MonitoringDashboard
import com.example.doan.Models.UserActivityLog
import com.example.doan.Models.UserRiskScore
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent

/**
 * 🛡️ MONITORING WEBSOCKET MANAGER
 * Quản lý WebSocket connection cho hệ thống giám sát realtime
 * Dành cho Admin/Manager nhận cảnh báo tức thì
 */
class MonitoringWebSocketManager private constructor() {

    private var stompClient: StompClient? = null
    private val compositeDisposable = CompositeDisposable()
    private val gson: Gson = GsonBuilder().setLenient().create()
    
    // Listeners
    private var alertListener: ((MonitoringAlert) -> Unit)? = null
    private var activityListener: ((UserActivityLog) -> Unit)? = null
    private var riskScoreListener: ((UserRiskScore) -> Unit)? = null
    private var dashboardListener: ((MonitoringDashboard) -> Unit)? = null
    private var connectionListener: ((Boolean) -> Unit)? = null
    
    private var isConnected = false
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5

    companion object {
        private const val TAG = "MonitoringWSManager"
        
        @Volatile
        private var instance: MonitoringWebSocketManager? = null
        
        fun getInstance(): MonitoringWebSocketManager {
            return instance ?: synchronized(this) {
                instance ?: MonitoringWebSocketManager().also { instance = it }
            }
        }
        
        /**
         * Chuyển đổi base URL sang WebSocket URL
         */
        fun getWebSocketUrl(baseUrl: String): String {
            return baseUrl
                .replace("https://", "wss://")
                .replace("http://", "ws://")
                .replace("/api/", "/ws")
                .replace("/api", "/ws")
                .trimEnd('/')
        }
    }

    /**
     * 🔌 Kết nối WebSocket
     */
    fun connect(baseUrl: String) {
        if (isConnected) {
            Log.d(TAG, "Already connected to monitoring WebSocket")
            return
        }
        
        val wsUrl = getWebSocketUrl(baseUrl)
        Log.d(TAG, "🔌 Connecting to Monitoring WebSocket: $wsUrl")
        
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsUrl)
        stompClient?.withClientHeartbeat(10000)?.withServerHeartbeat(10000)
        
        // Subscribe lifecycle events
        val lifecycleDisposable = stompClient!!.lifecycle()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ event ->
                when (event.type) {
                    LifecycleEvent.Type.OPENED -> {
                        Log.d(TAG, "✅ Monitoring WebSocket connected")
                        isConnected = true
                        reconnectAttempts = 0
                        connectionListener?.invoke(true)
                        subscribeToMonitoringTopics()
                    }
                    LifecycleEvent.Type.CLOSED -> {
                        Log.d(TAG, "❌ Monitoring WebSocket disconnected")
                        isConnected = false
                        connectionListener?.invoke(false)
                        attemptReconnect(baseUrl)
                    }
                    LifecycleEvent.Type.ERROR -> {
                        Log.e(TAG, "⚠️ Monitoring WebSocket error: ${event.exception?.message}")
                        isConnected = false
                        connectionListener?.invoke(false)
                    }
                    else -> {}
                }
            }, { throwable ->
                Log.e(TAG, "Lifecycle error: ${throwable.message}")
            })
        
        compositeDisposable.add(lifecycleDisposable)
        stompClient?.connect()
    }

    /**
     * 📡 Subscribe các topic giám sát
     */
    private fun subscribeToMonitoringTopics() {
        // 🚨 Subscribe alerts
        subscribeToAlerts()
        
        // 📋 Subscribe activities
        subscribeToActivities()
        
        // ⚠️ Subscribe risk scores
        subscribeToRiskScores()
        
        // 📊 Subscribe dashboard updates
        subscribeToDashboard()
    }

    private fun subscribeToAlerts() {
        val disposable = stompClient!!.topic("/topic/monitoring/alerts")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ message ->
                Log.d(TAG, "🚨 New alert received: ${message.payload}")
                try {
                    val alert = gson.fromJson(message.payload, MonitoringAlert::class.java)
                    alertListener?.invoke(alert)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing alert: ${e.message}")
                }
            }, { throwable ->
                Log.e(TAG, "Error subscribing to alerts: ${throwable.message}")
            })
        compositeDisposable.add(disposable)
    }

    private fun subscribeToActivities() {
        val disposable = stompClient!!.topic("/topic/monitoring/activities")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ message ->
                Log.d(TAG, "📋 New activity received: ${message.payload}")
                try {
                    val activity = gson.fromJson(message.payload, UserActivityLog::class.java)
                    activityListener?.invoke(activity)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing activity: ${e.message}")
                }
            }, { throwable ->
                Log.e(TAG, "Error subscribing to activities: ${throwable.message}")
            })
        compositeDisposable.add(disposable)
    }

    private fun subscribeToRiskScores() {
        val disposable = stompClient!!.topic("/topic/monitoring/risk-scores")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ message ->
                Log.d(TAG, "⚠️ Risk score update received: ${message.payload}")
                try {
                    val riskScore = gson.fromJson(message.payload, UserRiskScore::class.java)
                    riskScoreListener?.invoke(riskScore)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing risk score: ${e.message}")
                }
            }, { throwable ->
                Log.e(TAG, "Error subscribing to risk scores: ${throwable.message}")
            })
        compositeDisposable.add(disposable)
    }

    private fun subscribeToDashboard() {
        val disposable = stompClient!!.topic("/topic/monitoring/dashboard")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ message ->
                Log.d(TAG, "📊 Dashboard update received")
                try {
                    val dashboard = gson.fromJson(message.payload, MonitoringDashboard::class.java)
                    dashboardListener?.invoke(dashboard)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing dashboard: ${e.message}")
                }
            }, { throwable ->
                Log.e(TAG, "Error subscribing to dashboard: ${throwable.message}")
            })
        compositeDisposable.add(disposable)
    }

    /**
     * 🔄 Tự động reconnect khi mất kết nối
     */
    private fun attemptReconnect(baseUrl: String) {
        if (reconnectAttempts < maxReconnectAttempts) {
            reconnectAttempts++
            Log.d(TAG, "🔄 Attempting reconnect ($reconnectAttempts/$maxReconnectAttempts)...")
            
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (!isConnected) {
                    connect(baseUrl)
                }
            }, (reconnectAttempts * 2000).toLong()) // Exponential backoff
        } else {
            Log.e(TAG, "❌ Max reconnect attempts reached")
        }
    }

    // ==================== LISTENERS ====================

    /**
     * 🚨 Set listener nhận cảnh báo mới
     */
    fun setOnAlertListener(listener: (MonitoringAlert) -> Unit) {
        alertListener = listener
    }

    /**
     * 📋 Set listener nhận activity mới
     */
    fun setOnActivityListener(listener: (UserActivityLog) -> Unit) {
        activityListener = listener
    }

    /**
     * ⚠️ Set listener nhận cập nhật risk score
     */
    fun setOnRiskScoreListener(listener: (UserRiskScore) -> Unit) {
        riskScoreListener = listener
    }

    /**
     * 📊 Set listener nhận cập nhật dashboard
     */
    fun setOnDashboardListener(listener: (MonitoringDashboard) -> Unit) {
        dashboardListener = listener
    }

    /**
     * 🔌 Set listener trạng thái kết nối
     */
    fun setOnConnectionListener(listener: (Boolean) -> Unit) {
        connectionListener = listener
    }

    // ==================== CONTROL ====================

    /**
     * 🔌 Ngắt kết nối WebSocket
     */
    fun disconnect() {
        Log.d(TAG, "🔌 Disconnecting Monitoring WebSocket")
        compositeDisposable.clear()
        stompClient?.disconnect()
        stompClient = null
        isConnected = false
        reconnectAttempts = 0
    }

    /**
     * Kiểm tra trạng thái kết nối
     */
    fun isConnected(): Boolean = isConnected

    /**
     * 🔄 Reconnect nếu chưa kết nối
     */
    fun reconnectIfNeeded(baseUrl: String) {
        if (!isConnected) {
            reconnectAttempts = 0
            connect(baseUrl)
        }
    }

    /**
     * Clear tất cả listeners
     */
    fun clearListeners() {
        alertListener = null
        activityListener = null
        riskScoreListener = null
        dashboardListener = null
        connectionListener = null
    }
}
