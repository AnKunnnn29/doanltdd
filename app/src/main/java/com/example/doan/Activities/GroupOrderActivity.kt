package com.example.doan.Activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Button
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
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
        memberAdapter = GroupOrderMemberAdapter(
            order.members ?: emptyList(),
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
        
        if (expiresAt.isNullOrEmpty()) {
            tvExpiresAt.text = "Không giới hạn"
            return
        }

        try {
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            val expireTime = LocalDateTime.parse(expiresAt, formatter)
            val now = LocalDateTime.now()
            val remainingSeconds = ChronoUnit.SECONDS.between(now, expireTime)

            if (remainingSeconds <= 0) {
                tvExpiresAt.text = "Đã hết hạn"
                return
            }

            countDownTimer = object : CountDownTimer(remainingSeconds * 1000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val minutes = millisUntilFinished / 60000
                    val seconds = (millisUntilFinished % 60000) / 1000
                    tvExpiresAt.text = "Hết hạn sau: ${minutes}p ${seconds}s"
                }

                override fun onFinish() {
                    tvExpiresAt.text = "Đã hết hạn"
                    loadGroupOrder() // Reload to update status
                }
            }.start()
        } catch (e: Exception) {
            Log.e("GroupOrderActivity", "Error parsing expiration time", e)
            tvExpiresAt.text = "Không xác định"
        }
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
        
        if (groupOrder?.orderType == "DELIVERY" && groupOrder?.deliveryAddress.isNullOrEmpty()) {
            Toast.makeText(this, "Vui lòng nhập địa chỉ giao hàng", Toast.LENGTH_SHORT).show()
            return
        }

        // Hiển thị dialog chọn phương thức thanh toán
        showPaymentMethodDialog()
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
                        val order = response.body()?.data
                        Toast.makeText(this@GroupOrderActivity, 
                            "Đặt hàng thành công!", Toast.LENGTH_SHORT).show()
                        
                        // Navigate to order detail
                        val intent = Intent(this@GroupOrderActivity, OrderDetailActivity::class.java)
                        intent.putExtra("ORDER_ID", order?.id)
                        startActivity(intent)
                        finish()
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
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
