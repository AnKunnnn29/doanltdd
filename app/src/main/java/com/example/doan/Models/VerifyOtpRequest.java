package com.example.doan.Models;

public class VerifyOtpRequest {
    private String username;
    private String otp;

    public VerifyOtpRequest(String username, String otp) {
        this.username = username;
        this.otp = otp;
    }

    // Getters and setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}
