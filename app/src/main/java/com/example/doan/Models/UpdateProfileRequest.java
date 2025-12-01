package com.example.doan.Models;

public class UpdateProfileRequest {

    private String fullName;
    private String email;
    private String phone;
    private String address;


    public UpdateProfileRequest(String fullName, String email, String phone, String address) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.address = address;
    }
}
