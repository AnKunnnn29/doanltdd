# 📋 BACKEND FUNCTIONS - UTE TEA SYSTEM

## 🏗️ KIẾN TRÚC TỔNG QUAN

Backend được xây dựng theo mô hình **Spring Boot MVC** với kiến trúc phân lớp:
- **Controller Layer**: Xử lý HTTP requests/responses
- **Service Layer**: Business logic và xử lý nghiệp vụ
- **Security/Filter Layer**: Bảo mật và kiểm soát truy cập
- **Repository Layer**: Truy cập dữ liệu
- **Model Layer**: Định nghĩa entities và enums
- **DTO/Mapper Layer**: Chuyển đổi dữ liệu

---

## 🎯 1. CONTROLLER LAYER

### 🔐 AuthController
**Endpoint**: `/api/auth/**`
- `POST /register-with-otp` - Đăng ký tài khoản với xác thực OTP
- `POST /otp-verify` - Xác thực OTP và kích hoạt tài khoản
- `POST /resend-otp` - Gửi lại mã OTP
- `POST /login` - Đăng nhập với JWT token
- `POST /forgot-password` - Quên mật khẩu (gửi OTP qua email)
- `POST /reset-password` - Đặt lại mật khẩu với OTP
- `POST /refresh-token` - Làm mới access token
- `GET /health` - Kiểm tra trạng thái service

### 🥤 DrinkController
**Endpoint**: `/api/drinks/**`
- `GET /` - Lấy danh sách tất cả đồ uống active
- `GET /page` - Lấy danh sách đồ uống có phân trang
- `GET /{id}` - Lấy chi tiết đồ uống theo ID
- `GET /search` - Tìm kiếm đồ uống theo từ khóa

### 📦 OrderController
**Endpoint**: `/api/orders/**`
- `POST /` - Tạo đơn hàng mới
- `GET /my` - Lấy danh sách đơn hàng của user hiện tại
- `GET /my/current` - Lấy đơn hàng đang xử lý của user
- `GET /user/{userId}` - Lấy đơn hàng của user cụ thể (có kiểm tra quyền)
- `GET /{orderId}` - Lấy chi tiết đơn hàng
- `GET /all` - Lấy tất cả đơn hàng (Manager only)
- `PUT /{orderId}/status` - Cập nhật trạng thái đơn hàng (Manager only)

### 🛒 CartController
**Endpoint**: `/api/cart/**`
- `POST /add` - Thêm sản phẩm vào giỏ hàng
- `GET /` - Lấy giỏ hàng của user hiện tại
- `GET /{userId}` - Lấy giỏ hàng của user (có kiểm tra quyền)
- `PUT /items/{cartItemId}` - Cập nhật số lượng sản phẩm
- `DELETE /items/{cartItemId}` - Xóa sản phẩm khỏi giỏ
- `DELETE /clear` - Xóa toàn bộ giỏ hàng

### 👤 UserProfileController
**Endpoint**: `/api/me/**`
- `GET /` - Lấy thông tin profile user
- `PUT /` - Cập nhật thông tin profile
- `POST /avatar` - Upload avatar
- `PUT /change-password` - Đổi mật khẩu
- `DELETE /` - Xóa tài khoản

### 🎁 PromotionController
**Endpoint**: `/api/promotions/**`
- `GET /` - Lấy danh sách voucher active (User)
- `GET /{id}` - Lấy chi tiết voucher
- `GET /validate` - Validate mã voucher
- `GET /manager/all` - Lấy tất cả voucher (Manager)
- `POST /manager` - Tạo voucher mới (Manager)
- `PUT /manager/{id}` - Cập nhật voucher (Manager)
- `DELETE /manager/{id}` - Xóa voucher (Manager)
- `PATCH /manager/{id}/toggle-status` - Bật/tắt voucher (Manager)

### 👥 GroupOrderController
**Endpoint**: `/api/group-orders/**`
- `POST /` - Tạo phiên đặt hàng nhóm
- `POST /join` - Tham gia phiên bằng mã mời
- `GET /{id}` - Lấy thông tin phiên
- `GET /code/{inviteCode}` - Lấy phiên theo mã mời
- `GET /active` - Lấy phiên đang hoạt động của user
- `GET /my-orders` - Lịch sử phiên của user
- `PUT /{id}` - Cập nhật phiên (host only)
- `POST /{id}/items` - Thêm món vào phiên
- `PUT /{id}/items/{itemId}` - Cập nhật món
- `DELETE /{id}/items/{itemId}` - Xóa món
- `POST /{id}/lock` - Khóa phiên (host only)
- `POST /{id}/unlock` - Mở khóa phiên (host only)
- `POST /{id}/leave` - Rời khỏi phiên
- `POST /{id}/checkout` - Thanh toán đơn nhóm (host only)
- `DELETE /{id}` - Hủy phiên (host only)

### 💬 LiveChatController
**Endpoint**: `/api/chat/**`
- `POST /conversations` - Bắt đầu cuộc hội thoại mới
- `POST /messages` - Gửi tin nhắn
- `GET /conversations/my` - Lấy danh sách cuộc hội thoại
- `GET /conversations/{id}` - Lấy chi tiết cuộc hội thoại
- `POST /conversations/{id}/close` - Đóng cuộc hội thoại
- `GET /manager/conversations` - Lấy cuộc hội thoại (Manager)
- `GET /manager/conversations/waiting-count` - Đếm cuộc hội thoại chờ

### 🏆 LoyaltyController
**Endpoint**: `/api/loyalty/**`
- `GET /points` - Lấy thông tin điểm thưởng
- `POST /spin` - Quay vòng xoay may mắn
- `GET /rewards` - Lấy danh sách voucher chưa dùng
- `GET /voucher/validate` - Validate mã voucher spin
- `GET /tier/benefits` - Lấy quyền lợi hạng thành viên
- `POST /tier/check-upgrade` - Kiểm tra nâng cấp tier
- `GET /tier/preview-discount` - Preview discount theo tier

### 🛡️ UserMonitoringController
**Endpoint**: `/api/monitoring/**` (Admin only)
- `GET /dashboard` - Dashboard tổng quan giám sát
- `GET /activities` - Lấy log hoạt động với filter
- `GET /activities/user/{userId}` - Log hoạt động của user
- `GET /alerts` - Lấy danh sách cảnh báo
- `GET /alerts/pending` - Cảnh báo chờ xử lý
- `PUT /alerts/{alertId}/handle` - Xử lý cảnh báo
- `GET /risk-scores` - Lấy điểm rủi ro users
- `GET /risk-scores/user/{userId}` - Điểm rủi ro của user
- `POST /risk-scores/user/{userId}/note` - Thêm ghi chú admin
- `POST /risk-scores/user/{userId}/reset` - Reset điểm rủi ro
- `POST /users/{userId}/unblock` - Mở khóa user

---

## ⚙️ 2. SERVICE LAYER

### 🔐 AuthService
**Chức năng chính:**
- Đăng ký tài khoản với OTP verification
- Đăng nhập với JWT authentication
- Quên mật khẩu và reset password
- Refresh token mechanism
- Ghi log hoạt động đăng nhập/đăng xuất
- Kiểm tra thiết bị/IP mới đăng nhập

**Tính năng bảo mật:**
- Rate limiting cho login attempts
- Monitoring đăng nhập thất bại
- Kiểm tra tài khoản bị khóa/vô hiệu hóa

### 🥤 DrinkService
**Chức năng chính:**
- Quản lý danh sách đồ uống
- Tìm kiếm đồ uống với input sanitization
- Tối ưu hóa N+1 query problem
- Batch loading sizes và toppings
- Phân trang và filter

**Tối ưu hóa:**
- JOIN FETCH để giảm số query
- Batch loading cho pagination
- Input validation và sanitization

### 📦 OrderService
**Chức năng chính:**
- Tạo đơn hàng với validation nghiêm ngặt
- Áp dụng voucher/promotion
- Tính toán discount theo member tier
- Cập nhật trạng thái đơn hàng
- Tích điểm loyalty khi hoàn thành
- Gửi thông báo real-time

**Tính năng bảo mật:**
- Validate số lượng items và quantity
- Rate limiting cho việc tạo đơn
- Ghi log tất cả hoạt động đơn hàng
- Kiểm tra quyền truy cập

### 🛒 CartService
**Chức năng chính:**
- Quản lý giỏ hàng cá nhân
- Thêm/sửa/xóa sản phẩm
- Tính toán giá với sizes và toppings
- Gửi thông báo khi thêm sản phẩm
- Ghi log hoạt động giỏ hàng

### 👤 UserProfileService
**Chức năng chính:**
- Quản lý thông tin cá nhân
- Upload và quản lý avatar
- Đổi mật khẩu với validation
- Xóa tài khoản (soft delete)
- Backup dữ liệu trước khi xóa

### 🎁 PromotionService
**Chức năng chính:**
- Quản lý voucher/promotion
- Validate mã voucher với business rules
- Tính toán discount (percent/fixed)
- Kiểm tra điều kiện áp dụng
- Theo dõi usage limit

### 👥 GroupOrderService
**Chức năng chính:**
- Tạo và quản lý phiên đặt hàng nhóm
- Mã mời và join mechanism
- Quản lý items trong phiên
- Lock/unlock phiên
- Checkout đơn hàng nhóm
- Real-time chat trong phiên

### 💬 LiveChatService
**Chức năng chính:**
- Hệ thống chat real-time
- Quản lý cuộc hội thoại
- Phân quyền user/manager
- WebSocket notifications
- Lưu trữ lịch sử chat

### 🏆 LoyaltyService & MemberTierService
**Chức năng chính:**
- Hệ thống điểm thưởng
- Vòng xoay may mắn
- Quản lý hạng thành viên (Bronze/Silver/Gold/Diamond)
- Tính toán discount theo tier
- Nâng cấp tier tự động
- Quản lý voucher từ spin wheel

### 🛡️ UserMonitoringService
**Chức năng chính:**
- Giám sát hành vi người dùng
- Tính toán risk score
- Tạo cảnh báo tự động
- Ghi log tất cả hoạt động
- Phát hiện hành vi bất thường
- Quản lý IP bị chặn

---

## 🔒 3. SECURITY & FILTER LAYER

### 🛡️ SecurityConfig
**Cấu hình bảo mật:**
- JWT Authentication
- Role-based authorization (USER/MANAGER/ADMIN)
- CORS configuration
- Public/Protected endpoints
- Method-level security

**Phân quyền chi tiết:**
- Public: Auth, Drinks, Stores, Categories, Weather
- USER: Orders, Cart, Profile, Chat, Loyalty
- MANAGER: Order management, Promotions, Live chat
- ADMIN: User monitoring, IP blocking, System management

### 🔑 JwtAuthenticationFilter
**Chức năng:**
- Xác thực JWT token từ header
- Extract user information
- Set authentication context
- Token validation và expiry check

### ⚡ RateLimitFilter
**Chức năng:**
- Rate limiting theo IP
- Khác nhau cho từng endpoint:
  - Auth endpoints: Stricter limits
  - OTP endpoints: Very strict limits
  - General endpoints: Normal limits
- Ghi log rate limit violations
- Trả về thông tin remaining requests

### 🚫 BlockedIPFilter
**Chức năng:**
- Chặn IP bị blacklist
- Chạy trước tất cả filters khác
- Normalize IPv6 localhost
- Ghi log blocked attempts
- Bypass cho static resources

---

## 🗄️ 4. REPOSITORY & MODEL LAYER

### 📊 Core Models

#### 👤 User Model
```java
- id, username, email, phone, password
- fullName, address, role, memberTier
- points, active, isBlocked
- otp, otpExpiry, avatarUrl
- managedStores (for Manager role)
```

#### 📦 Order Model
```java
- user, store, type, address, pickupTime
- status, totalPrice, discount, finalPrice
- paymentMethod, promotion
- items (Set<OrderItem>)
```

#### 🥤 Drink Model
```java
- name, description, imageUrl, basePrice
- isActive, category
- sizes (List<DrinkSize>)
- toppings (List<DrinkTopping>)
```

#### 🎁 Promotion Model
```java
- code, description, discountType, discountValue
- startDate, endDate, minOrderValue
- maxDiscountAmount, usageLimit, usedCount
- isActive
```

### 🔍 Repository Features
- Custom queries với JPQL/Native SQL
- Pagination và sorting
- Batch operations
- Optimized joins để tránh N+1 problem
- Soft delete support

---

## 📋 5. DTO & MAPPER LAYER

### 📤 Response DTOs
- **ApiResponse<T>**: Wrapper cho tất cả API responses
- **OrderDto**: Chi tiết đơn hàng với items
- **DrinkDto**: Thông tin đồ uống với sizes/toppings
- **UserProfileDto**: Thông tin profile user
- **CartDto**: Giỏ hàng với items và total

### 📥 Request DTOs
- **LoginRequest**: Username/phone + password
- **RegisterRequest**: Thông tin đăng ký
- **OrderRequest**: Tạo đơn hàng với items
- **AddToCartRequest**: Thêm vào giỏ hàng
- **CreatePromotionRequest**: Tạo voucher mới

### 🔄 Mappers
- **DrinkMapper**: Convert Drink entity ↔ DrinkDto
- **PromotionMapper**: Convert Promotion entity ↔ PromotionDto
- Manual mapping trong services cho performance

---

## 🚀 6. TÍNH NĂNG ĐỘC ĐÁO

### 🛡️ User Monitoring System
- **Risk Score Calculation**: Tính điểm rủi ro dựa trên hành vi
- **Activity Logging**: Ghi log tất cả hoạt động quan trọng
- **Alert System**: Cảnh báo tự động khi phát hiện bất thường
- **IP Blocking**: Chặn IP có hành vi xấu

### 👥 Group Order System
- **Collaborative Ordering**: Đặt hàng nhóm với mã mời
- **Real-time Updates**: Cập nhật real-time khi có thay đổi
- **Role Management**: Host có quyền quản lý phiên
- **Chat Integration**: Chat trong phiên đặt hàng

### 🏆 Loyalty & Gamification
- **Member Tiers**: 4 hạng thành viên với quyền lợi khác nhau
- **Spin Wheel**: Vòng xoay may mắn với vouchers
- **Points System**: Tích điểm và đổi thưởng
- **Tier Benefits**: Discount và bonus theo hạng

### 💬 Live Chat System
- **Real-time Messaging**: WebSocket cho chat real-time
- **Conversation Management**: Quản lý cuộc hội thoại
- **Manager Assignment**: Phân công manager theo store
- **Chat History**: Lưu trữ lịch sử chat

### 📊 Analytics & Reporting
- **Revenue Statistics**: Thống kê doanh thu
- **Order Analytics**: Phân tích đơn hàng
- **User Behavior**: Theo dõi hành vi người dùng
- **Predictive Ordering**: Dự đoán đơn hàng

---

## 🔧 7. TÍNH NĂNG KỸ THUẬT

### ⚡ Performance Optimization
- **N+1 Query Prevention**: JOIN FETCH và batch loading
- **Caching**: Redis cho session và rate limiting
- **Database Indexing**: Index cho các truy vấn thường xuyên
- **Pagination**: Phân trang cho large datasets

### 🔒 Security Features
- **JWT Authentication**: Stateless authentication
- **Role-based Authorization**: Phân quyền chi tiết
- **Rate Limiting**: Chống spam và abuse
- **Input Validation**: Validate tất cả input
- **SQL Injection Prevention**: Parameterized queries

### 📡 Real-time Features
- **WebSocket**: Real-time notifications
- **Push Notifications**: OneSignal integration
- **Live Updates**: Order status, chat messages
- **Event-driven Architecture**: Async processing

### 🌐 Integration Features
- **VNPay Payment**: Thanh toán online
- **Email Service**: SendGrid cho email
- **SMS Service**: OTP qua SMS
- **Weather API**: Thông tin thời tiết
- **File Upload**: Cloudinary cho images

---

## 📈 8. MONITORING & LOGGING

### 📊 Application Monitoring
- **Health Checks**: Endpoint kiểm tra trạng thái
- **Metrics Collection**: Performance metrics
- **Error Tracking**: Exception handling và logging
- **Audit Trails**: Theo dõi tất cả thay đổi quan trọng

### 🔍 Security Monitoring
- **Failed Login Attempts**: Theo dõi đăng nhập thất bại
- **Suspicious Activities**: Phát hiện hành vi bất thường
- **IP Tracking**: Theo dõi IP và thiết bị
- **Rate Limit Violations**: Ghi log vi phạm rate limit

---

## 🎯 9. BUSINESS LOGIC HIGHLIGHTS

### 💰 Pricing & Discounts
- **Dynamic Pricing**: Giá theo size và toppings
- **Multi-tier Discounts**: Voucher + Member tier discount
- **Minimum Order Value**: Điều kiện áp dụng voucher
- **Maximum Discount**: Giới hạn giảm giá

### 📦 Order Management
- **Status Workflow**: PENDING → MAKING → SHIPPING/READY → DONE
- **Inventory Validation**: Kiểm tra tồn kho
- **Payment Integration**: Multiple payment methods
- **Notification System**: Thông báo mọi thay đổi

### 👥 User Management
- **Account Lifecycle**: Registration → Verification → Active
- **Profile Management**: Update thông tin, avatar
- **Security Features**: Password change, account deletion
- **Tier Progression**: Tự động nâng cấp hạng thành viên

---

Hệ thống backend UTE Tea được thiết kế với kiến trúc hiện đại, bảo mật cao và khả năng mở rộng tốt, đáp ứng đầy đủ nhu cầu của một ứng dụng đặt trà sữa chuyên nghiệp.