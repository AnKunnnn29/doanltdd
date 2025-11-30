package com.example.doan.Models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Order implements Serializable {

    @SerializedName("id")
    private int id;

    // Bảng DB không có order_number, dùng ID làm mã đơn
    @SerializedName("order_number")
    private String orderNumber;

    // Ánh xạ từ created_at trong DB
    @SerializedName("created_at")
    private String date;

    @SerializedName("status")
    private String status;

    // Ánh xạ từ total_price trong DB
    @SerializedName("total_price")
    private double totalAmount;
    
    @SerializedName("final_price")
    private double finalPrice;
    
    @SerializedName("address")
    private String address;

    public Order(int id, String orderNumber, String date, String status, double totalAmount) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.date = date;
        this.status = status;
        this.totalAmount = totalAmount;
    }

    public Order() {
    }

    public int getId() {
        return id;
    }

    public String getOrderNumber() {
        // Nếu server không trả về order_number, dùng ID thay thế
        if (orderNumber == null || orderNumber.isEmpty()) {
            return String.valueOf(id);
        }
        return orderNumber;
    }

    public String getDate() {
        return date;
    }

    public String getStatus() {
        return status;
    }

    public double getTotalAmount() {
        return totalAmount;
    }
    
    public double getFinalPrice() {
        return finalPrice;
    }
    
    public String getAddress() {
        return address;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }
}