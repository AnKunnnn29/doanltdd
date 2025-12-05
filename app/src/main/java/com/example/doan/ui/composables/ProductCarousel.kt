package com.example.doan.ui.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.doan.Models.Product
import com.example.doan.R
import dev.chrisbanes.snapper.ExperimentalSnapperApi
import dev.chrisbanes.snapper.rememberSnapperFlingBehavior
import kotlin.math.abs

/**
 * Hiển thị một danh sách sản phẩm theo chiều ngang với hiệu ứng carousel.
 * Item ở giữa sẽ được phóng to để tạo điểm nhấn.
 *
 * @param products Danh sách các sản phẩm cần hiển thị.
 * @param onProductClick Callback được gọi khi một sản phẩm được nhấn.
 * @param onAddToCartClick Callback được gọi khi nút thêm vào giỏ hàng được nhấn.
 */
@OptIn(ExperimentalSnapperApi::class)
@Composable
fun ProductCarousel(
    products: List<Product>,
    onProductClick: (Product) -> Unit,
    onAddToCartClick: (Product) -> Unit
) {
    // Trạng thái của LazyRow, dùng để theo dõi vị trí cuộn
    val lazyListState = rememberLazyListState()

    LazyRow(
        state = lazyListState,
        // Hiệu ứng cuộn và dừng lại tại item gần nhất
        flingBehavior = rememberSnapperFlingBehavior(lazyListState),
        // Căn giữa các item theo chiều dọc
        verticalAlignment = Alignment.CenterVertically,
        // Thêm khoảng đệm ở hai đầu để item đầu và cuối có thể vào giữa
        contentPadding = PaddingValues(horizontal = 16.dp),
        // Khoảng cách giữa các item
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(products) { product ->
            // Lấy thông tin layout của LazyRow
            val layoutInfo = lazyListState.layoutInfo
            // Tìm item tương ứng với sản phẩm hiện tại
            val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.key == product.id }

            if (itemInfo != null) {
                // Tính toán vị trí trung tâm của viewport
                val center = layoutInfo.viewportEndOffset / 2
                // Tính toán khoảng cách từ tâm của item đến tâm của viewport
                val itemCenter = itemInfo.offset + itemInfo.size / 2
                val distance = abs(itemCenter - center)

                // Tính toán tỷ lệ phóng to dựa trên khoảng cách
                // Item càng gần tâm, tỷ lệ càng lớn (tối đa là 1.0)
                val scale = 1.0f - 0.2f * (distance / (layoutInfo.viewportEndOffset / 2f))

                // Áp dụng hiệu ứng phóng to
                Box(modifier = Modifier.graphicsLayer(
                    scaleX = scale,
                    scaleY = scale
                )) {
                    ProductCarouselItem(
                        product = product,
                        onProductClick = { onProductClick(product) },
                        onAddToCartClick = { onAddToCartClick(product) }
                    )
                }
            }
        }
    }
}

/**
 * Hiển thị một item sản phẩm trong carousel.
 *
 * @param product Sản phẩm cần hiển thị.
 * @param onProductClick Callback được gọi khi item được nhấn.
 * @param onAddToCartClick Callback được gọi khi nút thêm vào giỏ hàng được nhấn.
 */
@Composable
fun ProductCarouselItem(
    product: Product,
    onProductClick: () -> Unit,
    onAddToCartClick: () -> Unit
) {
    Card(
        onClick = onProductClick,
        modifier = Modifier.width(150.dp) // Đặt chiều rộng cố định cho Card
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hình ảnh sản phẩm
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(product.imageUrl)
                        .placeholder(R.drawable.ic_launcher_background) // Ảnh hiển thị khi đang tải
                        .error(R.drawable.ic_launcher_background) // Ảnh hiển thị khi có lỗi
                        .build()
                ),
                contentDescription = product.name,
                modifier = Modifier
                    .height(120.dp)
                    .fillMaxWidth(),
                contentScale = ContentScale.Crop // Cắt ảnh để vừa với khung nhìn
            )
            Column(Modifier.padding(8.dp)) {
                // Tên sản phẩm
                Text(
                    text = product.name ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis, // Hiển thị '...' nếu tên quá dài
                    modifier = Modifier.padding(bottom = 4.dp),
                    textAlign = TextAlign.Center
                )
                // Hàng chứa giá và nút thêm vào giỏ hàng
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Giá sản phẩm
                    Text(
                        text = "%,.0f ₫".format(product.price),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    // Nút thêm vào giỏ hàng
                    IconButton(onClick = onAddToCartClick) {
                        Icon(
                            imageVector = Icons.Outlined.AddCircle,
                            contentDescription = "Add to cart",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
