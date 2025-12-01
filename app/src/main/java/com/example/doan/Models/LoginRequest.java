package com.example.doan.Models;

import com.google.gson.annotations.SerializedName;

public class LoginRequest {


    @SerializedName("usernameOrPhone")
    private String usernameOrPhone;

    @SerializedName("password")
    private String password;

    public LoginRequest(String usernameOrPhone, String password) {
        this.usernameOrPhone = usernameOrPhone;
        this.password = password;
    }

    public String getUsernameOrPhone() {
        return usernameOrPhone;
    }

    public void setUsernameOrPhone(String usernameOrPhone) {
        this.usernameOrPhone = usernameOrPhone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}