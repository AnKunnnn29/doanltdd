package com.example.doan.Models;

import java.util.List;

public class CartItem {
    private Long id;
    private Long drinkId;
    private String drinkName;
    private String drinkImage;
    private Long sizeId;
    private String sizeName;
    private Integer quantity;
    private Double unitPrice;
    private Double totalPrice;
    private List<DrinkTopping> toppings;
    private String note;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDrinkId() { return drinkId; }
    public void setDrinkId(Long drinkId) { this.drinkId = drinkId; }

    public String getDrinkName() { return drinkName; }
    public void setDrinkName(String drinkName) { this.drinkName = drinkName; }

    public String getDrinkImage() { return drinkImage; }
    public void setDrinkImage(String drinkImage) { this.drinkImage = drinkImage; }

    public Long getSizeId() { return sizeId; }
    public void setSizeId(Long sizeId) { this.sizeId = sizeId; }

    public String getSizeName() { return sizeName; }
    public void setSizeName(String sizeName) { this.sizeName = sizeName; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Double unitPrice) { this.unitPrice = unitPrice; }

    public Double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Double totalPrice) { this.totalPrice = totalPrice; }

    public List<DrinkTopping> getToppings() { return toppings; }
    public void setToppings(List<DrinkTopping> toppings) { this.toppings = toppings; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    
    // Helper method to get Product-like object for compatibility
    public Product getProduct() {
        Product product = new Product();
        product.setId(drinkId != null ? drinkId.intValue() : 0);
        product.setName(drinkName);
        product.setImageUrl(drinkImage);
        product.setPrice(unitPrice != null ? unitPrice : 0.0);
        return product;
    }
    
    // Helper setters for Product compatibility
    public void setProduct(Product product) {
        this.drinkId = (long) product.getId();
        this.drinkName = product.getName();
        this.drinkImage = product.getImageUrl();
        this.unitPrice = product.getPrice();
    }
}
