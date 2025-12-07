package com.example.doan.Fragments.Manager

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan.Adapters.VoucherManagerAdapter
import com.example.doan.Models.*
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

class ManageVouchersFragment : Fragment() {

    private lateinit var rvVouchers: RecyclerView
    private lateinit var voucherAdapter: VoucherManagerAdapter
    private lateinit var fabAddVoucher: FloatingActionButton
    private lateinit var tvEmptyState: TextView
    private lateinit var progressBar: ProgressBar
    
    private val vouchers = mutableListOf<Voucher>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

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
        fabAddVoucher = view.findViewById(R.id.fab_add_voucher)
        tvEmptyState = view.findViewById(R.id.tv_empty_vouchers)
        progressBar = view.findViewById(R.id.progress_bar)
    }

    private fun setupRecyclerView() {
        voucherAdapter = VoucherManagerAdapter(
            vouchers,
            onEditClick = { voucher -> showEditVoucherDialog(voucher) },
            onDeleteClick = { voucher -> showDeleteConfirmation(voucher) },
            onToggleClick = { voucher -> toggleVoucherStatus(voucher) }
        )
        rvVouchers.layoutManager = LinearLayoutManager(requireContext())
        rvVouchers.adapter = voucherAdapter
    }

    private fun setupListeners() {
        fabAddVoucher.setOnClickListener {
            showAddVoucherDialog()
        }
    }

    private fun loadVouchers() {
        progressBar.visibility = View.VISIBLE
        
        RetrofitClient.getInstance(requireContext()).apiService.getAllPromotions()
            .enqueue(object : Callback<ApiResponse<List<Voucher>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<Voucher>>>,
                    response: Response<ApiResponse<List<Voucher>>>
                ) {
                    progressBar.visibility = View.GONE
                    
                    Log.d("ManageVouchers", "Response code: ${response.code()}")
                    Log.d("ManageVouchers", "Response body: ${response.body()}")
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        val data = response.body()?.data ?: emptyList()
                        Log.d("ManageVouchers", "Loaded ${data.size} vouchers")
                        vouchers.clear()
                        vouchers.addAll(data)
                        voucherAdapter.notifyDataSetChanged()
                        updateEmptyState()
                    } else {
                        val errorMsg = response.body()?.message ?: response.errorBody()?.string() ?: "Unknown error"
                        Log.e("ManageVouchers", "Error: $errorMsg")
                        Toast.makeText(requireContext(), "Không thể tải danh sách voucher: $errorMsg", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<Voucher>>>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    Log.e("ManageVouchers", "Error loading vouchers", t)
                    Toast.makeText(requireContext(), "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                }
            })
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
        
        // Setup discount type spinner
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
                val description = etDescription.text.toString().trim()
                val type = spinnerType.selectedItem.toString()
                val value = etValue.text.toString().toDoubleOrNull()
                val minOrder = etMinOrder.text.toString().toDoubleOrNull()
                val maxDiscount = etMaxDiscount.text.toString().toDoubleOrNull()
                val usageLimit = etUsageLimit.text.toString().toIntOrNull()
                
                if (code.isEmpty() || value == null || minOrder == null || startDate == null || endDate == null) {
                    Toast.makeText(requireContext(), "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val request = CreateVoucherRequest(
                    code = code.uppercase(),
                    description = description.ifEmpty { null },
                    discountType = type,
                    discountValue = BigDecimal.valueOf(value),
                    startDate = dateFormat.format(startDate!!.time),
                    endDate = dateFormat.format(endDate!!.time),
                    minOrderValue = BigDecimal.valueOf(minOrder),
                    maxDiscountAmount = maxDiscount?.let { BigDecimal.valueOf(it) },
                    usageLimit = usageLimit,
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
        
        // Pre-fill data
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
                val request = UpdateVoucherRequest(
                    description = etDescription.text.toString().trim().ifEmpty { null },
                    discountType = spinnerType.selectedItem.toString(),
                    discountValue = etValue.text.toString().toDoubleOrNull()?.let { BigDecimal.valueOf(it) },
                    startDate = if (startDate != null) dateFormat.format(startDate!!.time) else null,
                    endDate = if (endDate != null) dateFormat.format(endDate!!.time) else null,
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
        
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                TimePickerDialog(
                    requireContext(),
                    { _, hour, minute ->
                        calendar.set(year, month, day, hour, minute, 0)
                        onDateTimeSelected(calendar)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun createVoucher(request: CreateVoucherRequest) {
        RetrofitClient.getInstance(requireContext()).apiService.createPromotion(request)
            .enqueue(object : Callback<ApiResponse<Voucher>> {
                override fun onResponse(
                    call: Call<ApiResponse<Voucher>>,
                    response: Response<ApiResponse<Voucher>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(requireContext(), "Tạo voucher thành công", Toast.LENGTH_SHORT).show()
                        loadVouchers()
                    } else {
                        Toast.makeText(requireContext(), response.body()?.message ?: "Tạo voucher thất bại", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Voucher>>, t: Throwable) {
                    Log.e("ManageVouchers", "Error creating voucher", t)
                    Toast.makeText(requireContext(), "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateVoucher(id: Long, request: UpdateVoucherRequest) {
        RetrofitClient.getInstance(requireContext()).apiService.updatePromotion(id, request)
            .enqueue(object : Callback<ApiResponse<Voucher>> {
                override fun onResponse(
                    call: Call<ApiResponse<Voucher>>,
                    response: Response<ApiResponse<Voucher>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(requireContext(), "Cập nhật voucher thành công", Toast.LENGTH_SHORT).show()
                        loadVouchers()
                    } else {
                        Toast.makeText(requireContext(), response.body()?.message ?: "Cập nhật thất bại", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Voucher>>, t: Throwable) {
                    Log.e("ManageVouchers", "Error updating voucher", t)
                    Toast.makeText(requireContext(), "Lỗi kết nối", Toast.LENGTH_SHORT).show()
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
        RetrofitClient.getInstance(requireContext()).apiService.deletePromotion(id)
            .enqueue(object : Callback<ApiResponse<Void>> {
                override fun onResponse(
                    call: Call<ApiResponse<Void>>,
                    response: Response<ApiResponse<Void>>
                ) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Đã xóa/vô hiệu hóa voucher", Toast.LENGTH_SHORT).show()
                        loadVouchers()
                    } else {
                        val errorMsg = response.body()?.message ?: "Xóa thất bại"
                        Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Void>>, t: Throwable) {
                    Log.e("ManageVouchers", "Error deleting voucher", t)
                    Toast.makeText(requireContext(), "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun toggleVoucherStatus(voucher: Voucher) {
        RetrofitClient.getInstance(requireContext()).apiService.togglePromotionStatus(voucher.id!!)
            .enqueue(object : Callback<ApiResponse<Voucher>> {
                override fun onResponse(
                    call: Call<ApiResponse<Voucher>>,
                    response: Response<ApiResponse<Voucher>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(requireContext(), "Đã thay đổi trạng thái", Toast.LENGTH_SHORT).show()
                        loadVouchers()
                    } else {
                        Toast.makeText(requireContext(), "Thay đổi trạng thái thất bại", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Voucher>>, t: Throwable) {
                    Log.e("ManageVouchers", "Error toggling status", t)
                    Toast.makeText(requireContext(), "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateEmptyState() {
        if (vouchers.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            rvVouchers.visibility = View.GONE
        } else {
            tvEmptyState.visibility = View.GONE
            rvVouchers.visibility = View.VISIBLE
        }
    }
}
