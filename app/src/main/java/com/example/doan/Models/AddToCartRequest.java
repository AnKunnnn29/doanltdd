package com.example.doan.Models;

import java.util.List;

public class AddToCartRequest {
    private Long drinkId;
    private Long sizeId;
    private Integer quantity;
    private List<Long> toppingIds;
    private String note;

    public AddToCartRequest(Long drinkId, Long sizeId, Integer quantity, List<Long> toppingIds, String note) {
        this.drinkId = drinkId;
        this.sizeId = sizeId;
        this.quantity = quantity;
        this.toppingIds = toppingIds;
        this.note = note;
    }

    // Getters and Setters
    public Long getDrinkId() { return drinkId; }
    public void setDrinkId(Long drinkId) { this.drinkId = drinkId; }

    public Long getSizeId() { return sizeId; }
    public void setSizeId(Long sizeId) { this.sizeId = sizeId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public List<Long> getToppingIds() { return toppingIds; }
    public void setToppingIds(List<Long> toppingIds) { this.toppingIds = toppingIds; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
