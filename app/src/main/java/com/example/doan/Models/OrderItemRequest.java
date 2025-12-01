package com.example.doan.Models;

import com.google.gson.annotations.SerializedName;

public class OrderItemRequest {
    @SerializedName("product_id")
    private int productId;

    @SerializedName("size")
    private String size;

    @SerializedName("quantity")
    private int quantity;

    @SerializedName("price")
    private double price;

    public OrderItemRequest(int productId, String size, int quantity, double price) {
        this.productId = productId;
        this.size = size;
        this.quantity = quantity;
        this.price = price;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}
