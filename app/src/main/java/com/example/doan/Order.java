package com.example.doan;

public class Order {
    private String orderNumber;
    private String date;
    private String status;

    public Order(String orderNumber, String date, String status) {
        this.orderNumber = orderNumber;
        this.date = date;
        this.status = status;
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
}
