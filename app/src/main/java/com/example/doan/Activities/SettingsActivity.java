package com.example.doan.Activities;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.doan.R;

public class SettingsActivity extends AppCompatActivity {

    private Switch notificationSwitch;
    private TextView termsOfUseText;
    private TextView privacyPolicyText;

    private static final String PREFS_NAME = "AppSettings";
    private static final String KEY_NOTIFICATIONS = "notificationsEnabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        notificationSwitch = findViewById(R.id.switch_notifications);
        termsOfUseText = findViewById(R.id.text_terms_of_use);
        privacyPolicyText = findViewById(R.id.text_privacy_policy);

        loadSettingsState();

        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveNotificationSetting(isChecked);
        });

        termsOfUseText.setOnClickListener(v -> showTermsOfUseDialog());
        privacyPolicyText.setOnClickListener(v -> showPrivacyPolicyDialog());
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

    private void showTermsOfUseDialog() {
        String termsContent = "Chào mừng bạn đến với ứng dụng của chúng tôi.\n\n" +
                "1. Bằng việc sử dụng ứng dụng, bạn đồng ý với các điều khoản này.\n" +
                "2. Không sử dụng ứng dụng cho các mục đích bất hợp pháp.\n" +
                "3. Chúng tôi có quyền thay đổi các điều khoản này bất cứ lúc nào.";

        showInfoDialog("Điều khoản sử dụng", termsContent);
    }

    private void showPrivacyPolicyDialog() {
        String policyContent = "Chúng tôi tôn trọng sự riêng tư của bạn.\n\n" +
                "1. Chúng tôi thu thập thông tin cá nhân để cải thiện dịch vụ.\n" +
                "2. Dữ liệu của bạn sẽ không được chia sẻ với bên thứ ba mà không có sự đồng ý của bạn.\n" +
                "3. Chúng tôi áp dụng các biện pháp bảo mật để bảo vệ thông tin của bạn.";

        showInfoDialog("Chính sách bảo mật", policyContent);
    }

    private void showInfoDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Đã hiểu", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
