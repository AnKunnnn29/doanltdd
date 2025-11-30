package com.example.doan.Fragments.Manager;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.example.doan.Models.User;
import com.example.doan.R;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class ManageUsersFragment extends Fragment {

    private RecyclerView rvUsers;
    private UserAdapter userAdapter;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private EditText etSearchUser;
    private MaterialButton btnFilterRole;
    private TextView tvTotalUsers, tvManagers, tvCustomers;

    private List<User> allUsers = new ArrayList<>();

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

        // TODO: Call API to get users
        // For now, show mock data
        allUsers = getMockUsers();
        userAdapter.updateUsers(allUsers);
        updateStats();
        updateEmptyState();
        
        showLoading(false);
        swipeRefresh.setRefreshing(false);
    }

    private List<User> getMockUsers() {
        List<User> users = new ArrayList<>();
        // Mock data - replace with actual API call
        User user1 = new User();
        user1.setId(1);
        user1.setFullName("Nguyễn Văn A");
        user1.setPhone("0123456789");
        user1.setEmail("nguyenvana@example.com");
        user1.setRole("USER");
        users.add(user1);

        User user2 = new User();
        user2.setId(2);
        user2.setFullName("Trần Thị B");
        user2.setPhone("0987654321");
        user2.setEmail("tranthib@example.com");
        user2.setRole("MANAGER");
        users.add(user2);

        return users;
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
}
