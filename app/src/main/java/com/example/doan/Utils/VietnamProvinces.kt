package com.example.doan.Utils

/**
 * Danh sách 34 tỉnh/thành phố Việt Nam
 * Với phí ship tính theo khoảng cách từ TP.HCM
 */
object VietnamProvinces {
    
    data class Province(
        val id: Int,
        val name: String,
        val shippingFee: Int // VNĐ
    )
    
    val provinces = listOf(
        // Miền Nam - Phí thấp (15-25k)
        Province(27, "TP Hồ Chí Minh", 15000),
        Province(28, "Đồng Nai", 20000),
        Province(29, "Tây Ninh", 25000),
        Province(30, "TP Cần Thơ", 30000),
        Province(31, "Vĩnh Long", 30000),
        Province(32, "Đồng Tháp", 30000),
        Province(33, "Cà Mau", 35000),
        Province(34, "An Giang", 35000),
        
        // Miền Trung - Phí trung bình (35-45k)
        Province(2, "TP Huế", 40000),
        Province(20, "Quảng Trị", 40000),
        Province(21, "TP Đà Nẵng", 40000),
        Province(22, "Quảng Ngãi", 40000),
        Province(23, "Gia Lai", 45000),
        Province(24, "Khánh Hòa", 40000),
        Province(25, "Lâm Đồng", 35000),
        Province(26, "Đắk Lắk", 45000),
        Province(11, "Hà Tĩnh", 45000),
        Province(10, "Nghệ An", 45000),
        Province(9, "Thanh Hóa", 45000),
        
        // Miền Bắc - Phí cao (45-55k)
        Province(1, "TP Hà Nội", 50000),
        Province(3, "Quảng Ninh", 50000),
        Province(4, "Cao Bằng", 55000),
        Province(5, "Lạng Sơn", 55000),
        Province(6, "Lai Châu", 60000),
        Province(7, "Điện Biên", 60000),
        Province(8, "Sơn La", 55000),
        Province(12, "Tuyên Quang", 55000),
        Province(13, "Lào Cai", 55000),
        Province(14, "Thái Nguyên", 50000),
        Province(15, "Phú Thọ", 50000),
        Province(16, "Bắc Ninh", 50000),
        Province(17, "Hưng Yên", 50000),
        Province(18, "TP Hải Phòng", 50000),
        Province(19, "Ninh Bình", 50000)
    )
    
    fun getProvinceNames(): List<String> {
        return provinces.map { it.name }
    }
    
    fun getShippingFee(provinceName: String): Int {
        return provinces.find { it.name == provinceName }?.shippingFee ?: 25000
    }
    
    fun getProvinceById(id: Int): Province? {
        return provinces.find { it.id == id }
    }
}
