package com.example.doan.Network

import android.util.Log
import com.example.doan.Models.LiveConversation
import com.example.doan.Models.LiveMessage
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent

/**
 * Manager class để quản lý WebSocket connection cho Live Chat
 */
class LiveChatWebSocketManager private constructor() {

    private var stompClient: StompClient? = null
    private val compositeDisposable = CompositeDisposable()
    private val gson: Gson = GsonBuilder().setLenient().create()
    
    private var newConversationListener: ((LiveConversation) -> Unit)? = null
    private var newMessageListener: ((LiveMessage) -> Unit)? = null
    private var conversationClosedListener: ((Long) -> Unit)? = null
    private var connectionListener: ((Boolean) -> Unit)? = null
    
    private var isConnected = false
    private var subscribedConversationId: Long? = null

    companion object {
        private const val TAG = "LiveChatWebSocket"
        
        @Volatile
        private var instance: LiveChatWebSocketManager? = null
        
        fun getInstance(): LiveChatWebSocketManager {
            return instance ?: synchronized(this) {
                instance ?: LiveChatWebSocketManager().also { instance = it }
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
                        subscribeToManagerTopics()
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
     * Subscribe to manager topics (new conversations)
     */
    private fun subscribeToManagerTopics() {
        // Subscribe to new conversations
        val newConvDisposable = stompClient!!.topic("/topic/chat/conversations/new")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ topicMessage ->
                Log.d(TAG, "New conversation: ${topicMessage.payload}")
                try {
                    val conversation = gson.fromJson(topicMessage.payload, LiveConversation::class.java)
                    newConversationListener?.invoke(conversation)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing conversation: ${e.message}")
                }
            }, { throwable ->
                Log.e(TAG, "Error subscribing to new conversations: ${throwable.message}")
            })
        compositeDisposable.add(newConvDisposable)
    }


    /**
     * Subscribe to specific conversation for messages
     */
    fun subscribeToConversation(conversationId: Long) {
        if (!isConnected || stompClient == null) {
            Log.d(TAG, "Not connected, cannot subscribe to conversation")
            return
        }
        
        // Unsubscribe from previous conversation
        if (subscribedConversationId != null && subscribedConversationId != conversationId) {
            // Note: STOMP doesn't have direct unsubscribe, we just don't process old messages
        }
        
        subscribedConversationId = conversationId
        
        // Subscribe to messages
        val messageDisposable = stompClient!!.topic("/topic/chat/conversation/$conversationId")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ topicMessage ->
                Log.d(TAG, "New message in conversation $conversationId: ${topicMessage.payload}")
                try {
                    val message = gson.fromJson(topicMessage.payload, LiveMessage::class.java)
                    newMessageListener?.invoke(message)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing message: ${e.message}")
                }
            }, { throwable ->
                Log.e(TAG, "Error subscribing to conversation $conversationId: ${throwable.message}")
            })
        compositeDisposable.add(messageDisposable)
        
        // Subscribe to conversation closed
        val closedDisposable = stompClient!!.topic("/topic/chat/conversation/$conversationId/closed")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ topicMessage ->
                Log.d(TAG, "Conversation $conversationId closed")
                conversationClosedListener?.invoke(conversationId)
            }, { throwable ->
                Log.e(TAG, "Error subscribing to closed: ${throwable.message}")
            })
        compositeDisposable.add(closedDisposable)
    }

    /**
     * Set listener for new conversations (for manager)
     */
    fun setOnNewConversationListener(listener: (LiveConversation) -> Unit) {
        newConversationListener = listener
    }

    /**
     * Set listener for new messages
     */
    fun setOnNewMessageListener(listener: (LiveMessage) -> Unit) {
        newMessageListener = listener
    }

    /**
     * Set listener for conversation closed
     */
    fun setOnConversationClosedListener(listener: (Long) -> Unit) {
        conversationClosedListener = listener
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
        subscribedConversationId = null
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
}
