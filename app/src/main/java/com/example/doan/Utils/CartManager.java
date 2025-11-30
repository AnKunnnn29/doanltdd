package com.example.doan.Utils;

import com.example.doan.Models.CartItem;
import com.example.doan.Models.Product;

import java.util.ArrayList;
import java.util.List;

public class CartManager {
    private static CartManager instance;
    private List<CartItem> cartItems;

    private CartManager() {
        cartItems = new ArrayList<>();
    }

    public static synchronized CartManager getInstance() {
        if (instance == null) {
            instance = new CartManager();
        }
        return instance;
    }

    public void addToCart(Product product, int quantity, String sizeName, double unitPrice) {
        // Kiểm tra xem món này (cùng ID và cùng Size) đã có trong giỏ chưa
        for (CartItem item : cartItems) {
            if (item.getProduct().getId() == product.getId() && item.getSizeName().equals(sizeName)) {
                item.setQuantity(item.getQuantity() + quantity);
                return;
            }
        }
        // Nếu chưa có, thêm mới
        cartItems.add(new CartItem(product, quantity, sizeName, unitPrice));
    }

    public void updateQuantity(int position, int newQuantity) {
        if (position >= 0 && position < cartItems.size()) {
            if (newQuantity <= 0) {
                cartItems.remove(position);
            } else {
                cartItems.get(position).setQuantity(newQuantity);
            }
        }
    }

    public void removeItem(int position) {
        if (position >= 0 && position < cartItems.size()) {
            cartItems.remove(position);
        }
    }

    public List<CartItem> getCartItems() {
        return cartItems;
    }

    public void clearCart() {
        cartItems.clear();
    }

    public double getTotalPrice() {
        double total = 0;
        for (CartItem item : cartItems) {
            total += item.getTotalPrice();
        }
        return total;
    }
    
    public int getItemCount() {
        return cartItems.size();
    }
}