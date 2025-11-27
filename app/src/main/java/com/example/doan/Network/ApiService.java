package com.example.doan.Network;

import com.example.doan.Models.ApiResponse;
import com.example.doan.Models.Category;
import com.example.doan.Models.Drink;
import com.example.doan.Models.LoginRequest;
import com.example.doan.Models.LoginResponse;
import com.example.doan.Models.Order;
import com.example.doan.Models.Product;
import com.example.doan.Models.RegisterRequest;
import com.example.doan.Models.RegisterResponse;
import com.example.doan.Models.Store;

import java.util.List;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Multipart;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // ==================== AUTHENTICATION ====================
    @POST("auth/login")
    Call<ApiResponse<LoginResponse>> login(@Body LoginRequest request);

    @POST("auth/register")
    Call<ApiResponse<RegisterResponse>> register(@Body RegisterRequest request);

    @GET("auth/health")
    Call<ApiResponse<String>> healthCheck();

    // ==================== CATEGORIES ====================
    @GET("categories")
    Call<ApiResponse<List<Category>>> getCategories();

    @GET("categories/{id}")
    Call<ApiResponse<Category>> getCategoryById(@Path("id") int id);

    // ==================== DRINKS ====================
    @GET("drinks")
    Call<ApiResponse<List<Drink>>> getDrinks();

    @GET("drinks/{id}")
    Call<ApiResponse<Drink>> getDrinkById(@Path("id") int id);

    @GET("drinks/search")
    Call<ApiResponse<List<Drink>>> searchDrinks(@Query("keyword") String keyword);

    // ==================== STORES ====================
    @GET("stores")
    Call<ApiResponse<List<Store>>> getStores();

    @GET("stores/{id}")
    Call<ApiResponse<Store>> getStoreById(@Path("id") int id);

    @GET("stores/search")
    Call<ApiResponse<List<Store>>> searchStores(@Query("keyword") String keyword);

    // ==================== ORDERS ====================
    @GET("orders/user/{userId}")
    Call<ApiResponse<List<Order>>> getUserOrders(@Path("userId") int userId);

    @GET("orders/user/{userId}/current")
    Call<ApiResponse<Order>> getCurrentOrder(@Path("userId") int userId);

    @GET("orders/{orderId}")
    Call<ApiResponse<Order>> getOrderById(@Path("orderId") int orderId);

    // ==================== LEGACY (Giữ lại để tương thích) ====================
    @GET("orders")
    Call<List<Order>> getOrders(@Query("userId") int userId);

    @Multipart
    @POST("products")
    Call<Product> addProduct(
            @Part MultipartBody.Part image,
            @Part("name") RequestBody name,
            @Part("price") RequestBody price,
            @Part("description") RequestBody description,
            @Part("category") RequestBody category
    );

    @GET("products")
    Call<List<Product>> getProducts();
}