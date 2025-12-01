package com.example.doan.Models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CreateOrderRequest {
    @SerializedName("user_id")
    private int userId;

    @SerializedName("store_id")
    private int storeId;

    @SerializedName("total_price")
    private double totalPrice;

    @SerializedName("final_price")
    private double finalPrice;

    @SerializedName("address")
    private String address;

    @SerializedName("payment_method")
    private String paymentMethod;

    @SerializedName("status")
    private String status;

    @SerializedName("type")
    private String type;
    
    @SerializedName("pickup_time")
    private String pickupTime;
    
    @SerializedName("promotion_id")
    private Integer promotionId; // Integer to allow null

    @SerializedName("discount")
    private double discount;

    @SerializedName("items")
    private List<OrderItemRequest> items;

    public CreateOrderRequest(int userId, int storeId, double totalPrice, List<OrderItemRequest> items) {
        this.userId = userId;
        this.storeId = storeId;
        this.totalPrice = totalPrice;
        this.finalPrice = totalPrice; 
        this.items = items;
        
        // Default values
        this.address = "Tại cửa hàng"; 
        this.paymentMethod = "CASH"; 
        this.status = "PENDING";
        this.type = "PICKUP";
        this.pickupTime = null;
        this.promotionId = null;
        this.discount = 0;
    }

    // Getters and Setters
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getStoreId() { return storeId; }
    public void setStoreId(int storeId) { this.storeId = storeId; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public double getFinalPrice() { return finalPrice; }
    public void setFinalPrice(double finalPrice) { this.finalPrice = finalPrice; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getPickupTime() { return pickupTime; }
    public void setPickupTime(String pickupTime) { this.pickupTime = pickupTime; }
    
    public Integer getPromotionId() { return promotionId; }
    public void setPromotionId(Integer promotionId) { this.promotionId = promotionId; }

    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }

    public List<OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderItemRequest> items) { this.items = items; }
}