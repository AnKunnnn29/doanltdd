package com.example.doan.Activities

import android.content.Context
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.doan.R
import com.example.doan.Utils.KeyStoreManager
import com.example.doan.Utils.SessionManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.materialswitch.MaterialSwitch

class SettingsActivity : AppCompatActivity() {

    private lateinit var switchDarkMode: MaterialSwitch
    private lateinit var switchBiometric: MaterialSwitch
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sessionManager = SessionManager(this)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar_settings)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Dark Mode
        switchDarkMode = findViewById(R.id.switch_dark_mode)
        val sharedPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val isDarkMode = sharedPreferences.getBoolean("dark_mode", false)
        switchDarkMode.isChecked = isDarkMode
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            sharedPreferences.edit().putBoolean("dark_mode", isChecked).apply()
        }

        // Biometric
        switchBiometric = findViewById(R.id.switch_biometric)
        switchBiometric.isChecked = KeyStoreManager.isBiometricEnrolled(this)
        switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                setupBiometricsEnrollment()
            } else {
                KeyStoreManager.clearBiometricData(this)
                Toast.makeText(this, "Đăng nhập bằng vân tay đã được tắt", Toast.LENGTH_SHORT).show()
            }
        }


        findViewById<TextView>(R.id.tv_about).setOnClickListener {
            showAboutDialog()
        }

        val notificationsSwitch = findViewById<MaterialSwitch>(R.id.switch_notifications)
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(this, "Thông báo đẩy đã được bật", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Thông báo đẩy đã được tắt", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<TextView>(R.id.tv_terms_of_use).setOnClickListener {
            showTermsDialog()
        }

        findViewById<TextView>(R.id.tv_privacy_policy).setOnClickListener {
            showPrivacyPolicyDialog()
        }
    }

    private fun setupBiometricsEnrollment() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                val refreshToken = sessionManager.getRefreshToken() // Lấy refreshToken từ SessionManager
                if (refreshToken != null) {
                    showBiometricPromptForEncryption(refreshToken)
                } else {
                    Toast.makeText(this, "Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show()
                    switchBiometric.isChecked = false
                }
            }
            else -> {
                Toast.makeText(this, "Thiết bị không hỗ trợ hoặc chưa cài đặt vân tay!", Toast.LENGTH_LONG).show()
                switchBiometric.isChecked = false
            }
        }
    }

    private fun showBiometricPromptForEncryption(token: String) {
        val executor = ContextCompat.getMainExecutor(this)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Kích hoạt đăng nhập bằng vân tay")
            .setSubtitle("Xác thực để bảo mật token đăng nhập.")
            .setNegativeButtonText("Hủy")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                try {
                    KeyStoreManager.saveEncryptedToken(this@SettingsActivity, token, result.cryptoObject!!)
                    Toast.makeText(applicationContext, "Kích hoạt vân tay thành công!", Toast.LENGTH_SHORT).show()
                    switchBiometric.isChecked = true
                } catch (e: Exception) {
                    Toast.makeText(applicationContext, "Lỗi bảo mật: " + e.message, Toast.LENGTH_LONG).show()
                    switchBiometric.isChecked = false
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(applicationContext, "Lỗi xác thực: $errString", Toast.LENGTH_SHORT).show()
                switchBiometric.isChecked = false
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(applicationContext, "Xác thực thất bại", Toast.LENGTH_SHORT).show()
                switchBiometric.isChecked = false
            }
        })

        try {
            val cryptoObject = KeyStoreManager.getEncryptionCryptoObject()
            biometricPrompt.authenticate(promptInfo, cryptoObject)
        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi khởi tạo bảo mật: " + e.message, Toast.LENGTH_LONG).show()
            switchBiometric.isChecked = false
        }
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Giới thiệu")
            .setMessage("Đây là ứng dụng đặt hàng trà sữa, cho phép bạn khám phá và đặt mua những loại trà sữa yêu thích của mình một cách nhanh chóng và tiện lợi.")
            .setPositiveButton("Đóng", null)
            .show()
    }

    private fun showTermsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Điều khoản sử dụng")
            .setMessage(
                "1. Giới thiệu\n" +
                        "- Ứng dụng dùng để đặt trà sữa và các sản phẩm đồ uống.\n\n" +

                        "2. Quy định sử dụng\n" +
                        "- Người dùng phải cung cấp thông tin chính xác khi đặt hàng.\n" +
                        "- Không lạm dụng ứng dụng để gian lận khuyến mãi hoặc gây thiệt hại cho cửa hàng.\n\n" +

                        "3. Thanh toán\n" +
                        "- Thanh toán có thể được thực hiện bằng tiền mặt khi nhận hàng hoặc các phương thức hỗ trợ trong ứng dụng.\n\n" +

                        "4. Trách nhiệm của người dùng\n" +
                        "- Kiểm tra kỹ đơn hàng trước khi xác nhận.\n" +
                        "- Nhận hàng đúng thời gian, hạn chế hủy đơn không hợp lệ.\n\n" +

                        "5. Thay đổi điều khoản\n" +
                        "- Cửa hàng có quyền cập nhật điều khoản mà không cần thông báo trước."
            )
            .setPositiveButton("Đóng", null)
            .show()
    }


    private fun showPrivacyPolicyDialog() {
        AlertDialog.Builder(this)
            .setTitle("Chính sách bảo mật")
            .setMessage(
                "1. Thu thập thông tin\n" +
                        "- Chúng tôi thu thập tên, số điện thoại, địa chỉ giao hàng và lịch sử đơn hàng để phục vụ việc giao hàng.\n\n" +

                        "2. Sử dụng thông tin\n" +
                        "- Thông tin được sử dụng để xử lý đơn hàng, chăm sóc khách hàng và cải thiện dịch vụ.\n\n" +

                        "3. Chia sẻ thông tin\n" +
                        "- Chúng tôi chỉ chia sẻ thông tin với đối tác giao hàng để thực hiện việc vận chuyển.\n" +
                        "- Không bán hoặc trao đổi thông tin cá nhân cho bên thứ ba.\n\n" +

                        "4. Bảo mật thông tin\n" +
                        "- Dữ liệu được mã hóa và bảo vệ theo tiêu chuẩn hệ thống.\n\n" +

                        "5. Quyền của người dùng\n" +
                        "- Người dùng có thể yêu cầu xem, cập nhật hoặc xóa thông tin cá nhân của mình.\n\n" +

                        "6. Thay đổi chính sách\n" +
                        "- Chính sách có thể được cập nhật tùy theo nhu cầu cải tiến sản phẩm."
            )
            .setPositiveButton("Đóng", null)
            .show()
    }

}
