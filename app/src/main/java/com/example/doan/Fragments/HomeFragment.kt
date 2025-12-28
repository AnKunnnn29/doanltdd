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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
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
import com.example.doan.Utils.SessionManager
import com.example.doan.Utils.VoiceOrderDialog
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
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
    private lateinit var deliveryCard: MaterialCardView
    private lateinit var pickupCard: MaterialCardView
    private lateinit var fabVoiceOrder: ExtendedFloatingActionButton
    private lateinit var fabChatbot: ExtendedFloatingActionButton
    private lateinit var fabSpinWheel: ExtendedFloatingActionButton
    private lateinit var fabGroupOrder: ExtendedFloatingActionButton

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

        return view
    }

    override fun onResume() {
        super.onResume()
        startAutoScroll()
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
    }

    private fun initViews(view: View) {
        userNameTextView = view.findViewById(R.id.user_name_home)
        greetingTextView = view.findViewById(R.id.greeting_text)
        avatarInitialTextView = view.findViewById(R.id.avatar_initial)
        avatarCard = view.findViewById(R.id.avatar_card)
        cartButton = view.findViewById(R.id.cart_button)
        cartBadge = view.findViewById(R.id.cart_badge)
        liveChatButton = view.findViewById(R.id.live_chat_button)
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

        // Update cart badge
        updateCartBadge()
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
        fabVoiceOrder.setOnClickListener {
            checkMicPermissionAndShowDialog()
        }
        
        fabChatbot.setOnClickListener {
            startActivity(Intent(context, ChatbotActivity::class.java))
        }
        
        fabSpinWheel.setOnClickListener {
            val sessionManager = SessionManager(requireContext())
            if (!sessionManager.isLoggedIn()) {
                Toast.makeText(context, "Vui lòng đăng nhập để tham gia vòng quay", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(context, SpinWheelActivity::class.java))
        }
        
        fabGroupOrder.setOnClickListener {
            val sessionManager = SessionManager(requireContext())
            if (!sessionManager.isLoggedIn()) {
                Toast.makeText(context, "Vui lòng đăng nhập để đặt hàng nhóm", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            checkActiveGroupOrderAndShow()
        }
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
