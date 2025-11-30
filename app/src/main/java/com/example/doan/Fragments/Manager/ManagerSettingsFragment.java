package com.example.doan.Fragments.Manager;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.doan.Activities.LoginActivity;
import com.example.doan.R;
import com.example.doan.Utils.SessionManager;
import com.google.android.material.card.MaterialCardView;

public class ManagerSettingsFragment extends Fragment {

    private SessionManager sessionManager;
    private TextView tvManagerName, tvManagerPhone, tvManagerRole;
    private MaterialCardView cardProfile, cardStore, cardUsers, cardLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manager_settings, container, false);

        sessionManager = new SessionManager(requireContext());

        // Initialize views
        tvManagerName = view.findViewById(R.id.tv_manager_name);
        tvManagerPhone = view.findViewById(R.id.tv_manager_phone);
        tvManagerRole = view.findViewById(R.id.tv_manager_role);
        
        cardProfile = view.findViewById(R.id.card_profile);
        cardStore = view.findViewById(R.id.card_store);
        cardUsers = view.findViewById(R.id.card_users);
        cardLogout = view.findViewById(R.id.card_logout);

        // Load manager info
        loadManagerInfo();

        // Setup listeners
        setupListeners();

        return view;
    }

    private void loadManagerInfo() {
        String fullName = sessionManager.getFullName();
        String phone = sessionManager.getPhone();
        String role = sessionManager.getRole();

        tvManagerName.setText(fullName != null ? fullName : "Manager");
        tvManagerPhone.setText(phone != null ? phone : "N/A");
        tvManagerRole.setText(role != null ? role : "MANAGER");
    }

    private void setupListeners() {
        // Profile card
        cardProfile.setOnClickListener(v -> {
            // TODO: Open profile edit
            android.widget.Toast.makeText(getContext(), "Chỉnh sửa profile - Coming soon", android.widget.Toast.LENGTH_SHORT).show();
        });

        // Store management card
        cardStore.setOnClickListener(v -> {
            navigateToFragment(new ManageStoresFragment());
        });

        // Users management card
        cardUsers.setOnClickListener(v -> {
            navigateToFragment(new ManageUsersFragment());
        });

        // Logout card
        cardLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc muốn đăng xuất?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    // Logout
                    sessionManager.logout();
                    
                    // Go to login
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void navigateToFragment(Fragment fragment) {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.manager_content_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }
}
