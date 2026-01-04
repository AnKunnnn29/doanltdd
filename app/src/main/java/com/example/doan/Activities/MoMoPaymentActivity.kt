package com.example.doan.Activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.CreateOrderRequest
import com.example.doan.Models.Order
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Services.PaymentService
import com.example.doan.Utils.InAppNotification
import com.example.doan.Utils.LoadingDialog
import com.example.doan.Utils.SeasonalEffectManager
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MoMoPaymentActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var paymentUrl: String? = null
    private var momoOrderId: String? = null
    private var orderRequestJson: String? = null
    private var voucherCode: String? = null
    private var spinVoucherCode: String? = null
    private var cartItemIds: LongArray? = null
    private var isPaymentCompleted = false
    private val handler = Handler(Looper.getMainLooper())
    private var statusCheckRunnable: Runnable? = null

    companion object {
        private const val TAG = "MoMoPaymentActivity"
        private const val STATUS_CHECK_INTERVAL = 3000L // 3 seconds
        private const val MAX_STATUS_CHECKS = 60 // Max 3 minutes
    }

    private var statusCheckCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vnpay_payment) // Reuse VNPay layout

        webView = findViewById(R.id.webview_vnpay)

        paymentUrl = intent.getStringExtra("PAYMENT_URL")
        momoOrderId = intent.getStringExtra("MOMO_ORDER_ID")
        orderRequestJson = intent.getStringExtra("ORDER_REQUEST")
        voucherCode = intent.getStringExtra("VOUCHER_CODE")
        spinVoucherCode = intent.getStringExtra("SPIN_VOUCHER_CODE")
        cartItemIds = intent.getLongArrayExtra("CART_ITEM_IDS")

        Log.d(TAG, "Received paymentUrl: $paymentUrl")
        Log.d(TAG, "Received momoOrderId: $momoOrderId")
        Log.d(TAG, "Received orderRequestJson: $orderRequestJson")

        if (paymentUrl.isNullOrEmpty()) {
            Toast.makeText(this, "Không có URL thanh toán", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Thử mở app MoMo trước, nếu không có thì dùng WebView
        tryOpenMoMoApp()
    }

    private fun tryOpenMoMoApp() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(paymentUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            if (intent.resolveActivity(packageManager) != null) {
                showPaymentMethodDialog()
            } else {
                Log.d(TAG, "No MoMo app found, using WebView")
                setupWebViewPayment()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening MoMo app", e)
            setupWebViewPayment()
        }
    }

    private fun showPaymentMethodDialog() {
        AlertDialog.Builder(this)
            .setTitle("Chọn phương thức thanh toán")
            .setMessage("Bạn muốn thanh toán bằng cách nào?")
            .setPositiveButton("Mở app MoMo") { _, _ ->
                openMoMoApp()
            }
            .setNegativeButton("Thanh toán trong app") { _, _ ->
                setupWebViewPayment()
            }
            .setCancelable(false)
            .show()
    }

    private fun openMoMoApp() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(paymentUrl))
            startActivity(intent)
            
            if (!momoOrderId.isNullOrEmpty()) {
                startStatusCheck()
            }
            
            Toast.makeText(this, "Vui lòng hoàn tất thanh toán trong app MoMo", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Cannot open MoMo app", e)
            Toast.makeText(this, "Không thể mở app MoMo", Toast.LENGTH_SHORT).show()
            setupWebViewPayment()
        }
    }

    private fun setupWebViewPayment() {
        setupWebView()
        webView.loadUrl(paymentUrl!!)

        if (!momoOrderId.isNullOrEmpty()) {
            startStatusCheck()
        }
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

                // Check for success/failure callbacks
                if (url.contains("resultCode=0") || url.contains("success")) {
                    handlePaymentSuccess()
                    return true
                }

                if (url.contains("cancel") || url.contains("fail") || url.contains("resultCode=")) {
                    handlePaymentFailure("Thanh toán bị hủy hoặc thất bại")
                    return true
                }

                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "Page finished: $url")
            }
        }
    }

    private fun startStatusCheck() {
        statusCheckRunnable = object : Runnable {
            override fun run() {
                if (isPaymentCompleted || statusCheckCount >= MAX_STATUS_CHECKS) {
                    return
                }

                checkPaymentStatus()
                statusCheckCount++
                handler.postDelayed(this, STATUS_CHECK_INTERVAL)
            }
        }
        handler.postDelayed(statusCheckRunnable!!, STATUS_CHECK_INTERVAL)
    }

    private fun checkPaymentStatus() {
        val orderId = momoOrderId ?: return

        PaymentService.checkMoMoOrderStatus(this, orderId) { success, message ->
            if (success && !isPaymentCompleted) {
                isPaymentCompleted = true
                handler.removeCallbacks(statusCheckRunnable!!)
                handlePaymentSuccess()
            }
        }
    }

    private fun handlePaymentSuccess() {
        if (isPaymentCompleted) return
        isPaymentCompleted = true

        Log.d(TAG, "MoMo payment success")
        createOrderAfterPayment()
    }

    private fun handlePaymentFailure(message: String) {
        isPaymentCompleted = true
        handler.removeCallbacks(statusCheckRunnable!!)

        Log.w(TAG, "MoMo payment failed: $message")
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

        val intent = Intent(this, CartActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }


    private fun createOrderAfterPayment() {
        Log.d(TAG, "createOrderAfterPayment - orderRequestJson: $orderRequestJson")
        
        if (orderRequestJson.isNullOrEmpty() || orderRequestJson == "null") {
            Log.e(TAG, "orderRequestJson is null or empty!")
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin đơn hàng", Toast.LENGTH_SHORT).show()
            navigateToCart()
            return
        }

        val loadingDialog = LoadingDialog(this)
        loadingDialog.show("Đang tạo đơn hàng...")

        try {
            val orderRequest = Gson().fromJson(orderRequestJson, CreateOrderRequest::class.java)
            val updatedRequest = orderRequest.copy(paymentMethod = "MOMO")

            RetrofitClient.getInstance(this).apiService.createOrder(updatedRequest)
                .enqueue(object : Callback<ApiResponse<Order>> {
                    override fun onResponse(
                        call: Call<ApiResponse<Order>>,
                        response: Response<ApiResponse<Order>>
                    ) {
                        loadingDialog.dismiss()

                        if (response.isSuccessful && response.body()?.success == true) {
                            val order = response.body()?.data

                            removeCartItems()

                            SeasonalEffectManager.showConfetti(3000L, 200)
                            InAppNotification.orderSuccess(
                                this@MoMoPaymentActivity,
                                order?.id?.toString() ?: "N/A"
                            )

                            Handler(Looper.getMainLooper()).postDelayed({
                                navigateToOrders()
                            }, 2500)
                        } else {
                            val errorMsg = response.body()?.message ?: "Đặt hàng thất bại"
                            InAppNotification.error(
                                this@MoMoPaymentActivity,
                                "Đặt hàng thất bại",
                                errorMsg
                            )
                            navigateToCart()
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<Order>>, t: Throwable) {
                        loadingDialog.dismiss()
                        InAppNotification.error(
                            this@MoMoPaymentActivity,
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

    override fun onDestroy() {
        super.onDestroy()
        statusCheckRunnable?.let { handler.removeCallbacks(it) }
    }
}
