package com.example.doan.Fragments.Manager

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.doan.Adapters.UserAdapter
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.PageResponse
import com.example.doan.Models.User
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ManageUsersFragment : Fragment() {

    private lateinit var rvUsers: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: LinearLayout
    private lateinit var etSearchUser: EditText
    private lateinit var btnFilterRole: MaterialButton
    private lateinit var tvTotalUsers: TextView
    private lateinit var tvManagers: TextView
    private lateinit var tvCustomers: TextView

    private var allUsers = mutableListOf<User>()
    private var currentRoleFilter: String? = null
    private var currentPage = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_manage_users, container, false)

        // Initialize views
        rvUsers = view.findViewById(R.id.rv_users)
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        progressBar = view.findViewById(R.id.progress_bar)
        emptyState = view.findViewById(R.id.empty_state)
        etSearchUser = view.findViewById(R.id.et_search_user)
        btnFilterRole = view.findViewById(R.id.btn_filter_role)
        tvTotalUsers = view.findViewById(R.id.tv_total_users)
        tvManagers = view.findViewById(R.id.tv_managers)
        tvCustomers = view.findViewById(R.id.tv_customers)

        // Setup RecyclerView
        rvUsers.layoutManager = LinearLayoutManager(context)
        userAdapter = UserAdapter(requireContext(), mutableListOf())
        userAdapter.setOnUserActionListener(object : UserAdapter.OnUserActionListener {
            override fun onViewUser(user: User) {
                showUserDetail(user)
            }

            override fun onToggleUserStatus(user: User) {
                toggleUserBlock(user)
            }
        })
        rvUsers.adapter = userAdapter

        // Setup listeners
        setupListeners()

        // Load data
        loadUsers()

        return view
    }

    private fun setupListeners() {
        // Swipe refresh
        swipeRefresh.setOnRefreshListener { loadUsers() }

        // Search
        etSearchUser.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterUsers(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Filter
        btnFilterRole.setOnClickListener {
            Toast.makeText(context, "Lọc theo vai trò - Coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUsers() {
        showLoading(true)
        
        Log.d(TAG, "Loading users - page: $currentPage, role: $currentRoleFilter")

        RetrofitClient.getInstance(requireContext()).apiService.getManagerUsers(currentRoleFilter, currentPage, PAGE_SIZE)
            .enqueue(object : Callback<ApiResponse<PageResponse<User>>> {
                override fun onResponse(
                    call: Call<ApiResponse<PageResponse<User>>>,
                    response: Response<ApiResponse<PageResponse<User>>>
                ) {
                    showLoading(false)
                    swipeRefresh.isRefreshing = false
                    
                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!
                        
                        if (apiResponse.success && apiResponse.data != null) {
                            val pageResponse = apiResponse.data!!
                            allUsers = pageResponse.content?.toMutableList() ?: mutableListOf()
                            
                            Log.d(TAG, "Loaded ${allUsers.size} users")
                            
                            userAdapter.updateUsers(allUsers)
                            updateStats()
                            updateEmptyState()
                        } else {
                            Log.e(TAG, "API error: ${apiResponse.message}")
                            Toast.makeText(context, "Lỗi: ${apiResponse.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e(TAG, "Response error: ${response.code()}")
                        Toast.makeText(context, "Không thể tải danh sách người dùng", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<PageResponse<User>>>, t: Throwable) {
                    showLoading(false)
                    swipeRefresh.isRefreshing = false
                    
                    Log.e(TAG, "Network error", t)
                    Toast.makeText(context, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun filterUsers(query: String) {
        val filtered = if (query.isEmpty()) {
            allUsers
        } else {
            allUsers.filter { user ->
                user.fullName?.lowercase()?.contains(query.lowercase()) == true ||
                user.phone?.contains(query) == true ||
                user.email?.lowercase()?.contains(query.lowercase()) == true
            }
        }
        userAdapter.updateUsers(filtered)
        updateEmptyState()
    }

    private fun updateStats() {
        val total = allUsers.size
        var managers = 0
        var customers = 0

        allUsers.forEach { user ->
            if (user.role == "MANAGER") managers++ else customers++
        }

        tvTotalUsers.text = total.toString()
        tvManagers.text = managers.toString()
        tvCustomers.text = customers.toString()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        rvUsers.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun updateEmptyState() {
        emptyState.visibility = if (userAdapter.itemCount == 0) View.VISIBLE else View.GONE
    }
    
    private fun showUserDetail(user: User) {
        Log.d(TAG, "Viewing user detail: ${user.id}")
        
        RetrofitClient.getInstance(requireContext()).apiService.getUserById(user.id)
            .enqueue(object : Callback<ApiResponse<User>> {
                override fun onResponse(
                    call: Call<ApiResponse<User>>,
                    response: Response<ApiResponse<User>>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!
                        
                        if (apiResponse.success && apiResponse.data != null) {
                            showUserDetailDialog(apiResponse.data!!)
                        } else {
                            Toast.makeText(context, "Lỗi: ${apiResponse.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Không thể tải thông tin người dùng", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<User>>, t: Throwable) {
                    Log.e(TAG, "Error loading user detail", t)
                    Toast.makeText(context, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
    
    private fun showUserDetailDialog(user: User) {
        val info = buildString {
            append("ID: ${user.id}\n")
            append("Tên: ${user.fullName ?: "N/A"}\n")
            append("Username: ${user.username ?: "N/A"}\n")
            append("Email: ${user.email ?: "N/A"}\n")
            append("SĐT: ${user.phone ?: "N/A"}\n")
            append("Địa chỉ: ${user.address ?: "N/A"}\n")
            append("Vai trò: ${user.role}\n")
            append("Hạng: ${user.memberTier ?: "N/A"}\n")
            append("Điểm: ${user.points}\n")
            append("Trạng thái: ${if (user.active) "Hoạt động" else "Không hoạt động"}\n")
            append("Khóa: ${if (user.isBlocked) "Đã khóa" else "Chưa khóa"}\n")
            append("Ngày tạo: ${user.createdAt ?: "N/A"}\n")
            append("Cập nhật: ${user.updatedAt ?: "N/A"}")
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Thông tin người dùng")
            .setMessage(info)
            .setPositiveButton("Đóng", null)
            .show()
    }
    
    private fun toggleUserBlock(user: User) {
        val newBlockStatus = !user.isBlocked
        val action = if (newBlockStatus) "khóa" else "mở khóa"
        
        AlertDialog.Builder(requireContext())
            .setTitle("Xác nhận")
            .setMessage("Bạn có chắc muốn $action tài khoản ${user.fullName}?")
            .setPositiveButton("Có") { _, _ ->
                performToggleUserBlock(user.id, newBlockStatus)
            }
            .setNegativeButton("Không", null)
            .show()
    }
    
    private fun performToggleUserBlock(userId: Int, blocked: Boolean) {
        Log.d(TAG, "Toggling user $userId block status to: $blocked")
        
        RetrofitClient.getInstance(requireContext()).apiService.toggleUserBlock(userId, blocked)
            .enqueue(object : Callback<ApiResponse<User>> {
                override fun onResponse(
                    call: Call<ApiResponse<User>>,
                    response: Response<ApiResponse<User>>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!
                        
                        if (apiResponse.success) {
                            val message = if (blocked) "Đã khóa tài khoản" else "Đã mở khóa tài khoản"
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            loadUsers() // Reload list
                        } else {
                            Toast.makeText(context, "Lỗi: ${apiResponse.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Không thể cập nhật trạng thái", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<User>>, t: Throwable) {
                    Log.e(TAG, "Error toggling user block", t)
                    Toast.makeText(context, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    companion object {
        private const val TAG = "ManageUsersFragment"
        private const val PAGE_SIZE = 20
    }
}
