package com.example.doan.Activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.doan.Models.*
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class ProductDetailActivity : AppCompatActivity() {

    private lateinit var ivProductImage: ImageView
    private lateinit var tvProductName: TextView
    private lateinit var tvProductPrice: TextView
    private lateinit var tvProductDescription: TextView
    private lateinit var tvQuantity: TextView
    private lateinit var tvTotalPrice: TextView
    private lateinit var btnDecrease: MaterialButton
    private lateinit var btnIncrease: MaterialButton
    private lateinit var btnAddToCart: MaterialButton
    private lateinit var btnConfirmOrder: MaterialButton
    private lateinit var spinnerSize: Spinner
    private lateinit var spinnerBranch: Spinner
    private lateinit var layoutToppings: LinearLayout
    private lateinit var btnBack: ImageButton

    private var product: Drink? = null
    private var quantity = 1
    private var selectedSize: DrinkSize? = null
    private val selectedToppings = mutableSetOf<DrinkTopping>()
    private var storeList = mutableListOf<Store>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)

        initViews()
        getProductDetails()
        loadStores()
        setupListeners()
    }

    private fun getLoggedInUserId(): Int {
        val sessionManager = SessionManager(this)
        if (sessionManager.isLoggedIn()) {
            return sessionManager.getUserId()
        }
        
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        return sharedPreferences.getInt("userId", -1)
    }

    private fun initViews() {
        ivProductImage = findViewById(R.id.ivProductImage)
        tvProductName = findViewById(R.id.tvProductName)
        tvProductPrice = findViewById(R.id.tvProductPrice)
        tvProductDescription = findViewById(R.id.tvProductDescription)
        tvQuantity = findViewById(R.id.tvQuantity)
        tvTotalPrice = findViewById(R.id.tvTotalPrice)
        btnDecrease = findViewById(R.id.btnDecrease)
        btnIncrease = findViewById(R.id.btnIncrease)
        btnAddToCart = findViewById(R.id.btnAddToCart)
        btnConfirmOrder = findViewById(R.id.btnConfirmOrder)
        spinnerSize = findViewById(R.id.spinnerSize)
        spinnerBranch = findViewById(R.id.spinnerBranch)
        layoutToppings = findViewById(R.id.layoutToppings)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener { finish() }
    }

    private fun getProductDetails() {
        intent?.getStringExtra("product")?.let { productJson ->
            Log.d(TAG, "Product JSON: $productJson")
            try {
                product = Gson().fromJson(productJson, Drink::class.java)
                product?.let {
                    Log.d(TAG, "Product parsed successfully: ${it.name}")
                    displayProductDetails()
                } ?: run {
                    Log.e(TAG, "Product is null after parsing")
                    Toast.makeText(this, "Không thể tải chi tiết sản phẩm", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing product: ${e.message}")
                Toast.makeText(this, "Lỗi tải sản phẩm: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        } ?: run {
            Log.e(TAG, "No product extra in intent")
            Toast.makeText(this, "Không có thông tin sản phẩm", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun displayProductDetails() {
        try {
            product?.let { prod ->
                tvProductName.text = prod.name ?: "Tên sản phẩm"
                tvProductDescription.text = prod.description ?: "Không có mô tả"
                tvProductPrice.text = String.format(Locale.getDefault(), "%,.0f VNĐ", prod.basePrice)

                prod.imageUrl?.takeIf { it.isNotEmpty() }?.let { imageUrl ->
                    Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_placeholder)
                        .into(ivProductImage)
                }

                setupSizeSpinner()
                setupToppingCheckboxes()
                updateTotalPrice()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying product details: ${e.message}")
            Toast.makeText(this, "Lỗi hiển thị sản phẩm: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadStores() {
        RetrofitClient.getInstance(this).apiService.getStores()
            .enqueue(object : Callback<ApiResponse<List<Store>>> {
                override fun onResponse(call: Call<ApiResponse<List<Store>>>, response: Response<ApiResponse<List<Store>>>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        storeList = response.body()?.data?.toMutableList() ?: mutableListOf()
                        if (storeList.isNotEmpty()) {
                            val storeNames = storeList.map { it.storeName }
                            val storeAdapter = ArrayAdapter(this@ProductDetailActivity, android.R.layout.simple_spinner_item, storeNames)
                            storeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            spinnerBranch.adapter = storeAdapter
                        }
                    } else {
                        Toast.makeText(this@ProductDetailActivity, "Không thể tải danh sách cửa hàng", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<Store>>>, t: Throwable) {
                    Log.e(TAG, "Lỗi tải danh sách cửa hàng: ${t.message}")
                    Toast.makeText(this@ProductDetailActivity, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun setupListeners() {
        btnDecrease.setOnClickListener {
            if (quantity > 1) {
                quantity--
                tvQuantity.text = quantity.toString()
                updateTotalPrice()
            }
        }

        btnIncrease.setOnClickListener {
            quantity++
            tvQuantity.text = quantity.toString()
            updateTotalPrice()
        }

        btnConfirmOrder.setOnClickListener { placeOrder() }
        btnAddToCart.setOnClickListener { addToCart() }
    }

    private fun setupSizeSpinner() {
        product?.sizes?.takeIf { it.isNotEmpty() }?.let { sizes ->
            val sizeOptions = sizes.map { size ->
                val option = size.sizeName
                if (size.extraPrice > 0) {
                    "$option (+${String.format(Locale.getDefault(), "%,.0f VNĐ", size.extraPrice)})"
                } else {
                    option
                }
            }
            
            val sizeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sizeOptions)
            sizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerSize.adapter = sizeAdapter
            
            selectedSize = sizes[0]
            
            spinnerSize.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    selectedSize = sizes[position]
                    updateTotalPrice()
                }
                
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        } ?: run {
            spinnerSize.visibility = View.GONE
            selectedSize = null
        }
    }
    
    private fun setupToppingCheckboxes() {
        product?.toppings?.takeIf { it.isNotEmpty() }?.let { toppings ->
            layoutToppings.removeAllViews()
            
            toppings.filter { it.isActive }.forEach { topping ->
                val checkBox = CheckBox(this).apply {
                    text = "${topping.toppingName} (+${String.format(Locale.getDefault(), "%,.0f VNĐ", topping.price)})"
                    textSize = 14f
                    setPadding(8, 8, 8, 8)
                    
                    setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            selectedToppings.add(topping)
                        } else {
                            selectedToppings.remove(topping)
                        }
                        updateTotalPrice()
                    }
                }
                
                layoutToppings.addView(checkBox)
            }
        } ?: run {
            layoutToppings.visibility = View.GONE
        }
    }
    
    private fun updateTotalPrice() {
        product?.let { prod ->
            val basePrice = prod.basePrice
            val sizeExtra = selectedSize?.extraPrice ?: 0.0
            val toppingTotal = selectedToppings.sumOf { it.price }
            
            val itemPrice = basePrice + sizeExtra + toppingTotal
            val total = itemPrice * quantity
            
            tvTotalPrice.text = String.format(Locale.getDefault(), "%,.0f VNĐ", total)
        }
    }

    private fun addToCart() {
        val userId = getLoggedInUserId()
        if (userId == -1) {
            Toast.makeText(this, "Vui lòng đăng nhập để thêm vào giỏ hàng", Toast.LENGTH_SHORT).show()
            return
        }
        
        val toppingIds = selectedToppings.map { it.id.toLong() }
        val note = if (selectedToppings.isNotEmpty()) {
            "Topping: ${selectedToppings.joinToString(", ") { it.toppingName ?: "" }}"
        } else ""
        
        val sizeId = selectedSize?.id?.toLong()
        
        val request = AddToCartRequest(
            product?.id?.toLong() ?: 0L,
            sizeId ?: 0L,
            quantity,
            toppingIds.takeIf { it.isNotEmpty() },
            note
        )
        
        btnAddToCart.isEnabled = false
        btnAddToCart.text = "Đang thêm..."
        
        RetrofitClient.getInstance(this).apiService.addToCart(userId.toLong(), request)
            .enqueue(object : Callback<ApiResponse<Cart>> {
                override fun onResponse(call: Call<ApiResponse<Cart>>, response: Response<ApiResponse<Cart>>) {
                    btnAddToCart.isEnabled = true
                    btnAddToCart.text = "THÊM VÀO GIỎ"
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        AlertDialog.Builder(this@ProductDetailActivity)
                            .setTitle("Thêm vào giỏ hàng thành công!")
                            .setMessage("Bạn có muốn xem giỏ hàng ngay không?\n\n⚠️ Lưu ý: Giỏ hàng sẽ tự động xóa sau 5 phút nếu không đặt hàng.")
                            .setPositiveButton("Xem giỏ hàng") { _, _ ->
                                startActivity(Intent(this@ProductDetailActivity, CartActivity::class.java))
                            }
                            .setNegativeButton("Tiếp tục mua") { dialog, _ -> dialog.dismiss() }
                            .show()
                    } else {
                        val message = response.body()?.message ?: "Lỗi thêm vào giỏ hàng"
                        Toast.makeText(this@ProductDetailActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }
                
                override fun onFailure(call: Call<ApiResponse<Cart>>, t: Throwable) {
                    btnAddToCart.isEnabled = true
                    btnAddToCart.text = "THÊM VÀO GIỎ"
                    Toast.makeText(this@ProductDetailActivity, "Không thể kết nối đến máy chủ", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun placeOrder() {
        if (storeList.isEmpty()) {
            Toast.makeText(this, "Đang tải danh sách cửa hàng, vui lòng đợi...", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedStoreIndex = spinnerBranch.selectedItemPosition
        if (selectedStoreIndex == -1) {
            Toast.makeText(this, "Vui lòng chọn cửa hàng", Toast.LENGTH_SHORT).show()
            return
        }
        
        val storeId = storeList[selectedStoreIndex].id.toLong()
        
        val userId = getLoggedInUserId()
        if (userId == -1) {
            Toast.makeText(this, "Vui lòng đăng nhập để đặt hàng", Toast.LENGTH_SHORT).show()
            return
        }
        
        val toppingIds = selectedToppings.map { it.id.toLong() }
        val note = if (selectedToppings.isNotEmpty()) {
            "Topping: ${selectedToppings.joinToString(", ") { it.toppingName ?: "" }}"
        } else ""
        
        val sizeName = selectedSize?.sizeName ?: "M"
        
        val item = OrderItemRequest(
            product?.id?.toLong() ?: 0L,
            sizeName,
            quantity,
            note,
            toppingIds.takeIf { it.isNotEmpty() }
        )

        val orderRequest = CreateOrderRequest(storeId, "PICKUP", "Tại cửa hàng", "COD", null, null, listOf(item))

        btnConfirmOrder.isEnabled = false
        btnConfirmOrder.text = "Đang xử lý..."

        RetrofitClient.getInstance(this).apiService.createOrder(orderRequest)
            .enqueue(object : Callback<ApiResponse<Order>> {
                override fun onResponse(call: Call<ApiResponse<Order>>, response: Response<ApiResponse<Order>>) {
                    btnConfirmOrder.isEnabled = true
                    btnConfirmOrder.text = "MUA NGAY"

                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@ProductDetailActivity, "Đặt hàng thành công!", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        val message = response.body()?.message ?: "Lỗi Server: ${response.code()}"
                        Toast.makeText(this@ProductDetailActivity, "Đặt hàng thất bại: $message", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Order>>, t: Throwable) {
                    btnConfirmOrder.isEnabled = true
                    btnConfirmOrder.text = "MUA NGAY"
                    Toast.makeText(this@ProductDetailActivity, "Không thể kết nối đến máy chủ", Toast.LENGTH_SHORT).show()
                }
            })
    }

    companion object {
        private const val TAG = "ProductDetailActivity"
    }
}
