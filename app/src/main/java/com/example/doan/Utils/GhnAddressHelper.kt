package com.example.doan.Utils

import android.app.Dialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.doan.Models.*
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

/**
 * Helper class để xử lý chọn địa chỉ giao hàng với GHN
 */
class GhnAddressHelper(private val context: Context) {

    companion object {
        private const val TAG = "GhnAddressHelper"
    }

    interface OnAddressSelectedListener {
        fun onAddressSelected(addressInfo: DeliveryAddressInfo, shippingFee: Int)
        fun onCancelled()
    }

    private var provinces: List<GhnProvince> = emptyList()
    private var districts: List<GhnDistrict> = emptyList()
    private var wards: List<GhnWard> = emptyList()

    private var selectedProvince: GhnProvince? = null
    private var selectedDistrict: GhnDistrict? = null
    private var selectedWard: GhnWard? = null
    private var currentShippingFee: Int = 0

    /**
     * Hiển thị dialog chọn địa chỉ giao hàng
     */
    fun showAddressSelectionDialog(listener: OnAddressSelectedListener) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_select_delivery_address, null)
        val dialog = Dialog(context)
        dialog.setContentView(dialogView)
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        // Views
        val spinnerProvince = dialogView.findViewById<AutoCompleteTextView>(R.id.spinner_province)
        val spinnerDistrict = dialogView.findViewById<AutoCompleteTextView>(R.id.spinner_district)
        val spinnerWard = dialogView.findViewById<AutoCompleteTextView>(R.id.spinner_ward)
        val etDetailAddress = dialogView.findViewById<TextInputEditText>(R.id.et_detail_address)
        val llShippingFee = dialogView.findViewById<LinearLayout>(R.id.ll_shipping_fee)
        val tvShippingFee = dialogView.findViewById<TextView>(R.id.tv_shipping_fee)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)
        val btnConfirm = dialogView.findViewById<MaterialButton>(R.id.btn_confirm)

        // Load provinces
        loadProvinces(spinnerProvince)

        // Province selection
        spinnerProvince.setOnItemClickListener { _, _, position, _ ->
            selectedProvince = provinces[position]
            selectedDistrict = null
            selectedWard = null
            currentShippingFee = 0
            
            spinnerDistrict.setText("")
            spinnerWard.setText("")
            spinnerDistrict.isEnabled = true
            spinnerWard.isEnabled = false
            llShippingFee.visibility = View.GONE
            btnConfirm.isEnabled = false
            
            selectedProvince?.let { loadDistricts(it.provinceId, spinnerDistrict) }
        }

        // District selection
        spinnerDistrict.setOnItemClickListener { _, _, position, _ ->
            selectedDistrict = districts[position]
            selectedWard = null
            currentShippingFee = 0
            
            spinnerWard.setText("")
            spinnerWard.isEnabled = true
            llShippingFee.visibility = View.GONE
            btnConfirm.isEnabled = false
            
            selectedDistrict?.let { loadWards(it.districtId, spinnerWard) }
        }

        // Ward selection
        spinnerWard.setOnItemClickListener { _, _, position, _ ->
            selectedWard = wards[position]
            
            // Calculate shipping fee
            selectedDistrict?.let { district ->
                selectedWard?.let { ward ->
                    calculateShippingFee(district.districtId, ward.wardCode, tvShippingFee, llShippingFee, btnConfirm)
                }
            }
        }

        // Cancel button
        btnCancel.setOnClickListener {
            dialog.dismiss()
            listener.onCancelled()
        }

        // Confirm button
        btnConfirm.setOnClickListener {
            val detailAddress = etDetailAddress.text.toString().trim()
            
            if (detailAddress.isEmpty()) {
                Toast.makeText(context, "Vui lòng nhập số nhà, tên đường", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val addressInfo = DeliveryAddressInfo(
                provinceId = selectedProvince?.provinceId,
                provinceName = selectedProvince?.provinceName,
                districtId = selectedDistrict?.districtId,
                districtName = selectedDistrict?.districtName,
                wardCode = selectedWard?.wardCode,
                wardName = selectedWard?.wardName,
                detailAddress = detailAddress
            )

            dialog.dismiss()
            listener.onAddressSelected(addressInfo, currentShippingFee)
        }

        dialog.show()
    }

    private fun loadProvinces(spinner: AutoCompleteTextView) {
        RetrofitClient.getInstance(context).apiService.getGhnProvinces()
            .enqueue(object : Callback<ApiResponse<List<GhnProvince>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<GhnProvince>>>,
                    response: Response<ApiResponse<List<GhnProvince>>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        provinces = response.body()?.data ?: emptyList()
                        val provinceNames = provinces.map { it.provinceName }
                        val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, provinceNames)
                        spinner.setAdapter(adapter)
                    } else {
                        Toast.makeText(context, "Không thể tải danh sách tỉnh/thành phố", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<GhnProvince>>>, t: Throwable) {
                    Log.e(TAG, "Error loading provinces", t)
                    Toast.makeText(context, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun loadDistricts(provinceId: Int, spinner: AutoCompleteTextView) {
        RetrofitClient.getInstance(context).apiService.getGhnDistricts(provinceId)
            .enqueue(object : Callback<ApiResponse<List<GhnDistrict>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<GhnDistrict>>>,
                    response: Response<ApiResponse<List<GhnDistrict>>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        districts = response.body()?.data ?: emptyList()
                        val districtNames = districts.map { it.districtName }
                        val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, districtNames)
                        spinner.setAdapter(adapter)
                    } else {
                        Toast.makeText(context, "Không thể tải danh sách quận/huyện", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<GhnDistrict>>>, t: Throwable) {
                    Log.e(TAG, "Error loading districts", t)
                    Toast.makeText(context, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun loadWards(districtId: Int, spinner: AutoCompleteTextView) {
        RetrofitClient.getInstance(context).apiService.getGhnWards(districtId)
            .enqueue(object : Callback<ApiResponse<List<GhnWard>>> {
                override fun onResponse(
                    call: Call<ApiResponse<List<GhnWard>>>,
                    response: Response<ApiResponse<List<GhnWard>>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        wards = response.body()?.data ?: emptyList()
                        val wardNames = wards.map { it.wardName }
                        val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, wardNames)
                        spinner.setAdapter(adapter)
                    } else {
                        Toast.makeText(context, "Không thể tải danh sách phường/xã", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<List<GhnWard>>>, t: Throwable) {
                    Log.e(TAG, "Error loading wards", t)
                    Toast.makeText(context, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun calculateShippingFee(
        districtId: Int,
        wardCode: String,
        tvShippingFee: TextView,
        llShippingFee: LinearLayout,
        btnConfirm: MaterialButton
    ) {
        val request = ShippingFeeRequest(
            toDistrictId = districtId,
            toWardCode = wardCode
        )

        RetrofitClient.getInstance(context).apiService.calculateShippingFee(request)
            .enqueue(object : Callback<ApiResponse<GhnCalculateFeeResponse>> {
                override fun onResponse(
                    call: Call<ApiResponse<GhnCalculateFeeResponse>>,
                    response: Response<ApiResponse<GhnCalculateFeeResponse>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val feeResponse = response.body()?.data
                        currentShippingFee = feeResponse?.total ?: 0
                        
                        tvShippingFee.text = String.format(Locale.getDefault(), "%,d VNĐ", currentShippingFee)
                        llShippingFee.visibility = View.VISIBLE
                        btnConfirm.isEnabled = true
                    } else {
                        Toast.makeText(context, "Không thể tính phí giao hàng", Toast.LENGTH_SHORT).show()
                        currentShippingFee = 0
                        btnConfirm.isEnabled = false
                    }
                }

                override fun onFailure(call: Call<ApiResponse<GhnCalculateFeeResponse>>, t: Throwable) {
                    Log.e(TAG, "Error calculating shipping fee", t)
                    Toast.makeText(context, "Lỗi tính phí giao hàng", Toast.LENGTH_SHORT).show()
                    currentShippingFee = 0
                    btnConfirm.isEnabled = false
                }
            })
    }
}
