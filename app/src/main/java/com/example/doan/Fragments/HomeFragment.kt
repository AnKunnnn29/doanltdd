package com.example.doan.Fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.doan.Activities.AccountActivity
import com.example.doan.Activities.CartActivity
import com.example.doan.Activities.ChatbotActivity
import com.example.doan.Activities.CreateGroupOrderActivity
import com.example.doan.Activities.JoinGroupOrderActivity
import com.example.doan.Activities.LiveChatActivity
import com.example.doan.Activities.SpinWheelActivity
import com.example.doan.Adapters.BannerAdapter
import com.example.doan.Adapters.ProductCarouselAdapter
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.Drink
import com.example.doan.Models.Product
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.DataCache
import com.example.doan.Utils.InAppNotification
import com.example.doan.Utils.PredictiveOrderHelper
import com.example.doan.Utils.SeasonalEffectManager
import com.example.doan.Utils.SessionManager
import com.example.doan.Utils.SnowfallView
import com.example.doan.Utils.VoiceOrderDialog
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar
import kotlin.math.abs

class HomeFragment : Fragment() {

    private lateinit var bannerViewPager: ViewPager2
    private lateinit var indicatorLayout: LinearLayout
    private lateinit var bestSellerRecyclerView: RecyclerView
    private lateinit var forYouRecyclerView: RecyclerView
    private lateinit var userNameTextView: TextView
    private lateinit var greetingTextView: TextView
    private lateinit var avatarInitialTextView: TextView
    private lateinit var avatarCard: MaterialCardView
    private lateinit var cartButton: FrameLayout
    private lateinit var cartBadge: TextView
    private lateinit var liveChatButton: FrameLayout
    private lateinit var notificationButton: FrameLayout
    private lateinit var notificationBadge: TextView
    private lateinit var deliveryCard: MaterialCardView
    private lateinit var pickupCard: MaterialCardView
    private lateinit var fabVoiceOrder: com.google.android.material.card.MaterialCardView
    private lateinit var fabChatbot: com.google.android.material.card.MaterialCardView
    private lateinit var fabSpinWheel: com.google.android.material.card.MaterialCardView
    private lateinit var fabGroupOrder: com.google.android.material.card.MaterialCardView
    
    // Smart Suggestion Card
    private lateinit var cardSmartSuggestion: MaterialCardView
    private lateinit var imgSuggestionDrink: ImageView
    private lateinit var tvSuggestionMessage: TextView
    private lateinit var tvSuggestionDrinkName: TextView
    private lateinit var tvSuggestionReason: TextView
    private lateinit var btnAddSuggestion: com.google.android.material.button.MaterialButton
    private lateinit var btnCloseSuggestion: ImageView
    private var currentPredictedDrink: com.example.doan.Models.PredictedDrink? = null
    
    // Seasonal effects
    private var rootContainer: RelativeLayout? = null
    private var snowfallView: SnowfallView? = null

    private lateinit var bannerAdapter: BannerAdapter
    private lateinit var bestSellerAdapter: ProductCarouselAdapter
    private lateinit var forYouAdapter: ProductCarouselAdapter

    private val bannerImages = listOf(
        R.drawable.quangcao1,
        R.drawable.quangcao2,
        R.drawable.quangcao3
    )

    private val autoScrollHandler = Handler(Looper.getMainLooper())
    private val autoScrollRunnable = object : Runnable {
        override fun run() {
            if (::bannerViewPager.isInitialized && bannerAdapter.getRealCount() > 0) {
                val nextItem = bannerViewPager.currentItem + 1
                bannerViewPager.setCurrentItem(nextItem, true)
            }
            autoScrollHandler.postDelayed(this, AUTO_SCROLL_DELAY)
        }
    }
    
    // Permission launcher for microphone
    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showVoiceOrderDialog()
        } else {
            Toast.makeText(context, "Cần quyền microphone để sử dụng tính năng này", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        initViews(view)
        setupHeader()
        setupBannerCarousel()
        setupRecyclerViews()
        loadData()
        setupViewAllButtons(view)
        setupDeliveryPickupButtons()
        setupVoiceOrder()
        setupSeasonalEffects(view)

        return view
    }
    
    /**
     * Setup hiệu ứng theo mùa
     * Tự động chọn hiệu ứng phù hợp dựa trên thời điểm trong năm:
     * - ❄️ WINTER/CHRISTMAS/NEW_YEAR: Tuyết rơi
     * - 🌸 SPRING/TET: Hoa đào rơi
     * - ☀️ SUMMER: Bong bóng & ánh nắng
     * - 🍂 AUTUMN: Lá rơi
     * - 💕 VALENTINE: Trái tim bay
     * 
     * Hỗ trợ 2 chế độ: REALTIME (tự động) và CUSTOM (tự chọn)
     */
    private fun setupSeasonalEffects(view: View) {
        rootContainer = view as? RelativeLayout
        rootContainer?.let { container ->
            // Kiểm tra settings trước khi thêm hiệu ứng
            if (SeasonalEffectManager.isSeasonalEffectsEnabled(requireContext())) {
                // Thêm hiệu ứng theo mùa hiện tại (tự động chọn đúng loại dựa trên mode)
                val effectView = SeasonalEffectManager.addSeasonalEffect(container, autoStart = true)
                
                if (effectView != null) {
                    // Cập nhật greeting với emoji theo mùa đang active
                    val activeSeason = SeasonalEffectManager.getActiveSeason(requireContext())
                    val seasonalEmoji = SeasonalEffectManager.getSeasonalEmoji(activeSeason)
                    val currentGreeting = greetingTextView.text.toString()
                    if (!currentGreeting.contains(seasonalEmoji)) {
                        greetingTextView.text = "$seasonalEmoji ${getGreetingMessage()}"
                    }
                }
            }
            
            // Thêm confetti view (sẽ hiển thị khi đặt hàng thành công)
            SeasonalEffectManager.addConfettiEffect(container)
        }
    }

    override fun onResume() {
        super.onResume()
        startAutoScroll()
        updateCartBadge()
        updateNotificationBadge()
        
        // Kiểm tra và hiển thị gợi ý thông minh (Predictive Order)
        // Delay 1 giây để đảm bảo UI đã sẵn sàng
        Handler(Looper.getMainLooper()).postDelayed({
            if (isAdded && context != null) {
                checkPredictiveOrder()
            }
        }, 1000)
    }
    
    /**
     * Kiểm tra và hiển thị gợi ý món dự đoán khi mở app
     * Dựa trên lịch sử đặt hàng và thói quen của user
     * Hiển thị dưới dạng card trên Home thay vì dialog popup
     */
    private fun checkPredictiveOrder() {
        val sessionManager = SessionManager(requireContext())
        if (!sessionManager.isLoggedIn()) {
            Log.d("HomeFragment", "User not logged in, skipping predictive order")
            cardSmartSuggestion.visibility = View.GONE
            return
        }
        
        Log.d("HomeFragment", "=== CHECKING PREDICTIVE ORDER ===")
        Log.d("HomeFragment", "User ID: ${sessionManager.getUserId()}")
        
        // Gọi API lấy prediction
        RetrofitClient.getInstance(requireContext()).apiService.getPredictiveOrder(null)
            .enqueue(object : Callback<ApiResponse<com.example.doan.Models.PredictiveOrderResponse>> {
                override fun onResponse(
                    call: Call<ApiResponse<com.example.doan.Models.PredictiveOrderResponse>>,
                    response: Response<ApiResponse<com.example.doan.Models.PredictiveOrderResponse>>
                ) {
                    if (!isAdded || context == null) {
                        Log.d("HomeFragment", "Fragment not attached, skipping")
                        return
                    }
                    
                    Log.d("HomeFragment", "API Response code: ${response.code()}")
                    Log.d("HomeFragment", "API Response success: ${response.body()?.success}")
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        val prediction = response.body()?.data
                        Log.d("HomeFragment", "Prediction data: hasPrediction=${prediction?.hasPrediction}, message=${prediction?.message}")
                        Log.d("HomeFragment", "Predicted drink: ${prediction?.predictedDrink?.drinkName}")
                        
                        if (prediction != null && prediction.hasPrediction && prediction.predictedDrink != null) {
                            Log.d("HomeFragment", ">>> SHOWING SMART SUGGESTION CARD <<<")
                            showSmartSuggestionCard(prediction)
                        } else {
                            Log.d("HomeFragment", "No prediction available, hiding card")
                            cardSmartSuggestion.visibility = View.GONE
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("HomeFragment", "Prediction API error: ${response.code()}, body: $errorBody")
                        cardSmartSuggestion.visibility = View.GONE
                    }
                }
                
                override fun onFailure(call: Call<ApiResponse<com.example.doan.Models.PredictiveOrderResponse>>, t: Throwable) {
                    Log.e("HomeFragment", "Prediction API failed: ${t.message}", t)
                    if (isAdded) {
                        cardSmartSuggestion.visibility = View.GONE
                    }
                }
            })
    }
    
    /**
     * Hiển thị card gợi ý thông minh trên Home
     */
    private fun showSmartSuggestionCard(prediction: com.example.doan.Models.PredictiveOrderResponse) {
        val drink = prediction.predictedDrink ?: return
        currentPredictedDrink = drink
        
        Log.d("HomeFragment", "showSmartSuggestionCard: ${drink.drinkName}")
        
        // Hiển thị card với animation
        cardSmartSuggestion.visibility = View.VISIBLE
        cardSmartSuggestion.alpha = 0f
        cardSmartSuggestion.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
        
        // Set message
        tvSuggestionMessage.text = prediction.message ?: "Có phải bạn muốn gọi lại..."
        
        // Set drink name
        tvSuggestionDrinkName.text = drink.drinkName ?: "Sản phẩm"
        
        // Set reason (lấy reason đầu tiên)
        val reason = prediction.triggerReasons?.firstOrNull() ?: ""
        tvSuggestionReason.text = reason
        tvSuggestionReason.visibility = if (reason.isNotEmpty()) View.VISIBLE else View.GONE
        
        // Load ảnh sản phẩm
        Log.d("HomeFragment", "Loading drink image: ${drink.drinkImage}")
        if (!drink.drinkImage.isNullOrEmpty()) {
            Glide.with(this)
                .load(drink.drinkImage)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_broken_image)
                .centerCrop()
                .into(imgSuggestionDrink)
        }
        
        // Nút thêm vào giỏ
        btnAddSuggestion.setOnClickListener {
            currentPredictedDrink?.let { predictedDrink ->
                addPredictedDrinkToCart(predictedDrink)
                // Ẩn card sau khi thêm
                hideSmartSuggestionCard()
            }
        }
        
        // Nút đóng
        btnCloseSuggestion.setOnClickListener {
            hideSmartSuggestionCard()
        }
    }
    
    /**
     * Ẩn card gợi ý với animation
     */
    private fun hideSmartSuggestionCard() {
        cardSmartSuggestion.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                cardSmartSuggestion.visibility = View.GONE
            }
            .start()
        currentPredictedDrink = null
    }
    
    /**
     * Thêm món được gợi ý vào giỏ hàng
     */
    private fun addPredictedDrinkToCart(drink: com.example.doan.Models.PredictedDrink) {
        val sessionManager = SessionManager(requireContext())
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(context, "Vui lòng đăng nhập để thêm vào giỏ hàng", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Lấy sizeId từ prediction
        var sizeId = drink.sizeId ?: 0L
        Log.d("HomeFragment", "PredictedDrink: drinkId=${drink.drinkId}, sizeId=$sizeId, sizeName=${drink.sizeName}")
        
        // Nếu sizeId không hợp lệ, tìm từ cache hoặc gọi API lấy drink detail
        if (sizeId <= 0L) {
            // Thử tìm trong cache trước
            val cachedProduct = DataCache.products?.find { it.id.toLong() == drink.drinkId }
            if (cachedProduct != null && !cachedProduct.sizes.isNullOrEmpty()) {
                // Nếu có sizeName từ prediction, tìm size khớp
                val matchingSize = if (!drink.sizeName.isNullOrEmpty()) {
                    cachedProduct.sizes?.find { it.sizeName.equals(drink.sizeName, ignoreCase = true) }
                } else null
                
                sizeId = matchingSize?.id?.toLong() ?: cachedProduct.sizes?.firstOrNull()?.id?.toLong() ?: 0L
                Log.d("HomeFragment", "Found sizeId from cache: $sizeId for drink: ${drink.drinkName}")
            }
            
            // Nếu vẫn không có sizeId, gọi API lấy drink detail
            if (sizeId <= 0L) {
                Log.d("HomeFragment", "Fetching drink detail to get sizeId for drinkId: ${drink.drinkId}")
                fetchDrinkAndAddToCart(drink)
                return
            }
        }
        
        // Có sizeId hợp lệ, thêm vào giỏ hàng
        addToCartWithSize(drink, sizeId)
    }
    
    /**
     * Gọi API lấy drink detail rồi thêm vào giỏ hàng
     */
    private fun fetchDrinkAndAddToCart(drink: com.example.doan.Models.PredictedDrink) {
        RetrofitClient.getInstance(requireContext()).apiService.getDrinkById(drink.drinkId.toInt())
            .enqueue(object : Callback<ApiResponse<Drink>> {
                override fun onResponse(
                    call: Call<ApiResponse<Drink>>,
                    response: Response<ApiResponse<Drink>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val drinkDetail = response.body()?.data
                        if (drinkDetail != null && !drinkDetail.sizes.isNullOrEmpty()) {
                            // Tìm size khớp với sizeName từ prediction
                            val matchingSize = if (!drink.sizeName.isNullOrEmpty()) {
                                drinkDetail.sizes?.find { it.sizeName.equals(drink.sizeName, ignoreCase = true) }
                            } else null
                            
                            val sizeId = matchingSize?.id?.toLong() ?: drinkDetail.sizes?.firstOrNull()?.id?.toLong() ?: 0L
                            Log.d("HomeFragment", "Got sizeId from API: $sizeId for drink: ${drink.drinkName}")
                            
                            if (sizeId > 0L) {
                                addToCartWithSize(drink, sizeId)
                            } else {
                                Toast.makeText(context, "Không tìm thấy size cho sản phẩm này", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            // Drink không có sizes, thêm với sizeId = 0 (backend sẽ xử lý)
                            addToCartWithSize(drink, 0L)
                        }
                    } else {
                        Log.e("HomeFragment", "Failed to get drink detail: ${response.code()}")
                        Toast.makeText(context, "Không thể lấy thông tin sản phẩm", Toast.LENGTH_SHORT).show()
                    }
                }
                
                override fun onFailure(call: Call<ApiResponse<Drink>>, t: Throwable) {
                    Log.e("HomeFragment", "Error fetching drink detail", t)
                    Toast.makeText(context, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                }
            })
    }
    
    /**
     * Thêm vào giỏ hàng với sizeId đã xác định
     */
    private fun addToCartWithSize(drink: com.example.doan.Models.PredictedDrink, sizeId: Long) {
        val request = com.example.doan.Models.AddToCartRequest(
            drinkId = drink.drinkId,
            sizeId = sizeId,
            quantity = 1,
            toppingIds = drink.toppings?.mapNotNull { if (it.toppingId > 0) it.toppingId else null } ?: emptyList(),
            note = drink.note ?: ""
        )
        
        Log.d("HomeFragment", "Adding to cart: drinkId=${request.drinkId}, sizeId=${request.sizeId}")
        
        RetrofitClient.getInstance(requireContext()).apiService.addToCart(request)
            .enqueue(object : Callback<ApiResponse<com.example.doan.Models.Cart>> {
                override fun onResponse(
                    call: Call<ApiResponse<com.example.doan.Models.Cart>>,
                    response: Response<ApiResponse<com.example.doan.Models.Cart>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        // Sử dụng InAppNotification thay vì Toast
                        activity?.let { act ->
                            InAppNotification.cartAdded(act, drink.drinkName ?: "Sản phẩm")
                        }
                        
                        // Update cart badge
                        val cartItems = response.body()?.data?.items?.size ?: 0
                        DataCache.cartItemCount = cartItems
                        updateCartBadge()
                    } else {
                        val errorMsg = response.body()?.message ?: "Không thể thêm vào giỏ hàng"
                        val errorBody = response.errorBody()?.string()
                        Log.e("HomeFragment", "Add to cart failed: $errorMsg, code: ${response.code()}, body: $errorBody")
                        activity?.let { act ->
                            InAppNotification.error(act, "Lỗi", errorMsg)
                        }
                    }
                }
                
                override fun onFailure(call: Call<ApiResponse<com.example.doan.Models.Cart>>, t: Throwable) {
                    Log.e("HomeFragment", "Error adding predicted drink to cart", t)
                    Toast.makeText(context, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                }
            })
    }

    override fun onPause() {
        super.onPause()
        stopAutoScroll()
    }
    
    /**
     * FIX Low #17: Remove callbacks trong onDestroyView để tránh memory leak
     */
    override fun onDestroyView() {
        super.onDestroyView()
        stopAutoScroll()
        // Cleanup seasonal effects
        SeasonalEffectManager.cleanup()
        snowfallView = null
        rootContainer = null
    }

    private fun initViews(view: View) {
        userNameTextView = view.findViewById(R.id.user_name_home)
        greetingTextView = view.findViewById(R.id.greeting_text)
        avatarInitialTextView = view.findViewById(R.id.avatar_initial)
        avatarCard = view.findViewById(R.id.avatar_card)
        cartButton = view.findViewById(R.id.cart_button)
        cartBadge = view.findViewById(R.id.cart_badge)
        liveChatButton = view.findViewById(R.id.live_chat_button)
        notificationButton = view.findViewById(R.id.notification_button)
        notificationBadge = view.findViewById(R.id.notification_badge)
        bannerViewPager = view.findViewById(R.id.banner_viewpager)
        indicatorLayout = view.findViewById(R.id.indicator_layout)
        bestSellerRecyclerView = view.findViewById(R.id.best_seller_recycler_view)
        forYouRecyclerView = view.findViewById(R.id.for_you_recycler_view)
        deliveryCard = view.findViewById(R.id.delivery_card)
        pickupCard = view.findViewById(R.id.pickup_card)
        fabVoiceOrder = view.findViewById(R.id.fab_voice_order)
        fabChatbot = view.findViewById(R.id.fab_chatbot)
        fabSpinWheel = view.findViewById(R.id.fab_spin_wheel)
        fabGroupOrder = view.findViewById(R.id.fab_group_order)
        
        // Smart Suggestion Card
        cardSmartSuggestion = view.findViewById(R.id.card_smart_suggestion)
        imgSuggestionDrink = view.findViewById(R.id.img_suggestion_drink)
        tvSuggestionMessage = view.findViewById(R.id.tv_suggestion_message)
        tvSuggestionDrinkName = view.findViewById(R.id.tv_suggestion_drink_name)
        tvSuggestionReason = view.findViewById(R.id.tv_suggestion_reason)
        btnAddSuggestion = view.findViewById(R.id.btn_add_suggestion)
        btnCloseSuggestion = view.findViewById(R.id.btn_close_suggestion)
    }

    private fun setupHeader() {
        val sessionManager = SessionManager(requireContext())
        val fullName = sessionManager.getFullName()

        // Set greeting based on time of day
        greetingTextView.text = getGreetingMessage()

        // Set user name
        val displayName = if (sessionManager.isLoggedIn() && !fullName.isNullOrEmpty()) {
            fullName
        } else {
            "Khách"
        }
        userNameTextView.text = displayName

        // Set avatar initial (first letter of name)
        avatarInitialTextView.text = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "K"

        // Avatar click -> Account
        avatarCard.setOnClickListener {
            startActivity(Intent(context, AccountActivity::class.java))
        }

        // Cart button click
        cartButton.setOnClickListener {
            startActivity(Intent(context, CartActivity::class.java))
        }

        // Live Chat button click
        liveChatButton.setOnClickListener {
            startActivity(Intent(context, LiveChatActivity::class.java))
        }

        // Notification button click
        notificationButton.setOnClickListener {
            startActivity(Intent(context, com.example.doan.Activities.NotificationActivity::class.java))
        }

        // Update cart badge
        updateCartBadge()
        
        // Update notification badge
        updateNotificationBadge()
    }

    private fun getGreetingMessage(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Chào buổi sáng ☀️"
            hour < 18 -> "Chào buổi chiều 🌤️"
            else -> "Chào buổi tối 🌙"
        }
    }

    private fun updateCartBadge() {
        val cartCount = DataCache.cartItemCount ?: 0
        if (cartCount > 0) {
            cartBadge.visibility = View.VISIBLE
            cartBadge.text = if (cartCount > 99) "99+" else cartCount.toString()
        } else {
            cartBadge.visibility = View.GONE
        }
    }

    /**
     * Cập nhật badge số thông báo chưa đọc
     */
    private fun updateNotificationBadge() {
        val sessionManager = SessionManager(requireContext())
        if (!sessionManager.isLoggedIn()) {
            notificationBadge.visibility = View.GONE
            return
        }
        
        RetrofitClient.getInstance(requireContext()).apiService.getUnreadNotificationCount()
            .enqueue(object : Callback<ApiResponse<Map<String, Long>>> {
                override fun onResponse(
                    call: Call<ApiResponse<Map<String, Long>>>,
                    response: Response<ApiResponse<Map<String, Long>>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val count = response.body()?.data?.get("unreadCount") ?: 0L
                        if (count > 0) {
                            notificationBadge.visibility = View.VISIBLE
                            notificationBadge.text = if (count > 99) "99+" else count.toString()
                        } else {
                            notificationBadge.visibility = View.GONE
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Map<String, Long>>>, t: Throwable) {
                    // Ignore error, just hide badge
                    notificationBadge.visibility = View.GONE
                }
            })
    }

    private fun setupBannerCarousel() {
        bannerAdapter = BannerAdapter(bannerImages) { position ->
            // Handle banner click
            Toast.makeText(context, "Banner ${position + 1} clicked", Toast.LENGTH_SHORT).show()
        }

        // Tính chiều cao ViewPager2 dựa trên tỷ lệ ảnh gốc
        val drawable = ContextCompat.getDrawable(requireContext(), bannerImages[0])
        drawable?.let {
            val imageWidth = it.intrinsicWidth
            val imageHeight = it.intrinsicHeight
            val screenWidth = resources.displayMetrics.widthPixels - (32.dpToPx()) // trừ margin 16dp * 2
            val calculatedHeight = (screenWidth.toFloat() / imageWidth * imageHeight).toInt()
            
            val params = bannerViewPager.layoutParams
            params.height = calculatedHeight
            bannerViewPager.layoutParams = params
        }

        bannerViewPager.apply {
            adapter = bannerAdapter
            offscreenPageLimit = 3
            clipToPadding = false
            clipChildren = false
            getChildAt(0)?.overScrollMode = RecyclerView.OVER_SCROLL_NEVER

            // Set initial position to middle for infinite scroll effect
            post {
                setCurrentItem(bannerImages.size * 100, false)
            }

            // Page transformer for zoom effect
            val transformer = CompositePageTransformer()
            transformer.addTransformer(MarginPageTransformer(16))
            transformer.addTransformer { page, position ->
                val scale = 1 - abs(position) * 0.05f
                page.scaleY = scale
            }
            setPageTransformer(transformer)

            // Page change callback
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    updateIndicators(position % bannerImages.size)
                }

                override fun onPageScrollStateChanged(state: Int) {
                    super.onPageScrollStateChanged(state)
                    when (state) {
                        ViewPager2.SCROLL_STATE_DRAGGING -> stopAutoScroll()
                        ViewPager2.SCROLL_STATE_IDLE -> startAutoScroll()
                    }
                }
            })
        }

        setupIndicators()
    }

    private fun setupIndicators() {
        indicatorLayout.removeAllViews()
        
        for (i in bannerImages.indices) {
            val dot = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    if (i == 0) 24.dpToPx() else 8.dpToPx(),
                    8.dpToPx()
                ).apply {
                    marginStart = 3.dpToPx()
                    marginEnd = 3.dpToPx()
                }
                setBackgroundResource(
                    if (i == 0) R.drawable.indicator_dot_selected 
                    else R.drawable.indicator_dot_unselected
                )
            }
            indicatorLayout.addView(dot)
        }
    }

    private fun updateIndicators(selectedPosition: Int) {
        for (i in 0 until indicatorLayout.childCount) {
            val dot = indicatorLayout.getChildAt(i)
            val isSelected = i == selectedPosition
            
            // Animate width change
            val targetWidth = if (isSelected) 24.dpToPx() else 8.dpToPx()
            val currentWidth = dot.layoutParams.width
            
            if (currentWidth != targetWidth) {
                val animator = android.animation.ValueAnimator.ofInt(currentWidth, targetWidth)
                animator.duration = 200
                animator.addUpdateListener { animation ->
                    val params = dot.layoutParams
                    params.width = animation.animatedValue as Int
                    dot.layoutParams = params
                }
                animator.start()
            }
            
            dot.setBackgroundResource(
                if (isSelected) R.drawable.indicator_dot_selected 
                else R.drawable.indicator_dot_unselected
            )
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun startAutoScroll() {
        autoScrollHandler.removeCallbacks(autoScrollRunnable)
        autoScrollHandler.postDelayed(autoScrollRunnable, AUTO_SCROLL_DELAY)
    }

    private fun stopAutoScroll() {
        autoScrollHandler.removeCallbacks(autoScrollRunnable)
    }

    private fun setupRecyclerViews() {
        val snapHelper = LinearSnapHelper()
        
        val carouselScrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                applyCarouselEffect(recyclerView)
            }
        }

        bestSellerRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        bestSellerAdapter = ProductCarouselAdapter(emptyList())
        bestSellerRecyclerView.adapter = bestSellerAdapter
        bestSellerRecyclerView.addOnScrollListener(carouselScrollListener)

        forYouRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        forYouAdapter = ProductCarouselAdapter(emptyList())
        forYouRecyclerView.adapter = forYouAdapter
        forYouRecyclerView.addOnScrollListener(carouselScrollListener)
    }

    private fun applyCarouselEffect(recyclerView: RecyclerView) {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        val center = recyclerView.width / 2f

        for (i in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(i)
            val childCenter = (layoutManager.getDecoratedLeft(child) + layoutManager.getDecoratedRight(child)) / 2f
            val distance = abs(center - childCenter)
            val scale = 1f - 0.15f * (distance / center)
            child.scaleX = scale
            child.scaleY = scale
        }
    }

    private fun loadData() {
        if (DataCache.products.isNullOrEmpty()) {
            loadAllProducts()
        } else {
            displayProductsFromCache()
        }
    }

    private fun setupDeliveryPickupButtons() {
        deliveryCard.setOnClickListener {
            navigateToMenuWithOrderType("delivery")
        }

        pickupCard.setOnClickListener {
            navigateToMenuWithOrderType("pickup")
        }
    }

    private fun navigateToMenuWithOrderType(orderType: String) {
        val prefs = requireContext().getSharedPreferences("UTETeaPrefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("orderType", orderType).apply()
        
        Log.d("HomeFragment", "Saved orderType to SharedPreferences: $orderType")
        
        val menuFragment = MenuFragment().apply {
            arguments = Bundle().apply {
                putString("orderType", orderType)
            }
        }
        
        parentFragmentManager.beginTransaction().apply {
            replace(R.id.content_container, menuFragment)
            commit()
        }
        
        val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav?.selectedItemId = R.id.nav_order
    }

    private fun displayProductsFromCache() {
        val allProducts = DataCache.products ?: return
        if (allProducts.isNotEmpty()) {
            val bestSellerProducts = allProducts.shuffled().take(10)
            val forYouProducts = allProducts.shuffled().take(10)

            bestSellerAdapter = ProductCarouselAdapter(bestSellerProducts)
            bestSellerRecyclerView.adapter = bestSellerAdapter

            forYouAdapter = ProductCarouselAdapter(forYouProducts)
            forYouRecyclerView.adapter = forYouAdapter
        }
    }

    private fun loadAllProducts() {
        RetrofitClient.getInstance(requireContext()).apiService.getDrinks()
            .enqueue(object : Callback<ApiResponse<List<Drink>>> {
                override fun onResponse(call: Call<ApiResponse<List<Drink>>>, response: Response<ApiResponse<List<Drink>>>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val drinks = response.body()?.data ?: emptyList()
                        val baseUrl = RetrofitClient.getBaseUrl()
                        val rootUrl = baseUrl.replace("/api/", "").removeSuffix("/")

                        val products = drinks.map { drink ->
                            var imageUrl = drink.imageUrl
                            if (imageUrl != null && !imageUrl.startsWith("http")) {
                                if (!imageUrl.startsWith("/")) imageUrl = "/$imageUrl"
                                imageUrl = rootUrl + imageUrl
                            }
                            Product(
                                id = drink.id,
                                name = drink.name,
                                description = drink.description ?: "",
                                price = drink.basePrice,
                                category = drink.categoryName ?: "",
                                categoryId = drink.categoryId,
                                imageUrl = imageUrl,
                                isAvailable = drink.isActive
                            ).apply {
                                sizes = drink.sizes
                                toppings = drink.toppings
                            }
                        }
                        DataCache.products = products
                        displayProductsFromCache()
                    } else {
                        Toast.makeText(context, "Không thể tải sản phẩm.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<Drink>>>, t: Throwable) {
                    Toast.makeText(context, "Lỗi mạng: Không thể tải sản phẩm.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun setupViewAllButtons(view: View) {
        val viewAllBestSeller = view.findViewById<TextView>(R.id.tv_view_all_best_seller)
        val viewAllForYou = view.findViewById<TextView>(R.id.tv_view_all_for_you)

        val clickListener = View.OnClickListener {
            val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
            bottomNav?.selectedItemId = R.id.nav_order
        }

        viewAllBestSeller.setOnClickListener(clickListener)
        viewAllForYou.setOnClickListener(clickListener)
    }

    companion object {
        private const val AUTO_SCROLL_DELAY = 4000L // 4 seconds
    }
    
    // ==================== Voice Order ====================
    
    private fun setupVoiceOrder() {
        // Animate buttons khi xuất hiện
        animateQuickActionsOnStart()
        
        fabVoiceOrder.setOnClickListener {
            animateButtonClick(it)
            checkMicPermissionAndShowDialog()
        }
        
        fabChatbot.setOnClickListener {
            animateButtonClick(it)
            startActivity(Intent(context, ChatbotActivity::class.java))
        }
        
        fabSpinWheel.setOnClickListener {
            animateButtonClick(it)
            val sessionManager = SessionManager(requireContext())
            if (!sessionManager.isLoggedIn()) {
                Toast.makeText(context, "Vui lòng đăng nhập để tham gia vòng quay", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(context, SpinWheelActivity::class.java))
        }
        
        fabGroupOrder.setOnClickListener {
            animateButtonClick(it)
            val sessionManager = SessionManager(requireContext())
            if (!sessionManager.isLoggedIn()) {
                Toast.makeText(context, "Vui lòng đăng nhập để đặt hàng nhóm", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            checkActiveGroupOrderAndShow()
        }
    }
    
    /**
     * Animation bounce-in cho 4 quick action buttons khi mở app
     */
    private fun animateQuickActionsOnStart() {
        val bounceAnim = android.view.animation.AnimationUtils.loadAnimation(context, R.anim.bounce_in)
        
        // Delay khác nhau cho mỗi button để tạo hiệu ứng stagger
        fabVoiceOrder.postDelayed({
            fabVoiceOrder.startAnimation(bounceAnim)
        }, 100)
        
        fabChatbot.postDelayed({
            fabChatbot.startAnimation(android.view.animation.AnimationUtils.loadAnimation(context, R.anim.bounce_in))
        }, 200)
        
        fabSpinWheel.postDelayed({
            fabSpinWheel.startAnimation(android.view.animation.AnimationUtils.loadAnimation(context, R.anim.bounce_in))
        }, 300)
        
        fabGroupOrder.postDelayed({
            fabGroupOrder.startAnimation(android.view.animation.AnimationUtils.loadAnimation(context, R.anim.bounce_in))
        }, 400)
    }
    
    /**
     * Animation pulse khi click button
     */
    private fun animateButtonClick(view: View) {
        val pulseAnim = android.view.animation.AnimationUtils.loadAnimation(context, R.anim.pulse)
        view.startAnimation(pulseAnim)
    }
    
    private fun checkActiveGroupOrderAndShow() {
        // Kiểm tra xem user có phiên đang hoạt động không
        RetrofitClient.getInstance(requireContext()).apiService.getActiveGroupOrders()
            .enqueue(object : Callback<ApiResponse<List<com.example.doan.Models.GroupOrderDto>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<com.example.doan.Models.GroupOrderDto>>>,
                    response: Response<ApiResponse<List<com.example.doan.Models.GroupOrderDto>>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val activeOrders = response.body()?.data ?: emptyList()
                        if (activeOrders.isNotEmpty()) {
                            // Có phiên đang hoạt động, mở trực tiếp
                            val activeOrder = activeOrders.first()
                            val intent = Intent(context, com.example.doan.Activities.GroupOrderActivity::class.java)
                            intent.putExtra("GROUP_ORDER_ID", activeOrder.id)
                            startActivity(intent)
                        } else {
                            // Không có phiên nào, hiện dialog chọn
                            showGroupOrderOptionsDialog()
                        }
                    } else {
                        // Lỗi API, vẫn hiện dialog
                        showGroupOrderOptionsDialog()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<com.example.doan.Models.GroupOrderDto>>>, t: Throwable) {
                    Log.e("HomeFragment", "Error checking active group orders", t)
                    // Lỗi kết nối, vẫn hiện dialog
                    showGroupOrderOptionsDialog()
                }
            })
    }
    
    private fun showGroupOrderOptionsDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_group_order_options, null)
        
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        dialogView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_create_new)
            .setOnClickListener {
                dialog.dismiss()
                startActivity(Intent(context, CreateGroupOrderActivity::class.java))
            }
        
        dialogView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_join)
            .setOnClickListener {
                dialog.dismiss()
                startActivity(Intent(context, JoinGroupOrderActivity::class.java))
            }
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }
    
    private fun checkMicPermissionAndShowDialog() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                showVoiceOrderDialog()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                Toast.makeText(
                    context,
                    "Cần quyền microphone để đặt hàng bằng giọng nói",
                    Toast.LENGTH_LONG
                ).show()
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            else -> {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
    
    private fun showVoiceOrderDialog() {
        // Đảm bảo đã load products
        if (DataCache.products.isNullOrEmpty()) {
            Toast.makeText(context, "Đang tải sản phẩm, vui lòng thử lại...", Toast.LENGTH_SHORT).show()
            loadAllProducts()
            return
        }
        
        VoiceOrderDialog(requireContext()) { product, quantity, sizeName ->
            // Callback khi user confirm order
            addToCartFromVoice(product, quantity, sizeName)
        }.show()
    }
    
    private fun addToCartFromVoice(product: Product, quantity: Int, sizeName: String) {
        val sessionManager = SessionManager(requireContext())
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(context, "Vui lòng đăng nhập để thêm vào giỏ hàng", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Tìm sizeId từ sizeName - default to 0 if not found
        val sizeId = product.sizes?.find { it.sizeName == sizeName }?.id?.toLong() ?: 0L
        
        val request = com.example.doan.Models.AddToCartRequest(
            drinkId = product.id.toLong(),
            sizeId = sizeId,
            quantity = quantity,
            toppingIds = emptyList(),
            note = ""
        )
        
        // API addToCart không cần userId - lấy từ JWT token
        RetrofitClient.getInstance(requireContext()).apiService.addToCart(request)
            .enqueue(object : Callback<ApiResponse<com.example.doan.Models.Cart>> {
                override fun onResponse(
                    call: Call<ApiResponse<com.example.doan.Models.Cart>>,
                    response: Response<ApiResponse<com.example.doan.Models.Cart>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(
                            context,
                            "Da them $quantity ${product.name} (Size $sizeName) vao gio!",
                            Toast.LENGTH_LONG
                        ).show()
                        
                        // Update cart badge
                        val cartItems = response.body()?.data?.items?.size ?: 0
                        DataCache.cartItemCount = cartItems
                        updateCartBadge()
                    } else {
                        Toast.makeText(context, "Khong the them vao gio hang", Toast.LENGTH_SHORT).show()
                    }
                }
                
                override fun onFailure(call: Call<ApiResponse<com.example.doan.Models.Cart>>, t: Throwable) {
                    Log.e("HomeFragment", "Error adding to cart", t)
                    Toast.makeText(context, "Loi ket noi", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
