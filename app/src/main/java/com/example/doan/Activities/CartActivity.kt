package com.example.doan.Activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Adapters.CartAdapter
import com.example.doan.Models.ApiResponse
import com.example.doan.Models.Cart
import com.example.doan.Models.CartItem
import com.example.doan.Models.CreateOrderRequest
import com.example.doan.Models.Order
import com.example.doan.Models.Store
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.example.doan.Utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class CartActivity : AppCompatActivity(), CartAdapter.OnCartItemChangeListener {

    private lateinit var rvCartItems: RecyclerView
    private lateinit var cartAdapter: CartAdapter
    private lateinit var tvEmptyCart: TextView
    private lateinit var tvTotalPrice: TextView
    private lateinit var btnCheckout: Button
    private lateinit var cbSelectAll: CheckBox
    private lateinit var btnDeleteSelected: Button
    private lateinit var spinnerBranch: Spinner
    private lateinit var spinnerPaymentMethod: Spinner
    
    private var cartItems = mutableListOf<CartItem>()
    private var stores = listOf<Store>()
    private val paymentMethods = listOf("COD", "Momo", "ZaloPay", "Banking")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        initViews()
        setupRecyclerView()
        setupListeners()
        loadCart()
        loadStores()
    }

    private fun initViews() {
        rvCartItems = findViewById(R.id.rv_cart_items)
        tvEmptyCart = findViewById(R.id.tv_empty_cart)
        tvTotalPrice = findViewById(R.id.tv_total_price_cart)
        btnCheckout = findViewById(R.id.btn_checkout)
        cbSelectAll = findViewById(R.id.cb_select_all)
        btnDeleteSelected = findViewById(R.id.btn_delete_selected)
        spinnerBranch = findViewById(R.id.spinner_branch_cart)
        spinnerPaymentMethod = findViewById(R.id.spinner_payment_method)
        
        // Setup Payment Method Spinner
        val paymentAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, paymentMethods)
        spinnerPaymentMethod.adapter = paymentAdapter
        
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_cart).setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(this, cartItems, this)
        rvCartItems.layoutManager = LinearLayoutManager(this)
        rvCartItems.adapter = cartAdapter
    }

    private fun setupListeners() {
        cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
            cartAdapter.selectAll(isChecked)
        }

        btnDeleteSelected.setOnClickListener {
            val selectedItems = cartAdapter.getSelectedItems()
            if (selectedItems.isNotEmpty()) {
                showDeleteConfirmationDialog(selectedItems)
            }
        }
        
        btnCheckout.setOnClickListener { 
            val selectedItems = cartAdapter.getSelectedItems()
            if (selectedItems.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn sản phẩm để mua", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val selectedStorePosition = spinnerBranch.selectedItemPosition
            if (selectedStorePosition == Spinner.INVALID_POSITION || stores.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn chi nhánh", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val selectedStore = stores[selectedStorePosition]
            
            val selectedPaymentMethod = spinnerPaymentMethod.selectedItem.toString()

            createOrder(selectedItems, selectedStore.id, selectedPaymentMethod)
        }
    }

    private fun loadCart() {
        val userId = SessionManager(this).getUserId()
        if (userId == -1) return

        RetrofitClient.getInstance(this).apiService.getCart(userId.toLong()).enqueue(object : Callback<ApiResponse<Cart>> {
            override fun onResponse(call: Call<ApiResponse<Cart>>, response: Response<ApiResponse<Cart>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val loadedItems = response.body()?.data?.items ?: emptyList()
                    cartItems.clear()
                    val itemsWithSelection = loadedItems.map { it.apply { isSelected = false } }
                    cartItems.addAll(itemsWithSelection)
                    cartAdapter.notifyDataSetChanged()
                    updateUi()
                }
            }

            override fun onFailure(call: Call<ApiResponse<Cart>>, t: Throwable) {
                Log.e("CartActivity", "Error loading cart", t)
            }
        })
    }
    
    private fun loadStores() {
        RetrofitClient.getInstance(this).apiService.getStores().enqueue(object : Callback<ApiResponse<List<Store>>> {
            override fun onResponse(call: Call<ApiResponse<List<Store>>>, response: Response<ApiResponse<List<Store>>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    stores = response.body()?.data ?: emptyList()
                    val storeNames = stores.map { it.storeName }
                    val adapter = ArrayAdapter(this@CartActivity, android.R.layout.simple_spinner_dropdown_item, storeNames)
                    spinnerBranch.adapter = adapter
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<Store>>>, t: Throwable) {
                Log.e("CartActivity", "Error loading stores", t)
            }
        })
    }

    private fun updateUi() {
        if (cartItems.isEmpty()) {
            tvEmptyCart.visibility = View.VISIBLE
            rvCartItems.visibility = View.GONE
        } else {
            tvEmptyCart.visibility = View.GONE
            rvCartItems.visibility = View.VISIBLE
        }
        calculateTotalPrice()
    }

    private fun calculateTotalPrice() {
        val selectedItems = cartAdapter.getSelectedItems()
        val total = selectedItems.sumOf { 
            (it.unitPrice ?: 0.0) * (it.quantity ?: 1)
        }
        tvTotalPrice.text = String.format(Locale.getDefault(), "%,.0f VNĐ", total)
    }

    override fun onItemSelectedChanged() {
        calculateTotalPrice()
    }

    override fun onItemDeleted(item: CartItem) {
        val userId = SessionManager(this).getUserId()
        if (userId == -1) return

        item.id?.let {
            RetrofitClient.getInstance(this).apiService.removeCartItem(it, userId.toLong()).enqueue(object : Callback<ApiResponse<Void>> {
                override fun onResponse(call: Call<ApiResponse<Void>>, response: Response<ApiResponse<Void>>) {
                    if (response.isSuccessful) {
                        loadCart()
                    }
                }
                override fun onFailure(call: Call<ApiResponse<Void>>, t: Throwable) {}
            })
        }
    }
    
    private fun showDeleteConfirmationDialog(itemsToDelete: List<CartItem>){
        AlertDialog.Builder(this)
            .setTitle("Xác nhận xóa")
            .setMessage("Bạn có chắc muốn xóa ${itemsToDelete.size} mục đã chọn?")
            .setPositiveButton("Xóa") { _, _ ->
                cartAdapter.deleteSelectedItems()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
    
    private fun createOrder(items: List<CartItem>, storeId: Int, paymentMethod: String) {
        val orderItems = items.map { item ->
            com.example.doan.Models.OrderItemRequest(
                drinkId = item.drinkId!!.toLong(), 
                quantity = item.quantity ?: 1,
                sizeName = item.sizeName ?: "M",
                toppingIds = item.toppings?.mapNotNull { topping -> topping.id.toLong() } ?: emptyList(),
                note = item.note
            )
        }
        
        val request = CreateOrderRequest(
            storeId = storeId.toLong(),
            items = orderItems,
            type = "DELIVERY", 
            paymentMethod = paymentMethod
        )

        RetrofitClient.getInstance(this).apiService.createOrder(request).enqueue(object: Callback<ApiResponse<Order>> {
            override fun onResponse(call: Call<ApiResponse<Order>>, response: Response<ApiResponse<Order>>) {
                if(response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@CartActivity, "Đặt hàng thành công!", Toast.LENGTH_SHORT).show()
                    clearCartOnServer()
                    val intent = Intent(this@CartActivity, MainActivity::class.java).apply {
                        putExtra("SELECTED_ITEM", R.id.nav_order)
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@CartActivity, "Đặt hàng thất bại: ${response.body()?.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<Order>>, t: Throwable) {
                Toast.makeText(this@CartActivity, "Lỗi: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun clearCartOnServer() {
        val userId = SessionManager(this).getUserId()
        if (userId == -1) return

        RetrofitClient.getInstance(this).apiService.clearCart(userId.toLong()).enqueue(object: Callback<ApiResponse<Void>> {
            override fun onResponse(call: Call<ApiResponse<Void>>, response: Response<ApiResponse<Void>>) {
                if (!response.isSuccessful) {
                    Log.e("CartActivity", "Failed to clear cart on server")
                }
            }

            override fun onFailure(call: Call<ApiResponse<Void>>, t: Throwable) {
                Log.e("CartActivity", "Error clearing cart on server", t)
            }
        })
    }
}
