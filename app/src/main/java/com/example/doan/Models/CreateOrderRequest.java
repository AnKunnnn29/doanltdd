package com.example.doan.Models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CreateOrderRequest {
    // Backend lấy user từ Token, không cần gửi userId
    
    @SerializedName("storeId")
    private int storeId;

    @SerializedName("type")
    private String type; // "DELIVERY" hoặc "PICKUP"

    @SerializedName("address")
    private String address;

    @SerializedName("pickupTime")
    private String pickupTime; // Format: "2023-11-27T10:30:00"

    @SerializedName("paymentMethod")
    private String paymentMethod; // "CASH", "VNPAY", ...

    @SerializedName("promotionCode")
    private String promotionCode;

    @SerializedName("items")
    private List<OrderItemRequest> items;

    public CreateOrderRequest(int storeId, List<OrderItemRequest> items) {
        this.storeId = storeId;
        this.items = items;
        
        // Default values
        this.type = "PICKUP";
        this.address = "Tại cửa hàng";
        this.paymentMethod = "CASH";
        this.pickupTime = null;
        this.promotionCode = null;
    }

    // Setters để thay đổi giá trị mặc định nếu cần
    public void setType(String type) { this.type = type; }
    public void setAddress(String address) { this.address = address; }
    public void setPickupTime(String pickupTime) { this.pickupTime = pickupTime; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public void setPromotionCode(String promotionCode) { this.promotionCode = promotionCode; }
}