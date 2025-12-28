package com.example.doan.Network

import android.util.Log
import com.example.doan.Models.GroupChatMessageDto
import com.example.doan.Models.SendGroupChatRequest
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent

/**
 * Manager class để quản lý WebSocket connection cho Group Chat
 * Mỗi group order có 1 topic riêng: /topic/group-chat/{groupOrderId}
 */
class GroupChatWebSocketManager private constructor() {

    private var stompClient: StompClient? = null
    private val compositeDisposable = CompositeDisposable()
    private val gson: Gson = GsonBuilder().setLenient().create()
    
    private var newMessageListener: ((GroupChatMessageDto) -> Unit)? = null
    private var connectionListener: ((Boolean) -> Unit)? = null
    
    private var isConnected = false
    private var subscribedGroupOrderId: Long? = null

    companion object {
        private const val TAG = "GroupChatWebSocket"
        
        @Volatile
        private var instance: GroupChatWebSocketManager? = null
        
        fun getInstance(): GroupChatWebSocketManager {
            return instance ?: synchronized(this) {
                instance ?: GroupChatWebSocketManager().also { instance = it }
            }
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
        
        val wsUrl = OrderWebSocketManager.getWebSocketUrl(baseUrl)
        Log.d(TAG, "Connecting to WebSocket: $wsUrl")
        
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsUrl)
        stompClient?.withClientHeartbeat(10000)?.withServerHeartbeat(10000)
        
        val lifecycleDisposable = stompClient!!.lifecycle()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ lifecycleEvent ->
                when (lifecycleEvent.type) {
                    LifecycleEvent.Type.OPENED -> {
                        Log.d(TAG, "WebSocket connected")
                        isConnected = true
                        connectionListener?.invoke(true)
                        // Re-subscribe nếu đã có groupOrderId
                        subscribedGroupOrderId?.let { subscribeToGroupChat(it) }
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
     * Subscribe to group chat topic
     */
    fun subscribeToGroupChat(groupOrderId: Long) {
        if (!isConnected || stompClient == null) {
            Log.d(TAG, "Not connected, saving groupOrderId for later subscription")
            subscribedGroupOrderId = groupOrderId
            return
        }
        
        subscribedGroupOrderId = groupOrderId
        
        // Subscribe to group chat messages
        val messageDisposable = stompClient!!.topic("/topic/group-chat/$groupOrderId")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ topicMessage ->
                Log.d(TAG, "New message in group $groupOrderId: ${topicMessage.payload}")
                try {
                    val message = gson.fromJson(topicMessage.payload, GroupChatMessageDto::class.java)
                    newMessageListener?.invoke(message)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing message: ${e.message}")
                }
            }, { throwable ->
                Log.e(TAG, "Error subscribing to group chat $groupOrderId: ${throwable.message}")
            })
        compositeDisposable.add(messageDisposable)
        
        Log.d(TAG, "Subscribed to group chat: $groupOrderId")
    }

    /**
     * Gửi tin nhắn qua WebSocket (realtime)
     * Client gửi đến: /app/group-chat/{groupOrderId}
     */
    fun sendMessage(groupOrderId: Long, content: String) {
        if (!isConnected || stompClient == null) {
            Log.e(TAG, "Cannot send message: not connected")
            return
        }
        
        val request = SendGroupChatRequest(content)
        val json = gson.toJson(request)
        
        val sendDisposable = stompClient!!.send("/app/group-chat/$groupOrderId", json)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Log.d(TAG, "Message sent successfully")
            }, { throwable ->
                Log.e(TAG, "Error sending message: ${throwable.message}")
            })
        compositeDisposable.add(sendDisposable)
    }

    /**
     * Set listener for new messages
     */
    fun setOnNewMessageListener(listener: (GroupChatMessageDto) -> Unit) {
        newMessageListener = listener
    }

    /**
     * Set listener for connection state
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
        subscribedGroupOrderId = null
    }

    /**
     * Check if connected
     */
    fun isConnected(): Boolean = isConnected

    /**
     * Reconnect if needed
     */
    fun reconnectIfNeeded(baseUrl: String) {
        if (!isConnected) {
            connect(baseUrl)
        }
    }
    
    /**
     * Unsubscribe from current group and clear listener
     */
    fun unsubscribeFromGroupChat() {
        subscribedGroupOrderId = null
        // Note: STOMP không có direct unsubscribe, chỉ clear listener
        newMessageListener = null
    }
}
