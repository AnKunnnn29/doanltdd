package com.example.doan.Models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class OrderItemRequest {
    @SerializedName("drinkId")
    private int drinkId; // Đổi từ product_id thành drinkId
    
    @SerializedName("sizeName")
    private String sizeName; // Bắt buộc phải có
    
    @SerializedName("quantity")
    private int quantity;
    
    @SerializedName("note")
    private String note;
    
    @SerializedName("toppingIds")
    private List<Integer> toppingIds;

    // Constructor đầy đủ
    public OrderItemRequest(int drinkId, String sizeName, int quantity, String note, List<Integer> toppingIds) {
        this.drinkId = drinkId;
        this.sizeName = sizeName;
        this.quantity = quantity;
        this.note = note;
        this.toppingIds = toppingIds;
    }

    // Constructor đơn giản
    public OrderItemRequest(int drinkId, String sizeName, int quantity) {
        this.drinkId = drinkId;
        this.sizeName = sizeName;
        this.quantity = quantity;
        this.note = "";
        this.toppingIds = null;
    }

    public int getDrinkId() { return drinkId; }
    public void setDrinkId(int drinkId) { this.drinkId = drinkId; }

    public String getSizeName() { return sizeName; }
    public void setSizeName(String sizeName) { this.sizeName = sizeName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public List<Integer> getToppingIds() { return toppingIds; }
    public void setToppingIds(List<Integer> toppingIds) { this.toppingIds = toppingIds; }
}