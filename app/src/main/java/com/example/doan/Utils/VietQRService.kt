package com.example.doan.Utils

import java.net.URLEncoder
import java.text.NumberFormat
import java.util.Locale

/**
 * Service class for VietQR payment operations
 */
object VietQRService {
    
    /**
     * Generate VietQR URL for payment
     * @param amount Payment amount in VND
     * @param orderInfo Order information/description
     * @return URL string for QR code image
     */
    fun generateQRUrl(amount: Double, orderInfo: String): String {
        val bankId = VietQRConfig.BANK_ID
        val accountNumber = VietQRConfig.ACCOUNT_NUMBER
        val template = VietQRConfig.TEMPLATE
        val accountName = URLEncoder.encode(VietQRConfig.ACCOUNT_NAME, "UTF-8")
        val encodedOrderInfo = URLEncoder.encode(orderInfo, "UTF-8")
        val amountLong = amount.toLong()
        
        return "${VietQRConfig.VIETQR_BASE_URL}$bankId-$accountNumber-$template.png" +
                "?amount=$amountLong" +
                "&addInfo=$encodedOrderInfo" +
                "&accountName=$accountName"
    }
    
    /**
     * Format currency to Vietnamese format
     * @param amount Amount to format
     * @return Formatted string with VNĐ suffix
     */
    fun formatCurrency(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        return "${formatter.format(amount)} VNĐ"
    }
    
    /**
     * Validate payment data before generating QR
     * @param amount Payment amount
     * @param orderId Order ID
     * @return true if valid, false otherwise
     */
    fun validatePaymentData(amount: Double, orderId: Long): Boolean {
        return amount > 0 && orderId > 0
    }
    
    /**
     * Generate transfer content for bank transfer
     * @param orderId Order ID
     * @return Transfer content string
     */
    fun generateTransferContent(orderId: Long): String {
        return "DH $orderId - UTE Tea"
    }
}
