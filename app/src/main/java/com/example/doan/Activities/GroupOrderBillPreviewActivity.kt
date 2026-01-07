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
import com.example.doan.Models.*
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

/**
 * Activity hiển thị bill preview cho đơn hàng nhóm
 * Tương tự BillPreviewActivity nhưng dành cho Group Order
 */
class GroupOrderBillPreviewActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "GroupOrderBillPreview"
        const val EXTRA_GROUP_ORDER_ID = "GROUP_ORDER_ID"
        const val EXTRA_PAYMENT_METHOD = "PAYMENT_METHOD"
        const val EXTRA_PROMOTION_CODE = "PROMOTION_CODE"
        const val EXTRA_SPIN_VOUCHER_CODE = "SPIN_VOUCHER_CODE"
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
    private var groupOrderId: Long = 0
    private var paymentMethod: String = "COD"
    private var promotionCode: String? = null
    private var spinVoucherCode: String? = null
    private var shippingFee: Int = 0
    private var billPreview: BillPreview? = null

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
        groupOrderId = intent.getLongExtra(EXTRA_GROUP_ORDER_ID, 0)
        paymentMethod = intent.getStringExtra(EXTRA_PAYMENT_METHOD) ?: "COD"
        promotionCode = intent.getStringExtra(EXTRA_PROMOTION_CODE)
        spinVoucherCode = intent.getStringExtra(EXTRA_SPIN_VOUCHER_CODE)
        shippingFee = intent.getIntExtra(EXTRA_SHIPPING_FEE, 0)

        if (groupOrderId == 0L) {
            Toast.makeText(this, "Lỗi: Không có thông tin đơn hàng nhóm", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadBillPreview() {
        val loadingDialog = LoadingDialog(this)
        loadingDialog.show("Đang tải thông tin đơn hàng...")

        val request = PreviewGroupOrderBillRequest(
            paymentMethod = paymentMethod,
            promotionCode = promotionCode,
            spinVoucherCode = spinVoucherCode,
            shippingFee = if (shippingFee > 0) shippingFee else null
        )

        RetrofitClient.getInstance(this).apiService.previewGroupOrderBill(groupOrderId, request)
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
                        Toast.makeText(this@GroupOrderBillPreviewActivity, errorMsg, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<BillPreview>>, t: Throwable) {
                    loadingDialog.dismiss()
                    Log.e(TAG, "Error loading bill preview", t)
                    Toast.makeText(this@GroupOrderBillPreviewActivity, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
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
        tvDeliveryAddress.text = "Địa chỉ: ${bill.deliveryAddress ?: "Tại Cửa Hàng"}"
        tvPaymentMethod.text = "Thanh toán: ${bill.paymentMethod ?: getPaymentMethodDisplay(paymentMethod)}"

        // Items
        bill.items?.let { items ->
            rvBillItems.adapter = BillItemAdapter(items)
        }

        // Price summary
        val subtotal = bill.subtotal ?: 0.0
        tvSubtotal.text = formatPrice(subtotal)

        // Shipping fee
        val displayShippingFee = bill.shippingFee ?: 0.0
        val originalShippingFee = bill.originalShippingFee ?: displayShippingFee
        val isFreeShipping = bill.freeShipping

        if (isFreeShipping && originalShippingFee > 0) {
            llShippingFee.visibility = View.VISIBLE
            tvShippingFee.text = "🎉 MIỄN PHÍ (${formatPrice(originalShippingFee)})"
            tvShippingFee.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
        } else if (displayShippingFee > 0) {
            llShippingFee.visibility = View.VISIBLE
            tvShippingFee.text = formatPrice(displayShippingFee)
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

        // Final price
        tvFinalPrice.text = formatPrice(bill.finalPrice ?: 0.0)
    }

    private fun getPaymentMethodDisplay(method: String): String {
        return when (method) {
            "COD" -> "Tiền mặt"
            "VNPAY" -> "VNPay"
            "MOMO" -> "MoMo"
            "VIETQR" -> "VietQR"
            "PAYPAL" -> "PayPal"
            else -> method
        }
    }

    private fun formatPrice(price: Double): String {
        return String.format(Locale.getDefault(), "%,.0f VNĐ", price)
    }

    private fun confirmOrder() {
        val loadingDialog = LoadingDialog(this)
        loadingDialog.show("Đang xử lý đơn hàng...")

        // Xử lý theo payment method
        when (paymentMethod) {
            "VNPAY" -> handleVNPayPayment(loadingDialog)
            "VIETQR" -> handleVietQRPayment(loadingDialog)
            "MOMO" -> handleMoMoPayment(loadingDialog)
            "PAYPAL" -> handlePayPalPayment(loadingDialog)
            else -> createCODOrder(loadingDialog)
        }
    }

    private fun createCODOrder(loadingDialog: LoadingDialog) {
        val request = CheckoutGroupOrderRequest(
            paymentMethod = paymentMethod,
            promotionCode = promotionCode,
            spinVoucherCode = spinVoucherCode
        )

        RetrofitClient.getInstance(this).apiService.checkoutGroupOrder(groupOrderId, request)
            .enqueue(object : Callback<ApiResponse<Order>> {
                override fun onResponse(
                    call: Call<ApiResponse<Order>>,
                    response: Response<ApiResponse<Order>>
                ) {
                    loadingDialog.dismiss()

                    if (response.isSuccessful && response.body()?.success == true) {
                        val order = response.body()?.data

                        // Show success
                        SeasonalEffectManager.showConfetti(3000L, 200)
                        InAppNotification.orderSuccess(
                            this@GroupOrderBillPreviewActivity,
                            order?.id?.toString() ?: "N/A"
                        )

                        // Navigate to Home after delay
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            navigateToHome()
                        }, 2500)
                    } else {
                        val errorMsg = response.body()?.message ?: "Đặt hàng thất bại"
                        InAppNotification.error(this@GroupOrderBillPreviewActivity, "Đặt hàng thất bại", errorMsg)
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Order>>, t: Throwable) {
                    loadingDialog.dismiss()
                    InAppNotification.error(
                        this@GroupOrderBillPreviewActivity,
                        "Lỗi kết nối",
                        t.message ?: "Không thể kết nối đến server"
                    )
                }
            })
    }

    private fun handleVNPayPayment(loadingDialog: LoadingDialog) {
        val totalAmount = (billPreview?.finalPrice ?: 0.0).toLong()

        RetrofitClient.getInstance(this).apiService.createVNPayPaymentWithAmount(totalAmount, "Thanh toan don nhom UTE Tea")
            .enqueue(object : Callback<ApiResponse<VNPayPaymentResponse>> {
                override fun onResponse(
                    call: Call<ApiResponse<VNPayPaymentResponse>>,
                    response: Response<ApiResponse<VNPayPaymentResponse>>
                ) {
                    loadingDialog.dismiss()

                    if (response.isSuccessful && response.body()?.success == true) {
                        val paymentUrl = response.body()?.data?.paymentUrl
                        if (!paymentUrl.isNullOrEmpty()) {
                            val checkoutRequest = CheckoutGroupOrderRequest(
                                paymentMethod = paymentMethod,
                                promotionCode = promotionCode,
                                spinVoucherCode = spinVoucherCode
                            )
                            
                            val intent = Intent(this@GroupOrderBillPreviewActivity, VNPayPaymentActivity::class.java)
                            intent.putExtra("PAYMENT_URL", paymentUrl)
                            intent.putExtra("GROUP_ORDER_ID", groupOrderId)
                            intent.putExtra("CHECKOUT_REQUEST", Gson().toJson(checkoutRequest))
                            intent.putExtra("IS_GROUP_ORDER", true)
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        Toast.makeText(this@GroupOrderBillPreviewActivity, "Lỗi tạo thanh toán VNPay", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<VNPayPaymentResponse>>, t: Throwable) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@GroupOrderBillPreviewActivity, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun handleVietQRPayment(loadingDialog: LoadingDialog) {
        loadingDialog.dismiss()
        val totalAmount = (billPreview?.finalPrice ?: 0.0).toLong()

        val checkoutRequest = CheckoutGroupOrderRequest(
            paymentMethod = paymentMethod,
            promotionCode = promotionCode,
            spinVoucherCode = spinVoucherCode
        )

        val intent = Intent(this, VietQRActivity::class.java).apply {
            putExtra("ORDER_ID", System.currentTimeMillis())
            putExtra("TOTAL_AMOUNT", totalAmount.toDouble())
            putExtra("GROUP_ORDER_ID", groupOrderId)
            putExtra("CHECKOUT_REQUEST", Gson().toJson(checkoutRequest))
            putExtra("IS_GROUP_ORDER", true)
        }
        startActivity(intent)
    }

    private fun handleMoMoPayment(loadingDialog: LoadingDialog) {
        val totalAmount = (billPreview?.finalPrice ?: 0.0).toLong()

        com.example.doan.Services.PaymentService.createMoMoPayment(
            this,
            totalAmount,
            "Thanh toan don nhom UTE Tea",
            object : com.example.doan.Services.PaymentService.PaymentCallback {
                override fun onSuccess(paymentUrl: String, transactionId: String?) {
                    loadingDialog.dismiss()

                    val checkoutRequest = CheckoutGroupOrderRequest(
                        paymentMethod = paymentMethod,
                        promotionCode = promotionCode,
                        spinVoucherCode = spinVoucherCode
                    )

                    val intent = Intent(this@GroupOrderBillPreviewActivity, MoMoPaymentActivity::class.java)
                    intent.putExtra("PAYMENT_URL", paymentUrl)
                    intent.putExtra("MOMO_ORDER_ID", transactionId)
                    intent.putExtra("GROUP_ORDER_ID", groupOrderId)
                    intent.putExtra("CHECKOUT_REQUEST", Gson().toJson(checkoutRequest))
                    intent.putExtra("IS_GROUP_ORDER", true)
                    startActivity(intent)
                    finish()
                }

                override fun onError(message: String) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@GroupOrderBillPreviewActivity, "Lỗi MoMo: $message", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun handlePayPalPayment(loadingDialog: LoadingDialog) {
        val totalAmountVND = (billPreview?.finalPrice ?: 0.0).toLong()
        val totalAmountUSD = com.example.doan.Services.PaymentService.convertVNDtoUSD(totalAmountVND)

        com.example.doan.Services.PaymentService.createPayPalPayment(
            this,
            totalAmountUSD,
            "USD",
            "Thanh toan don nhom UTE Tea",
            object : com.example.doan.Services.PaymentService.PaymentCallback {
                override fun onSuccess(paymentUrl: String, transactionId: String?) {
                    loadingDialog.dismiss()

                    val checkoutRequest = CheckoutGroupOrderRequest(
                        paymentMethod = paymentMethod,
                        promotionCode = promotionCode,
                        spinVoucherCode = spinVoucherCode
                    )

                    val intent = Intent(this@GroupOrderBillPreviewActivity, PayPalPaymentActivity::class.java)
                    intent.putExtra("PAYMENT_URL", paymentUrl)
                    intent.putExtra("GROUP_ORDER_ID", groupOrderId)
                    intent.putExtra("CHECKOUT_REQUEST", Gson().toJson(checkoutRequest))
                    intent.putExtra("IS_GROUP_ORDER", true)
                    startActivity(intent)
                    finish()
                }

                override fun onError(message: String) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@GroupOrderBillPreviewActivity, "Lỗi PayPal: $message", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun navigateToOrderDetail(orderId: Int?) {
        if (orderId != null) {
            val intent = Intent(this, OrderDetailActivity::class.java)
            intent.putExtra("ORDER_ID", orderId)
            startActivity(intent)
        }
        finish()
    }
    
    /**
     * Navigate về trang Home sau khi đặt hàng thành công
     */
    private fun navigateToHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}
