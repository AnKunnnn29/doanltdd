package com.example.doan.Models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Order implements Serializable {

    @SerializedName("id")
    private int id;

    @SerializedName("orderNumber")
    private String orderNumber;

    @SerializedName(value = "createdAt", alternate = {"created_at"})
    private String date;

    @SerializedName("status")
    private String status;

    @SerializedName(value = "totalPrice", alternate = {"total_price"})
    private double totalAmount;
    
    @SerializedName(value = "finalPrice", alternate = {"final_price"})
    private double finalPrice;
    
    @SerializedName("address")
    private String address;

    @SerializedName("userName")
    private String userName;

    @SerializedName("paymentMethod")
    private String paymentMethod;

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

    public void setId(int id) {
        this.id = id;
    }

    public String getOrderNumber() {
        if (orderNumber == null || orderNumber.isEmpty()) {
            return String.valueOf(id);
        }
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public double getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(double finalPrice) {
        this.finalPrice = finalPrice;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    // Helper methods for Adapter compatibility
    public String getCustomerName() {
        return userName;
    }

    public String getCustomerPhone() {
        return null; // Phone not available in DTO
    }

    public String getPaymentStatus() {
        return paymentMethod;
    }

    public Date getCreatedAt() {
        if (date == null) return null;
        try {
            // Handle ISO 8601 format (e.g., 2023-11-27T10:30:00)
            // Note: Locale.US is better for standard date formats
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            return isoFormat.parse(date);
        } catch (ParseException e) {
            try {
                 SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                 return simpleFormat.parse(date);
            } catch (ParseException ex) {
                return null;
            }
        }
    }
}