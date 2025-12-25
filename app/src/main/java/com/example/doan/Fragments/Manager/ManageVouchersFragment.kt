package com.example.doan.Fragments.Manager

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.doan.Adapters.VoucherManagerAdapter
import com.example.doan.Models.*
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

class ManageVouchersFragment : Fragment() {

    private lateinit var rvVouchers: RecyclerView
    private lateinit var voucherAdapter: VoucherManagerAdapter
    private lateinit var btnAddVoucher: MaterialButton
    private lateinit var emptyState: View
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var etSearchVoucher: EditText
    private lateinit var tvTotalVouchers: TextView
    private lateinit var tvActiveVouchers: TextView
    private lateinit var chipAll: Chip
    private lateinit var chipActive: Chip
    private lateinit var chipExpired: Chip
    
    private val allVouchers = mutableListOf<Voucher>()
    private val filteredVouchers = mutableListOf<Voucher>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    private var currentFilter = "ALL"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_manage_vouchers, container, false)
        initViews(view)
        setupRecyclerView()
        setupListeners()
        loadVouchers()
        return view
    }

    private fun initViews(view: View) {
        rvVouchers = view.findViewById(R.id.rv_vouchers)
        btnAddVoucher = view.findViewById(R.id.btn_add_voucher)
        emptyState = view.findViewById(R.id.tv_empty_vouchers)
        progressBar = view.findViewById(R.id.progress_bar)
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        etSearchVoucher = view.findViewById(R.id.et_search_voucher)
        tvTotalVouchers = view.findViewById(R.id.tv_total_vouchers)
        tvActiveVouchers = view.findViewById(R.id.tv_active_vouchers)
        chipAll = view.findViewById(R.id.chip_all)
        chipActive = view.findViewById(R.id.chip_active)
        chipExpired = view.findViewById(R.id.chip_expired)
    }

    private fun setupRecyclerView() {
        voucherAdapter = VoucherManagerAdapter(
            filteredVouchers,
            onEditClick = { voucher -> showEditVoucherDialog(voucher) },
            onDeleteClick = { voucher -> showDeleteConfirmation(voucher) },
            onToggleClick = { voucher -> toggleVoucherStatus(voucher) }
        )
        rvVouchers.layoutManager = LinearLayoutManager(requireContext())
        rvVouchers.adapter = voucherAdapter
    }


    private fun setupListeners() {
        btnAddVoucher.setOnClickListener { showAddVoucherDialog() }
        
        swipeRefresh.setColorSchemeResources(R.color.wine_primary)
        swipeRefresh.setOnRefreshListener { loadVouchers() }
        
        etSearchVoucher.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterVouchers(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        
        chipAll.setOnClickListener {
            currentFilter = "ALL"
            chipAll.isChecked = true
            chipActive.isChecked = false
            chipExpired.isChecked = false
            filterVouchers(etSearchVoucher.text.toString())
        }
        
        chipActive.setOnClickListener {
            currentFilter = "ACTIVE"
            chipAll.isChecked = false
            chipActive.isChecked = true
            chipExpired.isChecked = false
            filterVouchers(etSearchVoucher.text.toString())
        }
        
        chipExpired.setOnClickListener {
            currentFilter = "EXPIRED"
            chipAll.isChecked = false
            chipActive.isChecked = false
            chipExpired.isChecked = true
            filterVouchers(etSearchVoucher.text.toString())
        }
    }
    
    private fun filterVouchers(searchQuery: String) {
        filteredVouchers.clear()
        val now = Date()
        
        allVouchers.forEach { voucher ->
            val passesStatusFilter = when (currentFilter) {
                "ACTIVE" -> voucher.isActive && !isExpired(voucher, now)
                "EXPIRED" -> isExpired(voucher, now) || !voucher.isActive
                else -> true
            }
            
            val passesSearchFilter = if (searchQuery.isEmpty()) true
            else voucher.code.lowercase().contains(searchQuery.lowercase()) ||
                 voucher.description?.lowercase()?.contains(searchQuery.lowercase()) == true
            
            if (passesStatusFilter && passesSearchFilter) {
                filteredVouchers.add(voucher)
            }
        }
        
        voucherAdapter.notifyDataSetChanged()
        updateEmptyState()
    }
    
    private fun isExpired(voucher: Voucher, now: Date): Boolean {
        return try {
            val endDate = dateFormat.parse(voucher.endDate)
            endDate?.before(now) == true
        } catch (e: Exception) { false }
    }

    private fun loadVouchers() {
        progressBar.visibility = View.VISIBLE
        
        RetrofitClient.getInstance(requireContext()).apiService.getAllPromotions()
            .enqueue(object : Callback<ApiResponse<List<Voucher>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<Voucher>>>,
                    response: Response<ApiResponse<List<Voucher>>>
                ) {
                    if (!isAdded) return
                    progressBar.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        val data = response.body()?.data ?: emptyList()
                        allVouchers.clear()
                        allVouchers.addAll(data)
                        updateStats()
                        filterVouchers(etSearchVoucher.text.toString())
                    } else {
                        context?.let { Toast.makeText(it, "Không thể tải danh sách voucher", Toast.LENGTH_SHORT).show() }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<Voucher>>>, t: Throwable) {
                    if (!isAdded) return
                    progressBar.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                    context?.let { Toast.makeText(it, "Lỗi kết nối", Toast.LENGTH_SHORT).show() }
                }
            })
    }
    
    private fun updateStats() {
        val now = Date()
        tvTotalVouchers.text = allVouchers.size.toString()
        tvActiveVouchers.text = allVouchers.count { it.isActive && !isExpired(it, now) }.toString()
    }


    private fun showAddVoucherDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_voucher, null)
        
        val etCode = dialogView.findViewById<EditText>(R.id.et_voucher_code)
        val etDescription = dialogView.findViewById<EditText>(R.id.et_voucher_description)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinner_discount_type)
        val etValue = dialogView.findViewById<EditText>(R.id.et_discount_value)
        val etMinOrder = dialogView.findViewById<EditText>(R.id.et_min_order_value)
        val etMaxDiscount = dialogView.findViewById<EditText>(R.id.et_max_discount)
        val etUsageLimit = dialogView.findViewById<EditText>(R.id.et_usage_limit)
        val tvStartDate = dialogView.findViewById<TextView>(R.id.tv_start_date)
        val tvEndDate = dialogView.findViewById<TextView>(R.id.tv_end_date)
        val switchActive = dialogView.findViewById<Switch>(R.id.switch_active)
        
        val types = arrayOf("PERCENT", "FIXED")
        spinnerType.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, types)
        
        var startDate: Calendar? = null
        var endDate: Calendar? = null
        
        tvStartDate.setOnClickListener {
            showDateTimePicker { calendar ->
                startDate = calendar
                tvStartDate.text = dateFormat.format(calendar.time)
            }
        }
        
        tvEndDate.setOnClickListener {
            showDateTimePicker { calendar ->
                endDate = calendar
                tvEndDate.text = dateFormat.format(calendar.time)
            }
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Thêm Voucher Mới")
            .setView(dialogView)
            .setPositiveButton("Tạo") { _, _ ->
                val code = etCode.text.toString().trim()
                val type = spinnerType.selectedItem.toString()
                val value = etValue.text.toString().toDoubleOrNull()
                val minOrder = etMinOrder.text.toString().toDoubleOrNull()
                
                if (code.isEmpty() || value == null || minOrder == null || startDate == null || endDate == null) {
                    Toast.makeText(requireContext(), "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                if (type == "PERCENT" && value > 100) {
                    Toast.makeText(requireContext(), "Giá trị phần trăm không được vượt quá 100%", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val request = CreateVoucherRequest(
                    code = code.uppercase(),
                    description = etDescription.text.toString().trim().ifEmpty { null },
                    discountType = type,
                    discountValue = BigDecimal.valueOf(value),
                    startDate = dateFormat.format(startDate!!.time),
                    endDate = dateFormat.format(endDate!!.time),
                    minOrderValue = BigDecimal.valueOf(minOrder),
                    maxDiscountAmount = etMaxDiscount.text.toString().toDoubleOrNull()?.let { BigDecimal.valueOf(it) },
                    usageLimit = etUsageLimit.text.toString().toIntOrNull(),
                    isActive = switchActive.isChecked
                )
                createVoucher(request)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showEditVoucherDialog(voucher: Voucher) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_voucher, null)
        
        val etDescription = dialogView.findViewById<EditText>(R.id.et_voucher_description)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinner_discount_type)
        val etValue = dialogView.findViewById<EditText>(R.id.et_discount_value)
        val etMinOrder = dialogView.findViewById<EditText>(R.id.et_min_order_value)
        val etMaxDiscount = dialogView.findViewById<EditText>(R.id.et_max_discount)
        val etUsageLimit = dialogView.findViewById<EditText>(R.id.et_usage_limit)
        val tvStartDate = dialogView.findViewById<TextView>(R.id.tv_start_date)
        val tvEndDate = dialogView.findViewById<TextView>(R.id.tv_end_date)
        val switchActive = dialogView.findViewById<Switch>(R.id.switch_active)
        
        etDescription.setText(voucher.description)
        etValue.setText(voucher.discountValue.toString())
        etMinOrder.setText(voucher.minOrderValue.toString())
        etMaxDiscount.setText(voucher.maxDiscountAmount?.toString() ?: "")
        etUsageLimit.setText(voucher.usageLimit?.toString() ?: "")
        tvStartDate.text = voucher.startDate
        tvEndDate.text = voucher.endDate
        switchActive.isChecked = voucher.isActive
        
        val types = arrayOf("PERCENT", "FIXED")
        spinnerType.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, types)
        spinnerType.setSelection(if (voucher.discountType == "PERCENT") 0 else 1)
        
        var startDate: Calendar? = null
        var endDate: Calendar? = null
        
        tvStartDate.setOnClickListener {
            showDateTimePicker { calendar ->
                startDate = calendar
                tvStartDate.text = dateFormat.format(calendar.time)
            }
        }
        
        tvEndDate.setOnClickListener {
            showDateTimePicker { calendar ->
                endDate = calendar
                tvEndDate.text = dateFormat.format(calendar.time)
            }
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Chỉnh Sửa Voucher: ${voucher.code}")
            .setView(dialogView)
            .setPositiveButton("Cập nhật") { _, _ ->
                val type = spinnerType.selectedItem.toString()
                val value = etValue.text.toString().toDoubleOrNull()
                
                if (type == "PERCENT" && value != null && value > 100) {
                    Toast.makeText(requireContext(), "Giá trị phần trăm không được vượt quá 100%", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val request = UpdateVoucherRequest(
                    description = etDescription.text.toString().trim().ifEmpty { null },
                    discountType = type,
                    discountValue = value?.let { BigDecimal.valueOf(it) },
                    startDate = startDate?.let { dateFormat.format(it.time) },
                    endDate = endDate?.let { dateFormat.format(it.time) },
                    minOrderValue = etMinOrder.text.toString().toDoubleOrNull()?.let { BigDecimal.valueOf(it) },
                    maxDiscountAmount = etMaxDiscount.text.toString().toDoubleOrNull()?.let { BigDecimal.valueOf(it) },
                    usageLimit = etUsageLimit.text.toString().toIntOrNull(),
                    isActive = switchActive.isChecked
                )
                updateVoucher(voucher.id!!, request)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showDateTimePicker(onDateTimeSelected: (Calendar) -> Unit) {
        val calendar = Calendar.getInstance()
        
        DatePickerDialog(requireContext(), { _, year, month, day ->
            TimePickerDialog(requireContext(), { _, hour, minute ->
                calendar.set(year, month, day, hour, minute, 0)
                onDateTimeSelected(calendar)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun createVoucher(request: CreateVoucherRequest) {
        progressBar.visibility = View.VISIBLE
        
        RetrofitClient.getInstance(requireContext()).apiService.createPromotion(request)
            .enqueue(object : Callback<ApiResponse<Voucher>> {
                override fun onResponse(
                    call: Call<ApiResponse<Voucher>>,
                    response: Response<ApiResponse<Voucher>>
                ) {
                    if (!isAdded) return
                    progressBar.visibility = View.GONE
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        context?.let { Toast.makeText(it, "Tạo voucher thành công", Toast.LENGTH_SHORT).show() }
                        loadVouchers()
                    } else {
                        val errorMsg = response.body()?.message ?: "Không thể tạo voucher"
                        context?.let { Toast.makeText(it, errorMsg, Toast.LENGTH_SHORT).show() }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Voucher>>, t: Throwable) {
                    if (!isAdded) return
                    progressBar.visibility = View.GONE
                    context?.let { Toast.makeText(it, "Lỗi kết nối", Toast.LENGTH_SHORT).show() }
                }
            })
    }

    private fun updateVoucher(id: Long, request: UpdateVoucherRequest) {
        progressBar.visibility = View.VISIBLE
        
        RetrofitClient.getInstance(requireContext()).apiService.updatePromotion(id, request)
            .enqueue(object : Callback<ApiResponse<Voucher>> {
                override fun onResponse(
                    call: Call<ApiResponse<Voucher>>,
                    response: Response<ApiResponse<Voucher>>
                ) {
                    if (!isAdded) return
                    progressBar.visibility = View.GONE
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        context?.let { Toast.makeText(it, "Cập nhật voucher thành công", Toast.LENGTH_SHORT).show() }
                        loadVouchers()
                    } else {
                        val errorMsg = response.body()?.message ?: "Không thể cập nhật voucher"
                        context?.let { Toast.makeText(it, errorMsg, Toast.LENGTH_SHORT).show() }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Voucher>>, t: Throwable) {
                    if (!isAdded) return
                    progressBar.visibility = View.GONE
                    context?.let { Toast.makeText(it, "Lỗi kết nối", Toast.LENGTH_SHORT).show() }
                }
            })
    }

    private fun showDeleteConfirmation(voucher: Voucher) {
        AlertDialog.Builder(requireContext())
            .setTitle("Xác nhận xóa")
            .setMessage("Bạn có chắc muốn xóa voucher ${voucher.code}?")
            .setPositiveButton("Xóa") { _, _ ->
                deleteVoucher(voucher.id!!)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun deleteVoucher(id: Long) {
        progressBar.visibility = View.VISIBLE
        
        RetrofitClient.getInstance(requireContext()).apiService.deletePromotion(id)
            .enqueue(object : Callback<ApiResponse<Void>> {
                override fun onResponse(
                    call: Call<ApiResponse<Void>>,
                    response: Response<ApiResponse<Void>>
                ) {
                    if (!isAdded) return
                    progressBar.visibility = View.GONE
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        context?.let { Toast.makeText(it, "Xóa voucher thành công", Toast.LENGTH_SHORT).show() }
                        loadVouchers()
                    } else {
                        val errorMsg = response.body()?.message ?: "Không thể xóa voucher"
                        context?.let { Toast.makeText(it, errorMsg, Toast.LENGTH_SHORT).show() }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Void>>, t: Throwable) {
                    if (!isAdded) return
                    progressBar.visibility = View.GONE
                    context?.let { Toast.makeText(it, "Lỗi kết nối", Toast.LENGTH_SHORT).show() }
                }
            })
    }

    private fun toggleVoucherStatus(voucher: Voucher) {
        val newStatus = !voucher.isActive
        val request = UpdateVoucherRequest(
            description = null,
            discountType = null,
            discountValue = null,
            startDate = null,
            endDate = null,
            minOrderValue = null,
            maxDiscountAmount = null,
            usageLimit = null,
            isActive = newStatus
        )
        
        progressBar.visibility = View.VISIBLE
        
        RetrofitClient.getInstance(requireContext()).apiService.updatePromotion(voucher.id!!, request)
            .enqueue(object : Callback<ApiResponse<Voucher>> {
                override fun onResponse(
                    call: Call<ApiResponse<Voucher>>,
                    response: Response<ApiResponse<Voucher>>
                ) {
                    if (!isAdded) return
                    progressBar.visibility = View.GONE
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        val statusText = if (newStatus) "kích hoạt" else "vô hiệu hóa"
                        context?.let { Toast.makeText(it, "Đã $statusText voucher", Toast.LENGTH_SHORT).show() }
                        loadVouchers()
                    } else {
                        val errorMsg = response.body()?.message ?: "Không thể cập nhật trạng thái"
                        context?.let { Toast.makeText(it, errorMsg, Toast.LENGTH_SHORT).show() }
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Voucher>>, t: Throwable) {
                    if (!isAdded) return
                    progressBar.visibility = View.GONE
                    context?.let { Toast.makeText(it, "Lỗi kết nối", Toast.LENGTH_SHORT).show() }
                }
            })
    }

    private fun updateEmptyState() {
        if (filteredVouchers.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            rvVouchers.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            rvVouchers.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear lists to prevent memory leak
        allVouchers.clear()
        filteredVouchers.clear()
    }

    companion object {
        private const val TAG = "ManageVouchersFragment"
    }
}
