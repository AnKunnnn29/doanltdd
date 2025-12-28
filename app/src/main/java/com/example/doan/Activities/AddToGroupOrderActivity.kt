package com.example.doan.Activities

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
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
    private lateinit var btnDecrease: ImageButton
    private lateinit var btnIncrease: ImageButton
    private lateinit var tvQuantity: TextView
    private lateinit var etNote: EditText
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
        btnDecrease = findViewById(R.id.btn_decrease)
        btnIncrease = findViewById(R.id.btn_increase)
        tvQuantity = findViewById(R.id.tv_quantity)
        etNote = findViewById(R.id.et_note)
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

        btnAddToGroup.setOnClickListener {
            addToGroupOrder()
        }
    }

    private fun loadProductDetails() {
        // Tìm product từ cache để lấy sizes
        product = DataCache.products?.find { it.id == drinkId.toInt() }
        
        if (product != null) {
            setupSizeChips(product!!.sizes)
        } else {
            // Nếu không có trong cache, tạo size mặc định
            setupDefaultSizeChips()
        }
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

    private fun updateTotalPrice() {
        var totalPrice = drinkPrice
        
        // Add size extra price
        product?.sizes?.find { it.sizeName == selectedSize }?.let { size ->
            totalPrice += size.extraPrice ?: 0.0
        }
        
        totalPrice *= quantity
        
        btnAddToGroup.text = "Thêm vào đơn nhóm - ${String.format(Locale.getDefault(), "%,.0f VNĐ", totalPrice)}"
    }

    private fun addToGroupOrder() {
        loadingDialog.show("Đang thêm món...")

        val request = AddGroupOrderItemRequest(
            drinkId = drinkId,
            quantity = quantity,
            sizeName = selectedSize,
            toppingIds = null,
            note = etNote.text.toString().takeIf { it.isNotEmpty() }
        )

        RetrofitClient.getInstance(this).apiService.addGroupOrderItem(groupOrderId, request)
            .enqueue(object : Callback<ApiResponse<GroupOrderDto>> {
                override fun onResponse(
                    call: Call<ApiResponse<GroupOrderDto>>,
                    response: Response<ApiResponse<GroupOrderDto>>
                ) {
                    loadingDialog.dismiss()
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@AddToGroupOrderActivity, 
                            "Đã thêm $drinkName vào đơn nhóm!", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(this@AddToGroupOrderActivity,
                            response.body()?.message ?: "Lỗi thêm món", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<GroupOrderDto>>, t: Throwable) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@AddToGroupOrderActivity, 
                        "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
