package com.example.doan.Models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class Drink implements Serializable {

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("imageUrl")
    private String imageUrl;

    @SerializedName("basePrice")
    private double basePrice;

    @SerializedName("isActive")
    private boolean isActive;

    @SerializedName(value = "categoryId", alternate = {"category_id"})
    private int categoryId;

    @SerializedName("categoryName")
    private String categoryName;

    @SerializedName("sizes")
    private List<DrinkSize> sizes;

    @SerializedName("toppings")
    private List<DrinkTopping> toppings;

    public Drink() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public List<DrinkSize> getSizes() {
        return sizes;
    }

    public void setSizes(List<DrinkSize> sizes) {
        this.sizes = sizes;
    }

    public List<DrinkTopping> getToppings() {
        return toppings;
    }

    public void setToppings(List<DrinkTopping> toppings) {
        this.toppings = toppings;
    }
}
