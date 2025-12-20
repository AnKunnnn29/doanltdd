package com.example.doan.Activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Adapters.CartAdapter
import com.example.doan.Adapters.VoucherSelectionAdapter
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.Cart
import com.example.doan.Models.CartItem
import com.example.doan.Models.CreateOrderRequest
import com.example.doan.Models.Order
import com.example.doan.Models.Store
import com.example.doan.Models.VNPayPaymentRequest
import com.example.doan.Models.VNPayPaymentResponse
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class CartActivity : AppCompatActivity(), CartAdapter.OnCartItemChangeListener {

    private lateinit var rvCartItems: RecyclerView
    private lateinit var cartAdapter: CartAdapter
    private lateinit var tvEmptyCart: TextView
    private lateinit var tvTotalPrice: TextView
    private lateinit var btnCheckout: Button
    private lateinit var cbSelectAll: CheckBox
    private lateinit var btnDeleteSelected: Button
    private lateinit var spinnerPaymentMethod: Spinner
    private lateinit var spinnerStore: Spinner
    private lateinit var rgDeliveryType: RadioGroup
    private lateinit var rbPickup: RadioButton
    private lateinit var rbDelivery: RadioButton
    private lateinit var llDeliveryAddress: LinearLayout
    private lateinit var etDeliveryAddress: EditText
    private lateinit var llVoucherSection: LinearLayout
    private lateinit var etVoucherCode: EditText
    private lateinit var btnApplyVoucher: Button
    private lateinit var btnSelectVoucher: Button
    private lateinit var tvDiscountAmount: TextView
    private lateinit var tvFinalPrice: TextView
    
    private var cartItems = mutableListOf<CartItem>()
    private var storeList = mutableListOf<Store>()
    private var selectedStoreId: Int? = null
    private var selectedDeliveryType: String = "PICKUP" // Mặc định là đến lấy
    private val paymentMethods = listOf("COD", "VNPAY")
    private var appliedVoucher: com.example.doan.Models.Voucher? = null
    private var discountAmount: Double = 0.0
    private var createdOrderId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        initViews()
        setupRecyclerView()
        setupListeners()
        loadCart()
    }

    private fun initViews() {
        rvCartItems = findViewById(R.id.rv_cart_items)
        tvEmptyCart = findViewById(R.id.tv_empty_cart)
        tvTotalPrice = findViewById(R.id.tv_total_price_cart)
        btnCheckout = findViewById(R.id.btn_checkout)
        cbSelectAll = findViewById(R.id.cb_select_all)
        btnDeleteSelected = findViewById(R.id.btn_delete_selected)
        spinnerPaymentMethod = findViewById(R.id.spinner_payment_method)
        spinnerStore = findViewById(R.id.spinner_store)
        rgDeliveryType = findViewById(R.id.rg_delivery_type)
        rbPickup = findViewById(R.id.rb_pickup)
        rbDelivery = findViewById(R.id.rb_delivery)
        llDeliveryAddress = findViewById(R.id.ll_delivery_address)
        etDeliveryAddress = findViewById(R.id.et_delivery_address)
        llVoucherSection = findViewById(R.id.ll_voucher_section)
        etVoucherCode = findViewById(R.id.et_voucher_code)
        btnApplyVoucher = findViewById(R.id.btn_apply_voucher)
        btnSelectVoucher = findViewById(R.id.btn_select_voucher)
        tvDiscountAmount = findViewById(R.id.tv_discount_amount)
        tvFinalPrice = findViewById(R.id.tv_final_price)
        
        val orderType = intent.getStringExtra("orderType")
        Log.d("CartActivity", "Received orderType: $orderType")
        
        if (orderType == "delivery") {
            Log.d("CartActivity", "Setting delivery mode")
            rbDelivery.isChecked = true
            selectedDeliveryType = "DELIVERY"
            llDeliveryAddress.visibility = View.VISIBLE
        } else {
            Log.d("CartActivity", "Setting pickup mode")
            rbPickup.isChecked = true
            selectedDeliveryType = "PICKUP"
            llDeliveryAddress.visibility = View.GONE
        }

        // Setup Payment Method Spinner
        val paymentAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, paymentMethods)
        spinnerPaymentMethod.adapter = paymentAdapter
        
        // Load stores from API
        loadStores()
        
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_cart).setNavigationOnClickListener { 
            // Quay lại MainActivity với tab home được chọn
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("SELECTED_ITEM", R.id.nav_home)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }
        
        // Click vào icon lịch sử đơn hàng
        findViewById<ImageView>(R.id.btn_order_history).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("NAVIGATE_TO", "ORDER_HISTORY")
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(this, cartItems, this)
        rvCartItems.layoutManager = LinearLayoutManager(this)
        rvCartItems.adapter = cartAdapter
    }

    private fun setupListeners() {
        cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
            cartAdapter.selectAll(isChecked)
        }

        btnDeleteSelected.setOnClickListener {
            val selectedItems = cartAdapter.getSelectedItems()
            if (selectedItems.isNotEmpty()) {
                showDeleteConfirmationDialog(selectedItems)
            }
        }
        
        // Xử lý chọn loại giao hàng (PICKUP / DELIVERY)
        rgDeliveryType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_pickup -> {
                    selectedDeliveryType = "PICKUP"
                    llDeliveryAddress.visibility = View.GONE
                }
                R.id.rb_delivery -> {
                    selectedDeliveryType = "DELIVERY"
                    llDeliveryAddress.visibility = View.VISIBLE
                }
            }
        }
        
        // Xử lý chọn chi nhánh từ Spinner
        spinnerStore.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (storeList.isNotEmpty() && position < storeList.size) {
                    selectedStoreId = storeList[position].id
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedStoreId = null
            }
        }
        
        btnApplyVoucher.setOnClickListener {
            val code = etVoucherCode.text.toString().trim()
            if (code.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập mã voucher", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            validateAndApplyVoucher(code)
        }
        
        btnSelectVoucher.setOnClickListener {
            showVoucherSelectionDialog()
        }
        
        btnCheckout.setOnClickListener { 
            val selectedItems = cartAdapter.getSelectedItems()
            if (selectedItems.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn sản phẩm để mua", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (selectedStoreId == null) {
                Toast.makeText(this, "Vui lòng chọn chi nhánh", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Kiểm tra địa chỉ nếu chọn giao tận nơi
            val deliveryAddress = if (selectedDeliveryType == "DELIVERY") {
                val address = etDeliveryAddress.text.toString().trim()
                if (address.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập địa chỉ giao hàng", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                address
            } else null
            
            val selectedPaymentMethod = spinnerPaymentMethod.selectedItem.toString()

            createOrder(selectedItems, selectedStoreId!!, selectedPaymentMethod, deliveryAddress)
        }
    }
    
    private fun loadStores() {
        // Kiểm tra cache trước
        val cachedStores = com.example.doan.Utils.DataCache.stores
        if (!cachedStores.isNullOrEmpty()) {
            setupStoreSpinner(cachedStores)
            return
        }
        
        // Nếu không có cache, gọi API
        RetrofitClient.getInstance(this).apiService.getStores().enqueue(object : Callback<ApiResponse<List<Store>>> {
            override fun onResponse(call: Call<ApiResponse<List<Store>>>, response: Response<ApiResponse<List<Store>>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val stores = response.body()?.data ?: emptyList()
                    com.example.doan.Utils.DataCache.stores = stores
                    setupStoreSpinner(stores)
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<Store>>>, t: Throwable) {
                Log.e("CartActivity", "Error loading stores", t)
                Toast.makeText(this@CartActivity, "Không thể tải danh sách chi nhánh", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun setupStoreSpinner(stores: List<Store>) {
        storeList.clear()
        storeList.addAll(stores)
        
        // Setup Store Spinner
        val storeNames = storeList.map { it.storeName ?: "Chi nhánh ${it.id}" }
        val storeAdapter = ArrayAdapter(this@CartActivity, android.R.layout.simple_spinner_dropdown_item, storeNames)
        spinnerStore.adapter = storeAdapter
        
        // Chọn chi nhánh đầu tiên mặc định
        if (storeList.isNotEmpty()) {
            selectedStoreId = storeList[0].id
        }
    }

    private fun loadCart() {
        val userId = SessionManager(this).getUserId()
        if (userId == -1) return

        RetrofitClient.getInstance(this).apiService.getCart(userId.toLong()).enqueue(object : Callback<ApiResponse<Cart>> {
            override fun onResponse(call: Call<ApiResponse<Cart>>, response: Response<ApiResponse<Cart>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val loadedItems = response.body()?.data?.items ?: emptyList()
                    cartItems.clear()
                    val itemsWithSelection = loadedItems.map { it.apply { isSelected = false } }
                    cartItems.addAll(itemsWithSelection)
                    cartAdapter.notifyDataSetChanged()
                    updateUi()
                }
            }

            override fun onFailure(call: Call<ApiResponse<Cart>>, t: Throwable) {
                Log.e("CartActivity", "Error loading cart", t)
            }
        })
    }
    


    private fun updateUi() {
        if (cartItems.isEmpty()) {
            tvEmptyCart.visibility = View.VISIBLE
            rvCartItems.visibility = View.GONE
        } else {
            tvEmptyCart.visibility = View.GONE
            rvCartItems.visibility = View.VISIBLE
        }
        calculateTotalPrice()
    }

    private fun calculateTotalPrice() {
        val selectedItems = cartAdapter.getSelectedItems()
        val total = selectedItems.sumOf { 
            (it.unitPrice ?: 0.0) * (it.quantity ?: 1)
        }
        tvTotalPrice.text = String.format(Locale.getDefault(), "%,.0f VNĐ", total)
        
        // Recalculate discount if voucher is applied
        if (appliedVoucher != null) {
            calculateDiscount(total)
        } else {
            discountAmount = 0.0
            tvDiscountAmount.text = "0 VNĐ"
            tvFinalPrice.text = String.format(Locale.getDefault(), "%,.0f VNĐ", total)
        }
    }
    
    private fun calculateDiscount(totalPrice: Double) {
        val voucher = appliedVoucher ?: return
        
        val discountValue = voucher.discountValue?.toDouble() ?: 0.0
        
        discountAmount = if (voucher.discountType == "PERCENT") {
            val discount = totalPrice * discountValue / 100
            // Apply max discount if exists
            if (voucher.maxDiscountAmount != null) {
                minOf(discount, voucher.maxDiscountAmount?.toDouble() ?: discount)
            } else {
                discount
            }
        } else {
            // For FIXED type, discount cannot exceed total price
            minOf(discountValue, totalPrice)
        }
        
        val finalPrice = maxOf(0.0, totalPrice - discountAmount)
        
        tvDiscountAmount.text = String.format(Locale.getDefault(), "-%,.0f VNĐ", discountAmount)
        tvFinalPrice.text = String.format(Locale.getDefault(), "%,.0f VNĐ", finalPrice)
    }
    
    private fun validateAndApplyVoucher(code: String) {
        val selectedItems = cartAdapter.getSelectedItems()
        val totalPrice = selectedItems.sumOf { 
            (it.unitPrice ?: 0.0) * (it.quantity ?: 1)
        }
        
        RetrofitClient.getInstance(this).apiService.validatePromotion(code, totalPrice)
            .enqueue(object : Callback<ApiResponse<com.example.doan.Models.Voucher>> {
                override fun onResponse(
                    call: Call<ApiResponse<com.example.doan.Models.Voucher>>,
                    response: Response<ApiResponse<com.example.doan.Models.Voucher>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        appliedVoucher = response.body()?.data
                        etVoucherCode.setText(appliedVoucher?.code)
                        calculateDiscount(totalPrice)
                        Toast.makeText(this@CartActivity, "Áp dụng voucher thành công!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@CartActivity, response.body()?.message ?: "Mã voucher không hợp lệ", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<com.example.doan.Models.Voucher>>, t: Throwable) {
                    Log.e("CartActivity", "Error validating voucher", t)
                    Toast.makeText(this@CartActivity, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                }
            })
    }
    
    private fun showVoucherSelectionDialog() {
        RetrofitClient.getInstance(this).apiService.getActivePromotions()
            .enqueue(object : Callback<ApiResponse<List<com.example.doan.Models.Voucher>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<com.example.doan.Models.Voucher>>>,
                    response: Response<ApiResponse<List<com.example.doan.Models.Voucher>>>
                ) {
                    Log.d("CartActivity", "Voucher response code: ${response.code()}")
                    Log.d("CartActivity", "Voucher response: ${response.body()}")
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        val vouchers = response.body()?.data ?: emptyList()
                        Log.d("CartActivity", "Loaded ${vouchers.size} vouchers")
                        
                        if (vouchers.isEmpty()) {
                            Toast.makeText(this@CartActivity, "Không có voucher khả dụng", Toast.LENGTH_SHORT).show()
                            return
                        }
                        
                        val dialogView = LayoutInflater.from(this@CartActivity)
                            .inflate(R.layout.dialog_voucher_selection, null)
                        val rvVouchers: RecyclerView = dialogView.findViewById(R.id.rv_vouchers)
                        
                        val dialog = AlertDialog.Builder(this@CartActivity)
                            .setTitle("Chọn Voucher")
                            .setView(dialogView)
                            .setNegativeButton("Đóng", null)
                            .create()
                        
                        rvVouchers.layoutManager = LinearLayoutManager(this@CartActivity)
                        rvVouchers.adapter = VoucherSelectionAdapter(vouchers) { voucher ->
                            etVoucherCode.setText(voucher.code)
                            validateAndApplyVoucher(voucher.code)
                            dialog.dismiss()
                        }
                        
                        dialog.show()
                    } else {
                        val errorMsg = response.body()?.message ?: response.errorBody()?.string() ?: "Unknown error"
                        Log.e("CartActivity", "Error loading vouchers: $errorMsg")
                        Toast.makeText(this@CartActivity, "Lỗi: $errorMsg", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<com.example.doan.Models.Voucher>>>, t: Throwable) {
                    Log.e("CartActivity", "Error loading vouchers", t)
                    Toast.makeText(this@CartActivity, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    override fun onItemSelectedChanged() {
        calculateTotalPrice()
    }

    override fun onItemDeleted(item: CartItem) {
        // Kiểm tra user đã đăng nhập chưa
        if (SessionManager(this).getUserId() == -1) return

        item.id?.let { cartItemId ->
            // Sử dụng API mới - không cần truyền userId (lấy từ JWT token)
            RetrofitClient.getInstance(this).apiService.removeCartItem(cartItemId)
                .enqueue(object : Callback<ApiResponse<Void>> {
                    override fun onResponse(call: Call<ApiResponse<Void>>, response: Response<ApiResponse<Void>>) {
                        if (response.isSuccessful) {
                            loadCart()
                        } else {
                            Toast.makeText(this@CartActivity, "Không thể xóa sản phẩm", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<ApiResponse<Void>>, t: Throwable) {
                        Log.e("CartActivity", "Error removing cart item", t)
                        Toast.makeText(this@CartActivity, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }
    
    private fun showDeleteConfirmationDialog(itemsToDelete: List<CartItem>){
        AlertDialog.Builder(this)
            .setTitle("Xác nhận xóa")
            .setMessage("Bạn có chắc muốn xóa ${itemsToDelete.size} mục đã chọn?")
            .setPositiveButton("Xóa") { _, _ ->
                cartAdapter.deleteSelectedItems()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
    
    private fun createOrder(items: List<CartItem>, storeId: Int, paymentMethod: String, deliveryAddress: String? = null) {
        val orderItems = items.map { item ->
            com.example.doan.Models.OrderItemRequest(
                drinkId = item.drinkId!!.toLong(), 
                quantity = item.quantity ?: 1,
                sizeName = item.sizeName ?: "M",
                toppingIds = item.toppings?.mapNotNull { topping -> topping.id.toLong() } ?: emptyList(),
                note = item.note
            )
        }
        
        val request = CreateOrderRequest(
            storeId = storeId.toLong(),
            items = orderItems,
            type = selectedDeliveryType, // PICKUP hoặc DELIVERY
            paymentMethod = paymentMethod,
            address = deliveryAddress, // Địa chỉ giao hàng (null nếu PICKUP)
            promotionCode = appliedVoucher?.code // Mã voucher nếu có
        )

        RetrofitClient.getInstance(this).apiService.createOrder(request).enqueue(object: Callback<ApiResponse<Order>> {
            override fun onResponse(call: Call<ApiResponse<Order>>, response: Response<ApiResponse<Order>>) {
                if(response.isSuccessful && response.body()?.success == true) {
                    val order = response.body()?.data
                    createdOrderId = order?.id?.toLong()
                    
                    // Kiểm tra phương thức thanh toán
                    if (paymentMethod == "VNPAY") {
                        // Tạo URL thanh toán VNPAY
                        createVNPayPayment(createdOrderId!!)
                    } else {
                        // COD - thanh toán khi nhận hàng
                        Toast.makeText(this@CartActivity, "Đặt hàng thành công!", Toast.LENGTH_SHORT).show()
                        clearCartOnServer()
                        navigateToOrders()
                    }
                } else {
                    Toast.makeText(this@CartActivity, "Đặt hàng thất bại: ${response.body()?.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<Order>>, t: Throwable) {
                Toast.makeText(this@CartActivity, "Lỗi: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
    
    private fun createVNPayPayment(orderId: Long) {
        val request = VNPayPaymentRequest(
            orderId = orderId,
            orderInfo = "Thanh toan don hang $orderId",
            ipAddress = "127.0.0.1"
        )
        
        RetrofitClient.getInstance(this).apiService.createVNPayPayment(request)
            .enqueue(object : Callback<ApiResponse<VNPayPaymentResponse>> {
                override fun onResponse(
                    call: Call<ApiResponse<VNPayPaymentResponse>>,
                    response: Response<ApiResponse<VNPayPaymentResponse>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val paymentUrl = response.body()?.data?.paymentUrl
                        if (!paymentUrl.isNullOrEmpty()) {
                            // Clear cart trước khi chuyển sang thanh toán
                            clearCartOnServer()
                            
                            // Mở WebView để thanh toán
                            val intent = Intent(this@CartActivity, VNPayPaymentActivity::class.java)
                            intent.putExtra("PAYMENT_URL", paymentUrl)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@CartActivity, "Không thể tạo URL thanh toán", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@CartActivity, "Lỗi tạo thanh toán: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<VNPayPaymentResponse>>, t: Throwable) {
                    Log.e("CartActivity", "Error creating VNPAY payment", t)
                    Toast.makeText(this@CartActivity, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
    
    private fun navigateToOrders() {
        val intent = Intent(this@CartActivity, MainActivity::class.java).apply {
            putExtra("SELECTED_ITEM", R.id.nav_order)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun clearCartOnServer() {
        val userId = SessionManager(this).getUserId()
        if (userId == -1) return

        RetrofitClient.getInstance(this).apiService.clearCart(userId.toLong()).enqueue(object: Callback<ApiResponse<Void>> {
            override fun onResponse(call: Call<ApiResponse<Void>>, response: Response<ApiResponse<Void>>) {
                if (!response.isSuccessful) {
                    Log.e("CartActivity", "Failed to clear cart on server")
                }
            }

            override fun onFailure(call: Call<ApiResponse<Void>>, t: Throwable) {
                Log.e("CartActivity", "Error clearing cart on server", t)
            }
        })
    }
}
