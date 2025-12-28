package com.example.doan.Network

import android.util.Log
import com.example.doan.Models.Order
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent

/**
 * Manager class để quản lý WebSocket connection cho realtime order updates
 * Sử dụng STOMP protocol qua WebSocket
 */
class OrderWebSocketManager private constructor() {

    private var stompClient: StompClient? = null
    private val compositeDisposable = CompositeDisposable()
    private val gson: Gson = GsonBuilder().setLenient().create()
    
    private var newOrderListener: ((Order) -> Unit)? = null
    private var statusUpdateListener: ((Order) -> Unit)? = null
    private var connectionListener: ((Boolean) -> Unit)? = null
    
    private var isConnected = false
    private var selectedStoreId: Long? = null

    companion object {
        private const val TAG = "OrderWebSocketManager"
        
        @Volatile
        private var instance: OrderWebSocketManager? = null
        
        fun getInstance(): OrderWebSocketManager {
            return instance ?: synchronized(this) {
                instance ?: OrderWebSocketManager().also { instance = it }
            }
        }
        
        /**
         * Lấy WebSocket URL từ base URL
         * Chuyển https:// -> wss:// và http:// -> ws://
         */
        fun getWebSocketUrl(baseUrl: String): String {
            val wsUrl = baseUrl
                .replace("https://", "wss://")
                .replace("http://", "ws://")
                .replace("/api/", "/ws")
                .trimEnd('/')
            return wsUrl
        }
    }

    /**
     * Kết nối WebSocket
     */
    fun connect(baseUrl: String) {
        if (isConnected) {
            Log.d(TAG, "Already connected")
            return
        }
        
        val wsUrl = getWebSocketUrl(baseUrl)
        Log.d(TAG, "Connecting to WebSocket: $wsUrl")
        
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsUrl)
        stompClient?.withClientHeartbeat(10000)?.withServerHeartbeat(10000)
        
        // Subscribe to lifecycle events
        val lifecycleDisposable = stompClient!!.lifecycle()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ lifecycleEvent ->
                when (lifecycleEvent.type) {
                    LifecycleEvent.Type.OPENED -> {
                        Log.d(TAG, "WebSocket connected")
                        isConnected = true
                        connectionListener?.invoke(true)
                        subscribeToTopics()
                    }
                    LifecycleEvent.Type.CLOSED -> {
                        Log.d(TAG, "WebSocket disconnected")
                        isConnected = false
                        connectionListener?.invoke(false)
                    }
                    LifecycleEvent.Type.ERROR -> {
                        Log.e(TAG, "WebSocket error: ${lifecycleEvent.exception?.message}")
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
     * Subscribe to order topics
     */
    private fun subscribeToTopics() {
        // Subscribe to new orders (all stores)
        val newOrderDisposable = stompClient!!.topic("/topic/orders/new")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ topicMessage ->
                Log.d(TAG, "New order received: ${topicMessage.payload}")
                try {
                    val order = gson.fromJson(topicMessage.payload, Order::class.java)
                    newOrderListener?.invoke(order)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing new order: ${e.message}")
                }
            }, { throwable ->
                Log.e(TAG, "Error subscribing to new orders: ${throwable.message}")
            })
        compositeDisposable.add(newOrderDisposable)
        
        // Subscribe to order status updates
        val statusDisposable = stompClient!!.topic("/topic/orders/status")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ topicMessage ->
                Log.d(TAG, "Order status update: ${topicMessage.payload}")
                try {
                    val order = gson.fromJson(topicMessage.payload, Order::class.java)
                    statusUpdateListener?.invoke(order)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing status update: ${e.message}")
                }
            }, { throwable ->
                Log.e(TAG, "Error subscribing to status updates: ${throwable.message}")
            })
        compositeDisposable.add(statusDisposable)
        
        // Subscribe to specific store if selected
        selectedStoreId?.let { storeId ->
            subscribeToStore(storeId)
        }
    }

    /**
     * Subscribe to specific store's orders
     */
    fun subscribeToStore(storeId: Long) {
        selectedStoreId = storeId
        
        if (!isConnected || stompClient == null) {
            Log.d(TAG, "Not connected, will subscribe when connected")
            return
        }
        
        val storeDisposable = stompClient!!.topic("/topic/orders/store/$storeId")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ topicMessage ->
                Log.d(TAG, "Store $storeId order: ${topicMessage.payload}")
                try {
                    val order = gson.fromJson(topicMessage.payload, Order::class.java)
                    newOrderListener?.invoke(order)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing store order: ${e.message}")
                }
            }, { throwable ->
                Log.e(TAG, "Error subscribing to store $storeId: ${throwable.message}")
            })
        compositeDisposable.add(storeDisposable)
    }

    /**
     * Set listener for new orders
     */
    fun setOnNewOrderListener(listener: (Order) -> Unit) {
        newOrderListener = listener
    }

    /**
     * Set listener for order status updates
     */
    fun setOnStatusUpdateListener(listener: (Order) -> Unit) {
        statusUpdateListener = listener
    }

    /**
     * Set listener for connection state changes
     */
    fun setOnConnectionListener(listener: (Boolean) -> Unit) {
        connectionListener = listener
    }

    /**
     * Disconnect WebSocket
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting WebSocket")
        compositeDisposable.clear()
        stompClient?.disconnect()
        stompClient = null
        isConnected = false
        selectedStoreId = null
    }

    /**
     * Check if connected
     */
    fun isConnected(): Boolean = isConnected

    /**
     * Reconnect if disconnected
     */
    fun reconnectIfNeeded(baseUrl: String) {
        if (!isConnected) {
            connect(baseUrl)
        }
    }
}
