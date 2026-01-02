package com.example.doan.Utils

/**
 * ✅ SECURITY: Constants để validate input và prevent abuse
 */
object ValidationConstants {
    // Số lượng sản phẩm
    const val MAX_QUANTITY_PER_ITEM = 99
    const val MIN_QUANTITY = 1
    
    // Giá tiền (để detect giá trị bất thường)
    const val MAX_REASONABLE_PRICE = 1_000_000.0  // 1 triệu VNĐ/món
    const val MIN_REASONABLE_PRICE = 1_000.0      // 1k VNĐ/món
    
    // Order
    const val MAX_ITEMS_PER_ORDER = 50
    const val MAX_TOTAL_AMOUNT = 50_000_000.0  // 50 triệu VNĐ
    
    // Rate limiting
    const val OTP_COOLDOWN_MS = 60_000L  // 60 giây
    const val SPIN_COOLDOWN_MS = 5_000L   // 5 giây giữa các lần quay
    
    /**
     * Validate số lượng sản phẩm
     */
    fun isValidQuantity(quantity: Int): Boolean {
        return quantity in MIN_QUANTITY..MAX_QUANTITY_PER_ITEM
    }
    
    /**
     * Validate giá tiền có hợp lý không
     */
    fun isReasonablePrice(price: Double): Boolean {
        return price in MIN_REASONABLE_PRICE..MAX_REASONABLE_PRICE
    }
    
    /**
     * Validate tổng số lượng items trong order
     */
    fun isValidOrderSize(itemCount: Int): Boolean {
        return itemCount in 1..MAX_ITEMS_PER_ORDER
    }
    
    /**
     * Validate tổng tiền đơn hàng
     */
    fun isValidTotalAmount(amount: Double): Boolean {
        return amount > 0 && amount <= MAX_TOTAL_AMOUNT
    }
}
