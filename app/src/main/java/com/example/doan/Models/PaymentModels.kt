package com.example.doan.Models

/**
 * Payment Models cho các cổng thanh toán
 * MoMo, ZaloPay, PayPal
 */

// ==================== MOMO ====================
data class MoMoPaymentRequest(
    val amount: String,
    val orderInfo: String? = null
)

data class MoMoPaymentResponse(
    val partnerCode: String? = null,
    val orderId: String? = null,
    val requestId: String? = null,
    val amount: Long? = null,
    val responseTime: Long? = null,
    val message: String? = null,
    val resultCode: Int? = null,
    val payUrl: String? = null,
    val deeplink: String? = null,
    val qrCodeUrl: String? = null,
    val error: String? = null
)

// ==================== PAYPAL ====================
data class PayPalPaymentRequest(
    val total: String,
    val currency: String = "USD",
    val description: String? = null,
    val orderId: Long? = null
)

data class PayPalPaymentResponse(
    val approvalUrl: String? = null
)

// ==================== PAYMENT METHOD ENUM ====================
enum class PaymentMethod {
    CASH,
    COD,
    VNPAY,
    MOMO,
    ZALOPAY,
    PAYPAL,
    VIETQR
}
