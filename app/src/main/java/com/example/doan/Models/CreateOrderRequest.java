package com.example.doan.Models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CreateOrderRequest {
    // Backend expects camelCase, not snake_case
    @SerializedName("storeId")
    private Long storeId;

    @SerializedName("type")
    private String type;

    @SerializedName("address")
    private String address;

    @SerializedName("paymentMethod")
    private String paymentMethod;
    
    @SerializedName("pickupTime")
    private String pickupTime;
    
    @SerializedName("promotionCode")
    private String promotionCode;

    @SerializedName("items")
    private List<OrderItemRequest> items;

    // Constructor
    public CreateOrderRequest(Long storeId, String type, String address, String paymentMethod, List<OrderItemRequest> items) {
        this.storeId = storeId;
        this.type = type;
        this.address = address;
        this.paymentMethod = paymentMethod;
        this.items = items;
        this.pickupTime = null;
        this.promotionCode = null;
    }

    // Getters and Setters
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public String getPickupTime() { return pickupTime; }
    public void setPickupTime(String pickupTime) { this.pickupTime = pickupTime; }
    
    public String getPromotionCode() { return promotionCode; }
    public void setPromotionCode(String promotionCode) { this.promotionCode = promotionCode; }

    public List<OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderItemRequest> items) { this.items = items; }
}