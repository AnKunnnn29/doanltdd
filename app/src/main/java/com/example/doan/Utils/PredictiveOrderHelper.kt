package com.example.doan.Utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.doan.Models.*
import com.example.doan.Network.RetrofitClient
import com.example.doan.Views.PredictiveOrderDialog
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.TimeUnit

/**
 * Helper class để quản lý tính năng Predictive Order
 * - Kiểm tra điều kiện hiển thị
 * - Gọi API lấy prediction
 * - Hiển thị dialog gợi ý
 */
class PredictiveOrderHelper(private val context: Context) {
    
    companion object {
        private const val TAG = "PredictiveOrder"
        private const val PREFS_NAME = "predictive_order_prefs"
        private const val KEY_LAST_SHOWN = "last_shown_time"
        private const val KEY_DISMISSED_COUNT = "dismissed_count"
        private const val KEY_LAST_DISMISSED_DRINK = "last_dismissed_drink"
        private const val KEY_LAST_SESSION_ID = "last_session_id"
        
        // Không hiển thị lại trong vòng 2 giờ nếu user đã dismiss
        private const val MIN_INTERVAL_HOURS = 2L
        // Tối đa 3 lần dismiss trong ngày cho cùng 1 món
        private const val MAX_DISMISS_PER_DAY = 3
        
        // Session-based flag: Tắt trong phiên hiện tại (reset khi tắt app)
        @Volatile
        private var disabledForSession = false
        
        // Flag để tránh gọi API nhiều lần trong cùng 1 session
        @Volatile
        private var hasCheckedThisSession = false
        
        // Session ID để track phiên hiện tại
        private val currentSessionId = System.currentTimeMillis()
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Kiểm tra và hiển thị prediction dialog nếu phù hợp
     * @param activity Activity để hiển thị dialog
     * @param weather Điều kiện thời tiết (optional)
     * @param onAddToCart Callback khi user chọn thêm vào giỏ
     * @param forceShow Bỏ qua điều kiện thời gian (nhưng vẫn tôn trọng "Không hiển thị lại")
     */
    fun checkAndShowPrediction(
        activity: AppCompatActivity,
        weather: String? = null,
        forceShow: Boolean = false,
        onAddToCart: (PredictedDrink) -> Unit
    ) {
        // LUÔN kiểm tra xem user có tắt tính năng không (không bỏ qua dù forceShow = true)
        if (isPredictionDisabled()) {
            Log.d(TAG, "Skipping prediction - feature disabled by user")
            return
        }
        
        // Kiểm tra điều kiện thời gian (bỏ qua nếu forceShow = true)
        if (!forceShow && !shouldShowPrediction()) {
            Log.d(TAG, "Skipping prediction - too soon since last shown")
            return
        }
        
        // Gọi API
        fetchPrediction(weather) { prediction ->
            if (prediction != null && prediction.hasPrediction && prediction.predictedDrink != null) {
                // Kiểm tra xem có phải món đã bị dismiss nhiều lần không (bỏ qua nếu forceShow)
                if (!forceShow && isDrinkDismissedTooMuch(prediction.predictedDrink.drinkId)) {
                    Log.d(TAG, "Skipping prediction - drink dismissed too many times")
                    return@fetchPrediction
                }
                
                // Hiển thị dialog
                activity.runOnUiThread {
                    showPredictionDialog(activity, prediction, onAddToCart)
                }
            } else {
                Log.d(TAG, "No prediction available: ${prediction?.message}")
            }
        }
    }
    
    /**
     * Gọi API lấy prediction
     */
    private fun fetchPrediction(weather: String?, callback: (PredictiveOrderResponse?) -> Unit) {
        Log.d(TAG, "Fetching prediction from API...")
        
        RetrofitClient.getInstance(context).apiService.getPredictiveOrder(weather)
            .enqueue(object : Callback<ApiResponse<PredictiveOrderResponse>> {
                override fun onResponse(
                    call: Call<ApiResponse<PredictiveOrderResponse>>,
                    response: Response<ApiResponse<PredictiveOrderResponse>>
                ) {
                    Log.d(TAG, "API response code: ${response.code()}")
                    if (response.isSuccessful) {
                        val data = response.body()?.data
                        Log.d(TAG, "Prediction received: hasPrediction=${data?.hasPrediction}, message=${data?.message}")
                        if (data?.predictedDrink != null) {
                            Log.d(TAG, "Predicted drink: ${data.predictedDrink.drinkName}, confidence=${data.confidenceScore}")
                        }
                        callback(data)
                    } else {
                        Log.e(TAG, "API error: ${response.code()} - ${response.errorBody()?.string()}")
                        callback(null)
                    }
                }
                
                override fun onFailure(call: Call<ApiResponse<PredictiveOrderResponse>>, t: Throwable) {
                    Log.e(TAG, "API call failed: ${t.message}", t)
                    callback(null)
                }
            })
    }
    
    /**
     * Hiển thị dialog gợi ý
     */
    private fun showPredictionDialog(
        activity: AppCompatActivity,
        prediction: PredictiveOrderResponse,
        onAddToCart: (PredictedDrink) -> Unit
    ) {
        val dialog = PredictiveOrderDialog(
            context = activity,
            prediction = prediction,
            onAddToCart = { drink ->
                // Reset dismiss count khi user chấp nhận
                resetDismissCount()
                onAddToCart(drink)
            },
            onDismiss = {
                // Ghi nhận dismiss
                recordDismiss(prediction.predictedDrink?.drinkId)
            },
            onDontShowAgain = {
                // User chọn không hiển thị lại trong phiên này
                disabledForSession = true
                Log.d(TAG, "User disabled prediction for this session")
            }
        )
        
        dialog.show()
        recordShown()
    }
    
    /**
     * Kiểm tra có nên hiển thị prediction không
     */
    private fun shouldShowPrediction(): Boolean {
        val lastShown = prefs.getLong(KEY_LAST_SHOWN, 0)
        val now = System.currentTimeMillis()
        val hoursSinceLastShown = TimeUnit.MILLISECONDS.toHours(now - lastShown)
        
        Log.d(TAG, "Hours since last shown: $hoursSinceLastShown, required: $MIN_INTERVAL_HOURS")
        return hoursSinceLastShown >= MIN_INTERVAL_HOURS
    }
    
    /**
     * Kiểm tra món có bị dismiss quá nhiều không
     */
    private fun isDrinkDismissedTooMuch(drinkId: Long): Boolean {
        val lastDismissedDrink = prefs.getLong(KEY_LAST_DISMISSED_DRINK, -1)
        if (lastDismissedDrink != drinkId) {
            return false
        }
        
        val dismissCount = prefs.getInt(KEY_DISMISSED_COUNT, 0)
        return dismissCount >= MAX_DISMISS_PER_DAY
    }
    
    /**
     * Ghi nhận đã hiển thị
     */
    private fun recordShown() {
        prefs.edit()
            .putLong(KEY_LAST_SHOWN, System.currentTimeMillis())
            .apply()
    }
    
    /**
     * Ghi nhận user dismiss
     */
    private fun recordDismiss(drinkId: Long?) {
        if (drinkId == null) return
        
        val lastDismissedDrink = prefs.getLong(KEY_LAST_DISMISSED_DRINK, -1)
        val currentCount = if (lastDismissedDrink == drinkId) {
            prefs.getInt(KEY_DISMISSED_COUNT, 0)
        } else {
            0
        }
        
        prefs.edit()
            .putLong(KEY_LAST_DISMISSED_DRINK, drinkId)
            .putInt(KEY_DISMISSED_COUNT, currentCount + 1)
            .putLong(KEY_LAST_SHOWN, System.currentTimeMillis())
            .apply()
    }
    
    /**
     * Reset dismiss count (khi user chấp nhận gợi ý)
     */
    private fun resetDismissCount() {
        prefs.edit()
            .putInt(KEY_DISMISSED_COUNT, 0)
            .apply()
    }
    
    /**
     * Kiểm tra xem prediction có bị tắt trong phiên này không
     */
    private fun isPredictionDisabled(): Boolean {
        if (disabledForSession) {
            Log.d(TAG, "Prediction disabled for this session")
            return true
        }
        return false
    }
    
    /**
     * Bật lại prediction (reset session flag)
     */
    fun enablePrediction() {
        disabledForSession = false
        Log.d(TAG, "Prediction re-enabled")
    }
    
    /**
     * Kiểm tra trạng thái prediction
     */
    fun isPredictionEnabled(): Boolean {
        return !disabledForSession
    }
    
    /**
     * Clear all preferences (for testing)
     */
    fun clearPreferences() {
        prefs.edit().clear().apply()
    }
}
