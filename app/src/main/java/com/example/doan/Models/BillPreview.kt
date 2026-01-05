package com.example.doan.Models

import java.io.Serializable

/**
 * Model cho Bill Preview - hiển thị trước khi thanh toán
 */
data class BillPreview(
    // Thông tin khách hàng
    val customerName: String? = null,
    val customerPhone: String? = null,
    val customerEmail: String? = null,
    
    // Thông tin cửa hàng
    val storeId: Long? = null,
    val storeName: String? = null,
    val storeAddress: String? = null,
    
    // Thông tin đơn hàng
    val orderType: String? = null,       // DELIVERY / PICKUP
    val deliveryAddress: String? = null,
    val pickupTime: String? = null,
    val paymentMethod: String? = null,
    
    // Chi tiết sản phẩm
    val items: List<BillItem>? = null,
    
    // Tổng tiền
    val subtotal: Double? = null,            // Tổng tiền hàng
    
    // Phí giao hàng (GHN)
    val shippingFee: Double? = null,             // Phí giao hàng (sau khi áp dụng free ship)
    val originalShippingFee: Double? = null,     // Phí giao hàng gốc (trước khi áp dụng free ship)
    val ghnDistrictId: Int? = null,              // District ID cho GHN
    val ghnWardCode: String? = null,             // Ward code cho GHN
    val freeShipping: Boolean = false,           // Có được miễn phí ship không
    val freeShippingReason: String? = null,      // Lý do miễn phí ship
    
    // Chi tiết giảm giá
    val promotionCode: String? = null,       // Mã voucher (nếu có)
    val voucherDiscount: Double? = null,     // Số tiền giảm từ voucher
    val tierName: String? = null,            // Tên hạng thành viên
    val tierDiscountAmount: Double? = null,  // Số tiền giảm từ hạng thành viên
    
    val totalDiscount: Double? = null,       // Tổng giảm giá
    val finalPrice: Double? = null           // Thành tiền (subtotal + shippingFee - totalDiscount)
) : Serializable

data class BillItem(
    val drinkName: String? = null,
    val drinkImage: String? = null,
    val sizeName: String? = null,
    val toppings: List<String>? = null,
    val quantity: Int? = null,
    val unitPrice: Double? = null,       // Giá 1 sản phẩm (đã bao gồm size + topping)
    val totalPrice: Double? = null,      // Giá x số lượng
    val note: String? = null
) : Serializable
