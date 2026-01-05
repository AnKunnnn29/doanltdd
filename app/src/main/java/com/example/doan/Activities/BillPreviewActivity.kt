package com.example.doan.Activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Adapters.BillItemAdapter
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.BillPreview
import com.example.doan.Models.CreateOrderRequest
import com.example.doan.Models.Order
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.InAppNotification
import com.example.doan.Utils.LoadingDialog
import com.example.doan.Utils.SeasonalEffectManager
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class BillPreviewActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "BillPreviewActivity"
        const val EXTRA_ORDER_REQUEST = "ORDER_REQUEST"
        const val EXTRA_CART_ITEM_IDS = "CART_ITEM_IDS"
        const val EXTRA_SHIPPING_FEE = "SHIPPING_FEE"
    }

    // Views
    private lateinit var tvCustomerName: TextView
    private lateinit var tvCustomerPhone: TextView
    private lateinit var tvCustomerEmail: TextView
    private lateinit var tvStoreName: TextView
    private lateinit var tvOrderType: TextView
    private lateinit var tvDeliveryAddress: TextView
    private lateinit var tvPaymentMethod: TextView
    private lateinit var rvBillItems: RecyclerView
    private lateinit var tvSubtotal: TextView
    private lateinit var llShippingFee: LinearLayout
    private lateinit var tvShippingFee: TextView
    private lateinit var llVoucherDiscount: LinearLayout
    private lateinit var tvVoucherLabel: TextView
    private lateinit var tvVoucherDiscount: TextView
    private lateinit var llTierDiscount: LinearLayout
    private lateinit var tvTierLabel: TextView
    private lateinit var tvTierDiscount: TextView
    private lateinit var tvFinalPrice: TextView
    private lateinit var btnConfirmOrder: MaterialButton

    // Data
    private var orderRequest: CreateOrderRequest? = null
    private var cartItemIds: LongArray? = null
    private var billPreview: BillPreview? = null
    private var clientShippingFee: Int = 0 // Phí ship từ client

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bill_preview)

        initViews()
        parseIntent()
        loadBillPreview()
    }

    private fun initViews() {
        // Toolbar
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).setNavigationOnClickListener {
            finish()
        }

        // Customer info
        tvCustomerName = findViewById(R.id.tv_customer_name)
        tvCustomerPhone = findViewById(R.id.tv_customer_phone)
        tvCustomerEmail = findViewById(R.id.tv_customer_email)

        // Order info
        tvStoreName = findViewById(R.id.tv_store_name)
        tvOrderType = findViewById(R.id.tv_order_type)
        tvDeliveryAddress = findViewById(R.id.tv_delivery_address)
        tvPaymentMethod = findViewById(R.id.tv_payment_method)

        // Items
        rvBillItems = findViewById(R.id.rv_bill_items)
        rvBillItems.layoutManager = LinearLayoutManager(this)

        // Price summary
        tvSubtotal = findViewById(R.id.tv_subtotal)
        llShippingFee = findViewById(R.id.ll_shipping_fee)
        tvShippingFee = findViewById(R.id.tv_shipping_fee)
        llVoucherDiscount = findViewById(R.id.ll_voucher_discount)
        tvVoucherLabel = findViewById(R.id.tv_voucher_label)
        tvVoucherDiscount = findViewById(R.id.tv_voucher_discount)
        llTierDiscount = findViewById(R.id.ll_tier_discount)
        tvTierLabel = findViewById(R.id.tv_tier_label)
        tvTierDiscount = findViewById(R.id.tv_tier_discount)
        tvFinalPrice = findViewById(R.id.tv_final_price)

        // Button
        btnConfirmOrder = findViewById(R.id.btn_confirm_order)
        btnConfirmOrder.setOnClickListener {
            confirmOrder()
        }
    }

    private fun parseIntent() {
        val orderRequestJson = intent.getStringExtra(EXTRA_ORDER_REQUEST)
        if (!orderRequestJson.isNullOrEmpty()) {
            orderRequest = Gson().fromJson(orderRequestJson, CreateOrderRequest::class.java)
        }
        cartItemIds = intent.getLongArrayExtra(EXTRA_CART_ITEM_IDS)
        clientShippingFee = intent.getIntExtra(EXTRA_SHIPPING_FEE, 0)

        if (orderRequest == null) {
            Toast.makeText(this, "Lỗi: Không có thông tin đơn hàng", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadBillPreview() {
        val request = orderRequest ?: return

        val loadingDialog = LoadingDialog(this)
        loadingDialog.show("Đang tải thông tin đơn hàng...")

        RetrofitClient.getInstance(this).apiService.previewBill(request)
            .enqueue(object : Callback<ApiResponse<BillPreview>> {
                override fun onResponse(
                    call: Call<ApiResponse<BillPreview>>,
                    response: Response<ApiResponse<BillPreview>>
                ) {
                    loadingDialog.dismiss()

                    if (response.isSuccessful && response.body()?.success == true) {
                        billPreview = response.body()?.data
                        displayBillPreview()
                    } else {
                        val errorMsg = response.body()?.message ?: "Không thể tải thông tin đơn hàng"
                        Toast.makeText(this@BillPreviewActivity, errorMsg, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<BillPreview>>, t: Throwable) {
                    loadingDialog.dismiss()
                    Log.e(TAG, "Error loading bill preview", t)
                    Toast.makeText(this@BillPreviewActivity, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
    }

    private fun displayBillPreview() {
        val bill = billPreview ?: return

        // Customer info
        tvCustomerName.text = "Họ tên: ${bill.customerName ?: "N/A"}"
        tvCustomerPhone.text = "SĐT: ${bill.customerPhone ?: "Chưa cập nhật"}"
        tvCustomerEmail.text = "Email: ${bill.customerEmail ?: "N/A"}"

        // Order info
        tvStoreName.text = "Cửa hàng: ${bill.storeName ?: "N/A"}"
        tvOrderType.text = "Loại đơn: ${if (bill.orderType == "DELIVERY") "Giao hàng" else "Lấy tại cửa hàng"}"
        
        // Địa chỉ - Ưu tiên từ request nếu là DELIVERY
        val displayAddress = if (bill.orderType == "DELIVERY") {
            orderRequest?.address ?: bill.deliveryAddress ?: "N/A"
        } else {
            bill.deliveryAddress ?: "Tại Cửa Hàng"
        }
        tvDeliveryAddress.text = "Địa chỉ: $displayAddress"
        tvPaymentMethod.text = "Thanh toán: ${bill.paymentMethod ?: "N/A"}"

        // Items
        bill.items?.let { items ->
            rvBillItems.adapter = BillItemAdapter(items)
        }

        // Price summary
        val subtotal = bill.subtotal ?: 0.0
        tvSubtotal.text = formatPrice(subtotal)

        // Shipping fee - Ưu tiên từ client, nếu không có thì lấy từ backend
        val shippingFee = if (clientShippingFee > 0) clientShippingFee.toDouble() else (bill.shippingFee ?: 0.0)
        val originalShippingFee = bill.originalShippingFee ?: shippingFee
        val isFreeShipping = bill.freeShipping
        
        if (isFreeShipping && originalShippingFee > 0) {
            // Hiển thị free ship với giá gốc bị gạch
            llShippingFee.visibility = View.VISIBLE
            tvShippingFee.text = "🎉 MIỄN PHÍ (${formatPrice(originalShippingFee)})"
            tvShippingFee.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            
            // Hiển thị lý do free ship nếu có
            bill.freeShippingReason?.let { reason ->
                Log.d(TAG, "Free shipping reason: $reason")
            }
        } else if (shippingFee > 0) {
            llShippingFee.visibility = View.VISIBLE
            tvShippingFee.text = formatPrice(shippingFee)
            tvShippingFee.setTextColor(resources.getColor(android.R.color.black, null))
        } else {
            llShippingFee.visibility = View.GONE
        }

        // Voucher discount
        val voucherDiscount = bill.voucherDiscount ?: 0.0
        if (voucherDiscount > 0) {
            llVoucherDiscount.visibility = View.VISIBLE
            val voucherLabel = if (!bill.promotionCode.isNullOrEmpty()) {
                "🎫 Giảm giá voucher (${bill.promotionCode}):"
            } else {
                "🎫 Giảm giá voucher:"
            }
            tvVoucherLabel.text = voucherLabel
            tvVoucherDiscount.text = "-${formatPrice(voucherDiscount)}"
        } else {
            llVoucherDiscount.visibility = View.GONE
        }

        // Tier discount
        val tierDiscount = bill.tierDiscountAmount ?: 0.0
        if (tierDiscount > 0) {
            llTierDiscount.visibility = View.VISIBLE
            val tierLabel = "👑 Ưu đãi hạng ${bill.tierName ?: "BRONZE"}:"
            tvTierLabel.text = tierLabel
            tvTierDiscount.text = "-${formatPrice(tierDiscount)}"
        } else {
            llTierDiscount.visibility = View.GONE
        }

        // Final price - Sử dụng shippingFee từ backend (đã áp dụng free ship nếu có)
        val actualShippingFee = bill.shippingFee ?: 0.0  // Đã được backend tính toán free ship
        val finalPrice = subtotal + actualShippingFee - voucherDiscount - tierDiscount
        tvFinalPrice.text = formatPrice(maxOf(0.0, finalPrice))
    }

    private fun formatPrice(price: Double): String {
        return String.format(Locale.getDefault(), "%,.0f VNĐ", price)
    }

    private fun confirmOrder() {
        val request = orderRequest ?: return

        val loadingDialog = LoadingDialog(this)
        loadingDialog.show("Đang xử lý đơn hàng...")

        // Xử lý theo payment method
        when (request.paymentMethod) {
            "VNPAY" -> handleVNPayPayment(request, loadingDialog)
            "VIETQR" -> handleVietQRPayment(request, loadingDialog)
            "MOMO" -> handleMoMoPayment(request, loadingDialog)
            "PAYPAL" -> handlePayPalPayment(request, loadingDialog)
            else -> createCODOrder(request, loadingDialog)
        }
    }

    private fun createCODOrder(request: CreateOrderRequest, loadingDialog: LoadingDialog) {
        RetrofitClient.getInstance(this).apiService.createOrder(request)
            .enqueue(object : Callback<ApiResponse<Order>> {
                override fun onResponse(
                    call: Call<ApiResponse<Order>>,
                    response: Response<ApiResponse<Order>>
                ) {
                    loadingDialog.dismiss()

                    if (response.isSuccessful && response.body()?.success == true) {
                        val order = response.body()?.data

                        // Remove cart items
                        removeCartItems()

                        // Show success
                        SeasonalEffectManager.showConfetti(3000L, 200)
                        InAppNotification.orderSuccess(
                            this@BillPreviewActivity,
                            order?.id?.toString() ?: "N/A"
                        )

                        // Navigate to orders
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            navigateToOrders()
                        }, 2500)
                    } else {
                        val errorMsg = response.body()?.message ?: "Đặt hàng thất bại"
                        InAppNotification.error(this@BillPreviewActivity, "Đặt hàng thất bại", errorMsg)
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Order>>, t: Throwable) {
                    loadingDialog.dismiss()
                    InAppNotification.error(
                        this@BillPreviewActivity,
                        "Lỗi kết nối",
                        t.message ?: "Không thể kết nối đến server"
                    )
                }
            })
    }

    private fun handleVNPayPayment(request: CreateOrderRequest, loadingDialog: LoadingDialog) {
        val totalAmount = (billPreview?.finalPrice ?: 0.0).toLong()

        RetrofitClient.getInstance(this).apiService.createVNPayPaymentWithAmount(totalAmount, "Thanh toan UTE Tea")
            .enqueue(object : Callback<ApiResponse<com.example.doan.Models.VNPayPaymentResponse>> {
                override fun onResponse(
                    call: Call<ApiResponse<com.example.doan.Models.VNPayPaymentResponse>>,
                    response: Response<ApiResponse<com.example.doan.Models.VNPayPaymentResponse>>
                ) {
                    loadingDialog.dismiss()

                    if (response.isSuccessful && response.body()?.success == true) {
                        val paymentUrl = response.body()?.data?.paymentUrl
                        if (!paymentUrl.isNullOrEmpty()) {
                            val intent = Intent(this@BillPreviewActivity, VNPayPaymentActivity::class.java)
                            intent.putExtra("PAYMENT_URL", paymentUrl)
                            intent.putExtra("ORDER_REQUEST", Gson().toJson(request))
                            intent.putExtra("CART_ITEM_IDS", cartItemIds)
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        Toast.makeText(this@BillPreviewActivity, "Lỗi tạo thanh toán VNPay", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<com.example.doan.Models.VNPayPaymentResponse>>, t: Throwable) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@BillPreviewActivity, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun handleVietQRPayment(request: CreateOrderRequest, loadingDialog: LoadingDialog) {
        loadingDialog.dismiss()
        val totalAmount = (billPreview?.finalPrice ?: 0.0).toLong()

        val intent = Intent(this, VietQRActivity::class.java).apply {
            putExtra("ORDER_ID", System.currentTimeMillis())
            putExtra("TOTAL_AMOUNT", totalAmount.toDouble())
            putExtra("ORDER_REQUEST", Gson().toJson(request))
            putExtra("CART_ITEM_IDS", cartItemIds)
        }
        startActivity(intent)
    }

    private fun handleMoMoPayment(request: CreateOrderRequest, loadingDialog: LoadingDialog) {
        val totalAmount = (billPreview?.finalPrice ?: 0.0).toLong()

        com.example.doan.Services.PaymentService.createMoMoPayment(
            this,
            totalAmount,
            "Thanh toan UTE Tea",
            object : com.example.doan.Services.PaymentService.PaymentCallback {
                override fun onSuccess(paymentUrl: String, transactionId: String?) {
                    loadingDialog.dismiss()

                    val intent = Intent(this@BillPreviewActivity, MoMoPaymentActivity::class.java)
                    intent.putExtra("PAYMENT_URL", paymentUrl)
                    intent.putExtra("MOMO_ORDER_ID", transactionId)
                    intent.putExtra("ORDER_REQUEST", Gson().toJson(request))
                    intent.putExtra("CART_ITEM_IDS", cartItemIds)
                    startActivity(intent)
                    finish()
                }

                override fun onError(message: String) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@BillPreviewActivity, "Lỗi MoMo: $message", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun handlePayPalPayment(request: CreateOrderRequest, loadingDialog: LoadingDialog) {
        val totalAmountVND = (billPreview?.finalPrice ?: 0.0).toLong()
        // Convert VND to USD for PayPal
        val totalAmountUSD = com.example.doan.Services.PaymentService.convertVNDtoUSD(totalAmountVND)

        com.example.doan.Services.PaymentService.createPayPalPayment(
            this,
            totalAmountUSD,
            "USD",
            "Thanh toan UTE Tea",
            object : com.example.doan.Services.PaymentService.PaymentCallback {
                override fun onSuccess(paymentUrl: String, transactionId: String?) {
                    loadingDialog.dismiss()

                    val intent = Intent(this@BillPreviewActivity, PayPalPaymentActivity::class.java)
                    intent.putExtra("PAYMENT_URL", paymentUrl)
                    intent.putExtra("ORDER_REQUEST", Gson().toJson(request))
                    intent.putExtra("CART_ITEM_IDS", cartItemIds)
                    startActivity(intent)
                    finish()
                }

                override fun onError(message: String) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@BillPreviewActivity, "Lỗi PayPal: $message", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun removeCartItems() {
        cartItemIds?.forEach { cartItemId ->
            RetrofitClient.getInstance(this).apiService.removeCartItem(cartItemId)
                .enqueue(object : Callback<ApiResponse<Void>> {
                    override fun onResponse(call: Call<ApiResponse<Void>>, response: Response<ApiResponse<Void>>) {
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
}
