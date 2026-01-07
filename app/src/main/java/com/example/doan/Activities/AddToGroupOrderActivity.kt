package com.example.doan.Activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doan.Adapters.CombinedVoucherAdapter
import com.example.doan.Models.*
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.DataCache
import com.example.doan.Utils.LoadingDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class AddToGroupOrderActivity : AppCompatActivity() {

    private lateinit var ivDrinkImage: ImageView
    private lateinit var tvDrinkName: TextView
    private lateinit var tvDrinkPrice: TextView
    private lateinit var chipGroupSize: ChipGroup
    private lateinit var llToppings: LinearLayout
    private lateinit var tvToppingsLabel: TextView
    private lateinit var btnDecrease: ImageButton
    private lateinit var btnIncrease: ImageButton
    private lateinit var tvQuantity: TextView
    private lateinit var etNote: EditText
    private lateinit var btnSelectVoucher: Button
    private lateinit var tvSelectedVoucher: TextView
    private lateinit var btnAddToGroup: Button
    private lateinit var loadingDialog: LoadingDialog

    private var groupOrderId: Long = 0
    private var drinkId: Long = 0
    private var drinkName: String = ""
    private var drinkPrice: Double = 0.0
    private var drinkImage: String? = null
    
    private var selectedSize: String = "M"
    private var quantity: Int = 1
    private var product: Product? = null
    private val selectedToppings = mutableSetOf<DrinkTopping>()
    private var selectedVoucher: Voucher? = null
    private var selectedSpinVoucher: SpinRewardDto? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_to_group_order)

        groupOrderId = intent.getLongExtra("GROUP_ORDER_ID", 0)
        drinkId = intent.getLongExtra("DRINK_ID", 0)
        drinkName = intent.getStringExtra("DRINK_NAME") ?: ""
        drinkPrice = intent.getDoubleExtra("DRINK_PRICE", 0.0)
        drinkImage = intent.getStringExtra("DRINK_IMAGE")

        if (groupOrderId == 0L || drinkId == 0L) {
            Toast.makeText(this, "Lỗi: Thiếu thông tin", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadingDialog = LoadingDialog(this)
        initViews()
        loadProductDetails()
    }

    private fun initViews() {
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).apply {
            setNavigationOnClickListener { finish() }
        }

        ivDrinkImage = findViewById(R.id.iv_drink_image)
        tvDrinkName = findViewById(R.id.tv_drink_name)
        tvDrinkPrice = findViewById(R.id.tv_drink_price)
        chipGroupSize = findViewById(R.id.chip_group_size)
        llToppings = findViewById(R.id.ll_toppings)
        tvToppingsLabel = findViewById(R.id.tv_toppings_label)
        btnDecrease = findViewById(R.id.btn_decrease)
        btnIncrease = findViewById(R.id.btn_increase)
        tvQuantity = findViewById(R.id.tv_quantity)
        etNote = findViewById(R.id.et_note)
        btnSelectVoucher = findViewById(R.id.btn_select_voucher)
        tvSelectedVoucher = findViewById(R.id.tv_selected_voucher)
        btnAddToGroup = findViewById(R.id.btn_add_to_group)

        // Set initial data
        tvDrinkName.text = drinkName
        tvDrinkPrice.text = String.format(Locale.getDefault(), "%,.0f VNĐ", drinkPrice)
        
        if (!drinkImage.isNullOrEmpty()) {
            Glide.with(this).load(drinkImage).into(ivDrinkImage)
        }

        // Quantity controls
        btnDecrease.setOnClickListener {
            if (quantity > 1) {
                quantity--
                tvQuantity.text = quantity.toString()
                updateTotalPrice()
            }
        }

        btnIncrease.setOnClickListener {
            if (quantity < 20) {
                quantity++
                tvQuantity.text = quantity.toString()
                updateTotalPrice()
            }
        }

        btnSelectVoucher.setOnClickListener {
            showVoucherSelectionDialog()
        }

        btnAddToGroup.setOnClickListener {
            addToGroupOrder()
        }
    }

    private fun loadProductDetails() {
        // Tìm product từ cache để lấy sizes và toppings
        product = DataCache.products?.find { it.id == drinkId.toInt() }
        
        if (product != null) {
            setupSizeChips(product!!.sizes)
            setupToppings(product!!.toppings)
        } else {
            // Load từ API nếu không có trong cache
            loadProductFromApi()
        }
    }

    private fun loadProductFromApi() {
        loadingDialog.show("Đang tải thông tin sản phẩm...")

        RetrofitClient.getInstance(this).apiService.getDrinkById(drinkId.toInt())
            .enqueue(object : Callback<ApiResponse<Drink>> {
                override fun onResponse(
                    call: Call<ApiResponse<Drink>>,
                    response: Response<ApiResponse<Drink>>
                ) {
                    loadingDialog.dismiss()
                    if (response.isSuccessful && response.body()?.success == true) {
                        val drink = response.body()?.data
                        if (drink != null) {
                            // Convert Drink to Product
                            product = Product(
                                id = drink.id,
                                name = drink.name,
                                price = drink.basePrice,
                                imageUrl = drink.imageUrl,
                                sizes = drink.sizes,
                                toppings = drink.toppings
                            )
                            setupSizeChips(product!!.sizes)
                            setupToppings(product!!.toppings)
                        } else {
                            Toast.makeText(this@AddToGroupOrderActivity, "Không tìm thấy thông tin sản phẩm.", Toast.LENGTH_SHORT).show()
                            setupDefaultSizeChips()
                        }
                    } else {
                        Toast.makeText(this@AddToGroupOrderActivity, response.body()?.message ?: "Lỗi tải sản phẩm", Toast.LENGTH_SHORT).show()
                        setupDefaultSizeChips()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Drink>>, t: Throwable) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@AddToGroupOrderActivity, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                    setupDefaultSizeChips()
                }
            })
    }

    private fun setupSizeChips(sizes: List<DrinkSize>?) {
        chipGroupSize.removeAllViews()
        
        if (sizes.isNullOrEmpty()) {
            setupDefaultSizeChips()
            return
        }

        sizes.forEachIndexed { index, size ->
            val chip = Chip(this).apply {
                text = "${size.sizeName} (+${String.format(Locale.getDefault(), "%,.0f", size.extraPrice)})"
                isCheckable = true
                isChecked = index == 0
                tag = size.sizeName
            }
            chipGroupSize.addView(chip)
            
            if (index == 0) {
                selectedSize = size.sizeName ?: "M"
            }
        }

        chipGroupSize.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chip = group.findViewById<Chip>(checkedIds.first())
                selectedSize = chip.tag as String
                updateTotalPrice()
            }
        }
        
        updateTotalPrice()
    }

    private fun setupDefaultSizeChips() {
        chipGroupSize.removeAllViews()
        listOf("S", "M", "L").forEachIndexed { index, sizeName ->
            val chip = Chip(this).apply {
                text = sizeName
                isCheckable = true
                isChecked = sizeName == "M"
                tag = sizeName
            }
            chipGroupSize.addView(chip)
        }
        selectedSize = "M"

        chipGroupSize.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chip = group.findViewById<Chip>(checkedIds.first())
                selectedSize = chip.tag as String
            }
        }
    }

    private fun setupToppings(toppings: List<DrinkTopping>?) {
        llToppings.removeAllViews()
        
        if (toppings.isNullOrEmpty()) {
            tvToppingsLabel.visibility = View.GONE
            llToppings.visibility = View.GONE
            return
        }

        tvToppingsLabel.visibility = View.VISIBLE
        llToppings.visibility = View.VISIBLE

        toppings.forEach { topping ->
            val checkBox = CheckBox(this).apply {
                text = "${topping.toppingName} (+${String.format(Locale.getDefault(), "%,.0f VNĐ", topping.price)})"
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedToppings.add(topping)
                    } else {
                        selectedToppings.remove(topping)
                    }
                    updateTotalPrice()
                }
            }
            llToppings.addView(checkBox)
        }
    }

    private fun showVoucherSelectionDialog() {
        val loadingDialog = LoadingDialog(this)
        loadingDialog.show("Đang tải voucher...")

        var normalVouchers: List<Voucher> = emptyList()
        var spinVouchers: List<SpinRewardDto> = emptyList()
        var loadedCount = 0

        // Load voucher thường
        RetrofitClient.getInstance(this).apiService.getActivePromotions()
            .enqueue(object : Callback<ApiResponse<List<Voucher>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<Voucher>>>,
                    response: Response<ApiResponse<List<Voucher>>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        normalVouchers = response.body()?.data ?: emptyList()
                    }
                    loadedCount++
                    if (loadedCount >= 2) {
                        loadingDialog.dismiss()
                        showCombinedVoucherDialog(normalVouchers, spinVouchers)
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<Voucher>>>, t: Throwable) {
                    loadedCount++
                    if (loadedCount >= 2) {
                        loadingDialog.dismiss()
                        showCombinedVoucherDialog(normalVouchers, spinVouchers)
                    }
                }
            })

        // Load voucher spin
        RetrofitClient.getInstance(this).apiService.getAvailableRewards()
            .enqueue(object : Callback<ApiResponse<List<SpinRewardDto>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<SpinRewardDto>>>,
                    response: Response<ApiResponse<List<SpinRewardDto>>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        spinVouchers = response.body()?.data ?: emptyList()
                    }
                    loadedCount++
                    if (loadedCount >= 2) {
                        loadingDialog.dismiss()
                        showCombinedVoucherDialog(normalVouchers, spinVouchers)
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<SpinRewardDto>>>, t: Throwable) {
                    loadedCount++
                    if (loadedCount >= 2) {
                        loadingDialog.dismiss()
                        showCombinedVoucherDialog(normalVouchers, spinVouchers)
                    }
                }
            })
    }

    private fun showCombinedVoucherDialog(
        normalVouchers: List<Voucher>,
        spinVouchers: List<SpinRewardDto>
    ) {
        if (normalVouchers.isEmpty() && spinVouchers.isEmpty()) {
            Toast.makeText(this, "Không có voucher khả dụng", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_voucher_selection, null)
        val rvVouchers: RecyclerView = dialogView.findViewById(R.id.rv_vouchers)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Chọn Voucher")
            .setView(dialogView)
            .setNegativeButton("Đóng", null)
            .create()

        val combinedItems = mutableListOf<Any>()
        combinedItems.addAll(normalVouchers)
        combinedItems.addAll(spinVouchers)

        val combinedAdapter = CombinedVoucherAdapter(combinedItems) { item ->
            when (item) {
                is Voucher -> {
                    selectedVoucher = item
                    selectedSpinVoucher = null 
                    tvSelectedVoucher.text = item.description ?: ""
                }
                is SpinRewardDto -> {
                    selectedSpinVoucher = item
                    selectedVoucher = null
                    tvSelectedVoucher.text = item.discountLabel ?: ""
                }
            }
            tvSelectedVoucher.visibility = View.VISIBLE
            updateTotalPrice()
            dialog.dismiss()
        }
        rvVouchers.layoutManager = LinearLayoutManager(this)
        rvVouchers.adapter = combinedAdapter

        dialog.show()
    }
    
    private fun updateTotalPrice() {
        var total = product?.price ?: drinkPrice
        
        product?.sizes?.find { it.sizeName == selectedSize }?.let {
            total += it.extraPrice
        }

        selectedToppings.forEach { 
            total += it.price
        }

        total *= quantity

        // TODO: Apply voucher discount

        tvDrinkPrice.text = String.format(Locale.getDefault(), "%,.0f VNĐ", total)
    }

    private fun addToGroupOrder() {
        val selectedToppingIds = selectedToppings.map { it.id.toLong() }

        val request = AddGroupOrderItemRequest(
            drinkId = drinkId,
            quantity = quantity,
            sizeName = selectedSize,
            toppingIds = selectedToppingIds,
            note = etNote.text.toString().takeIf { it.isNotEmpty() },
            promotionCode = selectedVoucher?.code,
            spinVoucherCode = selectedSpinVoucher?.voucherCode
        )

        loadingDialog.show("Đang thêm vào nhóm...")

        RetrofitClient.getInstance(this).apiService.addGroupOrderItem(groupOrderId, request)
            .enqueue(object : Callback<ApiResponse<GroupOrderDto>> {
                override fun onResponse(
                    call: Call<ApiResponse<GroupOrderDto>>,
                    response: Response<ApiResponse<GroupOrderDto>>
                ) {
                    loadingDialog.dismiss()
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@AddToGroupOrderActivity, "Thêm thành công", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@AddToGroupOrderActivity, response.body()?.message ?: "Lỗi thêm món", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<GroupOrderDto>>, t: Throwable) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@AddToGroupOrderActivity, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                }
            })
    }
}