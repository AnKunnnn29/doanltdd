package com.example.doan.Fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Activities.CartActivity
import com.example.doan.Activities.ProductDetailActivity
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
import com.example.doan.Utils.VoiceSearchHelper
import com.google.android.material.card.MaterialCardView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.Normalizer

// YÊU CẦU: Chuyển toàn bộ nội dung của trang Home cũ sang đây.
// TÁC DỤNG: Màn hình này hiển thị toàn bộ thực đơn, cho phép tìm kiếm, lọc theo danh mục và giá.
class MenuFragment : Fragment(), CategoryAdapter.OnCategoryClickListener {

    private lateinit var productRecyclerView: RecyclerView
    private lateinit var categoryRecyclerView: RecyclerView
    private lateinit var searchSuggestionRecyclerView: RecyclerView

    private lateinit var productAdapter: ProductAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var searchSuggestionAdapter: SearchSuggestionAdapter

    private lateinit var searchView: SearchView
    private lateinit var btnFilterPrice: ImageView
    private lateinit var btnClearFilter: ImageView
    private lateinit var cardClearFilter: MaterialCardView
    private lateinit var btnCartMenu: FrameLayout
    private lateinit var cartBadgeMenu: TextView
    private lateinit var btnVoiceSearch: ImageView

    private val currentProductList = mutableListOf<Product>()
    private var selectedCategoryId = -1
    private var orderType: String? = null
    private var voiceSearchHelper: VoiceSearchHelper? = null

    // Cac bien trang thai cho bo loc.
    private var currentSearchQuery = ""
    private var isPriceAscending: Boolean? = null // null: khong sap xep, true: tang dan, false: giam dan
    
    // Permission launcher for microphone
    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startVoiceSearch()
        } else {
            Toast.makeText(context, "Can quyen microphone de tim kiem bang giong noi", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            orderType = it.getString("orderType")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return try {
            val view = inflater.inflate(R.layout.fragment_menu, container, false)

            // Lưu orderType vào SharedPreferences khi context đã sẵn sàng
            if (orderType != null) {
                val prefs = requireContext().getSharedPreferences("UTETeaPrefs", android.content.Context.MODE_PRIVATE)
                prefs.edit().putString("orderType", orderType).apply()
            }

            initViews(view)
            setupRecyclerViews()
            setupSearch()
            setupFilter()
            loadData()

            view
        } catch (e: Exception) {
            Log.e("MenuFragment", "Error in onCreateView: ${e.message}")
            e.printStackTrace()
            Toast.makeText(context, "Lỗi tải trang thực đơn", Toast.LENGTH_SHORT).show()
            inflater.inflate(R.layout.fragment_menu, container, false)
        }
    }

    private fun initViews(view: View) {
        // TAC DUNG: Gan cac bien view voi cac ID tu layout XML.
        productRecyclerView = view.findViewById(R.id.product_recycler_view_menu)
        categoryRecyclerView = view.findViewById(R.id.category_recycler_view_menu)
        searchSuggestionRecyclerView = view.findViewById(R.id.recycler_search_suggestions_menu)
        searchView = view.findViewById(R.id.search_view_menu)
        btnFilterPrice = view.findViewById(R.id.btn_filter_price_menu)
        btnClearFilter = view.findViewById(R.id.btn_clear_filter_menu)
        cardClearFilter = view.findViewById(R.id.card_clear_filter)
        btnCartMenu = view.findViewById(R.id.btn_cart_menu)
        cartBadgeMenu = view.findViewById(R.id.cart_badge_menu)
        btnVoiceSearch = view.findViewById(R.id.btn_voice_search)
        
        // Setup cart button
        btnCartMenu.setOnClickListener {
            startActivity(Intent(context, CartActivity::class.java).apply {
                putExtra("orderType", orderType)
            })
        }
        
        // Setup voice search button
        btnVoiceSearch.setOnClickListener {
            checkMicPermissionAndStartVoiceSearch()
        }
    }

    private fun setupRecyclerViews() {
        // TÁC DỤNG: Cấu hình LayoutManager và Adapter cho các RecyclerView.
        productRecyclerView.layoutManager = GridLayoutManager(context, 2)
        productAdapter = ProductAdapter(currentProductList)
        productRecyclerView.adapter = productAdapter
        
        // Add animation to product list
        productRecyclerView.layoutAnimation = AnimationUtils.loadLayoutAnimation(
            context, R.anim.layout_animation_fall_down
        )

        categoryRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        categoryAdapter = CategoryAdapter(DataCache.categories ?: listOf(), this)
        categoryRecyclerView.adapter = categoryAdapter

        searchSuggestionRecyclerView.layoutManager = LinearLayoutManager(context)
        searchSuggestionAdapter = SearchSuggestionAdapter(emptyList()) { product ->
            navigateToProductDetail(product)
        }
        searchSuggestionRecyclerView.adapter = searchSuggestionAdapter
    }

    private fun setupSearch() {
        // TÁC DỤNG: Thiết lập hành vi cho thanh tìm kiếm.
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                searchSuggestionRecyclerView.visibility = View.GONE
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchQuery = newText ?: ""

                if (currentSearchQuery.isNotEmpty()) {
                    updateSearchSuggestions(currentSearchQuery)
                } else {
                    searchSuggestionRecyclerView.visibility = View.GONE
                }

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
        // TÁC DỤNG: Cập nhật danh sách gợi ý tìm kiếm dựa trên từ khóa.
        val allProducts = DataCache.products ?: return
        val normalizedQuery = removeAccents(query.lowercase())

        val suggestions = allProducts.filter {
            removeAccents(it.name?.lowercase() ?: "").contains(normalizedQuery)
        }.take(5)

        if (suggestions.isNotEmpty()) {
            searchSuggestionAdapter.updateSuggestions(suggestions)
            searchSuggestionRecyclerView.visibility = View.VISIBLE
        } else {
            searchSuggestionRecyclerView.visibility = View.GONE
        }
    }

    private fun setupFilter() {
        // TÁC DỤNG: Thiết lập sự kiện cho các nút lọc.
        btnFilterPrice.setOnClickListener {
            isPriceAscending = when (isPriceAscending) {
                null -> true
                true -> false
                false -> null
            }

            updateFilterIcon()
            applyFilters()
        }

        cardClearFilter.setOnClickListener {
            resetFilters()
        }
        
        btnClearFilter.setOnClickListener {
            resetFilters()
        }
    }

    private fun updateFilterIcon() {
        // TÁC DỤNG: Cập nhật icon của nút lọc giá.
        val iconRes = when (isPriceAscending) {
            true -> R.drawable.ic_sort_ascending
            false -> R.drawable.ic_sort_descending
            null -> R.drawable.ic_sort
        }
        btnFilterPrice.setImageResource(iconRes)

        val tintColor = if (isPriceAscending != null) R.color.wine_primary else R.color.wine_neutral_dark
        btnFilterPrice.setColorFilter(resources.getColor(tintColor, null))
    }

    private fun resetFilters() {
        // TÁC DỤNG: Xóa tất cả các bộ lọc và quay về trạng thái ban đầu.
        isPriceAscending = null
        currentSearchQuery = ""
        selectedCategoryId = -1

        searchView.setQuery("", false)
        searchView.clearFocus()
        searchSuggestionRecyclerView.visibility = View.GONE

        updateFilterIcon()

        val categories = DataCache.categories ?: listOf()
        categoryAdapter.updateCategories(categories)

        applyFilters()
    }

    private fun applyFilters() {
        // TÁC DỤNG: Áp dụng tất cả các bộ lọc hiện tại vào danh sách sản phẩm.
        val allProducts = DataCache.products ?: return
        var filteredList = allProducts.toList()

        if (selectedCategoryId != -1) {
            filteredList = filteredList.filter { it.categoryId == selectedCategoryId }
        }

        if (currentSearchQuery.isNotEmpty()) {
            val normalizedQuery = removeAccents(currentSearchQuery.lowercase())
            filteredList = filteredList.filter {
                removeAccents(it.name?.lowercase() ?: "").contains(normalizedQuery)
            }
        }

        if (isPriceAscending != null) {
            filteredList = if (isPriceAscending == true) {
                filteredList.sortedBy { it.price }
            } else {
                filteredList.sortedByDescending { it.price }
            }
        }

        val isFiltered = selectedCategoryId != -1 || currentSearchQuery.isNotEmpty() || isPriceAscending != null
        cardClearFilter.visibility = if (isFiltered) View.VISIBLE else View.GONE

        currentProductList.clear()
        currentProductList.addAll(filteredList)
        productAdapter.notifyDataSetChanged()
        
        // Play animation when filter changes
        productRecyclerView.scheduleLayoutAnimation()
    }

    private fun removeAccents(str: String?): String {
        // TÁC DỤNG: Loại bỏ dấu tiếng Việt khỏi chuỗi để tìm kiếm không phân biệt dấu.
        if (str == null) return ""
        val nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD)
        val pattern = java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
        return pattern.matcher(nfdNormalizedString).replaceAll("").replace('đ', 'd').replace('Đ', 'D')
    }

    private fun loadData() {
        // TÁC DỤNG: Tải dữ liệu từ cache hoặc từ API nếu cache trống.
        if (DataCache.categories != null && DataCache.products != null) {
            categoryAdapter.updateCategories(DataCache.categories!!)
            applyFilters()
        } else {
            loadCategories()
        }
    }

    private fun loadCategories() {
        // TÁC DỤNG: Tải danh sách danh mục từ API.
        val context = context ?: return // Kiểm tra context trước khi sử dụng
        
        RetrofitClient.getInstance(context).apiService.getCategories()
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
                        Log.e("MenuFragment", "Error loading categories: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<Category>>>, t: Throwable) {
                    Log.e("MenuFragment", "Failed to connect for categories", t)
                }
            })
    }

    private fun loadAllProducts() {
        // TÁC DỤNG: Tải toàn bộ sản phẩm từ API.
        val context = context ?: return // Kiểm tra context trước khi sử dụng
        
        RetrofitClient.getInstance(context).apiService.getDrinks()
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
                        Log.e("MenuFragment", "Error loading products: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<Drink>>>, t: Throwable) {
                    Log.e("Menu.kt", "Failed to connect for products", t)
                    Toast.makeText(context, "Không thể tải thực đơn.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateCategoryImages() {
        // TÁC DỤNG: Cập nhật hình ảnh đại diện cho các danh mục dựa trên sản phẩm đầu tiên.
        val products = DataCache.products ?: return
        val categories = DataCache.categories ?: return
        if (products.isEmpty()) return

        val defaultImage = products.first().imageUrl

        categories.forEach { cat ->
            if (cat.image == null) { 
                cat.image = if (cat.id == -1) defaultImage else products.find { it.categoryId == cat.id }?.imageUrl ?: defaultImage
            }
        }
        categoryAdapter.notifyDataSetChanged()
    }

    override fun onCategoryClick(category: Category) {
        // TÁC DỤNG: Xử lý sự kiện khi người dùng nhấn vào một danh mục.
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
            putExtra("orderType", orderType) // Pass the order type to the detail screen
        }
        startActivity(intent)
    }

    private fun navigateToCheckout() {
        val bundle = Bundle().apply {
            putString("orderType", orderType)
        }
        findNavController().navigate(R.id.nav_cart, bundle)
    }
    
    // ==================== Voice Search ====================
    
    private fun checkMicPermissionAndStartVoiceSearch() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startVoiceSearch()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                Toast.makeText(
                    context,
                    "Can quyen microphone de tim kiem bang giong noi",
                    Toast.LENGTH_LONG
                ).show()
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            else -> {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
    
    private fun startVoiceSearch() {
        voiceSearchHelper?.destroy()
        voiceSearchHelper = VoiceSearchHelper(requireContext()) { searchText ->
            // Callback khi nhan duoc ket qua voice
            searchView.setQuery(searchText, true)
            Toast.makeText(context, "Tim kiem: $searchText", Toast.LENGTH_SHORT).show()
        }
        voiceSearchHelper?.startListening()
        
        // Hien thi trang thai dang nghe
        Toast.makeText(context, "Dang nghe... Hay noi ten do uong", Toast.LENGTH_SHORT).show()
        btnVoiceSearch.setColorFilter(ContextCompat.getColor(requireContext(), R.color.success))
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        voiceSearchHelper?.destroy()
        voiceSearchHelper = null
    }
}
