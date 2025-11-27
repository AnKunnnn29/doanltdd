package com.example.doan.Models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class DrinkTopping implements Serializable {

    @SerializedName("id")
    private int id;

    @SerializedName("toppingName")
    private String toppingName;

    @SerializedName("price")
    private double price;

    @SerializedName("isActive")
    private boolean isActive;

    public DrinkTopping() {}

    public DrinkTopping(int id, String toppingName, double price, boolean isActive) {
        this.id = id;
        this.toppingName = toppingName;
        this.price = price;
        this.isActive = isActive;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getToppingName() {
        return toppingName;
    }

    public void setToppingName(String toppingName) {
        this.toppingName = toppingName;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
