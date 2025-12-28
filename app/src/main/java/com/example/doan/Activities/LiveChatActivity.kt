package com.example.doan.Activities

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Adapters.LiveChatAdapter
import com.example.doan.Models.*
import com.example.doan.Network.LiveChatWebSocketManager
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.SessionManager
import com.google.android.material.appbar.MaterialToolbar
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
                            // No active conversation, show empty state
                            tvEmpty.visibility = View.VISIBLE
                            tvEmpty.text = "Nhập tin nhắn để bắt đầu cuộc hội thoại với nhân viên hỗ trợ"
                            tvStatus.text = "Sẵn sàng hỗ trợ"
                        }
                    } else {
                        tvEmpty.visibility = View.VISIBLE
                        tvEmpty.text = "Nhập tin nhắn để bắt đầu cuộc hội thoại với nhân viên hỗ trợ"
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<ConversationListItem>>>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = "Nhập tin nhắn để bắt đầu cuộc hội thoại với nhân viên hỗ trợ"
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
        val isManager = sessionManager.isManager()
        chatAdapter = LiveChatAdapter(currentUserId, isManager)
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
        
        val isManager = sessionManager.isManager()
        
        webSocketManager.setOnNewMessageListener { message ->
            runOnUiThread {
                // Chỉ thêm tin nhắn từ phía đối phương
                // Nếu là Manager: chỉ thêm tin từ USER
                // Nếu là User: chỉ thêm tin từ MANAGER
                val shouldAdd = if (isManager) {
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
            startNewConversation(content)
        } else {
            // Send message to existing conversation
            sendMessageToConversation(content)
        }
    }

    private fun startNewConversation(initialMessage: String) {
        progressBar.visibility = View.VISIBLE
        
        val request = StartConversationRequest(
            subject = "Hỗ trợ khách hàng",
            initialMessage = initialMessage
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
                        displayConversation(conversation)
                        
                        // Subscribe to WebSocket
                        webSocketManager.subscribeToConversation(conversation.id)
                        
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
        
        val isManager = sessionManager.isManager()
        
        // Add message locally first (optimistic update)
        val localMessage = LiveMessage(
            id = System.currentTimeMillis(),
            conversationId = conversationId!!,
            senderId = currentUserId,
            senderName = sessionManager.getFullName(),
            senderAvatar = null,
            content = content,
            senderType = if (isManager) "MANAGER" else "USER",
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
