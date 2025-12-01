package com.example.doan.Models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

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

    private int categoryId;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("is_available")
    private boolean isAvailable;

    @SerializedName("sizes")
    private List<DrinkSize> sizes;

    public Product(int id, String name, String description, double price, String category, int categoryId, String imageUrl, boolean isAvailable, List<DrinkSize> sizes) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.categoryId = categoryId;
        this.imageUrl = imageUrl;
        this.isAvailable = isAvailable;
        this.sizes = sizes;
    }
    
    // Constructor cũ (để tương thích nếu cần, nhưng nên cập nhật)
    public Product(int id, String name, String description, double price, String category, int categoryId, String imageUrl, boolean isAvailable) {
        this(id, name, description, price, category, categoryId, imageUrl, isAvailable, new ArrayList<>());
    }

    public Product() { }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public String getCategory() { return category; }
    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public String getImageUrl() { return imageUrl; }
    public boolean isAvailable() { return isAvailable; }
    
    public List<DrinkSize> getSizes() {
        return sizes;
    }

    public void setSizes(List<DrinkSize> sizes) {
        this.sizes = sizes;
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