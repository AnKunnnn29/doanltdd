package com.example.doan.Models

import com.google.gson.annotations.SerializedName

// ==================== GHN Province ====================
data class GhnProvince(
    @SerializedName("ProvinceID")
    val provinceId: Int,
    
    @SerializedName("ProvinceName")
    val provinceName: String,
    
    @SerializedName("Code")
    val code: String? = null,
    
    @SerializedName("NameExtension")
    val nameExtension: List<String>? = null
)

// ==================== GHN District ====================
data class GhnDistrict(
    @SerializedName("DistrictID")
    val districtId: Int,
    
    @SerializedName("ProvinceID")
    val provinceId: Int,
    
    @SerializedName("DistrictName")
    val districtName: String,
    
    @SerializedName("Code")
    val code: String? = null,
    
    @SerializedName("Type")
    val type: Int? = null,
    
    @SerializedName("SupportType")
    val supportType: Int? = null,
    
    @SerializedName("NameExtension")
    val nameExtension: List<String>? = null
)

// ==================== GHN Ward ====================
data class GhnWard(
    @SerializedName("WardCode")
    val wardCode: String,
    
    @SerializedName("DistrictID")
    val districtId: Int,
    
    @SerializedName("WardName")
    val wardName: String,
    
    @SerializedName("NameExtension")
    val nameExtension: List<String>? = null
)

// ==================== GHN Service ====================
data class GhnService(
    @SerializedName("service_id")
    val serviceId: Int,
    
    @SerializedName("short_name")
    val shortName: String,
    
    @SerializedName("service_type_id")
    val serviceTypeId: Int
)

// ==================== GHN Calculate Fee Request ====================
data class GhnCalculateFeeRequest(
    @SerializedName("to_district_id")
    val toDistrictId: Int,
    
    @SerializedName("to_ward_code")
    val toWardCode: String,
    
    @SerializedName("service_type_id")
    val serviceTypeId: Int = 2, // 2 = Standard
    
    @SerializedName("weight")
    val weight: Int = 500, // 500 gram default
    
    @SerializedName("height")
    val height: Int = 20,
    
    @SerializedName("width")
    val width: Int = 15,
    
    @SerializedName("length")
    val length: Int = 15,
    
    @SerializedName("insurance_value")
    val insuranceValue: Int? = null,
    
    @SerializedName("cod_value")
    val codValue: Int? = null
)

// ==================== GHN Calculate Fee Response ====================
data class GhnCalculateFeeResponse(
    @SerializedName("total")
    val total: Int,
    
    @SerializedName("service_fee")
    val serviceFee: Int,
    
    @SerializedName("insurance_fee")
    val insuranceFee: Int? = null,
    
    @SerializedName("cod_fee")
    val codFee: Int? = null
)

// ==================== Shipping Fee Request (Simplified) ====================
data class ShippingFeeRequest(
    val toDistrictId: Int,
    val toWardCode: String,
    val weight: Int? = null,
    val insuranceValue: Int? = null,
    val codValue: Int? = null,
    val serviceTypeId: Int? = null
)

// ==================== Delivery Address Info ====================
data class DeliveryAddressInfo(
    var provinceId: Int? = null,
    var provinceName: String? = null,
    var districtId: Int? = null,
    var districtName: String? = null,
    var wardCode: String? = null,
    var wardName: String? = null,
    var detailAddress: String? = null
) {
    fun getFullAddress(): String {
        val parts = mutableListOf<String>()
        detailAddress?.let { if (it.isNotEmpty()) parts.add(it) }
        wardName?.let { parts.add(it) }
        districtName?.let { parts.add(it) }
        provinceName?.let { parts.add(it) }
        return parts.joinToString(", ")
    }
    
    fun isComplete(): Boolean {
        return provinceId != null && districtId != null && wardCode != null && !detailAddress.isNullOrEmpty()
    }
}
