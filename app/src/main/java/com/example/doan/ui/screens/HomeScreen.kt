package com.example.doan.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.doan.Models.Product
import com.example.doan.ui.composables.ProductCarousel

/**
 * Màn hình chính hiển thị các danh sách sản phẩm.
 *
 * @param bestSellerProducts Danh sách sản phẩm bán chạy nhất.
 * @param forYouProducts Danh sách sản phẩm dành cho bạn.
 * @param onProductClick Callback được gọi khi một sản phẩm được nhấn.
 * @param onAddToCartClick Callback được gọi khi nút thêm vào giỏ hàng được nhấn.
 */
@Composable
fun HomeScreen(
    bestSellerProducts: List<Product>,
    forYouProducts: List<Product>,
    onProductClick: (Product) -> Unit,
    onAddToCartClick: (Product) -> Unit
) {
    // Sử dụng Column để xếp chồng các mục theo chiều dọc
    Column {
        // Mục "Best Seller"
        Text(
            text = "Best Seller",
            style = MaterialTheme.typography.headlineSmall, // Sử dụng style cho tiêu đề
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
        )
        ProductCarousel(
            products = bestSellerProducts,
            onProductClick = onProductClick,
            onAddToCartClick = onAddToCartClick
        )

        // Thêm một khoảng trống giữa hai mục
        Spacer(modifier = Modifier.height(24.dp))

        // Mục "Dành cho bạn"
        Text(
            text = "Dành cho bạn",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
        )
        ProductCarousel(
            products = forYouProducts,
            onProductClick = onProductClick,
            onAddToCartClick = onAddToCartClick
        )
    }
}
