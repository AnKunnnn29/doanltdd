package com.example.doan.Network

import com.example.doan.Models.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    // ==================== USER PROFILE ====================
    @GET("me")
    fun getMyProfile(): Call<ApiResponse<UserProfileDto>>

    @PUT("me")
    fun updateProfile(@Body request: UpdateProfileRequest): Call<ApiResponse<UserProfileDto>>
    
    @PUT("me/change-password")
    fun changePassword(@Body request: ChangePasswordRequest): Call<String>

    // ==================== AUTHENTICATION ====================
    @POST("auth/login")
    fun login(@Body request: LoginRequest): Call<ApiResponse<LoginResponse>>

    @POST("auth/register")
    fun register(@Body request: RegisterRequest): Call<ApiResponse<RegisterResponse>>

    @POST("auth/register-with-otp")
    fun registerWithOtp(@Body request: RegisterRequest): Call<ApiResponse<String>>

    @POST("auth/otp-verify")
    fun verifyOtp(@Body request: VerifyOtpRequest): Call<ApiResponse<String>>

    @POST("auth/resend-otp")
    fun resendOtp(@Query("target") emailOrPhone: String): Call<ApiResponse<String>>

    @POST("auth/forgot-password")
    fun forgotPassword(@Body request: ForgotPasswordRequest): Call<String>
    
    @POST("auth/reset-password")
    fun resetPassword(@Body request: ResetPasswordRequest): Call<String>

    @GET("auth/health")
    fun healthCheck(): Call<ApiResponse<String>>

    // ==================== CATEGORIES ====================
    @GET("categories")
    fun getCategories(): Call<ApiResponse<List<Category>>>

    @GET("categories/{id}")
    fun getCategoryById(@Path("id") id: Int): Call<ApiResponse<Category>>

    // ==================== DRINKS ====================
    @GET("drinks")
    fun getDrinks(): Call<ApiResponse<List<Drink>>>

    @GET("drinks")
    fun getAllDrinks(): Call<ApiResponse<List<Drink>>>

    @GET("drinks/{id}")
    fun getDrinkById(@Path("id") id: Int): Call<ApiResponse<Drink>>

    @GET("drinks/category/{id}")
    fun getProductsByCategory(@Path("id") id: Int): Call<ApiResponse<List<Drink>>>

    @GET("drinks/search")
    fun searchDrinks(@Query("keyword") keyword: String): Call<ApiResponse<List<Drink>>>

    // ==================== STORES & BRANCHES ====================
    @GET("stores")
    fun getStores(): Call<ApiResponse<List<Store>>>

    @GET("stores/{id}")
    fun getStoreById(@Path("id") id: Int): Call<ApiResponse<Store>>

    @GET("stores/search")
    fun searchStores(@Query("keyword") keyword: String): Call<ApiResponse<List<Store>>>

    @GET("branches")
    fun getBranches(): Call<ApiResponse<List<Branch>>>

    // ==================== CART ====================
    @POST("cart/add")
    fun addToCart(
        @Query("userId") userId: Long,
        @Body request: AddToCartRequest
    ): Call<ApiResponse<Cart>>

    @GET("cart/{userId}")
    fun getCart(@Path("userId") userId: Long): Call<ApiResponse<Cart>>

    @PUT("cart/items/{cartItemId}")
    fun updateCartItem(
        @Path("cartItemId") cartItemId: Long,
        @Query("userId") userId: Long,
        @Query("quantity") quantity: Int
    ): Call<ApiResponse<Cart>>

    @DELETE("cart/items/{cartItemId}")
    fun removeCartItem(
        @Path("cartItemId") cartItemId: Long,
        @Query("userId") userId: Long
    ): Call<ApiResponse<Void>>

    @DELETE("cart/{userId}/clear")
    fun clearCart(@Path("userId") userId: Long): Call<ApiResponse<Void>>

    // ==================== ORDERS ====================
    @GET("orders/user/{userId}")
    fun getUserOrders(@Path("userId") userId: Int): Call<ApiResponse<List<Order>>>

    @GET("orders/user/{userId}/current")
    fun getCurrentOrder(@Path("userId") userId: Int): Call<ApiResponse<Order>>

    @GET("orders/{orderId}")
    fun getOrderById(@Path("orderId") orderId: Int): Call<ApiResponse<Order>>

    @POST("orders")
    fun createOrder(@Body request: CreateOrderRequest): Call<ApiResponse<Order>>

    @POST("orders/{id}/cancel")
    fun cancelOrder(@Path("id") orderId: Int): Call<ApiResponse<String>>

    @POST("orders/{id}/confirm")
    fun confirmOrder(@Path("id") orderId: Int): Call<ApiResponse<String>>

    // ==================== MANAGER APIs ====================
    @GET("manager/summary")
    fun getDashboardSummary(): Call<ApiResponse<DashboardSummary>>

    @GET("manager/orders")
    fun getManagerOrders(
        @Query("status") status: String?,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Call<ApiResponse<PageResponse<Order>>>

    @PUT("manager/orders/{orderId}/status")
    fun updateOrderStatus(
        @Path("orderId") orderId: Int,
        @Query("status") status: String
    ): Call<ApiResponse<Order>>

    // ==================== MANAGER USER MANAGEMENT ====================
    @GET("manager/users")
    fun getManagerUsers(
        @Query("role") role: String?,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Call<ApiResponse<PageResponse<User>>>

    @GET("manager/users/{userId}")
    fun getUserById(@Path("userId") userId: Int): Call<ApiResponse<User>>

    @PUT("manager/users/{userId}/block")
    fun toggleUserBlock(
        @Path("userId") userId: Int,
        @Query("blocked") blocked: Boolean
    ): Call<ApiResponse<User>>

    @GET("manager/users/search")
    fun searchUsers(
        @Query("keyword") keyword: String,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Call<ApiResponse<PageResponse<User>>>

    // ==================== ADMIN APIs ====================
    @POST("admin/drinks")
    fun createDrink(@Body drink: Drink): Call<ApiResponse<Drink>>

    @PUT("admin/drinks/{id}")
    fun updateDrink(@Path("id") id: Int, @Body drink: Drink): Call<ApiResponse<Drink>>

    @DELETE("admin/drinks/{id}")
    fun deleteDrink(@Path("id") id: Long): Call<ApiResponse<Void>>

    @GET("admin/categories")
    fun getAdminCategories(): Call<ApiResponse<List<Category>>>

    @POST("admin/categories")
    fun createCategory(@Body category: Category): Call<ApiResponse<Category>>

    @PUT("admin/categories/{id}")
    fun updateCategory(@Path("id") id: Int, @Body category: Category): Call<ApiResponse<Category>>

    @DELETE("admin/categories/{id}")
    fun deleteCategory(@Path("id") id: Int): Call<ApiResponse<Void>>

    // ==================== LEGACY (Giữ lại để tương thích) ====================
    @GET("orders")
    fun getOrders(@Query("userId") userId: Int): Call<List<Order>>

    @Multipart
    @POST("products")
    fun addProduct(
        @Part image: MultipartBody.Part,
        @Part("name") name: RequestBody,
        @Part("price") price: RequestBody,
        @Part("description") description: RequestBody,
        @Part("category") category: RequestBody
    ): Call<Product>

    @GET("products")
    fun getProducts(): Call<List<Product>>
}
