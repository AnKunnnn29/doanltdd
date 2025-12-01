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
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class ProductDetailActivity : AppCompatActivity() {

    // Views
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
    private lateinit var spinnerSize: AutoCompleteTextView
    private lateinit var layoutToppings: LinearLayout
    private lateinit var btnBack: ImageButton
    private lateinit var spinnerBranch: AutoCompleteTextView
    private lateinit var spinnerSizeInputLayout: TextInputLayout
    private lateinit var tvToppingLabel: View
    private lateinit var tvCategory: TextView
    private lateinit var spinnerBranchLayout: TextInputLayout

    // Data
    private var product: Drink? = null
    private var quantity = 1
    private var selectedSize: DrinkSize? = null
    private val selectedToppings = mutableSetOf<DrinkTopping>()
    private var selectedBranchId: Long? = null

    companion object {
        private const val TAG = "ProductDetailActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)

        initViews()
        getProductDetails()
        setupListeners()
        fetchBranches()
    }

    private fun getLoggedInUserId(): Int {
        return SessionManager(this).getUserId()
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
        layoutToppings = findViewById(R.id.layoutToppings)
        btnBack = findViewById(R.id.btnBack)
        tvToppingLabel = findViewById(R.id.tv_topping_label)
        tvCategory = findViewById(R.id.detail_product_category)
        spinnerBranch = findViewById(R.id.spinnerBranch)
        spinnerBranchLayout = findViewById(R.id.spinner_branch_layout)
        spinnerSizeInputLayout = findViewById(R.id.spinner_size_layout)

        btnBack.setOnClickListener { finish() }
    }

    private fun getProductDetails() {
        intent?.getStringExtra("product")?.let { productJson ->
            try {
                product = Gson().fromJson(productJson, Drink::class.java)
                product?.let { displayProductDetails() } ?: throw IllegalStateException("Product is null after parsing")
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing product: ${e.message}")
                Toast.makeText(this, "Lỗi tải sản phẩm.", Toast.LENGTH_SHORT).show()
                finish()
            }
        } ?: run {
            Toast.makeText(this, "Không có thông tin sản phẩm.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun displayProductDetails() {
        product?.let { prod ->
            tvProductName.text = prod.name
            tvProductDescription.text = prod.description
            tvProductPrice.text = String.format(Locale.getDefault(), "%,.0f VNĐ", prod.basePrice)
            tvCategory.text = prod.categoryName ?: "Khác"

            prod.imageUrl?.let { Glide.with(this).load(it).placeholder(R.drawable.ic_image_placeholder).into(ivProductImage) }

            setupSizeSpinner()
            setupToppingCheckboxes()
            updateTotalPrice()
        }
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

        btnAddToCart.setOnClickListener { addToCart(redirectToCart = false) }
        btnConfirmOrder.setOnClickListener { addToCart(redirectToCart = true) }
    }

    private fun setupSizeSpinner() {
        product?.sizes?.takeIf { it.isNotEmpty() }?.let { sizes ->
            spinnerSizeInputLayout.visibility = View.VISIBLE
            val sizeOptions = sizes.map { size ->
                if (size.extraPrice > 0) "${size.sizeName} (+${String.format(Locale.getDefault(), "%,.0f", size.extraPrice)}đ)" else size.sizeName
            }
            val sizeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, sizeOptions)
            spinnerSize.setAdapter(sizeAdapter)

            spinnerSize.setText(sizeAdapter.getItem(0), false)
            selectedSize = sizes[0]

            spinnerSize.setOnItemClickListener { _, _, position, _ ->
                selectedSize = sizes[position]
                updateTotalPrice()
            }
        } ?: run {
            spinnerSizeInputLayout.visibility = View.GONE
            selectedSize = null
        }
    }

    private fun setupToppingCheckboxes() {
        product?.toppings?.takeIf { it.isNotEmpty() }?.let { toppings ->
            tvToppingLabel.visibility = View.VISIBLE
            layoutToppings.visibility = View.VISIBLE
            layoutToppings.removeAllViews()
            toppings.filter { it.isActive }.forEach { topping ->
                val checkBox = CheckBox(this).apply {
                    text = "${topping.toppingName} (+${String.format(Locale.getDefault(), "%,.0f", topping.price)}đ)"
                    setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) selectedToppings.add(topping) else selectedToppings.remove(topping)
                        updateTotalPrice()
                    }
                }
                layoutToppings.addView(checkBox)
            }
        } ?: run {
            tvToppingLabel.visibility = View.GONE
            layoutToppings.visibility = View.GONE
        }
    }

    private fun fetchBranches() {
        RetrofitClient.getInstance(this).apiService.getBranches().enqueue(object : Callback<ApiResponse<List<Branch>>> {
            override fun onResponse(call: Call<ApiResponse<List<Branch>>>, response: Response<ApiResponse<List<Branch>>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    response.body()?.data?.let { setupBranchSpinner(it) }
                } else {
                    spinnerBranchLayout.visibility = View.GONE
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<Branch>>>, t: Throwable) {
                spinnerBranchLayout.visibility = View.GONE
                Log.e(TAG, "Error fetching branches", t)
            }
        })
    }

    private fun setupBranchSpinner(branches: List<Branch>) {
        if (branches.isNotEmpty()) {
            spinnerBranchLayout.visibility = View.VISIBLE
            val branchOptions = branches.map { it.branchName ?: "N/A" }
            val branchAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, branchOptions)
            spinnerBranch.setAdapter(branchAdapter)

            spinnerBranch.setText(branchAdapter.getItem(0), false)
            selectedBranchId = branches[0].id?.toLong()

            spinnerBranch.setOnItemClickListener { _, _, position, _ ->
                selectedBranchId = branches[position].id?.toLong()
            }
        } else {
            spinnerBranchLayout.visibility = View.GONE
        }
    }

    private fun updateTotalPrice() {
        product?.let { prod ->
            val sizeExtra = selectedSize?.extraPrice ?: 0.0
            val toppingTotal = selectedToppings.sumOf { it.price }
            val total = (prod.basePrice + sizeExtra + toppingTotal) * quantity
            tvTotalPrice.text = String.format(Locale.getDefault(), "%,.0f VNĐ", total)
        }
    }

    private fun addToCart(redirectToCart: Boolean = false) {
        val userId = getLoggedInUserId()
        if (userId == -1) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedBranchId == null) {
            Toast.makeText(this, "Vui lòng chọn chi nhánh", Toast.LENGTH_SHORT).show()
            return
        }

        val request = AddToCartRequest(
            drinkId = product?.id?.toLong() ?: 0L,
            sizeId = selectedSize?.id?.toLong() ?: 0L,
            quantity = quantity,
            toppingIds = selectedToppings.map { it.id.toLong() }.takeIf { it.isNotEmpty() },
            note = null, // Note is currently not supported in this UI
            branchId = selectedBranchId!!
        )

        setLoading(true, redirectToCart)

        RetrofitClient.getInstance(this).apiService.addToCart(userId.toLong(), request).enqueue(object : Callback<ApiResponse<Cart>> {
            override fun onResponse(call: Call<ApiResponse<Cart>>, response: Response<ApiResponse<Cart>>) {
                setLoading(false, redirectToCart)
                if (response.isSuccessful && response.body()?.success == true) {
                    if (redirectToCart) {
                        startActivity(Intent(this@ProductDetailActivity, CartActivity::class.java))
                        finish()
                    } else {
                        showSuccessDialog()
                    }
                } else {
                    Toast.makeText(this@ProductDetailActivity, response.body()?.message ?: "Lỗi không xác định", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<Cart>>, t: Throwable) {
                setLoading(false, redirectToCart)
                Toast.makeText(this@ProductDetailActivity, "Không thể kết nối đến máy chủ", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setLoading(isLoading: Boolean, isConfirmAction: Boolean) {
        btnAddToCart.isEnabled = !isLoading
        btnConfirmOrder.isEnabled = !isLoading
        if (isLoading) {
            if (isConfirmAction) {
                btnConfirmOrder.text = "Đang xử lý..."
            } else {
                btnAddToCart.text = "Đang thêm..."
            }
        } else {
            btnAddToCart.text = "Thêm vào giỏ"
            btnConfirmOrder.text = "Mua ngay"
        }
    }

    private fun showSuccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("Thêm vào giỏ hàng thành công!")
            .setMessage("Bạn có muốn xem giỏ hàng ngay không?\n\n⚠️ Giỏ hàng sẽ tự động xóa sau 5 phút.")
            .setPositiveButton("Xem giỏ hàng") { _, _ -> startActivity(Intent(this, CartActivity::class.java)) }
            .setNegativeButton("Tiếp tục mua") { d, _ -> d.dismiss() }
            .show()
    }
}
