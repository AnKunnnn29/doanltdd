package com.example.doan.Models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

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

    // Customer info
    @SerializedName("customer_name")
    private String customerName;
    
    @SerializedName("customer_phone")
    private String customerPhone;
    
    // Payment info
    @SerializedName("payment_status")
    private String paymentStatus;
    
    @SerializedName("payment_method")
    private String paymentMethod;
    
    // Timestamp
    @SerializedName("createdAt")
    private String createdAt;
    
    @SerializedName("updatedAt")
    private String updatedAt;

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
    
    public String getCustomerName() {
        return customerName;
    }
    
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
    
    public String getCustomerPhone() {
        return customerPhone;
    }
    
    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }
    
    public String getPaymentStatus() {
        return paymentStatus;
    }
    
    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}