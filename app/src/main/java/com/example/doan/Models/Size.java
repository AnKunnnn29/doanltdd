package com.example.doan.Models;

import com.google.gson.annotations.SerializedName;

public class Size {
    @SerializedName("id")
    private int id;
    @SerializedName("sizeName")
    private String sizeName;
    @SerializedName("extraPrice")
    private double extraPrice;

    // Getters
    public int getId() { return id; }
    public String getSizeName() { return sizeName; }
    public double getExtraPrice() { return extraPrice; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setSizeName(String sizeName) { this.sizeName = sizeName; }
    public void setExtraPrice(double extraPrice) { this.extraPrice = extraPrice; }
}
