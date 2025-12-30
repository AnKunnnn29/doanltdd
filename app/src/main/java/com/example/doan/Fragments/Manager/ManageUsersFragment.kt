package com.example.doan.Fragments.Manager

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
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
import com.example.doan.Models.Store
import com.example.doan.Models.User
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.SessionManager
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
    
    private lateinit var sessionManager: SessionManager
    private var isAdmin = false

    private var allUsers = mutableListOf<User>()
    private var allStores = mutableListOf<Store>()
    private var currentRoleFilter: String? = null
    private var currentPage = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_manage_users, container, false)
        
        sessionManager = SessionManager(requireContext())
        isAdmin = sessionManager.isAdmin()

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
            
            override fun onDeleteUser(user: User) {
                confirmDeleteUser(user)
            }
            
            override fun onPromoteUser(user: User) {
                showPromoteWithStoreSelectionDialog(user)
            }
            
            override fun onDemoteUser(user: User) {
                confirmDemoteUser(user)
            }
            
            override fun onManageStores(user: User) {
                showManageStoresDialog(user)
            }
        })
        rvUsers.adapter = userAdapter

        // Setup listeners
        setupListeners()

        // Load data
        loadUsers()
        loadStores()

        return view
    }

    private fun setupListeners() {
        swipeRefresh.setOnRefreshListener { loadUsers() }

        etSearchUser.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterUsers(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnFilterRole.setOnClickListener {
            showRoleFilterDialog()
        }
    }
    
    private fun showRoleFilterDialog() {
        val roles = arrayOf("Tất cả", "Khách hàng (USER)", "Quản lý (MANAGER)", "Admin (ADMIN)")
        val roleValues = arrayOf(null, "USER", "MANAGER", "ADMIN")
        
        AlertDialog.Builder(requireContext())
            .setTitle("Lọc theo vai trò")
            .setItems(roles) { _, which ->
                currentRoleFilter = roleValues[which]
                currentPage = 0
                loadUsers()
                btnFilterRole.text = if (which == 0) "Lọc vai trò" else roles[which]
            }
            .show()
    }
    
    private fun loadStores() {
        RetrofitClient.getInstance(requireContext()).apiService.getStores()
            .enqueue(object : Callback<ApiResponse<List<Store>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<Store>>>,
                    response: Response<ApiResponse<List<Store>>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        allStores = response.body()?.data?.toMutableList() ?: mutableListOf()
                        Log.d(TAG, "Loaded ${allStores.size} stores")
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<Store>>>, t: Throwable) {
                    Log.e(TAG, "Error loading stores", t)
                }
            })
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
            when (user.role) {
                "MANAGER" -> managers++
                "ADMIN" -> {}
                else -> customers++
            }
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
        val managedStoresText = if (user.role == "MANAGER") {
            val stores = user.managedStores
            if (stores.isNullOrEmpty()) {
                "Quản lý tất cả chi nhánh (Super Manager)"
            } else {
                "Chi nhánh: ${stores.joinToString(", ") { it.storeName ?: "N/A" }}"
            }
        } else ""
        
        val info = buildString {
            append("ID: ${user.id}\n")
            append("Tên: ${user.fullName ?: "N/A"}\n")
            append("Username: ${user.username ?: "N/A"}\n")
            append("Email: ${user.email ?: "N/A"}\n")
            append("SĐT: ${user.phone ?: "N/A"}\n")
            append("Địa chỉ: ${user.address ?: "N/A"}\n")
            append("Vai trò: ${user.role}\n")
            if (managedStoresText.isNotEmpty()) {
                append("$managedStoresText\n")
            }
            append("Hạng: ${user.memberTier ?: "N/A"}\n")
            append("Điểm: ${user.points}\n")
            append("Trạng thái: ${if (user.active) "Hoạt động" else "Không hoạt động"}\n")
            append("Khóa: ${if (user.isBlocked) "Đã khóa" else "Chưa khóa"}\n")
            append("Ngày tạo: ${user.createdAt ?: "N/A"}\n")
            append("Cập nhật: ${user.updatedAt ?: "N/A"}")
        }
        
        val builder = AlertDialog.Builder(requireContext())
            .setTitle("Thông tin người dùng")
            .setMessage(info)
            .setPositiveButton("Đóng", null)
        
        if (isAdmin && user.role == "MANAGER") {
            builder.setNeutralButton("Quản lý chi nhánh") { _, _ ->
                showManageStoresDialog(user)
            }
        }
        
        builder.show()
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
                            loadUsers()
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
    
    private fun confirmDeleteUser(user: User) {
        AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Xác nhận xóa")
            .setMessage("Bạn có chắc muốn xóa tài khoản \"${user.fullName}\"?\n\nDữ liệu doanh thu sẽ được backup trước khi xóa.")
            .setPositiveButton("Xóa") { _, _ ->
                performDeleteUser(user.id)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
    
    private fun performDeleteUser(userId: Int) {
        Log.d(TAG, "Deleting user $userId")
        showLoading(true)
        
        RetrofitClient.getInstance(requireContext()).apiService.deleteUser(userId)
            .enqueue(object : Callback<ApiResponse<String>> {
                override fun onResponse(
                    call: Call<ApiResponse<String>>,
                    response: Response<ApiResponse<String>>
                ) {
                    showLoading(false)
                    
                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!
                        
                        if (apiResponse.success) {
                            Toast.makeText(context, "Đã xóa tài khoản thành công", Toast.LENGTH_SHORT).show()
                            loadUsers()
                        } else {
                            Toast.makeText(context, "Lỗi: ${apiResponse.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "Delete error: $errorBody")
                        Toast.makeText(context, "Không thể xóa tài khoản", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<String>>, t: Throwable) {
                    showLoading(false)
                    Log.e(TAG, "Error deleting user", t)
                    Toast.makeText(context, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    
    // ==================== PROMOTE USER WITH STORE SELECTION ====================
    
    private fun showPromoteWithStoreSelectionDialog(user: User) {
        if (allStores.isEmpty()) {
            Toast.makeText(context, "Đang tải danh sách chi nhánh...", Toast.LENGTH_SHORT).show()
            loadStores()
            return
        }
        
        val selectedStoreIds = mutableSetOf<Int>()
        
        val scrollView = ScrollView(requireContext())
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }
        
        val descText = TextView(requireContext()).apply {
            text = "Chọn chi nhánh để \"${user.fullName}\" quản lý:\n(Không chọn = Quản lý tất cả)"
            setPadding(0, 0, 0, 24)
        }
        container.addView(descText)
        
        allStores.forEach { store ->
            val checkBox = CheckBox(requireContext()).apply {
                text = "${store.storeName}\n${store.address}"
                tag = store.id
                setPadding(0, 8, 0, 8)
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedStoreIds.add(store.id)
                    } else {
                        selectedStoreIds.remove(store.id)
                    }
                }
            }
            container.addView(checkBox)
        }
        
        scrollView.addView(container)
        
        AlertDialog.Builder(requireContext())
            .setTitle("🎉 Nâng cấp lên Manager")
            .setView(scrollView)
            .setPositiveButton("Nâng cấp") { _, _ ->
                performPromoteUserWithStores(user.id, selectedStoreIds.map { it.toLong() })
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
    
    private fun performPromoteUserWithStores(userId: Int, storeIds: List<Long>) {
        Log.d(TAG, "Promoting user $userId to Manager with stores: $storeIds")
        showLoading(true)
        
        RetrofitClient.getInstance(requireContext()).apiService.promoteToManager(userId)
            .enqueue(object : Callback<ApiResponse<User>> {
                override fun onResponse(
                    call: Call<ApiResponse<User>>,
                    response: Response<ApiResponse<User>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        if (storeIds.isNotEmpty()) {
                            assignStoresToManager(userId, storeIds)
                        } else {
                            showLoading(false)
                            Toast.makeText(context, "🎉 Đã nâng cấp thành Manager (quản lý tất cả)!", Toast.LENGTH_SHORT).show()
                            loadUsers()
                        }
                    } else {
                        showLoading(false)
                        val errorMsg = response.body()?.message ?: "Không thể nâng cấp tài khoản"
                        Toast.makeText(context, "Lỗi: $errorMsg", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<User>>, t: Throwable) {
                    showLoading(false)
                    Log.e(TAG, "Error promoting user", t)
                    Toast.makeText(context, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
    
    private fun assignStoresToManager(userId: Int, storeIds: List<Long>) {
        RetrofitClient.getInstance(requireContext()).apiService.assignStoresToManager(userId, storeIds)
            .enqueue(object : Callback<ApiResponse<User>> {
                override fun onResponse(
                    call: Call<ApiResponse<User>>,
                    response: Response<ApiResponse<User>>
                ) {
                    showLoading(false)
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(context, "🎉 Đã nâng cấp thành Manager và gán ${storeIds.size} chi nhánh!", Toast.LENGTH_SHORT).show()
                        loadUsers()
                    } else {
                        Toast.makeText(context, "Đã nâng cấp nhưng không thể gán chi nhánh", Toast.LENGTH_SHORT).show()
                        loadUsers()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<User>>, t: Throwable) {
                    showLoading(false)
                    Log.e(TAG, "Error assigning stores", t)
                    Toast.makeText(context, "Đã nâng cấp nhưng lỗi gán chi nhánh", Toast.LENGTH_SHORT).show()
                    loadUsers()
                }
            })
    }
    
    // ==================== MANAGE STORES FOR EXISTING MANAGER ====================
    
    private fun showManageStoresDialog(user: User) {
        if (allStores.isEmpty()) {
            Toast.makeText(context, "Đang tải danh sách chi nhánh...", Toast.LENGTH_SHORT).show()
            loadStores()
            return
        }
        
        val currentStoreIds = user.managedStores?.map { it.id.toInt() } ?: emptyList()
        val selectedStoreIds = currentStoreIds.toMutableSet()
        
        val scrollView = ScrollView(requireContext())
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }
        
        val currentStatus = if (currentStoreIds.isEmpty()) {
            "Hiện tại: Quản lý tất cả chi nhánh (Super Manager)"
        } else {
            "Hiện tại: Quản lý ${currentStoreIds.size} chi nhánh"
        }
        val descText = TextView(requireContext()).apply {
            text = "$currentStatus\n\nChọn chi nhánh để \"${user.fullName}\" quản lý:\n(Không chọn = Quản lý tất cả)"
            setPadding(0, 0, 0, 24)
        }
        container.addView(descText)
        
        allStores.forEach { store ->
            val checkBox = CheckBox(requireContext()).apply {
                text = "${store.storeName}\n${store.address}"
                tag = store.id
                isChecked = currentStoreIds.contains(store.id)
                setPadding(0, 8, 0, 8)
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedStoreIds.add(store.id)
                    } else {
                        selectedStoreIds.remove(store.id)
                    }
                }
            }
            container.addView(checkBox)
        }
        
        scrollView.addView(container)
        
        AlertDialog.Builder(requireContext())
            .setTitle("🏪 Quản lý chi nhánh")
            .setView(scrollView)
            .setPositiveButton("Lưu") { _, _ ->
                updateManagerStores(user.id, selectedStoreIds.map { it.toLong() })
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
    
    private fun updateManagerStores(userId: Int, storeIds: List<Long>) {
        Log.d(TAG, "Updating stores for manager $userId: $storeIds")
        showLoading(true)
        
        RetrofitClient.getInstance(requireContext()).apiService.assignStoresToManager(userId, storeIds)
            .enqueue(object : Callback<ApiResponse<User>> {
                override fun onResponse(
                    call: Call<ApiResponse<User>>,
                    response: Response<ApiResponse<User>>
                ) {
                    showLoading(false)
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        val msg = if (storeIds.isEmpty()) {
                            "Đã cập nhật: Quản lý tất cả chi nhánh"
                        } else {
                            "Đã cập nhật: Quản lý ${storeIds.size} chi nhánh"
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        loadUsers()
                    } else {
                        val errorMsg = response.body()?.message ?: "Không thể cập nhật chi nhánh"
                        Toast.makeText(context, "Lỗi: $errorMsg", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<User>>, t: Throwable) {
                    showLoading(false)
                    Log.e(TAG, "Error updating stores", t)
                    Toast.makeText(context, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
    
    // ==================== DEMOTE USER ====================
    
    private fun confirmDemoteUser(user: User) {
        AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Hạ cấp xuống User")
            .setMessage("Bạn có chắc muốn hạ cấp \"${user.fullName}\" xuống User thường?\n\nNgười này sẽ mất quyền quản lý tất cả chi nhánh.")
            .setPositiveButton("Hạ cấp") { _, _ ->
                performDemoteUser(user.id)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
    
    private fun performDemoteUser(userId: Int) {
        Log.d(TAG, "Demoting user $userId to User")
        showLoading(true)
        
        RetrofitClient.getInstance(requireContext()).apiService.demoteToUser(userId)
            .enqueue(object : Callback<ApiResponse<User>> {
                override fun onResponse(
                    call: Call<ApiResponse<User>>,
                    response: Response<ApiResponse<User>>
                ) {
                    showLoading(false)
                    
                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!
                        
                        if (apiResponse.success) {
                            Toast.makeText(context, "Đã hạ cấp xuống User thành công", Toast.LENGTH_SHORT).show()
                            loadUsers()
                        } else {
                            Toast.makeText(context, "Lỗi: ${apiResponse.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "Demote error: $errorBody")
                        Toast.makeText(context, "Không thể hạ cấp tài khoản", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<User>>, t: Throwable) {
                    showLoading(false)
                    Log.e(TAG, "Error demoting user", t)
                    Toast.makeText(context, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    companion object {
        private const val TAG = "ManageUsersFragment"
        private const val PAGE_SIZE = 20
    }
}
