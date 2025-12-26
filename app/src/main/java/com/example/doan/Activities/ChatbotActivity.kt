package com.example.doan.Activities

import android.content.Context
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Adapters.ChatAdapter
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.ChatMessage
import com.example.doan.Models.ChatRequest
import com.example.doan.Models.ChatResponse
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChatbotActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var toolbar: MaterialToolbar
    private lateinit var chatAdapter: ChatAdapter
    
    // Suggestion chips
    private lateinit var chipMenu: Chip
    private lateinit var chipHot: Chip
    private lateinit var chipPromo: Chip
    private lateinit var chipStore: Chip
    private lateinit var chipOrder: Chip
    private lateinit var chipHelp: Chip

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatbot)

        initViews()
        setupToolbar()
        setupRecyclerView()
        setupListeners()
        setupSuggestionChips()
        sendWelcomeMessage()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.rvChat)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        toolbar = findViewById(R.id.toolbar)
        
        // Suggestion chips
        chipMenu = findViewById(R.id.chipMenu)
        chipHot = findViewById(R.id.chipHot)
        chipPromo = findViewById(R.id.chipPromo)
        chipStore = findViewById(R.id.chipStore)
        chipOrder = findViewById(R.id.chipOrder)
        chipHelp = findViewById(R.id.chipHelp)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Trợ lý UTE Tea"
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatbotActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun setupListeners() {
        btnSend.setOnClickListener {
            sendMessage()
        }

        etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }
    }
    
    private fun setupSuggestionChips() {
        chipMenu.setOnClickListener { sendQuickMessage("Xem menu") }
        chipHot.setOnClickListener { sendQuickMessage("Món nào bán chạy?") }
        chipPromo.setOnClickListener { sendQuickMessage("Có khuyến mãi gì không?") }
        chipStore.setOnClickListener { sendQuickMessage("Cửa hàng ở đâu?") }
        chipOrder.setOnClickListener { sendQuickMessage("Xem đơn hàng của tôi") }
        chipHelp.setOnClickListener { sendQuickMessage("Hướng dẫn sử dụng") }
    }
    
    private fun sendQuickMessage(message: String) {
        etMessage.setText(message)
        sendMessage()
    }

    private fun sendWelcomeMessage() {
        val welcomeMessage = ChatMessage(
            message = "Xin chào! 👋 Tôi là trợ lý ảo của UTE Tea.\n\n" +
                    "Tôi có thể giúp bạn:\n" +
                    "🍵 Tìm kiếm đồ uống\n" +
                    "💰 Xem giá sản phẩm\n" +
                    "🎁 Xem khuyến mãi/voucher\n" +
                    "📍 Tìm cửa hàng\n" +
                    "📦 Kiểm tra đơn hàng\n\n" +
                    "Hãy chọn gợi ý bên dưới hoặc nhập câu hỏi! 😊",
            isUser = false
        )
        chatAdapter.addMessage(welcomeMessage)
    }

    private fun sendMessage() {
        val messageText = etMessage.text.toString().trim()
        if (messageText.isEmpty()) return

        // Add user message to chat
        val userMessage = ChatMessage(
            message = messageText,
            isUser = true
        )
        chatAdapter.addMessage(userMessage)
        scrollToBottom()

        // Clear input
        etMessage.text.clear()

        // Get user ID from SharedPreferences
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getLong("user_id", -1L)

        // Send to API
        val request = ChatRequest(
            message = messageText,
            userId = if (userId != -1L) userId else null
        )

        // Show typing indicator
        val typingMessage = ChatMessage(
            message = "Đang nhập...",
            isUser = false
        )
        chatAdapter.addMessage(typingMessage)
        scrollToBottom()

        RetrofitClient.getInstance(this).apiService.sendChatMessage(request)
            .enqueue(object : Callback<ApiResponse<ChatResponse>> {
                override fun onResponse(
                    call: Call<ApiResponse<ChatResponse>>,
                    response: Response<ApiResponse<ChatResponse>>
                ) {
                    removeLastMessage()

                    if (response.isSuccessful && response.body()?.data != null) {
                        val chatResponse = response.body()!!.data!!
                        val botMessage = ChatMessage(
                            message = chatResponse.message,
                            isUser = false,
                            type = chatResponse.type,
                            data = chatResponse.data
                        )
                        chatAdapter.addMessage(botMessage)
                    } else {
                        addErrorMessage("Xin lỗi, có lỗi xảy ra. Vui lòng thử lại.")
                    }
                    scrollToBottom()
                }

                override fun onFailure(call: Call<ApiResponse<ChatResponse>>, t: Throwable) {
                    removeLastMessage()
                    addErrorMessage("Không thể kết nối đến server. Vui lòng kiểm tra kết nối mạng.")
                    scrollToBottom()
                }
            })
    }

    private fun removeLastMessage() {
        val messages = chatAdapter.getMessages().toMutableList()
        if (messages.isNotEmpty()) {
            messages.removeAt(messages.size - 1)
            recyclerView.adapter = ChatAdapter(messages)
            chatAdapter = recyclerView.adapter as ChatAdapter
        }
    }

    private fun addErrorMessage(message: String) {
        val errorMessage = ChatMessage(
            message = message,
            isUser = false
        )
        chatAdapter.addMessage(errorMessage)
    }

    private fun scrollToBottom() {
        recyclerView.post {
            if (chatAdapter.itemCount > 0) {
                recyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)
            }
        }
    }
}
