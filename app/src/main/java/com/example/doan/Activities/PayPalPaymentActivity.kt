package com.example.doan.Activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.CreateOrderRequest
import com.example.doan.Models.Order
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.InAppNotification
import com.example.doan.Utils.LoadingDialog
import com.example.doan.Utils.SeasonalEffectManager
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PayPalPaymentActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var paymentUrl: String? = null
    private var orderRequestJson: String? = null
    private var voucherCode: String? = null
    private var spinVoucherCode: String? = null
    private var cartItemIds: LongArray? = null
    private var isPaymentCompleted = false

    companion object {
        private const val TAG = "PayPalPaymentActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vnpay_payment) // Reuse VNPay layout

        webView = findViewById(R.id.webview_vnpay)

        paymentUrl = intent.getStringExtra("PAYMENT_URL")
        orderRequestJson = intent.getStringExtra("ORDER_REQUEST")
        voucherCode = intent.getStringExtra("VOUCHER_CODE")
        spinVoucherCode = intent.getStringExtra("SPIN_VOUCHER_CODE")
        cartItemIds = intent.getLongArrayExtra("CART_ITEM_IDS")

        if (paymentUrl.isNullOrEmpty()) {
            Toast.makeText(this, "Không có URL thanh toán", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupWebView()
        webView.loadUrl(paymentUrl!!)
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                Log.d(TAG, "Loading URL: $url")

                // Check for PayPal success callback - chỉ check URL callback của backend
                if (url.contains("/api/paypal/success") && url.contains("paymentId=")) {
                    handlePaymentSuccess()
                    return true
                }

                // Check for PayPal cancel callback - chỉ check URL callback của backend
                if (url.contains("/api/paypal/cancel")) {
                    handlePaymentFailure("Thanh toán đã bị hủy")
                    return true
                }

                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "Page finished: $url")
                
                // Check URL after page load - chỉ check khi có paymentId (success từ PayPal)
                url?.let {
                    if (it.contains("/api/paypal/success") && it.contains("paymentId=") && !isPaymentCompleted) {
                        handlePaymentSuccess()
                    }
                }
            }
        }
    }

    private fun handlePaymentSuccess() {
        if (isPaymentCompleted) return
        isPaymentCompleted = true

        Log.d(TAG, "PayPal payment success")
        createOrderAfterPayment()
    }

    private fun handlePaymentFailure(message: String) {
        isPaymentCompleted = true

        Log.w(TAG, "PayPal payment failed: $message")
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

        // Navigate back to cart
        val intent = Intent(this, CartActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }

    private fun createOrderAfterPayment() {
        if (orderRequestJson.isNullOrEmpty()) {
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin đơn hàng", Toast.LENGTH_SHORT).show()
            navigateToCart()
            return
        }

        val loadingDialog = LoadingDialog(this)
        loadingDialog.show("Đang tạo đơn hàng...")

        try {
            val orderRequest = Gson().fromJson(orderRequestJson, CreateOrderRequest::class.java)
            val updatedRequest = orderRequest.copy(paymentMethod = "PAYPAL")

            RetrofitClient.getInstance(this).apiService.createOrder(updatedRequest)
                .enqueue(object : Callback<ApiResponse<Order>> {
                    override fun onResponse(
                        call: Call<ApiResponse<Order>>,
                        response: Response<ApiResponse<Order>>
                    ) {
                        loadingDialog.dismiss()

                        if (response.isSuccessful && response.body()?.success == true) {
                            val order = response.body()?.data

                            // Remove purchased items from cart
                            removeCartItems()

                            // Show success
                            SeasonalEffectManager.showConfetti(3000L, 200)
                            InAppNotification.orderSuccess(
                                this@PayPalPaymentActivity,
                                order?.id?.toString() ?: "N/A"
                            )

                            // Navigate to orders
                            Handler(Looper.getMainLooper()).postDelayed({
                                navigateToOrders()
                            }, 2500)
                        } else {
                            val errorMsg = response.body()?.message ?: "Đặt hàng thất bại"
                            InAppNotification.error(
                                this@PayPalPaymentActivity,
                                "Đặt hàng thất bại",
                                errorMsg
                            )
                            navigateToCart()
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<Order>>, t: Throwable) {
                        loadingDialog.dismiss()
                        InAppNotification.error(
                            this@PayPalPaymentActivity,
                            "Lỗi kết nối",
                            t.message ?: "Không thể kết nối đến server"
                        )
                        navigateToCart()
                    }
                })
        } catch (e: Exception) {
            loadingDialog.dismiss()
            Log.e(TAG, "Error parsing order request", e)
            Toast.makeText(this, "Lỗi xử lý đơn hàng", Toast.LENGTH_SHORT).show()
            navigateToCart()
        }
    }

    private fun removeCartItems() {
        cartItemIds?.forEach { cartItemId ->
            RetrofitClient.getInstance(this).apiService.removeCartItem(cartItemId)
                .enqueue(object : Callback<ApiResponse<Void>> {
                    override fun onResponse(
                        call: Call<ApiResponse<Void>>,
                        response: Response<ApiResponse<Void>>
                    ) {
                        Log.d(TAG, "Removed cart item: $cartItemId")
                    }

                    override fun onFailure(call: Call<ApiResponse<Void>>, t: Throwable) {
                        Log.e(TAG, "Error removing cart item: $cartItemId", t)
                    }
                })
        }
    }

    private fun navigateToOrders() {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("SELECTED_ITEM", R.id.nav_order)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToCart() {
        val intent = Intent(this, CartActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            handlePaymentFailure("Đã hủy thanh toán")
        }
    }
}
