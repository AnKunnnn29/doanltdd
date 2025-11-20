package com.example.doan;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class AccountFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        LinearLayout profileOption = view.findViewById(R.id.profile_option);
        LinearLayout settingsOption = view.findViewById(R.id.settings_option);
        LinearLayout logoutOption = view.findViewById(R.id.logout_option);

        profileOption.setOnClickListener(v -> Toast.makeText(getContext(), "Profile clicked", Toast.LENGTH_SHORT).show());

        settingsOption.setOnClickListener(v -> Toast.makeText(getContext(), "Settings clicked", Toast.LENGTH_SHORT).show());

        logoutOption.setOnClickListener(v -> Toast.makeText(getContext(), "Logout clicked", Toast.LENGTH_SHORT).show());

        return view;
    }
}
