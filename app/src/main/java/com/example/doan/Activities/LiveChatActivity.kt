package com.example.doan.Activities

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Adapters.LiveChatAdapter
import com.example.doan.Adapters.StoreSelectAdapter
import com.example.doan.Models.*
import com.example.doan.Network.LiveChatWebSocketManager
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.SessionManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LiveChatActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var tvEmpty: TextView
    
    private lateinit var chatAdapter: LiveChatAdapter
    private lateinit var sessionManager: SessionManager
    private lateinit var webSocketManager: LiveChatWebSocketManager
    
    private var conversationId: Long? = null
    private var currentUserId: Long = 0L

    companion object {
        private const val TAG = "LiveChatActivity"
        const val EXTRA_CONVERSATION_ID = "conversation_id"
        const val EXTRA_STORE_ID = "store_id"
        const val EXTRA_STORE_NAME = "store_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_chat)

        sessionManager = SessionManager(this)
        currentUserId = sessionManager.getUserId().toLong()
        
        initViews()
        setupToolbar()
        setupRecyclerView()
        setupListeners()
        setupWebSocket()
        
        // Check if opening existing conversation or starting new
        conversationId = intent.getLongExtra(EXTRA_CONVERSATION_ID, -1).takeIf { it > 0 }
        
        // Check if store is pre-selected (from ManageStoresFragment)
        val preSelectedStoreId = intent.getLongExtra(EXTRA_STORE_ID, -1).takeIf { it > 0 }
        val preSelectedStoreName = intent.getStringExtra(EXTRA_STORE_NAME)
        
        if (preSelectedStoreId != null) {
            selectedStoreId = preSelectedStoreId
            toolbar.subtitle = "Chi nhánh: $preSelectedStoreName"
        }
        
        if (conversationId != null) {
            loadConversation(conversationId!!)
        } else {
            // Check if user has active conversation
            checkAndLoadActiveConversation()
        }
    }

    private fun checkAndLoadActiveConversation() {
        progressBar.visibility = View.VISIBLE
        
        RetrofitClient.getInstance(this).apiService.getMyConversations()
            .enqueue(object : Callback<ApiResponse<List<ConversationListItem>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<ConversationListItem>>>,
                    response: Response<ApiResponse<List<ConversationListItem>>>
                ) {
                    progressBar.visibility = View.GONE
                    
                    if (response.isSuccessful && response.body()?.data != null) {
                        val conversations = response.body()!!.data!!
                        // Find active conversation (WAITING or ACTIVE)
                        val activeConversation = conversations.find { 
                            it.status == "WAITING" || it.status == "ACTIVE" 
                        }
                        
                        if (activeConversation != null) {
                            conversationId = activeConversation.id
                            loadConversation(activeConversation.id)
                        } else {
                            // No active conversation
                            showEmptyStateAndSelectStore()
                        }
                    } else {
                        showEmptyStateAndSelectStore()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<ConversationListItem>>>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    showEmptyStateAndSelectStore()
                }
            })
    }
    
    private fun showEmptyStateAndSelectStore() {
        tvEmpty.visibility = View.VISIBLE
        
        if (selectedStoreId != null) {
            // Đã chọn store rồi
            tvEmpty.text = "Nhập tin nhắn để bắt đầu cuộc hội thoại với nhân viên hỗ trợ"
            tvStatus.text = "Sẵn sàng hỗ trợ"
        } else {
            // Chưa chọn store - hiển thị BottomSheet chọn store
            tvEmpty.text = "Vui lòng chọn chi nhánh để được tư vấn"
            tvStatus.text = "Chọn chi nhánh"
            
            // Load stores và hiển thị BottomSheet
            loadStoresAndShowSelection()
        }
    }
    
    private fun loadStoresAndShowSelection() {
        progressBar.visibility = View.VISIBLE
        RetrofitClient.getInstance(this).apiService.getStores()
            .enqueue(object : Callback<ApiResponse<List<Store>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<Store>>>,
                    response: Response<ApiResponse<List<Store>>>
                ) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful && response.body()?.data != null) {
                        allStores = response.body()!!.data!!.toMutableList()
                        displayStoreSelectionBottomSheet()
                    } else {
                        Toast.makeText(this@LiveChatActivity, "Không thể tải danh sách chi nhánh", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<Store>>>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@LiveChatActivity, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        progressBar = findViewById(R.id.progressBar)
        tvStatus = findViewById(R.id.tvStatus)
        tvEmpty = findViewById(R.id.tvEmpty)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Chat với nhân viên"
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_live_chat, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_close_chat -> {
                showCloseConversationDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showCloseConversationDialog() {
        if (conversationId == null) {
            Toast.makeText(this, "Chưa có cuộc hội thoại để đóng", Toast.LENGTH_SHORT).show()
            return
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Đóng cuộc hội thoại")
            .setMessage("Bạn có chắc muốn đóng cuộc hội thoại này?")
            .setPositiveButton("Đóng") { _, _ ->
                closeConversation()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun closeConversation() {
        progressBar.visibility = View.VISIBLE
        
        RetrofitClient.getInstance(this).apiService.closeConversation(conversationId!!)
            .enqueue(object : Callback<ApiResponse<LiveConversation>> {
                override fun onResponse(
                    call: Call<ApiResponse<LiveConversation>>,
                    response: Response<ApiResponse<LiveConversation>>
                ) {
                    progressBar.visibility = View.GONE
                    
                    if (response.isSuccessful) {
                        tvStatus.text = "Cuộc hội thoại đã đóng"
                        tvStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                        etMessage.isEnabled = false
                        btnSend.isEnabled = false
                        Toast.makeText(this@LiveChatActivity, "Đã đóng cuộc hội thoại", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@LiveChatActivity, "Không thể đóng cuộc hội thoại", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<LiveConversation>>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@LiveChatActivity, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun setupRecyclerView() {
        // isManager hoặc isAdmin đều được coi là "staff"
        val isStaff = sessionManager.isManager() || sessionManager.isAdmin()
        chatAdapter = LiveChatAdapter(currentUserId, isStaff)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@LiveChatActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun setupListeners() {
        btnSend.setOnClickListener { sendMessage() }
        
        etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else false
        }
    }

    private fun setupWebSocket() {
        webSocketManager = LiveChatWebSocketManager.getInstance()
        
        val isStaff = sessionManager.isManager() || sessionManager.isAdmin()
        
        webSocketManager.setOnNewMessageListener { message ->
            runOnUiThread {
                // Chỉ thêm tin nhắn từ phía đối phương (tránh duplicate với optimistic update)
                // Nếu là Staff (Manager/Admin): chỉ thêm tin từ USER hoặc SYSTEM
                // Nếu là User: chỉ thêm tin từ MANAGER hoặc SYSTEM
                val shouldAdd = if (isStaff) {
                    message.isFromUser() || message.isSystem()
                } else {
                    message.isFromManager() || message.isSystem()
                }
                
                if (shouldAdd) {
                    chatAdapter.addMessage(message)
                    scrollToBottom()
                }
            }
        }
        
        webSocketManager.setOnConversationClosedListener { closedId ->
            runOnUiThread {
                if (closedId == conversationId) {
                    tvStatus.text = "Cuộc hội thoại đã đóng"
                    tvStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                    etMessage.isEnabled = false
                    btnSend.isEnabled = false
                }
            }
        }
        
        webSocketManager.connect(RetrofitClient.getBaseUrl())
    }


    private fun loadConversation(id: Long) {
        progressBar.visibility = View.VISIBLE
        
        RetrofitClient.getInstance(this).apiService.getConversation(id)
            .enqueue(object : Callback<ApiResponse<LiveConversation>> {
                override fun onResponse(
                    call: Call<ApiResponse<LiveConversation>>,
                    response: Response<ApiResponse<LiveConversation>>
                ) {
                    progressBar.visibility = View.GONE
                    
                    if (response.isSuccessful && response.body()?.data != null) {
                        val conversation = response.body()!!.data!!
                        displayConversation(conversation)
                        
                        // Subscribe to WebSocket for this conversation
                        webSocketManager.subscribeToConversation(id)
                    } else {
                        Toast.makeText(this@LiveChatActivity, "Không thể tải cuộc hội thoại", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<LiveConversation>>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@LiveChatActivity, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun displayConversation(conversation: LiveConversation) {
        // Update status
        when (conversation.status) {
            "WAITING" -> {
                tvStatus.text = "Đang chờ nhân viên tiếp nhận..."
                tvStatus.setTextColor(getColor(android.R.color.holo_orange_dark))
            }
            "ACTIVE" -> {
                tvStatus.text = "Đang chat với ${conversation.managerName ?: "nhân viên"}"
                tvStatus.setTextColor(getColor(android.R.color.holo_green_dark))
            }
            "CLOSED" -> {
                tvStatus.text = "Cuộc hội thoại đã đóng"
                tvStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                etMessage.isEnabled = false
                btnSend.isEnabled = false
            }
        }
        
        // Display messages
        conversation.messages?.let { messages ->
            if (messages.isNotEmpty()) {
                tvEmpty.visibility = View.GONE
                chatAdapter.setMessages(messages)
                scrollToBottom()
            }
        }
    }

    private fun sendMessage() {
        val content = etMessage.text.toString().trim()
        if (content.isEmpty()) return
        
        etMessage.text.clear()
        
        if (conversationId == null) {
            // Start new conversation
            if (selectedStoreId != null) {
                // Store đã được chọn sẵn, bắt đầu conversation luôn
                startNewConversation(content, selectedStoreId!!)
            } else {
                // Cần chọn store trước
                showStoreSelectionDialog(content)
            }
        } else {
            // Send message to existing conversation
            sendMessageToConversation(content)
        }
    }
    
    private var allStores = mutableListOf<Store>()
    private var selectedStoreId: Long? = null
    private var pendingMessage: String? = null
    private var storeSelectionBottomSheet: BottomSheetDialog? = null
    
    private fun showStoreSelectionDialog(initialMessage: String) {
        pendingMessage = initialMessage
        
        if (allStores.isEmpty()) {
            // Load stores first
            progressBar.visibility = View.VISIBLE
            RetrofitClient.getInstance(this).apiService.getStores()
                .enqueue(object : Callback<ApiResponse<List<Store>>> {
                    override fun onResponse(
                        call: Call<ApiResponse<List<Store>>>,
                        response: Response<ApiResponse<List<Store>>>
                    ) {
                        progressBar.visibility = View.GONE
                        if (response.isSuccessful && response.body()?.data != null) {
                            allStores = response.body()!!.data!!.toMutableList()
                            displayStoreSelectionBottomSheet()
                        } else {
                            Toast.makeText(this@LiveChatActivity, "Không thể tải danh sách chi nhánh", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<List<Store>>>, t: Throwable) {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this@LiveChatActivity, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                    }
                })
        } else {
            displayStoreSelectionBottomSheet()
        }
    }
    
    private fun displayStoreSelectionBottomSheet() {
        if (allStores.isEmpty()) {
            Toast.makeText(this, "Không có chi nhánh nào", Toast.LENGTH_SHORT).show()
            return
        }
        
        storeSelectionBottomSheet = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_select_store, null)
        
        val rvStores = sheetView.findViewById<RecyclerView>(R.id.rv_stores)
        val btnClose = sheetView.findViewById<ImageView>(R.id.btn_close)
        
        val storeAdapter = StoreSelectAdapter(this, allStores) { selectedStore ->
            selectedStoreId = selectedStore.id.toLong()
            toolbar.subtitle = "🏪 ${selectedStore.storeName}"
            storeSelectionBottomSheet?.dismiss()
            
            // Cập nhật UI
            tvEmpty.text = "Nhập tin nhắn để bắt đầu cuộc hội thoại với nhân viên hỗ trợ"
            tvStatus.text = "Sẵn sàng hỗ trợ tại ${selectedStore.storeName}"
            
            // Nếu có tin nhắn đang chờ, gửi luôn
            pendingMessage?.let { message ->
                startNewConversation(message, selectedStoreId!!)
                pendingMessage = null
            }
        }
        
        rvStores.layoutManager = LinearLayoutManager(this)
        rvStores.adapter = storeAdapter
        
        btnClose.setOnClickListener {
            storeSelectionBottomSheet?.dismiss()
        }
        
        storeSelectionBottomSheet?.setContentView(sheetView)
        storeSelectionBottomSheet?.show()
    }
    
    private fun displayStoreSelectionDialog(initialMessage: String) {
        // Deprecated - use displayStoreSelectionBottomSheet instead
        showStoreSelectionDialog(initialMessage)
    }

    private fun startNewConversation(initialMessage: String, storeId: Long) {
        progressBar.visibility = View.VISIBLE
        
        val request = StartConversationRequest(
            subject = "Hỗ trợ khách hàng",
            initialMessage = initialMessage,
            storeId = storeId
        )
        
        RetrofitClient.getInstance(this).apiService.startLiveConversation(request)
            .enqueue(object : Callback<ApiResponse<LiveConversation>> {
                override fun onResponse(
                    call: Call<ApiResponse<LiveConversation>>,
                    response: Response<ApiResponse<LiveConversation>>
                ) {
                    progressBar.visibility = View.GONE
                    
                    if (response.isSuccessful && response.body()?.data != null) {
                        val conversation = response.body()!!.data!!
                        conversationId = conversation.id
                        
                        // Subscribe to WebSocket trước
                        webSocketManager.subscribeToConversation(conversation.id)
                        
                        // Hiển thị conversation
                        displayConversation(conversation)
                        
                        // Nếu conversation không có messages (API không trả về), thêm tin nhắn local
                        if (conversation.messages.isNullOrEmpty()) {
                            val isStaff = sessionManager.isManager() || sessionManager.isAdmin()
                            val localMessage = LiveMessage(
                                id = System.currentTimeMillis(),
                                conversationId = conversation.id,
                                senderId = currentUserId,
                                senderName = sessionManager.getFullName(),
                                senderAvatar = null,
                                content = initialMessage,
                                senderType = if (isStaff) "MANAGER" else "USER",
                                isRead = false,
                                createdAt = null
                            )
                            chatAdapter.addMessage(localMessage)
                            scrollToBottom()
                        }
                        
                        tvEmpty.visibility = View.GONE
                        Toast.makeText(this@LiveChatActivity, "Đã gửi yêu cầu hỗ trợ", Toast.LENGTH_SHORT).show()
                    } else {
                        val errorMsg = response.body()?.message ?: "Không thể tạo cuộc hội thoại"
                        Toast.makeText(this@LiveChatActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<LiveConversation>>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@LiveChatActivity, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun sendMessageToConversation(content: String) {
        val request = SendLiveMessageRequest(
            conversationId = conversationId!!,
            content = content
        )
        
        val isStaff = sessionManager.isManager() || sessionManager.isAdmin()
        
        // Add message locally first (optimistic update)
        val localMessage = LiveMessage(
            id = System.currentTimeMillis(),
            conversationId = conversationId!!,
            senderId = currentUserId,
            senderName = sessionManager.getFullName(),
            senderAvatar = null,
            content = content,
            senderType = if (isStaff) "MANAGER" else "USER",
            isRead = false,
            createdAt = null
        )
        chatAdapter.addMessage(localMessage)
        scrollToBottom()
        
        RetrofitClient.getInstance(this).apiService.sendLiveMessage(request)
            .enqueue(object : Callback<ApiResponse<LiveMessage>> {
                override fun onResponse(
                    call: Call<ApiResponse<LiveMessage>>,
                    response: Response<ApiResponse<LiveMessage>>
                ) {
                    if (!response.isSuccessful) {
                        Toast.makeText(this@LiveChatActivity, "Không thể gửi tin nhắn", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<LiveMessage>>, t: Throwable) {
                    Toast.makeText(this@LiveChatActivity, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun scrollToBottom() {
        recyclerView.post {
            if (chatAdapter.itemCount > 0) {
                recyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Don't disconnect WebSocket here, let Application manage it
    }
}
