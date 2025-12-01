package com.example.doan.Fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.doan.Adapters.BannerAdapter
import com.example.doan.Adapters.CategoryAdapter
import com.example.doan.Adapters.ProductAdapter
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.Category
import com.example.doan.Models.Drink
import com.example.doan.Models.Product
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.DataCache
import me.relex.circleindicator.CircleIndicator3
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeFragment : Fragment(), CategoryAdapter.OnCategoryClickListener {

    private lateinit var productRecyclerView: RecyclerView
    private lateinit var categoryRecyclerView: RecyclerView
    private lateinit var productAdapter: ProductAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var bannerViewPager: ViewPager2
    private lateinit var bannerIndicator: CircleIndicator3

    private val currentProductList = mutableListOf<Product>()
    
    private var selectedCategoryId = -1

    private val autoSlideHandler = Handler(Looper.getMainLooper())
    private var autoSlideRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Setup Banner
        bannerViewPager = view.findViewById(R.id.banner_view_pager)
        bannerIndicator = view.findViewById(R.id.banner_indicator)
        setupBannerSlider()

        // Setup Product RecyclerView
        productRecyclerView = view.findViewById(R.id.product_recycler_view)
        productRecyclerView.layoutManager = GridLayoutManager(context, 2)
        productAdapter = ProductAdapter(currentProductList)
        productRecyclerView.adapter = productAdapter

        // Setup Category RecyclerView
        categoryRecyclerView = view.findViewById(R.id.category_recycler_view)
        categoryRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        categoryAdapter = CategoryAdapter(DataCache.categories ?: listOf(), this)
        categoryRecyclerView.adapter = categoryAdapter

        // Load data
        loadData()

        return view
    }

    private fun setupBannerSlider() {
        val bannerImages = listOf(R.drawable.banners, R.drawable.banners2, R.drawable.banners3)
        val adapter = BannerAdapter(bannerImages)
        bannerViewPager.adapter = adapter
        bannerIndicator.setViewPager(bannerViewPager)

        // Auto-slide logic
        autoSlideRunnable = Runnable {
            var currentItem = bannerViewPager.currentItem
            currentItem++
            if (currentItem >= bannerImages.size) {
                currentItem = 0
            }
            bannerViewPager.setCurrentItem(currentItem, true)
        }

        bannerViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                autoSlideHandler.removeCallbacks(autoSlideRunnable!!)
                autoSlideHandler.postDelayed(autoSlideRunnable!!, 3000)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        autoSlideRunnable?.let { autoSlideHandler.postDelayed(it, 3000) }
    }

    override fun onPause() {
        super.onPause()
        autoSlideRunnable?.let { autoSlideHandler.removeCallbacks(it) }
    }

    private fun loadData() {
        if (DataCache.categories != null && DataCache.products != null) {
            // Data is cached, just display it
            categoryAdapter.updateCategories(DataCache.categories!!)
            filterProductsByCategory(selectedCategoryId)
        } else {
            // Data not cached, fetch from network
            loadCategories()
        }
    }

    private fun loadCategories() {
        RetrofitClient.getInstance(requireContext()).apiService.getCategories()
            .enqueue(object : Callback<ApiResponse<List<Category>>> {
                override fun onResponse(call: Call<ApiResponse<List<Category>>>, response: Response<ApiResponse<List<Category>>>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val categories = response.body()?.data ?: listOf()
                        val allCategory = Category(id = -1, name = "Tất cả")
                        val fullCategoryList = mutableListOf(allCategory).apply { addAll(categories) }
                        DataCache.categories = fullCategoryList
                        categoryAdapter.updateCategories(fullCategoryList)
                        loadAllProducts()
                    } else {
                        Log.e("HomeFragment", "Error loading categories: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<Category>>>, t: Throwable) {
                    Log.e("HomeFragment", "Failed to connect for categories", t)
                }
            })
    }

    private fun loadAllProducts() {
        RetrofitClient.getInstance(requireContext()).apiService.getDrinks()
            .enqueue(object : Callback<ApiResponse<List<Drink>>> {
                override fun onResponse(call: Call<ApiResponse<List<Drink>>>, response: Response<ApiResponse<List<Drink>>>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val drinks = response.body()?.data ?: listOf()
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
                        updateCategoryImages()
                        filterProductsByCategory(selectedCategoryId)
                    } else {
                        Log.e("HomeFragment", "Error loading products: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<Drink>>>, t: Throwable) {
                    Log.e("HomeFragment", "Failed to connect for products", t)
                    Toast.makeText(context, "Không thể tải thực đơn.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateCategoryImages() {
        val products = DataCache.products ?: return
        val categories = DataCache.categories ?: return
        if (products.isEmpty()) return

        val defaultImage = products.first().imageUrl

        categories.forEach { cat ->
            if (cat.image == null) { // Only update if not already set
                cat.image = if (cat.id == -1) defaultImage else products.find { it.categoryId == cat.id }?.imageUrl ?: defaultImage
            }
        }
        categoryAdapter.notifyDataSetChanged()
    }

    override fun onCategoryClick(category: Category) {
        selectedCategoryId = category.id
        filterProductsByCategory(selectedCategoryId)
    }

    private fun filterProductsByCategory(categoryId: Int) {
        currentProductList.clear()
        val allProducts = DataCache.products ?: listOf()
        
        if (categoryId == -1) {
            currentProductList.addAll(allProducts)
        } else {
            currentProductList.addAll(allProducts.filter { it.categoryId == categoryId })
        }
        
        currentProductList.sortBy { it.name?.lowercase() }
        productAdapter.notifyDataSetChanged()
    }
}
