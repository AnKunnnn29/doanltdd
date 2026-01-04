package com.example.doan.Services

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.doan.Models.*
import com.example.doan.Network.RetrofitClient
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * PaymentService - Quản lý các cổng thanh toán
 * Hỗ trợ: VNPay, MoMo, PayPal
 */
object PaymentService {
    
    private const val TAG = "PaymentService"
    
    interface PaymentCallback {
        fun onSuccess(paymentUrl: String, transactionId: String?)
        fun onError(message: String)
    }
    
    // ==================== VNPAY ====================
    fun createVNPayPayment(
        context: Context,
        amount: Long,
        orderInfo: String,
        callback: PaymentCallback
    ) {
        val apiService = RetrofitClient.getInstance(context).apiService
        apiService.createVNPayPaymentWithAmount(amount, orderInfo)
            .enqueue(object : Callback<ApiResponse<VNPayPaymentResponse>> {
                override fun onResponse(
                    call: Call<ApiResponse<VNPayPaymentResponse>>,
                    response: Response<ApiResponse<VNPayPaymentResponse>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val paymentUrl = response.body()?.data?.paymentUrl
                        if (!paymentUrl.isNullOrEmpty()) {
                            callback.onSuccess(paymentUrl, null)
                        } else {
                            callback.onError("Không nhận được URL thanh toán VNPay")
                        }
                    } else {
                        callback.onError(response.body()?.message ?: "Lỗi tạo thanh toán VNPay")
                    }
                }
                
                override fun onFailure(call: Call<ApiResponse<VNPayPaymentResponse>>, t: Throwable) {
                    Log.e(TAG, "VNPay error", t)
                    callback.onError("Lỗi kết nối: ${t.message}")
                }
            })
    }
    
    // ==================== MOMO ====================
    fun createMoMoPayment(
        context: Context,
        amount: Long,
        orderInfo: String?,
        callback: PaymentCallback
    ) {
        val apiService = RetrofitClient.getInstance(context).apiService
        val request = MoMoPaymentRequest(
            amount = amount.toString(),
            orderInfo = orderInfo ?: "UTE Tea Payment"
        )
        
        apiService.createMoMoPayment(request)
            .enqueue(object : Callback<String> {
                override fun onResponse(call: Call<String>, response: Response<String>) {
                    if (response.isSuccessful) {
                        try {
                            val json = JSONObject(response.body() ?: "{}")
                            val resultCode = json.optInt("resultCode", -1)
                            
                            if (resultCode == 0) {
                                val payUrl = json.optString("payUrl", "")
                                val orderId = json.optString("orderId", "")
                                
                                if (payUrl.isNotEmpty()) {
                                    callback.onSuccess(payUrl, orderId)
                                } else {
                                    callback.onError("Không nhận được URL thanh toán MoMo")
                                }
                            } else {
                                val message = json.optString("message", "Lỗi tạo thanh toán MoMo")
                                callback.onError(message)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "MoMo parse error", e)
                            callback.onError("Lỗi xử lý response MoMo")
                        }
                    } else {
                        callback.onError("Lỗi tạo thanh toán MoMo: ${response.code()}")
                    }
                }
                
                override fun onFailure(call: Call<String>, t: Throwable) {
                    Log.e(TAG, "MoMo error", t)
                    callback.onError("Lỗi kết nối: ${t.message}")
                }
            })
    }
    
    fun checkMoMoOrderStatus(
        context: Context,
        orderId: String,
        callback: (success: Boolean, message: String) -> Unit
    ) {
        val apiService = RetrofitClient.getInstance(context).apiService
        apiService.checkMoMoOrderStatus(orderId)
            .enqueue(object : Callback<String> {
                override fun onResponse(call: Call<String>, response: Response<String>) {
                    if (response.isSuccessful) {
                        try {
                            val json = JSONObject(response.body() ?: "{}")
                            val resultCode = json.optInt("resultCode", -1)
                            val message = json.optString("message", "")
                            
                            // resultCode = 0: success
                            callback(resultCode == 0, message)
                        } catch (e: Exception) {
                            callback(false, "Lỗi xử lý response")
                        }
                    } else {
                        callback(false, "Lỗi kiểm tra trạng thái")
                    }
                }
                
                override fun onFailure(call: Call<String>, t: Throwable) {
                    callback(false, "Lỗi kết nối: ${t.message}")
                }
            })
    }
    
    // ==================== PAYPAL ====================
    fun createPayPalPayment(
        context: Context,
        total: Double,
        currency: String = "USD",
        description: String?,
        callback: PaymentCallback
    ) {
        val apiService = RetrofitClient.getInstance(context).apiService
        val request = PayPalPaymentRequest(
            total = String.format("%.2f", total),
            currency = currency,
            description = description
        )
        
        apiService.createPayPalPayment(request)
            .enqueue(object : Callback<ApiResponse<PayPalPaymentResponse>> {
                override fun onResponse(
                    call: Call<ApiResponse<PayPalPaymentResponse>>,
                    response: Response<ApiResponse<PayPalPaymentResponse>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val approvalUrl = response.body()?.data?.approvalUrl
                        if (!approvalUrl.isNullOrEmpty()) {
                            callback.onSuccess(approvalUrl, null)
                        } else {
                            callback.onError("Không nhận được URL thanh toán PayPal")
                        }
                    } else {
                        callback.onError(response.body()?.message ?: "Lỗi tạo thanh toán PayPal")
                    }
                }
                
                override fun onFailure(call: Call<ApiResponse<PayPalPaymentResponse>>, t: Throwable) {
                    Log.e(TAG, "PayPal error", t)
                    callback.onError("Lỗi kết nối: ${t.message}")
                }
            })
    }
    
    // ==================== HELPER ====================
    fun openPaymentUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Cannot open payment URL", e)
        }
    }
    
    // Convert VND to USD (approximate rate)
    fun convertVNDtoUSD(vndAmount: Long): Double {
        val rate = 24500.0 // 1 USD = ~24,500 VND
        return vndAmount / rate
    }
}
