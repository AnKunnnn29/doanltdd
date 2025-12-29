package com.example.doan.Models

data class VietQRPaymentData(
    val orderId: Long,
    val amount: Double,
    val orderInfo: String,
    val qrUrl: String,
    val bankInfo: BankInfo
)

data class BankInfo(
    val bankId: String = "MB",
    val accountNumber: String = "87343556868",
    val accountName: String = "UTE Tea Shop"
)

data class OrderSummaryItem(
    val drinkName: String,
    val sizeName: String,
    val quantity: Int,
    val unitPrice: Double,
    val toppings: List<String> = emptyList()
)
