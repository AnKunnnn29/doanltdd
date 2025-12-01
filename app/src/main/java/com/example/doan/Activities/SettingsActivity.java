package com.example.doan.Activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.example.doan.R;

public class SettingsActivity extends AppCompatActivity {


    private Switch notificationSwitch;
    private static final String PREFS_NAME = "AppSettings";
    private static final String KEY_NOTIFICATIONS = "notificationsEnabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        notificationSwitch = findViewById(R.id.switch_notifications);

        loadSettingsState();

        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveNotificationSetting(isChecked);
        });
    }

    private void loadSettingsState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        boolean isEnabled = prefs.getBoolean(KEY_NOTIFICATIONS, true);
        notificationSwitch.setChecked(isEnabled);
    }

    private void saveNotificationSetting(boolean isEnabled) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_NOTIFICATIONS, isEnabled);
        editor.apply();

        String status = isEnabled ? "Bật" : "Tắt";
        Toast.makeText(this, "Thông báo đã được: " + status, Toast.LENGTH_SHORT).show();
    }
}