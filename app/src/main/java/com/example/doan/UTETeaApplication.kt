package com.example.doan

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.doan.Network.OrderWebSocketManager
import com.example.doan.Network.RetrofitClient
import com.example.doan.Utils.CartManager

class UTETeaApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize CartManager with context
        CartManager.getInstance().init(this)
        
        // Setup lifecycle observer for WebSocket management
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver())
    }
    
    /**
     * Observer để quản lý WebSocket khi app vào foreground/background
     */
    private inner class AppLifecycleObserver : DefaultLifecycleObserver {
        
        override fun onStart(owner: LifecycleOwner) {
            // App comes to foreground - reconnect WebSocket if needed
            val webSocketManager = OrderWebSocketManager.getInstance()
            val baseUrl = RetrofitClient.getBaseUrl()
            webSocketManager.reconnectIfNeeded(baseUrl)
        }
        
        override fun onStop(owner: LifecycleOwner) {
            // App goes to background - disconnect WebSocket to save battery
            OrderWebSocketManager.getInstance().disconnect()
        }
    }
}
