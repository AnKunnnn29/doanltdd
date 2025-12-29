package com.example.doan.Activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.doan.Models.*
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.LoadingDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CreateGroupOrderActivity : AppCompatActivity() {

    private lateinit var etGroupName: TextInputEditText
    private lateinit var spinnerStore: Spinner
    private lateinit var rgOrderType: RadioGroup
    private lateinit var rbPickup: RadioButton
    private lateinit var rbDelivery: RadioButton
    private lateinit var tilAddress: TextInputLayout
    private lateinit var etAddress: TextInputEditText
    private lateinit var seekbarMaxMembers: SeekBar
    private lateinit var tvMaxMembers: TextView
    private lateinit var spinnerExpiration: Spinner
    private lateinit var btnCreate: Button
    private lateinit var loadingDialog: LoadingDialog

    private var storeList = mutableListOf<Store>()
    private val expirationOptions = listOf(
        Pair("15 phút", 15),
        Pair("30 phút", 30),
        Pair("1 giờ", 60),
        Pair("2 giờ", 120),
        Pair("4 giờ", 240)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_group_order)

        loadingDialog = LoadingDialog(this)
        initViews()
        setupListeners()
        loadStores()
    }

    private fun initViews() {
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).setNavigationOnClickListener {
            finish()
        }

        etGroupName = findViewById(R.id.et_group_name)
        spinnerStore = findViewById(R.id.spinner_store)
        rgOrderType = findViewById(R.id.rg_order_type)
        rbPickup = findViewById(R.id.rb_pickup)
        rbDelivery = findViewById(R.id.rb_delivery)
        tilAddress = findViewById(R.id.til_address)
        etAddress = findViewById(R.id.et_address)
        seekbarMaxMembers = findViewById(R.id.seekbar_max_members)
        tvMaxMembers = findViewById(R.id.tv_max_members)
        spinnerExpiration = findViewById(R.id.spinner_expiration)
        btnCreate = findViewById(R.id.btn_create)

        // Setup expiration spinner
        val expirationAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            expirationOptions.map { it.first }
        )
        spinnerExpiration.adapter = expirationAdapter
        spinnerExpiration.setSelection(2) // Default 1 hour
    }

    private fun setupListeners() {
        rgOrderType.setOnCheckedChangeListener { _, checkedId ->
            tilAddress.visibility = if (checkedId == R.id.rb_delivery) View.VISIBLE else View.GONE
        }

        seekbarMaxMembers.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvMaxMembers.text = "$progress người"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnCreate.setOnClickListener { createGroupOrder() }
    }

    private fun loadStores() {
        val cachedStores = com.example.doan.Utils.DataCache.stores
        if (!cachedStores.isNullOrEmpty()) {
            setupStoreSpinner(cachedStores)
            return
        }

        RetrofitClient.getInstance(this).apiService.getStores()
            .enqueue(object : Callback<ApiResponse<List<Store>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<Store>>>,
                    response: Response<ApiResponse<List<Store>>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val stores = response.body()?.data ?: emptyList()
                        com.example.doan.Utils.DataCache.stores = stores
                        setupStoreSpinner(stores)
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<Store>>>, t: Throwable) {
                    Toast.makeText(this@CreateGroupOrderActivity, 
                        "Không thể tải danh sách chi nhánh", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun setupStoreSpinner(stores: List<Store>) {
        storeList.clear()
        storeList.addAll(stores)
        val storeNames = stores.map { it.storeName ?: "Chi nhánh ${it.id}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, storeNames)
        spinnerStore.adapter = adapter
    }

    private fun createGroupOrder() {
        val orderType = if (rbDelivery.isChecked) "DELIVERY" else "PICKUP"
        val address = etAddress.text.toString().trim()

        if (orderType == "DELIVERY" && address.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập địa chỉ giao hàng", Toast.LENGTH_SHORT).show()
            return
        }

        loadingDialog.show("Đang tạo phiên...")

        val request = CreateGroupOrderRequest(
            name = etGroupName.text.toString().takeIf { it.isNotEmpty() },
            storeId = if (storeList.isNotEmpty()) storeList[spinnerStore.selectedItemPosition].id?.toLong() else null,
            orderType = orderType,
            deliveryAddress = if (orderType == "DELIVERY") address else null,
            maxMembers = seekbarMaxMembers.progress,
            expirationMinutes = expirationOptions[spinnerExpiration.selectedItemPosition].second
        )

        RetrofitClient.getInstance(this).apiService.createGroupOrder(request)
            .enqueue(object : Callback<ApiResponse<GroupOrderDto>> {
                override fun onResponse(
                    call: Call<ApiResponse<GroupOrderDto>>,
                    response: Response<ApiResponse<GroupOrderDto>>
                ) {
                    loadingDialog.dismiss()
                    if (response.isSuccessful && response.body()?.success == true) {
                        val groupOrder = response.body()?.data
                        Toast.makeText(this@CreateGroupOrderActivity, 
                            "Tạo phiên thành công!", Toast.LENGTH_SHORT).show()
                        
                        // Navigate to GroupOrderActivity
                        val intent = Intent(this@CreateGroupOrderActivity, GroupOrderActivity::class.java)
                        intent.putExtra("GROUP_ORDER_ID", groupOrder?.id)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@CreateGroupOrderActivity,
                            response.body()?.message ?: "Lỗi tạo phiên", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<GroupOrderDto>>, t: Throwable) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@CreateGroupOrderActivity, 
                        "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
