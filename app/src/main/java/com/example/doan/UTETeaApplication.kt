package com.example.doan

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.doan.Network.OrderWebSocketManager
import com.example.doan.Network.RetrofitClient
import com.example.doan.Utils.CartManager
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UTETeaApplication : Application() {

    companion object {
        private const val ONESIGNAL_APP_ID = "e47a596f-789f-4573-b270-6cd10f557fd3"
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize CartManager with context
        CartManager.getInstance().init(this)

        // Setup lifecycle observer for WebSocket management
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver())

        // --- OneSignal Initialization ---
        // Bật ghi log chi tiết để gỡ lỗi trong môi trường development
        OneSignal.Debug.logLevel = LogLevel.VERBOSE

        // Khởi tạo OneSignal
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID)

        // Yêu cầu quyền thông báo trong coroutine
        CoroutineScope(Dispatchers.Main).launch {
            OneSignal.Notifications.requestPermission(true)
        }
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
