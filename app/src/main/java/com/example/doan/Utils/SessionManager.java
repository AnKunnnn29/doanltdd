package com.example.doan.Utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    
    private static final String PREF_NAME = "UTETeaPrefs";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_ROLE = "role";
    private static final String KEY_MEMBER_TIER = "member_tier";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private Context context;
    
    public SessionManager(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }
    
    public void saveLoginSession(int userId, String username, String fullName, String phone, 
                                  String role, String memberTier, String token) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_FULL_NAME, fullName);
        editor.putString(KEY_PHONE, phone);
        editor.putString(KEY_ROLE, role);
        editor.putString(KEY_MEMBER_TIER, memberTier);
        editor.putString(KEY_TOKEN, token);
        editor.apply();
    }
    
    public void logout() {
        editor.clear();
        editor.apply();
    }
    
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }
    
    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }
    
    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, -1);
    }
    
    public String getUsername() {
        return prefs.getString(KEY_USERNAME, null);
    }
    
    public String getFullName() {
        return prefs.getString(KEY_FULL_NAME, null);
    }
    
    public String getPhone() {
        return prefs.getString(KEY_PHONE, null);
    }
    
    public String getRole() {
        return prefs.getString(KEY_ROLE, "USER");
    }
    
    public String getMemberTier() {
        return prefs.getString(KEY_MEMBER_TIER, "BRONZE");
    }
    
    public boolean isManager() {
        return "MANAGER".equals(getRole());
    }
}
