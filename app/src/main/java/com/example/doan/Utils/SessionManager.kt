package com.example.doan.Utils

import android.content.Context
import android.content.SharedPreferences
import com.example.doan.Models.JwtResponse

class SessionManager(private val context: Context) { // Store context as a property

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = prefs.edit()

    fun saveLoginSession(
        userId: Int,
        username: String?,
        email: String?,
        fullName: String?,
        phone: String?,
        role: String?,
        memberTier: String?,
        token: String?, // Access Token
        refreshToken: String?,
        avatar: String? = null
    ) {
        editor.apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putInt(KEY_USER_ID, userId)
            putString(KEY_USERNAME, username)
            putString(KEY_EMAIL, email)
            putString(KEY_FULL_NAME, fullName)
            putString(KEY_PHONE, phone)
            putString(KEY_ROLE, role)
            putString(KEY_MEMBER_TIER, memberTier)
            putString(KEY_TOKEN, token)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putString(KEY_AVATAR, avatar)
            apply()
        }
    }

    fun updateTokens(jwtResponse: JwtResponse) {
        editor.apply {
            putString(KEY_TOKEN, jwtResponse.accessToken)
            putString(KEY_REFRESH_TOKEN, jwtResponse.refreshToken)
            apply()
        }
    }

    fun logout() {
        // Xóa thông tin phiên đăng nhập, nhưng giữ lại dữ liệu sinh trắc học
        //Lấy ra các giá trị không cần xóa
        val biometricEnrolled = KeyStoreManager.isBiometricEnrolled(context)

        // Xóa tất cả
        editor.clear().apply()

        //Lưu lại
        if (biometricEnrolled) {
            // Hacky way to keep biometric data, ideally biometric data should be in a separate pref file
            editor.putBoolean("biometric_enabled", true).apply()
        }
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, -1)

    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)

    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)

    fun setEmail(email: String) {
        editor.putString(KEY_EMAIL, email).apply()
    }

    fun getFullName(): String? = prefs.getString(KEY_FULL_NAME, null)

    fun setFullName(fullName: String) {
        editor.putString(KEY_FULL_NAME, fullName).apply()
    }

    fun getPhoneNumber(): String? = prefs.getString(KEY_PHONE, null)

    fun getRole(): String = prefs.getString(KEY_ROLE, "USER") ?: "USER"

    fun getMemberTier(): String = prefs.getString(KEY_MEMBER_TIER, "BRONZE") ?: "BRONZE"

    fun getAvatar(): String? = prefs.getString(KEY_AVATAR, null)

    fun isManager(): Boolean = getRole() == "MANAGER"

    companion object {
        private const val PREF_NAME = "UTETeaPrefs"
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_PHONE = "phone"
        private const val KEY_ROLE = "role"
        private const val KEY_MEMBER_TIER = "member_tier"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_AVATAR = "avatar"
    }
}
