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
import com.example.doan.Adapters.CombinedVoucherAdapter
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.Cart
import com.example.doan.Models.CartItem
import com.example.doan.Models.CreateOrderRequest
import com.example.doan.Models.DeliveryAddressInfo
import com.example.doan.Models.Order
import com.example.doan.Models.Store
import com.example.doan.Models.UpdateProfileRequest
import com.example.doan.Models.UserProfileDto
import com.example.doan.Models.VNPayPaymentRequest
import com.example.doan.Models.VNPayPaymentResponse
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.InAppNotification
import com.example.doan.Utils.LoadingDialog
import com.example.doan.Utils.SeasonalEffectManager
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
    private lateinit var spinnerProvince: Spinner
    private lateinit var etManualAddress: EditText
    private lateinit var tvShippingFee: TextView
    private lateinit var llVoucherSection: LinearLayout
    private lateinit var etVoucherCode: EditText
    private lateinit var btnApplyVoucher: Button
    private lateinit var btnSelectVoucher: Button
    private lateinit var btnClearVoucher: ImageView
    private lateinit var tvDiscountAmount: TextView
    private lateinit var tvFinalPrice: TextView
    
    // Shipping fee summary views
    private lateinit var llShippingFeeSummary: LinearLayout
    private lateinit var tvShippingFeeSummary: TextView

    private var cartItems = mutableListOf<CartItem>()
    private var storeList = mutableListOf<Store>()
    private var selectedStoreId: Int? = null
    private var selectedDeliveryType: String = "PICKUP" // Mặc định là đến lấy
    private val paymentMethods = listOf("COD", "VNPAY", "MOMO", "VIETQR", "PAYPAL")
    private var appliedVoucher: com.example.doan.Models.Voucher? = null
    private var appliedSpinVoucher: com.example.doan.Models.SpinRewardDto? = null
    private var discountAmount: Double = 0.0
    private var tierDiscountAmount: Double = 0.0
    private var tierDiscountPercent: Double = 0.0
    private var tierName: String = ""
    private var createdOrderId: Long? = null
    private var currentUserProfile: UserProfileDto? = null
    
    // Delivery Address
    private var selectedProvince: String? = null
    private var shippingFee: Int = 0
    private var deliveryAddressInfo: DeliveryAddressInfo? = null
    private lateinit var tvSelectedAddress: TextView
    

    private var lastOtpSentTime = 0L
    private val OTP_COOLDOWN = 60_000L // 60 giây
    
    // Pending order request for payment callbacks
    private var pendingOrderRequest: CreateOrderRequest? = null
    private var pendingCartItems: List<CartItem>? = null
    
    companion object {
        private const val TAG = "CartActivity"
    }

    // Tier discount views
    private lateinit var llTierDiscount: LinearLayout
    private lateinit var tvTierDiscountLabel: TextView
    private lateinit var tvTierDiscountAmount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        initViews()
        setupRecyclerView()
        setupListeners()
        setupConfetti()
        loadCart()
        loadUserProfile()
    }


    private fun setupConfetti() {
        val rootView = findViewById<View>(android.R.id.content) as? android.view.ViewGroup
        rootView?.let {
            SeasonalEffectManager.addConfettiEffect(it)
        }
    }

    override fun onResume() {
        super.onResume()
        // Clear voucher đã áp dụng khi quay lại để tránh dùng voucher đã hết hạn
        if (appliedSpinVoucher != null || appliedVoucher != null) {
            // Re-validate voucher đã áp dụng
            val code = etVoucherCode.text.toString().trim()
            if (code.isNotEmpty()) {
                revalidateAppliedVoucher(code)
            }
        }
    }

    private fun revalidateAppliedVoucher(code: String) {
        if (appliedSpinVoucher != null) {
            // Re-validate spin voucher
            RetrofitClient.getInstance(this).apiService.validateSpinVoucher(code)
                .enqueue(object : Callback<ApiResponse<com.example.doan.Models.SpinRewardDto>> {
                    override fun onResponse(
                        call: Call<ApiResponse<com.example.doan.Models.SpinRewardDto>>,
                        response: Response<ApiResponse<com.example.doan.Models.SpinRewardDto>>
                    ) {
                        if (!response.isSuccessful || response.body()?.success != true) {
                            // Voucher không còn hợp lệ, clear nó
                            clearAppliedVoucher()
                            Toast.makeText(this@CartActivity, "Voucher đã hết hạn hoặc đã được sử dụng", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<com.example.doan.Models.SpinRewardDto>>, t: Throwable) {
                        // Ignore network error
                    }
                })
        }
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
        spinnerProvince = findViewById(R.id.spinner_province)
        etManualAddress = findViewById(R.id.et_manual_address)
        tvShippingFee = findViewById(R.id.tv_shipping_fee)
        tvSelectedAddress = findViewById(R.id.tv_selected_address)
        llVoucherSection = findViewById(R.id.ll_voucher_section)
        etVoucherCode = findViewById(R.id.et_voucher_code)
        btnApplyVoucher = findViewById(R.id.btn_apply_voucher)
        btnSelectVoucher = findViewById(R.id.btn_select_voucher)
        btnClearVoucher = findViewById(R.id.btn_clear_voucher)
        tvDiscountAmount = findViewById(R.id.tv_discount_amount)
        tvFinalPrice = findViewById(R.id.tv_final_price)
        
        // Shipping fee summary views
        llShippingFeeSummary = findViewById(R.id.ll_shipping_fee_summary)
        tvShippingFeeSummary = findViewById(R.id.tv_shipping_fee_summary)

        // Tier discount views
        llTierDiscount = findViewById(R.id.ll_tier_discount)
        tvTierDiscountLabel = findViewById(R.id.tv_tier_discount_label)
        tvTierDiscountAmount = findViewById(R.id.tv_tier_discount_amount)

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
        
        // Setup Province Spinner - Sử dụng VietnamProvinces object
        val provinceNames = com.example.doan.Utils.VietnamProvinces.getProvinceNames()
        val provinceAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, provinceNames)
        spinnerProvince.adapter = provinceAdapter
        
        // Province selection listener
        spinnerProvince.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedProvince = provinceNames[position]
                calculateShippingFee()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedProvince = null
                shippingFee = 0
            }
        }

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
                    shippingFee = 0
                    calculateTotalPrice()
                }
                R.id.rb_delivery -> {
                    selectedDeliveryType = "DELIVERY"
                    llDeliveryAddress.visibility = View.VISIBLE
                    calculateShippingFee()
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

        btnClearVoucher.setOnClickListener {
            clearAppliedVoucher()
        }

        btnSelectVoucher.setOnClickListener {
            showVoucherSelectionDialog()
        }

        btnCheckout.setOnClickListener { 
            handleCheckout()
        }
    }
    
    private fun calculateShippingFee() {
        if (selectedDeliveryType == "DELIVERY" && selectedProvince != null) {
            // Sử dụng VietnamProvinces để lấy phí ship theo tỉnh
            shippingFee = com.example.doan.Utils.VietnamProvinces.getShippingFee(selectedProvince!!)
            tvShippingFee.text = String.format(Locale.getDefault(), "Phí ship: %,d VNĐ", shippingFee)
            calculateTotalPrice()
        } else {
            shippingFee = 0
            tvShippingFee.text = "Phí ship: 0 VNĐ"
        }
    }
    
     private fun loadUserProfile() {
        RetrofitClient.getInstance(this).apiService.getMyProfile().enqueue(object : Callback<ApiResponse<UserProfileDto>> {
            override fun onResponse(call: Call<ApiResponse<UserProfileDto>>, response: Response<ApiResponse<UserProfileDto>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    currentUserProfile = response.body()?.data
                }
            }

            override fun onFailure(call: Call<ApiResponse<UserProfileDto>>, t: Throwable) {
                Log.e("CartActivity", "Failed to load user profile", t)
            }
        })
    }

    private fun handleCheckout() {
        val selectedItems = cartAdapter.getSelectedItems()
        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn sản phẩm để mua", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedStoreId == null) {
            Toast.makeText(this, "Vui lòng chọn chi nhánh", Toast.LENGTH_SHORT).show()
            return
        }

        val deliveryAddress = if (selectedDeliveryType == "DELIVERY") {
            if (selectedProvince == null) {
                Toast.makeText(this, "Vui lòng chọn tỉnh/thành phố", Toast.LENGTH_SHORT).show()
                return
            }
            val detailAddress = etManualAddress.text.toString().trim()
            if (detailAddress.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập địa chỉ chi tiết", Toast.LENGTH_SHORT).show()
                return
            }
            "$detailAddress, $selectedProvince"
        } else null

        val selectedPaymentMethod = spinnerPaymentMethod.selectedItem.toString()
        
        // Kiểm tra số điện thoại trước khi xác thực OTP
        val phone = currentUserProfile?.phone
        if (phone.isNullOrEmpty()) {
            showEnterPhoneDialog()
            return
        }
        
        // Yêu cầu xác thực OTP trước khi thanh toán
        showOtpVerificationDialog(phone, selectedItems, selectedStoreId!!, selectedPaymentMethod, deliveryAddress)
    }
    

    private fun showOtpVerificationDialog(
        phoneNumber: String, 
        items: List<CartItem>, 
        storeId: Int, 
        paymentMethod: String, 
        deliveryAddress: String?
    ) {
        // Gửi OTP
        sendOtpToUser(phoneNumber)

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_otp_verify, null)
        val edtOtp = dialogView.findViewById<EditText>(R.id.edt_otp_code)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Xác thực đặt hàng")
            .setMessage("Mã OTP đã gửi đến $phoneNumber\n\n💡 Nếu không nhận được OTP, bạn có thể chọn 'Bỏ qua' để tiếp tục.")
            .setView(dialogView)
            .setPositiveButton("Xác nhận", null) // Set null để tự xử lý
            .setNeutralButton("Gửi lại", null) // Set null để không dismiss dialog
            .setNegativeButton("Bỏ qua") { _, _ ->
                // Cho phép bỏ qua OTP và tiếp tục thanh toán (cho môi trường test)
                Log.d(TAG, "User skipped OTP verification")
                navigateToBillPreview(items, storeId, paymentMethod, deliveryAddress)
            }
            .create()

        dialog.setOnShowListener {
            // Xử lý nút Xác nhận
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val code = edtOtp.text.toString().trim()
                if (code.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập mã OTP", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                // Verify OTP
                verifyOtpAndProceed(phoneNumber, code, items, storeId, paymentMethod, deliveryAddress, dialog)
            }
            
            // Xử lý nút Gửi lại - không dismiss dialog
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                sendOtpToUser(phoneNumber)
            }
        }

        dialog.show()
    }
    

    private fun verifyOtpAndProceed(
        phone: String, 
        code: String, 
        items: List<CartItem>, 
        storeId: Int, 
        paymentMethod: String, 
        deliveryAddress: String?,
        dialog: AlertDialog
    ) {
        val loadingDialog = LoadingDialog(this)
        loadingDialog.show("Đang xác thực...")
        
        RetrofitClient.getInstance(this).apiService.verifyOtp(phone, code)
            .enqueue(object : Callback<ApiResponse<Boolean>> {
                override fun onResponse(call: Call<ApiResponse<Boolean>>, response: Response<ApiResponse<Boolean>>) {
                    loadingDialog.dismiss()
                    
                    if (response.isSuccessful && response.body()?.data == true) {
                        // OTP hợp lệ, đóng dialog và chuyển sang màn hình xem bill
                        dialog.dismiss()
                        navigateToBillPreview(items, storeId, paymentMethod, deliveryAddress)
                    } else {
                        Toast.makeText(this@CartActivity, "Mã OTP không đúng, vui lòng thử lại", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Boolean>>, t: Throwable) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@CartActivity, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
    

    private fun showAddressSelectionDialog() {
        val helper = com.example.doan.Utils.GhnAddressHelper(this)
        helper.showAddressSelectionDialog(object : com.example.doan.Utils.GhnAddressHelper.OnAddressSelectedListener {
            override fun onAddressSelected(addressInfo: DeliveryAddressInfo, calculatedShippingFee: Int) {
                deliveryAddressInfo = addressInfo
                shippingFee = calculatedShippingFee
                
                // Hiển thị địa chỉ đã chọn
                tvSelectedAddress.text = addressInfo.getFullAddress()
                tvSelectedAddress.visibility = View.VISIBLE
                
                // Hiển thị phí ship
                tvShippingFee.text = String.format(Locale.getDefault(), "Phí ship: %,d VNĐ", shippingFee)
                tvShippingFee.visibility = View.VISIBLE
                

                calculateTotalPrice()
            }
            
            override fun onCancelled() {

            }
        })
    }

    private fun showEnterPhoneDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_enter_phone, null)
        val edtPhoneNumber = dialogView.findViewById<EditText>(R.id.edt_phone_number)

        AlertDialog.Builder(this)
            .setTitle("Cập nhật số điện thoại")
            .setMessage("Vui lòng cập nhật số điện thoại để tiếp tục đặt hàng.")
            .setView(dialogView)
            .setPositiveButton("Cập nhật") { _, _ ->
                val newPhone = edtPhoneNumber.text.toString().trim()
                if (newPhone.isNotEmpty()) {
                    updatePhoneNumber(newPhone)
                } else {
                    Toast.makeText(this, "Số điện thoại không được để trống", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun updatePhoneNumber(phone: String) {
        val profile = currentUserProfile ?: return
        val updateRequest = UpdateProfileRequest(
            fullName = profile.fullName ?: "",
            email = profile.email ?: "",
            phone = phone,
            address = profile.address ?: ""
        )

        RetrofitClient.getInstance(this).apiService.updateProfile(updateRequest).enqueue(object : Callback<ApiResponse<UserProfileDto>> {
            override fun onResponse(call: Call<ApiResponse<UserProfileDto>>, response: Response<ApiResponse<UserProfileDto>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    currentUserProfile = response.body()?.data
                    Toast.makeText(this@CartActivity, "Cập nhật số điện thoại thành công", Toast.LENGTH_SHORT).show()
                    showOtpDialog(phone) // Proceed to OTP verification
                } else {
                    Toast.makeText(this@CartActivity, "Lỗi cập nhật số điện thoại: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<UserProfileDto>>, t: Throwable) {
                Toast.makeText(this@CartActivity, "Lỗi mạng", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showOtpDialog(phoneNumber: String) {
        sendOtpToUser(phoneNumber)

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_otp_verify, null)
        val edtOtp = dialogView.findViewById<EditText>(R.id.edt_otp_code)

        AlertDialog.Builder(this)
            .setTitle("Xác thực đặt hàng")
            .setMessage("Mã OTP đã gửi đến $phoneNumber")
            .setView(dialogView)
            .setPositiveButton("Xác nhận") { _, _ ->
                val code = edtOtp.text.toString().trim()
                if (code.isNotEmpty()) {
                    val selectedItems = cartAdapter.getSelectedItems()
                    val deliveryAddress = if (selectedDeliveryType == "DELIVERY") {
                        deliveryAddressInfo?.getFullAddress()
                    } else null
                    val selectedPaymentMethod = spinnerPaymentMethod.selectedItem.toString()
                    verifyOtpAndPlaceOrder(phoneNumber, code, selectedItems, selectedStoreId!!, selectedPaymentMethod, deliveryAddress)
                }
            }
            .setNeutralButton("Gửi lại") { _, _ ->
                sendOtpToUser(phoneNumber)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun sendOtpToUser(phone: String) {

        val now = System.currentTimeMillis()
        if (now - lastOtpSentTime < OTP_COOLDOWN) {
            val remaining = (OTP_COOLDOWN - (now - lastOtpSentTime)) / 1000
            Toast.makeText(this, "Vui lòng đợi ${remaining}s trước khi gửi lại OTP", Toast.LENGTH_LONG).show()
            return
        }
        
        lastOtpSentTime = now
        
        RetrofitClient.getInstance(this).apiService.sendOtp(phone).enqueue(object : Callback<ApiResponse<String>> {
            override fun onResponse(call: Call<ApiResponse<String>>, response: Response<ApiResponse<String>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@CartActivity, "Đã gửi OTP đến $phone", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "OTP sent successfully to $phone")
                } else {
                    val errorMsg = response.body()?.message ?: response.message()
                    Log.e(TAG, "OTP send failed: $errorMsg")
                    Toast.makeText(this@CartActivity, "Lỗi gửi OTP: $errorMsg", Toast.LENGTH_LONG).show()
                    // Reset timer nếu gửi thất bại
                    lastOtpSentTime = 0L
                }
            }

            override fun onFailure(call: Call<ApiResponse<String>>, t: Throwable) {
                Log.e(TAG, "OTP send network error", t)
                Toast.makeText(this@CartActivity, "Lỗi kết nối: ${t.message}", Toast.LENGTH_LONG).show()
                // Reset timer nếu gửi thất bại
                lastOtpSentTime = 0L
            }
        })
    }

    private fun verifyOtpAndPlaceOrder(phone: String, code: String, items: List<CartItem>, storeId: Int, paymentMethod: String, deliveryAddress: String?) {
        RetrofitClient.getInstance(this).apiService.verifyOtp(phone, code).enqueue(object : Callback<ApiResponse<Boolean>> {
            override fun onResponse(call: Call<ApiResponse<Boolean>>, response: Response<ApiResponse<Boolean>>) {
                if (response.isSuccessful && response.body()?.data == true) {
                    // Chuyển sang màn hình xem bill trước khi thanh toán
                    navigateToBillPreview(items, storeId, paymentMethod, deliveryAddress)
                } else {
                    Toast.makeText(this@CartActivity, "Mã OTP sai, vui lòng thử lại", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<Boolean>>, t: Throwable) {
                Toast.makeText(this@CartActivity, "Lỗi mạng", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun navigateToBillPreview(items: List<CartItem>, storeId: Int, paymentMethod: String, deliveryAddress: String?) {
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
            type = selectedDeliveryType,
            paymentMethod = paymentMethod,
            address = deliveryAddress,
            promotionCode = appliedVoucher?.code,
            spinVoucherCode = appliedSpinVoucher?.voucherCode,
            ghnDistrictId = null,
            ghnWardCode = null,
            shippingFee = shippingFee
        )

        // Lấy danh sách cartItemIds đã chọn
        val cartItemIds = items.mapNotNull { it.id }

        val intent = Intent(this, BillPreviewActivity::class.java).apply {
            putExtra(BillPreviewActivity.EXTRA_ORDER_REQUEST, com.google.gson.Gson().toJson(request))
            putExtra(BillPreviewActivity.EXTRA_CART_ITEM_IDS, cartItemIds.toLongArray())
            putExtra(BillPreviewActivity.EXTRA_SHIPPING_FEE, shippingFee)
        }
        startActivity(intent)
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
        // Tạm tính = tổng tiền sản phẩm + phí ship
        val subtotal = total + shippingFee
        tvTotalPrice.text = String.format(Locale.getDefault(), "%,.0f VNĐ", subtotal)

        // Load tier discount từ API
        if (total > 0) {
            loadTierDiscount(total)
        } else {
            hideTierDiscount()
        }

        // Recalculate discount if voucher is applied
        when {
            appliedSpinVoucher != null -> {
                // Tính discount từ spin voucher
                discountAmount = total * appliedSpinVoucher!!.discountPercent / 100
                updateFinalPrice(total)
            }
            appliedVoucher != null -> {
                calculateDiscount(total)
            }
            else -> {
                discountAmount = 0.0
                tvDiscountAmount.text = "0 VNĐ"
                updateFinalPrice(total)
            }
        }
    }

    private fun loadTierDiscount(orderTotal: Double) {
        RetrofitClient.getInstance(this).apiService.previewTierDiscount(orderTotal)
            .enqueue(object : Callback<ApiResponse<com.example.doan.Models.TierDiscountPreview>> {
                override fun onResponse(
                    call: Call<ApiResponse<com.example.doan.Models.TierDiscountPreview>>,
                    response: Response<ApiResponse<com.example.doan.Models.TierDiscountPreview>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val preview = response.body()?.data
                        if (preview != null && preview.tierDiscount > 0) {
                            tierDiscountAmount = preview.tierDiscount
                            tierDiscountPercent = preview.discountPercent
                            tierName = preview.tierName
                            showTierDiscount()
                            updateFinalPrice(orderTotal)
                        } else {
                            tierDiscountAmount = 0.0
                            hideTierDiscount()
                        }
                    } else {
                        tierDiscountAmount = 0.0
                        hideTierDiscount()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<com.example.doan.Models.TierDiscountPreview>>, t: Throwable) {
                    Log.e("CartActivity", "Error loading tier discount", t)
                    tierDiscountAmount = 0.0
                    hideTierDiscount()
                }
            })
    }

    private fun showTierDiscount() {
        llTierDiscount.visibility = View.VISIBLE
        tvTierDiscountLabel.text = "Ưu đãi hạng $tierName (${tierDiscountPercent.toInt()}%):"
        tvTierDiscountAmount.text = String.format(Locale.getDefault(), "-%,.0f VNĐ", tierDiscountAmount)
    }

    private fun hideTierDiscount() {
        llTierDiscount.visibility = View.GONE
        tierDiscountAmount = 0.0
    }

    private fun updateFinalPrice(total: Double) {
        val totalDiscount = discountAmount + tierDiscountAmount
        val subtotal = total + shippingFee // Cộng phí ship
        val finalPrice = maxOf(0.0, subtotal - totalDiscount)
        tvFinalPrice.text = String.format(Locale.getDefault(), "%,.0f VNĐ", finalPrice)
    }

    // FIX Medium #14: Improved null handling in calculateDiscount
    private fun calculateDiscount(totalPrice: Double) {
        val voucher = appliedVoucher ?: return

        // Early return if discountValue is null
        val discountValue = voucher.discountValue?.toDouble()
        if (discountValue == null || discountValue <= 0) {
            discountAmount = 0.0
            tvDiscountAmount.text = "0 VNĐ"
            updateFinalPrice(totalPrice)
            return
        }

        discountAmount = if (voucher.discountType == "PERCENT") {
            val discount = totalPrice * discountValue / 100
            // Apply max discount if exists
            val maxDiscount = voucher.maxDiscountAmount?.toDouble()
            if (maxDiscount != null && maxDiscount > 0) {
                minOf(discount, maxDiscount)
            } else {
                discount
            }
        } else {

            minOf(discountValue, totalPrice)
        }

        tvDiscountAmount.text = String.format(Locale.getDefault(), "-%,.0f VNĐ", discountAmount)
        updateFinalPrice(totalPrice)
    }

    private fun validateAndApplyVoucher(code: String) {
        val selectedItems = cartAdapter.getSelectedItems()
        val totalPrice = selectedItems.sumOf { 
            (it.unitPrice ?: 0.0) * (it.quantity ?: 1)
        }


        RetrofitClient.getInstance(this).apiService.validateSpinVoucher(code)
            .enqueue(object : Callback<ApiResponse<com.example.doan.Models.SpinRewardDto>> {
                override fun onResponse(
                    call: Call<ApiResponse<com.example.doan.Models.SpinRewardDto>>,
                    response: Response<ApiResponse<com.example.doan.Models.SpinRewardDto>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        // Đây là voucher từ spin wheel - còn hợp lệ
                        val spinVoucher = response.body()?.data
                        if (spinVoucher != null) {
                            appliedSpinVoucher = spinVoucher
                            appliedVoucher = null // Clear voucher thường
                            etVoucherCode.setText(spinVoucher.voucherCode)
                            etVoucherCode.isEnabled = false // Disable edit khi đã áp dụng
                            btnClearVoucher.visibility = View.VISIBLE

                            // Tính discount từ spin voucher
                            discountAmount = totalPrice * spinVoucher.discountPercent / 100

                            tvDiscountAmount.text = String.format(Locale.getDefault(), "-%,.0f VNĐ (${spinVoucher.discountPercent}%%)", discountAmount)
                            updateFinalPrice(totalPrice)

                            Toast.makeText(this@CartActivity, "Áp dụng voucher giảm ${spinVoucher.discountPercent}% thành công!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Voucher spin không hợp lệ hoặc đã dùng
                        val errorMsg = response.body()?.message ?: ""
                        val errorCode = response.code()

                        // Nếu là lỗi 400 (Bad Request) - voucher spin đã dùng hoặc không tồn tại
                        if (errorCode == 400 || errorMsg.contains("đã được sử dụng") || errorMsg.contains("không hợp lệ")) {
                            // Clear spin voucher nếu đang có
                            if (appliedSpinVoucher?.voucherCode?.equals(code, ignoreCase = true) == true) {
                                appliedSpinVoucher = null
                            }
                            // Thử validate như voucher thường
                            validateNormalVoucher(code, totalPrice)
                        } else {
                            // Lỗi khác (server error, etc.) - thử voucher thường
                            validateNormalVoucher(code, totalPrice)
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<com.example.doan.Models.SpinRewardDto>>, t: Throwable) {
                    // Lỗi kết nối, thử voucher thường
                    validateNormalVoucher(code, totalPrice)
                }
            })
    }

    private fun validateNormalVoucher(code: String, totalPrice: Double) {
        RetrofitClient.getInstance(this).apiService.validatePromotion(code, totalPrice)
            .enqueue(object : Callback<ApiResponse<com.example.doan.Models.Voucher>> {
                override fun onResponse(
                    call: Call<ApiResponse<com.example.doan.Models.Voucher>>,
                    response: Response<ApiResponse<com.example.doan.Models.Voucher>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        appliedVoucher = response.body()?.data
                        appliedSpinVoucher = null // Clear spin voucher
                        etVoucherCode.setText(appliedVoucher?.code)
                        etVoucherCode.isEnabled = false // Disable edit khi đã áp dụng
                        btnClearVoucher.visibility = View.VISIBLE
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

    private fun clearAppliedVoucher() {
        appliedVoucher = null
        appliedSpinVoucher = null
        discountAmount = 0.0
        etVoucherCode.setText("")
        etVoucherCode.isEnabled = true
        btnClearVoucher.visibility = View.GONE
        calculateTotalPrice()
        Toast.makeText(this, "Đã xóa voucher", Toast.LENGTH_SHORT).show()
    }

    private fun showVoucherSelectionDialog() {
        // Load cả voucher thường và voucher spin
        val loadingDialog = LoadingDialog(this)
        loadingDialog.show("Đang tải voucher...")

        var normalVouchers: List<com.example.doan.Models.Voucher> = emptyList()
        var spinVouchers: List<com.example.doan.Models.SpinRewardDto> = emptyList()
        var loadedCount = 0

        // Load voucher thường
        RetrofitClient.getInstance(this).apiService.getActivePromotions()
            .enqueue(object : Callback<ApiResponse<List<com.example.doan.Models.Voucher>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<com.example.doan.Models.Voucher>>>,
                    response: Response<ApiResponse<List<com.example.doan.Models.Voucher>>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        normalVouchers = response.body()?.data ?: emptyList()
                    }
                    loadedCount++
                    if (loadedCount >= 2) {
                        loadingDialog.dismiss()
                        showCombinedVoucherDialog(normalVouchers, spinVouchers)
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<com.example.doan.Models.Voucher>>>, t: Throwable) {
                    Log.e("CartActivity", "Error loading vouchers", t)
                    loadedCount++
                    if (loadedCount >= 2) {
                        loadingDialog.dismiss()
                        showCombinedVoucherDialog(normalVouchers, spinVouchers)
                    }
                }
            })

        // Load voucher spin
        RetrofitClient.getInstance(this).apiService.getAvailableRewards()
            .enqueue(object : Callback<ApiResponse<List<com.example.doan.Models.SpinRewardDto>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<com.example.doan.Models.SpinRewardDto>>>,
                    response: Response<ApiResponse<List<com.example.doan.Models.SpinRewardDto>>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        spinVouchers = response.body()?.data ?: emptyList()
                    }
                    loadedCount++
                    if (loadedCount >= 2) {
                        loadingDialog.dismiss()
                        showCombinedVoucherDialog(normalVouchers, spinVouchers)
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<com.example.doan.Models.SpinRewardDto>>>, t: Throwable) {
                    Log.e("CartActivity", "Error loading spin vouchers", t)
                    loadedCount++
                    if (loadedCount >= 2) {
                        loadingDialog.dismiss()
                        showCombinedVoucherDialog(normalVouchers, spinVouchers)
                    }
                }
            })
    }

    private fun showCombinedVoucherDialog(
        normalVouchers: List<com.example.doan.Models.Voucher>,
        spinVouchers: List<com.example.doan.Models.SpinRewardDto>
    ) {
        if (normalVouchers.isEmpty() && spinVouchers.isEmpty()) {
            Toast.makeText(this, "Không có voucher khả dụng", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_voucher_selection, null)
        val rvVouchers: RecyclerView = dialogView.findViewById(R.id.rv_vouchers)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Chọn Voucher")
            .setView(dialogView)
            .setNegativeButton("Đóng", null)
            .create()

        // Tạo danh sách kết hợp
        val combinedList = mutableListOf<Any>()

        // Thêm voucher spin trước (ưu tiên)
        if (spinVouchers.isNotEmpty()) {
            combinedList.addAll(spinVouchers)
        }

        // Thêm voucher thường
        if (normalVouchers.isNotEmpty()) {
            combinedList.addAll(normalVouchers)
        }

        rvVouchers.layoutManager = LinearLayoutManager(this)
        rvVouchers.adapter = CombinedVoucherAdapter(combinedList) { item ->
            when (item) {
                is com.example.doan.Models.SpinRewardDto -> {
                    etVoucherCode.setText(item.voucherCode)
                    validateAndApplyVoucher(item.voucherCode ?: "")
                }
                is com.example.doan.Models.Voucher -> {
                    etVoucherCode.setText(item.code)
                    validateAndApplyVoucher(item.code ?: "")
                }
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onItemSelectedChanged() {
        calculateTotalPrice()
    }

    override fun onItemDeleted(item: CartItem) {

        if (SessionManager(this).getUserId() == -1) return

        item.id?.let { cartItemId ->

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
    
    override fun onQuantityChanged(item: CartItem, newQuantity: Int) {
        if (SessionManager(this).getUserId() == -1) return
        
        item.id?.let { cartItemId ->
            RetrofitClient.getInstance(this).apiService.updateCartItem(cartItemId, newQuantity)
                .enqueue(object : Callback<ApiResponse<Cart>> {
                    override fun onResponse(call: Call<ApiResponse<Cart>>, response: Response<ApiResponse<Cart>>) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            // Cập nhật local data
                            item.quantity = newQuantity
                            item.totalPrice = (item.unitPrice ?: 0.0) * newQuantity
                            cartAdapter.notifyDataSetChanged()
                            calculateTotalPrice()
                        } else {
                            Toast.makeText(this@CartActivity, "Không thể cập nhật số lượng", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<ApiResponse<Cart>>, t: Throwable) {
                        Log.e("CartActivity", "Error updating quantity", t)
                        Toast.makeText(this@CartActivity, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }
    
    override fun onEditOptionsClicked(item: CartItem) {
        showEditCartItemDialog(item)
    }
    
    private fun showEditCartItemDialog(item: CartItem) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_cart_item, null)
        
        val ivDrinkImage: ImageView = dialogView.findViewById(R.id.iv_drink_image)
        val tvDrinkName: TextView = dialogView.findViewById(R.id.tv_drink_name)
        val tvBasePrice: TextView = dialogView.findViewById(R.id.tv_base_price)
        val rgSizes: RadioGroup = dialogView.findViewById(R.id.rg_sizes)
        val llToppings: LinearLayout = dialogView.findViewById(R.id.ll_toppings)
        val etNote: EditText = dialogView.findViewById(R.id.et_note)
        val tvTotalPrice: TextView = dialogView.findViewById(R.id.tv_total_price)
        
        // Set current values
        com.bumptech.glide.Glide.with(this).load(item.drinkImage).into(ivDrinkImage)
        tvDrinkName.text = item.drinkName
        etNote.setText(item.note ?: "")
        
        var selectedSizeId: Long? = item.sizeId
        val selectedToppingIds = item.toppings?.mapNotNull { it.id.toLong() }?.toMutableList() ?: mutableListOf()
        var basePrice = 0.0
        
        // Load drink details to get sizes and toppings
        item.drinkId?.let { drinkId ->
            RetrofitClient.getInstance(this).apiService.getDrinkById(drinkId.toInt())
                .enqueue(object : Callback<ApiResponse<com.example.doan.Models.Drink>> {
                    override fun onResponse(
                        call: Call<ApiResponse<com.example.doan.Models.Drink>>,
                        response: Response<ApiResponse<com.example.doan.Models.Drink>>
                    ) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            val drink = response.body()?.data
                            drink?.let {
                                basePrice = it.basePrice ?: 0.0
                                tvBasePrice.text = String.format(Locale.getDefault(), "%,.0f VNĐ", basePrice)
                                
                                // Add sizes
                                it.sizes?.forEach { size ->
                                    val radioButton = RadioButton(this@CartActivity).apply {
                                        id = View.generateViewId()
                                        text = "${size.sizeName} (+${String.format(Locale.getDefault(), "%,.0f", size.extraPrice ?: 0.0)} VNĐ)"
                                        tag = size.id.toLong()
                                        isChecked = size.id.toLong() == selectedSizeId
                                    }
                                    rgSizes.addView(radioButton)
                                }
                                
                                // Add toppings
                                it.toppings?.forEach { topping ->
                                    val checkBox = CheckBox(this@CartActivity).apply {
                                        text = "${topping.toppingName} (+${String.format(Locale.getDefault(), "%,.0f", topping.price ?: 0.0)} VNĐ)"
                                        tag = topping.id.toLong()
                                        isChecked = selectedToppingIds.contains(topping.id.toLong())
                                        setOnCheckedChangeListener { _, isChecked ->
                                            if (isChecked) {
                                                selectedToppingIds.add(topping.id.toLong())
                                            } else {
                                                selectedToppingIds.remove(topping.id.toLong())
                                            }
                                            updateDialogTotalPrice(drink, selectedSizeId, selectedToppingIds, item.quantity ?: 1, tvTotalPrice)
                                        }
                                    }
                                    llToppings.addView(checkBox)
                                }
                                
                                // Size change listener
                                rgSizes.setOnCheckedChangeListener { group, checkedId ->
                                    val selectedRadio = group.findViewById<RadioButton>(checkedId)
                                    selectedSizeId = selectedRadio?.tag as? Long
                                    updateDialogTotalPrice(drink, selectedSizeId, selectedToppingIds, item.quantity ?: 1, tvTotalPrice)
                                }
                                
                                // Initial total price
                                updateDialogTotalPrice(drink, selectedSizeId, selectedToppingIds, item.quantity ?: 1, tvTotalPrice)
                            }
                        }
                    }
                    
                    override fun onFailure(call: Call<ApiResponse<com.example.doan.Models.Drink>>, t: Throwable) {
                        Log.e("CartActivity", "Error loading drink details", t)
                    }
                })
        }
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("Chỉnh sửa sản phẩm")
            .setView(dialogView)
            .setPositiveButton("Cập nhật") { _, _ ->
                // Get final selected size
                val checkedRadioId = rgSizes.checkedRadioButtonId
                val finalSizeId = if (checkedRadioId != -1) {
                    rgSizes.findViewById<RadioButton>(checkedRadioId)?.tag as? Long
                } else null
                
                updateCartItemFull(item, item.quantity ?: 1, finalSizeId, selectedToppingIds, etNote.text.toString())
            }
            .setNegativeButton("Hủy", null)
            .create()
        
        dialog.show()
    }
    
    private fun updateDialogTotalPrice(
        drink: com.example.doan.Models.Drink,
        sizeId: Long?,
        toppingIds: List<Long>,
        quantity: Int,
        tvTotalPrice: TextView
    ) {
        var total = drink.basePrice ?: 0.0
        
        // Add size price
        sizeId?.let { id ->
            drink.sizes?.find { it.id.toLong() == id }?.let { size ->
                total += size.extraPrice ?: 0.0
            }
        }
        
        // Add topping prices
        toppingIds.forEach { toppingId ->
            drink.toppings?.find { it.id.toLong() == toppingId }?.let { topping ->
                total += topping.price ?: 0.0
            }
        }
        
        total *= quantity
        tvTotalPrice.text = String.format(Locale.getDefault(), "%,.0f VNĐ", total)
    }
    
    private fun updateCartItemFull(item: CartItem, quantity: Int, sizeId: Long?, toppingIds: List<Long>, note: String) {
        item.id?.let { cartItemId ->
            val request = com.example.doan.Models.UpdateCartItemRequest(
                quantity = quantity,
                sizeId = sizeId,
                toppingIds = toppingIds,
                note = note.ifEmpty { null }
            )
            
            RetrofitClient.getInstance(this).apiService.updateCartItemFull(cartItemId, request)
                .enqueue(object : Callback<ApiResponse<Cart>> {
                    override fun onResponse(call: Call<ApiResponse<Cart>>, response: Response<ApiResponse<Cart>>) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            Toast.makeText(this@CartActivity, "Đã cập nhật sản phẩm", Toast.LENGTH_SHORT).show()
                            loadCart() // Reload cart to get updated data
                        } else {
                            Toast.makeText(this@CartActivity, "Không thể cập nhật sản phẩm", Toast.LENGTH_SHORT).show()
                        }
                    }
                    
                    override fun onFailure(call: Call<ApiResponse<Cart>>, t: Throwable) {
                        Log.e("CartActivity", "Error updating cart item", t)
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

    private fun performPlaceOrder(items: List<CartItem>, storeId: Int, paymentMethod: String, deliveryAddress: String? = null) {

        if (appliedVoucher != null || appliedSpinVoucher != null) {
            revalidateVoucherBeforeCheckout(items, storeId, paymentMethod, deliveryAddress)
        } else {
            proceedWithOrder(items, storeId, paymentMethod, deliveryAddress)
        }
    }
    

    private fun revalidateVoucherBeforeCheckout(
        items: List<CartItem>, 
        storeId: Int, 
        paymentMethod: String, 
        deliveryAddress: String?
    ) {
        val code = appliedVoucher?.code ?: appliedSpinVoucher?.voucherCode
        if (code.isNullOrEmpty()) {
            proceedWithOrder(items, storeId, paymentMethod, deliveryAddress)
            return
        }
        
        val totalPrice = items.sumOf { (it.unitPrice ?: 0.0) * (it.quantity ?: 1) }
        
        // Validate spin voucher
        if (appliedSpinVoucher != null) {
            RetrofitClient.getInstance(this).apiService.validateSpinVoucher(code)
                .enqueue(object : Callback<ApiResponse<com.example.doan.Models.SpinRewardDto>> {
                    override fun onResponse(
                        call: Call<ApiResponse<com.example.doan.Models.SpinRewardDto>>,
                        response: Response<ApiResponse<com.example.doan.Models.SpinRewardDto>>
                    ) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            // Voucher còn hợp lệ, tiếp tục đặt hàng
                            proceedWithOrder(items, storeId, paymentMethod, deliveryAddress)
                        } else {
                            // Voucher hết hạn hoặc đã dùng
                            clearAppliedVoucher()
                            Toast.makeText(this@CartActivity, 
                                "Voucher đã hết hạn hoặc đã được sử dụng. Vui lòng chọn voucher khác.", 
                                Toast.LENGTH_LONG).show()
                        }
                    }
                    
                    override fun onFailure(call: Call<ApiResponse<com.example.doan.Models.SpinRewardDto>>, t: Throwable) {
                        Toast.makeText(this@CartActivity, "Lỗi kiểm tra voucher", Toast.LENGTH_SHORT).show()
                    }
                })
        } 
        // Validate normal voucher
        else if (appliedVoucher != null) {
            RetrofitClient.getInstance(this).apiService.validatePromotion(code, totalPrice)
                .enqueue(object : Callback<ApiResponse<com.example.doan.Models.Voucher>> {
                    override fun onResponse(
                        call: Call<ApiResponse<com.example.doan.Models.Voucher>>,
                        response: Response<ApiResponse<com.example.doan.Models.Voucher>>
                    ) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            // Voucher còn hợp lệ, tiếp tục đặt hàng
                            proceedWithOrder(items, storeId, paymentMethod, deliveryAddress)
                        } else {
                            // Voucher hết hạn hoặc không hợp lệ
                            clearAppliedVoucher()
                            Toast.makeText(this@CartActivity, 
                                "Voucher không còn hợp lệ. Vui lòng chọn voucher khác.", 
                                Toast.LENGTH_LONG).show()
                        }
                    }
                    
                    override fun onFailure(call: Call<ApiResponse<com.example.doan.Models.Voucher>>, t: Throwable) {
                        Toast.makeText(this@CartActivity, "Lỗi kiểm tra voucher", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }
    

    private fun proceedWithOrder(items: List<CartItem>, storeId: Int, paymentMethod: String, deliveryAddress: String? = null) {
        val loadingDialog = LoadingDialog(this)
        loadingDialog.show("Đang xử lý đơn hàng...")

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
            type = selectedDeliveryType,
            paymentMethod = paymentMethod,
            address = deliveryAddress,
            promotionCode = appliedVoucher?.code,
            spinVoucherCode = appliedSpinVoucher?.voucherCode
        )

        // Nếu là VNPAY, tạo payment URL trước, KHÔNG tạo đơn hàng
        if (paymentMethod == "VNPAY") {
            // Tính tổng tiền để tạo payment URL
            val totalAmount = calculateFinalAmount(items)
            createVNPayPaymentFirst(totalAmount, request, loadingDialog)
            return
        }

        // Nếu là VIETQR, chuyển sang màn hình VietQR
        if (paymentMethod == "VIETQR") {
            val totalAmount = calculateFinalAmount(items)
            loadingDialog.dismiss()
            navigateToVietQRPayment(totalAmount, items, request)
            return
        }

        // Nếu là MOMO, xử lý thanh toán MoMo
        if (paymentMethod == "MOMO") {
            val totalAmount = calculateFinalAmount(items)
            loadingDialog.dismiss()
            // Lưu request để dùng sau khi thanh toán MoMo thành công
            pendingOrderRequest = request
            pendingCartItems = items
            requestMoMoPayment(totalAmount)
            return
        }

        // Nếu là PAYPAL, xử lý thanh toán PayPal
        if (paymentMethod == "PAYPAL") {
            val totalAmount = calculateFinalAmount(items)
            loadingDialog.dismiss()
            // Lưu request để dùng sau khi thanh toán PayPal thành công
            pendingOrderRequest = request
            pendingCartItems = items
            requestPayPalPayment(totalAmount)
            return
        }

        // COD - Tạo đơn hàng ngay
        Log.d("CartActivity", "Creating COD order with spinVoucherCode: ${appliedSpinVoucher?.voucherCode}")

        RetrofitClient.getInstance(this).apiService.createOrder(request).enqueue(object: Callback<ApiResponse<Order>> {
            override fun onResponse(call: Call<ApiResponse<Order>>, response: Response<ApiResponse<Order>>) {
                if(response.isSuccessful && response.body()?.success == true) {
                    val order = response.body()?.data
                    // FIX: Chỉ xóa những sản phẩm đã mua, giữ lại các sản phẩm khác trong giỏ hàng
                    removeSelectedItemsFromCart(items)
                    appliedVoucher = null
                    appliedSpinVoucher = null

                    loadingDialog.dismiss()

                    // Show confetti celebration
                    SeasonalEffectManager.showConfetti(3000L, 200)

                    // Show beautiful order success notification
                    InAppNotification.orderSuccess(
                        this@CartActivity,
                        order?.id?.toString() ?: "N/A"
                    )

                    // Navigate after delay to let user see celebration
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        navigateToOrders()
                    }, 2500)
                } else {
                    loadingDialog.dismiss()
                    val errorMsg = response.body()?.message ?: "Đặt hàng thất bại"

                    // Nếu lỗi liên quan đến voucher, clear voucher đã chọn
                    if (errorMsg.contains("voucher", ignoreCase = true) || 
                        errorMsg.contains("đã được sử dụng", ignoreCase = true)) {
                        clearAppliedVoucher()
                    }

                    InAppNotification.error(this@CartActivity, "Đặt hàng thất bại", errorMsg)
                }
            }

            override fun onFailure(call: Call<ApiResponse<Order>>, t: Throwable) {
                loadingDialog.dismiss()
                InAppNotification.error(this@CartActivity, "Lỗi kết nối", t.message ?: "Không thể kết nối đến server")
            }
        })
    }

    private fun calculateFinalAmount(items: List<CartItem>): Long {
        val total = items.sumOf { (it.unitPrice ?: 0.0) * (it.quantity ?: 1) }
        val finalAmount = maxOf(0.0, total - discountAmount)
        return finalAmount.toLong()
    }

    private fun createVNPayPaymentFirst(amount: Long, orderRequest: CreateOrderRequest, loadingDialog: LoadingDialog) {
        // Tạo payment URL với amount, không cần orderId
        val request = VNPayPaymentRequest(
            orderId = 0, // Sẽ được xử lý ở backend
            orderInfo = "Thanh toan UTE Tea",
            ipAddress = "127.0.0.1"
        )

        RetrofitClient.getInstance(this).apiService.createVNPayPaymentWithAmount(amount, request.orderInfo ?: "")
            .enqueue(object : Callback<ApiResponse<VNPayPaymentResponse>> {
                override fun onResponse(
                    call: Call<ApiResponse<VNPayPaymentResponse>>,
                    response: Response<ApiResponse<VNPayPaymentResponse>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val paymentUrl = response.body()?.data?.paymentUrl
                        if (!paymentUrl.isNullOrEmpty()) {
                            loadingDialog.dismiss()

                            // Lấy danh sách cartItemIds đã chọn để xóa sau khi thanh toán thành công
                            val selectedItems = cartAdapter.getSelectedItems()
                            val cartItemIds = selectedItems.mapNotNull { it.id }

                            // Mở WebView để thanh toán, truyền thêm orderRequest để tạo đơn sau
                            val intent = Intent(this@CartActivity, VNPayPaymentActivity::class.java)
                            intent.putExtra("PAYMENT_URL", paymentUrl)
                            intent.putExtra("ORDER_REQUEST", com.google.gson.Gson().toJson(orderRequest))
                            intent.putExtra("VOUCHER_CODE", appliedVoucher?.code)
                            intent.putExtra("SPIN_VOUCHER_CODE", appliedSpinVoucher?.voucherCode)
                            intent.putExtra("CART_ITEM_IDS", cartItemIds.toLongArray())
                            startActivity(intent)
                            finish()
                        } else {
                            loadingDialog.dismiss()
                            Toast.makeText(this@CartActivity, "Không thể tạo URL thanh toán", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        loadingDialog.dismiss()
                        Toast.makeText(this@CartActivity, "Lỗi tạo thanh toán: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<VNPayPaymentResponse>>, t: Throwable) {
                    loadingDialog.dismiss()
                    Log.e("CartActivity", "Error creating VNPAY payment", t)
                    Toast.makeText(this@CartActivity, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun createVNPayPayment(orderId: Long, loadingDialog: LoadingDialog) {
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
                            clearCartOnServerAsync()

                            loadingDialog.dismiss()
                            val intent = Intent(this@CartActivity, VNPayPaymentActivity::class.java)
                            intent.putExtra("PAYMENT_URL", paymentUrl)
                            startActivity(intent)
                            finish()
                        } else {
                            loadingDialog.dismiss()
                            Toast.makeText(this@CartActivity, "Không thể tạo URL thanh toán", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        loadingDialog.dismiss()
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

    private fun navigateToVietQRPayment(totalAmount: Long, items: List<CartItem>, orderRequest: CreateOrderRequest) {
        // Convert cart items to order summary items
        val orderSummaryItems = items.map { item ->
            com.example.doan.Models.OrderSummaryItem(
                drinkName = item.drinkName ?: "Đồ uống",
                sizeName = item.sizeName ?: "M",
                quantity = item.quantity ?: 1,
                unitPrice = item.unitPrice ?: 0.0,
                toppings = item.toppings?.mapNotNull { it.toppingName } ?: emptyList()
            )
        }

        // Lấy danh sách cartItemIds đã chọn để xóa sau khi thanh toán thành công
        val cartItemIds = items.mapNotNull { it.id }

        val intent = Intent(this, VietQRActivity::class.java).apply {
            putExtra("ORDER_ID", System.currentTimeMillis())
            putExtra("TOTAL_AMOUNT", totalAmount.toDouble())
            putExtra("ORDER_REQUEST", com.google.gson.Gson().toJson(orderRequest))
            putExtra("ORDER_ITEMS", com.google.gson.Gson().toJson(orderSummaryItems))
            putExtra("VOUCHER_CODE", appliedVoucher?.code)
            putExtra("SPIN_VOUCHER_CODE", appliedSpinVoucher?.voucherCode)
            putExtra("CART_ITEM_IDS", cartItemIds.toLongArray())
        }
        startActivity(intent)
        // Không finish() để user có thể quay lại khi hủy thanh toán
    }


    private fun removeSelectedItemsFromCart(selectedItems: List<CartItem>) {
        if (selectedItems.isEmpty()) return

        // Xóa từng item đã được chọn mua
        for (item in selectedItems) {
            item.id?.let { cartItemId ->
                RetrofitClient.getInstance(this).apiService.removeCartItem(cartItemId)
                    .enqueue(object : Callback<ApiResponse<Void>> {
                        override fun onResponse(call: Call<ApiResponse<Void>>, response: Response<ApiResponse<Void>>) {
                            if (!response.isSuccessful) {
                                Log.e("CartActivity", "Failed to remove cart item: $cartItemId")
                            } else {
                                Log.d("CartActivity", "Removed cart item: $cartItemId")
                            }
                        }

                        override fun onFailure(call: Call<ApiResponse<Void>>, t: Throwable) {
                            Log.e("CartActivity", "Error removing cart item: $cartItemId", t)
                        }
                    })
            }
        }
    }


    private fun clearCartOnServerAsync() {
        val userId = SessionManager(this).getUserId()
        if (userId == -1) return

        // Gọi async, không chờ response
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

    private fun clearCartOnServer() {
        clearCartOnServerAsync()
    }


    private fun requestMoMoPayment(amount: Long) {
        val loadingDialog = LoadingDialog(this)
        loadingDialog.show("Đang tạo thanh toán MoMo...")

        // Lưu ORDER_REQUEST JSON trước khi gọi API để tránh race condition
        val orderRequestJson = com.google.gson.Gson().toJson(pendingOrderRequest)
        val savedVoucherCode = appliedVoucher?.code
        val savedSpinVoucherCode = appliedSpinVoucher?.voucherCode
        val selectedItems = cartAdapter.getSelectedItems()
        val cartItemIds = selectedItems.mapNotNull { it.id }
        
        Log.d(TAG, "MoMo - pendingOrderRequest: $pendingOrderRequest")
        Log.d(TAG, "MoMo - orderRequestJson: $orderRequestJson")

        com.example.doan.Services.PaymentService.createMoMoPayment(
            this,
            amount,
            "Thanh toan UTE Tea",
            object : com.example.doan.Services.PaymentService.PaymentCallback {
                override fun onSuccess(paymentUrl: String, transactionId: String?) {
                    loadingDialog.dismiss()
                    
                    Log.d(TAG, "MoMo success - paymentUrl: $paymentUrl")
                    Log.d(TAG, "MoMo success - orderRequestJson length: ${orderRequestJson?.length}")
                    
                    // Kiểm tra orderRequestJson trước khi mở Activity
                    if (orderRequestJson.isNullOrEmpty() || orderRequestJson == "null") {
                        Toast.makeText(this@CartActivity, "Lỗi: Không thể tạo thông tin đơn hàng", Toast.LENGTH_SHORT).show()
                        return
                    }
                    
                    // Mở WebView để thanh toán MoMo
                    val intent = Intent(this@CartActivity, MoMoPaymentActivity::class.java)
                    intent.putExtra("PAYMENT_URL", paymentUrl)
                    intent.putExtra("MOMO_ORDER_ID", transactionId)
                    intent.putExtra("ORDER_REQUEST", orderRequestJson)
                    intent.putExtra("VOUCHER_CODE", savedVoucherCode)
                    intent.putExtra("SPIN_VOUCHER_CODE", savedSpinVoucherCode)
                    intent.putExtra("CART_ITEM_IDS", cartItemIds.toLongArray())
                    startActivity(intent)
                }

                override fun onError(message: String) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@CartActivity, "Lỗi MoMo: $message", Toast.LENGTH_SHORT).show()
                    pendingOrderRequest = null
                    pendingCartItems = null
                }
            }
        )
    }


    private fun requestPayPalPayment(amount: Long) {
        val loadingDialog = LoadingDialog(this)
        loadingDialog.show("Đang tạo thanh toán PayPal...")

        // Convert VND to USD
        val amountUSD = com.example.doan.Services.PaymentService.convertVNDtoUSD(amount)

        com.example.doan.Services.PaymentService.createPayPalPayment(
            this,
            amountUSD,
            "USD",
            "Thanh toan UTE Tea",
            object : com.example.doan.Services.PaymentService.PaymentCallback {
                override fun onSuccess(paymentUrl: String, transactionId: String?) {
                    loadingDialog.dismiss()
                    
                    // Lấy danh sách cartItemIds đã chọn để xóa sau khi thanh toán thành công
                    val selectedItems = cartAdapter.getSelectedItems()
                    val cartItemIds = selectedItems.mapNotNull { it.id }
                    
                    // Mở WebView để thanh toán PayPal
                    val intent = Intent(this@CartActivity, PayPalPaymentActivity::class.java)
                    intent.putExtra("PAYMENT_URL", paymentUrl)
                    intent.putExtra("ORDER_REQUEST", com.google.gson.Gson().toJson(pendingOrderRequest))
                    intent.putExtra("VOUCHER_CODE", appliedVoucher?.code)
                    intent.putExtra("SPIN_VOUCHER_CODE", appliedSpinVoucher?.voucherCode)
                    intent.putExtra("CART_ITEM_IDS", cartItemIds.toLongArray())
                    startActivity(intent)
                }

                override fun onError(message: String) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@CartActivity, "Lỗi PayPal: $message", Toast.LENGTH_SHORT).show()
                    pendingOrderRequest = null
                    pendingCartItems = null
                }
            }
        )
    }
}
