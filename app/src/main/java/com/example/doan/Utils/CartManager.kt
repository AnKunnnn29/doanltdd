package com.example.doan.Utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.doan.Models.CartItem
import com.example.doan.Models.Product
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * ⚠️ WARNING: DEPRECATED - Có xung đột với Cart API từ server
 * 
 * Class này quản lý giỏ hàng LOCAL (SharedPreferences) nhưng app cũng sử dụng
 * Cart API từ server, gây ra mất đồng bộ dữ liệu nghiêm trọng.
 * 
 * KHUYẾN NGHỊ:
 * - Loại bỏ CartManager này hoàn toàn
 * - Chỉ sử dụng Cart API từ server (CartActivity đang dùng)
 * - Hoặc đồng bộ 2 chiều giữa local và server
 * 
 * HIỆN TẠI: Class này vẫn được giữ lại để tương thích với code cũ,
 * nhưng NÊN TRÁNH sử dụng trong code mới.
 */
class CartManager private constructor() {
    
    private var cartItems: MutableList<CartItem> = mutableListOf()
    private var context: Context? = null
    private var lastActivityTime: Long = System.currentTimeMillis()
    private val gson = Gson()
    
    fun init(context: Context) {
        this.context = context.applicationContext
        loadCart()
        loadLastActivityTime()
        clearCartIfExpired()
    }
    
    fun addToCart(product: Product, quantity: Int, sizeName: String, unitPrice: Double) {
        // Kiểm tra xem món này (cùng ID và cùng Size) đã có trong giỏ chưa
        cartItems.find { 
            it.drinkId == product.id.toLong() && it.sizeName == sizeName 
        }?.let { item ->
            item.quantity = (item.quantity ?: 0) + quantity
            item.totalPrice = (item.unitPrice ?: 0.0) * (item.quantity ?: 0)
            updateLastActivityTime()
            saveCart()
            return
        }
        
        // Nếu chưa có, thêm mới
        val newItem = CartItem(
            drinkId = product.id.toLong(),
            drinkName = product.name,
            drinkImage = product.imageUrl,
            sizeName = sizeName,
            quantity = quantity,
            unitPrice = unitPrice,
            totalPrice = unitPrice * quantity
        )
        
        cartItems.add(newItem)
        updateLastActivityTime()
        saveCart()
        Log.i(TAG, "Added to cart: ${product.name} x$quantity")
    }
    
    fun updateQuantity(position: Int, newQuantity: Int) {
        if (position in cartItems.indices) {
            if (newQuantity <= 0) {
                cartItems.removeAt(position)
            } else {
                cartItems[position].apply {
                    quantity = newQuantity
                    totalPrice = (unitPrice ?: 0.0) * newQuantity
                }
            }
            updateLastActivityTime()
            saveCart()
        }
    }
    
    fun removeItem(position: Int) {
        if (position in cartItems.indices) {
            cartItems.removeAt(position)
            updateLastActivityTime()
            saveCart()
            Log.i(TAG, "Removed item at position: $position")
        }
    }
    
    fun getCartItems(): List<CartItem> {
        clearCartIfExpired()
        return cartItems
    }
    
    fun clearCart() {
        cartItems.clear()
        saveCart()
        Log.i(TAG, "Cart cleared")
    }
    
    fun getTotalPrice(): Double {
        return cartItems.sumOf { it.totalPrice ?: 0.0 }
    }
    
    fun getItemCount(): Int = cartItems.size
    
    fun updateLastActivityTime() {
        lastActivityTime = System.currentTimeMillis()
        saveLastActivityTime()
    }
    
    fun clearCartIfExpired() {
        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - lastActivityTime
        
        if (timeDiff > CART_TIMEOUT_MS) {
            Log.w(TAG, "Cart expired after ${timeDiff / 1000} seconds. Clearing cart.")
            clearCart()
        }
    }
    
    private fun saveCart() {
        context?.let { ctx ->
            try {
                val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val json = gson.toJson(cartItems)
                prefs.edit().putString(KEY_CART_ITEMS, json).apply()
                Log.d(TAG, "Cart saved: ${cartItems.size} items")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving cart: ${e.message}")
            }
        } ?: Log.e(TAG, "Context is null, cannot save cart")
    }
    
    private fun loadCart() {
        context?.let { ctx ->
            try {
                val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val json = prefs.getString(KEY_CART_ITEMS, null)
                
                if (!json.isNullOrEmpty()) {
                    val type = object : TypeToken<MutableList<CartItem>>() {}.type
                    cartItems = gson.fromJson(json, type) ?: mutableListOf()
                    Log.d(TAG, "Cart loaded: ${cartItems.size} items")
                } else {
                    cartItems = mutableListOf()
                    Log.d(TAG, "No saved cart found")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading cart: ${e.message}")
                cartItems = mutableListOf()
            }
        } ?: Log.e(TAG, "Context is null, cannot load cart")
    }
    
    private fun saveLastActivityTime() {
        context?.let { ctx ->
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putLong(KEY_LAST_ACTIVITY, lastActivityTime).apply()
        }
    }
    
    private fun loadLastActivityTime() {
        context?.let { ctx ->
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            lastActivityTime = prefs.getLong(KEY_LAST_ACTIVITY, System.currentTimeMillis())
        } ?: run {
            lastActivityTime = System.currentTimeMillis()
        }
    }
    
    companion object {
        private const val TAG = "CartManager"
        private const val PREFS_NAME = "CartPrefs"
        private const val KEY_CART_ITEMS = "cart_items"
        private const val KEY_LAST_ACTIVITY = "last_activity_time"
        private const val CART_TIMEOUT_MS = 5 * 60 * 1000L // 5 phút
        
        @Volatile
        private var instance: CartManager? = null
        
        fun getInstance(): CartManager {
            return instance ?: synchronized(this) {
                instance ?: CartManager().also { instance = it }
            }
        }
    }
}
