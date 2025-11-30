package com.example.doan.Models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class DashboardSummary implements Serializable {

    @SerializedName("totalRevenue")
    private double totalRevenue;

    @SerializedName("totalOrders")
    private long totalOrders;

    @SerializedName("pendingOrders")
    private long pendingOrders;

    @SerializedName("completedOrders")
    private long completedOrders;

    @SerializedName("canceledOrders")
    private long canceledOrders;

    @SerializedName("topSellingDrinks")
    private List<TopSellingDrink> topSellingDrinks;

    public DashboardSummary() {}

    // Getters and Setters
    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public long getPendingOrders() {
        return pendingOrders;
    }

    public void setPendingOrders(long pendingOrders) {
        this.pendingOrders = pendingOrders;
    }

    public long getCompletedOrders() {
        return completedOrders;
    }

    public void setCompletedOrders(long completedOrders) {
        this.completedOrders = completedOrders;
    }

    public long getCanceledOrders() {
        return canceledOrders;
    }

    public void setCanceledOrders(long canceledOrders) {
        this.canceledOrders = canceledOrders;
    }

    public List<TopSellingDrink> getTopSellingDrinks() {
        return topSellingDrinks;
    }

    public void setTopSellingDrinks(List<TopSellingDrink> topSellingDrinks) {
        this.topSellingDrinks = topSellingDrinks;
    }

    public static class TopSellingDrink implements Serializable {
        @SerializedName("drinkName")
        private String drinkName;

        @SerializedName("totalSold")
        private long totalSold;

        @SerializedName("revenue")
        private double revenue;

        public String getDrinkName() {
            return drinkName;
        }

        public long getTotalSold() {
            return totalSold;
        }

        public double getRevenue() {
            return revenue;
        }
    }
}
