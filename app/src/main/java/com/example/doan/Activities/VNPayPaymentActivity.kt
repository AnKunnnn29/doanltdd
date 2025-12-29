package com.example.doan.Activities

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.CreateOrderRequest
import com.example.doan.Models.Order
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.SessionManager
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VNPayPaymentActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private var paymentUrl: String? = null
    private var orderRequestJson: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vnpay_payment)

        paymentUrl = intent.getStringExtra("PAYMENT_URL")
        orderRequestJson = intent.getStringExtra("ORDER_REQUEST")
        
        if (paymentUrl.isNullOrEmpty()) {
            Toast.makeText(this, "URL thanh toán không hợp lệ", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupWebView()
        setupBackPressHandler()
        loadPaymentUrl()
    }

    private fun initViews() {
        webView = findViewById(R.id.webview_vnpay)
        progressBar = findViewById(R.id.progress_bar)
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
                Log.d("VNPayPayment", "Loading URL: $url")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                
                if (url != null && url.contains("/api/vnpay/callback")) {
                    Log.d("VNPayPayment", "Callback page loaded: $url")
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                Log.d("VNPayPayment", "Navigating to: $url")
                
                when {
                    url.startsWith("myapp://payment/success") -> {
                        handlePaymentSuccess()
                        return true
                    }
                    url.startsWith("myapp://payment/failure") -> {
                        handlePaymentFailure()
                        return true
                    }
                    url.startsWith("myapp://payment/exit") -> {
                        handlePaymentExit()
                        return true
                    }
                    url.contains("/api/vnpay/callback") -> {
                        val responseCode = extractResponseCode(url)
                        if (responseCode == "00") {
                            handlePaymentSuccess()
                        } else {
                            handlePaymentFailure()
                        }
                        return true
                    }
                }
                
                return false
            }
            
            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url == null) return false
                Log.d("VNPayPayment", "Navigating to (legacy): $url")
                
                when {
                    url.startsWith("myapp://payment/success") -> {
                        handlePaymentSuccess()
                        return true
                    }
                    url.startsWith("myapp://payment/failure") -> {
                        handlePaymentFailure()
                        return true
                    }
                    url.startsWith("myapp://payment/exit") -> {
                        handlePaymentExit()
                        return true
                    }
                    url.contains("/api/vnpay/callback") -> {
                        val responseCode = extractResponseCode(url)
                        if (responseCode == "00") {
                            handlePaymentSuccess()
                        } else {
                            handlePaymentFailure()
                        }
                        return true
                    }
                }
                
                return false
            }
        }
    }
    
    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun loadPaymentUrl() {
        paymentUrl?.let {
            Log.d("VNPayPayment", "Loading payment URL: $it")
            webView.loadUrl(it)
        }
    }

    private fun handlePaymentSuccess() {
        // Thanh toán thành công, tạo đơn hàng
        if (!orderRequestJson.isNullOrEmpty()) {
            createOrderAfterPayment()
        } else {
            Toast.makeText(this, "Thanh toán thành công!", Toast.LENGTH_LONG).show()
            navigateToOrderSuccess()
        }
    }
    
    private fun createOrderAfterPayment() {
        progressBar.visibility = View.VISIBLE
        
        try {
            val orderRequest = Gson().fromJson(orderRequestJson, CreateOrderRequest::class.java)
            
            RetrofitClient.getInstance(this).apiService.createOrderAfterPayment(orderRequest)
                .enqueue(object : Callback<ApiResponse<Order>> {
                    override fun onResponse(call: Call<ApiResponse<Order>>, response: Response<ApiResponse<Order>>) {
                        progressBar.visibility = View.GONE
                        
                        if (response.isSuccessful && response.body()?.success == true) {
                            // Clear cart
                            clearCartOnServer()
                            
                            Toast.makeText(this@VNPayPaymentActivity, "Thanh toán và đặt hàng thành công!", Toast.LENGTH_LONG).show()
                            navigateToOrderSuccess()
                        } else {
                            Toast.makeText(this@VNPayPaymentActivity, "Thanh toán thành công nhưng tạo đơn thất bại: ${response.body()?.message}", Toast.LENGTH_LONG).show()
                            navigateToOrderSuccess()
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<Order>>, t: Throwable) {
                        progressBar.visibility = View.GONE
                        Log.e("VNPayPayment", "Error creating order after payment", t)
                        Toast.makeText(this@VNPayPaymentActivity, "Thanh toán thành công nhưng lỗi tạo đơn: ${t.message}", Toast.LENGTH_LONG).show()
                        navigateToOrderSuccess()
                    }
                })
        } catch (e: Exception) {
            progressBar.visibility = View.GONE
            Log.e("VNPayPayment", "Error parsing order request", e)
            Toast.makeText(this, "Thanh toán thành công!", Toast.LENGTH_LONG).show()
            navigateToOrderSuccess()
        }
    }
    
    private fun clearCartOnServer() {
        val userId = SessionManager(this).getUserId()
        if (userId == -1) return
        
        RetrofitClient.getInstance(this).apiService.clearCart(userId.toLong())
            .enqueue(object : Callback<ApiResponse<Void>> {
                override fun onResponse(call: Call<ApiResponse<Void>>, response: Response<ApiResponse<Void>>) {
                    Log.d("VNPayPayment", "Cart cleared: ${response.isSuccessful}")
                }
                override fun onFailure(call: Call<ApiResponse<Void>>, t: Throwable) {
                    Log.e("VNPayPayment", "Error clearing cart", t)
                }
            })
    }
    
    private fun handlePaymentFailure() {
        Toast.makeText(this, "Thanh toán thất bại, vui lòng thử lại!", Toast.LENGTH_SHORT).show()
        webView.clearHistory()
        webView.clearCache(true)
        paymentUrl?.let { webView.loadUrl(it) }
    }
    
    private fun handlePaymentExit() {
        Toast.makeText(this, "Đã hủy thanh toán", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun extractResponseCode(url: String): String? {
        return try {
            val uri = android.net.Uri.parse(url)
            uri.getQueryParameter("vnp_ResponseCode")
        } catch (e: Exception) {
            Log.e("VNPayPayment", "Error parsing response code", e)
            null
        }
    }

    private fun navigateToOrderSuccess() {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("SELECTED_ITEM", R.id.nav_order)
            putExtra("PAYMENT_SUCCESS", true)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }
}
