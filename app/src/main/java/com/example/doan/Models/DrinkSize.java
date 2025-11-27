package com.example.doan.Models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class DrinkSize implements Serializable {

    @SerializedName("id")
    private int id;

    @SerializedName("sizeName")
    private String sizeName;

    @SerializedName("extraPrice")
    private double extraPrice;

    public DrinkSize() {}

    public DrinkSize(int id, String sizeName, double extraPrice) {
        this.id = id;
        this.sizeName = sizeName;
        this.extraPrice = extraPrice;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSizeName() {
        return sizeName;
    }

    public void setSizeName(String sizeName) {
        this.sizeName = sizeName;
    }

    public double getExtraPrice() {
        return extraPrice;
    }

    public void setExtraPrice(double extraPrice) {
        this.extraPrice = extraPrice;
    }
}
