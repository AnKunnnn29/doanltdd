package com.example.doan.Models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Order implements Serializable {

    @SerializedName("id")
    private int id;

    @SerializedName("order_number")
    private String orderNumber;

    @SerializedName("order_date")
    private String date;

    @SerializedName("status")
    private String status;

    @SerializedName("total_amount")
    private double totalAmount;

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