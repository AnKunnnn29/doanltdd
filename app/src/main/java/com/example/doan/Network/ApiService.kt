package com.example.doan.Network

import com.example.doan.Models.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    // ==================== OTP ====================
    @POST("otp/send")
    fun sendOtp(@Query("phone") phone: String): Call<ApiResponse<String>>

    @POST("otp/verify")
    fun verifyOtp(
        @Query("phone") phone: String, 
        @Query("code") code: String
    ): Call<ApiResponse<Boolean>>

    // ==================== NOTIFICATIONS ====================
    @POST("notifications/send")
    fun sendCustomNotification(@Body request: NotificationRequestDto): Call<ApiResponse<String>>
    
    @GET("notifications/my")
    fun getMyNotifications(): Call<ApiResponse<List<NotificationDto>>>
    
    @GET("notifications/paged")
    fun getMyNotificationsPaged(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Call<ApiResponse<PageResponse<NotificationDto>>>
    
    @GET("notifications/unread")
    fun getUnreadNotifications(): Call<ApiResponse<List<NotificationDto>>>
    
    @GET("notifications/unread-count")
    fun getUnreadNotificationCount(): Call<ApiResponse<Map<String, Long>>>
    
    @PUT("notifications/{id}/read")
    fun markNotificationAsRead(@Path("id") id: Long): Call<ApiResponse<NotificationDto>>
    
    @PUT("notifications/read-all")
    fun markAllNotificationsAsRead(): Call<ApiResponse<Map<String, Int>>>
    
    @DELETE("notifications/{id}")
    fun deleteNotification(@Path("id") id: Long): Call<ApiResponse<String>>

    // ==================== CHATBOT ====================
    @POST("chatbot/message")
    fun sendChatMessage(@Body request: ChatRequest): Call<ApiResponse<ChatResponse>>

    // ==================== WEATHER (PUBLIC) ====================
    @GET("weather")
    fun getPublicWeather(): Call<ApiResponse<WeatherResponse>>
    
    @GET("weather/city")
    fun getPublicWeatherByCity(
        @Query("city") city: String,
        @Query("country") country: String = "VN"
    ): Call<ApiResponse<WeatherResponse>>

    // ==================== LIVE CHAT ====================
    @POST("chat/conversations")
    fun startLiveConversation(@Body request: StartConversationRequest): Call<ApiResponse<LiveConversation>>
    
    @POST("chat/messages")
    fun sendLiveMessage(@Body request: SendLiveMessageRequest): Call<ApiResponse<LiveMessage>>
    
    @GET("chat/conversations/my")
    fun getMyConversations(): Call<ApiResponse<List<ConversationListItem>>>
    
    @GET("chat/conversations/{id}")
    fun getConversation(@Path("id") id: Long): Call<ApiResponse<LiveConversation>>
    
    @POST("chat/conversations/{id}/close")
    fun closeConversation(@Path("id") id: Long): Call<ApiResponse<LiveConversation>>
    
    // Manager Live Chat APIs
    @GET("chat/manager/conversations")
    fun getManagerConversations(): Call<ApiResponse<List<ConversationListItem>>>
    
    @GET("chat/manager/conversations/waiting-count")
    fun getWaitingConversationsCount(): Call<ApiResponse<Long>>

    // ==================== USER PROFILE ====================
    @GET("me")
    fun getMyProfile(): Call<ApiResponse<UserProfileDto>>

    @PUT("me")
    fun updateProfile(@Body request: UpdateProfileRequest): Call<ApiResponse<UserProfileDto>>

    @Multipart
    @POST("me/avatar")
    fun uploadAvatar(@Part image: MultipartBody.Part): Call<ApiResponse<UserProfileDto>>

    @PUT("me/change-password")
    fun changePassword(@Body request: ChangePasswordRequest): Call<String>

    @DELETE("me")
    fun deleteAccount(): Call<ApiResponse<String>>

    // ==================== AUTHENTICATION ====================
    @POST("auth/login")
    fun login(@Body request: LoginRequest): Call<ApiResponse<LoginResponse>>

    @POST("auth/refresh-token")
    fun refreshToken(@Body request: RefreshTokenRequest): Call<ApiResponse<JwtResponse>>

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

    @POST("manager/categories")
    fun createCategory(@Body categoryData: Map<String, String>): Call<ApiResponse<Category>>

    @PUT("manager/categories/{id}")
    fun updateCategory(@Path("id") id: Long, @Body categoryData: Map<String, String>): Call<ApiResponse<Category>>

    @DELETE("manager/categories/{id}")
    fun deleteCategory(@Path("id") id: Long): Call<ApiResponse<String>>

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
    
    @GET("stores/with-managers")
    fun getStoresWithManagers(): Call<ApiResponse<List<StoreWithManagers>>>

    @GET("stores/{id}")
    fun getStoreById(@Path("id") id: Int): Call<ApiResponse<Store>>
    
    @GET("stores/{id}/managers")
    fun getStoreManagers(@Path("id") id: Int): Call<ApiResponse<List<User>>>
    
    @GET("stores/admins")
    fun getAllAdmins(): Call<ApiResponse<List<User>>>

    @GET("stores/search")
    fun searchStores(@Query("keyword") keyword: String): Call<ApiResponse<List<Store>>>

    // getBranches() removed - backend doesn't have this endpoint
    // Use getStores() instead for store/branch selection

    // ==================== CART ====================
    // Backend sử dụng Authentication từ JWT token
    // Các endpoint vẫn hỗ trợ userId để backward compatible
    
    @POST("cart/add")
    fun addToCart(@Body request: AddToCartRequest): Call<ApiResponse<Cart>>
    
    // Lấy cart của user hiện tại từ JWT
    @GET("cart")
    fun getMyCart(): Call<ApiResponse<Cart>>

    // Lấy cart theo userId (backend vẫn verify quyền)
    @GET("cart/{userId}")
    fun getCart(@Path("userId") userId: Long): Call<ApiResponse<Cart>>

    // Cập nhật số lượng item trong cart
    @PUT("cart/items/{cartItemId}")
    fun updateCartItem(
        @Path("cartItemId") cartItemId: Long,
        @Query("quantity") quantity: Int
    ): Call<ApiResponse<Cart>>

    // Xóa item khỏi cart
    @DELETE("cart/items/{cartItemId}")
    fun removeCartItem(@Path("cartItemId") cartItemId: Long): Call<ApiResponse<Void>>
    
    // Xóa toàn bộ cart của user hiện tại
    @DELETE("cart/clear")
    fun clearMyCart(): Call<ApiResponse<Void>>

    // Xóa cart theo userId (backend vẫn verify quyền)
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

    // ==================== PROMOTIONS/VOUCHERS ====================
    @GET("promotions")
    fun getActivePromotions(): Call<ApiResponse<List<Voucher>>>
    
    @GET("promotions/{id}")
    fun getPromotionById(@Path("id") id: Long): Call<ApiResponse<Voucher>>
    
    @GET("promotions/validate")
    fun validatePromotion(
        @Query("code") code: String,
        @Query("orderAmount") orderAmount: Double?
    ): Call<ApiResponse<Voucher>>
    
    // Manager APIs for Vouchers
    @GET("promotions/manager/all")
    fun getAllPromotions(): Call<ApiResponse<List<Voucher>>>
    
    @POST("promotions/manager")
    fun createPromotion(
        @Body request: CreateVoucherRequest,
        @Query("sendNotification") sendNotification: Boolean
    ): Call<ApiResponse<Voucher>>
    
    @PUT("promotions/manager/{id}")
    fun updatePromotion(
        @Path("id") id: Long,
        @Body request: UpdateVoucherRequest
    ): Call<ApiResponse<Voucher>>
    
    @DELETE("promotions/manager/{id}")
    fun deletePromotion(@Path("id") id: Long): Call<ApiResponse<Void>>
    
    @PATCH("promotions/manager/{id}/toggle-status")
    fun togglePromotionStatus(@Path("id") id: Long): Call<ApiResponse<Voucher>>

    // ==================== MANAGER APIs ====================
    @GET("manager/summary")
    fun getDashboardSummary(): Call<ApiResponse<DashboardSummary>>
    
    @GET("manager/statistics/revenue")
    fun getRevenueStatistics(
        @Query("days") days: Int = 7,
        @Query("months") months: Int = 6
    ): Call<ApiResponse<RevenueStatistics>>

    // ==================== MANAGER FORECAST APIs ====================
    @GET("manager/forecast")
    fun getFullForecast(): Call<ApiResponse<ForecastDto>>
    
    @GET("manager/forecast/revenue")
    fun getRevenueForecast(): Call<ApiResponse<RevenueForecast>>
    
    @GET("manager/forecast/peak-hours")
    fun getPeakHours(): Call<ApiResponse<List<PeakHourAnalysis>>>
    
    @GET("manager/forecast/low-stock")
    fun getLowStockWarnings(): Call<ApiResponse<List<LowStockWarning>>>
    
    @GET("manager/forecast/staffing")
    fun getStaffingRecommendations(): Call<ApiResponse<List<StaffingRecommendation>>>
    
    @GET("manager/forecast/overload")
    fun getOverloadWarnings(): Call<ApiResponse<List<OverloadWarning>>>

    // ==================== MANAGER WEATHER APIs ====================
    @GET("manager/weather")
    fun getCurrentWeather(): Call<ApiResponse<WeatherResponse>>
    
    @GET("manager/weather/city")
    fun getWeatherByCity(
        @Query("city") city: String,
        @Query("country") country: String = "VN"
    ): Call<ApiResponse<WeatherResponse>>

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

    @DELETE("manager/users/{userId}")
    fun deleteUser(@Path("userId") userId: Int): Call<ApiResponse<String>>

    @PUT("manager/users/{userId}/promote")
    fun promoteToManager(@Path("userId") userId: Int): Call<ApiResponse<User>>

    @PUT("manager/users/{userId}/demote")
    fun demoteToUser(@Path("userId") userId: Int): Call<ApiResponse<User>>

    // ==================== MANAGER STORE ASSIGNMENT ====================
    @GET("manager/my-stores")
    fun getMyManagedStores(): Call<ApiResponse<List<Store>>>

    @GET("manager/users/{userId}/stores")
    fun getManagedStores(@Path("userId") userId: Int): Call<ApiResponse<List<Store>>>

    @POST("manager/users/{userId}/stores/{storeId}")
    fun assignStoreToManager(
        @Path("userId") userId: Int,
        @Path("storeId") storeId: Long
    ): Call<ApiResponse<User>>

    @PUT("manager/users/{userId}/stores")
    fun assignStoresToManager(
        @Path("userId") userId: Int,
        @Body storeIds: List<Long>
    ): Call<ApiResponse<User>>

    @DELETE("manager/users/{userId}/stores/{storeId}")
    fun removeStoreFromManager(
        @Path("userId") userId: Int,
        @Path("storeId") storeId: Long
    ): Call<ApiResponse<User>>

    @GET("manager/users/{userId}/has-stores")
    fun hasAssignedStores(@Path("userId") userId: Int): Call<ApiResponse<Boolean>>

    // ==================== ADMIN APIs ====================
    @POST("admin/drinks")
    fun createDrink(@Body drink: Drink): Call<ApiResponse<Drink>>

    @PUT("admin/drinks/{id}")
    fun updateDrink(@Path("id") id: Int, @Body drink: Drink): Call<ApiResponse<Drink>>

    @DELETE("admin/drinks/{id}")
    fun deleteDrink(@Path("id") id: Long): Call<ApiResponse<Void>>
    
    @Multipart
    @POST("admin/drinks/upload-image")
    fun uploadDrinkImage(
        @Part file: MultipartBody.Part,
        @Part("drinkName") drinkName: RequestBody
    ): Call<ApiResponse<Map<String, String>>>

    @GET("admin/categories")
    fun getAdminCategories(): Call<ApiResponse<List<Category>>>

    @POST("admin/categories")
    fun createCategory(@Body category: Category): Call<ApiResponse<Category>>

    @PUT("admin/categories/{id}")
    fun updateCategory(@Path("id") id: Int, @Body category: Category): Call<ApiResponse<Category>>

    @DELETE("admin/categories/{id}")
    fun deleteCategory(@Path("id") id: Int): Call<ApiResponse<Void>>

    // ==================== VNPAY PAYMENT ====================
    @POST("vnpay/create-payment")
    fun createVNPayPayment(@Body request: VNPayPaymentRequest): Call<ApiResponse<VNPayPaymentResponse>>
    
    @POST("vnpay/create-payment-amount")
    fun createVNPayPaymentWithAmount(
        @Query("amount") amount: Long,
        @Query("orderInfo") orderInfo: String
    ): Call<ApiResponse<VNPayPaymentResponse>>
    
    @POST("vnpay/create-order-after-payment")
    fun createOrderAfterPayment(@Body request: CreateOrderRequest): Call<ApiResponse<Order>>

    // ==================== REVIEWS ====================
    @POST("reviews")
    fun createReview(@Body request: CreateReviewRequest): Call<ApiResponse<Review>>
    
    @GET("reviews/drink/{drinkId}")
    fun getReviewsByDrink(@Path("drinkId") drinkId: Long): Call<ApiResponse<List<Review>>>
    
    @GET("reviews/drink/{drinkId}/summary")
    fun getDrinkRatingSummary(@Path("drinkId") drinkId: Long): Call<ApiResponse<DrinkRatingSummary>>
    
    @GET("reviews/my-reviews")
    fun getMyReviews(): Call<ApiResponse<List<Review>>>
    
    @GET("reviews/can-review/{orderItemId}")
    fun canReviewOrderItem(@Path("orderItemId") orderItemId: Long): Call<ApiResponse<Boolean>>
    
    @DELETE("reviews/{reviewId}")
    fun deleteReview(@Path("reviewId") reviewId: Long): Call<ApiResponse<String>>

    // ==================== MANAGER REVIEW MANAGEMENT ====================
    @GET("manager/reviews")
    fun getManagerReviews(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Call<ApiResponse<PageResponse<ReviewManagement>>>
    
    @GET("manager/reviews/drink/{drinkId}")
    fun getManagerReviewsByDrink(
        @Path("drinkId") drinkId: Long,
        @Query("includeBackup") includeBackup: Boolean = true,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Call<ApiResponse<PageResponse<ReviewManagement>>>
    
    @GET("manager/reviews/drink/{drinkId}/statistics")
    fun getReviewStatistics(@Path("drinkId") drinkId: Long): Call<ApiResponse<ReviewStatistics>>
    
    @GET("manager/reviews/backup")
    fun getBackupReviews(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Call<ApiResponse<PageResponse<ReviewManagement>>>
    
    @DELETE("manager/reviews/{reviewId}")
    fun deleteReviewByAdmin(@Path("reviewId") reviewId: Long): Call<ApiResponse<String>>

    // ==================== LOYALTY / SPIN WHEEL ====================
    @GET("loyalty/points")
    fun getUserPoints(): Call<ApiResponse<UserPointsDto>>
    
    @POST("loyalty/spin")
    fun spinWheel(): Call<ApiResponse<SpinWheelResponse>>
    
    @GET("loyalty/rewards")
    fun getAvailableRewards(): Call<ApiResponse<List<SpinRewardDto>>>
    
    @GET("loyalty/voucher/validate")
    fun validateSpinVoucher(@Query("code") code: String): Call<ApiResponse<SpinRewardDto>>
    
    // Member Tier Benefits
    @GET("loyalty/tier/benefits")
    fun getTierBenefits(): Call<ApiResponse<MemberTierBenefitsDto>>
    
    @POST("loyalty/tier/check-upgrade")
    fun checkTierUpgrade(): Call<ApiResponse<MemberTierBenefitsDto>>
    
    @GET("loyalty/tier/preview-discount")
    fun previewTierDiscount(@Query("orderTotal") orderTotal: Double): Call<ApiResponse<TierDiscountPreview>>

    // ==================== GROUP ORDER (ĐẶT HÀNG NHÓM) ====================
    @POST("group-orders")
    fun createGroupOrder(@Body request: CreateGroupOrderRequest): Call<ApiResponse<GroupOrderDto>>
    
    @POST("group-orders/join")
    fun joinGroupOrder(@Body request: JoinGroupOrderRequest): Call<ApiResponse<GroupOrderDto>>
    
    @GET("group-orders/{id}")
    fun getGroupOrder(@Path("id") id: Long): Call<ApiResponse<GroupOrderDto>>
    
    @GET("group-orders/code/{inviteCode}")
    fun getGroupOrderByCode(@Path("inviteCode") inviteCode: String): Call<ApiResponse<GroupOrderDto>>
    
    @GET("group-orders/active")
    fun getActiveGroupOrders(): Call<ApiResponse<List<GroupOrderDto>>>
    
    @GET("group-orders/my-orders")
    fun getMyGroupOrders(): Call<ApiResponse<List<GroupOrderDto>>>
    
    @PUT("group-orders/{id}")
    fun updateGroupOrder(
        @Path("id") id: Long,
        @Body request: UpdateGroupOrderRequest
    ): Call<ApiResponse<GroupOrderDto>>
    
    @POST("group-orders/{id}/items")
    fun addGroupOrderItem(
        @Path("id") id: Long,
        @Body request: AddGroupOrderItemRequest
    ): Call<ApiResponse<GroupOrderDto>>
    
    @PUT("group-orders/{id}/items/{itemId}")
    fun updateGroupOrderItem(
        @Path("id") id: Long,
        @Path("itemId") itemId: Long,
        @Body request: AddGroupOrderItemRequest
    ): Call<ApiResponse<GroupOrderDto>>
    
    @DELETE("group-orders/{id}/items/{itemId}")
    fun removeGroupOrderItem(
        @Path("id") id: Long,
        @Path("itemId") itemId: Long
    ): Call<ApiResponse<GroupOrderDto>>
    
    @POST("group-orders/{id}/lock")
    fun lockGroupOrder(@Path("id") id: Long): Call<ApiResponse<GroupOrderDto>>
    
    @POST("group-orders/{id}/unlock")
    fun unlockGroupOrder(@Path("id") id: Long): Call<ApiResponse<GroupOrderDto>>
    
    @POST("group-orders/{id}/leave")
    fun leaveGroupOrder(@Path("id") id: Long): Call<ApiResponse<GroupOrderDto>>
    
    @POST("group-orders/{id}/checkout")
    fun checkoutGroupOrder(
        @Path("id") id: Long,
        @Body request: CheckoutGroupOrderRequest
    ): Call<ApiResponse<Order>>
    
    @DELETE("group-orders/{id}")
    fun cancelGroupOrder(@Path("id") id: Long): Call<ApiResponse<Void>>

    // ==================== GROUP CHAT (CHAT NHÓM) ====================
    @POST("group-orders/{groupOrderId}/chat")
    fun sendGroupChatMessage(
        @Path("groupOrderId") groupOrderId: Long,
        @Body request: SendGroupChatRequest
    ): Call<ApiResponse<GroupChatMessageDto>>
    
    @GET("group-orders/{groupOrderId}/chat")
    fun getGroupChatHistory(
        @Path("groupOrderId") groupOrderId: Long
    ): Call<ApiResponse<List<GroupChatMessageDto>>>
    
    @GET("group-orders/{groupOrderId}/chat/recent")
    fun getRecentGroupChatMessages(
        @Path("groupOrderId") groupOrderId: Long,
        @Query("limit") limit: Int = 50
    ): Call<ApiResponse<List<GroupChatMessageDto>>>

    // ==================== PREDICTIVE ORDER (DỰ ĐOÁN MÓN) ====================
    @GET("predictive-order")
    fun getPredictiveOrder(
        @Query("weather") weather: String? = null
    ): Call<ApiResponse<PredictiveOrderResponse>>

    // ==================== 🛡️ USER MONITORING (GIÁM SÁT NGƯỜI DÙNG) ====================
    
    @GET("monitoring/dashboard")
    fun getMonitoringDashboard(): Call<ApiResponse<MonitoringDashboard>>
    
    @GET("monitoring/activities")
    fun getActivityLogs(
        @Query("userId") userId: Long? = null,
        @Query("activityType") activityType: String? = null,
        @Query("riskLevel") riskLevel: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Call<ApiResponse<PageResponse<UserActivityLog>>>
    
    @GET("monitoring/activities/user/{userId}")
    fun getUserActivityLogs(
        @Path("userId") userId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Call<ApiResponse<PageResponse<UserActivityLog>>>
    
    @GET("monitoring/alerts")
    fun getMonitoringAlerts(
        @Query("userId") userId: Long? = null,
        @Query("alertType") alertType: String? = null,
        @Query("severity") severity: String? = null,
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Call<ApiResponse<PageResponse<MonitoringAlert>>>
    
    @GET("monitoring/alerts/pending")
    fun getPendingAlerts(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Call<ApiResponse<PageResponse<MonitoringAlert>>>
    
    @PUT("monitoring/alerts/{alertId}/handle")
    fun handleAlert(
        @Path("alertId") alertId: Long,
        @Body request: HandleAlertRequest
    ): Call<ApiResponse<MonitoringAlert>>
    
    @GET("monitoring/risk-scores")
    fun getRiskScores(
        @Query("riskLevel") riskLevel: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Call<ApiResponse<PageResponse<UserRiskScore>>>
    
    @GET("monitoring/risk-scores/user/{userId}")
    fun getUserRiskScore(@Path("userId") userId: Long): Call<ApiResponse<UserRiskScore>>
    
    @POST("monitoring/risk-scores/user/{userId}/note")
    fun addAdminNote(
        @Path("userId") userId: Long,
        @Body request: Map<String, String>
    ): Call<ApiResponse<UserRiskScore>>
    
    @POST("monitoring/risk-scores/user/{userId}/reset")
    fun resetRiskScore(@Path("userId") userId: Long): Call<ApiResponse<UserRiskScore>>
    
    @POST("monitoring/users/{userId}/unblock")
    fun unblockUser(
        @Path("userId") userId: Long,
        @Body request: Map<String, String>
    ): Call<ApiResponse<String>>

    // ==================== 🚫 BLOCKED IP MANAGEMENT ====================
    
    @POST("blocked-ips/block")
    fun blockIP(@Body request: BlockIPRequest): Call<ApiResponse<BlockedIP>>
    
    @POST("blocked-ips/{id}/unblock")
    fun unblockIP(
        @Path("id") id: Long,
        @Body request: Map<String, String>?
    ): Call<ApiResponse<BlockedIP>>
    
    @GET("blocked-ips")
    fun getActiveBlockedIPs(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Call<ApiResponse<PageResponse<BlockedIP>>>
    
    @GET("blocked-ips/all")
    fun getAllBlockedIPs(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Call<ApiResponse<PageResponse<BlockedIP>>>
    
    @GET("blocked-ips/search")
    fun searchBlockedIP(@Query("ip") ip: String): Call<ApiResponse<List<BlockedIP>>>
    
    @GET("blocked-ips/statistics")
    fun getBlockedIPStatistics(): Call<ApiResponse<Map<String, Any>>>
    
    @GET("blocked-ips/check")
    fun checkIPStatus(@Query("ip") ip: String): Call<ApiResponse<Map<String, Any>>>

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
