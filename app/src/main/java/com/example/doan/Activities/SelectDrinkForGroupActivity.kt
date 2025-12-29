package com.example.doan.Activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Adapters.ProductSelectAdapter
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.Drink
import com.example.doan.Models.Product
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.DataCache
import com.example.doan.Utils.LoadingDialog
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SelectDrinkForGroupActivity : AppCompatActivity() {

    private lateinit var rvProducts: RecyclerView
    private lateinit var loadingDialog: LoadingDialog
    private var groupOrderId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_drink_for_group)

        groupOrderId = intent.getLongExtra("GROUP_ORDER_ID", 0)
        if (groupOrderId == 0L) {
            Toast.makeText(this, "Lỗi: Không tìm thấy phiên", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadingDialog = LoadingDialog(this)
        initViews()
        loadProducts()
    }

    private fun initViews() {
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).apply {
            setNavigationOnClickListener { finish() }
        }

        rvProducts = findViewById(R.id.rv_products)
        rvProducts.layoutManager = GridLayoutManager(this, 2)
    }

    private fun loadProducts() {
        // Kiểm tra cache trước
        if (!DataCache.products.isNullOrEmpty()) {
            displayProducts(DataCache.products!!)
            return
        }

        loadingDialog.show("Đang tải sản phẩm...")
        
        RetrofitClient.getInstance(this).apiService.getDrinks()
            .enqueue(object : Callback<ApiResponse<List<Drink>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<Drink>>>,
                    response: Response<ApiResponse<List<Drink>>>
                ) {
                    loadingDialog.dismiss()
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
                        displayProducts(products)
                    } else {
                        Toast.makeText(this@SelectDrinkForGroupActivity, 
                            "Không thể tải sản phẩm", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<Drink>>>, t: Throwable) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@SelectDrinkForGroupActivity, 
                        "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun displayProducts(products: List<Product>) {
        val adapter = ProductSelectAdapter(products) { product ->
            // Khi chọn sản phẩm, mở Activity để chọn size, số lượng
            showAddToGroupDialog(product)
        }
        rvProducts.adapter = adapter
    }

    private fun showAddToGroupDialog(product: Product) {
        val intent = Intent(this, AddToGroupOrderActivity::class.java).apply {
            putExtra("GROUP_ORDER_ID", groupOrderId)
            putExtra("DRINK_ID", product.id.toLong())
            putExtra("DRINK_NAME", product.name)
            putExtra("DRINK_PRICE", product.price)
            putExtra("DRINK_IMAGE", product.imageUrl)
        }
        startActivityForResult(intent, REQUEST_ADD_ITEM)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ADD_ITEM && resultCode == RESULT_OK) {
            // Item đã được thêm, quay lại GroupOrderActivity
            setResult(RESULT_OK)
            finish()
        }
    }

    companion object {
        private const val REQUEST_ADD_ITEM = 100
    }
}
