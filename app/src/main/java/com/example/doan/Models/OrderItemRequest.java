package com.example.doan.Models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class OrderItemRequest {
    @SerializedName("drinkId")
    private Long drinkId;

    @SerializedName("sizeName")
    private String sizeName;

    @SerializedName("quantity")
    private int quantity;

    @SerializedName("note")
    private String note;
    
    @SerializedName("toppingIds")
    private List<Long> toppingIds;

    public OrderItemRequest(Long drinkId, String sizeName, int quantity, String note, List<Long> toppingIds) {
        this.drinkId = drinkId;
        this.sizeName = sizeName;
        this.quantity = quantity;
        this.note = note;
        this.toppingIds = toppingIds;
    }
    
    // Constructor without toppings
    public OrderItemRequest(Long drinkId, String sizeName, int quantity, String note) {
        this(drinkId, sizeName, quantity, note, null);
    }

    public Long getDrinkId() {
        return drinkId;
    }

    public void setDrinkId(Long drinkId) {
        this.drinkId = drinkId;
    }

    public String getSizeName() {
        return sizeName;
    }

    public void setSizeName(String sizeName) {
        this.sizeName = sizeName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
    
    public List<Long> getToppingIds() {
        return toppingIds;
    }

    public void setToppingIds(List<Long> toppingIds) {
        this.toppingIds = toppingIds;
    }
}
