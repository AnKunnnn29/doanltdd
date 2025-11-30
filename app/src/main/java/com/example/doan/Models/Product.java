package com.example.doan.Models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Product implements Serializable {

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("price")
    private double price;

    @SerializedName("category")
    private String category;

    private int categoryId; // Added categoryId

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("is_available")
    private boolean isAvailable;


    public Product(int id, String name, String description, double price, String category, int categoryId, String imageUrl, boolean isAvailable) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.categoryId = categoryId;
        this.imageUrl = imageUrl;
        this.isAvailable = isAvailable;
    }
    
    // Constructor for backward compatibility or if categoryId is not available
    public Product(int id, String name, String description, double price, String category, String imageUrl, boolean isAvailable) {
        this(id, name, description, price, category, 0, imageUrl, isAvailable);
    }

    public Product() { }


    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getPrice() {
        return price;
    }

    public String getCategory() {
        return category;
    }
    
    public int getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", category='" + category + '\'' +
                ", categoryId=" + categoryId +
                ", isAvailable=" + isAvailable +
                '}';
    }
}