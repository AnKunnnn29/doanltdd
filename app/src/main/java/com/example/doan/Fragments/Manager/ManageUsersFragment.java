package com.example.doan.Fragments.Manager;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.doan.Adapters.UserAdapter;
import com.example.doan.Models.ApiResponse;
import com.example.doan.Models.PageResponse;
import com.example.doan.Models.User;
import com.example.doan.Network.RetrofitClient;
import com.example.doan.R;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageUsersFragment extends Fragment {

    private static final String TAG = "ManageUsersFragment";
    
    private RecyclerView rvUsers;
    private UserAdapter userAdapter;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private EditText etSearchUser;
    private MaterialButton btnFilterRole;
    private TextView tvTotalUsers, tvManagers, tvCustomers;

    private List<User> allUsers = new ArrayList<>();
    private String currentRoleFilter = null;
    private int currentPage = 0;
    private static final int PAGE_SIZE = 20;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_users, container, false);

        // Initialize views
        rvUsers = view.findViewById(R.id.rv_users);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyState = view.findViewById(R.id.empty_state);
        etSearchUser = view.findViewById(R.id.et_search_user);
        btnFilterRole = view.findViewById(R.id.btn_filter_role);
        tvTotalUsers = view.findViewById(R.id.tv_total_users);
        tvManagers = view.findViewById(R.id.tv_managers);
        tvCustomers = view.findViewById(R.id.tv_customers);

        // Setup RecyclerView
        rvUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        userAdapter = new UserAdapter(getContext(), new ArrayList<>());
        userAdapter.setOnUserActionListener(new UserAdapter.OnUserActionListener() {
            @Override
            public void onViewUser(User user) {
                showUserDetail(user);
            }

            @Override
            public void onToggleUserStatus(User user) {
                toggleUserBlock(user);
            }
        });
        rvUsers.setAdapter(userAdapter);

        // Setup listeners
        setupListeners();

        // Load data
        loadUsers();

        return view;
    }

    private void setupListeners() {
        // Swipe refresh
        swipeRefresh.setOnRefreshListener(this::loadUsers);

        // Search
        etSearchUser.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Filter
        btnFilterRole.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Lọc theo vai trò - Coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadUsers() {
        showLoading(true);
        
        Log.d(TAG, "Loading users - page: " + currentPage + ", role: " + currentRoleFilter);

        RetrofitClient.getInstance(requireContext()).getApiService().getManagerUsers(currentRoleFilter, currentPage, PAGE_SIZE)
            .enqueue(new Callback<ApiResponse<PageResponse<User>>>() {
                @Override
                public void onResponse(Call<ApiResponse<PageResponse<User>>> call, Response<ApiResponse<PageResponse<User>>> response) {
                    showLoading(false);
                    swipeRefresh.setRefreshing(false);
                    
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<PageResponse<User>> apiResponse = response.body();
                        
                        if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                            PageResponse<User> pageResponse = apiResponse.getData();
                            allUsers = pageResponse.getContent();
                            
                            Log.d(TAG, "Loaded " + allUsers.size() + " users");
                            
                            userAdapter.updateUsers(allUsers);
                            updateStats();
                            updateEmptyState();
                        } else {
                            Log.e(TAG, "API error: " + apiResponse.getMessage());
                            Toast.makeText(getContext(), "Lỗi: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "Response error: " + response.code());
                        Toast.makeText(getContext(), "Không thể tải danh sách người dùng", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<PageResponse<User>>> call, Throwable t) {
                    showLoading(false);
                    swipeRefresh.setRefreshing(false);
                    
                    Log.e(TAG, "Network error", t);
                    Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void filterUsers(String query) {
        if (query.isEmpty()) {
            userAdapter.updateUsers(allUsers);
        } else {
            List<User> filtered = new ArrayList<>();
            for (User user : allUsers) {
                if (user.getFullName().toLowerCase().contains(query.toLowerCase()) ||
                    user.getPhone().contains(query) ||
                    (user.getEmail() != null && user.getEmail().toLowerCase().contains(query.toLowerCase()))) {
                    filtered.add(user);
                }
            }
            userAdapter.updateUsers(filtered);
        }
        updateEmptyState();
    }

    private void updateStats() {
        int total = allUsers.size();
        int managers = 0;
        int customers = 0;

        for (User user : allUsers) {
            if ("MANAGER".equals(user.getRole())) {
                managers++;
            } else {
                customers++;
            }
        }

        tvTotalUsers.setText(String.valueOf(total));
        tvManagers.setText(String.valueOf(managers));
        tvCustomers.setText(String.valueOf(customers));
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        rvUsers.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void updateEmptyState() {
        emptyState.setVisibility(userAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }
    
    private void showUserDetail(User user) {
        Log.d(TAG, "Viewing user detail: " + user.getId());
        
        RetrofitClient.getInstance(requireContext()).getApiService().getUserById(user.getId())
            .enqueue(new Callback<ApiResponse<User>>() {
                @Override
                public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<User> apiResponse = response.body();
                        
                        if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                            User detailUser = apiResponse.getData();
                            showUserDetailDialog(detailUser);
                        } else {
                            Toast.makeText(getContext(), "Lỗi: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Không thể tải thông tin người dùng", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                    Log.e(TAG, "Error loading user detail", t);
                    Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    private void showUserDetailDialog(User user) {
        StringBuilder info = new StringBuilder();
        info.append("ID: ").append(user.getId()).append("\n");
        info.append("Tên: ").append(user.getFullName() != null ? user.getFullName() : "N/A").append("\n");
        info.append("Username: ").append(user.getUsername() != null ? user.getUsername() : "N/A").append("\n");
        info.append("Email: ").append(user.getEmail() != null ? user.getEmail() : "N/A").append("\n");
        info.append("SĐT: ").append(user.getPhone() != null ? user.getPhone() : "N/A").append("\n");
        info.append("Địa chỉ: ").append(user.getAddress() != null ? user.getAddress() : "N/A").append("\n");
        info.append("Vai trò: ").append(user.getRole()).append("\n");
        info.append("Hạng: ").append(user.getMemberTier() != null ? user.getMemberTier() : "N/A").append("\n");
        info.append("Điểm: ").append(user.getPoints()).append("\n");
        info.append("Trạng thái: ").append(user.isActive() ? "Hoạt động" : "Không hoạt động").append("\n");
        info.append("Khóa: ").append(user.isBlocked() ? "Đã khóa" : "Chưa khóa").append("\n");
        info.append("Ngày tạo: ").append(user.getCreatedAt() != null ? user.getCreatedAt() : "N/A").append("\n");
        info.append("Cập nhật: ").append(user.getUpdatedAt() != null ? user.getUpdatedAt() : "N/A");
        
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
            .setTitle("Thông tin người dùng")
            .setMessage(info.toString())
            .setPositiveButton("Đóng", null)
            .show();
    }
    
    private void toggleUserBlock(User user) {
        boolean newBlockStatus = !user.isBlocked();
        String action = newBlockStatus ? "khóa" : "mở khóa";
        
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
            .setTitle("Xác nhận")
            .setMessage("Bạn có chắc muốn " + action + " tài khoản " + user.getFullName() + "?")
            .setPositiveButton("Có", (dialog, which) -> {
                performToggleUserBlock(user.getId(), newBlockStatus);
            })
            .setNegativeButton("Không", null)
            .show();
    }
    
    private void performToggleUserBlock(int userId, boolean blocked) {
        Log.d(TAG, "Toggling user " + userId + " block status to: " + blocked);
        
        RetrofitClient.getInstance(requireContext()).getApiService().toggleUserBlock(userId, blocked)
            .enqueue(new Callback<ApiResponse<User>>() {
                @Override
                public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<User> apiResponse = response.body();
                        
                        if (apiResponse.isSuccess()) {
                            String message = blocked ? "Đã khóa tài khoản" : "Đã mở khóa tài khoản";
                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                            loadUsers(); // Reload list
                        } else {
                            Toast.makeText(getContext(), "Lỗi: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Không thể cập nhật trạng thái", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                    Log.e(TAG, "Error toggling user block", t);
                    Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }
}
