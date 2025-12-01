package com.example.doan.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.doan.Models.CartItem;
import com.example.doan.Models.Product;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CartManager {
    private static final String TAG = "CartManager";
    private static final String PREFS_NAME = "CartPrefs";
    private static final String KEY_CART_ITEMS = "cart_items";
    private static final String KEY_LAST_ACTIVITY = "last_activity_time";
    private static final long CART_TIMEOUT_MS = 5 * 60 * 1000; // 5 phút
    
    private static CartManager instance;
    private List<CartItem> cartItems;
    private Context context;
    private long lastActivityTime;
    private Gson gson;

    private CartManager() {
        cartItems = new ArrayList<>();
        gson = new Gson();
        lastActivityTime = System.currentTimeMillis();
    }

    public static synchronized CartManager getInstance() {
        if (instance == null) {
            instance = new CartManager();
        }
        return instance;
    }
    
    /**
     * Initialize CartManager with Context (call this in Application or first Activity)
     */
    public void init(Context context) {
        this.context = context.getApplicationContext();
        loadCart();
        loadLastActivityTime();
        clearCartIfExpired();
    }

    public void addToCart(Product product, int quantity, String sizeName, double unitPrice) {
        // Kiểm tra xem món này (cùng ID và cùng Size) đã có trong giỏ chưa
        for (CartItem item : cartItems) {
            if (item.getDrinkId() != null && item.getDrinkId() == product.getId() && 
                item.getSizeName() != null && item.getSizeName().equals(sizeName)) {
                item.setQuantity(item.getQuantity() + quantity);
                item.setTotalPrice(item.getUnitPrice() * item.getQuantity());
                updateLastActivityTime();
                saveCart();
                return;
            }
        }
        // Nếu chưa có, thêm mới
        CartItem newItem = new CartItem();
        newItem.setDrinkId((long) product.getId());
        newItem.setDrinkName(product.getName());
        newItem.setDrinkImage(product.getImageUrl());
        newItem.setSizeName(sizeName);
        newItem.setQuantity(quantity);
        newItem.setUnitPrice(unitPrice);
        newItem.setTotalPrice(unitPrice * quantity);
        
        cartItems.add(newItem);
        updateLastActivityTime();
        saveCart();
        Log.i(TAG, "Added to cart: " + product.getName() + " x" + quantity);
    }

    public void updateQuantity(int position, int newQuantity) {
        if (position >= 0 && position < cartItems.size()) {
            if (newQuantity <= 0) {
                cartItems.remove(position);
            } else {
                CartItem item = cartItems.get(position);
                item.setQuantity(newQuantity);
                item.setTotalPrice(item.getUnitPrice() * newQuantity);
            }
            updateLastActivityTime();
            saveCart();
        }
    }

    public void removeItem(int position) {
        if (position >= 0 && position < cartItems.size()) {
            cartItems.remove(position);
            updateLastActivityTime();
            saveCart();
            Log.i(TAG, "Removed item at position: " + position);
        }
    }

    public List<CartItem> getCartItems() {
        clearCartIfExpired();
        return cartItems;
    }

    public void clearCart() {
        cartItems.clear();
        saveCart();
        Log.i(TAG, "Cart cleared");
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
    
    /**
     * Update last activity time (call when user interacts with cart)
     */
    public void updateLastActivityTime() {
        lastActivityTime = System.currentTimeMillis();
        saveLastActivityTime();
    }
    
    /**
     * Check if cart has expired (5 minutes of inactivity)
     */
    public void clearCartIfExpired() {
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - lastActivityTime;
        
        if (timeDiff > CART_TIMEOUT_MS) {
            Log.w(TAG, "Cart expired after " + (timeDiff / 1000) + " seconds. Clearing cart.");
            clearCart();
        }
    }
    
    /**
     * Save cart to SharedPreferences
     */
    private void saveCart() {
        if (context == null) {
            Log.e(TAG, "Context is null, cannot save cart");
            return;
        }
        
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String json = gson.toJson(cartItems);
            prefs.edit().putString(KEY_CART_ITEMS, json).apply();
            Log.d(TAG, "Cart saved: " + cartItems.size() + " items");
        } catch (Exception e) {
            Log.e(TAG, "Error saving cart: " + e.getMessage());
        }
    }
    
    /**
     * Load cart from SharedPreferences
     */
    private void loadCart() {
        if (context == null) {
            Log.e(TAG, "Context is null, cannot load cart");
            return;
        }
        
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String json = prefs.getString(KEY_CART_ITEMS, null);
            
            if (json != null && !json.isEmpty()) {
                Type type = new TypeToken<List<CartItem>>(){}.getType();
                cartItems = gson.fromJson(json, type);
                if (cartItems == null) {
                    cartItems = new ArrayList<>();
                }
                Log.d(TAG, "Cart loaded: " + cartItems.size() + " items");
            } else {
                cartItems = new ArrayList<>();
                Log.d(TAG, "No saved cart found");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading cart: " + e.getMessage());
            cartItems = new ArrayList<>();
        }
    }
    
    /**
     * Save last activity time
     */
    private void saveLastActivityTime() {
        if (context == null) return;
        
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(KEY_LAST_ACTIVITY, lastActivityTime).apply();
    }
    
    /**
     * Load last activity time
     */
    private void loadLastActivityTime() {
        if (context == null) {
            lastActivityTime = System.currentTimeMillis();
            return;
        }
        
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        lastActivityTime = prefs.getLong(KEY_LAST_ACTIVITY, System.currentTimeMillis());
    }
}