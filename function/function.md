# 📱 TÀI LIỆU CHỨC NĂNG ỨNG DỤNG UTE TEA - PHIÊN BẢN ĐẦY ĐỦ

## 📋 Tổng Quan

**UTE Tea** là ứng dụng đặt đồ uống trà sữa trên nền tảng Android (Kotlin) với Backend API (Spring Boot). Ứng dụng hỗ trợ cả người dùng thông thường (User) và quản lý (Manager) với các chức năng đầy đủ từ đặt hàng, thanh toán đến quản lý cửa hàng, dự báo doanh thu và phân tích thông minh.

### 🎯 Công nghệ sử dụng

**Frontend (Android):**
- Kotlin 100%
- Material Design 3
- Retrofit 2 + OkHttp 3
- WebSocket (STOMP)
- Glide (Image Loading)
- Biometric Authentication
- Speech-to-Text (Voice Order)

**Backend (Spring Boot):**
- Java 17
- Spring Boot 3.5.7
- Spring Security + JWT
- MySQL 8.0 (Aiven Cloud)
- WebSocket (STOMP)
- VNPay Payment Gateway
- OneSignal Push Notification

---

## 🔐 1. XÁC THỰC & TÀI KHOẢN

### 1.1 Đăng nhập (LoginActivity)
**Endpoint:** `POST /api/auth/login`

**Chức năng:**
- Đăng nhập bằng username/phone + password
- **Đăng nhập sinh trắc học (Biometric)**: Hỗ trợ đăng nhập bằng vân tay/Face ID
  - Lưu token được mã hóa trong Android KeyStore
  - Xác thực sinh trắc học trước khi giải mã token
- Tự động refresh token khi hết hạn (JWT 24h)
- Lưu phiên đăng nhập (SessionManager)
- Chuyển hướng tự động: User → MainActivity, Manager → ManagerActivity

**Request:**
```json
{
  "usernameOrPhone": "ute_student_01",
  "password": "123456"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 2,
    "username": "ute_student_01",
    "fullName": "Nguyen Van A",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "role": "USER",
    "memberTier": "SILVER"
  }
}
```


### 1.2 Đăng ký (RegisterActivity + OtpActivity)
**Endpoints:** 
- `POST /api/auth/register-with-otp` - Bước 1: Đăng ký và gửi OTP
- `POST /api/auth/otp-verify` - Bước 2: Xác thực OTP

**Chức năng:**
- Đăng ký tài khoản mới với username, email, password, phone
- **Xác thực OTP qua email** trước khi hoàn tất đăng ký
- Validation đầy đủ:
  - Username: 3-50 ký tự
  - Email: định dạng hợp lệ
  - Password: 6-100 ký tự
  - Phone: số điện thoại Việt Nam

**Luồng đăng ký:**
1. User nhập thông tin → Gửi OTP qua email
2. User nhập mã OTP 6 số
3. Xác thực thành công → Tài khoản được kích hoạt
4. Chuyển về màn hình Login

### 1.3 Quên mật khẩu (ForgotPasswordActivity + ResetPasswordActivity)
**Endpoints:**
- `POST /api/auth/forgot-password` - Gửi OTP reset password
- `POST /api/auth/reset-password` - Đặt lại mật khẩu mới

**Chức năng:**
- Gửi yêu cầu reset password qua email
- Nhập OTP và mật khẩu mới để đặt lại
- Xác thực OTP trước khi cho phép đổi mật khẩu

### 1.4 Đổi mật khẩu (ChangePasswordActivity)
**Endpoint:** `PUT /api/me/change-password`

**Chức năng:**
- Thay đổi mật khẩu khi đã đăng nhập
- Yêu cầu nhập mật khẩu cũ để xác thực
- Validation mật khẩu mới

### 1.5 Quản lý hồ sơ (AccountFragment + UserProfileActivity)
**Endpoints:**
- `GET /api/me` - Lấy thông tin profile
- `PUT /api/me` - Cập nhật thông tin
- `POST /api/me/avatar` - Upload avatar
- `DELETE /api/me` - Xóa tài khoản

**Chức năng:**
- Xem thông tin tài khoản chi tiết
- Cập nhật thông tin cá nhân (tên, email, SĐT, địa chỉ)
- **Upload avatar**: Chọn ảnh từ thư viện và upload lên server
- Xem điểm tích lũy và hạng thành viên (Member Tier)
- **Xóa tài khoản**: Xóa vĩnh viễn tài khoản người dùng
  - Backup doanh thu trước khi xóa (DeletedUserOrderBackup)
  - Xóa tất cả đơn hàng, giỏ hàng, đánh giá
- Đăng xuất

---

## 🏠 2. TRANG CHỦ (HomeFragment)

### 2.1 Giao diện chính
**Chức năng:**
- **Banner quảng cáo**: Carousel tự động chuyển với hiệu ứng zoom
- **Lời chào theo thời gian**: "Chào buổi sáng/chiều/tối"
- Hiển thị tên người dùng và avatar
- **Hiệu ứng theo mùa**: 
  - Mùa xuân: Hoa anh đào rơi (SakuraView)
  - Mùa hè: Ánh nắng (SunshineView)
  - Mùa thu: Lá vàng rơi (FallingLeavesView)
  - Mùa đông: Tuyết rơi (SnowfallView)
  - Tết: Pháo hoa và lì xì (TetView)

### 2.2 Sản phẩm nổi bật
**Endpoints:**
- `GET /api/drinks` - Lấy danh sách sản phẩm
- `GET /api/drinks/best-sellers` - Sản phẩm bán chạy

**Chức năng:**
- **Best Seller**: Danh sách sản phẩm bán chạy (carousel ngang)
- **Dành cho bạn**: Gợi ý sản phẩm ngẫu nhiên
- Animation khi load danh sách

### 2.3 Nút chức năng nhanh
**Chức năng:**
- **Giao hàng (Delivery)**: Chuyển đến menu với chế độ giao hàng
- **Đến lấy (Pickup)**: Chuyển đến menu với chế độ tự đến lấy
- **Đặt hàng bằng giọng nói**: Sử dụng Speech-to-Text để tìm và đặt món
  - Nhận diện giọng nói tiếng Việt
  - Tự động tìm kiếm sản phẩm
  - Thêm vào giỏ hàng nhanh
- **Chatbot AI**: Trợ lý ảo hỗ trợ tìm kiếm, gợi ý đồ uống
- **Vòng quay may mắn**: Quay để nhận voucher giảm giá
- **Đặt hàng nhóm**: Tạo hoặc tham gia phiên đặt hàng nhóm

### 2.4 Truy cập nhanh
**Chức năng:**
- Nút giỏ hàng với badge số lượng (real-time)
- Nút Live Chat với nhân viên hỗ trợ
- Nút thông báo (OneSignal Push Notification)


### 2.5 Gợi ý thông minh - Predictive Order ⭐⭐⭐⭐⭐ MỚI
**Endpoint:** `GET /api/predictive-order`

**Chức năng:**
- **Dự đoán món yêu thích**: Khi mở app, hệ thống phân tích lịch sử đặt hàng và gợi ý món bạn có thể muốn đặt
- **Điều kiện kích hoạt**:
  - Thứ/giờ quen thuộc (ví dụ: bạn hay đặt trà sữa vào buổi sáng thứ 2)
  - Tần suất đặt món (món yêu thích được đặt nhiều lần)
  - Độ gần đây của đơn hàng (đơn gần đây có trọng số cao hơn)
  - Thời tiết (nóng → đồ mát, lạnh → đồ ấm) - optional

**Thuật toán dự đoán:**
```
Score = (TimeScore × 0.35) + (DayScore × 0.25) + (RecencyScore × 0.2) + (FrequencyScore × 0.2)

- TimeScore: Điểm dựa trên khung giờ tương tự (1.0 nếu cùng giờ, giảm dần)
- DayScore: Điểm dựa trên ngày trong tuần (1.0 nếu cùng thứ)
- RecencyScore: Điểm dựa trên độ gần đây (1.0 nếu ≤3 ngày, giảm dần)
- FrequencyScore: Tỷ lệ đặt món này / tổng đơn hàng
```

**Dialog gợi ý thông minh:**
- Hiển thị: "Có phải bạn đang muốn gọi lại Trà sữa trân châu size L không?"
- Thông tin món: Hình ảnh, tên, size, giá, số lần đã đặt
- Lý do gợi ý:
  - "Bạn thường đặt món này vào buổi sáng"
  - "Đây là món yêu thích của bạn (5 lần đặt)"
  - "Bạn hay đặt món này vào Thứ Hai"
- Nút "Thêm vào giỏ" hoặc "Để sau"

**Cơ chế chống spam:**
- Không hiển thị lại trong 2 giờ nếu user đã bỏ qua
- Tối đa 3 lần gợi ý cùng 1 món trong ngày
- Độ tin cậy tối thiểu 20% mới hiển thị

**Response:**
```json
{
  "hasPrediction": true,
  "message": "Có phải bạn đang muốn gọi lại Trà sữa trân châu size L không?",
  "predictedDrink": {
    "drinkId": 1,
    "drinkName": "Trà sữa trân châu",
    "drinkImage": "/uploads/drinks/tra-sua.jpg",
    "sizeName": "L",
    "sizeId": 3,
    "price": 35000,
    "orderCount": 5,
    "lastOrderTime": "25/12/2024 08:30",
    "toppings": [
      {
        "toppingName": "Trân châu đen",
        "price": 5000
      }
    ],
    "note": "Ít đường"
  },
  "triggerReasons": [
    "Bạn thường đặt món này vào buổi sáng",
    "Đây là món yêu thích của bạn (5 lần đặt)",
    "Bạn hay đặt món này vào Thứ Hai"
  ],
  "confidenceScore": 0.85
}
```

---

## 📜 3. THỰC ĐƠN (MenuFragment)

### 3.1 Hiển thị sản phẩm
**Endpoint:** `GET /api/drinks`

**Chức năng:**
- Danh sách sản phẩm dạng Grid (2 cột)
- Hiển thị hình ảnh, tên, giá, danh mục
- Animation khi load danh sách (fade in)
- Lazy loading với pagination

### 3.2 Danh mục sản phẩm
**Endpoint:** `GET /api/categories`

**Chức năng:**
- Danh sách danh mục ngang (horizontal scroll)
- Lọc sản phẩm theo danh mục
- Danh mục "Tất cả" để xem toàn bộ
- 4 danh mục chính:
  - **Milk Tea** (Trà sữa)
  - **Fruit Tea** (Trà trái cây)
  - **Macchiato** (Trà kem cheese)
  - **Special** (Đặc biệt)

### 3.3 Tìm kiếm
**Endpoint:** `GET /api/drinks?search={keyword}`

**Chức năng:**
- **Tìm kiếm text**: Hỗ trợ tiếng Việt có dấu và không dấu
- **Gợi ý tìm kiếm**: Hiển thị kết quả real-time khi gõ
- **Tìm kiếm bằng giọng nói**: Speech-to-Text
  - Nhận diện giọng nói tiếng Việt
  - Tự động điền vào ô tìm kiếm

### 3.4 Bộ lọc
**Chức năng:**
- **Sắp xếp theo giá**: Tăng dần / Giảm dần
- **Lọc theo danh mục**: Chọn 1 hoặc nhiều danh mục
- Nút xóa bộ lọc để reset về mặc định

---

## 🍵 4. CHI TIẾT SẢN PHẨM (ProductDetailActivity)

### 4.1 Thông tin sản phẩm
**Endpoint:** `GET /api/drinks/{id}`

**Chức năng:**
- Hình ảnh sản phẩm lớn (zoom được)
- Tên, mô tả, danh mục
- Giá cơ bản
- Điểm đánh giá trung bình (rating bar)

### 4.2 Tùy chọn đặt hàng
**Chức năng:**
- **Chọn Size**: S/M/L/Jumbo với giá cộng thêm
  - Size M: +0đ
  - Size L: +5,000đ
  - Size Jumbo: +10,000đ
- **Chọn Topping**: Checkbox nhiều topping với giá riêng
  - Trân châu đen: 5,000đ
  - Thạch dừa: 3,000đ
  - Pudding: 7,000đ
  - Kem cheese: 10,000đ
- **Số lượng**: Tăng/giảm số lượng (1-99)
- **Ghi chú**: Nhập yêu cầu đặc biệt (ít đường, nhiều đá...)
- **Tổng tiền**: Tự động tính theo công thức:
  ```
  itemPrice = (basePrice + sizeExtraPrice + sum(toppingPrices)) × quantity
  ```

### 4.3 Đánh giá sản phẩm
**Endpoint:** `GET /api/reviews/drink/{drinkId}`

**Chức năng:**
- Hiển thị điểm đánh giá trung bình (1-5 sao)
- Tổng số đánh giá
- Danh sách 3 đánh giá gần nhất
- Xem tất cả đánh giá (pagination)
- Lọc theo số sao

### 4.4 Hành động
**Chức năng:**
- **Thêm vào giỏ**: Thêm sản phẩm vào giỏ hàng với animation
- **Mua ngay**: Thêm và chuyển đến giỏ hàng
- Animation "bay vào giỏ hàng" khi thêm sản phẩm

---

## 🛒 5. GIỎ HÀNG (CartActivity)

### 5.1 Quản lý giỏ hàng
**Endpoints:**
- `GET /api/cart/{userId}` - Lấy giỏ hàng
- `POST /api/cart/add` - Thêm sản phẩm
- `PUT /api/cart/update` - Cập nhật số lượng
- `DELETE /api/cart/remove` - Xóa sản phẩm

**Chức năng:**
- Danh sách sản phẩm trong giỏ
- Checkbox chọn từng sản phẩm hoặc chọn tất cả
- Cập nhật số lượng từng item (real-time)
- Xóa sản phẩm đã chọn
- Hiển thị size, topping, ghi chú của từng item


### 5.2 Thông tin đơn hàng
**Chức năng:**
- **Chọn chi nhánh**: Dropdown danh sách cửa hàng
- **Loại giao hàng**: 
  - Đến lấy (Pickup) - Không phí ship
  - Giao tận nơi (Delivery) - Yêu cầu nhập địa chỉ, có phí ship
- **Phương thức thanh toán**: 
  - COD (Thanh toán khi nhận hàng)
  - VNPAY (Thanh toán online)
  - VietQR (Chuyển khoản QR)

### 5.3 Voucher & Giảm giá
**Endpoints:**
- `GET /api/promotions` - Lấy danh sách voucher
- `GET /api/promotions/validate?code={code}` - Validate voucher

**Chức năng:**
- Nhập mã voucher thủ công
- **Chọn voucher**: Dialog hiển thị voucher khả dụng
  - Voucher thường (từ promotion)
  - Voucher từ vòng quay (spin wheel)
  - Voucher sinh nhật (member tier)
- Hiển thị số tiền giảm và tổng tiền sau giảm
- Xóa voucher đã áp dụng
- Validation voucher:
  - Kiểm tra hạn sử dụng
  - Kiểm tra đơn tối thiểu
  - Kiểm tra số lần sử dụng

**Tính toán giảm giá:**
```
if (promotionType == PERCENTAGE) {
    discount = subtotal × (value / 100)
    if (discount > maxDiscount) discount = maxDiscount
} else {
    discount = value
}

finalPrice = subtotal - discount + shippingFee - tierDiscount
```

### 5.4 Giảm giá theo hạng thành viên
**Chức năng:**
- Tự động áp dụng giảm giá theo Member Tier
- BRONZE: 0%
- SILVER: 5%
- GOLD: 10%
- PLATINUM: 15%

### 5.5 Thanh toán
**Endpoint:** `POST /api/orders`

**Chức năng:**
- Tính tổng tiền sản phẩm đã chọn
- Áp dụng giảm giá từ voucher và tier
- Tạo đơn hàng và chuyển đến thanh toán
- Xóa giỏ hàng sau khi đặt hàng thành công

---

## 💳 6. THANH TOÁN

### 6.1 Thanh toán COD
**Endpoint:** `POST /api/orders`

**Chức năng:**
- Tạo đơn hàng trực tiếp
- Thanh toán khi nhận hàng
- Không cần xác thực thanh toán

### 6.2 Thanh toán VNPAY (VNPayPaymentActivity)
**Endpoints:**
- `POST /api/vnpay/create-payment-amount` - Tạo payment URL
- `POST /api/vnpay/create-order-after-payment` - Tạo đơn sau khi thanh toán
- `GET /api/vnpay/callback` - Callback từ VNPAY

**Chức năng:**
- Mở WebView trang thanh toán VNPAY
- Xử lý callback thành công/thất bại
- Tự động tạo đơn hàng sau khi thanh toán thành công
- Xóa giỏ hàng sau khi hoàn tất
- Hiển thị kết quả thanh toán

**Luồng thanh toán:**
1. User chọn VNPAY → Tạo payment URL
2. Mở WebView VNPAY → User nhập thông tin thẻ
3. VNPAY callback → Xác thực chữ ký
4. Nếu thành công → Tạo đơn hàng
5. Hiển thị kết quả

### 6.3 Thanh toán VietQR (VietQRActivity)
**Chức năng:**
- Tạo mã QR chuyển khoản ngân hàng
- Hiển thị thông tin tài khoản nhận
- Nội dung chuyển khoản tự động (mã đơn hàng)
- Xác nhận đã thanh toán để tạo đơn hàng
- Hỗ trợ nhiều ngân hàng Việt Nam

---

## 📦 7. LỊCH SỬ ĐƠN HÀNG (OrderFragment + OrderDetailActivity)

### 7.1 Danh sách đơn hàng
**Endpoints:**
- `GET /api/orders/my` - Lấy đơn hàng của user
- `GET /api/orders/my/current` - Lấy đơn đang xử lý

**Chức năng:**
- Thống kê: Tổng đơn / Đang xử lý / Hoàn thành
- Danh sách đơn hàng với phân trang (load more)
- Sắp xếp thông minh: Đơn đang xử lý lên trước
- Animation khi load
- Lọc theo trạng thái:
  - Tất cả
  - Chờ xử lý (PENDING)
  - Đang làm (MAKING)
  - Đang giao (SHIPPING)
  - Sẵn sàng (READY)
  - Hoàn thành (DONE)
  - Đã hủy (CANCELED)

### 7.2 Chi tiết đơn hàng
**Endpoint:** `GET /api/orders/{orderId}`

**Chức năng:**
- Mã đơn hàng, ngày đặt
- Trạng thái đơn hàng (chip màu)
- Danh sách sản phẩm trong đơn
  - Tên món, size, topping
  - Số lượng, giá
  - Ghi chú
- Thông tin giao hàng:
  - Loại: Delivery/Pickup
  - Địa chỉ (nếu Delivery)
  - Chi nhánh
- Tổng tiền:
  - Tạm tính
  - Giảm giá (voucher + tier)
  - Phí ship
  - Thành tiền
- Phương thức thanh toán

### 7.3 Đánh giá sản phẩm
**Endpoint:** `POST /api/reviews`

**Chức năng:**
- **Đánh giá sau khi hoàn thành**: Chỉ hiển thị khi đơn hàng DONE
- Chọn số sao (1-5)
- Viết nhận xét
- Tùy chọn đánh giá ẩn danh
- Kiểm tra sản phẩm đã đánh giá chưa
- Upload hình ảnh đánh giá (optional)

**Request:**
```json
{
  "orderItemId": 123,
  "rating": 5,
  "comment": "Trà sữa rất ngon, trân châu dai",
  "isAnonymous": false
}
```

---

## 👥 8. ĐẶT HÀNG NHÓM (Group Order)

### 8.1 Tạo phiên đặt hàng nhóm (CreateGroupOrderActivity)
**Endpoint:** `POST /api/group-orders`

**Chức năng:**
- Chọn chi nhánh
- Chọn loại giao hàng (Pickup/Delivery)
- Nhập địa chỉ giao hàng (nếu Delivery)
- Đặt số thành viên tối đa (2-20)
- Đặt thời gian hết hạn phiên (15 phút - 24 giờ)
- Tạo mã mời 6 ký tự ngẫu nhiên

**Request:**
```json
{
  "storeId": 1,
  "deliveryType": "DELIVERY",
  "deliveryAddress": "KTX khu A, UTE",
  "maxMembers": 10,
  "expiresAt": "2024-12-30T20:00:00"
}
```

**Response:**
```json
{
  "id": 1,
  "inviteCode": "ABC123",
  "status": "OPEN",
  "hostId": 2,
  "hostName": "Nguyen Van A",
  "members": [...],
  "items": [],
  "totalAmount": 0
}
```


### 8.2 Tham gia nhóm (JoinGroupOrderActivity)
**Endpoint:** `POST /api/group-orders/join`

**Chức năng:**
- Nhập mã mời 6 ký tự để tham gia phiên
- Kiểm tra phiên còn mở không
- Kiểm tra số lượng thành viên tối đa
- Tự động thêm vào danh sách thành viên

### 8.3 Quản lý phiên nhóm (GroupOrderActivity)
**Endpoint:** `GET /api/group-orders/{id}`

**Chức năng:**
- **Thông tin phiên**: 
  - Mã mời (copy được)
  - Trạng thái (OPEN/LOCKED/COMPLETED/CANCELED/EXPIRED)
  - Thời gian hết hạn (countdown real-time)
  - Số thành viên hiện tại / tối đa
- **Danh sách thành viên**: 
  - Hiển thị avatar, tên
  - Phân biệt Host (có icon vương miện)
- **Danh sách món**: 
  - Xem món của tất cả thành viên
  - Hiển thị tên người đặt
  - Tổng tiền từng món
- **Chia sẻ mã mời**: 
  - Copy mã mời
  - Share qua app khác (Zalo, Messenger, WhatsApp...)

### 8.4 Thêm món vào nhóm (SelectDrinkForGroupActivity + AddToGroupOrderActivity)
**Endpoint:** `POST /api/group-orders/{id}/items`

**Chức năng:**
- Chọn sản phẩm từ menu
- Chọn size, topping, số lượng
- Nhập ghi chú
- Thêm vào phiên nhóm
- Real-time update cho tất cả thành viên

**Request:**
```json
{
  "drinkId": 1,
  "sizeName": "L",
  "quantity": 2,
  "note": "Ít đường",
  "toppings": [
    { "toppingName": "Trân châu đen" }
  ]
}
```

### 8.5 Quyền Host (người tạo phiên)
**Endpoints:**
- `POST /api/group-orders/{id}/lock` - Khóa phiên
- `POST /api/group-orders/{id}/unlock` - Mở khóa
- `PUT /api/group-orders/{id}` - Cập nhật thông tin
- `POST /api/group-orders/{id}/checkout` - Thanh toán
- `DELETE /api/group-orders/{id}` - Hủy phiên

**Chức năng:**
- **Khóa/Mở khóa phiên**: Ngăn thành viên thêm món mới
- **Thay đổi chi nhánh**: Cập nhật chi nhánh giao hàng
- **Thanh toán**: Checkout toàn bộ đơn hàng nhóm
  - Tính tổng tiền tất cả món
  - Áp dụng voucher (nếu có)
  - Chọn phương thức thanh toán
  - Tạo đơn hàng chung
- **Hủy phiên**: Xóa phiên và loại tất cả thành viên
- **Xóa món của bất kỳ ai**: Host có quyền xóa món của member

### 8.6 Quyền Member
**Endpoints:**
- `POST /api/group-orders/{id}/items` - Thêm món
- `DELETE /api/group-orders/{id}/items/{itemId}` - Xóa món của mình
- `POST /api/group-orders/{id}/leave` - Rời khỏi phiên

**Chức năng:**
- Thêm/xóa món của mình
- Rời khỏi phiên (không phải host)
- Xem danh sách món và tổng tiền
- Không thể thêm món khi phiên bị khóa

### 8.7 Chat nhóm (GroupChatActivity) ⭐ MỚI
**Endpoints:**
- `POST /api/group-orders/{id}/chat` - Gửi tin nhắn
- `GET /api/group-orders/{id}/chat` - Lấy lịch sử chat
- `GET /api/group-orders/{id}/chat/recent` - Lấy tin nhắn gần đây
- WebSocket: `/topic/group-chat/{groupOrderId}` - Nhận tin nhắn realtime

**Chức năng:**
- **Chat realtime**: Tin nhắn được gửi/nhận ngay lập tức qua WebSocket
- **Thông báo hệ thống tự động**:
  - Khi có thành viên mới tham gia: "Nguyễn Văn A đã tham gia nhóm 🎉"
  - Khi thành viên rời nhóm: "Nguyễn Văn A đã rời khỏi nhóm 👋"
  - Khi host khóa đơn: "🔒 Host đã khóa đơn! Không thể thêm/sửa món nữa."
  - Khi host mở khóa: "🔓 Host đã mở khóa đơn! Có thể thêm/sửa món."
  - Khi thêm món: "Nguyễn Văn A đã thêm Trà sữa trân châu 🧋"
  - Khi xóa món: "Nguyễn Văn A đã xóa Trà sữa trân châu ❌"
- **Hiển thị tin nhắn**: 
  - Phân biệt tin nhắn của mình (bên phải) và người khác (bên trái)
  - Hiển thị avatar + tên người gửi
  - Thời gian gửi
- **Lịch sử chat**: Lưu trữ và hiển thị lịch sử tin nhắn khi mở lại

**WebSocket Connection:**
```kotlin
// Connect
stompClient.connect("/ws")

// Subscribe
stompClient.subscribe("/topic/group-chat/${groupOrderId}") { message ->
    // Handle incoming message
}

// Send
stompClient.send("/app/group-chat/${groupOrderId}", message)
```

---

## 🎡 9. VÒNG QUAY MAY MẮN (SpinWheelActivity)

### 9.1 Hệ thống điểm
**Endpoint:** `GET /api/loyalty/points`

**Chức năng:**
- Hiển thị điểm hiện tại
- Cần 5 điểm để quay 1 lần
- Tích điểm từ đơn hàng:
  - Mỗi đơn hoàn thành = 1 điểm cơ bản
  - Nhân với hệ số theo Member Tier:
    - BRONZE: 1x
    - SILVER: 1.5x
    - GOLD: 2x
    - PLATINUM: 3x

### 9.2 Vòng quay
**Endpoint:** `POST /api/loyalty/spin`

**Chức năng:**
- Animation quay vòng tròn (smooth rotation)
- Kết quả ngẫu nhiên:
  - 70% cơ hội: Voucher giảm giá (5%, 10%, 15%, 20%)
  - 30% cơ hội: "Chúc may mắn lần sau"
- Dialog hiển thị kết quả với mã voucher
- Trừ 5 điểm sau khi quay
- Lưu voucher vào tài khoản nếu trúng

**Response:**
```json
{
  "success": true,
  "message": "Chúc mừng! Bạn nhận được voucher giảm 15%",
  "reward": {
    "id": 123,
    "code": "SPIN15ABC",
    "discountPercent": 15,
    "expiresAt": "2025-01-30T23:59:59"
  },
  "remainingPoints": 3
}
```

### 9.3 Voucher đã nhận
**Endpoint:** `GET /api/loyalty/rewards`

**Chức năng:**
- Danh sách voucher từ vòng quay
- Hiển thị mã, % giảm giá, hạn sử dụng
- Copy mã voucher để sử dụng
- Trạng thái: Chưa dùng / Đã dùng / Hết hạn

---

## 🏆 10. HẠNG THÀNH VIÊN (MemberTierActivity)

### 10.1 Hệ thống hạng thành viên
**Endpoint:** `GET /api/loyalty/tier/benefits`

**Chức năng:**
- **4 cấp hạng**: BRONZE (Đồng), SILVER (Bạc), GOLD (Vàng), PLATINUM (Bạch Kim)
- Tự động nâng hạng khi đủ điểm
- Hiển thị tiến độ lên hạng tiếp theo (progress bar)
- Animation khi nâng hạng


### 10.2 Quyền lợi theo hạng

| Hạng | Điểm | Giảm giá | Nhân điểm | Freeship | Voucher sinh nhật |
|------|------|----------|-----------|----------|-------------------|
| BRONZE | 0-49 | 0% | 1x | ❌ | ❌ |
| SILVER | 50-149 | 5% | 1.5x | Đơn từ 100K | 10% |
| GOLD | 150-299 | 10% | 2x | Đơn từ 50K | 20% |
| PLATINUM | 300+ | 15% | 3x | Mọi đơn | 30% |

### 10.3 Quyền lợi đặc biệt
**GOLD+:**
- Hỗ trợ khách hàng ưu tiên
- Ưu đãi độc quyền

**PLATINUM:**
- Trải nghiệm sớm sản phẩm mới
- Sự kiện VIP

### 10.4 Cách tích điểm
**Chức năng:**
- Mỗi đơn hàng hoàn thành = 1 điểm cơ bản
- Điểm được nhân theo hệ số của hạng hiện tại
- Ví dụ: 
  - BRONZE: 1 đơn = 1 điểm
  - GOLD: 1 đơn = 2 điểm
  - PLATINUM: 1 đơn = 3 điểm

### 10.5 Tính toán giảm giá
**Endpoint:** `GET /api/loyalty/tier/preview-discount?orderTotal={amount}`

**Chức năng:**
- Preview giảm giá trước khi đặt hàng
- Tự động áp dụng khi checkout
- Không cần nhập mã voucher

**Response:**
```json
{
  "tier": "GOLD",
  "tierName": "Vàng",
  "discountPercent": 10,
  "orderTotal": 100000,
  "tierDiscount": 10000,
  "finalTotal": 90000,
  "message": "Bạn được giảm 10% (-10,000 VND) với hạng Vàng"
}
```

---

## 🤖 11. CHATBOT AI (ChatbotActivity)

### 11.1 Trợ lý ảo
**Endpoint:** `POST /api/chatbot/message`

**Chức năng:**
- Giao diện chat với bot
- Gợi ý nhanh (chips):
  - Menu
  - Món bán chạy
  - Khuyến mãi
  - Cửa hàng
  - Đơn hàng
  - Hướng dẫn

### 11.2 Gợi ý theo tâm trạng
**Chức năng:**
- Chips tâm trạng:
  - Mệt mỏi → Gợi ý đồ có caffeine
  - Vui vẻ → Gợi ý đồ ngọt
  - Trời nóng → Gợi ý đồ mát
  - Cần năng lượng → Gợi ý đồ có đường
- Bot gợi ý đồ uống phù hợp với hình ảnh

### 11.3 Chức năng
**Chức năng:**
- Tìm kiếm đồ uống theo tên
- Xem giá sản phẩm
- Xem khuyến mãi/voucher hiện có
- Tìm cửa hàng gần nhất
- Kiểm tra trạng thái đơn hàng
- Hướng dẫn sử dụng app

**Request:**
```json
{
  "message": "Tìm trà sữa trân châu",
  "userId": 2
}
```

**Response:**
```json
{
  "message": "Tìm thấy 3 món trà sữa trân châu:",
  "type": "PRODUCT_LIST",
  "data": {
    "products": [...]
  }
}
```

---

## 💬 12. LIVE CHAT (LiveChatActivity)

### 12.1 Chat với nhân viên hỗ trợ
**Endpoints:**
- `POST /api/chat/conversations` - Tạo cuộc hội thoại
- `POST /api/chat/messages` - Gửi tin nhắn
- `GET /api/chat/conversations/my` - Lấy danh sách hội thoại
- `GET /api/chat/conversations/{id}` - Lấy chi tiết hội thoại
- WebSocket: `/topic/chat/{conversationId}` - Nhận tin nhắn realtime

**Chức năng:**
- Tạo cuộc hội thoại mới
- Gửi/nhận tin nhắn real-time (WebSocket)
- Hiển thị trạng thái:
  - WAITING: Đang chờ nhân viên
  - ACTIVE: Đang chat
  - CLOSED: Đã đóng
- Upload hình ảnh trong chat
- Typing indicator (đang gõ...)

### 12.2 Quản lý hội thoại
**Endpoints:**
- `POST /api/chat/conversations/{id}/close` - Đóng hội thoại

**Chức năng:**
- Xem lịch sử tin nhắn
- Đóng cuộc hội thoại
- Tự động reconnect WebSocket khi mất kết nối
- Notification khi có tin nhắn mới

---

## 🏪 13. CỬA HÀNG (StoreFragment)

### 13.1 Danh sách cửa hàng
**Endpoint:** `GET /api/stores`

**Chức năng:**
- Hiển thị tất cả chi nhánh
- Thông tin:
  - Tên cửa hàng
  - Địa chỉ
  - Số điện thoại
  - Giờ mở cửa
  - Khoảng cách từ vị trí hiện tại
- Hiển thị trên bản đồ (Google Maps)

### 13.2 Tìm kiếm cửa hàng
**Chức năng:**
- Tìm theo tên hoặc địa chỉ
- Lọc theo khoảng cách
- Chỉ đường đến cửa hàng (Google Maps)

### 13.3 Tính khoảng cách
**Chức năng:**
- Sử dụng Google Maps Distance Matrix API
- Tính khoảng cách và thời gian di chuyển
- Sắp xếp cửa hàng theo khoảng cách gần nhất

---

## ⚙️ 14. CÀI ĐẶT (SettingsActivity)

### 14.1 Cài đặt ứng dụng
**Chức năng:**
- **Thông báo**:
  - Bật/tắt thông báo đơn hàng
  - Bật/tắt thông báo khuyến mãi
  - Bật/tắt âm thanh thông báo
- **Bảo mật**:
  - Bật/tắt đăng nhập sinh trắc học
  - Đổi mật khẩu
- **Giao diện**:
  - Chế độ tối (Dark Mode)
  - Ngôn ngữ (Tiếng Việt/English)
- **Thông tin ứng dụng**:
  - Phiên bản
  - Điều khoản sử dụng
  - Chính sách bảo mật
  - Liên hệ hỗ trợ

---

## 👨‍💼 15. QUẢN LÝ (Manager - ManagerActivity)

### 15.1 Dashboard (DashboardFragment)
**Endpoint:** `GET /api/manager/summary`

**Chức năng:**
- **Thống kê tổng quan**:
  - Doanh thu hôm nay
  - Tổng đơn hàng
  - Đơn chờ xử lý
  - Số khách hàng mới
- **Biểu đồ doanh thu**:
  - Theo ngày (7 ngày gần nhất) - Line Chart
  - Theo tháng (6 tháng gần nhất) - Bar Chart
- **Top sản phẩm bán chạy**:
  - Danh sách với số lượng bán
  - Doanh thu từng sản phẩm
  - Biểu đồ tròn phân bố

**Response:**
```json
{
  "todayRevenue": 2500000,
  "todayOrders": 45,
  "pendingOrders": 5,
  "newCustomers": 12,
  "dailyRevenue": [
    { "date": "2024-12-24", "revenue": 2000000 },
    { "date": "2024-12-25", "revenue": 2500000 }
  ],
  "topProducts": [
    {
      "drinkId": 1,
      "drinkName": "Trà sữa trân châu",
      "soldCount": 50,
      "revenue": 1250000
    }
  ]
}
```


### 15.2 Dự báo & Cảnh báo (ForecastFragment) ⭐⭐⭐⭐⭐ MỚI
**Endpoint:** `GET /api/manager/forecast`

**Chức năng:**
- **Nút cảnh báo nhấp nháy**: Button đỏ với animation pulse trên header, thu hút Manager kiểm tra

#### 15.2.1 Dự báo doanh thu
**Endpoint:** `GET /api/manager/forecast/revenue`

**Chức năng:**
- Dự báo hôm nay (điều chỉnh theo tiến độ trong ngày)
- Dự báo ngày mai
- Dự báo 7 ngày tới với biểu đồ
- Dự báo tháng (30 ngày)
- Tỷ lệ tăng trưởng (%) so với tuần trước
- Xu hướng: Tăng trưởng / Giảm / Ổn định

**Thuật toán dự báo:**
```
1. Lấy dữ liệu lịch sử 30 ngày gần nhất
2. Tính trung bình doanh thu theo ngày trong tuần
3. Dự báo hôm nay:
   - Nếu đang trong ngày: Điều chỉnh theo tiến độ
   - todayForecast = todayActual / (currentHour / 14)
4. Dự báo 7 ngày: Dựa trên pattern ngày trong tuần
5. Tính growth rate: (lastWeek - prevWeek) / prevWeek × 100
```

**Response:**
```json
{
  "todayForecast": 2800000,
  "tomorrowForecast": 2500000,
  "weekForecast": 18000000,
  "monthForecast": 75000000,
  "growthRate": 12.5,
  "trend": "UP",
  "dailyForecasts": [
    {
      "date": "2024-12-30",
      "dayOfWeek": "Thứ Hai",
      "forecastRevenue": 2800000,
      "forecastOrders": 50,
      "confidence": 85.0
    }
  ]
}
```

#### 15.2.2 Phân tích giờ cao điểm
**Endpoint:** `GET /api/manager/forecast/peak-hours`

**Chức năng:**
- Thống kê số đơn trung bình theo từng giờ (8h-22h)
- Doanh thu trung bình theo giờ
- Mức độ cao điểm: LOW / MEDIUM / HIGH / VERY_HIGH
- Đề xuất số nhân viên cần thiết cho mỗi khung giờ

**Công thức:**
```
avgOrders = totalOrders / 30 (ngày)
peakLevel = 
  - LOW: < 5 đơn/giờ
  - MEDIUM: 5-15 đơn/giờ
  - HIGH: 15-30 đơn/giờ
  - VERY_HIGH: > 30 đơn/giờ
recommendedStaff = ceil(avgOrders / 8) // 8 đơn/nhân viên/giờ
```

**Response:**
```json
[
  {
    "hour": 8,
    "timeRange": "08:00 - 09:00",
    "avgOrders": 12,
    "avgRevenue": 300000,
    "peakLevel": "MEDIUM",
    "recommendedStaff": 2
  },
  {
    "hour": 14,
    "timeRange": "14:00 - 15:00",
    "avgOrders": 35,
    "avgRevenue": 875000,
    "peakLevel": "VERY_HIGH",
    "recommendedStaff": 5
  }
]
```

#### 15.2.3 Cảnh báo món sắp hết ⚠️
**Endpoint:** `GET /api/manager/forecast/low-stock`

**Chức năng:**
- Phát hiện món đang bán nhanh hơn bình thường
- Tốc độ bán (đơn/giờ)
- So sánh với trung bình 30 ngày
- Mức cảnh báo: WARNING / CRITICAL
- Thông báo: "Dự kiến hết hàng trong X giờ"

**Thuật toán:**
```
salesVelocity = soldToday / (currentHour - 8)
normalVelocity = avgDaily / 14 // 14 giờ hoạt động

if (salesVelocity > normalVelocity × 1.5 && soldToday > avgDaily × 0.7) {
  warningLevel = salesVelocity > normalVelocity × 2 ? "CRITICAL" : "WARNING"
  hoursUntilOut = (avgDaily - soldToday) / salesVelocity
}
```

**Response:**
```json
[
  {
    "drinkId": 1,
    "drinkName": "Trà sữa trân châu",
    "imageUrl": "/uploads/drinks/tra-sua.jpg",
    "soldToday": 45,
    "avgDaily": 50,
    "salesVelocity": 5.6,
    "warningLevel": "CRITICAL",
    "message": "Dự kiến hết hàng trong 0.9 giờ"
  }
]
```

#### 15.2.4 Đề xuất nhân sự theo ngày 👥
**Endpoint:** `GET /api/manager/forecast/staffing`

**Chức năng:**
- Dự báo 7 ngày tới
- Số nhân viên đề xuất cho mỗi ngày
- Chi tiết theo từng khung giờ
- Highlight ngày cuối tuần (lượng đơn cao)
- Lý do đề xuất: "Dự kiến X đơn (cuối tuần)"

**Response:**
```json
[
  {
    "date": "2024-12-30",
    "dayOfWeek": "Thứ Hai",
    "currentStaff": null,
    "recommendedStaff": 4,
    "additionalNeeded": null,
    "reason": "Dự kiến 120 đơn (ngày thường), cần 5 nhân viên giờ cao điểm",
    "hourlyBreakdown": [
      {
        "hour": 8,
        "timeRange": "08:00 - 09:00",
        "recommendedStaff": 2,
        "expectedOrders": 12,
        "loadLevel": "MEDIUM"
      },
      {
        "hour": 14,
        "timeRange": "14:00 - 15:00",
        "recommendedStaff": 5,
        "expectedOrders": 35,
        "loadLevel": "VERY_HIGH"
      }
    ]
  }
]
```

#### 15.2.5 Cảnh báo nguy cơ quá tải 🚨
**Endpoint:** `GET /api/manager/forecast/overload`

**Chức năng:**
- Phát hiện khung giờ có thể vượt công suất
- % vượt công suất tối đa (50 đơn/giờ)
- Mức độ: WARNING / CRITICAL
- Đề xuất xử lý: "Cần tăng cường X nhân viên và chuẩn bị nguyên liệu gấp đôi"

**Thuật toán:**
```
maxCapacity = 50 đơn/giờ
if (maxOrders > maxCapacity × 0.8) {
  overloadPercent = (maxOrders / maxCapacity - 1) × 100
  severity = overloadPercent > 30 ? "CRITICAL" : "WARNING"
}
```

**Response:**
```json
[
  {
    "date": "2024-12-31",
    "hour": 14,
    "timeRange": "14:00 - 15:00",
    "expectedOrders": 65,
    "maxCapacity": 50,
    "overloadPercent": 30.0,
    "severity": "CRITICAL",
    "recommendation": "Cần tăng cường 9 nhân viên và chuẩn bị nguyên liệu gấp đôi"
  }
]
```

### 15.3 Quản lý đơn hàng (ManageOrdersFragment)
**Endpoints:**
- `GET /api/manager/orders` - Lấy danh sách đơn hàng
- `PUT /api/manager/orders/{id}/status` - Cập nhật trạng thái

**Chức năng:**
- **Tabs trạng thái**: 
  - Tất cả
  - Chờ xử lý (PENDING)
  - Đang làm (MAKING)
  - Đang giao (SHIPPING)
  - Sẵn sàng (READY)
  - Hoàn thành (DONE)
  - Đã hủy (CANCELED)
- **Lọc theo chi nhánh**: Bottom sheet chọn cửa hàng
- **Thống kê nhanh**: Số đơn Pending / Making / Done
- **Real-time updates**: WebSocket nhận đơn mới với thông báo âm thanh
- **Cập nhật trạng thái**: Chuyển đơn qua các trạng thái
- **Hủy đơn hàng**: Xác nhận và hủy đơn
- **Push Notification**: Gửi thông báo cho user khi cập nhật trạng thái

**WebSocket:**
```kotlin
// Subscribe to new orders
stompClient.subscribe("/topic/manager/orders") { message ->
    // Play notification sound
    // Show in-app notification
    // Refresh order list
}
```


### 15.4 Quản lý đồ uống (ManageDrinksFragment)
**Endpoints:**
- `GET /api/drinks` - Lấy danh sách đồ uống
- `POST /api/admin/drinks` - Thêm đồ uống mới
- `PUT /api/admin/drinks/{id}` - Sửa thông tin đồ uống
- `DELETE /api/admin/drinks/{id}` - Xóa đồ uống

**Chức năng:**
- Danh sách tất cả đồ uống (Grid view)
- Tìm kiếm đồ uống
- Lọc theo danh mục
- **Thêm đồ uống mới**:
  - Tên, mô tả
  - Giá cơ bản
  - Danh mục
  - Upload hình ảnh
  - Quản lý size (S/M/L/Jumbo) với giá cộng thêm
  - Quản lý topping với giá riêng
  - Trạng thái (Đang bán / Ngừng bán)
- **Sửa thông tin đồ uống**
- **Xóa đồ uống** (soft delete)
- **Bật/Tắt trạng thái** đồ uống

### 15.5 Quản lý danh mục (ManageCategoriesFragment)
**Endpoints:**
- `GET /api/categories` - Lấy danh sách danh mục
- `POST /api/manager/categories` - Thêm danh mục
- `PUT /api/manager/categories/{id}` - Sửa danh mục
- `DELETE /api/manager/categories/{id}` - Xóa danh mục

**Chức năng:**
- Danh sách danh mục
- Thêm/Sửa/Xóa danh mục
- Upload hình ảnh danh mục
- Sắp xếp thứ tự hiển thị
- Bật/Tắt trạng thái danh mục

### 15.6 Quản lý voucher (ManageVouchersFragment)
**Endpoints:**
- `GET /api/promotions/manager/all` - Lấy tất cả voucher
- `POST /api/promotions/manager` - Thêm voucher mới
- `PUT /api/promotions/manager/{id}` - Sửa voucher
- `DELETE /api/promotions/manager/{id}` - Xóa voucher
- `PATCH /api/promotions/manager/{id}/toggle-status` - Bật/Tắt voucher

**Chức năng:**
- **Danh sách voucher**: 
  - Tất cả
  - Đang hoạt động
  - Hết hạn
- **Tìm kiếm voucher**: Theo mã hoặc mô tả
- **Thống kê**: Tổng voucher / Voucher active
- **Thêm voucher mới**:
  - Mã voucher (unique)
  - Mô tả
  - Loại giảm giá:
    - PERCENTAGE: Giảm theo %
    - FIXED_AMOUNT: Giảm số tiền cố định
  - Giá trị giảm
  - Đơn tối thiểu
  - Giảm tối đa (nếu %)
  - Giới hạn sử dụng (số lần)
  - Thời gian hiệu lực (startDate - endDate)
- **Sửa/Xóa voucher**
- **Bật/Tắt trạng thái voucher**
- **Gửi thông báo**: Khi tạo voucher mới, có thể gửi push notification cho tất cả user

**Request:**
```json
{
  "code": "STUDENT20",
  "description": "Giảm 20% cho sinh viên",
  "type": "PERCENTAGE",
  "value": 20,
  "minOrderAmount": 50000,
  "maxDiscount": 30000,
  "maxUsage": 100,
  "startDate": "2024-12-30T00:00:00",
  "endDate": "2025-01-30T23:59:59",
  "isActive": true
}
```

### 15.7 Quản lý người dùng (ManageUsersFragment)
**Endpoints:**
- `GET /api/manager/users` - Lấy danh sách người dùng
- `GET /api/manager/users/{id}` - Xem chi tiết người dùng
- `PUT /api/manager/users/{id}/block` - Khóa/Mở khóa tài khoản
- `GET /api/manager/users/search` - Tìm kiếm người dùng
- `DELETE /api/manager/users/{id}` - Xóa người dùng

**Chức năng:**
- Danh sách người dùng (pagination)
- Tìm kiếm người dùng theo username, email, phone
- Lọc theo role (USER/MANAGER)
- Xem chi tiết người dùng:
  - Thông tin cá nhân
  - Hạng thành viên
  - Điểm tích lũy
  - Lịch sử đơn hàng
  - Tổng chi tiêu
- Khóa/Mở khóa tài khoản
- Xóa người dùng (backup doanh thu trước khi xóa)

### 15.8 Quản lý Live Chat (ManageChatsFragment)
**Endpoints:**
- `GET /api/chat/manager/conversations` - Lấy danh sách hội thoại
- `GET /api/chat/manager/conversations/waiting-count` - Đếm số hội thoại chờ
- `POST /api/chat/messages` - Gửi tin nhắn
- `POST /api/chat/conversations/{id}/close` - Đóng hội thoại

**Chức năng:**
- Danh sách cuộc hội thoại đang chờ
- Badge số lượng hội thoại chờ
- Tiếp nhận và trả lời chat real-time
- Đóng cuộc hội thoại
- Xem lịch sử chat

### 15.9 Quản lý cửa hàng (ManageStoresFragment)
**Endpoints:**
- `GET /api/stores` - Lấy danh sách cửa hàng
- `POST /api/stores` - Thêm cửa hàng
- `PUT /api/stores/{id}` - Sửa cửa hàng
- `DELETE /api/stores/{id}` - Xóa cửa hàng

**Chức năng:**
- Danh sách cửa hàng
- Thêm/Sửa/Xóa cửa hàng
- Thông tin cửa hàng:
  - Tên
  - Địa chỉ
  - Số điện thoại
  - Giờ mở cửa
  - Tọa độ (latitude, longitude)
- Hiển thị trên bản đồ

### 15.10 Thống kê doanh thu (RevenueStatistics)
**Endpoint:** `GET /api/manager/statistics/revenue?days=7&months=6`

**Chức năng:**
- Doanh thu theo ngày (7 ngày gần nhất)
- Doanh thu theo tháng (6 tháng gần nhất)
- Top sản phẩm bán chạy
- Biểu đồ Line Chart và Bar Chart
- Export báo cáo (PDF/Excel)

### 15.11 Gửi thông báo (NotificationManagerFragment)
**Chức năng:**
- Gửi thông báo đến tất cả user (OneSignal)
- Gửi thông báo đến user cụ thể
- Gửi thông báo theo segment (tier, location...)
- Lịch sử thông báo đã gửi
- Thống kê tỷ lệ mở thông báo

### 15.12 Cài đặt Manager (ManagerSettingsFragment)
**Chức năng:**
- Cài đặt thông báo
- Cài đặt âm thanh đơn hàng mới
- Đăng xuất

---

## 🔧 16. TÍNH NĂNG KỸ THUẬT

### 16.1 Network
**Công nghệ:**
- **Retrofit**: REST API client
- **OkHttp**: HTTP client với interceptor
- **AuthInterceptor**: Tự động thêm JWT token vào request header
- **Token Refresh**: Tự động refresh token khi hết hạn (401 Unauthorized)

**Cấu hình:**
```kotlin
object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8080/api/"
    
    private val authInterceptor = Interceptor { chain ->
        val token = SessionManager.getToken()
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
        chain.proceed(request)
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
```

### 16.2 Real-time (WebSocket)
**Công nghệ:**
- **STOMP over WebSocket**: Protocol cho messaging
- **SockJS**: Fallback khi WebSocket không khả dụng

**Managers:**
- **OrderWebSocketManager**: Nhận đơn hàng mới real-time (Manager)
- **LiveChatWebSocketManager**: Chat real-time với nhân viên
- **GroupChatWebSocketManager**: Chat nhóm trong phiên đặt hàng

**Cấu hình:**
```kotlin
class OrderWebSocketManager {
    private val stompClient = Stomp.over(
        Stomp.ConnectionProvider.OKHTTP,
        "ws://10.0.2.2:8080/ws"
    )
    
    fun connect() {
        stompClient.connect()
        stompClient.topic("/topic/manager/orders").subscribe { message ->
            // Handle new order
        }
    }
}
```


### 16.3 Caching (DataCache)
**Chức năng:**
- Cache categories, products, stores, user profile
- Giảm số lần gọi API
- Tự động refresh khi cần
- Memory cache với TTL (Time To Live)

**Cấu hình:**
```kotlin
object DataCache {
    private val categoryCache = mutableMapOf<String, CacheEntry<List<Category>>>()
    private const val CACHE_TTL = 5 * 60 * 1000L // 5 minutes
    
    fun getCategories(): List<Category>? {
        val entry = categoryCache["categories"]
        return if (entry != null && !entry.isExpired()) {
            entry.data
        } else null
    }
    
    fun setCategories(data: List<Category>) {
        categoryCache["categories"] = CacheEntry(data, System.currentTimeMillis() + CACHE_TTL)
    }
}
```

### 16.4 Security
**Công nghệ:**
- **JWT Token**: Expiration 24 hours
- **BCrypt**: Password hashing (cost factor: 10)
- **Android KeyStore**: Mã hóa token cho biometric login
- **Biometric Authentication**: Vân tay/Face ID

**KeyStoreManager:**
```java
public class KeyStoreManager {
    private static final String KEY_ALIAS = "UTE_TEA_KEY";
    
    public String encryptToken(String token) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        
        SecretKey key = (SecretKey) keyStore.getKey(KEY_ALIAS, null);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        
        byte[] encrypted = cipher.doFinal(token.getBytes());
        return Base64.encodeToString(encrypted, Base64.DEFAULT);
    }
}
```

### 16.5 Voice Features
**Công nghệ:**
- **Speech-to-Text**: Android SpeechRecognizer
- **VoiceOrderHelper**: Đặt hàng bằng giọng nói
- **VoiceSearchHelper**: Tìm kiếm bằng giọng nói

**Cấu hình:**
```kotlin
class VoiceOrderHelper(private val context: Context) {
    private val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    
    fun startListening(callback: (String) -> Unit) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
        }
        
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { callback(it) }
            }
        })
        
        speechRecognizer.startListening(intent)
    }
}
```

### 16.6 UI/UX
**Công nghệ:**
- **Material Design 3**: Giao diện hiện đại
- **Animations**: 
  - Fade in/out
  - Slide in/out
  - Fall down
  - Zoom
  - Rotate
- **Dark Mode**: Hỗ trợ chế độ tối
- **Lottie**: Animation loading
- **Seasonal Effects**:
  - SakuraView (Hoa anh đào)
  - SnowfallView (Tuyết rơi)
  - FallingLeavesView (Lá vàng)
  - SunshineView (Ánh nắng)
  - TetView (Pháo hoa Tết)

**SeasonalEffectManager:**
```kotlin
class SeasonalEffectManager(private val context: Context) {
    fun getCurrentEffect(): View? {
        val month = Calendar.getInstance().get(Calendar.MONTH)
        return when (month) {
            Calendar.JANUARY, Calendar.FEBRUARY -> TetView(context)
            Calendar.MARCH, Calendar.APRIL -> SakuraView(context)
            Calendar.JUNE, Calendar.JULY, Calendar.AUGUST -> SunshineView(context)
            Calendar.SEPTEMBER, Calendar.OCTOBER -> FallingLeavesView(context)
            Calendar.DECEMBER -> SnowfallView(context)
            else -> null
        }
    }
}
```

### 16.7 Push Notification (OneSignal)
**Chức năng:**
- Gửi thông báo khi cập nhật trạng thái đơn hàng
- Gửi thông báo khuyến mãi mới
- Gửi thông báo sinh nhật
- Gửi thông báo voucher sắp hết hạn
- Segmentation theo Member Tier

**Cấu hình:**
```kotlin
class UTETeaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize OneSignal
        OneSignal.initWithContext(this)
        OneSignal.setAppId("YOUR_ONESIGNAL_APP_ID")
        
        // Set external user ID
        val userId = SessionManager.getUserId()
        if (userId != null) {
            OneSignal.setExternalUserId(userId.toString())
        }
    }
}
```

**Backend Service:**
```java
@Service
public class OneSignalService {
    private static final String APP_ID = "YOUR_APP_ID";
    private static final String REST_API_KEY = "YOUR_REST_API_KEY";
    
    public void sendToUser(String userId, String title, String content) {
        // Send notification to specific user
    }
    
    public void sendToAll(String title, String content) {
        // Send notification to all users
    }
}
```

### 16.8 Payment Integration
**VNPay:**
- Sandbox environment
- Secure hash validation
- WebView integration
- Callback handling

**VietQR:**
- QR code generation
- Bank account info
- Transfer content auto-fill

### 16.9 Location Services
**Chức năng:**
- Lấy vị trí hiện tại (GPS)
- Tính khoảng cách đến cửa hàng
- Google Maps Distance Matrix API
- Chỉ đường đến cửa hàng

**LocationHelper:**
```kotlin
class LocationHelper(private val context: Context) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    
    fun getCurrentLocation(callback: (Location) -> Unit) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let { callback(it) }
            }
        }
    }
}
```

---

## 📊 17. MODELS DỮ LIỆU CHÍNH

### 17.1 User Models
```kotlin
data class User(
    val id: Long,
    val username: String,
    val email: String,
    val fullName: String,
    val phone: String?,
    val address: String?,
    val avatarUrl: String?,
    val role: UserRole, // USER, MANAGER
    val memberTier: MemberTier, // BRONZE, SILVER, GOLD, PLATINUM
    val points: Int,
    val isActive: Boolean,
    val createdAt: String
)

enum class UserRole { USER, MANAGER }
enum class MemberTier { BRONZE, SILVER, GOLD, PLATINUM }
```

### 17.2 Product Models
```kotlin
data class Drink(
    val id: Long,
    val name: String,
    val description: String?,
    val basePrice: BigDecimal,
    val imageUrl: String?,
    val categoryId: Long,
    val categoryName: String,
    val isActive: Boolean,
    val sizes: List<DrinkSize>,
    val toppings: List<DrinkTopping>,
    val avgRating: Double?,
    val reviewCount: Int?
)

data class DrinkSize(
    val id: Long,
    val sizeName: String, // S, M, L, Jumbo
    val extraPrice: BigDecimal
)

data class DrinkTopping(
    val id: Long,
    val toppingName: String,
    val price: BigDecimal
)

data class Category(
    val id: Long,
    val name: String,
    val description: String?,
    val imageUrl: String?,
    val displayOrder: Int,
    val isActive: Boolean
)
```

### 17.3 Order Models
```kotlin
data class Order(
    val id: Long,
    val userId: Long,
    val storeId: Long,
    val storeName: String,
    val status: OrderStatus,
    val type: OrderType, // DELIVERY, PICKUP
    val deliveryAddress: String?,
    val paymentMethod: PaymentMethod, // COD, VNPAY, VIETQR
    val subtotal: BigDecimal,
    val discount: BigDecimal,
    val tierDiscount: BigDecimal,
    val shippingFee: BigDecimal,
    val finalPrice: BigDecimal,
    val promotionCode: String?,
    val items: List<OrderItem>,
    val createdAt: String,
    val updatedAt: String
)

enum class OrderStatus {
    PENDING, MAKING, SHIPPING, READY, DONE, CANCELED
}

enum class OrderType { DELIVERY, PICKUP }
enum class PaymentMethod { COD, VNPAY, VIETQR }

data class OrderItem(
    val id: Long,
    val drinkId: Long,
    val drinkName: String,
    val drinkImage: String?,
    val sizeNameSnapshot: String,
    val basePriceSnapshot: BigDecimal,
    val sizeExtraPriceSnapshot: BigDecimal,
    val quantity: Int,
    val note: String?,
    val toppings: List<OrderItemTopping>,
    val itemTotal: BigDecimal
)
```

### 17.4 Group Order Models
```kotlin
data class GroupOrder(
    val id: Long,
    val inviteCode: String,
    val hostId: Long,
    val hostName: String,
    val storeId: Long,
    val storeName: String,
    val status: GroupOrderStatus,
    val deliveryType: OrderType,
    val deliveryAddress: String?,
    val maxMembers: Int,
    val expiresAt: String,
    val members: List<GroupOrderMember>,
    val items: List<GroupOrderItem>,
    val totalAmount: BigDecimal,
    val createdAt: String
)

enum class GroupOrderStatus {
    OPEN, LOCKED, COMPLETED, CANCELED, EXPIRED
}

data class GroupOrderMember(
    val userId: Long,
    val username: String,
    val fullName: String,
    val avatarUrl: String?,
    val isHost: Boolean,
    val joinedAt: String
)
```


### 17.5 Promotion & Loyalty Models
```kotlin
data class Promotion(
    val id: Long,
    val code: String,
    val description: String,
    val type: PromotionType, // PERCENTAGE, FIXED_AMOUNT
    val value: BigDecimal,
    val minOrderAmount: BigDecimal,
    val maxDiscount: BigDecimal?,
    val maxUsage: Int,
    val usageCount: Int,
    val startDate: String,
    val endDate: String,
    val isActive: Boolean
)

enum class PromotionType { PERCENTAGE, FIXED_AMOUNT }

data class SpinReward(
    val id: Long,
    val code: String,
    val discountPercent: Int,
    val expiresAt: String,
    val isUsed: Boolean
)

data class MemberTierBenefits(
    val currentTier: MemberTier,
    val tierName: String,
    val tierColor: String,
    val currentPoints: Int,
    val pointsToNextTier: Int,
    val nextTier: MemberTier?,
    val nextTierName: String?,
    val progressPercent: Double,
    val discountPercent: BigDecimal,
    val pointsMultiplier: Double,
    val freeShipping: Boolean,
    val freeShippingMinOrder: BigDecimal,
    val birthdayVoucher: Boolean,
    val birthdayVoucherPercent: Int,
    val prioritySupport: Boolean,
    val exclusiveOffers: Boolean,
    val earlyAccess: Boolean,
    val description: String,
    val benefitsList: List<String>,
    val allTiers: List<TierInfo>
)
```

### 17.6 Forecast Models ⭐ MỚI
```kotlin
data class ForecastDto(
    val revenueForecast: RevenueForecast,
    val peakHours: List<PeakHourAnalysis>,
    val lowStockWarnings: List<LowStockWarning>,
    val staffingRecommendations: List<StaffingRecommendation>,
    val overloadWarnings: List<OverloadWarning>
)

data class RevenueForecast(
    val todayForecast: BigDecimal,
    val tomorrowForecast: BigDecimal,
    val weekForecast: BigDecimal,
    val monthForecast: BigDecimal,
    val growthRate: Double,
    val trend: String, // UP, DOWN, STABLE
    val dailyForecasts: List<DailyForecast>
)

data class PeakHourAnalysis(
    val hour: Int,
    val timeRange: String,
    val avgOrders: Long,
    val avgRevenue: BigDecimal,
    val peakLevel: String, // LOW, MEDIUM, HIGH, VERY_HIGH
    val recommendedStaff: Int
)

data class LowStockWarning(
    val drinkId: Long,
    val drinkName: String,
    val imageUrl: String?,
    val soldToday: Long,
    val avgDaily: Long,
    val salesVelocity: Double,
    val warningLevel: String, // WARNING, CRITICAL
    val message: String
)

data class StaffingRecommendation(
    val date: LocalDate,
    val dayOfWeek: String,
    val currentStaff: Int?,
    val recommendedStaff: Int,
    val additionalNeeded: Int?,
    val reason: String,
    val hourlyBreakdown: List<HourlyStaffing>
)

data class OverloadWarning(
    val date: LocalDate,
    val hour: Int,
    val timeRange: String,
    val expectedOrders: Long,
    val maxCapacity: Long,
    val overloadPercent: Double,
    val severity: String, // WARNING, CRITICAL
    val recommendation: String
)
```

### 17.7 Predictive Order Models ⭐ MỚI
```kotlin
data class PredictiveOrderDto(
    val hasPrediction: Boolean,
    val message: String,
    val predictedDrink: PredictedDrink?,
    val triggerReasons: List<String>,
    val confidenceScore: Double
)

data class PredictedDrink(
    val drinkId: Long,
    val drinkName: String,
    val drinkImage: String?,
    val sizeName: String?,
    val sizeId: Long?,
    val price: BigDecimal,
    val orderCount: Int,
    val lastOrderTime: String?,
    val toppings: List<PredictedTopping>,
    val note: String?
)

data class PredictedTopping(
    val toppingId: Long,
    val toppingName: String,
    val price: BigDecimal
)
```

### 17.8 Chat Models
```kotlin
data class LiveConversation(
    val id: Long,
    val userId: Long,
    val userName: String,
    val userAvatar: String?,
    val managerId: Long?,
    val managerName: String?,
    val status: ConversationStatus, // WAITING, ACTIVE, CLOSED
    val messages: List<LiveMessage>,
    val createdAt: String,
    val closedAt: String?
)

enum class ConversationStatus { WAITING, ACTIVE, CLOSED }

data class LiveMessage(
    val id: Long,
    val senderId: Long,
    val senderName: String,
    val senderRole: UserRole,
    val content: String,
    val sentAt: String
)

data class GroupChatMessage(
    val id: Long,
    val groupOrderId: Long,
    val senderId: Long?,
    val senderName: String?,
    val senderAvatar: String?,
    val messageType: MessageType, // USER, SYSTEM
    val content: String,
    val sentAt: String
)

enum class MessageType { USER, SYSTEM }
```

### 17.9 Review Models
```kotlin
data class Review(
    val id: Long,
    val userId: Long,
    val userName: String,
    val userAvatar: String?,
    val drinkId: Long,
    val drinkName: String,
    val orderItemId: Long,
    val rating: Int, // 1-5
    val comment: String?,
    val isAnonymous: Boolean,
    val createdAt: String
)

data class DrinkRatingSummary(
    val drinkId: Long,
    val avgRating: Double,
    val totalReviews: Int,
    val ratingDistribution: Map<Int, Int> // 1 star: 5, 2 star: 3, ...
)
```

---

## 🔗 18. API ENDPOINTS TỔNG HỢP

### 18.1 Authentication
```
POST   /api/auth/register-with-otp      - Đăng ký với OTP
POST   /api/auth/otp-verify              - Xác thực OTP
POST   /api/auth/resend-otp              - Gửi lại OTP
POST   /api/auth/login                   - Đăng nhập
POST   /api/auth/refresh-token           - Refresh JWT token
POST   /api/auth/forgot-password         - Quên mật khẩu
POST   /api/auth/reset-password          - Đặt lại mật khẩu
GET    /api/auth/health                  - Health check
```

### 18.2 User Profile
```
GET    /api/me                           - Lấy profile
PUT    /api/me                           - Cập nhật profile
POST   /api/me/avatar                    - Upload avatar
PUT    /api/me/change-password           - Đổi mật khẩu
DELETE /api/me                           - Xóa tài khoản
```

### 18.3 Products
```
GET    /api/drinks                       - Danh sách đồ uống
GET    /api/drinks/{id}                  - Chi tiết đồ uống
GET    /api/drinks?search={keyword}      - Tìm kiếm đồ uống
GET    /api/categories                   - Danh sách danh mục
GET    /api/categories/{id}/drinks       - Sản phẩm theo danh mục
GET    /api/stores                       - Danh sách cửa hàng
```

### 18.4 Cart & Orders
```
POST   /api/cart/add                     - Thêm vào giỏ
GET    /api/cart/{userId}                - Lấy giỏ hàng
PUT    /api/cart/update                  - Cập nhật giỏ hàng
DELETE /api/cart/remove                  - Xóa khỏi giỏ
POST   /api/orders                       - Tạo đơn hàng
GET    /api/orders/my                    - Đơn hàng của tôi
GET    /api/orders/my/current            - Đơn đang xử lý
GET    /api/orders/{id}                  - Chi tiết đơn hàng
```

### 18.5 Payments
```
POST   /api/vnpay/create-payment-amount  - Tạo payment URL
POST   /api/vnpay/create-order-after-payment - Tạo đơn sau thanh toán
GET    /api/vnpay/callback               - Callback từ VNPAY
```

### 18.6 Reviews
```
POST   /api/reviews                      - Tạo đánh giá
GET    /api/reviews/drink/{drinkId}      - Đánh giá theo sản phẩm
GET    /api/reviews/drink/{drinkId}/paged - Đánh giá (phân trang)
GET    /api/reviews/drink/{drinkId}/summary - Thống kê đánh giá
GET    /api/reviews/my-reviews           - Đánh giá của tôi
GET    /api/reviews/can-review/{orderItemId} - Kiểm tra có thể đánh giá
DELETE /api/reviews/{id}                 - Xóa đánh giá
```

### 18.7 Group Order
```
POST   /api/group-orders                 - Tạo phiên nhóm
POST   /api/group-orders/join            - Tham gia nhóm
GET    /api/group-orders/{id}            - Thông tin phiên
GET    /api/group-orders/code/{code}     - Lấy phiên theo mã mời
GET    /api/group-orders/active          - Phiên đang hoạt động
GET    /api/group-orders/my-orders       - Lịch sử phiên
PUT    /api/group-orders/{id}            - Cập nhật phiên
POST   /api/group-orders/{id}/items      - Thêm món
PUT    /api/group-orders/{id}/items/{itemId} - Sửa món
DELETE /api/group-orders/{id}/items/{itemId} - Xóa món
POST   /api/group-orders/{id}/lock       - Khóa phiên
POST   /api/group-orders/{id}/unlock     - Mở khóa phiên
POST   /api/group-orders/{id}/leave      - Rời khỏi phiên
POST   /api/group-orders/{id}/checkout   - Thanh toán nhóm
DELETE /api/group-orders/{id}            - Hủy phiên
```

### 18.8 Group Chat
```
POST   /api/group-orders/{id}/chat       - Gửi tin nhắn
GET    /api/group-orders/{id}/chat       - Lịch sử chat
GET    /api/group-orders/{id}/chat/recent - Tin nhắn gần đây
WS     /topic/group-chat/{id}            - WebSocket subscribe
```

### 18.9 Loyalty & Member Tier
```
GET    /api/loyalty/points               - Điểm tích lũy
POST   /api/loyalty/spin                 - Quay vòng quay
GET    /api/loyalty/rewards              - Voucher đã nhận
GET    /api/loyalty/voucher/validate     - Validate voucher
GET    /api/loyalty/tier/benefits        - Quyền lợi tier
POST   /api/loyalty/tier/check-upgrade   - Kiểm tra nâng hạng
GET    /api/loyalty/tier/preview-discount - Preview giảm giá tier
```

### 18.10 Predictive Order ⭐ MỚI
```
GET    /api/predictive-order             - Gợi ý món dự đoán
GET    /api/predictive-order/user/{userId} - Gợi ý cho user cụ thể
```

### 18.11 Chatbot
```
POST   /api/chatbot/message              - Chat với bot
GET    /api/chatbot/health               - Health check
```

### 18.12 Live Chat
```
POST   /api/chat/conversations           - Tạo hội thoại
POST   /api/chat/messages                - Gửi tin nhắn
GET    /api/chat/conversations/my        - Hội thoại của tôi
GET    /api/chat/conversations/{id}      - Chi tiết hội thoại
POST   /api/chat/conversations/{id}/close - Đóng hội thoại
WS     /topic/chat/{conversationId}      - WebSocket subscribe
```


### 18.13 Promotions
```
GET    /api/promotions                   - Voucher đang hoạt động
GET    /api/promotions/{id}              - Chi tiết voucher
GET    /api/promotions/validate          - Validate voucher
GET    /api/promotions/manager/all       - Tất cả voucher (Manager)
POST   /api/promotions/manager           - Tạo voucher (Manager)
PUT    /api/promotions/manager/{id}      - Sửa voucher (Manager)
DELETE /api/promotions/manager/{id}      - Xóa voucher (Manager)
PATCH  /api/promotions/manager/{id}/toggle-status - Bật/Tắt voucher
```

### 18.14 Manager - Dashboard & Statistics
```
GET    /api/manager/summary              - Dashboard tổng quan
GET    /api/manager/statistics/revenue   - Thống kê doanh thu
```

### 18.15 Manager - Orders
```
GET    /api/manager/orders               - Danh sách đơn hàng
PUT    /api/manager/orders/{id}/status   - Cập nhật trạng thái
WS     /topic/manager/orders             - WebSocket đơn mới
```

### 18.16 Manager - Forecast ⭐⭐⭐⭐⭐ MỚI
```
GET    /api/manager/forecast             - Toàn bộ dữ liệu dự báo
GET    /api/manager/forecast/revenue     - Dự báo doanh thu
GET    /api/manager/forecast/peak-hours  - Phân tích giờ cao điểm
GET    /api/manager/forecast/low-stock   - Cảnh báo món sắp hết
GET    /api/manager/forecast/staffing    - Đề xuất nhân sự
GET    /api/manager/forecast/overload    - Cảnh báo quá tải
```

### 18.17 Manager - Users
```
GET    /api/manager/users                - Danh sách người dùng
GET    /api/manager/users/{id}           - Chi tiết người dùng
PUT    /api/manager/users/{id}/block     - Khóa/Mở khóa tài khoản
GET    /api/manager/users/search         - Tìm kiếm người dùng
DELETE /api/manager/users/{id}           - Xóa người dùng
```

### 18.18 Manager - Products
```
POST   /api/admin/drinks                 - Thêm đồ uống
PUT    /api/admin/drinks/{id}            - Sửa đồ uống
DELETE /api/admin/drinks/{id}            - Xóa đồ uống
```

### 18.19 Manager - Categories
```
GET    /api/admin/categories             - Danh sách danh mục
POST   /api/admin/categories             - Thêm danh mục
PUT    /api/admin/categories/{id}        - Sửa danh mục
DELETE /api/admin/categories/{id}        - Xóa danh mục
POST   /api/manager/categories           - Tạo danh mục
PUT    /api/manager/categories/{id}      - Cập nhật danh mục
DELETE /api/manager/categories/{id}      - Xóa danh mục
```

### 18.20 Manager - Live Chat
```
GET    /api/chat/manager/conversations   - Danh sách hội thoại
GET    /api/chat/manager/conversations/waiting-count - Số hội thoại chờ
```

---

## 🎨 19. UI/UX HIGHLIGHTS

### 19.1 Animations
**Chức năng:**
- **Fade In/Out**: Hiển thị/ẩn elements mượt mà
- **Slide In/Out**: Chuyển màn hình với hiệu ứng trượt
- **Fall Down**: Sản phẩm rơi xuống khi load
- **Zoom**: Phóng to/thu nhỏ hình ảnh
- **Rotate**: Quay vòng (Spin Wheel)
- **Add to Cart**: Animation bay vào giỏ hàng
- **Confetti**: Pháo giấy khi đạt thành tựu
- **Fireworks**: Pháo hoa khi nâng hạng

### 19.2 Seasonal Effects
**Chức năng:**
- **Mùa xuân (3-4)**: Hoa anh đào rơi (SakuraView)
- **Mùa hè (6-8)**: Ánh nắng (SunshineView)
- **Mùa thu (9-10)**: Lá vàng rơi (FallingLeavesView)
- **Mùa đông (12)**: Tuyết rơi (SnowfallView)
- **Tết (1-2)**: Pháo hoa và lì xì (TetView)

### 19.3 Custom Views
**Chức năng:**
- **SpinWheelView**: Vòng quay may mắn với animation
- **TeaRefreshDrawable**: Pull-to-refresh với icon trà sữa
- **LoadingDialog**: Dialog loading với Lottie animation
- **InAppNotification**: Thông báo trong app (snackbar custom)

### 19.4 Haptic Feedback
**Chức năng:**
- Rung nhẹ khi thêm vào giỏ hàng
- Rung khi quay vòng quay
- Rung khi nhận được thông báo
- Rung khi nâng hạng thành viên

### 19.5 Dark Mode
**Chức năng:**
- Hỗ trợ chế độ tối toàn bộ app
- Tự động theo hệ thống hoặc chọn thủ công
- Màu sắc tối ưu cho từng theme

---

## 🔄 20. LUỒNG HOẠT ĐỘNG CHI TIẾT

### 20.1 Luồng đăng ký
```
1. User mở app → StartActivity
2. Click "Đăng ký" → RegisterActivity
3. Nhập thông tin (username, email, password, phone)
4. Click "Đăng ký" → POST /api/auth/register-with-otp
5. Backend gửi OTP qua email
6. Chuyển sang OtpActivity
7. Nhập mã OTP 6 số
8. Click "Xác nhận" → POST /api/auth/otp-verify
9. Backend kích hoạt tài khoản
10. Hiển thị thông báo thành công
11. Chuyển về LoginActivity
```

### 20.2 Luồng đặt hàng
```
1. User browse menu → MenuFragment
2. Click sản phẩm → ProductDetailActivity
3. Chọn size, topping, số lượng
4. Click "Thêm vào giỏ" → POST /api/cart/add
5. Animation bay vào giỏ hàng
6. Click icon giỏ hàng → CartActivity
7. Chọn sản phẩm cần đặt (checkbox)
8. Chọn chi nhánh, loại giao hàng
9. Nhập địa chỉ (nếu Delivery)
10. Chọn voucher (optional)
11. Chọn phương thức thanh toán
12. Click "Đặt hàng"
    - Nếu COD: POST /api/orders → Tạo đơn ngay
    - Nếu VNPAY: POST /api/vnpay/create-payment-amount → Mở WebView
13. Thanh toán thành công → Tạo đơn hàng
14. Xóa giỏ hàng
15. Hiển thị thông báo thành công
16. Chuyển về OrderFragment
```

### 20.3 Luồng đặt hàng nhóm
```
1. User click "Đặt nhóm" → GroupOrderActivity
2. Chọn "Tạo phiên mới" → CreateGroupOrderActivity
3. Nhập thông tin phiên (chi nhánh, loại giao hàng, số thành viên, thời gian)
4. Click "Tạo phiên" → POST /api/group-orders
5. Nhận mã mời 6 ký tự
6. Chia sẻ mã mời cho bạn bè
7. Bạn bè nhập mã mời → POST /api/group-orders/join
8. Thành viên thêm món → POST /api/group-orders/{id}/items
9. Real-time update qua WebSocket
10. Chat trong nhóm → POST /api/group-orders/{id}/chat
11. Host khóa phiên → POST /api/group-orders/{id}/lock
12. Host thanh toán → POST /api/group-orders/{id}/checkout
13. Tạo đơn hàng chung
14. Phiên chuyển sang COMPLETED
```

### 20.4 Luồng Manager xử lý đơn
```
1. Manager login → ManagerActivity
2. Vào ManageOrdersFragment
3. WebSocket connect → /topic/manager/orders
4. Đơn mới đến → Hiển thị notification + âm thanh
5. Click đơn hàng → Xem chi tiết
6. Cập nhật trạng thái:
   PENDING → MAKING → SHIPPING/READY → DONE
7. PUT /api/manager/orders/{id}/status
8. Backend gửi push notification cho user
9. User nhận thông báo cập nhật trạng thái
```

### 20.5 Luồng Predictive Order
```
1. User mở app → HomeFragment
2. Backend phân tích:
   - Lịch sử đơn hàng
   - Thời gian hiện tại (giờ, thứ)
   - Tần suất đặt món
   - Độ gần đây
3. Tính score cho từng món
4. Nếu score > 0.2 → Hiển thị dialog gợi ý
5. Dialog hiển thị:
   - Hình ảnh món
   - Tên, size, giá
   - Số lần đã đặt
   - Lý do gợi ý
6. User click "Thêm vào giỏ" → POST /api/cart/add
7. Hoặc click "Để sau" → Đóng dialog
8. Lưu trạng thái để không spam
```

---

## 📈 21. THỐNG KÊ & PHÂN TÍCH

### 21.1 Dashboard Manager
**Metrics:**
- Doanh thu hôm nay
- Tổng đơn hàng
- Đơn chờ xử lý
- Số khách hàng mới
- Tỷ lệ hoàn thành đơn
- Giá trị đơn hàng trung bình

### 21.2 Revenue Statistics
**Metrics:**
- Doanh thu theo ngày (7 ngày)
- Doanh thu theo tháng (6 tháng)
- Top 10 sản phẩm bán chạy
- Doanh thu theo danh mục
- Doanh thu theo chi nhánh
- Tỷ lệ tăng trưởng

### 21.3 Forecast Analytics ⭐ MỚI
**Metrics:**
- Dự báo doanh thu (hôm nay, ngày mai, tuần, tháng)
- Tỷ lệ tăng trưởng (%)
- Xu hướng (UP/DOWN/STABLE)
- Giờ cao điểm (số đơn, doanh thu)
- Món sắp hết (tốc độ bán)
- Đề xuất nhân sự (theo ngày, theo giờ)
- Cảnh báo quá tải (% vượt công suất)

---

## 🔒 22. BẢO MẬT & QUYỀN HẠN

### 22.1 Authentication
- JWT Token (24h expiration)
- Refresh Token (30 days)
- BCrypt password hashing
- Biometric authentication
- Android KeyStore encryption

### 22.2 Authorization
**USER Role:**
- Xem menu, sản phẩm
- Đặt hàng, thanh toán
- Xem lịch sử đơn hàng
- Chat với nhân viên
- Tham gia đặt hàng nhóm
- Quay vòng quay
- Đánh giá sản phẩm

**MANAGER Role:**
- Tất cả quyền của USER
- Xem dashboard, thống kê
- Quản lý đơn hàng (tất cả)
- Quản lý sản phẩm, danh mục
- Quản lý voucher
- Quản lý người dùng
- Xem dự báo & cảnh báo
- Gửi thông báo

### 22.3 Data Protection
- HTTPS cho tất cả API calls
- SQL Injection prevention (JPA)
- XSS prevention
- CSRF protection
- Rate limiting
- Input validation

---

## 📱 23. PUSH NOTIFICATION

### 23.1 Loại thông báo
**Order Updates:**
- Đơn hàng đang được xử lý
- Đơn hàng đang giao
- Đơn hàng sẵn sàng lấy
- Đơn hàng hoàn thành
- Đơn hàng bị hủy

**Promotions:**
- Voucher mới
- Khuyến mãi đặc biệt
- Flash sale

**Loyalty:**
- Nâng hạng thành viên
- Voucher sinh nhật
- Điểm sắp hết hạn

**Group Order:**
- Có người tham gia nhóm
- Host khóa/mở khóa đơn
- Đơn nhóm đã thanh toán

### 23.2 Segmentation
- Theo Member Tier (BRONZE, SILVER, GOLD, PLATINUM)
- Theo vị trí (gần cửa hàng nào)
- Theo hành vi (khách hàng thường xuyên, lâu không đặt)

---

## 🎯 24. KẾT LUẬN

**UTE Tea** là một ứng dụng đặt đồ uống hoàn chỉnh với các tính năng:

### 24.1 Điểm mạnh
✅ **Giao diện đẹp**: Material Design 3, animations mượt mà
✅ **Tính năng đầy đủ**: Từ đặt hàng cơ bản đến nâng cao (group order, predictive order)
✅ **Real-time**: WebSocket cho chat và đơn hàng
✅ **Thông minh**: AI chatbot, predictive order, forecast analytics
✅ **Bảo mật**: JWT, biometric, encryption
✅ **Thanh toán đa dạng**: COD, VNPAY, VietQR
✅ **Loyalty program**: Member tier với quyền lợi hấp dẫn
✅ **Manager tools**: Dashboard, forecast, analytics

### 24.2 Công nghệ nổi bật
🔥 **Predictive Order**: Dự đoán món khách hàng muốn đặt
🔥 **Forecast Analytics**: Dự báo doanh thu, cảnh báo quá tải
🔥 **Group Order**: Đặt hàng nhóm với chat real-time
🔥 **Member Tier**: Hệ thống hạng thành viên 4 cấp
🔥 **Voice Order**: Đặt hàng bằng giọng nói
🔥 **Biometric Login**: Đăng nhập sinh trắc học

### 24.3 Số liệu
- **16+ màn hình** chính
- **100+ API endpoints**
- **30+ models** dữ liệu
- **4 cấp hạng** thành viên
- **3 phương thức** thanh toán
- **2 loại** đặt hàng (cá nhân, nhóm)
- **Real-time** chat & orders

---

*Tài liệu được tạo tự động từ source code ứng dụng UTE Tea Android & Backend API.*
*Phiên bản: 1.0.0*
*Ngày cập nhật: 30/12/2024*

A) Tạo alert trực tiếp (ngay lập tức) Brute force Nếu LOGIN_FAILED >= 5 lần trong 15 phút → tạo alert AlertType.BRUTE_FORCE, CRITICAL Payment fraud Nếu PAYMENT_FAILED >= 3 lần trong 1 giờ → tạo alert AlertType.PAYMENT_FRAUD, HIGH Spam request Khi gọi logSpamRequest(userId, endpoint, requestCount, ...) → tạo alert AlertType.SPAM_DETECTED, HIGH (không cần đợi cộng dồn theo giờ) Promotion abuse Khi gọi logPromotionAbuse(...) → tạo alert AlertType.PROMOTION_ABUSE, MEDIUM Order cancel abuse Nếu ORDER_CANCEL >= 3 trong 24h → chỉ log + tăng risk score (CHƯA alert) Nếu ORDER_CANCEL >= 5 trong 24h → mới tạo alert AlertType.ORDER_ABUSE, MEDIUM Lưu ý: cái này khác với bạn mô tả “3+ là alert”. Trong code: 3+ chỉ warning, 5+ mới alert. đưa tôi lệnh

Alert cho đăng nhập từ thiết bị/IP mới
Alert cho đơn hàng giá trị cao bất thường
Alert cho thay đổi mật khẩu
Alert cho nhiều lần đổi địa chỉ