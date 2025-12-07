package com.example.doan.Activities

import android.content.Context
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.doan.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.materialswitch.MaterialSwitch

class SettingsActivity : AppCompatActivity() {

    private lateinit var switchDarkMode: MaterialSwitch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar_settings)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

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
