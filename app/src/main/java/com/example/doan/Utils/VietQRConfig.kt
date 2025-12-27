package com.example.doan.Utils

/**
 * Configuration for VietQR payment integration
 */
object VietQRConfig {
    // Bank information
    const val BANK_ID = "MB"
    const val ACCOUNT_NUMBER = "87343556868"
    const val ACCOUNT_NAME = "UTE Tea Shop"
    
    // VietQR API configuration
    const val VIETQR_BASE_URL = "https://img.vietqr.io/image/"
    const val TEMPLATE = "compact2"
    
    // QR Code dimensions
    const val QR_WIDTH = 300
    const val QR_HEIGHT = 300
}
