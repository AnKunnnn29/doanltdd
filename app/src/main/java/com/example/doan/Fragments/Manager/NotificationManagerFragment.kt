package com.example.doan.Fragments.Manager

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Adapters.UserSelectableAdapter
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.NotificationRequestDto
import com.example.doan.Models.PageResponse
import com.example.doan.Models.User
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotificationManagerFragment : Fragment() {

    private lateinit var edtTitle: EditText
    private lateinit var edtContent: EditText
    private lateinit var rgSendTo: RadioGroup
    private lateinit var rbSendAll: RadioButton
    private lateinit var specificUsersLayout: LinearLayout
    private lateinit var edtSearchUser: EditText
    private lateinit var rvUsers: RecyclerView
    private lateinit var btnSend: Button

    private lateinit var userAdapter: UserSelectableAdapter
    private val allUsers = mutableListOf<User>()
    private val filteredUsers = mutableListOf<User>()
    private val selectedUsers = mutableSetOf<User>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notification_manager, container, false)

        edtTitle = view.findViewById(R.id.edt_title)
        edtContent = view.findViewById(R.id.edt_content)
        rgSendTo = view.findViewById(R.id.rg_send_to)
        rbSendAll = view.findViewById(R.id.rb_send_all)
        specificUsersLayout = view.findViewById(R.id.specific_users_layout)
        edtSearchUser = view.findViewById(R.id.edt_search_user)
        rvUsers = view.findViewById(R.id.rv_users)
        btnSend = view.findViewById(R.id.btn_send)

        setupRecyclerView()
        setupListeners()
        loadUsers()

        return view
    }

    private fun setupRecyclerView() {
        userAdapter = UserSelectableAdapter(filteredUsers, selectedUsers)
        rvUsers.adapter = userAdapter
    }

    private fun setupListeners() {
        rgSendTo.setOnCheckedChangeListener { _, checkedId ->
            specificUsersLayout.visibility = if (checkedId == R.id.rb_send_to_specific) View.VISIBLE else View.GONE
        }

        edtSearchUser.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterUsers(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnSend.setOnClickListener { sendNotification() }
    }

    private fun loadUsers() {
        // For simplicity, we load all users at once. Consider pagination for large user bases.
        RetrofitClient.getInstance(requireContext()).apiService.getManagerUsers(role = "USER", page = 0, size = 1000)
            .enqueue(object : Callback<ApiResponse<PageResponse<User>>> {
                override fun onResponse(
                    call: Call<ApiResponse<PageResponse<User>>>,
                    response: Response<ApiResponse<PageResponse<User>>>
                ) {
                    if (response.isSuccessful && response.body()?.data != null) {
                        response.body()?.data?.content?.let { userList ->
                            allUsers.clear()
                            allUsers.addAll(userList) // userList is guaranteed to be non-null here
                            filterUsers("")
                        } ?: run {
                            // This block runs if any part of the chain is null
                            Toast.makeText(context, "Failed to load users: No user data found", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Failed to load users", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<PageResponse<User>>>, t: Throwable) {
                    Toast.makeText(context, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun filterUsers(query: String) {
        filteredUsers.clear()
        if (query.isEmpty()) {
            filteredUsers.addAll(allUsers)
        } else {
            val lowerCaseQuery = query.lowercase()
            allUsers.forEach {
                if (it.fullName?.lowercase()?.contains(lowerCaseQuery) == true ||
                    it.username!!.lowercase().contains(lowerCaseQuery)) {
                    filteredUsers.add(it)
                }
            }
        }
        userAdapter.notifyDataSetChanged()
    }

    private fun sendNotification() {
        val title = edtTitle.text.toString().trim()
        val content = edtContent.text.toString().trim()

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val isSendAll = rbSendAll.isChecked
        val userIds = if (isSendAll) null else selectedUsers.mapNotNull { it.id?.toString() }

        if (!isSendAll && userIds.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Please select at least one user", Toast.LENGTH_SHORT).show()
            return
        }

        val request = NotificationRequestDto(title, content, isSendAll, userIds)

        btnSend.isEnabled = false
        btnSend.text = "Sending..."

        RetrofitClient.getInstance(requireContext()).apiService.sendCustomNotification(request)
            .enqueue(object : Callback<ApiResponse<String>> {
                override fun onResponse(call: Call<ApiResponse<String>>, response: Response<ApiResponse<String>>) {
                    btnSend.isEnabled = true
                    btnSend.text = "Send Notification"
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Notification sent successfully", Toast.LENGTH_SHORT).show()
                        clearForm()
                    } else {
                        Toast.makeText(context, "Failed to send notification", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<String>>, t: Throwable) {
                    btnSend.isEnabled = true
                    btnSend.text = "Send Notification"
                    Toast.makeText(context, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun clearForm() {
        edtTitle.text.clear()
        edtContent.text.clear()
        rbSendAll.isChecked = true
        selectedUsers.clear()
        filterUsers("")
    }
}
