package com.example.doan.Fragments

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.doan.Activities.ProductDetailActivity
import com.example.doan.Adapters.BannerAdapter
import com.example.doan.Adapters.CategoryAdapter
import com.example.doan.Adapters.ProductAdapter
import com.example.doan.Adapters.SearchSuggestionAdapter
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
import java.text.Normalizer

class HomeFragment : Fragment(), CategoryAdapter.OnCategoryClickListener {

    private lateinit var productRecyclerView: RecyclerView
    private lateinit var categoryRecyclerView: RecyclerView
    private lateinit var searchSuggestionRecyclerView: RecyclerView
    
    private lateinit var productAdapter: ProductAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var searchSuggestionAdapter: SearchSuggestionAdapter
    
    private lateinit var bannerViewPager: ViewPager2
    private lateinit var bannerIndicator: CircleIndicator3
    private lateinit var searchView: SearchView
    private lateinit var btnFilterPrice: ImageView
    private lateinit var btnClearFilter: ImageView

    private val currentProductList = mutableListOf<Product>()
    private var selectedCategoryId = -1
    
    // Filter states
    private var currentSearchQuery = ""
    private var isPriceAscending: Boolean? = null // null: no sort, true: asc, false: desc

    private val autoSlideHandler = Handler(Looper.getMainLooper())
    private var autoSlideRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return try {
            val view = inflater.inflate(R.layout.fragment_home, container, false)

            initViews(view)
            setupBannerSlider()
            setupRecyclerViews()
            setupSearch()
            setupFilter()
            loadData()

            view
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error in onCreateView: ${e.message}")
            e.printStackTrace()
            Toast.makeText(context, "Lỗi tải trang chủ", Toast.LENGTH_SHORT).show()
            inflater.inflate(R.layout.fragment_home, container, false)
        }
    }

    private fun initViews(view: View) {
        bannerViewPager = view.findViewById(R.id.banner_view_pager)
        bannerIndicator = view.findViewById(R.id.banner_indicator)
        productRecyclerView = view.findViewById(R.id.product_recycler_view)
        categoryRecyclerView = view.findViewById(R.id.category_recycler_view)
        searchSuggestionRecyclerView = view.findViewById(R.id.recycler_search_suggestions)
        searchView = view.findViewById(R.id.search_view)
        btnFilterPrice = view.findViewById(R.id.btn_filter_price)
        btnClearFilter = view.findViewById(R.id.btn_clear_filter)
    }

    private fun setupRecyclerViews() {
        // Product RecyclerView
        productRecyclerView.layoutManager = GridLayoutManager(context, 2)
        productAdapter = ProductAdapter(currentProductList)
        productRecyclerView.adapter = productAdapter

        // Category RecyclerView
        categoryRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        categoryAdapter = CategoryAdapter(DataCache.categories ?: listOf(), this)
        categoryRecyclerView.adapter = categoryAdapter
        
        // Search Suggestion RecyclerView
        searchSuggestionRecyclerView.layoutManager = LinearLayoutManager(context)
        searchSuggestionAdapter = SearchSuggestionAdapter(emptyList()) { product ->
            navigateToProductDetail(product)
        }
        searchSuggestionRecyclerView.adapter = searchSuggestionAdapter
    }

    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                searchSuggestionRecyclerView.visibility = View.GONE
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchQuery = newText ?: ""
                
                // Update suggestions
                if (currentSearchQuery.isNotEmpty()) {
                    updateSearchSuggestions(currentSearchQuery)
                } else {
                    searchSuggestionRecyclerView.visibility = View.GONE
                }
                
                // Apply filters
                applyFilters()
                
                return true
            }
        })
        
        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                searchSuggestionRecyclerView.visibility = View.GONE
            }
        }
    }
    
    private fun updateSearchSuggestions(query: String) {
        val allProducts = DataCache.products ?: return
        val normalizedQuery = removeAccents(query.lowercase())
        
        val suggestions = allProducts.filter { 
            removeAccents(it.name?.lowercase() ?: "").contains(normalizedQuery)
        }.take(5) // Limit to 5 suggestions
        
        if (suggestions.isNotEmpty()) {
            searchSuggestionAdapter.updateSuggestions(suggestions)
            searchSuggestionRecyclerView.visibility = View.VISIBLE
        } else {
            searchSuggestionRecyclerView.visibility = View.GONE
        }
    }

    private fun setupFilter() {
        btnFilterPrice.setOnClickListener {
            // Toggle sort state: null -> asc -> desc -> null
            isPriceAscending = when (isPriceAscending) {
                null -> true
                true -> false
                false -> null
            }
            
            updateFilterIcon()
            applyFilters()
        }
        
        btnClearFilter.setOnClickListener {
            resetFilters()
        }
    }
    
    private fun updateFilterIcon() {
        val iconRes = when (isPriceAscending) {
            true -> R.drawable.ic_sort_ascending
            false -> R.drawable.ic_sort_descending
            null -> R.drawable.ic_sort
        }
        btnFilterPrice.setImageResource(iconRes)
        
        // Tint logic if needed (optional, depends on drawable color)
        val tintColor = if (isPriceAscending != null) R.color.wine_primary else R.color.wine_neutral_dark
        btnFilterPrice.setColorFilter(resources.getColor(tintColor, null))
        btnFilterPrice.rotation = 0f // Reset rotation as we use different drawables
    }
    
    private fun resetFilters() {
        isPriceAscending = null
        currentSearchQuery = ""
        selectedCategoryId = -1
        
        searchView.setQuery("", false)
        searchView.clearFocus()
        searchSuggestionRecyclerView.visibility = View.GONE
        
        updateFilterIcon()
        
        // Reset category selection
        val categories = DataCache.categories ?: listOf()
        categoryAdapter.updateCategories(categories)
        
        applyFilters()
    }

    private fun applyFilters() {
        val allProducts = DataCache.products ?: return
        var filteredList = allProducts.toList()

        // 1. Filter by Category
        if (selectedCategoryId != -1) {
            filteredList = filteredList.filter { it.categoryId == selectedCategoryId }
        }

        // 2. Filter by Name (Search)
        if (currentSearchQuery.isNotEmpty()) {
            val normalizedQuery = removeAccents(currentSearchQuery.lowercase())
            filteredList = filteredList.filter {
                removeAccents(it.name?.lowercase() ?: "").contains(normalizedQuery)
            }
        }

        // 3. Sort by Price
        if (isPriceAscending != null) {
            filteredList = if (isPriceAscending == true) {
                filteredList.sortedBy { it.price }
            } else {
                filteredList.sortedByDescending { it.price }
            }
        }
        
        // Show/Hide clear button
        val isFiltered = selectedCategoryId != -1 || currentSearchQuery.isNotEmpty() || isPriceAscending != null
        btnClearFilter.visibility = if (isFiltered) View.VISIBLE else View.GONE

        // Update Adapter
        currentProductList.clear()
        currentProductList.addAll(filteredList)
        productAdapter.notifyDataSetChanged()
    }
    
    private fun removeAccents(str: String?): String {
        if (str == null) return ""
        val nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD)
        val pattern = java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
        return pattern.matcher(nfdNormalizedString).replaceAll("").replace('đ', 'd').replace('Đ', 'D')
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
            categoryAdapter.updateCategories(DataCache.categories!!)
            applyFilters()
        } else {
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
                        applyFilters()
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
        applyFilters()
    }
    
    private fun navigateToProductDetail(product: Product) {
        val intent = Intent(context, ProductDetailActivity::class.java).apply {
            putExtra("PRODUCT_ID", product.id)
            putExtra("PRODUCT_NAME", product.name)
            putExtra("PRODUCT_PRICE", product.price)
            putExtra("PRODUCT_DESC", product.description)
            putExtra("PRODUCT_IMAGE", product.imageUrl)
            putExtra("CATEGORY_NAME", product.category)
        }
        startActivity(intent)
    }
}
