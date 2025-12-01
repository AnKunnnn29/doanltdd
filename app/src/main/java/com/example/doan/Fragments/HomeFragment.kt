package com.example.doan.Fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Adapters.CategoryAdapter
import com.example.doan.Adapters.ProductAdapter
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.Category
import com.example.doan.Models.Drink
import com.example.doan.Models.Product
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeFragment : Fragment(), CategoryAdapter.OnCategoryClickListener {

    private lateinit var productRecyclerView: RecyclerView
    private lateinit var categoryRecyclerView: RecyclerView
    private lateinit var productAdapter: ProductAdapter
    private lateinit var categoryAdapter: CategoryAdapter

    private val currentProductList = mutableListOf<Product>()
    private val allProducts = mutableListOf<Product>()
    private val categoryList = mutableListOf<Category>()
    
    private var selectedCategoryId = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Setup Product RecyclerView
        productRecyclerView = view.findViewById(R.id.product_recycler_view)
        productRecyclerView.layoutManager = GridLayoutManager(context, 2)
        productAdapter = ProductAdapter(currentProductList)
        productRecyclerView.adapter = productAdapter

        // Setup Category RecyclerView
        categoryRecyclerView = view.findViewById(R.id.category_recycler_view)
        categoryRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        categoryAdapter = CategoryAdapter(categoryList, this)
        categoryRecyclerView.adapter = categoryAdapter

        loadCategories()

        return view
    }

    private fun loadCategories() {
        RetrofitClient.getInstance(requireContext()).apiService.getCategories()
            .enqueue(object : Callback<ApiResponse<List<Category>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<Category>>>,
                    response: Response<ApiResponse<List<Category>>>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!
                        if (apiResponse.success && apiResponse.data != null) {
                            categoryList.clear()
                            
                            val allCategory = Category().apply {
                                id = -1
                                name = "Tất cả"
                            }
                            categoryList.add(allCategory)
                            
                            categoryList.addAll(apiResponse.data!!)
                            categoryAdapter.notifyDataSetChanged()
                            
                            loadAllProducts()
                        }
                    } else {
                        Log.e("HomeFragment", "Lỗi tải danh mục: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<Category>>>, t: Throwable) {
                    Log.e("HomeFragment", "Lỗi kết nối danh mục: ${t.message}")
                }
            })
    }

    private fun loadAllProducts() {
        RetrofitClient.getInstance(requireContext()).apiService.getDrinks()
            .enqueue(object : Callback<ApiResponse<List<Drink>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<Drink>>>,
                    response: Response<ApiResponse<List<Drink>>>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!
                        if (apiResponse.success && apiResponse.data != null) {
                            allProducts.clear()
                            
                            val baseUrl = RetrofitClient.getBaseUrl()
                            var rootUrl = baseUrl.replace("/api/", "")
                            if (rootUrl.endsWith("/")) rootUrl = rootUrl.substring(0, rootUrl.length - 1)

                            apiResponse.data!!.forEach { drink ->
                                var imageUrl = drink.imageUrl
                                if (imageUrl != null && !imageUrl.startsWith("http")) {
                                    if (!imageUrl.startsWith("/")) imageUrl = "/$imageUrl"
                                    imageUrl = rootUrl + imageUrl
                                }

                                val pCategoryId = drink.categoryId
                                
                                Log.d("HomeFragment", "Sản phẩm: ${drink.name} - Category ID: $pCategoryId")

                                var categoryDisplayName = categoryList.find { it.id == pCategoryId }?.name ?: ""
                                if (categoryDisplayName.isEmpty()) categoryDisplayName = drink.categoryName ?: ""

                                val product = Product(
                                    id = drink.id,
                                    name = drink.name,
                                    description = drink.description ?: "",
                                    price = drink.basePrice,
                                    category = categoryDisplayName,
                                    categoryId = pCategoryId,
                                    imageUrl = imageUrl,
                                    isAvailable = drink.isActive
                                ).apply {
                                    sizes = drink.sizes
                                    toppings = drink.toppings
                                }
                                
                                allProducts.add(product)
                            }

                            updateCategoryImages()
                            filterProductsByCategory(selectedCategoryId)
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<Drink>>>, t: Throwable) {
                    Log.e("HomeFragment", "Lỗi kết nối sản phẩm: ${t.message}")
                    Toast.makeText(context, "Không thể tải thực đơn.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateCategoryImages() {
        if (allProducts.isEmpty()) return

        val defaultImage = allProducts[0].imageUrl

        categoryList.forEach { cat ->
            if (cat.id == -1) {
                cat.image = defaultImage
            } else {
                val product = allProducts.find { it.categoryId == cat.id }
                cat.image = product?.imageUrl ?: defaultImage
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
        
        if (categoryId == -1) {
            currentProductList.addAll(allProducts)
        } else {
            currentProductList.addAll(allProducts.filter { it.categoryId == categoryId })
        }
        
        if (categoryId != -1) {
            if (currentProductList.isEmpty()) {
                Toast.makeText(context, "Không tìm thấy sản phẩm nào cho danh mục ID: $categoryId", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Tìm thấy ${currentProductList.size} sản phẩm", Toast.LENGTH_SHORT).show()
            }
        }
        
        currentProductList.sortBy { it.name?.lowercase() }
        productAdapter.notifyDataSetChanged()
    }
}
