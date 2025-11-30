package com.example.doan.Models;

import com.google.gson.annotations.SerializedName;

public class UserProfileDto {

    @SerializedName("id")
    private Long id;

    @SerializedName("username")
    private String username;

    @SerializedName("fullName")
    private String fullName;

    @SerializedName("email")
    private String email;

    @SerializedName("phone")
    private String phone;

    @SerializedName("address")
    private String address;

    @SerializedName("memberTier")
    private String memberTier;

    @SerializedName("points")
    private Integer points;

    // Getters
    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public String getMemberTier() {
        return memberTier;
    }

    public Integer getPoints() {
        return points;
    }
}
