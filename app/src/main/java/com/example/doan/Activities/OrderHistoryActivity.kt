package com.example.doan.Activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.doan.R

// YÊU CẦU: Tạo một Activity riêng để chứa màn hình Lịch sử đơn hàng.
// TÁC DỤNG: Lớp này đóng vai trò là "container" cho OrderFragment.
class OrderHistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Thiết lập layout cho Activity.
        // Layout này chỉ chứa một FragmentContainerView để hiển thị OrderFragment.
        setContentView(R.layout.activity_order_history)

        // OrderFragment được tự động thêm vào thông qua XML, không cần thêm bằng code.
    }
}
