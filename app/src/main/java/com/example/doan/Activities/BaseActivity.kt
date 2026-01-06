package com.example.doan.Activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.doan.Network.AuthInterceptor
import com.example.doan.Utils.IpBlockHandler

/**
 * BaseActivity xử lý các broadcast chung như:
 * - IP_BLOCKED: Hiển thị dialog thông báo user bị block
 * - TOKEN_EXPIRED: Chuyển về màn hình login
 * 
 * Các Activity khác nên extend từ BaseActivity để tự động có các xử lý này
 */
open class BaseActivity : AppCompatActivity() {
    
    private var ipBlockedReceiver: BroadcastReceiver? = null
    private var tokenExpiredReceiver: BroadcastReceiver? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerReceivers()
    }
    
    private fun registerReceivers() {
        // Receiver cho IP_BLOCKED
        ipBlockedReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                handleIpBlocked()
            }
        }
        
        // Receiver cho TOKEN_EXPIRED
        tokenExpiredReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                handleTokenExpired()
            }
        }
        
        LocalBroadcastManager.getInstance(this).apply {
            registerReceiver(ipBlockedReceiver!!, IntentFilter(AuthInterceptor.ACTION_IP_BLOCKED))
            registerReceiver(tokenExpiredReceiver!!, IntentFilter(AuthInterceptor.ACTION_TOKEN_EXPIRED))
        }
    }
    
    /**
     * Xử lý khi IP bị block
     * Hiển thị dialog với thông tin liên hệ support
     */
    protected open fun handleIpBlocked() {
        IpBlockHandler.showBlockedDialog(this) {
            // Callback khi user chọn "Thử lại sau"
            onRetryAfterBlock()
        }
    }
    
    /**
     * Override để xử lý retry sau khi bị block
     */
    protected open fun onRetryAfterBlock() {
        // Default: không làm gì
        // Các Activity con có thể override để retry request
    }
    
    /**
     * Xử lý khi token hết hạn
     * Chuyển về màn hình login
     */
    protected open fun handleTokenExpired() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceivers()
    }
    
    private fun unregisterReceivers() {
        LocalBroadcastManager.getInstance(this).apply {
            ipBlockedReceiver?.let { unregisterReceiver(it) }
            tokenExpiredReceiver?.let { unregisterReceiver(it) }
        }
        ipBlockedReceiver = null
        tokenExpiredReceiver = null
    }
}
