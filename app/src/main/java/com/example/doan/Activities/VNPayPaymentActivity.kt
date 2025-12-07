package com.example.doan.Activities

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.doan.R

class VNPayPaymentActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private var paymentUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vnpay_payment)

        paymentUrl = intent.getStringExtra("PAYMENT_URL")
        
        if (paymentUrl.isNullOrEmpty()) {
            Toast.makeText(this, "URL thanh toán không hợp lệ", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupWebView()
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
                
                // Kiểm tra callback URL
                Log.d("VNPayPayment", "Loading URL: $url")
                if (url != null && url.contains("/api/vnpay/callback")) {
                    handlePaymentCallback(url)
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                Log.d("VNPayPayment", "Navigating to: $url")
                
                // Kiểm tra callback URL
                if (url != null && url.contains("/api/vnpay/callback")) {
                    handlePaymentCallback(url)
                    return true
                }
                
                return false
            }
        }
    }

    private fun loadPaymentUrl() {
        paymentUrl?.let {
            Log.d("VNPayPayment", "Loading payment URL: $it")
            webView.loadUrl(it)
        }
    }

    private fun handlePaymentCallback(url: String) {
        // Parse response code từ URL
        val responseCode = extractResponseCode(url)
        
        when (responseCode) {
            "00" -> {
                // Thanh toán thành công
                Toast.makeText(this, "Thanh toán thành công!", Toast.LENGTH_LONG).show()
                navigateToOrderSuccess()
            }
            else -> {
                // Thanh toán thất bại
                Toast.makeText(this, "Thanh toán thất bại hoặc bị hủy", Toast.LENGTH_LONG).show()
                navigateToOrderFailed()
            }
        }
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

    private fun navigateToOrderFailed() {
        finish()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
