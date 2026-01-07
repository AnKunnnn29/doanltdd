package com.example.doan.Activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Adapters.GroupOrderItemAdapter
import com.example.doan.Adapters.GroupOrderMemberAdapter
import com.example.doan.Models.*
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.LoadingDialog
import com.example.doan.Utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class GroupOrderActivity : AppCompatActivity() {

    private lateinit var tvInviteCode: TextView
    private lateinit var tvExpiresAt: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvStoreName: TextView
    private lateinit var tvTotalPrice: TextView
    private lateinit var tvEmptyItems: TextView
    private lateinit var rvMembers: RecyclerView
    private lateinit var rvItems: RecyclerView
    private lateinit var btnCopyCode: Button
    private lateinit var btnShareCode: Button
    private lateinit var btnAddItem: Button
    private lateinit var btnLockUnlock: Button
    private lateinit var btnCheckout: Button
    private lateinit var btnLeave: Button
    private lateinit var btnCancel: Button
    private lateinit var btnChat: Button
    private lateinit var llHostActions: LinearLayout

    private lateinit var memberAdapter: GroupOrderMemberAdapter
    private lateinit var itemAdapter: GroupOrderItemAdapter
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var sessionManager: SessionManager

    private var groupOrderId: Long = 0
    private var groupOrder: GroupOrderDto? = null
    private var isHost = false
    private var countDownTimer: CountDownTimer? = null
    
    // Shipping fee - tính theo tỉnh/thành phố trong địa chỉ giao hàng
    private var shippingFee: Int = 0
    
    // ✅ OTP RATE LIMITING
    private var lastOtpSentTime = 0L
    private val OTP_COOLDOWN = 60_000L // 60 giây
    private var currentUserProfile: UserProfileDto? = null
    
    // ✅ REALTIME POLLING - Cập nhật thành viên và món mới
    private val pollingHandler = Handler(Looper.getMainLooper())
    private var isPollingActive = false
    private val POLLING_INTERVAL = 5000L // 5 giây
    private var lastMemberCount = 0
    private var lastItemCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_order)

        loadingDialog = LoadingDialog(this)
        sessionManager = SessionManager(this)
        groupOrderId = intent.getLongExtra("GROUP_ORDER_ID", 0)

        if (groupOrderId == 0L) {
            Toast.makeText(this, "Không tìm thấy phiên đặt hàng", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        loadGroupOrder()
        loadUserProfile()
    }
    
    private fun loadUserProfile() {
        RetrofitClient.getInstance(this).apiService.getMyProfile().enqueue(object : Callback<ApiResponse<UserProfileDto>> {
            override fun onResponse(call: Call<ApiResponse<UserProfileDto>>, response: Response<ApiResponse<UserProfileDto>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    currentUserProfile = response.body()?.data
                }
            }

            override fun onFailure(call: Call<ApiResponse<UserProfileDto>>, t: Throwable) {
                Log.e("GroupOrderActivity", "Failed to load user profile", t)
            }
        })
    }

    private fun initViews() {
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).setNavigationOnClickListener {
            finish()
        }

        tvInviteCode = findViewById(R.id.tv_invite_code)
        tvExpiresAt = findViewById(R.id.tv_expires_at)
        tvStatus = findViewById(R.id.tv_status)
        tvStoreName = findViewById(R.id.tv_store_name)
        tvTotalPrice = findViewById(R.id.tv_total_price)
        tvEmptyItems = findViewById(R.id.tv_empty_items)
        rvMembers = findViewById(R.id.rv_members)
        rvItems = findViewById(R.id.rv_items)
        btnCopyCode = findViewById(R.id.btn_copy_code)
        btnShareCode = findViewById(R.id.btn_share_code)
        btnAddItem = findViewById(R.id.btn_add_item)
        btnLockUnlock = findViewById(R.id.btn_lock_unlock)
        btnCheckout = findViewById(R.id.btn_checkout)
        btnLeave = findViewById(R.id.btn_leave)
        btnCancel = findViewById(R.id.btn_cancel)
        btnChat = findViewById(R.id.btn_chat)
        llHostActions = findViewById(R.id.ll_host_actions)

        // Setup RecyclerViews
        rvMembers.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvItems.layoutManager = LinearLayoutManager(this)

        // Button listeners
        btnCopyCode.setOnClickListener { copyInviteCode() }
        btnShareCode.setOnClickListener { shareInviteCode() }
        btnAddItem.setOnClickListener { openAddItemDialog() }
        btnLockUnlock.setOnClickListener { toggleLock() }
        btnCheckout.setOnClickListener { showCheckoutDialog() }
        btnLeave.setOnClickListener { confirmLeave() }
        btnCancel.setOnClickListener { confirmCancel() }
        btnChat.setOnClickListener { openGroupChat() }
        
        // Host có thể click vào store name để thay đổi
        tvStoreName.setOnClickListener { 
            if (isHost && (groupOrder?.status == GroupOrderStatus.OPEN || groupOrder?.status == GroupOrderStatus.LOCKED)) {
                showEditStoreDialog()
            }
        }
    }
    
    private fun showEditStoreDialog() {
        val cachedStores = com.example.doan.Utils.DataCache.stores
        if (cachedStores.isNullOrEmpty()) {
            Toast.makeText(this, "Không thể tải danh sách chi nhánh", Toast.LENGTH_SHORT).show()
            return
        }
        
        val storeNames = cachedStores.map { it.storeName ?: "Chi nhánh ${it.id}" }.toTypedArray()
        var selectedIndex = cachedStores.indexOfFirst { it.id?.toLong() == groupOrder?.storeId }
        if (selectedIndex < 0) selectedIndex = 0
        
        AlertDialog.Builder(this)
            .setTitle("Chọn chi nhánh")
            .setSingleChoiceItems(storeNames, selectedIndex) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton("Xác nhận") { _, _ ->
                val selectedStore = cachedStores[selectedIndex]
                updateGroupOrderStore(selectedStore.id?.toLong())
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
    
    private fun updateGroupOrderStore(storeId: Long?) {
        if (storeId == null) return
        
        loadingDialog.show("Đang cập nhật...")
        
        val request = UpdateGroupOrderRequest(storeId = storeId)
        RetrofitClient.getInstance(this).apiService.updateGroupOrder(groupOrderId, request)
            .enqueue(object : Callback<ApiResponse<GroupOrderDto>> {
                override fun onResponse(
                    call: Call<ApiResponse<GroupOrderDto>>,
                    response: Response<ApiResponse<GroupOrderDto>>
                ) {
                    loadingDialog.dismiss()
                    if (response.isSuccessful && response.body()?.success == true) {
                        groupOrder = response.body()?.data
                        updateUI()
                        Toast.makeText(this@GroupOrderActivity, "Đã cập nhật chi nhánh", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@GroupOrderActivity,
                            response.body()?.message ?: "Lỗi cập nhật", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<GroupOrderDto>>, t: Throwable) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@GroupOrderActivity, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun loadGroupOrder() {
        loadingDialog.show("Đang tải...")

        RetrofitClient.getInstance(this).apiService.getGroupOrder(groupOrderId)
            .enqueue(object : Callback<ApiResponse<GroupOrderDto>> {
                override fun onResponse(
                    call: Call<ApiResponse<GroupOrderDto>>,
                    response: Response<ApiResponse<GroupOrderDto>>
                ) {
                    loadingDialog.dismiss()
                    if (response.isSuccessful && response.body()?.success == true) {
                        groupOrder = response.body()?.data
                        updateUI()
                    } else {
                        Toast.makeText(this@GroupOrderActivity,
                            response.body()?.message ?: "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<GroupOrderDto>>, t: Throwable) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@GroupOrderActivity, 
                        "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
    }

    private fun updateUI() {
        val order = groupOrder ?: return
        val currentUserId = sessionManager.getUserId().toLong()
        isHost = order.hostUserId == currentUserId

        // Invite code
        tvInviteCode.text = order.inviteCode ?: ""

        // Status
        updateStatusUI(order.status)

        // Store
        tvStoreName.text = order.storeName ?: "Chưa chọn"

        // Total price
        tvTotalPrice.text = String.format(Locale.getDefault(), "%,.0f VNĐ", order.totalPrice ?: 0.0)

        // Expiration countdown
        startExpirationCountdown(order.expiresAt)
        
        // Calculate shipping fee if DELIVERY
        calculateShippingFee()

        // Members
        val members = order.members ?: emptyList()
        memberAdapter = GroupOrderMemberAdapter(
            members,
            onKickMember = { /* TODO: Implement kick */ },
            isHost = isHost
        )
        rvMembers.adapter = memberAdapter

        // Items
        val items = order.items ?: emptyList()
        val isOpenStatus = order.status == GroupOrderStatus.OPEN
        itemAdapter = GroupOrderItemAdapter(
            items,
            currentUserId,
            isHost,
            isOpenStatus, // Chỉ cho phép edit/delete khi phiên OPEN
            onEditItem = { /* TODO: Implement edit */ },
            onDeleteItem = { item -> confirmDeleteItem(item) }
        )
        rvItems.adapter = itemAdapter

        // Empty state
        tvEmptyItems.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        rvItems.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        
        // Update tracking counts cho polling
        lastMemberCount = order.currentMemberCount ?: members.size
        lastItemCount = items.size

        // Action buttons visibility
        updateActionButtons(order.status)
    }
    
    /**
     * Tính phí ship dựa trên địa chỉ giao hàng
     * Sử dụng VietnamProvinces để tính phí ship theo tỉnh/thành phố
     */
    private fun calculateShippingFee() {
        val order = groupOrder ?: return
        
        if (order.orderType == "DELIVERY" && !order.deliveryAddress.isNullOrEmpty()) {
            // Tìm tỉnh/thành phố trong địa chỉ
            val address = order.deliveryAddress!!
            val provinceNames = com.example.doan.Utils.VietnamProvinces.getProvinceNames()
            
            // Tìm tỉnh/thành phố trong địa chỉ
            var foundProvince: String? = null
            for (province in provinceNames) {
                if (address.contains(province, ignoreCase = true)) {
                    foundProvince = province
                    break
                }
            }
            
            shippingFee = if (foundProvince != null) {
                com.example.doan.Utils.VietnamProvinces.getShippingFee(foundProvince)
            } else {
                // Mặc định phí ship nếu không tìm thấy tỉnh
                30000
            }
        } else {
            shippingFee = 0
        }
    }

    private fun updateStatusUI(status: GroupOrderStatus?) {
        when (status) {
            GroupOrderStatus.OPEN -> {
                tvStatus.text = "Đang mở"
                tvStatus.setTextColor(getColor(android.R.color.holo_green_dark))
            }
            GroupOrderStatus.LOCKED -> {
                tvStatus.text = "Đã khóa"
                tvStatus.setTextColor(getColor(android.R.color.holo_orange_dark))
            }
            GroupOrderStatus.COMPLETED -> {
                tvStatus.text = "Hoàn thành"
                tvStatus.setTextColor(getColor(android.R.color.holo_blue_dark))
            }
            GroupOrderStatus.CANCELLED -> {
                tvStatus.text = "Đã hủy"
                tvStatus.setTextColor(getColor(android.R.color.holo_red_dark))
            }
            GroupOrderStatus.EXPIRED -> {
                tvStatus.text = "Hết hạn"
                tvStatus.setTextColor(getColor(android.R.color.darker_gray))
            }
            else -> {
                tvStatus.text = "Không xác định"
            }
        }
    }

    private fun updateActionButtons(status: GroupOrderStatus?) {
        val isActive = status == GroupOrderStatus.OPEN || status == GroupOrderStatus.LOCKED
        val isOpen = status == GroupOrderStatus.OPEN

        // Chỉ cho phép thêm món khi phiên OPEN (không phải LOCKED)
        btnAddItem.visibility = if (isOpen) View.VISIBLE else View.GONE

        if (isHost) {
            llHostActions.visibility = if (isActive) View.VISIBLE else View.GONE
            btnCancel.visibility = if (isActive) View.VISIBLE else View.GONE
            btnLeave.visibility = View.GONE

            btnLockUnlock.text = if (status == GroupOrderStatus.LOCKED) "Mở khóa" else "Khóa đơn"
            
            // Chỉ cho checkout khi OPEN hoặc LOCKED
            btnCheckout.isEnabled = isActive
        } else {
            llHostActions.visibility = View.GONE
            btnCancel.visibility = View.GONE
            // Member có thể rời khi phiên OPEN hoặc LOCKED
            btnLeave.visibility = if (isActive) View.VISIBLE else View.GONE
        }
    }

    private fun startExpirationCountdown(expiresAt: String?) {
        countDownTimer?.cancel()
        
        // Sử dụng remainingSeconds từ server (đã tính sẵn)
        val remainingSeconds = groupOrder?.remainingSeconds ?: 0L
        
        if (remainingSeconds <= 0) {
            tvExpiresAt.text = "Đã hết hạn"
            tvExpiresAt.setTextColor(getColor(android.R.color.holo_red_dark))
            return
        }

        countDownTimer = object : CountDownTimer(remainingSeconds * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val hours = millisUntilFinished / 3600000
                val minutes = (millisUntilFinished % 3600000) / 60000
                val seconds = (millisUntilFinished % 60000) / 1000
                
                val timeText = if (hours > 0) {
                    "⏰ ${hours}h ${minutes}p ${seconds}s"
                } else {
                    "⏰ ${minutes}p ${seconds}s"
                }
                tvExpiresAt.text = timeText
                
                // Đổi màu khi còn ít thời gian
                if (millisUntilFinished < 300000) { // < 5 phút
                    tvExpiresAt.setTextColor(getColor(android.R.color.holo_red_dark))
                } else if (millisUntilFinished < 600000) { // < 10 phút
                    tvExpiresAt.setTextColor(getColor(android.R.color.holo_orange_dark))
                } else {
                    tvExpiresAt.setTextColor(getColor(android.R.color.white))
                }
            }

            override fun onFinish() {
                tvExpiresAt.text = "Đã hết hạn"
                tvExpiresAt.setTextColor(getColor(android.R.color.holo_red_dark))
                loadGroupOrder() // Reload to update status
            }
        }.start()
    }

    private fun copyInviteCode() {
        val code = groupOrder?.inviteCode ?: return
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Invite Code", code)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Đã sao chép mã: $code", Toast.LENGTH_SHORT).show()
    }

    private fun shareInviteCode() {
        val code = groupOrder?.inviteCode ?: return
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, 
                "Tham gia đặt hàng cùng mình nhé! Mã mời: $code\n" +
                "Mở app và nhập mã để tham gia.")
        }
        startActivity(Intent.createChooser(shareIntent, "Chia sẻ mã mời"))
    }

    private fun openAddItemDialog() {
        // Navigate to SelectDrinkForGroupActivity
        val intent = Intent(this, SelectDrinkForGroupActivity::class.java).apply {
            putExtra("GROUP_ORDER_ID", groupOrderId)
        }
        startActivityForResult(intent, REQUEST_ADD_ITEM)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ADD_ITEM && resultCode == RESULT_OK) {
            // Reload group order to show new item
            loadGroupOrder()
        }
    }
    
    companion object {
        private const val REQUEST_ADD_ITEM = 100
    }
    
    private fun openGroupChat() {
        val order = groupOrder ?: return
        val intent = Intent(this, GroupChatActivity::class.java).apply {
            putExtra(GroupChatActivity.EXTRA_GROUP_ORDER_ID, groupOrderId)
            putExtra(GroupChatActivity.EXTRA_GROUP_NAME, order.name ?: "Chat nhóm")
            putExtra(GroupChatActivity.EXTRA_MEMBER_COUNT, order.currentMemberCount ?: 0)
        }
        startActivity(intent)
    }

    private fun toggleLock() {
        loadingDialog.show("Đang xử lý...")
        
        val call = if (groupOrder?.status == GroupOrderStatus.LOCKED) {
            RetrofitClient.getInstance(this).apiService.unlockGroupOrder(groupOrderId)
        } else {
            RetrofitClient.getInstance(this).apiService.lockGroupOrder(groupOrderId)
        }

        call.enqueue(object : Callback<ApiResponse<GroupOrderDto>> {
            override fun onResponse(
                call: Call<ApiResponse<GroupOrderDto>>,
                response: Response<ApiResponse<GroupOrderDto>>
            ) {
                loadingDialog.dismiss()
                if (response.isSuccessful && response.body()?.success == true) {
                    groupOrder = response.body()?.data
                    updateUI()
                    Toast.makeText(this@GroupOrderActivity, 
                        response.body()?.message ?: "Thành công", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@GroupOrderActivity,
                        response.body()?.message ?: "Lỗi", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<GroupOrderDto>>, t: Throwable) {
                loadingDialog.dismiss()
                Toast.makeText(this@GroupOrderActivity, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showCheckoutDialog() {
        // Validate trước khi checkout
        if (groupOrder?.items.isNullOrEmpty()) {
            Toast.makeText(this, "Chưa có món nào trong đơn", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (groupOrder?.storeId == null) {
            Toast.makeText(this, "Vui lòng chọn chi nhánh trước khi thanh toán", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Kiểm tra nếu là DELIVERY thì phải có địa chỉ
        if (groupOrder?.orderType == "DELIVERY" && groupOrder?.deliveryAddress.isNullOrEmpty()) {
            Toast.makeText(this, "Vui lòng nhập địa chỉ giao hàng", Toast.LENGTH_SHORT).show()
            // Hiển thị dialog nhập địa chỉ
            showDeliveryAddressDialog("")
            return
        }
        
        // Tính phí ship trước khi thanh toán
        calculateShippingFee()
        
        // ✅ XÁC THỰC OTP TRƯỚC KHI THANH TOÁN
        val phoneNumber = currentUserProfile?.phone
        if (phoneNumber.isNullOrEmpty()) {
            // Nếu chưa có số điện thoại, yêu cầu nhập
            showEnterPhoneDialog()
            return
        }
        
        // Hiển thị dialog xác thực OTP
        showOtpVerificationDialog(phoneNumber)
    }
    
    /**
     * Hiển thị dialog nhập địa chỉ giao hàng (chỉ khi cần)
     */
    private fun showDeliveryAddressDialog(currentAddress: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delivery_address, null)
        val etAddress = dialogView.findViewById<EditText>(R.id.et_delivery_address)
        val tvShippingFee = dialogView.findViewById<TextView>(R.id.tv_shipping_fee_preview)
        
        etAddress.setText(currentAddress)
        
        // Tính phí ship khi nhập địa chỉ
        etAddress.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val address = s.toString()
                val fee = calculateShippingFeeForAddress(address)
                tvShippingFee.text = if (fee > 0) {
                    "Phí giao hàng dự kiến: ${formatPrice(fee.toDouble())}"
                } else {
                    "Nhập địa chỉ để tính phí giao hàng"
                }
            }
        })
        
        // Trigger initial calculation
        val initialFee = calculateShippingFeeForAddress(currentAddress)
        tvShippingFee.text = if (initialFee > 0) {
            "Phí giao hàng dự kiến: ${formatPrice(initialFee.toDouble())}"
        } else {
            "Nhập địa chỉ để tính phí giao hàng"
        }
        
        AlertDialog.Builder(this)
            .setTitle("Nhập địa chỉ giao hàng")
            .setView(dialogView)
            .setPositiveButton("Tiếp tục") { _, _ ->
                val address = etAddress.text.toString().trim()
                if (address.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập địa chỉ giao hàng", Toast.LENGTH_SHORT).show()
                    showDeliveryAddressDialog("")
                    return@setPositiveButton
                }
                // Cập nhật địa chỉ và tiếp tục thanh toán
                updateDeliveryAddressAndContinue(address)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
    
    /**
     * Tính phí ship cho địa chỉ
     */
    private fun calculateShippingFeeForAddress(address: String): Int {
        if (address.isEmpty()) return 0
        
        val provinceNames = com.example.doan.Utils.VietnamProvinces.getProvinceNames()
        for (province in provinceNames) {
            if (address.contains(province, ignoreCase = true)) {
                return com.example.doan.Utils.VietnamProvinces.getShippingFee(province)
            }
        }
        return 30000 // Mặc định
    }
    
    /**
     * Cập nhật địa chỉ giao hàng và tiếp tục thanh toán
     */
    private fun updateDeliveryAddressAndContinue(address: String) {
        loadingDialog.show("Đang cập nhật...")
        
        val request = UpdateGroupOrderRequest(deliveryAddress = address)
        
        RetrofitClient.getInstance(this).apiService.updateGroupOrder(groupOrderId, request)
            .enqueue(object : Callback<ApiResponse<GroupOrderDto>> {
                override fun onResponse(
                    call: Call<ApiResponse<GroupOrderDto>>,
                    response: Response<ApiResponse<GroupOrderDto>>
                ) {
                    loadingDialog.dismiss()
                    if (response.isSuccessful && response.body()?.success == true) {
                        groupOrder = response.body()?.data
                        updateUI()
                        
                        // Tính lại phí ship
                        calculateShippingFee()
                        
                        // Tiếp tục chọn phương thức thanh toán
                        showPaymentMethodDialog()
                    } else {
                        Toast.makeText(this@GroupOrderActivity,
                            response.body()?.message ?: "Lỗi cập nhật", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<GroupOrderDto>>, t: Throwable) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@GroupOrderActivity, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                }
            })
    }
    
    /**
     * Hiển thị dialog nhập số điện thoại nếu chưa có
     */
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
    
    /**
     * Cập nhật số điện thoại cho user
     */
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
                    Toast.makeText(this@GroupOrderActivity, "Cập nhật số điện thoại thành công", Toast.LENGTH_SHORT).show()
                    // Tiếp tục xác thực OTP
                    showOtpVerificationDialog(phone)
                } else {
                    Toast.makeText(this@GroupOrderActivity, "Lỗi cập nhật số điện thoại: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<UserProfileDto>>, t: Throwable) {
                Toast.makeText(this@GroupOrderActivity, "Lỗi mạng", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    /**
     * Hiển thị dialog xác thực OTP trước khi thanh toán
     */
    private fun showOtpVerificationDialog(phoneNumber: String) {
        // Gửi OTP
        sendOtpToUser(phoneNumber)

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_otp_verify, null)
        val edtOtp = dialogView.findViewById<EditText>(R.id.edt_otp_code)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Xác thực đặt hàng")
            .setMessage("Mã OTP đã gửi đến $phoneNumber")
            .setView(dialogView)
            .setPositiveButton("Xác nhận", null) // Set null để tự xử lý
            .setNeutralButton("Gửi lại", null) // Set null để không dismiss dialog
            .setNegativeButton("Hủy", null)
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
                verifyOtpAndProceed(phoneNumber, code, dialog)
            }
            
            // Xử lý nút Gửi lại - không dismiss dialog
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                sendOtpToUser(phoneNumber)
            }
        }

        dialog.show()
    }
    
    /**
     * Gửi OTP đến số điện thoại
     */
    private fun sendOtpToUser(phone: String) {
        // ✅ CHECK RATE LIMITING
        val now = System.currentTimeMillis()
        if (now - lastOtpSentTime < OTP_COOLDOWN) {
            val remaining = (OTP_COOLDOWN - (now - lastOtpSentTime)) / 1000
            Toast.makeText(this, "Vui lòng đợi ${remaining}s trước khi gửi lại OTP", Toast.LENGTH_LONG).show()
            return
        }
        
        lastOtpSentTime = now
        
        RetrofitClient.getInstance(this).apiService.sendOtp(phone).enqueue(object : Callback<ApiResponse<String>> {
            override fun onResponse(call: Call<ApiResponse<String>>, response: Response<ApiResponse<String>>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@GroupOrderActivity, "Đã gửi OTP", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@GroupOrderActivity, "Lỗi gửi OTP: ${response.message()}", Toast.LENGTH_SHORT).show()
                    // Reset timer nếu gửi thất bại
                    lastOtpSentTime = 0L
                }
            }

            override fun onFailure(call: Call<ApiResponse<String>>, t: Throwable) {
                Toast.makeText(this@GroupOrderActivity, "Lỗi mạng", Toast.LENGTH_SHORT).show()
                // Reset timer nếu gửi thất bại
                lastOtpSentTime = 0L
            }
        })
    }
    
    /**
     * Xác thực OTP và tiếp tục thanh toán
     */
    private fun verifyOtpAndProceed(phone: String, code: String, dialog: AlertDialog) {
        val verifyLoadingDialog = LoadingDialog(this)
        verifyLoadingDialog.show("Đang xác thực...")
        
        RetrofitClient.getInstance(this).apiService.verifyOtp(phone, code)
            .enqueue(object : Callback<ApiResponse<Boolean>> {
                override fun onResponse(call: Call<ApiResponse<Boolean>>, response: Response<ApiResponse<Boolean>>) {
                    verifyLoadingDialog.dismiss()
                    
                    if (response.isSuccessful && response.body()?.data == true) {
                        // OTP hợp lệ, đóng dialog và hiển thị dialog chọn phương thức thanh toán
                        dialog.dismiss()
                        showPaymentMethodDialog()
                    } else {
                        Toast.makeText(this@GroupOrderActivity, "Mã OTP không đúng, vui lòng thử lại", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Boolean>>, t: Throwable) {
                    verifyLoadingDialog.dismiss()
                    Toast.makeText(this@GroupOrderActivity, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
    
    private fun showPaymentMethodDialog() {
        val paymentMethods = arrayOf(
            "COD - Tiền mặt", 
            "VNPAY - Thanh toán online",
            "MOMO - Ví MoMo",
            "VIETQR - Quét mã QR",
            "PAYPAL - Thanh toán quốc tế"
        )
        val paymentCodes = arrayOf("COD", "VNPAY", "MOMO", "VIETQR", "PAYPAL")
        var selectedIndex = 0

        AlertDialog.Builder(this)
            .setTitle("Chọn phương thức thanh toán")
            .setSingleChoiceItems(paymentMethods, 0) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton("Tiếp tục") { _, _ ->
                // Chuyển sang màn hình Bill Preview
                navigateToBillPreview(paymentCodes[selectedIndex])
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
    
    /**
     * Chuyển sang màn hình xem bill trước khi thanh toán
     */
    private fun navigateToBillPreview(paymentMethod: String) {
        val intent = Intent(this, GroupOrderBillPreviewActivity::class.java).apply {
            putExtra(GroupOrderBillPreviewActivity.EXTRA_GROUP_ORDER_ID, groupOrderId)
            putExtra(GroupOrderBillPreviewActivity.EXTRA_PAYMENT_METHOD, paymentMethod)
            // Truyền shipping fee đã tính
            putExtra(GroupOrderBillPreviewActivity.EXTRA_SHIPPING_FEE, shippingFee)
            // Có thể thêm voucher code nếu có
            // putExtra(GroupOrderBillPreviewActivity.EXTRA_PROMOTION_CODE, promotionCode)
            // putExtra(GroupOrderBillPreviewActivity.EXTRA_SPIN_VOUCHER_CODE, spinVoucherCode)
        }
        startActivity(intent)
    }
    
    // Giữ lại method cũ để backward compatible (có thể xóa sau)
    @Suppress("unused")
    private fun showBillPreviewOld(paymentMethod: String) {
        loadingDialog.show("Đang tải bill...")
        
        val request = PreviewGroupOrderBillRequest(paymentMethod = paymentMethod)
        RetrofitClient.getInstance(this).apiService.previewGroupOrderBill(groupOrderId, request)
            .enqueue(object : Callback<ApiResponse<BillPreview>> {
                override fun onResponse(
                    call: Call<ApiResponse<BillPreview>>,
                    response: Response<ApiResponse<BillPreview>>
                ) {
                    loadingDialog.dismiss()
                    if (response.isSuccessful && response.body()?.success == true) {
                        val bill = response.body()?.data
                        showBillPreviewDialog(bill, paymentMethod)
                    } else {
                        Toast.makeText(this@GroupOrderActivity,
                            response.body()?.message ?: "Lỗi tải bill", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<BillPreview>>, t: Throwable) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@GroupOrderActivity, 
                        "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
    
    private fun showBillPreviewDialog(bill: BillPreview?, paymentMethod: String) {
        if (bill == null) {
            Toast.makeText(this, "Không thể tải thông tin bill", Toast.LENGTH_SHORT).show()
            return
        }
        
        val message = StringBuilder()
        message.append("📍 Chi nhánh: ${bill.storeName ?: "N/A"}\n")
        message.append("📦 Loại đơn: ${if (bill.orderType == "DELIVERY") "Giao hàng" else "Tự đến lấy"}\n")
        if (bill.orderType == "DELIVERY") {
            message.append("🏠 Địa chỉ: ${bill.deliveryAddress ?: "N/A"}\n")
        }
        message.append("\n")
        message.append("💰 Tổng tiền hàng: ${formatPrice(bill.subtotal ?: 0.0)}\n")
        
        if ((bill.shippingFee ?: 0.0) > 0) {
            message.append("🚚 Phí giao hàng: ${formatPrice(bill.shippingFee ?: 0.0)}\n")
        }
        if (bill.freeShipping == true) {
            message.append("🎉 ${bill.freeShippingReason ?: "Miễn phí ship"}\n")
        }
        
        if ((bill.voucherDiscount ?: 0.0) > 0) {
            message.append("🎫 Giảm voucher: -${formatPrice(bill.voucherDiscount ?: 0.0)}\n")
        }
        if ((bill.tierDiscountAmount ?: 0.0) > 0) {
            message.append("⭐ Giảm hạng ${bill.tierName}: -${formatPrice(bill.tierDiscountAmount ?: 0.0)}\n")
        }
        
        message.append("\n")
        message.append("💵 THÀNH TIỀN: ${formatPrice(bill.finalPrice ?: 0.0)}\n")
        message.append("💳 Thanh toán: ${if (paymentMethod == "COD") "Tiền mặt" else "VNPay"}")
        
        AlertDialog.Builder(this)
            .setTitle("🧾 Xác nhận đơn hàng")
            .setMessage(message.toString())
            .setPositiveButton("Thanh toán") { _, _ ->
                checkout(paymentMethod)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
    
    private fun formatPrice(price: Double): String {
        return String.format(Locale.getDefault(), "%,.0f VNĐ", price)
    }

    private fun checkout(paymentMethod: String) {
        loadingDialog.show("Đang xử lý thanh toán...")

        val request = CheckoutGroupOrderRequest(paymentMethod = paymentMethod)
        RetrofitClient.getInstance(this).apiService.checkoutGroupOrder(groupOrderId, request)
            .enqueue(object : Callback<ApiResponse<Order>> {
                override fun onResponse(
                    call: Call<ApiResponse<Order>>,
                    response: Response<ApiResponse<Order>>
                ) {
                    loadingDialog.dismiss()
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@GroupOrderActivity, 
                            "🎉 Đặt hàng thành công!", Toast.LENGTH_SHORT).show()
                        
                        // Navigate to Home
                        navigateToHome()
                    } else {
                        Toast.makeText(this@GroupOrderActivity,
                            response.body()?.message ?: "Lỗi thanh toán", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Order>>, t: Throwable) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@GroupOrderActivity, 
                        "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
    
    /**
     * Navigate về trang Home
     */
    private fun navigateToHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun confirmDeleteItem(item: GroupOrderItemDto) {
        AlertDialog.Builder(this)
            .setTitle("Xóa món")
            .setMessage("Bạn có chắc muốn xóa ${item.drinkName}?")
            .setPositiveButton("Xóa") { _, _ ->
                deleteItem(item.id ?: return@setPositiveButton)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun deleteItem(itemId: Long) {
        loadingDialog.show("Đang xóa...")

        RetrofitClient.getInstance(this).apiService.removeGroupOrderItem(groupOrderId, itemId)
            .enqueue(object : Callback<ApiResponse<GroupOrderDto>> {
                override fun onResponse(
                    call: Call<ApiResponse<GroupOrderDto>>,
                    response: Response<ApiResponse<GroupOrderDto>>
                ) {
                    loadingDialog.dismiss()
                    if (response.isSuccessful && response.body()?.success == true) {
                        groupOrder = response.body()?.data
                        updateUI()
                        Toast.makeText(this@GroupOrderActivity, "Đã xóa món", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@GroupOrderActivity,
                            response.body()?.message ?: "Lỗi xóa món", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<GroupOrderDto>>, t: Throwable) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@GroupOrderActivity, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun confirmLeave() {
        AlertDialog.Builder(this)
            .setTitle("Rời khỏi nhóm")
            .setMessage("Bạn có chắc muốn rời khỏi phiên đặt hàng này? Các món bạn đã thêm sẽ bị xóa.")
            .setPositiveButton("Rời") { _, _ -> leaveGroup() }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun leaveGroup() {
        loadingDialog.show("Đang rời...")

        RetrofitClient.getInstance(this).apiService.leaveGroupOrder(groupOrderId)
            .enqueue(object : Callback<ApiResponse<GroupOrderDto>> {
                override fun onResponse(
                    call: Call<ApiResponse<GroupOrderDto>>,
                    response: Response<ApiResponse<GroupOrderDto>>
                ) {
                    loadingDialog.dismiss()
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@GroupOrderActivity, "Đã rời khỏi nhóm", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@GroupOrderActivity,
                            response.body()?.message ?: "Lỗi", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<GroupOrderDto>>, t: Throwable) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@GroupOrderActivity, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun confirmCancel() {
        AlertDialog.Builder(this)
            .setTitle("Hủy phiên")
            .setMessage("Bạn có chắc muốn hủy phiên đặt hàng này? Tất cả thành viên sẽ bị loại khỏi phiên.")
            .setPositiveButton("Hủy phiên") { _, _ -> cancelGroup() }
            .setNegativeButton("Không", null)
            .show()
    }

    private fun cancelGroup() {
        loadingDialog.show("Đang hủy...")

        RetrofitClient.getInstance(this).apiService.cancelGroupOrder(groupOrderId)
            .enqueue(object : Callback<ApiResponse<Void>> {
                override fun onResponse(
                    call: Call<ApiResponse<Void>>,
                    response: Response<ApiResponse<Void>>
                ) {
                    loadingDialog.dismiss()
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@GroupOrderActivity, "Đã hủy phiên", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@GroupOrderActivity,
                            response.body()?.message ?: "Lỗi", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Void>>, t: Throwable) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@GroupOrderActivity, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                }
            })
    }

    override fun onResume() {
        super.onResume()
        if (groupOrderId > 0) {
            loadGroupOrder()
            // Bắt đầu polling để cập nhật realtime
            startPolling()
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Dừng polling khi activity không visible
        stopPolling()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        stopPolling()
    }
    
    // ==================== REALTIME POLLING ====================
    
    /**
     * Bắt đầu polling để cập nhật thành viên và món mới
     */
    private fun startPolling() {
        if (isPollingActive) return
        isPollingActive = true
        pollingHandler.postDelayed(pollingRunnable, POLLING_INTERVAL)
    }
    
    /**
     * Dừng polling
     */
    private fun stopPolling() {
        isPollingActive = false
        pollingHandler.removeCallbacks(pollingRunnable)
    }
    
    /**
     * Runnable để polling dữ liệu mới
     */
    private val pollingRunnable = object : Runnable {
        override fun run() {
            if (!isPollingActive) return
            
            // Lấy dữ liệu mới (silent - không hiện loading)
            fetchGroupOrderSilent()
            
            // Schedule next poll
            pollingHandler.postDelayed(this, POLLING_INTERVAL)
        }
    }
    
    /**
     * Lấy dữ liệu group order mới (không hiện loading dialog)
     */
    private fun fetchGroupOrderSilent() {
        RetrofitClient.getInstance(this).apiService.getGroupOrder(groupOrderId)
            .enqueue(object : Callback<ApiResponse<GroupOrderDto>> {
                override fun onResponse(
                    call: Call<ApiResponse<GroupOrderDto>>,
                    response: Response<ApiResponse<GroupOrderDto>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val newOrder = response.body()?.data ?: return
                        
                        // Kiểm tra có thay đổi không
                        val newMemberCount = newOrder.currentMemberCount ?: 0
                        val newItemCount = newOrder.items?.size ?: 0
                        
                        val hasChanges = newMemberCount != lastMemberCount || 
                                         newItemCount != lastItemCount ||
                                         newOrder.status != groupOrder?.status
                        
                        if (hasChanges) {
                            // Cập nhật UI
                            groupOrder = newOrder
                            lastMemberCount = newMemberCount
                            lastItemCount = newItemCount
                            updateUI()
                            
                            // Hiển thị thông báo nếu có thành viên mới
                            if (newMemberCount > lastMemberCount) {
                                Toast.makeText(this@GroupOrderActivity, 
                                    "👋 Có thành viên mới tham gia!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<GroupOrderDto>>, t: Throwable) {
                    // Silent fail - không hiện lỗi
                }
            })
    }
}
