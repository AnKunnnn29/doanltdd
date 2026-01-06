package com.example.doan.Utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.example.doan.BuildConfig
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONObject

/**
 * Handler xử lý khi user bị block IP (HTTP 403 - IP_BLOCKED)
 * - Hiển thị thông báo rõ ràng cho user
 * - Cung cấp thông tin liên hệ support
 * - Retry mechanism sau một khoảng thời gian
 */
object IpBlockHandler {
    
    private const val TAG = "IpBlockHandler"
    const val SUPPORT_PHONE = "0968046024"
    const val ERROR_CODE_IP_BLOCKED = "IP_BLOCKED"
    
    // Retry configuration
    private const val INITIAL_RETRY_DELAY_MS = 30_000L // 30 giây
    private const val MAX_RETRY_DELAY_MS = 300_000L // 5 phút
    private var currentRetryDelay = INITIAL_RETRY_DELAY_MS
    private var lastBlockedTime = 0L
    
    /**
     * Kiểm tra response có phải là IP_BLOCKED không
     */
    fun isIpBlockedError(responseCode: Int, responseBody: String?): Boolean {
        if (responseCode != 403) return false
        
        return try {
            responseBody?.let {
                val json = JSONObject(it)
                val errorCode = json.optString("errorCode", "")
                    .takeIf { code -> code.isNotEmpty() }
                    ?: json.optString("error", "")
                
                errorCode.equals(ERROR_CODE_IP_BLOCKED, ignoreCase = true)
            } ?: false
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Error parsing response: ${e.message}")
            }
            false
        }
    }
    
    /**
     * Hiển thị dialog thông báo IP bị block
     */
    fun showBlockedDialog(context: Context, onRetry: (() -> Unit)? = null) {
        if (context !is Activity || context.isFinishing || context.isDestroyed) {
            return
        }
        
        lastBlockedTime = System.currentTimeMillis()
        
        val message = buildString {
            append("Tài khoản của bạn đã bị tạm khóa do phát hiện hoạt động bất thường.\n\n")
            append("Nếu bạn cho rằng đây là nhầm lẫn, vui lòng liên hệ bộ phận hỗ trợ:\n\n")
            append("📞 Hotline: $SUPPORT_PHONE\n\n")
            append("Chúng tôi sẽ hỗ trợ bạn trong thời gian sớm nhất.")
        }
        
        MaterialAlertDialogBuilder(context)
            .setTitle("⚠️ Tài khoản bị tạm khóa")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Gọi hỗ trợ") { dialog, _ ->
                dialog.dismiss()
                callSupport(context)
            }
            .setNegativeButton("Thử lại sau") { dialog, _ ->
                dialog.dismiss()
                onRetry?.invoke()
            }
            .setNeutralButton("Đóng") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    /**
     * Gọi điện thoại hỗ trợ
     */
    private fun callSupport(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$SUPPORT_PHONE")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Cannot open dialer: ${e.message}")
            }
        }
    }
    
    /**
     * Kiểm tra có thể retry không (dựa trên thời gian chờ)
     */
    fun canRetry(): Boolean {
        val elapsed = System.currentTimeMillis() - lastBlockedTime
        return elapsed >= currentRetryDelay
    }
    
    /**
     * Lấy thời gian còn lại trước khi có thể retry (milliseconds)
     */
    fun getRetryDelayRemaining(): Long {
        val elapsed = System.currentTimeMillis() - lastBlockedTime
        return maxOf(0, currentRetryDelay - elapsed)
    }
    
    /**
     * Tăng thời gian chờ retry (exponential backoff)
     */
    fun increaseRetryDelay() {
        currentRetryDelay = minOf(currentRetryDelay * 2, MAX_RETRY_DELAY_MS)
    }
    
    /**
     * Reset retry delay về mặc định
     */
    fun resetRetryDelay() {
        currentRetryDelay = INITIAL_RETRY_DELAY_MS
        lastBlockedTime = 0L
    }
    
    /**
     * Format thời gian chờ còn lại thành chuỗi đọc được
     */
    fun formatRetryDelay(): String {
        val remaining = getRetryDelayRemaining()
        val seconds = (remaining / 1000) % 60
        val minutes = (remaining / 1000) / 60
        
        return when {
            minutes > 0 -> "$minutes phút $seconds giây"
            else -> "$seconds giây"
        }
    }
}
