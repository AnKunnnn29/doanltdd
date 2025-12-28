package com.example.doan.Fragments.Manager

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.doan.Activities.LiveChatActivity
import com.example.doan.Adapters.ConversationListAdapter
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.ConversationListItem
import com.example.doan.Models.LiveConversation
import com.example.doan.Network.LiveChatWebSocketManager
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ManageChatsFragment : Fragment() {

    private lateinit var rvConversations: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvWaitingCount: TextView
    
    private lateinit var adapter: ConversationListAdapter
    private val conversations = mutableListOf<ConversationListItem>()
    private lateinit var webSocketManager: LiveChatWebSocketManager
    private var notificationSound: MediaPlayer? = null

    companion object {
        private const val TAG = "ManageChatsFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_manage_chats, container, false)
        
        initViews(view)
        setupRecyclerView()
        setupListeners()
        setupWebSocket()
        loadConversations()
        
        return view
    }

    private fun initViews(view: View) {
        rvConversations = view.findViewById(R.id.rvConversations)
        progressBar = view.findViewById(R.id.progressBar)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        tvWaitingCount = view.findViewById(R.id.tvWaitingCount)
    }

    private fun setupRecyclerView() {
        adapter = ConversationListAdapter(conversations) { conversation ->
            openConversation(conversation)
        }
        rvConversations.layoutManager = LinearLayoutManager(context)
        rvConversations.adapter = adapter
    }

    private fun setupListeners() {
        swipeRefresh.setColorSchemeResources(R.color.wine_primary)
        swipeRefresh.setOnRefreshListener {
            loadConversations()
        }
    }

    private fun setupWebSocket() {
        webSocketManager = LiveChatWebSocketManager.getInstance()
        
        webSocketManager.setOnNewConversationListener { newConversation ->
            activity?.runOnUiThread {
                handleNewConversation(newConversation)
            }
        }
        
        webSocketManager.connect(RetrofitClient.getBaseUrl())
    }

    private fun handleNewConversation(conversation: LiveConversation) {
        Log.d(TAG, "New conversation received: #${conversation.id}")
        
        // Play notification sound
        playNotificationSound()
        
        // Show toast
        Toast.makeText(
            context,
            "🔔 Khách hàng mới cần hỗ trợ: ${conversation.userName}",
            Toast.LENGTH_SHORT
        ).show()
        
        // Reload list
        loadConversations()
    }

    private fun playNotificationSound() {
        try {
            notificationSound?.release()
            notificationSound = MediaPlayer.create(context, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
            notificationSound?.start()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing notification: ${e.message}")
        }
    }


    private fun loadConversations() {
        if (!swipeRefresh.isRefreshing) {
            progressBar.visibility = View.VISIBLE
        }
        
        RetrofitClient.getInstance(requireContext()).apiService.getManagerConversations()
            .enqueue(object : Callback<ApiResponse<List<ConversationListItem>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<ConversationListItem>>>,
                    response: Response<ApiResponse<List<ConversationListItem>>>
                ) {
                    progressBar.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                    
                    if (response.isSuccessful && response.body()?.data != null) {
                        conversations.clear()
                        conversations.addAll(response.body()!!.data!!)
                        adapter.notifyDataSetChanged()
                        
                        // Update waiting count
                        val waitingCount = conversations.count { it.status == "WAITING" }
                        tvWaitingCount.text = "Đang chờ: $waitingCount"
                        
                        if (conversations.isEmpty()) {
                            tvEmpty.visibility = View.VISIBLE
                        } else {
                            tvEmpty.visibility = View.GONE
                        }
                    } else {
                        Toast.makeText(context, "Không thể tải danh sách chat", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<ConversationListItem>>>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                    Toast.makeText(context, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun openConversation(conversation: ConversationListItem) {
        val intent = Intent(requireContext(), LiveChatActivity::class.java)
        intent.putExtra(LiveChatActivity.EXTRA_CONVERSATION_ID, conversation.id)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        loadConversations()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        notificationSound?.release()
        notificationSound = null
    }
}
