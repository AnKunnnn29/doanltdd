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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class ProductDetailActivity : AppCompatActivity() {

    private lateinit var ivProductImage: ImageView
    private lateinit var tvProductName: TextView
    private lateinit var tvProductPrice: TextView
    private lateinit var tvProductDescription: TextView
    private lateinit var tvCategory: TextView
    private lateinit var tvQuantity: TextView
    private lateinit var tvTotalPrice: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var btnDecrease: MaterialButton
    private lateinit var btnIncrease: MaterialButton
    private lateinit var btnAddToCart: MaterialButton
    private lateinit var btnConfirmOrder: MaterialButton
    private lateinit var spinnerSize: Spinner
    private lateinit var layoutToppings: LinearLayout
    private lateinit var tvToppingLabel: TextView

    private var productId: Int = -1
    private var product: Drink? = null
    private var quantity = 1
    private var selectedSize: DrinkSize? = null
    private val selectedToppings = mutableSetOf<DrinkTopping>()
    
    // FIX C5: Lưu reference của các Retrofit calls để cancel khi Activity destroy
    private var loadProductCall: Call<ApiResponse<List<Drink>>>? = null
    private var addToCartCall: Call<ApiResponse<Cart>>? = null

    companion object {
        private const val TAG = "ProductDetailActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail_new)

        initViews()
        getIntentData()
        setupListeners()
        loadProductDetails()
    }
    
    // FIX C5: Cancel tất cả pending calls khi Activity bị destroy
    override fun onDestroy() {
        super.onDestroy()
        loadProductCall?.cancel()
        addToCartCall?.cancel()
        Log.d(TAG, "Cancelled pending API calls")
    }

    private fun initViews() {
        ivProductImage = findViewById(R.id.detail_product_image)
        tvProductName = findViewById(R.id.detail_product_name)
        tvProductPrice = findViewById(R.id.detail_product_price)
        tvProductDescription = findViewById(R.id.detail_product_description)
        tvCategory = findViewById(R.id.detail_product_category)
        tvQuantity = findViewById(R.id.tv_quantity)
        tvTotalPrice = findViewById(R.id.tv_total_price)

        btnBack = findViewById(R.id.btn_back)
        btnDecrease = findViewById(R.id.btn_decrease_qty)
        btnIncrease = findViewById(R.id.btn_increase_qty)
        btnAddToCart = findViewById(R.id.btn_add_to_cart)
        btnConfirmOrder = findViewById(R.id.btn_confirm_order)

        spinnerSize = findViewById(R.id.spinner_size)
        layoutToppings = findViewById(R.id.layout_toppings)
        tvToppingLabel = findViewById(R.id.tv_topping_label)
    }

    private fun getIntentData() {
        productId = intent.getIntExtra("PRODUCT_ID", -1)
        if (productId == -1) {
            Toast.makeText(this, "Không tìm thấy sản phẩm", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tvProductName.text = intent.getStringExtra("PRODUCT_NAME")
        tvCategory.text = intent.getStringExtra("CATEGORY_NAME")
        tvProductDescription.text = intent.getStringExtra("PRODUCT_DESC")

        val price = intent.getDoubleExtra("PRODUCT_PRICE", 0.0)
        tvProductPrice.text = String.format(Locale.getDefault(), "%,.0f VNĐ", price)
        tvTotalPrice.text = String.format(Locale.getDefault(), "%,.0f VNĐ", price)

        val imageUrl = intent.getStringExtra("PRODUCT_IMAGE")
        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.ic_image_placeholder)
            .into(ivProductImage)
    }

    private fun loadProductDetails() {
        // FIX C5: Lưu reference để có thể cancel
        loadProductCall = RetrofitClient.getInstance(this).apiService.getDrinks()
        loadProductCall?.enqueue(object : Callback<ApiResponse<List<Drink>>> {
            override fun onResponse(call: Call<ApiResponse<List<Drink>>>, response: Response<ApiResponse<List<Drink>>>) {
                // FIX C5: Kiểm tra Activity còn tồn tại không
                if (isFinishing || isDestroyed) {
                    Log.d(TAG, "Activity destroyed, ignoring response")
                    return
                }
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val drinks = response.body()?.data ?: emptyList()
                    product = drinks.find { it.id == productId }

                    product?.let {
                        displayProductFullDetails(it)
                    } ?: run {
                        // FIX C2: Hiển thị lỗi cho user khi không tìm thấy sản phẩm
                        Toast.makeText(this@ProductDetailActivity, "Không tìm thấy thông tin sản phẩm", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // FIX C2: Xử lý lỗi response và hiển thị cho user
                    val errorMessage = try {
                        response.body()?.message ?: response.errorBody()?.string() ?: "Lỗi tải thông tin sản phẩm"
                    } catch (e: Exception) {
                        "Lỗi tải thông tin sản phẩm"
                    }
                    Toast.makeText(this@ProductDetailActivity, errorMessage, Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Error response: $errorMessage")
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<Drink>>>, t: Throwable) {
                // FIX C5: Kiểm tra Activity còn tồn tại không
                if (isFinishing || isDestroyed) {
                    Log.d(TAG, "Activity destroyed, ignoring failure")
                    return
                }
                
                // FIX C2: Hiển thị lỗi mạng cho user
                if (!call.isCanceled) {
                    val errorMessage = when {
                        t is java.net.UnknownHostException -> "Không có kết nối mạng"
                        t is java.net.SocketTimeoutException -> "Kết nối quá thời gian chờ"
                        else -> "Lỗi kết nối: ${t.message}"
                    }
                    Toast.makeText(this@ProductDetailActivity, errorMessage, Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Error loading product details", t)
                }
            }
        })
    }

    private fun displayProductFullDetails(prod: Drink) {
        val sizes = prod.sizes
        if (!sizes.isNullOrEmpty()) {
            val sizeNames = sizes.map {
                if (it.extraPrice > 0)
                    "${it.sizeName} (+${String.format("%,.0f", it.extraPrice)})"
                else it.sizeName
            }
            val sizeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sizeNames)
            spinnerSize.adapter = sizeAdapter

            spinnerSize.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    selectedSize = sizes[position]
                    updateTotalPrice()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            selectedSize = sizes[0]
        } else {
            spinnerSize.visibility = View.GONE
        }

        val toppings = prod.toppings
        if (!toppings.isNullOrEmpty()) {
            tvToppingLabel.visibility = View.VISIBLE
            layoutToppings.visibility = View.VISIBLE
            layoutToppings.removeAllViews()

            toppings.filter { it.isActive }.forEach { topping ->
                val checkBox = CheckBox(this)
                checkBox.text = "${topping.toppingName} (+${String.format("%,.0f", topping.price)})"

                checkBox.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) selectedToppings.add(topping)
                    else selectedToppings.remove(topping)
                    updateTotalPrice()
                }

                layoutToppings.addView(checkBox)
            }
        } else {
            tvToppingLabel.visibility = View.GONE
            layoutToppings.visibility = View.GONE
        }

        updateTotalPrice()
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

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

        btnAddToCart.setOnClickListener { addToCart(false) }
        btnConfirmOrder.setOnClickListener { addToCart(true) }
    }

    private fun updateTotalPrice() {
        val basePrice = product?.basePrice ?: intent.getDoubleExtra("PRODUCT_PRICE", 0.0)
        val sizeExtra = selectedSize?.extraPrice ?: 0.0
        val toppingTotal = selectedToppings.sumOf { it.price }

        val total = (basePrice + sizeExtra + toppingTotal) * quantity
        tvTotalPrice.text = String.format(Locale.getDefault(), "%,.0f VNĐ", total)
    }

    private fun addToCart(goToCart: Boolean) {
        val session = SessionManager(this)
        if (!session.isLoggedIn()) {
            Toast.makeText(this, "Vui lòng đăng nhập để đặt hàng", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            return
        }

        val request = AddToCartRequest(
            drinkId = productId.toLong(),
            sizeId = selectedSize?.id?.toLong() ?: 0L,
            quantity = quantity,
            toppingIds = selectedToppings.map { it.id.toLong() },
            note = ""
        )

        // FIX C5: Lưu reference để có thể cancel
        // Sử dụng API mới - không cần truyền userId (lấy từ JWT token)
        addToCartCall = RetrofitClient.getInstance(this).apiService.addToCart(request)
        addToCartCall?.enqueue(object : Callback<ApiResponse<Cart>> {
            override fun onResponse(call: Call<ApiResponse<Cart>>, response: Response<ApiResponse<Cart>>) {
                // FIX C5: Kiểm tra Activity còn tồn tại không
                if (isFinishing || isDestroyed) return
                
                if (response.isSuccessful && response.body()?.success == true) {
                    if (goToCart) {
                        startActivity(Intent(this@ProductDetailActivity, CartActivity::class.java))
                        finish()
                    } else {
                        showSuccessDialog()
                    }
                } else {
                    // FIX C2: Xử lý lỗi response tốt hơn
                    val errorMessage = try {
                        response.body()?.message ?: response.errorBody()?.string() ?: "Lỗi thêm giỏ hàng"
                    } catch (e: Exception) {
                        "Lỗi thêm giỏ hàng"
                    }
                    Toast.makeText(this@ProductDetailActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<Cart>>, t: Throwable) {
                // FIX C5: Kiểm tra Activity còn tồn tại không
                if (isFinishing || isDestroyed) return
                
                // FIX C2: Hiển thị lỗi mạng cho user
                if (!call.isCanceled) {
                    val errorMessage = when {
                        t is java.net.UnknownHostException -> "Không có kết nối mạng"
                        t is java.net.SocketTimeoutException -> "Kết nối quá thời gian chờ"
                        else -> "Lỗi kết nối: ${t.message}"
                    }
                    Toast.makeText(this@ProductDetailActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun showSuccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("Thành công")
            .setMessage("Đã thêm món vào giỏ hàng")
            .setPositiveButton("Đến giỏ hàng") { _, _ ->
                startActivity(Intent(this, CartActivity::class.java))
                finish()
            }
            .setNegativeButton("Tiếp tục mua") { d, _ -> d.dismiss() }
            .show()
    }
}
