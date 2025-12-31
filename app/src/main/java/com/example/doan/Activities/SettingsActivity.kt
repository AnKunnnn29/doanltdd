package com.example.doan.Activities

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.Store
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.KeyStoreManager
import com.example.doan.Utils.SeasonalEffectManager
import com.example.doan.Utils.SessionManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputLayout
import com.onesignal.OneSignal
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SettingsActivity : AppCompatActivity() {

    private lateinit var switchDarkMode: MaterialSwitch
    private lateinit var switchBiometric: MaterialSwitch
    private lateinit var switchNotifications: MaterialSwitch
    private lateinit var switchSeasonalEffects: MaterialSwitch
    private lateinit var switchConfetti: MaterialSwitch
    private lateinit var switchCartAnimation: MaterialSwitch
    private lateinit var tvCurrentSeason: TextView
    private lateinit var radioGroupSeasonMode: RadioGroup
    private lateinit var radioRealtime: RadioButton
    private lateinit var radioCustom: RadioButton
    private lateinit var layoutSeasonDropdown: TextInputLayout
    private lateinit var dropdownSeason: AutoCompleteTextView
    private lateinit var sessionManager: SessionManager

    // Manager info views
    private lateinit var layoutManagerInfo: LinearLayout
    private lateinit var tvRoleValue: TextView
    private lateinit var tvManagedStores: TextView
    
    // Danh sách các mùa với emoji và tên
    private val seasonItems = mutableListOf<Pair<SeasonalEffectManager.Season, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sessionManager = SessionManager(this)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar_settings)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val sharedPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE)

        // Dark Mode
        switchDarkMode = findViewById(R.id.switch_dark_mode)
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

        // Notifications
        switchNotifications = findViewById(R.id.switch_notifications)
        val areNotificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", true)
        switchNotifications.isChecked = areNotificationsEnabled

        // Đồng bộ trạng thái với OneSignal khi activity được tạo
        if (areNotificationsEnabled) {
            OneSignal.User.pushSubscription.optIn()
        } else {
            OneSignal.User.pushSubscription.optOut()
        }

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                OneSignal.User.pushSubscription.optIn()
                Toast.makeText(this, "Thông báo đẩy đã được bật", Toast.LENGTH_SHORT).show()
            } else {
                OneSignal.User.pushSubscription.optOut()
                Toast.makeText(this, "Thông báo đẩy đã được tắt", Toast.LENGTH_SHORT).show()
            }
            sharedPreferences.edit().putBoolean("notifications_enabled", isChecked).apply()
        }

        // Seasonal Effects Settings
        setupSeasonalEffectsSettings(sharedPreferences)

        // Setup Manager Info (chỉ hiển thị cho MANAGER/ADMIN)
        setupManagerInfo()

        findViewById<TextView>(R.id.tv_about).setOnClickListener {
            showAboutDialog()
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

    /**
     * Setup thông tin quản lý cho MANAGER/ADMIN
     */
    private fun setupManagerInfo() {
        layoutManagerInfo = findViewById(R.id.layout_manager_info)
        tvRoleValue = findViewById(R.id.tv_role_value)
        tvManagedStores = findViewById(R.id.tv_managed_stores)

        val role = sessionManager.getRole()
        
        // Chỉ hiển thị cho MANAGER hoặc ADMIN
        if (role == "MANAGER" || role == "ADMIN") {
            layoutManagerInfo.visibility = View.VISIBLE
            
            // Hiển thị vai trò
            val roleDisplay = when (role) {
                "ADMIN" -> "🔑 Quản trị viên (Admin)"
                "MANAGER" -> "👔 Quản lý cửa hàng (Manager)"
                else -> role ?: "N/A"
            }
            tvRoleValue.text = roleDisplay
            
            // Load danh sách cửa hàng quản lý
            loadManagedStores()
        } else {
            layoutManagerInfo.visibility = View.GONE
        }
    }

    /**
     * Load danh sách cửa hàng mà Manager đang quản lý
     */
    private fun loadManagedStores() {
        tvManagedStores.text = "Đang tải..."
        
        RetrofitClient.getInstance(this).apiService.getMyManagedStores()
            .enqueue(object : Callback<ApiResponse<List<Store>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<Store>>>,
                    response: Response<ApiResponse<List<Store>>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val stores = response.body()?.data ?: emptyList()
                        displayManagedStores(stores)
                    } else {
                        val role = sessionManager.getRole()
                        if (role == "ADMIN") {
                            tvManagedStores.text = "📍 Quản lý tất cả cửa hàng"
                        } else {
                            tvManagedStores.text = "Chưa được phân công cửa hàng"
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<Store>>>, t: Throwable) {
                    Log.e("SettingsActivity", "Error loading managed stores", t)
                    tvManagedStores.text = "Không thể tải thông tin"
                }
            })
    }

    /**
     * Hiển thị danh sách cửa hàng
     */
    private fun displayManagedStores(stores: List<Store>) {
        if (stores.isEmpty()) {
            val role = sessionManager.getRole()
            if (role == "ADMIN") {
                tvManagedStores.text = "📍 Quản lý tất cả cửa hàng"
            } else {
                tvManagedStores.text = "Chưa được phân công cửa hàng"
            }
            return
        }

        val storeText = stores.mapIndexed { index, store ->
            val name = store.storeName ?: "Chi nhánh ${store.id}"
            val address = store.address?.let { "\n   📍 $it" } ?: ""
            "${index + 1}. $name$address"
        }.joinToString("\n\n")

        tvManagedStores.text = storeText
    }

    /**
     * Setup các cài đặt hiệu ứng theo mùa
     */
    private fun setupSeasonalEffectsSettings(sharedPreferences: android.content.SharedPreferences) {
        switchSeasonalEffects = findViewById(R.id.switch_seasonal_effects)
        switchConfetti = findViewById(R.id.switch_confetti)
        switchCartAnimation = findViewById(R.id.switch_cart_animation)
        tvCurrentSeason = findViewById(R.id.tv_current_season)
        radioGroupSeasonMode = findViewById(R.id.radio_group_season_mode)
        radioRealtime = findViewById(R.id.radio_realtime)
        radioCustom = findViewById(R.id.radio_custom)
        layoutSeasonDropdown = findViewById(R.id.layout_season_dropdown)
        dropdownSeason = findViewById(R.id.dropdown_season)

        // Setup dropdown với danh sách các mùa
        setupSeasonDropdown()
        
        // Load current mode
        val currentMode = SeasonalEffectManager.getSeasonMode(this)
        when (currentMode) {
            SeasonalEffectManager.SeasonMode.REALTIME -> radioRealtime.isChecked = true
            SeasonalEffectManager.SeasonMode.CUSTOM -> {
                radioCustom.isChecked = true
                layoutSeasonDropdown.visibility = View.VISIBLE
            }
        }
        
        // Hiển thị mùa hiện tại (realtime)
        updateCurrentSeasonDisplay()

        // Load saved preferences
        val seasonalEffectsEnabled = sharedPreferences.getBoolean("seasonal_effects_enabled", true)
        val confettiEnabled = sharedPreferences.getBoolean("confetti_enabled", true)
        val cartAnimationEnabled = sharedPreferences.getBoolean("cart_animation_enabled", true)

        switchSeasonalEffects.isChecked = seasonalEffectsEnabled
        switchConfetti.isChecked = confettiEnabled
        switchCartAnimation.isChecked = cartAnimationEnabled
        
        // Enable/disable season mode options based on switch
        updateSeasonModeVisibility(seasonalEffectsEnabled)

        // Seasonal Effects toggle
        switchSeasonalEffects.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("seasonal_effects_enabled", isChecked).apply()
            updateSeasonModeVisibility(isChecked)
            val activeSeason = SeasonalEffectManager.getActiveSeason(this)
            val emoji = SeasonalEffectManager.getSeasonalEmoji(activeSeason)
            val message = if (isChecked) {
                "Hiệu ứng theo mùa đã được bật $emoji"
            } else {
                "Hiệu ứng theo mùa đã được tắt"
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
        
        // Radio group listener
        radioGroupSeasonMode.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radio_realtime -> {
                    SeasonalEffectManager.setSeasonMode(this, SeasonalEffectManager.SeasonMode.REALTIME)
                    layoutSeasonDropdown.visibility = View.GONE
                    updateCurrentSeasonDisplay()
                    Toast.makeText(this, "🕐 Chế độ thời gian thực", Toast.LENGTH_SHORT).show()
                }
                R.id.radio_custom -> {
                    SeasonalEffectManager.setSeasonMode(this, SeasonalEffectManager.SeasonMode.CUSTOM)
                    layoutSeasonDropdown.visibility = View.VISIBLE
                    updateCurrentSeasonDisplay()
                    Toast.makeText(this, "🎨 Chế độ tự chọn", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Confetti toggle
        switchConfetti.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("confetti_enabled", isChecked).apply()
            val message = if (isChecked) "Pháo giấy đã được bật 🎉" else "Pháo giấy đã được tắt"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        // Cart Animation toggle
        switchCartAnimation.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("cart_animation_enabled", isChecked).apply()
            val message = if (isChecked) "Animation giỏ hàng đã được bật 🛒" else "Animation giỏ hàng đã được tắt"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupSeasonDropdown() {
        // Tạo danh sách các mùa
        seasonItems.clear()
        SeasonalEffectManager.getAllSeasonsWithNames().forEach { (season, emoji, name) ->
            seasonItems.add(Pair(season, "$emoji $name"))
        }
        
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            seasonItems.map { it.second }
        )
        dropdownSeason.setAdapter(adapter)
        
        // Set current custom season
        val currentCustomSeason = SeasonalEffectManager.getCustomSeason(this)
        val currentItem = seasonItems.find { it.first == currentCustomSeason }
        currentItem?.let { dropdownSeason.setText(it.second, false) }
        
        // Listener khi chọn mùa
        dropdownSeason.setOnItemClickListener { _, _, position, _ ->
            val selectedSeason = seasonItems[position].first
            SeasonalEffectManager.setCustomSeason(this, selectedSeason)
            updateCurrentSeasonDisplay()
            
            val emoji = SeasonalEffectManager.getSeasonalEmoji(selectedSeason)
            val name = SeasonalEffectManager.getSeasonNameVi(selectedSeason)
            Toast.makeText(this, "Đã chọn: $emoji $name", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateCurrentSeasonDisplay() {
        val mode = SeasonalEffectManager.getSeasonMode(this)
        val realtimeSeason = SeasonalEffectManager.getRealTimeSeason()
        val realtimeEmoji = SeasonalEffectManager.getSeasonalEmoji(realtimeSeason)
        val realtimeName = SeasonalEffectManager.getSeasonNameVi(realtimeSeason)
        
        when (mode) {
            SeasonalEffectManager.SeasonMode.REALTIME -> {
                tvCurrentSeason.text = "🕐 Thời gian thực: $realtimeEmoji $realtimeName"
            }
            SeasonalEffectManager.SeasonMode.CUSTOM -> {
                val customSeason = SeasonalEffectManager.getCustomSeason(this)
                val customEmoji = SeasonalEffectManager.getSeasonalEmoji(customSeason)
                val customName = SeasonalEffectManager.getSeasonNameVi(customSeason)
                tvCurrentSeason.text = "🎨 Đang dùng: $customEmoji $customName (Thực tế: $realtimeEmoji)"
            }
        }
    }
    
    private fun updateSeasonModeVisibility(enabled: Boolean) {
        val visibility = if (enabled) View.VISIBLE else View.GONE
        findViewById<View>(R.id.layout_season_mode)?.alpha = if (enabled) 1f else 0.5f
        radioRealtime.isEnabled = enabled
        radioCustom.isEnabled = enabled
        dropdownSeason.isEnabled = enabled
    }

}
