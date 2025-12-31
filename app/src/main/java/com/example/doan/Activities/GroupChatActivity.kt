package com.example.doan.Activities

import android.os.Bundle
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
import com.example.doan.Adapters.GroupChatAdapter
import com.example.doan.Models.*
import com.example.doan.Network.GroupChatWebSocketManager
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.SessionManager
import com.google.android.material.appbar.MaterialToolbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Activity cho Group Chat trong Group Order
 * Cho phép các thành viên trong nhóm chat với nhau realtime
 */
class GroupChatActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var tvGroupName: TextView
    private lateinit var tvMemberCount: TextView
    
    private lateinit var chatAdapter: GroupChatAdapter
    private lateinit var sessionManager: SessionManager
    private lateinit var webSocketManager: GroupChatWebSocketManager
    
    private var groupOrderId: Long = 0L
    private var groupName: String = ""
    private var memberCount: Int = 0
    private var currentUserId: Long = 0L

    companion object {
        private const val TAG = "GroupChatActivity"
        const val EXTRA_GROUP_ORDER_ID = "group_order_id"
        const val EXTRA_GROUP_NAME = "group_name"
        const val EXTRA_MEMBER_COUNT = "member_count"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_chat)

        sessionManager = SessionManager(this)
        currentUserId = sessionManager.getUserId().toLong()
        
        // Get extras
        groupOrderId = intent.getLongExtra(EXTRA_GROUP_ORDER_ID, 0)
        groupName = intent.getStringExtra(EXTRA_GROUP_NAME) ?: "Chat nhóm"
        memberCount = intent.getIntExtra(EXTRA_MEMBER_COUNT, 0)
        
        if (groupOrderId == 0L) {
            Toast.makeText(this, "Không tìm thấy phiên đặt hàng nhóm", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        initViews()
        setupToolbar()
        setupRecyclerView()
        setupListeners()
        setupWebSocket()
        loadChatHistory()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        progressBar = findViewById(R.id.progressBar)
        tvStatus = findViewById(R.id.tvStatus)
        tvEmpty = findViewById(R.id.tvEmpty)
        tvGroupName = findViewById(R.id.tvGroupName)
        tvMemberCount = findViewById(R.id.tvMemberCount)
        
        // Set group info
        tvGroupName.text = groupName
        tvMemberCount.text = "$memberCount thành viên"
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Chat nhóm"
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRecyclerView() {
        chatAdapter = GroupChatAdapter(currentUserId)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@GroupChatActivity).apply {
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
        webSocketManager = GroupChatWebSocketManager.getInstance()
        
        webSocketManager.setOnNewMessageListener { message ->
            runOnUiThread {
                // Chỉ thêm tin nhắn từ người khác hoặc tin nhắn hệ thống
                // Tin nhắn TEXT của mình đã được thêm local (optimistic update)
                val isMyTextMessage = message.senderId == currentUserId && 
                                      message.messageType == GroupChatMessageType.TEXT
                
                if (!isMyTextMessage) {
                    chatAdapter.addMessage(message)
                    scrollToBottom()
                    tvEmpty.visibility = View.GONE
                }
            }
        }
        
        webSocketManager.setOnConnectionListener { connected ->
            runOnUiThread {
                if (connected) {
                    tvStatus.visibility = View.GONE
                    // Subscribe to group chat
                    webSocketManager.subscribeToGroupChat(groupOrderId)
                } else {
                    tvStatus.visibility = View.VISIBLE
                    tvStatus.text = "Đang kết nối lại..."
                }
            }
        }
        
        // Connect WebSocket
        webSocketManager.connect(RetrofitClient.getBaseUrl())
    }

    private fun loadChatHistory() {
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
        
        RetrofitClient.getInstance(this).apiService.getGroupChatHistory(groupOrderId)
            .enqueue(object : Callback<ApiResponse<List<GroupChatMessageDto>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<GroupChatMessageDto>>>,
                    response: Response<ApiResponse<List<GroupChatMessageDto>>>
                ) {
                    progressBar.visibility = View.GONE
                    
                    if (response.isSuccessful && response.body()?.data != null) {
                        val messages = response.body()!!.data!!
                        if (messages.isNotEmpty()) {
                            chatAdapter.setMessages(messages)
                            scrollToBottom()
                        } else {
                            tvEmpty.visibility = View.VISIBLE
                        }
                    } else {
                        tvEmpty.visibility = View.VISIBLE
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<GroupChatMessageDto>>>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    tvEmpty.visibility = View.VISIBLE
                    Toast.makeText(this@GroupChatActivity, "Lỗi tải tin nhắn: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun sendMessage() {
        val content = etMessage.text.toString().trim()
        if (content.isEmpty()) return
        
        etMessage.text.clear()
        
        // Add message locally first (optimistic update)
        val localMessage = GroupChatMessageDto(
            id = System.currentTimeMillis(),
            groupOrderId = groupOrderId,
            senderId = currentUserId,
            senderName = sessionManager.getFullName(),
            senderAvatar = null,
            content = content,
            messageType = GroupChatMessageType.TEXT,
            createdAt = null
        )
        chatAdapter.addMessage(localMessage)
        scrollToBottom()
        tvEmpty.visibility = View.GONE
        
        // Send via REST API (WebSocket sẽ broadcast cho người khác)
        val request = SendGroupChatRequest(content)
        
        RetrofitClient.getInstance(this).apiService.sendGroupChatMessage(groupOrderId, request)
            .enqueue(object : Callback<ApiResponse<GroupChatMessageDto>> {
                override fun onResponse(
                    call: Call<ApiResponse<GroupChatMessageDto>>,
                    response: Response<ApiResponse<GroupChatMessageDto>>
                ) {
                    if (!response.isSuccessful) {
                        val errorMsg = response.body()?.message ?: "Không thể gửi tin nhắn"
                        Toast.makeText(this@GroupChatActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<GroupChatMessageDto>>, t: Throwable) {
                    Toast.makeText(this@GroupChatActivity, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun scrollToBottom() {
        recyclerView.post {
            val lastPosition = chatAdapter.getLastPosition()
            if (lastPosition >= 0) {
                recyclerView.smoothScrollToPosition(lastPosition)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reconnect WebSocket if needed
        webSocketManager.reconnectIfNeeded(RetrofitClient.getBaseUrl())
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unsubscribe from group chat
        webSocketManager.unsubscribeFromGroupChat()
    }
}
