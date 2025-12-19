package com.example.doan.Fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Activities.AccountActivity
import com.example.doan.Adapters.ProductCarouselAdapter
import com.example.doan.Adapters.PromotionAdapter
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.Drink
import com.example.doan.Models.Product
import com.example.doan.Models.Promotion
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.DataCache
import com.example.doan.Utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeFragment : Fragment() {

    private lateinit var promotionsRecyclerView: RecyclerView
    private lateinit var bestSellerRecyclerView: RecyclerView
    private lateinit var forYouRecyclerView: RecyclerView
    private lateinit var userNameTextView: TextView
    private lateinit var profileImageView: ImageView
    private lateinit var deliveryCard: MaterialCardView
    private lateinit var pickupCard: MaterialCardView

    private lateinit var promotionAdapter: PromotionAdapter
    private lateinit var bestSellerAdapter: ProductCarouselAdapter
    private lateinit var forYouAdapter: ProductCarouselAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        initViews(view)
        setupHeader()
        setupRecyclerViews()
        loadData()
        setupViewAllButtons(view)
        setupDeliveryPickupButtons()

        return view
    }

    private fun initViews(view: View) {
        userNameTextView = view.findViewById(R.id.user_name_home)
        profileImageView = view.findViewById(R.id.profile_image_home)
        promotionsRecyclerView = view.findViewById(R.id.promotions_recycler_view)
        bestSellerRecyclerView = view.findViewById(R.id.best_seller_recycler_view)
        forYouRecyclerView = view.findViewById(R.id.for_you_recycler_view)
        deliveryCard = view.findViewById(R.id.delivery_card)
        pickupCard = view.findViewById(R.id.pickup_card)
    }

    private fun setupHeader() {
        val sessionManager = SessionManager(requireContext())
        val fullName = sessionManager.getFullName()

        userNameTextView.text = if (sessionManager.isLoggedIn() && !fullName.isNullOrEmpty()) {
            fullName
        } else {
            "Guest"
        }

        profileImageView.setOnClickListener {
            startActivity(Intent(context, AccountActivity::class.java))
        }
    }

    private fun setupRecyclerViews() {
        // Promotions
        promotionsRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        promotionAdapter = PromotionAdapter(emptyList())
        promotionsRecyclerView.adapter = promotionAdapter

        // Best Seller & For You
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(bestSellerRecyclerView)
        snapHelper.attachToRecyclerView(forYouRecyclerView)

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
            val distance = kotlin.math.abs(center - childCenter)

            val scale = 1f - 0.15f * (distance / center)

            child.scaleX = scale
            child.scaleY = scale
        }
    }

    private fun loadData() {
        // Tải dữ liệu cho mục "Ưu đãi cho bạn" (dữ liệu mẫu).
        val promotions = listOf(
            Promotion("Giảm 30% cho đơn hàng đầu tiên", "@drawable/banners"),
            Promotion("Mua 1 tặng 1 trà sữa bất kỳ", "@drawable/banners2"),
            Promotion("Miễn phí vận chuyển cho đơn hàng từ 100k", "@drawable/banners3")
        )
        promotionAdapter.updateData(promotions) // Cập nhật dữ liệu cho adapter

        // SỬA LỖI: Gọi API để tải dữ liệu sản phẩm nếu cache trống.
        if (DataCache.products.isNullOrEmpty()) {
            loadAllProducts()
        } else {
            // Nếu cache đã có dữ liệu, hiển thị ngay.
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
        // Lưu orderType vào SharedPreferences
        val prefs = requireContext().getSharedPreferences("UTETeaPrefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("orderType", orderType).apply()
        
        Log.d("HomeFragment", "Saved orderType to SharedPreferences: $orderType")
        
        // Tạo MenuFragment mới với orderType
        val menuFragment = MenuFragment().apply {
            arguments = Bundle().apply {
                putString("orderType", orderType)
            }
        }
        
        // Thay thế fragment và cập nhật BottomNavigationView
        parentFragmentManager.beginTransaction().apply {
            replace(R.id.content_container, menuFragment)
            commit()
        }
        
        // Cập nhật BottomNavigationView để hiển thị tab nav_order được chọn
        val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav?.selectedItemId = R.id.nav_order
    }
    
    // Helper method in PromotionAdapter to update data
    // fun updateData(newPromotions: List<Promotion>) {
    //     this.promotions = newPromotions
    //     notifyDataSetChanged()
    // }

    private fun displayProductsFromCache() {
        // TÁC DỤNG: Lấy dữ liệu từ cache và hiển thị lên các mục "Best Seller" và "Dành cho bạn".
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
        // TÁC DỤNG: Tải toàn bộ sản phẩm từ API và lưu vào cache.
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
                        displayProductsFromCache() // Sau khi tải xong, hiển thị dữ liệu.
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
}
