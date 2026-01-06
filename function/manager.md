# 📚 MANAGER MODULE - TÀI LIỆU HỌC TẬP CHI TIẾT

## 📋 MỤC LỤC

1. [Tổng Quan Chức Năng Manager](#1-tổng-quan-chức-năng-manager)
2. [Kiến Trúc Hệ Thống](#2-kiến-trúc-hệ-thống)
3. [Quản Lý Đơn Hàng](#3-quản-lý-đơn-hàng)
4. [Quản Lý Người Dùng](#4-quản-lý-người-dùng)
5. [Quản Lý Cửa Hàng](#5-quản-lý-cửa-hàng)
6. [Dashboard & Thống Kê](#6-dashboard--thống-kê)
7. [Dự Báo & Phân Tích](#7-dự-báo--phân-tích)
8. [Whitelist IP - Bảo Mật Nâng Cao](#8-whitelist-ip---bảo-mật-nâng-cao)
9. [Hệ Thống Giám Sát (Monitoring)](#9-hệ-thống-giám-sát-monitoring)
10. [Luồng Logic Chi Tiết](#10-luồng-logic-chi-tiết)
11. [Giải Thích Code](#11-giải-thích-code)
12. [API Reference](#12-api-reference)
13. [Câu Hỏi Phỏng Vấn](#13-câu-hỏi-phỏng-vấn)

---

## 1. TỔNG QUAN CHỨC NĂNG MANAGER

### 1.1 Vai Trò Trong Hệ Thống
```
┌─────────────────────────────────────────────────────────────┐
│                    HỆ THỐNG UTE TEA                         │
├─────────────────────────────────────────────────────────────┤
│  ADMIN (Quản trị viên)                                      │
│  ├── Quản lý TẤT CẢ cửa hàng                               │
│  ├── Quản lý TẤT CẢ người dùng (bao gồm Manager)           │
│  ├── Phân quyền: Nâng/Hạ cấp User ↔ Manager                │
│  ├── Gán cửa hàng cho Manager                               │
│  ├── Quản lý Whitelist IP                                   │
│  └── Xem doanh thu backup (user đã xóa)                     │
├─────────────────────────────────────────────────────────────┤
│  MANAGER (Quản lý cửa hàng)                                 │
│  ├── Quản lý đơn hàng của CỬA HÀNG ĐƯỢC GÁN                │
│  ├── Quản lý người dùng (chỉ USER, không được khóa Manager)│
│  ├── Quản lý voucher/khuyến mãi                             │
│  ├── Xem dashboard & thống kê                               │
│  ├── Dự báo doanh thu, nhân sự                              │
│  └── Live Chat với khách hàng                               │
├─────────────────────────────────────────────────────────────┤
│  USER (Người dùng)                                          │
│  └── Đặt hàng, thanh toán, đánh giá...                      │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 Phân Quyền Chi Tiết
| Chức năng | USER | MANAGER | ADMIN |
|-----------|------|---------|-------|
| Xem đơn hàng | Của mình | Của store được gán | Tất cả |
| Cập nhật trạng thái đơn | ❌ | ✅ (store được gán) | ✅ |
| Khóa/Mở khóa User | ❌ | ✅ (chỉ USER) | ✅ |
| Khóa/Mở khóa Manager | ❌ | ❌ | ✅ |
| Nâng cấp User → Manager | ❌ | ❌ | ✅ |
| Gán store cho Manager | ❌ | ❌ | ✅ |
| Quản lý Whitelist IP | ❌ | ❌ | ✅ |
| Xem doanh thu backup | ❌ | ❌ | ✅ |
| Quản lý voucher/khuyến mãi | ❌ | ✅ | ✅ |
| Xem dashboard thống kê | ❌ | ✅ (store được gán) | ✅ (tất cả) |
| Dự báo doanh thu | ❌ | ✅ | ✅ |
| Live Chat hỗ trợ | ❌ | ✅ | ✅ |
| Monitoring & Risk Score | ❌ | ❌ | ✅ |

### 1.3 Luồng Hoạt Động Chính
```
┌─────────────────────────────────────────────────────────────────────┐
│                    LUỒNG HOẠT ĐỘNG MANAGER                          │
└─────────────────────────────────────────────────────────────────────┘

    ┌──────────────┐
    │ 1. ĐĂNG NHẬP │
    │ Manager App  │
    └──────┬───────┘
           │ JWT Authentication + Whitelist IP Check
           ▼
    ┌──────────────────────────────────────────────────────────────┐
    │ 2. KIỂM TRA QUYỀN & STORE                                    │
    │ - Xác thực role = MANAGER hoặc ADMIN                         │
    │ - Lấy danh sách stores được gán (nếu là Manager)             │
    │ - ADMIN: quản lý tất cả stores                               │
    │ - MANAGER: chỉ quản lý stores được gán                       │
    └──────────┬───────────────────────────────────────────────────┘
               ▼
    ┌──────────────────────────────────────────────────────────────┐
    │ 3. DASHBOARD & THỐNG KÊ                                      │
    │ - Xem tổng quan doanh thu, đơn hàng                          │
    │ - Top sản phẩm bán chạy                                      │
    │ - Thống kê theo store (Manager) hoặc tất cả (Admin)          │
    └──────────┬───────────────────────────────────────────────────┘
               ▼
    ┌──────────────────────────────────────────────────────────────┐
    │ 4. QUẢN LÝ ĐƠN HÀNG                                          │
    │ - Xem danh sách đơn hàng (filter theo store)                │
    │ - Cập nhật trạng thái: PENDING → MAKING → SHIPPING → DONE   │
    │ - Gửi notification realtime cho khách hàng                   │
    └──────────┬───────────────────────────────────────────────────┘
               ▼
    ┌──────────────────────────────────────────────────────────────┐
    │ 5. QUẢN LÝ NGƯỜI DÙNG                                        │
    │ - Xem danh sách users                                        │
    │ - Khóa/Mở khóa tài khoản USER                                │
    │ - Nâng cấp User → Manager (chỉ Admin)                        │
    │ - Gán store cho Manager (chỉ Admin)                          │
    └──────────┬───────────────────────────────────────────────────┘
               ▼
    ┌──────────────────────────────────────────────────────────────┐
    │ 6. TÍNH NĂNG NÂNG CAO                                        │
    │ - Dự báo doanh thu, giờ cao điểm                             │
    │ - Quản lý voucher/khuyến mãi                                 │
    │ - Live Chat hỗ trợ khách hàng                                │
    │ - Monitoring & Risk Score (chỉ Admin)                        │
    └──────────────────────────────────────────────────────────────┘
```

### 1.4 Tính Năng Nổi Bật
```
┌─────────────────────────────────────────────────────────────────────┐
│                    TÍNH NĂNG NỔI BẬT MANAGER MODULE                 │
└─────────────────────────────────────────────────────────────────────┘

    🏪 QUẢN LÝ THEO STORE
    ├── Manager chỉ quản lý stores được Admin gán
    ├── Admin quản lý tất cả stores
    ├── Phân quyền chặt chẽ theo store
    └── Dashboard riêng biệt theo store

    📊 DASHBOARD THÔNG MINH
    ├── Realtime revenue tracking
    ├── Top selling products
    ├── Peak hours analysis
    ├── Staff recommendations
    └── Overload warnings

    🔮 DỰ BÁO & PHÂN TÍCH
    ├── Revenue forecasting (7 ngày, 30 ngày)
    ├── Peak hours prediction
    ├── Low stock warnings
    ├── Staffing recommendations
    └── Growth trend analysis

    🛡️ BẢO MẬT NÂNG CAO
    ├── Whitelist IP cho Admin/Manager
    ├── Blocked IP tự động
    ├── Risk scoring system
    ├── Brute force detection
    └── Activity monitoring

    💬 LIVE CHAT HỖ TRỢ
    ├── Chat realtime với khách hàng
    ├── WebSocket connection
    ├── Queue management
    ├── Chat history
    └── Transfer between managers

    📱 REALTIME NOTIFICATIONS
    ├── WebSocket cho đơn hàng mới
    ├── Push notifications
    ├── Email notifications
    ├── Alert system
    └── Status updates
```

### 1.5 Kiến Trúc Phân Quyền
```
┌─────────────────────────────────────────────────────────────────────┐
│                    KIẾN TRÚC PHÂN QUYỀN                             │
└─────────────────────────────────────────────────────────────────────┘

    ┌─────────────────────────────────────────────────────────────────┐
    │                         ADMIN                                   │
    │  ┌─────────────────────────────────────────────────────────┐   │
    │  │ • Quản lý TẤT CẢ stores (không cần gán)                 │   │
    │  │ • getManagedStoreIds() → return null (xem tất cả)       │   │
    │  │ • Có thể nâng/hạ cấp User ↔ Manager                     │   │
    │  │ • Gán/bỏ gán store cho Manager                          │   │
    │  │ • Quản lý Whitelist IP                                  │   │
    │  │ • Xem Monitoring & Risk Score                           │   │
    │  │ • Xem doanh thu backup (user đã xóa)                    │   │
    │  └─────────────────────────────────────────────────────────┘   │
    └─────────────────────────────────────────────────────────────────┘

    ┌─────────────────────────────────────────────────────────────────┐
    │                        MANAGER                                  │
    │  ┌─────────────────────────────────────────────────────────┐   │
    │  │ • Chỉ quản lý stores được Admin gán                     │   │
    │  │ • getManagedStoreIds() → return [1, 2, 3]              │   │
    │  │ • Có thể khóa/mở USER (không khóa Manager khác)         │   │
    │  │ • Xem dashboard chỉ của stores được gán                 │   │
    │  │ • Quản lý đơn hàng chỉ của stores được gán              │   │
    │  │ • Tạo/quản lý voucher                                   │   │
    │  │ • Live Chat với khách hàng                              │   │
    │  │ • KHÔNG xem Monitoring (chỉ Admin)                      │   │
    │  └─────────────────────────────────────────────────────────┘   │
    └─────────────────────────────────────────────────────────────────┘

    ┌─────────────────────────────────────────────────────────────────┐
    │                         USER                                    │
    │  ┌─────────────────────────────────────────────────────────┐   │
    │  │ • Chỉ xem/quản lý dữ liệu của chính mình                │   │
    │  │ • Đặt hàng, thanh toán, đánh giá                        │   │
    │  │ • Không có quyền quản lý                                │   │
    │  └─────────────────────────────────────────────────────────┘   │
    └─────────────────────────────────────────────────────────────────┘
```

---

## 2. KIẾN TRÚC HỆ THỐNG

### 2.1 Sơ Đồ Kiến Trúc Backend
```
┌─────────────────────────────────────────────────────────────────────┐
│                         ANDROID APP (Kotlin)                        │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐     │
│  │ManagerActivity  │  │ManagerSettings  │  │ ManageOrders    │     │
│  │                 │  │ Fragment        │  │ Fragment        │     │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘     │
│           │                    │                    │               │
│           └────────────────────┼────────────────────┘               │
│                                │                                    │
│                    ┌───────────▼───────────┐                        │
│                    │    RetrofitClient     │                        │
│                    │    (ApiService)       │                        │
│                    └───────────┬───────────┘                        │
└────────────────────────────────┼────────────────────────────────────┘
                                 │ HTTP/HTTPS
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      SPRING BOOT BACKEND                            │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                    SECURITY FILTERS                          │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐   │   │
│  │  │BlockedIPFilter│→│JwtAuthFilter │→│WhitelistIPFilter │   │   │
│  │  └──────────────┘  └──────────────┘  └──────────────────┘   │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                │                                    │
│  ┌─────────────────────────────▼─────────────────────────────────┐ │
│  │                      CONTROLLERS                               │ │
│  │  ┌────────────────┐  ┌────────────────┐  ┌─────────────────┐  │ │
│  │  │ManagerController│  │WhitelistedIP  │  │ ForecastController│ │ │
│  │  │                │  │ Controller     │  │                 │  │ │
│  │  └───────┬────────┘  └───────┬────────┘  └────────┬────────┘  │ │
│  └──────────┼───────────────────┼───────────────────┼────────────┘ │
│             │                   │                   │               │
│  ┌──────────▼───────────────────▼───────────────────▼────────────┐ │
│  │                       SERVICES                                 │ │
│  │  ┌────────────────┐  ┌────────────────┐  ┌─────────────────┐  │ │
│  │  │ ManagerService │  │WhitelistedIP   │  │ ForecastService │  │ │
│  │  │                │  │ Service        │  │                 │  │ │
│  │  └───────┬────────┘  └───────┬────────┘  └────────┬────────┘  │ │
│  └──────────┼───────────────────┼───────────────────┼────────────┘ │
│             │                   │                   │               │
│  ┌──────────▼───────────────────▼───────────────────▼────────────┐ │
│  │                     REPOSITORIES                               │ │
│  │  ┌────────────────┐  ┌────────────────┐  ┌─────────────────┐  │ │
│  │  │OrderRepository │  │WhitelistedIP   │  │ UserRepository  │  │ │
│  │  │                │  │ Repository     │  │                 │  │ │
│  │  └───────┬────────┘  └───────┬────────┘  └────────┬────────┘  │ │
│  └──────────┼───────────────────┼───────────────────┼────────────┘ │
│             │                   │                   │               │
│             └───────────────────┼───────────────────┘               │
│                                 ▼                                   │
│                    ┌───────────────────────┐                        │
│                    │      MySQL 8.0        │                        │
│                    │   (Database Server)   │                        │
│                    └───────────────────────┘                        │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.2 Cấu Trúc File Backend
```
Backend_APP/src/main/java/com/utetea/backend/
├── controller/
│   ├── ManagerController.java      # API quản lý cho Manager
│   └── WhitelistedIPController.java # API quản lý Whitelist IP
├── service/
│   ├── ManagerService.java         # Business logic Manager
│   ├── WhitelistedIPService.java   # Logic Whitelist IP
│   └── ForecastService.java        # Dự báo doanh thu, nhân sự
├── filter/
│   ├── JwtAuthenticationFilter.java # Xác thực JWT
│   ├── BlockedIPFilter.java        # Chặn IP bị block
│   └── WhitelistIPFilter.java      # Kiểm tra Whitelist IP
├── model/
│   ├── User.java                   # Entity User
│   ├── Order.java                  # Entity Order
│   └── WhitelistedIP.java          # Entity Whitelist IP
└── repository/
    ├── UserRepository.java
    ├── OrderRepository.java
    └── WhitelistedIPRepository.java
```

---

## 3. QUẢN LÝ ĐƠN HÀNG

### 3.1 Luồng Cập Nhật Trạng Thái Đơn Hàng
```
┌─────────────────────────────────────────────────────────────────────┐
│                  FLOW: CẬP NHẬT TRẠNG THÁI ĐƠN HÀNG                 │
└─────────────────────────────────────────────────────────────────────┘

    ┌──────────────┐
    │ Manager App  │
    │ (Android)    │
    └──────┬───────┘
           │ 1. PUT /api/manager/orders/{id}/status?status=MAKING
           │    Header: Authorization: Bearer <JWT>
           ▼
    ┌──────────────────────────────────────────────────────────────┐
    │                    SECURITY FILTERS                          │
    │  ┌────────────────┐                                          │
    │  │ BlockedIPFilter│ → Kiểm tra IP có bị block không          │
    │  └───────┬────────┘                                          │
    │          │ ✅ IP không bị block                              │
    │          ▼                                                   │
    │  ┌────────────────┐                                          │
    │  │JwtAuthFilter   │ → Xác thực JWT token                     │
    │  │                │ → Extract username, role từ token        │
    │  └───────┬────────┘                                          │
    │          │ ✅ Token hợp lệ, role = MANAGER                   │
    │          ▼                                                   │
    │  ┌────────────────┐                                          │
    │  │WhitelistFilter │ → Kiểm tra IP có trong whitelist         │
    │  │                │   (Chỉ áp dụng cho ADMIN/MANAGER)        │
    │  └───────┬────────┘                                          │
    │          │ ✅ IP trong whitelist (hoặc tính năng tắt)        │
    └──────────┼───────────────────────────────────────────────────┘
               ▼
    ┌──────────────────────────────────────────────────────────────┐
    │                   ManagerController                          │
    │  @PutMapping("/orders/{orderId}/status")                     │
    │  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")             │
    │                                                              │
    │  2. Validate status parameter (PENDING, MAKING, DONE...)     │
    │  3. Gọi managerService.updateOrderStatus(orderId, newStatus) │
    └──────────┬───────────────────────────────────────────────────┘
               ▼
    ┌──────────────────────────────────────────────────────────────┐
    │                    ManagerService                            │
    │                                                              │
    │  4. getCurrentManager() → Lấy user từ SecurityContext        │
    │  5. getManagedStoreIds(manager) → Lấy danh sách store IDs    │
    │     - ADMIN: return null (quản lý tất cả)                    │
    │     - MANAGER: return list store IDs được gán                │
    │                                                              │
    │  6. orderRepository.findById(orderId) → Lấy order            │
    │                                                              │
    │  7. validateStoreAccess(manager, order.store.id)             │
    │     - ADMIN: bỏ qua kiểm tra                                 │
    │     - MANAGER: kiểm tra order.store.id có trong storeIds     │
    │       → Nếu không có quyền: throw BusinessException          │
    │                                                              │
    │  8. orderService.updateOrderStatus(orderId, newStatus)       │
    └──────────┬───────────────────────────────────────────────────┘
               ▼
    ┌──────────────────────────────────────────────────────────────┐
    │                     OrderService                             │
    │                                                              │
    │  9. Validate transition hợp lệ:                              │
    │     PENDING → MAKING → SHIPPING → READY → DONE               │
    │     Bất kỳ → CANCELED                                        │
    │                                                              │
    │  10. order.setStatus(newStatus)                              │
    │  11. orderRepository.save(order)                             │
    │                                                              │
    │  12. Gửi thông báo:                                          │
    │      - WebSocket: notifyOrderStatusChange(order)             │
    │      - Push: oneSignalService.sendToUser(...)                │
    │      - Email: emailService.sendOrderStatusEmail(...)         │
    └──────────┬───────────────────────────────────────────────────┘
               ▼
    ┌──────────────────────────────────────────────────────────────┐
    │                      RESPONSE                                │
    │  {                                                           │
    │    "success": true,                                          │
    │    "message": "Order status updated",                        │
    │    "data": { ...order details... }                           │
    │  }                                                           │
    └──────────────────────────────────────────────────────────────┘
```

### 3.2 Trạng Thái Đơn Hàng (OrderStatus)
```
┌─────────────────────────────────────────────────────────────────────┐
│                    VÒNG ĐỜI ĐƠN HÀNG                                │
└─────────────────────────────────────────────────────────────────────┘

    ┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
    │ PENDING  │────▶│  MAKING  │────▶│ SHIPPING │────▶│  READY   │
    │ (Chờ xử  │     │ (Đang    │     │ (Đang    │     │ (Sẵn     │
    │  lý)     │     │  pha chế)│     │  giao)   │     │  sàng)   │
    └────┬─────┘     └────┬─────┘     └────┬─────┘     └────┬─────┘
         │                │                │                │
         │                │                │                ▼
         │                │                │           ┌──────────┐
         │                │                └──────────▶│   DONE   │
         │                │                            │ (Hoàn    │
         │                │                            │  thành)  │
         │                │                            └──────────┘
         │                │
         ▼                ▼
    ┌──────────────────────────┐
    │       CANCELED           │
    │   (Đã hủy - có thể      │
    │    hủy từ bất kỳ        │
    │    trạng thái nào)      │
    └──────────────────────────┘
```

---

## 4. QUẢN LÝ NGƯỜI DÙNG

### 4.1 Luồng Khóa/Mở Khóa Tài Khoản
```
┌─────────────────────────────────────────────────────────────────────┐
│                  FLOW: KHÓA/MỞ KHÓA TÀI KHOẢN                       │
└─────────────────────────────────────────────────────────────────────┘

    ┌──────────────┐
    │ Manager App  │
    └──────┬───────┘
           │ PUT /api/manager/users/{userId}/block?blocked=true
           ▼
    ┌──────────────────────────────────────────────────────────────┐
    │                   ManagerController                          │
    │  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")             │
    └──────────┬───────────────────────────────────────────────────┘
               ▼
    ┌──────────────────────────────────────────────────────────────┐
    │                    ManagerService                            │
    │                                                              │
    │  1. getCurrentManager() → Lấy current user                   │
    │                                                              │
    │  2. userRepository.findById(userId) → Lấy target user        │
    │                                                              │
    │  3. KIỂM TRA QUYỀN:                                          │
    │     ┌─────────────────────────────────────────────────────┐  │
    │     │ if (targetUser.role == ADMIN)                       │  │
    │     │   → throw "Không thể khóa tài khoản Admin"          │  │
    │     │                                                     │  │
    │     │ if (currentUser.role == MANAGER &&                  │  │
    │     │     targetUser.role == MANAGER)                     │  │
    │     │   → throw "Manager không có quyền khóa Manager khác"│  │
    │     └─────────────────────────────────────────────────────┘  │
    │                                                              │
    │  4. user.setIsBlocked(blocked)                               │
    │     user.setActive(!blocked)                                 │
    │                                                              │
    │  5. userRepository.save(user)                                │
    └──────────────────────────────────────────────────────────────┘
```

### 4.2 Luồng Nâng Cấp User → Manager (CHỈ ADMIN)
```
┌─────────────────────────────────────────────────────────────────────┐
│                  FLOW: NÂNG CẤP USER → MANAGER                      │
└─────────────────────────────────────────────────────────────────────┘

    ┌──────────────┐
    │  Admin App   │
    └──────┬───────┘
           │ PUT /api/manager/users/{userId}/promote
           ▼
    ┌──────────────────────────────────────────────────────────────┐
    │                    ManagerService                            │
    │                                                              │
    │  1. requireAdminRole() → Kiểm tra current user là ADMIN      │
    │     → Nếu không phải ADMIN: throw BusinessException          │
    │                                                              │
    │  2. userRepository.findById(userId) → Lấy target user        │
    │                                                              │
    │  3. KIỂM TRA:                                                │
    │     - Nếu đã là MANAGER → throw "Đã là Manager"              │
    │     - Nếu là ADMIN → throw "Không thể thay đổi role Admin"   │
    │     - Nếu bị khóa → throw "Không thể nâng cấp tài khoản bị   │
    │                            khóa"                             │
    │                                                              │
    │  4. user.setRole(UserRole.MANAGER)                           │
    │                                                              │
    │  5. userRepository.save(user)                                │
    │                                                              │
    │  ⚠️ LƯU Ý: Sau khi nâng cấp, Admin cần GÁN STORE cho        │
    │     Manager mới để họ có thể quản lý đơn hàng                │
    └──────────────────────────────────────────────────────────────┘
```

### 4.3 Luồng Gán Store Cho Manager (CHỈ ADMIN)
```
┌─────────────────────────────────────────────────────────────────────┐
│                  FLOW: GÁN STORE CHO MANAGER                        │
└─────────────────────────────────────────────────────────────────────┘

    ┌──────────────┐
    │  Admin App   │
    └──────┬───────┘
           │ POST /api/manager/users/{userId}/stores/{storeId}
           ▼
    ┌──────────────────────────────────────────────────────────────┐
    │                    ManagerService                            │
    │                                                              │
    │  1. requireAdminRole() → Chỉ ADMIN mới có quyền              │
    │                                                              │
    │  2. userRepository.findByIdWithManagedStores(managerId)      │
    │     → Lấy Manager với danh sách stores đã gán                │
    │                                                              │
    │  3. KIỂM TRA:                                                │
    │     - Nếu không phải MANAGER → throw "Không phải Manager"    │
    │     - Nếu đã gán store này → throw "Đã được gán"             │
    │                                                              │
    │  4. storeRepository.findById(storeId) → Lấy store            │
    │                                                              │
    │  5. manager.getManagedStores().add(store)                    │
    │                                                              │
    │  6. userRepository.save(manager)                             │
    │                                                              │
    │  ✅ Sau khi gán, Manager có thể:                             │
    │     - Xem đơn hàng của store                                 │
    │     - Cập nhật trạng thái đơn hàng                           │
    │     - Xem thống kê doanh thu của store                       │
    └──────────────────────────────────────────────────────────────┘
```

---

## 5. QUẢN LÝ CỬA HÀNG

### 5.1 Mối Quan Hệ Manager - Store
```
┌─────────────────────────────────────────────────────────────────────┐
│                    QUAN HỆ MANAGER - STORE                          │
└─────────────────────────────────────────────────────────────────────┘

    ┌─────────────────────────────────────────────────────────────────┐
    │                         ADMIN                                   │
    │  ┌─────────────────────────────────────────────────────────┐   │
    │  │ Quản lý TẤT CẢ stores (không cần gán)                   │   │
    │  │ getManagedStoreIds() → return null (xem tất cả)         │   │
    │  └─────────────────────────────────────────────────────────┘   │
    └─────────────────────────────────────────────────────────────────┘

    ┌─────────────────────────────────────────────────────────────────┐
    │                        MANAGER A                                │
    │  ┌─────────────────────────────────────────────────────────┐   │
    │  │ managedStores = [Store 1, Store 2]                      │   │
    │  │ getManagedStoreIds() → return [1, 2]                    │   │
    │  │                                                         │   │
    │  │ ✅ Có thể xem/cập nhật đơn hàng của Store 1, Store 2    │   │
    │  │ ❌ Không thể xem đơn hàng của Store 3                   │   │
    │  └─────────────────────────────────────────────────────────┘   │
    └─────────────────────────────────────────────────────────────────┘

    ┌─────────────────────────────────────────────────────────────────┐
    │                        MANAGER B                                │
    │  ┌─────────────────────────────────────────────────────────┐   │
    │  │ managedStores = [] (chưa được gán)                      │   │
    │  │ getManagedStoreIds() → return [] (empty list)           │   │
    │  │                                                         │   │
    │  │ ⚠️ Không thể xem bất kỳ đơn hàng nào                    │   │
    │  │ ⚠️ Dashboard hiển thị empty                             │   │
    │  └─────────────────────────────────────────────────────────┘   │
    └─────────────────────────────────────────────────────────────────┘
```

### 5.2 Database Schema - manager_stores
```sql
-- Bảng trung gian Many-to-Many giữa User (Manager) và Store
CREATE TABLE manager_stores (
  user_id BIGINT NOT NULL,      -- ID của Manager
  store_id BIGINT NOT NULL,     -- ID của Store
  PRIMARY KEY (user_id, store_id),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE
);

-- Ví dụ dữ liệu:
-- | user_id | store_id |
-- |---------|----------|
-- |    5    |    1     |  -- Manager 5 quản lý Store 1
-- |    5    |    2     |  -- Manager 5 quản lý Store 2
-- |    7    |    3     |  -- Manager 7 quản lý Store 3
```

---

## 6. DASHBOARD & THỐNG KÊ

### 6.1 Luồng Lấy Dashboard Summary
```
┌─────────────────────────────────────────────────────────────────────┐
│                  FLOW: GET DASHBOARD SUMMARY                        │
└─────────────────────────────────────────────────────────────────────┘

    ┌──────────────┐
    │ Manager App  │
    └──────┬───────┘
           │ GET /api/manager/summary
           ▼
    ┌──────────────────────────────────────────────────────────────┐
    │                    ManagerService                            │
    │                                                              │
    │  1. getCurrentManager() → Lấy current user                   │
    │  2. getManagedStoreIds(manager) → Lấy store IDs              │
    │                                                              │
    │  ┌─────────────────────────────────────────────────────────┐ │
    │  │ TRƯỜNG HỢP 1: ADMIN (storeIds = null)                   │ │
    │  │ - Đếm tất cả đơn hàng                                   │ │
    │  │ - Tính tổng doanh thu TẤT CẢ stores                     │ │
    │  │ - CỘNG THÊM doanh thu backup (user đã xóa)              │ │
    │  └─────────────────────────────────────────────────────────┘ │
    │                                                              │
    │  ┌─────────────────────────────────────────────────────────┐ │
    │  │ TRƯỜNG HỢP 2: MANAGER có stores (storeIds = [1,2])      │ │
    │  │ - Đếm đơn hàng WHERE store_id IN (1,2)                  │ │
    │  │ - Tính doanh thu CHỈ của stores được gán                │ │
    │  │ - KHÔNG cộng doanh thu backup                           │ │
    │  └─────────────────────────────────────────────────────────┘ │
    │                                                              │
    │  ┌─────────────────────────────────────────────────────────┐ │
    │  │ TRƯỜNG HỢP 3: MANAGER không có stores (storeIds = [])   │ │
    │  │ - Trả về empty dashboard                                │ │
    │  │ - totalRevenue = 0, totalOrders = 0                     │ │
    │  └─────────────────────────────────────────────────────────┘ │
    │                                                              │
    │  3. Query top selling drinks (theo stores)                   │
    │  4. Query top rated drinks                                   │
    │  5. Build DashboardSummaryDto và return                      │
    └──────────────────────────────────────────────────────────────┘
```

### 6.2 DashboardSummaryDto Structure
```java
public class DashboardSummaryDto {
    private BigDecimal totalRevenue;      // Tổng doanh thu
    private Long totalOrders;             // Tổng số đơn
    private Long pendingOrders;           // Đơn chờ xử lý
    private Long completedOrders;         // Đơn hoàn thành
    private Long canceledOrders;          // Đơn đã hủy
    private Boolean isAdmin;              // Có phải Admin không
    
    // Top sản phẩm bán chạy
    private List<TopSellingDrinkDto> topSellingDrinks;
    
    // Top sản phẩm được đánh giá cao
    private List<TopRatedDrinkDto> topRatedDrinks;
    
    // Danh sách stores được quản lý (cho Manager)
    private List<ManagedStoreInfo> managedStores;
}
```

---

## 7. DỰ BÁO & PHÂN TÍCH

### 7.1 Các Loại Dự Báo
```
┌─────────────────────────────────────────────────────────────────────┐
│                    HỆ THỐNG DỰ BÁO (ForecastService)                │
└─────────────────────────────────────────────────────────────────────┘

    ┌─────────────────────────────────────────────────────────────────┐
    │ 1. REVENUE FORECAST (Dự báo doanh thu)                          │
    │    - Dự báo hôm nay (điều chỉnh theo tiến độ)                   │
    │    - Dự báo ngày mai và 7 ngày tới                              │
    │    - Dự báo tháng (30 ngày)                                     │
    │    - Tỷ lệ tăng trưởng so với tuần trước                        │
    │    - Xu hướng: GROWTH / DECLINE / STABLE                        │
    │                                                                 │
    │    Công thức:                                                   │
    │    dailyAvg = sum(last7Days) / 7                                │
    │    growthRate = (lastWeek - prevWeek) / prevWeek                │
    │    forecastNextDay = dailyAvg × (1 + growthRate)                │
    └─────────────────────────────────────────────────────────────────┘

    ┌─────────────────────────────────────────────────────────────────┐
    │ 2. PEAK HOURS ANALYSIS (Phân tích giờ cao điểm)                 │
    │    - Thống kê đơn hàng theo từng giờ (8h-22h)                   │
    │    - Doanh thu trung bình theo giờ                              │
    │    - Mức độ cao điểm: LOW / MEDIUM / HIGH / VERY_HIGH           │
    │    - Đề xuất số nhân viên cho mỗi khung giờ                     │
    │                                                                 │
    │    Ví dụ output:                                                │
    │    | Giờ | Đơn TB | Doanh thu TB | Mức độ    | Nhân viên |      │
    │    |-----|--------|--------------|-----------|-----------|      │
    │    | 8h  |   5    |   200,000    | LOW       |     2     |      │
    │    | 12h |  25    | 1,000,000    | HIGH      |     5     |      │
    │    | 18h |  30    | 1,200,000    | VERY_HIGH |     6     |      │
    └─────────────────────────────────────────────────────────────────┘

    ┌─────────────────────────────────────────────────────────────────┐
    │ 3. LOW STOCK WARNINGS (Cảnh báo món sắp hết)                    │
    │    - Phát hiện món bán nhanh hơn bình thường                    │
    │    - Tốc độ bán (đơn/giờ) so với trung bình                     │
    │    - Mức cảnh báo: WARNING / CRITICAL                           │
    │    - Dự kiến hết hàng trong X giờ                               │
    └─────────────────────────────────────────────────────────────────┘

    ┌─────────────────────────────────────────────────────────────────┐
    │ 4. STAFFING RECOMMENDATIONS (Đề xuất nhân sự)                   │
    │    - Dự báo 7 ngày tới                                          │
    │    - Số nhân viên đề xuất cho mỗi ngày                          │
    │    - Chi tiết theo từng khung giờ                               │
    │    - Highlight ngày cuối tuần (lượng đơn cao)                   │
    └─────────────────────────────────────────────────────────────────┘

    ┌─────────────────────────────────────────────────────────────────┐
    │ 5. OVERLOAD WARNINGS (Cảnh báo quá tải)                         │
    │    - Phát hiện khung giờ vượt công suất                         │
    │    - % vượt công suất tối đa (50 đơn/giờ)                       │
    │    - Mức độ: WARNING / CRITICAL                                 │
    │    - Đề xuất xử lý: tăng nhân viên + nguyên liệu                │
    └─────────────────────────────────────────────────────────────────┘
```

### 7.2 API Endpoints Dự Báo
```
GET /api/manager/forecast              → Full forecast (tất cả)
GET /api/manager/forecast/revenue      → Dự báo doanh thu
GET /api/manager/forecast/peak-hours   → Phân tích giờ cao điểm
GET /api/manager/forecast/low-stock    → Cảnh báo món sắp hết
GET /api/manager/forecast/staffing     → Đề xuất nhân sự
GET /api/manager/forecast/overload     → Cảnh báo quá tải
```

---

## 8. WHITELIST IP - BẢO MẬT NÂNG CAO

### 8.1 Tổng Quan Whitelist IP
```
┌─────────────────────────────────────────────────────────────────────┐
│                    WHITELIST IP SYSTEM                              │
└─────────────────────────────────────────────────────────────────────┘

    ┌─────────────────────────────────────────────────────────────────┐
    │ MỤC ĐÍCH:                                                       │
    │ - Bảo vệ quyền truy cập Admin/Manager                           │
    │ - Chỉ cho phép IP trong whitelist truy cập với quyền cao        │
    │ - User thường KHÔNG bị ảnh hưởng                                │
    └─────────────────────────────────────────────────────────────────┘

    ┌─────────────────────────────────────────────────────────────────┐
    │ SO SÁNH BLOCKED IP vs WHITELIST IP:                             │
    │                                                                 │
    │ BLOCKED IP:                                                     │
    │ - Chặn IP không cho truy cập HỆ THỐNG                           │
    │ - Áp dụng cho TẤT CẢ người dùng                                 │
    │ - Blacklist approach                                            │
    │                                                                 │
    │ WHITELIST IP:                                                   │
    │ - Chỉ cho phép IP trong danh sách truy cập với QUYỀN CAO        │
    │ - CHỈ áp dụng cho ADMIN/MANAGER                                 │
    │ - User thường vẫn truy cập bình thường                          │
    │ - Whitelist approach                                            │
    └─────────────────────────────────────────────────────────────────┘
```

### 8.2 Luồng Kiểm Tra Whitelist IP
```
┌─────────────────────────────────────────────────────────────────────┐
│                  FLOW: WHITELIST IP FILTER                          │
└─────────────────────────────────────────────────────────────────────┘

    ┌──────────────┐
    │   Request    │
    │ (Manager/    │
    │  Admin)      │
    └──────┬───────┘
           │
           ▼
    ┌──────────────────────────────────────────────────────────────┐
    │                  WhitelistIPFilter                           │
    │                                                              │
    │  1. Kiểm tra whitelist.check.enabled                         │
    │     ┌─────────────────────────────────────────────────────┐  │
    │     │ if (!whitelistCheckEnabled) {                       │  │
    │     │   filterChain.doFilter(request, response);          │  │
    │     │   return; // Bỏ qua kiểm tra                         │  │
    │     │ }                                                    │  │
    │     └─────────────────────────────────────────────────────┘  │
    │                                                              │
    │  2. Lấy Authentication từ SecurityContext                    │
    │                                                              │
    │  3. Kiểm tra có phải Admin/Manager không                     │
    │     ┌─────────────────────────────────────────────────────┐  │
    │     │ if (!isAdminOrManager(authentication)) {            │  │
    │     │   filterChain.doFilter(request, response);          │  │
    │     │   return; // User thường - cho qua                   │  │
    │     │ }                                                    │  │
    │     └─────────────────────────────────────────────────────┘  │
    │                                                              │
    │  4. Lấy IP của client                                        │
    │     - Kiểm tra headers: X-Forwarded-For, X-Real-IP...        │
    │     - Normalize IP (::1 → 127.0.0.1)                         │
    │                                                              │
    │  5. Kiểm tra IP có trong whitelist không                     │
    │     ┌─────────────────────────────────────────────────────┐  │
    │     │ if (!whitelistedIPService.isIPWhitelisted(ip)) {    │  │
    │     │   response.setStatus(403);                          │  │
    │     │   response.write({                                  │  │
    │     │     "error": "IP_NOT_WHITELISTED",                  │  │
    │     │     "message": "IP chưa được cấp quyền...",         │  │
    │     │     "yourIP": clientIP                              │  │
    │     │   });                                                │  │
    │     │   return; // CHẶN                                    │  │
    │     │ }                                                    │  │
    │     └─────────────────────────────────────────────────────┘  │
    │                                                              │
    │  6. IP hợp lệ → cho qua                                      │
    │     filterChain.doFilter(request, response);                 │
    └──────────────────────────────────────────────────────────────┘
```

### 8.3 Luồng Thêm IP Vào Whitelist
```
┌─────────────────────────────────────────────────────────────────────┐
│                  FLOW: THÊM IP VÀO WHITELIST                        │
└─────────────────────────────────────────────────────────────────────┘

    ┌──────────────┐
    │  Admin App   │
    └──────┬───────┘
           │ POST /api/whitelist-ips/add
           │ Body: { "ipAddress": "123.21.109.117", 
           │         "description": "IP văn phòng" }
           ▼
    ┌──────────────────────────────────────────────────────────────┐
    │                WhitelistedIPController                       │
    │  @PreAuthorize("hasRole('ADMIN')")  ← CHỈ ADMIN              │
    └──────────┬───────────────────────────────────────────────────┘
               ▼
    ┌──────────────────────────────────────────────────────────────┐
    │                 WhitelistedIPService                         │
    │                                                              │
    │  1. Validate IP không rỗng                                   │
    │                                                              │
    │  2. Normalize IP (::1 → 127.0.0.1)                           │
    │                                                              │
    │  3. Kiểm tra đã tồn tại chưa                                 │
    │     ┌─────────────────────────────────────────────────────┐  │
    │     │ if (existsByIpAddress(normalizedIP)) {              │  │
    │     │   // Nếu đã tồn tại và active → throw error         │  │
    │     │   // Nếu đã tồn tại nhưng inactive → kích hoạt lại  │  │
    │     │ }                                                    │  │
    │     └─────────────────────────────────────────────────────┘  │
    │                                                              │
    │  4. Tạo WhitelistedIP entity                                 │
    │     WhitelistedIP.builder()                                  │
    │       .ipAddress(normalizedIP)                               │
    │       .description(description)                              │
    │       .addedById(currentUserId)                              │
    │       .isActive(true)                                        │
    │       .build();                                              │
    │                                                              │
    │  5. whitelistedIPRepository.save(whitelistedIP)              │
    └──────────────────────────────────────────────────────────────┘
```

### 8.4 Database Schema - whitelisted_ips
```sql
CREATE TABLE whitelisted_ips (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  ip_address VARCHAR(50) NOT NULL,        -- Địa chỉ IP
  description VARCHAR(500),               -- Mô tả (VD: "IP văn phòng")
  added_by_id BIGINT,                     -- ID Admin đã thêm
  is_active BOOLEAN DEFAULT TRUE,         -- Trạng thái active
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  FOREIGN KEY (added_by_id) REFERENCES users(id),
  INDEX idx_ip_address (ip_address),
  INDEX idx_is_active (is_active)
);

-- Ví dụ dữ liệu:
-- | id | ip_address      | description        | added_by_id | is_active |
-- |----|-----------------|--------------------| ------------|-----------|
-- | 1  | 127.0.0.1       | Localhost - Dev    | NULL        | true      |
-- | 2  | 123.21.109.117  | IP văn phòng chính | 1           | true      |
-- | 3  | 192.168.1.100   | IP nhà quản lý     | 1           | true      |
```

### 8.5 Cấu Hình Whitelist
```properties
# application.properties

# ============================================
# WHITELIST IP FOR ADMIN/MANAGER (DATABASE)
# ============================================
# Bật/tắt kiểm tra whitelist IP cho Admin/Manager
# Khi bật: Admin/Manager chỉ truy cập được từ IP trong whitelist (database)
# User thường KHÔNG bị ảnh hưởng
whitelist.check.enabled=${WHITELIST_CHECK_ENABLED:true}
```

### 8.6 API Endpoints Whitelist IP
```
POST /api/whitelist-ips/add              → Thêm IP vào whitelist (ADMIN)
POST /api/whitelist-ips/{id}/remove      → Xóa IP khỏi whitelist (ADMIN)
GET  /api/whitelist-ips                  → Lấy danh sách IP active (ADMIN)
GET  /api/whitelist-ips/all              → Lấy tất cả IP (có phân trang)
GET  /api/whitelist-ips/check?ip=...     → Kiểm tra IP có trong whitelist
```

---

## 9. LUỒNG LOGIC CHI TIẾT

### 9.1 Sequence Diagram - Cập Nhật Trạng Thái Đơn Hàng
```
┌─────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────┐
│ Manager │     │ Controller   │     │   Service    │     │ Repository   │     │ Database │
│   App   │     │              │     │              │     │              │     │          │
└────┬────┘     └──────┬───────┘     └──────┬───────┘     └──────┬───────┘     └────┬─────┘
     │                 │                    │                    │                  │
     │ PUT /orders/1/  │                    │                    │                  │
     │ status?status=  │                    │                    │                  │
     │ MAKING          │                    │                    │                  │
     │────────────────▶│                    │                    │                  │
     │                 │                    │                    │                  │
     │                 │ updateOrderStatus  │                    │                  │
     │                 │ (1, MAKING)        │                    │                  │
     │                 │───────────────────▶│                    │                  │
     │                 │                    │                    │                  │
     │                 │                    │ getCurrentManager()│                  │
     │                 │                    │───────────────────▶│                  │
     │                 │                    │                    │ SELECT * FROM    │
     │                 │                    │                    │ users WHERE...   │
     │                 │                    │                    │─────────────────▶│
     │                 │                    │                    │◀─────────────────│
     │                 │                    │◀───────────────────│                  │
     │                 │                    │                    │                  │
     │                 │                    │ getManagedStoreIds │                  │
     │                 │                    │ (manager)          │                  │
     │                 │                    │───────────────────▶│                  │
     │                 │                    │◀───────────────────│ [1, 2]           │
     │                 │                    │                    │                  │
     │                 │                    │ findById(1)        │                  │
     │                 │                    │───────────────────▶│                  │
     │                 │                    │                    │ SELECT * FROM    │
     │                 │                    │                    │ orders WHERE...  │
     │                 │                    │                    │─────────────────▶│
     │                 │                    │                    │◀─────────────────│
     │                 │                    │◀───────────────────│ Order            │
     │                 │                    │                    │                  │
     │                 │                    │ validateStoreAccess│                  │
     │                 │                    │ (manager, storeId) │                  │
     │                 │                    │ ✅ OK              │                  │
     │                 │                    │                    │                  │
     │                 │                    │ order.setStatus    │                  │
     │                 │                    │ (MAKING)           │                  │
     │                 │                    │                    │                  │
     │                 │                    │ save(order)        │                  │
     │                 │                    │───────────────────▶│                  │
     │                 │                    │                    │ UPDATE orders    │
     │                 │                    │                    │ SET status=...   │
     │                 │                    │                    │─────────────────▶│
     │                 │                    │                    │◀─────────────────│
     │                 │                    │◀───────────────────│                  │
     │                 │                    │                    │                  │
     │                 │                    │ notifyOrderStatus  │                  │
     │                 │                    │ (WebSocket + Push) │                  │
     │                 │                    │                    │                  │
     │                 │◀───────────────────│ OrderDto           │                  │
     │                 │                    │                    │                  │
     │◀────────────────│ Response           │                    │                  │
     │                 │ {success: true,    │                    │                  │
     │                 │  data: OrderDto}   │                    │                  │
     │                 │                    │                    │                  │
```

---

## 11. GIẢI THÍCH CODE

### 11.1 ManagerService - Các Method Quan Trọng

#### getCurrentManager() - Lấy Manager Hiện Tại
```java
/**
 * Lấy current user từ Security Context
 * SecurityContext chứa thông tin user đã authenticate qua JWT
 */
private User getCurrentManager() {
    // Lấy username từ JWT token đã được parse bởi JwtAuthenticationFilter
    String username = SecurityContextHolder.getContext()
        .getAuthentication().getName();
    
    // Query user với managed stores (JOIN FETCH để tránh N+1)
    return userRepository.findByUsernameWithManagedStores(username)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Manager not found: " + username));
}
```

#### getManagedStoreIds() - Lấy Danh Sách Store IDs
```java
/**
 * Lấy danh sách store IDs mà manager được phép quản lý
 * 
 * @return null nếu ADMIN (quản lý tất cả)
 * @return empty list nếu MANAGER chưa được gán store
 * @return list store IDs nếu MANAGER có stores
 */
private List<Long> getManagedStoreIds(User manager) {
    // ADMIN luôn quản lý tất cả - return null để query không filter
    if (manager.getRole() == UserRole.ADMIN) {
        return null;
    }
    
    // MANAGER phải có stores được gán
    Set<Store> managedStores = manager.getManagedStores();
    if (managedStores == null || managedStores.isEmpty()) {
        log.warn("Manager {} has no assigned stores", manager.getUsername());
        return new ArrayList<>(); // Empty list - không xem được gì
    }
    
    // Extract store IDs
    return managedStores.stream()
        .map(Store::getId)
        .collect(Collectors.toList());
}
```

#### validateStoreAccess() - Kiểm Tra Quyền Truy Cập Store
```java
/**
 * Kiểm tra manager có quyền truy cập store không
 * ADMIN bỏ qua kiểm tra
 * MANAGER phải có store trong danh sách được gán
 */
private void validateStoreAccess(User manager, Long storeId) {
    // ADMIN có quyền truy cập tất cả
    if (manager.getRole() == UserRole.ADMIN) {
        return;
    }
    
    // MANAGER phải có store trong danh sách
    if (!manager.canManageStore(storeId)) {
        log.error("Manager {} cannot access store {}. Managed stores: {}", 
            manager.getUsername(), storeId, 
            manager.getManagedStores().stream()
                .map(Store::getId)
                .collect(Collectors.toList()));
        
        throw new BusinessException(
            "Bạn không có quyền quản lý đơn hàng của chi nhánh này. " +
            "Vui lòng liên hệ Admin để được gán chi nhánh.");
    }
}
```

### 11.2 WhitelistIPFilter - Giải Thích Chi Tiết

```java
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10) // Chạy SAU JwtAuthFilter
@Slf4j
public class WhitelistIPFilter extends OncePerRequestFilter {

    private final WhitelistedIPService whitelistedIPService;

    // Đọc từ application.properties
    @Value("${whitelist.check.enabled:false}")
    private boolean whitelistCheckEnabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) 
            throws ServletException, IOException {

        // BƯỚC 1: Kiểm tra tính năng có bật không
        if (!whitelistCheckEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        // BƯỚC 2: Lấy Authentication từ SecurityContext
        // (đã được set bởi JwtAuthenticationFilter)
        Authentication authentication = SecurityContextHolder
            .getContext().getAuthentication();

        // BƯỚC 3: Nếu chưa authenticate hoặc không phải Admin/Manager
        // → cho qua (User thoải mái)
        if (authentication == null || !isAdminOrManager(authentication)) {
            filterChain.doFilter(request, response);
            return;
        }

        // BƯỚC 4: Lấy IP của client
        String clientIP = getClientIP(request);
        String normalizedIP = normalizeIP(clientIP);

        // BƯỚC 5: Kiểm tra IP có trong whitelist không
        if (!whitelistedIPService.isIPWhitelisted(normalizedIP)) {
            log.warn("🚫 WHITELIST CHECK FAILED | User: {} | IP: {}",
                    authentication.getName(), clientIP);

            // Trả về 403 Forbidden
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                "{\"success\":false," +
                "\"error\":\"IP_NOT_WHITELISTED\"," +
                "\"message\":\"IP của bạn chưa được cấp quyền...\"," +
                "\"yourIP\":\"" + clientIP + "\"}"
            );
            return; // CHẶN - không cho đi tiếp
        }

        // BƯỚC 6: IP hợp lệ → cho qua
        log.info("✅ WHITELIST CHECK PASSED | User: {} | IP: {}",
                authentication.getName(), clientIP);
        filterChain.doFilter(request, response);
    }

    /**
     * Kiểm tra user có phải Admin hoặc Manager không
     */
    private boolean isAdminOrManager(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(role -> 
                role.equals("ROLE_ADMIN") || role.equals("ROLE_MANAGER"));
    }

    /**
     * Lấy IP thực của client (xử lý proxy/load balancer)
     */
    private String getClientIP(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",  // Proxy header phổ biến
            "X-Real-IP",        // Nginx
            "Proxy-Client-IP",  // Apache
            "WL-Proxy-Client-IP"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim(); // Lấy IP đầu tiên
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * Normalize IP address (IPv6 localhost → IPv4)
     */
    private String normalizeIP(String ip) {
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }

    /**
     * Các endpoint không cần kiểm tra whitelist
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/swagger") ||
               path.startsWith("/api/auth/") ||  // Login, register...
               path.equals("/actuator/health");
    }
}
```

---

## 12. API REFERENCE

### 12.1 Manager APIs

#### Dashboard & Statistics
| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| GET | `/api/manager/summary` | Dashboard tổng quan | MANAGER, ADMIN |
| GET | `/api/manager/statistics/revenue` | Thống kê doanh thu | MANAGER, ADMIN |

#### Order Management
| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| GET | `/api/manager/orders` | Danh sách đơn hàng | MANAGER, ADMIN |
| PUT | `/api/manager/orders/{id}/status` | Cập nhật trạng thái | MANAGER, ADMIN |

#### User Management
| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| GET | `/api/manager/users` | Danh sách người dùng | MANAGER, ADMIN |
| GET | `/api/manager/users/{id}` | Chi tiết người dùng | MANAGER, ADMIN |
| PUT | `/api/manager/users/{id}/block` | Khóa/Mở khóa | MANAGER, ADMIN |
| PUT | `/api/manager/users/{id}/promote` | Nâng cấp → Manager | ADMIN |
| PUT | `/api/manager/users/{id}/demote` | Hạ cấp → User | ADMIN |
| DELETE | `/api/manager/users/{id}` | Xóa người dùng | MANAGER, ADMIN |

#### Store Assignment
| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| GET | `/api/manager/my-stores` | Stores của tôi | MANAGER, ADMIN |
| GET | `/api/manager/users/{id}/stores` | Stores của Manager | MANAGER, ADMIN |
| POST | `/api/manager/users/{id}/stores/{storeId}` | Gán store | ADMIN |
| PUT | `/api/manager/users/{id}/stores` | Gán nhiều stores | ADMIN |
| DELETE | `/api/manager/users/{id}/stores/{storeId}` | Bỏ gán store | ADMIN |

#### Forecast & Analytics
| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| GET | `/api/manager/forecast` | Full forecast | MANAGER, ADMIN |
| GET | `/api/manager/forecast/revenue` | Dự báo doanh thu | MANAGER, ADMIN |
| GET | `/api/manager/forecast/peak-hours` | Giờ cao điểm | MANAGER, ADMIN |
| GET | `/api/manager/forecast/low-stock` | Cảnh báo hết hàng | MANAGER, ADMIN |
| GET | `/api/manager/forecast/staffing` | Đề xuất nhân sự | MANAGER, ADMIN |
| GET | `/api/manager/forecast/overload` | Cảnh báo quá tải | MANAGER, ADMIN |

### 12.2 Whitelist IP APIs

| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| POST | `/api/whitelist-ips/add` | Thêm IP vào whitelist | ADMIN |
| POST | `/api/whitelist-ips/{id}/remove` | Xóa IP khỏi whitelist | ADMIN |
| GET | `/api/whitelist-ips` | Danh sách IP active | ADMIN |
| GET | `/api/whitelist-ips/all` | Tất cả IP (phân trang) | ADMIN |
| GET | `/api/whitelist-ips/check?ip=...` | Kiểm tra IP | ADMIN |

### 12.3 Monitoring APIs

#### Dashboard & Overview
| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| GET | `/api/monitoring/dashboard` | Dashboard tổng quan giám sát | ADMIN |

#### Activity Logs
| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| GET | `/api/monitoring/activities` | Danh sách log hoạt động | ADMIN |
| GET | `/api/monitoring/activities/user/{userId}` | Log của user cụ thể | ADMIN |

#### Alerts Management
| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| GET | `/api/monitoring/alerts` | Danh sách cảnh báo | ADMIN |
| GET | `/api/monitoring/alerts/pending` | Cảnh báo chờ xử lý | ADMIN |
| PUT | `/api/monitoring/alerts/{id}/handle` | Xử lý cảnh báo | ADMIN |

#### Risk Scores
| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| GET | `/api/monitoring/risk-scores` | Danh sách điểm rủi ro | ADMIN |
| GET | `/api/monitoring/risk-scores/user/{userId}` | Điểm rủi ro của user | ADMIN |
| POST | `/api/monitoring/risk-scores/user/{userId}/note` | Thêm ghi chú admin | ADMIN |
| POST | `/api/monitoring/risk-scores/user/{userId}/reset` | Reset điểm rủi ro | ADMIN |

#### User Actions
| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| POST | `/api/monitoring/users/{userId}/unblock` | Mở khóa user | ADMIN |

### 12.4 Request/Response Examples

#### Thêm IP vào Whitelist
```http
POST /api/whitelist-ips/add
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "ipAddress": "123.21.109.117",
  "description": "IP văn phòng chính"
}
```

Response:
```json
{
  "success": true,
  "message": "IP đã được thêm vào whitelist",
  "data": {
    "id": 2,
    "ipAddress": "123.21.109.117",
    "description": "IP văn phòng chính",
    "addedById": 1,
    "isActive": true,
    "createdAt": "2024-01-15T10:00:00"
  }
}
```

#### Cập Nhật Trạng Thái Đơn Hàng
```http
PUT /api/manager/orders/123/status?status=MAKING
Authorization: Bearer <JWT_TOKEN>
```

Response:
```json
{
  "success": true,
  "message": "Order status updated",
  "data": {
    "id": 123,
    "status": "MAKING",
    "userId": 5,
    "storeId": 1,
    "totalPrice": 150000,
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

#### Lấy Dashboard Monitoring
```http
GET /api/monitoring/dashboard
Authorization: Bearer <JWT_TOKEN>
```

Response:
```json
{
  "success": true,
  "data": {
    "totalPendingAlerts": 15,
    "criticalAlerts": 3,
    "highAlerts": 5,
    "mediumAlerts": 4,
    "lowAlerts": 3,
    "normalUsers": 1200,
    "warningUsers": 45,
    "suspiciousUsers": 12,
    "criticalUsers": 3,
    "activityStats": {
      "LOGIN_SUCCESS": 450,
      "LOGIN_FAILED": 23,
      "ORDER_CREATE": 89,
      "PAYMENT_SUCCESS": 76,
      "BRUTE_FORCE_ATTEMPT": 2
    },
    "alertTypeStats": {
      "LOGIN_ANOMALY": 5,
      "BRUTE_FORCE": 2,
      "HIGH_RISK_SCORE": 8
    }
  }
}
```

#### Xử Lý Cảnh Báo
```http
PUT /api/monitoring/alerts/15/handle
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "status": "RESOLVED",
  "actionTaken": "TEMP_BLOCKED",
  "note": "Đã khóa tài khoản tạm thời 24h do brute force"
}
```

Response:
```json
{
  "success": true,
  "message": "Alert handled",
  "data": {
    "id": 15,
    "alertType": "BRUTE_FORCE",
    "severity": "CRITICAL",
    "status": "RESOLVED",
    "actionTaken": "TEMP_BLOCKED",
    "handlerNote": "Đã khóa tài khoản tạm thời 24h do brute force",
    "handledAt": "2024-01-15T11:00:00"
  }
}
```

#### Lấy Risk Score của User
```http
GET /api/monitoring/risk-scores/user/123
Authorization: Bearer <JWT_TOKEN>
```

Response:
```json
{
  "success": true,
  "data": {
    "userId": 123,
    "username": "user123",
    "totalScore": 65,
    "riskLevel": "SUSPICIOUS",
    "loginFailedCount": 3,
    "orderCancelCount": 2,
    "paymentFailedCount": 1,
    "rateLimitHitCount": 0,
    "promotionAbuseCount": 0,
    "spamRequestCount": 0,
    "autoBlocked": false,
    "adminNote": "User có hành vi đáng ngờ, cần theo dõi",
    "lastActivity": "2024-01-15T10:45:00"
  }
}
```

---

## 13. CÂU HỎI PHỎNG VẤN

### 13.1 Câu Hỏi Cơ Bản

**Q1: Mô tả flow khi Manager cập nhật trạng thái đơn từ PENDING → MAKING?**
```
A: Controller nhận request → validate JWT + role → Service lấy current manager 
→ lấy danh sách store IDs được gán → query order → validate quyền truy cập store 
→ validate transition hợp lệ → update status → save → gửi notification 
(WebSocket + Push + Email) → return response
```

**Q2: Làm sao tránh 2 manager cập nhật trùng 1 order (race condition)?**
```
A: Sử dụng optimistic locking với @Version field trong entity Order.
Khi update, JPA sẽ check version. Nếu version không khớp (đã bị update bởi 
người khác), throw OptimisticLockException → trả về 409 Conflict.
```

**Q3: Whitelist IP khác gì với Blocked IP?**
```
A: 
- Blocked IP: Chặn IP không cho truy cập HỆ THỐNG (blacklist approach)
- Whitelist IP: Chỉ cho phép IP trong danh sách truy cập với QUYỀN CAO 
  (Admin/Manager). User thường không bị ảnh hưởng (whitelist approach)
```

### 13.2 Câu Hỏi Nâng Cao

**Q4: Tại sao WhitelistIPFilter phải chạy SAU JwtAuthenticationFilter?**
```
A: Vì WhitelistIPFilter cần biết user đã authenticate là ai và có role gì 
để quyết định có cần kiểm tra whitelist không. JwtAuthenticationFilter 
parse JWT token và set Authentication vào SecurityContext. Nếu chạy trước, 
SecurityContext sẽ rỗng và không thể kiểm tra role.
```

**Q5: Làm sao Manager chỉ xem được đơn hàng của store được gán?**
```
A: 
1. Khi query, lấy danh sách store IDs từ manager.getManagedStores()
2. Thêm điều kiện WHERE store_id IN (:storeIds) vào query
3. ADMIN return null → không filter (xem tất cả)
4. MANAGER không có store → return empty list → không xem được gì
```

**Q6: Giải thích cách tính doanh thu trong Dashboard?**
```
A:
- ADMIN: Tổng doanh thu TẤT CẢ stores + doanh thu backup (user đã xóa)
- MANAGER: Chỉ tính doanh thu của stores được gán, KHÔNG cộng backup

Query:
SELECT COALESCE(SUM(o.finalPrice), 0) 
FROM Order o 
WHERE o.status = 'DONE' 
  AND o.store.id IN :storeIds  -- Chỉ cho MANAGER
```

**Q7: Giải thích hệ thống Risk Scoring trong Monitoring?**
```
A: Hệ thống tính điểm rủi ro dựa trên hành vi:
- Mỗi hành vi bất thường cộng điểm (login fail +5, brute force +40...)
- Phân loại: NORMAL (0-29) → WARNING (30-59) → SUSPICIOUS (60-79) → CRITICAL (80+)
- Tự động khóa tài khoản khi >= 80 điểm
- Gửi cảnh báo realtime cho Admin qua WebSocket + Push notification
```

**Q8: Làm sao phát hiện Brute Force Attack?**
```
A: 
1. Đếm số lần login failed trong 15 phút
2. Nếu >= 5 lần → log BRUTE_FORCE_ATTEMPT với CRITICAL level
3. Cộng 40 điểm vào Risk Score
4. Tạo Alert với severity CRITICAL
5. Nếu tổng điểm >= 80 → tự động khóa tài khoản
6. Gửi notification cho Admin
```

### 13.3 Câu Hỏi Về Monitoring

**Q9: Monitoring System ghi log những hoạt động nào?**
```
A: Ghi log tất cả hoạt động quan trọng:
- Authentication: login success/failed, logout, password change
- Order: create, cancel, status update
- Payment: success/failed
- Cart: add/remove items
- Profile: update info, avatar change
- Security: rate limit hit, spam request, brute force
- Group Order: create, join, leave
- Live Chat: start conversation, send message
```

**Q10: Khi nào hệ thống tự động khóa tài khoản?**
```
A: Tự động khóa khi Risk Score >= 80 điểm:
1. Tính tổng điểm từ các hành vi bất thường
2. Nếu >= 80 → user.setIsBlocked(true)
3. Tạo Alert AUTO_BLOCKED với severity CRITICAL
4. Gửi WebSocket notification đặc biệt
5. Gửi push notification cho Admin
6. Log activity ACCOUNT_BLOCKED
```

**Q11: Làm sao Admin xử lý một Alert?**
```
A: Flow xử lý Alert:
1. Admin xem Alert trong dashboard
2. PUT /api/monitoring/alerts/{id}/handle
3. Chọn status (RESOLVED/DISMISSED) và action (TEMP_BLOCKED/WARNING_SENT...)
4. Thêm handler note giải thích
5. Nếu action = TEMP_BLOCKED → tự động khóa user
6. Update alert.handledBy = currentAdmin
7. Gửi notification cho user (nếu cần)
```

### 13.4 Quick Checklist Trước Phỏng Vấn

✅ Nắm 3 endpoints chính: GET orders, PUT order status, GET dashboard

✅ Mô tả sequence flow khi update trạng thái (controller → service → repo → event → ws → push)

✅ Nêu 3 security checks: role check, store scope, whitelist IP

✅ Giải thích optimistic locking vs SELECT FOR UPDATE

✅ Phân biệt Blocked IP vs Whitelist IP

✅ Hiểu cách phân quyền ADMIN vs MANAGER

✅ Biết cách gán store cho Manager

✅ Hiểu hệ thống Risk Scoring và Auto-block

✅ Biết cách phát hiện Brute Force Attack

✅ Hiểu các loại Activity Type và Alert Type

✅ Biết cách xử lý Alert và unblock user

---

## 📚 TÀI LIỆU THAM KHẢO

- `UTE_TEA_API_DOCUMENTATION.md` - API chi tiết
- `UTE_TEA_DATABASE_SCHEMA.md` - Cấu trúc database
- `UTE_TEA_FEATURES_DOCUMENTATION.md` - Danh sách chức năng
- `Manager_Admin_CheatSheet.md` - Cheat sheet nhanh

---

*Tài liệu được tạo để học tập và ôn thi. Cập nhật: 2024*


---

## 13. QUẢN LÝ SẢN PHẨM (DRINKS & CATEGORIES)

### 13.1 Tổng Quan Quản Lý Sản Phẩm
```
┌─────────────────────────────────────────────────────────────────────┐
│                    QUẢN LÝ SẢN PHẨM                                 │
└─────────────────────────────────────────────────────────────────────┘

    ┌─────────────────────────────────────────────────────────────────┐
    │ DRINK (Món nước)                                                │
    │ ├── id, name, description, imageUrl                             │
    │ ├── basePrice (giá gốc)                                         │
    │ ├── categoryId (danh mục)                                       │
    │ ├── isActive (còn bán/ngừng bán)                                │
    │ │                                                               │
    │ ├── SIZES (Kích cỡ)                                             │
    │ │   ├── M: extraPrice = 0                                       │
    │ │   ├── L: extraPrice = 5,000                                   │
    │ │   └── Jumbo: extraPrice = 10,000                              │
    │ │                                                               │
    │ └── TOPPINGS (Topping)                                          │
    │     ├── Trân châu đen: 10,000                                   │
    │     ├── Thạch dừa: 8,000                                        │
    │     └── Pudding: 12,000                                         │
    └─────────────────────────────────────────────────────────────────┘

    ┌─────────────────────────────────────────────────────────────────┐
    │ CATEGORY (Danh mục)                                             │
    │ ├── id, name, description, imageUrl                             │
    │ ├── displayOrder (thứ tự hiển thị)                              │
    │ └── isActive                                                    │
    │                                                                 │
    │ Ví dụ: Trà sữa, Cà phê, Trà trái cây, Đá xay...                 │
    └─────────────────────────────────────────────────────────────────┘
```

### 13.2 Luồng Tạo/Cập Nhật Sản Phẩm
```
┌─────────────────────────────────────────────────────────────────────┐
│                  FLOW: TẠO SẢN PHẨM MỚI                             │
└─────────────────────────────────────────────────────────────────────┘

    ┌──────────────┐
    │ Manager App  │
    └──────┬───────┘
           │ POST /api/admin/drinks
           │ Body: { name, description, basePrice, categoryId, imageUrl }
           ▼
    ┌──────────────────────────────────────────────────────────────┐
    │                 AdminDrinkController                         │
    │  @PreAuthorize("hasRole('ADMIN')")                           │
    └──────────┬───────────────────────────────────────────────────┘
               ▼
    ┌──────────────────────────────────────────────────────────────┐
    │                     DrinkService                             │
    │                                                              │
    │  1. Tạo Drink entity mới                                     │
    │     drink.setName(dto.getName())                             │
    │     drink.setDescription(dto.getDescription())               │
    │     drink.setBasePrice(dto.getBasePrice())                   │
    │     drink.setIsActive(true)                                  │
    │                                                              │
    │  2. drinkRepository.save(drink)                              │
    │                                                              │
    │  3. @CacheEvict(value = DRINKS_CACHE, allEntries = true)     │
    │     → Xóa cache để cập nhật danh sách mới                    │
    │                                                              │
    │  4. Return DrinkDto                                          │
    └──────────────────────────────────────────────────────────────┘
```

### 13.3 Giải Thích Code DrinkService
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class DrinkService {
    
    private final DrinkRepository drinkRepository;
    private final DrinkSizeRepository drinkSizeRepository;
    private final DrinkToppingRepository drinkToppingRepository;
    
    /**
     * FIX N+1 QUERY: Load tất cả drinks với sizes trong 1 query,
     * sau đó batch load toppings
     */
    @Transactional(readOnly = true)
    @Cacheable(value = DRINKS_CACHE, key = "'all-active'")
    public List<DrinkDto> getAllActiveDrinks() {
        // Query 1: Load drinks với sizes và category (JOIN FETCH)
        List<Drink> drinks = drinkRepository
            .findByIsActiveTrueWithSizesAndCategory();
        
        if (drinks.isEmpty()) {
            return List.of();
        }
        
        // Query 2: Batch load toppings cho tất cả drinks
        List<Long> drinkIds = drinks.stream()
            .map(Drink::getId)
            .collect(Collectors.toList());
        Map<Long, List<DrinkTopping>> toppingsMap = 
            loadToppingsForDrinks(drinkIds);
        
        // Query 3: Load global toppings (drink_id = NULL)
        List<DrinkTopping> globalToppings = drinkToppingRepository
            .findByDrinkIdIsNullAndIsActiveTrue();
        
        return drinks.stream()
            .map(drink -> mapToDtoOptimized(drink, 
                toppingsMap.get(drink.getId()), globalToppings))
            .collect(Collectors.toList());
    }
    
    /**
     * Tìm kiếm sản phẩm với input sanitization
     */
    @Transactional(readOnly = true)
    public List<DrinkDto> searchDrinks(String keyword) {
        // Input sanitization: validate and clean keyword
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        
        // Sanitize: remove special characters, keep Vietnamese
        String sanitized = keyword.replaceAll(
            "[^a-zA-Z0-9\\sàáạảã...đĐ]", "");
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }
        
        List<Drink> drinks = drinkRepository
            .searchByNameWithSizesAndCategory(sanitized);
        // ... batch load toppings và map to DTO
    }
    
    /**
     * Xóa sản phẩm (soft delete)
     */
    @Transactional
    @CacheEvict(value = DRINKS_CACHE, allEntries = true)
    public void deleteDrink(Long id) {
        Drink drink = drinkRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Drink", "id", id));
        drink.setIsActive(false);  // Soft delete - không xóa thật
        drinkRepository.save(drink);
    }
}
```

### 13.4 API Endpoints Quản Lý Sản Phẩm
| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| GET | `/api/drinks` | Danh sách sản phẩm | PUBLIC |
| GET | `/api/drinks/{id}` | Chi tiết sản phẩm | PUBLIC |
| GET | `/api/drinks/search?keyword=...` | Tìm kiếm | PUBLIC |
| POST | `/api/admin/drinks` | Tạo sản phẩm mới | ADMIN |
| PUT | `/api/admin/drinks/{id}` | Cập nhật sản phẩm | ADMIN |
| DELETE | `/api/admin/drinks/{id}` | Xóa sản phẩm | ADMIN |
| GET | `/api/categories` | Danh sách danh mục | PUBLIC |
| POST | `/api/manager/categories` | Tạo danh mục | MANAGER, ADMIN |
| PUT | `/api/manager/categories/{id}` | Cập nhật danh mục | MANAGER, ADMIN |
| DELETE | `/api/manager/categories/{id}` | Xóa danh mục | MANAGER, ADMIN |
### 1.2 Phân Quyền Chi Tiết

#### 🔐 ADMIN Permissions
- **Quản lý hệ thống**: Tạo/sửa/xóa Manager, Store, Category
- **Bảo mật nâng cao**: Whitelist IP, Blocked IP, Risk Management
- **Giám sát toàn diện**: User Monitoring, Activity Logs, Alerts
- **Báo cáo tổng hợp**: Cross-store analytics, Revenue forecasting
- **Cấu hình hệ thống**: Payment methods, Shipping, Notifications

#### 👨‍💼 MANAGER Permissions  
- **Quản lý đơn hàng**: Xem, cập nhật trạng thái, xử lý hoàn tiền
- **Quản lý sản phẩm**: CRUD drinks, toppings, categories trong store
- **Live Chat**: Tư vấn khách hàng, hỗ trợ đặt hàng realtime
- **Voucher Management**: Tạo mã giảm giá, theo dõi usage
- **Analytics**: Dashboard store, doanh thu, top products
- **Staff Management**: Quản lý nhân viên trong cửa hàng

### 1.3 Luồng Hoạt Động Chính

```
┌─────────────────────────────────────────────────────────────┐
│                    MANAGER WORKFLOW                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  🌅 BUỔI SÁNG:                                             │
│  ├── 📊 Kiểm tra Dashboard - Doanh thu hôm qua            │
│  ├── 📦 Xem đơn hàng mới - Cập nhật trạng thái            │
│  ├── 💬 Đọc tin nhắn Live Chat - Phản hồi khách hàng      │
│  ├── 🎫 Kiểm tra voucher - Tạo khuyến mãi mới             │
│  └── 🍵 Cập nhật menu - Thêm/sửa sản phẩm                 │
│                                                             │
│  🌞 TRONG NGÀY:                                            │
│  ├── 📱 Nhận notification đơn hàng mới                     │
│  ├── 💬 Chat realtime với khách hàng                       │
│  ├── 📦 Xử lý đơn hàng: PENDING → CONFIRMED → DELIVERED   │
│  ├── 🛡️ Theo dõi alerts từ hệ thống monitoring            │
│  └── 📈 Xem realtime analytics                            │
│                                                             │
│  🌙 BUỔI TỐI:                                              │
│  ├── 📊 Xem báo cáo doanh thu ngày                         │
│  ├── 📈 Phân tích xu hướng bán hàng                        │
│  ├── 🎯 Lên kế hoạch khuyến mãi ngày mai                   │
│  ├── 👥 Đánh giá hiệu suất nhân viên                       │
│  └── 💡 Dự báo nhu cầu sản phẩm                            │
└─────────────────────────────────────────────────────────────┘
```### 1
.4 Tính Năng Nổi Bật

#### 🚀 Real-time Features
- **Live Chat**: WebSocket-based chat với khách hàng
- **Order Notifications**: Push notifications cho đơn hàng mới
- **Dashboard Updates**: Cập nhật metrics realtime
- **Monitoring Alerts**: Cảnh báo bảo mật tức thời

#### 🤖 AI & Automation
- **Smart Forecasting**: Dự báo doanh thu dựa trên lịch sử
- **Auto Voucher**: Tự động tạo voucher theo rules
- **Risk Detection**: Phát hiện hành vi bất thường
- **Peak Hours Analysis**: Phân tích giờ cao điểm

#### 🛡️ Security & Monitoring
- **IP Whitelist**: Chỉ cho phép IP tin cậy truy cập
- **Blocked IP**: Tự động chặn IP độc hại
- **User Monitoring**: Theo dõi hành vi người dùng
- **Activity Logs**: Ghi log tất cả hoạt động

#### 📊 Advanced Analytics
- **Multi-dimensional Reports**: Báo cáo đa chiều
- **Cohort Analysis**: Phân tích nhóm khách hàng
- **A/B Testing**: Test hiệu quả khuyến mãi
- **Predictive Analytics**: Dự đoán xu hướng

---

## 2. KIẾN TRÚC HỆ THỐNG

### 2.1 Sơ Đồ Kiến Trúc Backend
```
┌─────────────────────────────────────────────────────────────────────┐
│                         ANDROID APP (Kotlin)                        │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐     │
│  │ManagerActivity  │  │ManagerSettings  │  │ ManageOrders    │     │
│  │                 │  │ Fragment        │  │ Fragment        │     │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘     │
│           │                    │                    │               │
│           └────────────────────┼────────────────────┘               │
│                                │                                    │
│                    ┌───────────▼───────────┐                        │
│                    │    RetrofitClient     │                        │
│                    │    (ApiService)       │                        │
│                    └───────────┬───────────┘                        │
└────────────────────────────────┼────────────────────────────────────┘
                                 │ HTTP/HTTPS
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      SPRING BOOT BACKEND                            │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                    SECURITY FILTERS                          │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐   │   │
│  │  │BlockedIPFilter│→│JwtAuthFilter │→│WhitelistIPFilter │   │   │
│  │  └──────────────┘  └──────────────┘  └──────────────────┘   │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                │                                    │
│  ┌─────────────────────────────▼─────────────────────────────────┐ │
│  │                      CONTROLLERS                               │ │
│  │  ┌────────────────┐  ┌────────────────┐  ┌─────────────────┐  │ │
│  │  │ManagerController│  │WhitelistedIP  │  │ ForecastController│ │ │
│  │  │                │  │ Controller     │  │                 │  │ │
│  │  └───────┬────────┘  └───────┬────────┘  └────────┬────────┘  │ │
│  └──────────┼───────────────────┼───────────────────┼────────────┘ │
│             │                   │                   │               │
│  ┌──────────▼───────────────────▼───────────────────▼────────────┐ │
│  │                       SERVICES                                 │ │
│  │  ┌────────────────┐  ┌────────────────┐  ┌─────────────────┐  │ │
│  │  │ ManagerService │  │WhitelistedIP   │  │ ForecastService │  │ │
│  │  │                │  │ Service        │  │                 │  │ │
│  │  └───────┬────────┘  └───────┬────────┘  └────────┬────────┘  │ │
│  └──────────┼───────────────────┼───────────────────┼────────────┘ │
│             │                   │                   │               │
│  ┌──────────▼───────────────────▼───────────────────▼────────────┐ │
│  │                     REPOSITORIES                               │ │
│  │  ┌────────────────┐  ┌────────────────┐  ┌─────────────────┐  │ │
│  │  │OrderRepository │  │WhitelistedIP   │  │ UserRepository  │  │ │
│  │  │                │  │ Repository     │  │                 │  │ │
│  │  └───────┬────────┘  └───────┬────────┘  └────────┬────────┘  │ │
│  └──────────┼───────────────────┼───────────────────┼────────────┘ │
│             │                   │                   │               │
│             └───────────────────┼───────────────────┘               │
│                                 ▼                                   │
│                    ┌───────────────────────┐                        │
│                    │      MySQL 8.0        │                        │
│                    │   (Database Server)   │                        │
│                    └───────────────────────┘                        │
└─────────────────────────────────────────────────────────────────────┘
```##
# 2.2 Cấu Trúc File Backend
```
Backend_APP/src/main/java/com/utetea/backend/
├── controller/
│   ├── ManagerController.java      # API quản lý cho Manager
│   ├── WhitelistedIPController.java # API quản lý Whitelist IP
│   ├── BlockedIPController.java    # API quản lý Blocked IP
│   ├── DrinkController.java        # API quản lý sản phẩm
│   └── PromotionController.java    # API quản lý voucher
├── service/
│   ├── ManagerService.java         # Business logic Manager
│   ├── WhitelistedIPService.java   # Logic Whitelist IP
│   ├── BlockedIPService.java       # Logic Blocked IP
│   ├── DrinkService.java           # Logic quản lý sản phẩm
│   ├── PromotionService.java       # Logic voucher/khuyến mãi
│   └── ForecastService.java        # Dự báo doanh thu, nhân sự
├── filter/
│   ├── JwtAuthenticationFilter.java # Xác thực JWT
│   ├── BlockedIPFilter.java        # Chặn IP bị block
│   └── WhitelistIPFilter.java      # Kiểm tra Whitelist IP
├── model/
│   ├── User.java                   # Entity User
│   ├── Order.java                  # Entity Order
│   ├── WhitelistedIP.java          # Entity Whitelist IP
│   ├── BlockedIP.java              # Entity Blocked IP
│   ├── Drink.java                  # Entity Sản phẩm
│   └── Promotion.java              # Entity Voucher
└── repository/
    ├── UserRepository.java
    ├── OrderRepository.java
    ├── WhitelistedIPRepository.java
    ├── BlockedIPRepository.java
    ├── DrinkRepository.java
    └── PromotionRepository.java
```

---

## 3. QUẢN LÝ ĐƠN HÀNG

### 3.1 Luồng Cập Nhật Trạng Thái Đơn Hàng
```
┌─────────────────────────────────────────────────────────────────────┐
│                  FLOW: CẬP NHẬT TRẠNG THÁI ĐƠN HÀNG                 │
└─────────────────────────────────────────────────────────────────────┘

    ┌──────────────┐
    │ Manager App  │
    │ (Android)    │
    └──────┬───────┘
           │ 1. PUT /api/manager/orders/{id}/status?status=MAKING
           │    Header: Authorization: Bearer <JWT>
           ▼
    ┌──────────────────────────────────────────────────────────────┐
    │                    SECURITY FILTERS                          │
    │  ┌────────────────┐                                          │
    │  │ BlockedIPFilter│ → Kiểm tra IP có bị block không          │
    │  └───────┬────────┘                                          │
    │          │ ✅ IP không bị block                              │
    │          ▼                                                   │
    │  ┌────────────────┐                                          │
    │  │JwtAuthFilter   │ → Xác thực JWT token                     │
    │  │                │ → Extract username, role từ token        │
    │  └───────┬────────┘                                          │
    │          │ ✅ Token hợp lệ, role = MANAGER                   │
    │          ▼                                                   │
    │  ┌────────────────┐                                          │
    │  │WhitelistFilter │ → Kiểm tra IP có trong whitelist         │
    │  │                │   (Chỉ áp dụng cho ADMIN/MANAGER)        │
    │  └───────┬────────┘                                          │
    │          │ ✅ IP trong whitelist (hoặc tính năng tắt)        │
    └──────────┼───────────────────────────────────────────────────┘
               ▼
    ┌──────────────────────────────────────────────────────────────┐
    │                   ManagerController                          │
    │  @PutMapping("/orders/{orderId}/status")                     │
    │  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")             │
    │                                                              │
    │  2. Validate status parameter (PENDING, MAKING, DONE...)     │
    │  3. Gọi managerService.updateOrderStatus(orderId, newStatus) │
    └──────────┬───────────────────────────────────────────────────┘
               ▼
    ┌──────────────────────────────────────────────────────────────┐
    │                    ManagerService                            │
    │                                                              │
    │  4. getCurrentManager() → Lấy user từ SecurityContext        │
    │  5. getManagedStoreIds(manager) → Lấy danh sách store IDs    │
    │     - ADMIN: return null (quản lý tất cả)                    │
    │     - MANAGER: return list store IDs được gán                │
    │                                                              │
    │  6. orderRepository.findById(orderId) → Lấy order            │
    │                                                              │
    │  7. validateStoreAccess(manager, order.store.id)             │
    │     - ADMIN: bỏ qua kiểm tra                                 │
    │     - MANAGER: kiểm tra order.store.id có trong storeIds     │
    │       → Nếu không có quyền: throw BusinessException          │
    │                                                              │
    │  8. orderService.updateOrderStatus(orderId, newStatus)       │
    └──────────┬───────────────────────────────────────────────────┘
               ▼
    ┌──────────────────────────────────────────────────────────────┐
    │                     OrderService                             │
    │                                                              │
    │  9. Validate transition hợp lệ:                              │
    │     PENDING → MAKING → SHIPPING → READY → DONE               │
    │     Bất kỳ → CANCELED                                        │
    │                                                              │
    │  10. order.setStatus(newStatus)                              │
    │  11. orderRepository.save(order)                             │
    │                                                              │
    │  12. Gửi thông báo:                                          │
    │      - WebSocket: notifyOrderStatusChange(order)             │
    │      - Push: oneSignalService.sendToUser(...)                │
    │      - Email: emailService.sendOrderStatusEmail(...)         │
    └──────────┬───────────────────────────────────────────────────┘
               ▼
    ┌──────────────────────────────────────────────────────────────┐
    │                      RESPONSE                                │
    │  {                                                           │
    │    "success": true,                                          │
    │    "message": "Order status updated",                        │
    │    "data": { ...order details... }                           │
    │  }                                                           │
    └──────────────────────────────────────────────────────────────┘
```### 3.2
 Trạng Thái Đơn Hàng (OrderStatus)
```
┌──────────────────────────────────────────────────────────────────┐
│                    VÒNG ĐỜI ĐƠN HÀNG                                │
└─────────────────────────────────────────────────────────────────────┘

    ┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
    │ PENDING  │────▶│  MAKING  │────▶│ SHIPPING │────▶│  READY   │
    │ (Chờ xử  │     │ (Đang    │     │ (Đang    │     │ (Sẵn     │
    │  lý)     │     │  pha chế)│     │  giao)   │     │  sàng)   │
    └────┬─────┘     └────┬─────┘     └────┬─────┘     └────┬─────┘
         │                │                │                │
         │                │                │                ▼
         │                │                │           ┌──────────┐
         │                │                └──────────▶│   DONE   │
         │                │                            │ (Hoàn    │
         │                │                            │  thành)  │
         │                │                            └──────────┘
         │                │
         ▼                ▼
    ┌──────────────────────────┐
    │       CANCELED           │
    │   (Đã hủy - có thể      │
    │    hủy từ bất kỳ        │
    │    trạng thái nào)      │
    └──────────────────────────┘
```

---

## 4. QUẢN LÝ NGƯỜI DÙNG

### 4.1 Luồng Khóa/Mở Khóa Tài Khoản
```
┌─────────────────────────────────────────────────────────────────────┐
│                  FLOW: KHÓA/MỞ KHÓA TÀI KHOẢN                       │
└─────────────────────────────────────────────────────────────────────┘

    ┌──────────────┐
    │ Manager App  │
    └──────┬───────┘
           │ PUT /api/manager/users/{userId}/block?blocked=true
           ▼
    ┌──────────────────────────────────────────────────────────────┐
    │                   ManagerController                          │
    │  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")             │
    └──────────┬───────────────────────────────────────────────────┘
               ▼
    ┌──────────────────────────────────────────────────────────────┐
    │                    ManagerService                            │
    │                                                              │
    │  1. getCurrentManager() → Lấy current user                   │
    │                                                              │
    │  2. userRepository.findById(userId) → Lấy target user        │
    │                                                              │
    │  3. KIỂM TRA QUYỀN:                                          │
    │     ┌─────────────────────────────────────────────────────┐  │
    │     │ if (targetUser.role == ADMIN)                       │  │
    │     │   → throw "Không thể khóa tài khoản Admin"          │  │
    │     │                                                     │  │
    │     │ if (currentUser.role == MANAGER &&                  │  │
    │     │     targetUser.role == MANAGER)                     │  │
    │     │   → throw "Manager không có quyền khóa Manager khác"│  │
    │     └─────────────────────────────────────────────────────┘  │
    │                                                              │
    │  4. user.setIsBlocked(blocked)                               │
    │     user.setActive(!blocked)                                 │
    │                                                              │
    │  5. userRepository.save(user)                                │
    └──────────────────────────────────────────────────────────────┘
```

### 4.2 Luồng Nâng Cấp User → Manager (CHỈ ADMIN)
```
┌─────────────────────────────────────────────────────────────────────┐
│                  FLOW: NÂNG CẤP USER → MANAGER                      │
└─────────────────────────────────────────────────────────────────────┘

    ┌──────────────┐
    │  Admin App   │
    └──────┬───────┘
           │ PUT /api/manager/users/{userId}/promote
           ▼
    ┌──────────────────────────────────────────────────────────────┐
    │                    ManagerService                            │
    │                                                              │
    │  1. requireAdminRole() → Kiểm tra current user là ADMIN      │
    │     → Nếu không phải ADMIN: throw BusinessException          │
    │                                                              │
    │  2. userRepository.findById(userId) → Lấy target user        │
    │                                                              │
    │  3. KIỂM TRA:                                                │
    │     - Nếu đã là MANAGER → throw "Đã là Manager"              │
    │     - Nếu là ADMIN → throw "Không thể thay đổi role Admin"   │
    │     - Nếu bị khóa → throw "Không thể nâng cấp tài khoản bị   │
    │                            khóa"                             │
    │                                                              │
    │  4. user.setRole(UserRole.MANAGER)                           │
    │                                                              │
    │  5. userRepository.save(user)                                │
    │                                                              │
    │  ⚠️ LƯU Ý: Sau khi nâng cấp, Admin cần GÁN STORE cho        │
    │     Manager mới để họ có thể quản lý đơn hàng                │
    └──────────────────────────────────────────────────────────────┘
```-
--

## 5. QUẢN LÝ CỬA HÀNG

### 5.1 Mối Quan Hệ Manager - Store
```
┌─────────────────────────────────────────────────────────────────────┐
│                    QUAN HỆ MANAGER - STORE                          │
└─────────────────────────────────────────────────────────────────────┘

    ┌─────────────────────────────────────────────────────────────────┐
    │                         ADMIN                                   │
    │  ┌─────────────────────────────────────────────────────────┐   │
    │  │ Quản lý TẤT CẢ stores (không cần gán)                   │   │
    │  │ getManagedStoreIds() → return null (xem tất cả)         │   │
    │  └─────────────────────────────────────────────────────────┘   │
    └─────────────────────────────────────────────────────────────────┘

    ┌─────────────────────────────────────────────────────────────────┐
    │                        MANAGER A                                │
    │  ┌─────────────────────────────────────────────────────────┐   │
    │  │ managedStores = [Store 1, Store 2]                      │   │
    │  │ getManagedStoreIds() → return [1, 2]                    │   │
    │  │                                                         │   │
    │  │ ✅ Có thể xem/cập nhật đơn hàng của Store 1, Store 2    │   │
    │  │ ❌ Không thể xem đơn hàng của Store 3                   │   │
    │  └─────────────────────────────────────────────────────────┘   │
    └─────────────────────────────────────────────────────────────────┘

    ┌─────────────────────────────────────────────────────────────────┐
    │                        MANAGER B                                │
    │  ┌─────────────────────────────────────────────────────────┐   │
    │  │ managedStores = [] (chưa được gán)                      │   │
    │  │ getManagedStoreIds() → return [] (empty list)           │   │
    │  │                                                         │   │
    │  │ ⚠️ Không thể xem bất kỳ đơn hàng nào                    │   │
    │  │ ⚠️ Dashboard hiển thị empty                             │   │
    │  └─────────────────────────────────────────────────────────┘   │
    └─────────────────────────────────────────────────────────────────┘
```

### 5.2 Mối Quan Hệ Manager - Store

**Cấu trúc quan hệ Many-to-Many:**
- Một Manager có thể quản lý nhiều Store
- Một Store có thể có nhiều Manager
- ADMIN có quyền truy cập tất cả Store (không cần gán)

---

## 6. QUẢN LÝ SẢN PHẨM & DANH MỤC

### 6.1 Tổng Quan Quản Lý Sản Phẩm
```
┌─────────────────────────────────────────────────────────────────────┐
│                    QUẢN LÝ SẢN PHẨM (DRINK)                        │
├─────────────────────────────────────────────────────────────────────┤
│  📋 CHỨC NĂNG CHÍNH:                                               │
│  ├── Thêm/Sửa/Xóa sản phẩm (Drink)                                │
│  ├── Quản lý danh mục (Category)                                   │
│  ├── Quản lý topping                                               │
│  ├── Quản lý size và giá                                           │
│  ├── Upload/Quản lý hình ảnh sản phẩm                              │
│  └── Kích hoạt/Vô hiệu hóa sản phẩm                               │
└─────────────────────────────────────────────────────────────────────┘
```

### 6.2 Cấu Trúc Dữ Liệu Sản Phẩm

**Entities chính:**
- **Category**: Danh mục sản phẩm (Trà sữa, Cà phê, Sinh tố...)
- **Drink**: Sản phẩm chính với thông tin cơ bản
- **DrinkSize**: Size và giá của từng sản phẩm (S, M, L)
- **Topping**: Topping có thể thêm vào
- **DrinkTopping**: Quan hệ drink-topping với giá

### 6.3 Luồng Quản Lý Sản Phẩm
```
Manager/Admin → DrinkController → DrinkService → Database
                      ↓
              ┌─────────────────────────────────────────┐
              │         DrinkService Methods            │
              ├─────────────────────────────────────────┤
              │  • createDrink()                        │
              │  • updateDrink()                        │
              │  • deleteDrink()                        │
              │  • toggleAvailability()                 │
              │  • uploadDrinkImage()                   │
              │  • addTopping()                         │
              │  • removeTopping()                      │
              │  • updatePricing()                      │
              └─────────────────────────────────────────┘
```-
--

## 7. QUẢN LÝ VOUCHER & KHUYẾN MÃI

### 7.1 Tổng Quan Hệ Thống Voucher
```
┌─────────────────────────────────────────────────────────────────────┐
│                    HỆ THỐNG VOUCHER & KHUYẾN MÃI                   │
├─────────────────────────────────────────────────────────────────────┤
│  🎫 LOẠI VOUCHER:                                                  │
│  ├── Giảm giá theo % (PERCENTAGE)                                  │
│  ├── Giảm giá cố định (FIXED_AMOUNT)                              │
│  ├── Miễn phí ship (FREE_SHIPPING)                                │
│  ├── Mua 1 tặng 1 (BUY_ONE_GET_ONE)                              │
│  └── Voucher sinh nhật (BIRTHDAY_SPECIAL)                         │
│                                                                     │
│  🎯 ĐIỀU KIỆN ÁP DỤNG:                                            │
│  ├── Giá trị đơn hàng tối thiểu                                   │
│  ├── Số lượng sử dụng giới hạn                                    │
│  ├── Thời gian hiệu lực                                           │
│  ├── Áp dụng cho user cụ thể                                      │
│  └── Áp dụng cho sản phẩm/danh mục cụ thể                        │
└─────────────────────────────────────────────────────────────────────┘
```

### 7.2 Luồng Tạo & Áp Dụng Voucher
```
┌─────────────────────────────────────────────────────────────────────┐
│                        TẠO VOUCHER                                  │
├─────────────────────────────────────────────────────────────────────┤
│  Manager Input:                                                     │
│  ├── Mã voucher (VD: SUMMER2024)                                   │
│  ├── Loại giảm giá                                                 │
│  ├── Giá trị giảm                                                  │
│  ├── Điều kiện áp dụng                                             │
│  └── Thời gian hiệu lực                                            │
│                           ↓                                         │
│  PromotionService.createPromotion()                                 │
│  ├── Validate mã voucher unique                                    │
│  ├── Validate thời gian hợp lệ                                     │
│  ├── Validate điều kiện logic                                      │
│  ├── Check trùng mã voucher                                        │
│  ├── Lưu vào database                                              │
│  └── Log activity cho monitoring                                   │
└─────────────────────────────────────────────────────────────────────┘
```

### 7.3 Luồng Áp Dụng Voucher Khi Đặt Hàng
```
┌─────────────────────────────────────────────────────────────────────┐
│                      ÁP DỤNG VOUCHER                               │
├─────────────────────────────────────────────────────────────────────┤
│  User nhập mã voucher → CartService.applyVoucher()                 │
│                           ↓                                         │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                 VALIDATION STEPS                            │   │
│  │  1. Kiểm tra voucher tồn tại                               │   │
│  │  2. Kiểm tra còn hiệu lực (start_date, end_date)           │   │
│  │  3. Kiểm tra còn lượt sử dụng (usage_limit)               │   │
│  │  4. Kiểm tra user đã dùng chưa (per_user_limit)           │   │
│  │  5. Kiểm tra giá trị đơn hàng tối thiểu                    │   │
│  │  6. Kiểm tra sản phẩm áp dụng (nếu có)                     │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                           ↓                                         │
│  ✅ Tính toán giảm giá:                                            │
│  ├── PERCENTAGE: orderTotal * (discount_value / 100)               │
│  ├── FIXED_AMOUNT: discount_value                                  │
│  ├── FREE_SHIPPING: shipping_fee                                   │
│  └── Áp dụng max_discount_amount (nếu có)                          │
│                           ↓                                         │
│  💾 Lưu vào promotion_usages khi order thành công                  │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 8. LIVE CHAT HỖ TRỢ

### 8.1 Tổng Quan Live Chat System
```
┌─────────────────────────────────────────────────────────────────────┐
│                      LIVE CHAT SYSTEM                              │
├─────────────────────────────────────────────────────────────────────┤
│  💬 CHỨC NĂNG CHÍNH:                                               │
│  ├── Chat realtime giữa Customer và Manager                        │
│  ├── Hỗ trợ tư vấn sản phẩm                                       │
│  ├── Xử lý khiếu nại và hỗ trợ kỹ thuật                           │
│  ├── Lưu trữ lịch sử hội thoại                                     │
│  ├── Typing indicator (đang gõ...)                                │
│  ├── File/Image sharing                                            │
│  └── Auto-assign Manager theo store                               │
└─────────────────────────────────────────────────────────────────────┘
```

### 8.2 Luồng Chat Realtime
```
┌─────────────────────────────────────────────────────────────────────┐
│                    REALTIME CHAT FLOW                              │
└─────────────────────────────────────────────────────────────────────┘

    ┌──────────────┐                              ┌──────────────┐
    │ Customer App │                              │ Manager App  │
    │              │                              │              │
    └──────┬───────┘                              └──────┬───────┘
           │                                             │
           │ 1. Tạo conversation                         │
           │ POST /api/chat/conversations                │
           ▼                                             │
    ┌──────────────────────────────────────────────────────────────┐
    │                   ChatService                                │
    │  • Tạo conversation mới                                      │
    │  • Auto-assign Manager theo store gần nhất                   │
    │  • Gửi WebSocket notification cho Manager                    │
    └──────────────────────────────────────────────────────────────┘
           │                                             │
           │ 2. Gửi tin nhắn                            │
           │ POST /api/chat/messages                     │
           ▼                                             │
    ┌──────────────────────────────────────────────────────────────┐
    │                 WebSocket Server                             │
    │  • Lưu message vào database                                  │
    │  • Broadcast đến tất cả participants                         │
    │  • Update conversation last_message                          │
    └──────────────────────────────────────────────────────────────┘
           │                                             │
           │ 3. Realtime message delivery                │
           ▼                                             ▼
    ┌──────────────┐                              ┌──────────────┐
    │ Customer     │◄────── WebSocket ──────────►│ Manager      │
    │ receives     │        /topic/chat/{convId}  │ receives     │
    │ message      │                              │ message      │
    └──────────────┘                              └──────────────┘
```---

### 15.7 DrinkService - Quản Lý Sản Phẩm

#### getAllActiveDrinks() - Tối Ưu N+1 Query
```java
/**
 * FIX N+1 QUERY: Load tất cả drinks với sizes trong 1 query,
 * sau đó batch load toppings
 */
@Transactional(readOnly = true)
@Cacheable(value = DRINKS_CACHE, key = "'all-active'")
public List<DrinkDto> getAllActiveDrinks() {
    // Query 1: Load drinks với sizes và category (JOIN FETCH)
    List<Drink> drinks = drinkRepository.findByIsActiveTrueWithSizesAndCategory();
    
    if (drinks.isEmpty()) {
        return List.of();
    }
    
    // Query 2: Batch load toppings cho tất cả drinks
    List<Long> drinkIds = drinks.stream().map(Drink::getId).collect(Collectors.toList());
    Map<Long, List<DrinkTopping>> toppingsMap = loadToppingsForDrinks(drinkIds);
    
    // Query 3: Load global toppings (drink_id = NULL)
    List<DrinkTopping> globalToppings = drinkToppingRepository.findByDrinkIdIsNullAndIsActiveTrue();
    
    return drinks.stream()
        .map(drink -> mapToDtoOptimized(drink, toppingsMap.get(drink.getId()), globalToppings))
        .collect(Collectors.toList());
}
```

**Giải thích:**
- **@Cacheable**: Cache kết quả để tránh query lại
- **JOIN FETCH**: Load sizes và category trong 1 query
- **Batch Load**: Load toppings cho tất cả drinks cùng lúc
- **Global Toppings**: Toppings áp dụng cho tất cả drinks (drink_id = NULL)

#### searchDrinks() - Tìm Kiếm An Toàn
```java
@Transactional(readOnly = true)
public List<DrinkDto> searchDrinks(String keyword) {
    // Input sanitization: validate and clean keyword
    if (keyword == null || keyword.trim().isEmpty()) {
        return List.of();
    }
    
    // Sanitize: remove special characters, keep Vietnamese characters
    String sanitized = keyword.replaceAll(
        "[^a-zA-Z0-9\\sàáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴÈÉẸẺẼÊỀẾỆỂỄÌÍỊỈĨÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠÙÚỤỦŨƯỪỨỰỬỮỲÝỴỶỸĐ]", 
        "");
    
    // Giới hạn độ dài để tránh DoS
    if (sanitized.length() > 100) {
        sanitized = sanitized.substring(0, 100);
    }
    
    if (sanitized.trim().isEmpty()) {
        return List.of();
    }
    
    List<Drink> drinks = drinkRepository.searchByNameWithSizesAndCategory(sanitized);
    // ... batch load toppings và map to DTO
}
```

**Giải thích:**
- **Input Sanitization**: Loại bỏ ký tự đặc biệt để tránh SQL injection
- **Vietnamese Support**: Giữ lại ký tự tiếng Việt
- **Length Limit**: Giới hạn 100 ký tự để tránh DoS attack

#### createDrink() - Tạo Sản Phẩm Mới
```java
@Transactional
@CacheEvict(value = DRINKS_CACHE, allEntries = true)
public DrinkDto createDrink(DrinkDto dto) {
    Drink drink = new Drink();
    drink.setName(dto.getName());
    drink.setDescription(dto.getDescription());
    drink.setImageUrl(dto.getImageUrl());
    drink.setBasePrice(dto.getBasePrice());
    drink.setIsActive(true);
    
    drink = drinkRepository.save(drink);
    return mapToDto(drink);
}
```

**Giải thích:**
- **@CacheEvict**: Xóa cache khi tạo mới để đảm bảo data consistency
- **allEntries = true**: Xóa tất cả entries trong cache

---

### 15.8 PromotionService - Quản Lý Voucher

#### validatePromotion() - Core Validation Method
```java
/**
 * ✅ SECURITY: VALIDATE PROMOTION - CORE METHOD
 * Kiểm tra tất cả điều kiện của voucher
 */
private void validatePromotion(Promotion promotion, BigDecimal orderAmount, Long userId) {
    LocalDateTime now = LocalDateTime.now();
    
    // 1. Check active
    if (!promotion.getIsActive()) {
        throw new BusinessException("Mã voucher đã bị vô hiệu hóa");
    }
    
    // 2. Check thời gian bắt đầu
    if (now.isBefore(promotion.getStartDate())) {
        throw new BusinessException("Mã voucher chưa có hiệu lực");
    }
    
    // 3. Check thời gian kết thúc
    if (now.isAfter(promotion.getEndDate())) {
        throw new VoucherExpiredException(
            "Mã voucher đã hết hạn vào " + promotion.getEndDate()
        );
    }
    
    // 4. Check usage limit (tổng số lần dùng)
    if (promotion.getUsageLimit() != null && 
        promotion.getUsedCount() >= promotion.getUsageLimit()) {
        throw new BusinessException("Mã voucher đã hết lượt sử dụng");
    }
    
    // 5. ✅ SECURITY: Check user đã dùng voucher này chưa
    if (userId != null) {
        boolean hasUsed = promotionUsageRepository.existsByPromotionIdAndUserId(
            promotion.getId(), userId
        );
        
        if (hasUsed) {
            throw new VoucherAlreadyUsedException(
                "Bạn đã sử dụng mã voucher này rồi"
            );
        }
    }
    
    // 6. Check minimum order value
    if (orderAmount != null && orderAmount.compareTo(promotion.getMinOrderValue()) < 0) {
        throw new BusinessException(String.format(
            "Giá trị đơn hàng tối thiểu là %s VND cho mã voucher này", 
            promotion.getMinOrderValue()
        ));
    }
}
```

**Giải thích:**
- **6 bước validation**: Kiểm tra đầy đủ tất cả điều kiện
- **Custom Exceptions**: VoucherExpiredException, VoucherAlreadyUsedException
- **User-specific check**: Kiểm tra user đã dùng voucher chưa

#### createPromotion() - Tạo Voucher Mới
```java
@Transactional
@CacheEvict(value = PROMOTIONS_CACHE, allEntries = true)
public PromotionDto createPromotion(CreatePromotionRequest request) {
    log.info("Creating new promotion with code: {}", request.getCode());
    
    // Validate code uniqueness
    if (promotionRepository.findByCode(request.getCode()).isPresent()) {
        throw new BusinessException("Mã voucher đã tồn tại: " + request.getCode());
    }
    
    // Validate dates
    if (request.getEndDate().isBefore(request.getStartDate())) {
        throw new BusinessException("Ngày kết thúc phải sau ngày bắt đầu");
    }
    
    // Validate discount value for PERCENT type
    if (request.getDiscountType() == DiscountType.PERCENT) {
        if (request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException("Giá trị giảm giá phần trăm không được vượt quá 100%");
        }
    }
    
    Promotion promotion = Promotion.builder()
            .code(request.getCode().toUpperCase())
            .description(request.getDescription())
            .discountType(request.getDiscountType())
            .discountValue(request.getDiscountValue())
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .minOrderValue(request.getMinOrderValue())
            .maxDiscountAmount(request.getMaxDiscountAmount())
            .usageLimit(request.getUsageLimit())
            .usedCount(0)
            .isActive(request.getIsActive())
            .build();
    
    promotion = promotionRepository.save(promotion);
    
    // Gửi thông báo voucher mới cho tất cả user
    if (promotion.getIsActive()) {
        String title = "🎉 Voucher mới dành cho bạn!";
        String content = promotion.getDescription() + " - Mã: " + promotion.getCode();
        oneSignalService.sendToAll(title, content, NotificationType.PROMOTION, promotion.getId());
    }
    
    return promotionMapper.toDto(promotion);
}
```

**Giải thích:**
- **Code Uniqueness**: Kiểm tra mã voucher không trùng
- **Date Validation**: Ngày kết thúc phải sau ngày bắt đầu
- **Percent Validation**: Giảm giá % không được > 100%
- **Push Notification**: Gửi thông báo cho tất cả user khi tạo voucher mới

---

### 15.9 OrderService - Xử Lý Đơn Hàng

#### previewBill() - Xem Trước Hóa Đơn
```java
/**
 * Preview bill trước khi thanh toán
 * Tính toán chi tiết đơn hàng, giá tiền, giảm giá để user xem trước
 */
@Transactional(readOnly = true)
public BillPreviewDto previewBill(String username, OrderRequest request) {
    log.info("Generating bill preview for user: {}", username);
    
    // Validate request
    validateOrderRequest(request);
    
    // Get user info
    User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    
    // Get store info
    Store store = storeRepository.findById(request.getStoreId())
            .orElseThrow(() -> new ResourceNotFoundException("Store", "id", request.getStoreId()));
    
    // Calculate items
    List<BillPreviewDto.BillItemDto> billItems = new ArrayList<>();
    BigDecimal subtotal = BigDecimal.ZERO;
    
    for (OrderItemRequest itemReq : request.getItems()) {
        Drink drink = drinkRepository.findById(itemReq.getDrinkId())
                .orElseThrow(() -> new ResourceNotFoundException("Drink", "id", itemReq.getDrinkId()));
        
        if (!drink.getIsActive()) {
            throw new BusinessException("Drink '" + drink.getName() + "' is not available");
        }
        
        BigDecimal unitPrice = drink.getBasePrice();
        
        // Add size price
        // Add toppings price
        // ... (tính toán chi tiết)
        
        BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
        subtotal = subtotal.add(itemTotal);
        
        billItems.add(BillPreviewDto.BillItemDto.builder()
                .drinkName(drink.getName())
                .sizeName(itemReq.getSizeName())
                .quantity(itemReq.getQuantity())
                .unitPrice(unitPrice)
                .totalPrice(itemTotal)
                .build());
    }
    
    // Calculate discount (voucher + tier)
    BigDecimal voucherDiscount = calculateVoucherDiscount(request, subtotal);
    BigDecimal tierDiscount = memberTierService.calculateTierDiscount(user.getMemberTier(), subtotal);
    
    // Calculate shipping fee
    BigDecimal shippingFee = calculateShippingFee(request, user, subtotal);
    
    // Final price
    BigDecimal totalDiscount = voucherDiscount.add(tierDiscount);
    BigDecimal finalPrice = subtotal.add(shippingFee).subtract(totalDiscount);
    
    return BillPreviewDto.builder()
            .items(billItems)
            .subtotal(subtotal)
            .shippingFee(shippingFee)
            .voucherDiscount(voucherDiscount)
            .tierDiscountAmount(tierDiscount)
            .totalDiscount(totalDiscount)
            .finalPrice(finalPrice)
            .build();
}
```

**Giải thích:**
- **Preview trước khi đặt**: User xem trước tổng tiền trước khi thanh toán
- **Tính toán đầy đủ**: Giá sản phẩm + size + topping + voucher + tier discount + shipping
- **Free Shipping**: Kiểm tra điều kiện miễn phí ship theo hạng thành viên

#### createOrder() - Tạo Đơn Hàng
```java
@Transactional
public OrderDto createOrder(String username, OrderRequest request) {
    log.info("Creating order for user: {}, store: {}", username, request.getStoreId());

    // ✅ SECURITY: VALIDATE REQUEST TRƯỚC KHI XỬ LÝ
    validateOrderRequest(request);

    // Validate user
    User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

    if (!user.getActive()) {
        throw new BusinessException("User account is inactive", HttpStatus.FORBIDDEN);
    }
    
    // ✅ SECURITY: Check rate limit (20 đơn/giờ per user)
    rateLimitService.checkOrderRateLimit(user.getId());

    // Validate store
    Store store = storeRepository.findById(request.getStoreId())
            .orElseThrow(() -> new ResourceNotFoundException("Store", "id", request.getStoreId()));

    Order order = new Order();
    order.setUser(user);
    order.setStore(store);
    order.setType(request.getType());
    order.setStatus(OrderStatus.PENDING);
    order.setPaymentMethod(request.getPaymentMethod());

    // Tính toán items và giá
    BigDecimal totalPrice = calculateOrderItems(order, request);
    
    // Apply voucher với PESSIMISTIC_WRITE lock để tránh race condition
    BigDecimal discount = applyVoucherWithLock(request, totalPrice, order);
    
    // Apply tier discount (cộng dồn với voucher)
    BigDecimal tierDiscount = memberTierService.calculateTierDiscount(user.getMemberTier(), totalPrice);
    discount = discount.add(tierDiscount);
    
    order.setDiscount(discount);
    
    // Calculate shipping fee với free ship check
    BigDecimal shippingFee = calculateShippingWithFreeShipCheck(request, user, totalPrice);
    order.setShippingFee(shippingFee);
    
    // Final price = totalPrice + shippingFee - discount
    BigDecimal finalPrice = totalPrice.add(shippingFee).subtract(discount);
    if (finalPrice.compareTo(BigDecimal.ZERO) < 0) {
        finalPrice = BigDecimal.ZERO;
    }
    order.setFinalPrice(finalPrice);

    order = orderRepository.save(order);
    log.info("Order created successfully with id: {}", order.getId());

    // 🛡️ Log activity - Tạo đơn hàng (Monitoring)
    userMonitoringService.logOrderCreate(user.getId(), order.getId(), 
        order.getFinalPrice().doubleValue(), RequestContextUtil.getCurrentRequest());

    // Notifications
    orderWebSocketService.notifyNewOrder(orderDto);      // WebSocket
    emailService.sendOrderConfirmationEmail(order);       // Email
    sendNotificationToManagers(order);                    // Push to Manager

    return mapToDto(order);
}
```

**Giải thích:**
- **Rate Limiting**: Giới hạn 20 đơn/giờ per user để tránh spam
- **PESSIMISTIC_WRITE Lock**: Tránh race condition khi dùng voucher
- **Tier Discount**: Cộng dồn với voucher discount
- **Free Shipping**: Kiểm tra điều kiện miễn phí ship
- **Multi-channel Notifications**: WebSocket + Email + Push

#### updateOrderStatus() - Cập Nhật Trạng Thái
```java
@Transactional
public OrderDto updateOrderStatus(Long orderId, OrderStatus newStatus) {
    log.info("Updating order {} status to {}", orderId, newStatus);

    Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

    // Validate transition hợp lệ
    validateStatusTransition(order.getStatus(), newStatus);

    Long userId = order.getUser() != null ? order.getUser().getId() : null;
    
    order.setStatus(newStatus);
    order = orderRepository.save(order);
    orderRepository.flush();

    OrderDto orderDto = mapToDto(order);
    
    // Xử lý các tác vụ phụ trong thread riêng
    final Order finalOrder = order;
    final Long finalUserId = userId;
    
    new Thread(() -> {
        processPostStatusUpdate(finalOrder, orderDto, newStatus, finalUserId, orderId);
    }).start();

    return orderDto;
}

/**
 * Validate transition hợp lệ giữa các trạng thái
 */
private void validateStatusTransition(OrderStatus current, OrderStatus newStatus) {
    // PENDING → MAKING → SHIPPING → READY → DONE
    // Bất kỳ → CANCELED
    
    if (newStatus == OrderStatus.CANCELED) {
        return; // Luôn cho phép hủy
    }
    
    // Kiểm tra transition hợp lệ
    Map<OrderStatus, List<OrderStatus>> validTransitions = Map.of(
        OrderStatus.PENDING, List.of(OrderStatus.MAKING, OrderStatus.CANCELED),
        OrderStatus.MAKING, List.of(OrderStatus.SHIPPING, OrderStatus.READY, OrderStatus.CANCELED),
        OrderStatus.SHIPPING, List.of(OrderStatus.READY, OrderStatus.DONE, OrderStatus.CANCELED),
        OrderStatus.READY, List.of(OrderStatus.DONE, OrderStatus.CANCELED)
    );
    
    if (!validTransitions.getOrDefault(current, List.of()).contains(newStatus)) {
        throw new BusinessException("Invalid status transition: " + current + " → " + newStatus);
    }
}
```

**Giải thích:**
- **Status Transition Validation**: Chỉ cho phép chuyển trạng thái hợp lệ
- **Async Post-processing**: Xử lý notifications trong thread riêng
- **Monitoring**: Log activity khi hủy đơn

---

### 15.10 AuthService - Xác Thực & Đăng Ký

#### login() - Đăng Nhập
```java
@Transactional
public LoginResponse login(LoginRequest request) {
    log.debug("Login attempt for: {}", request.getUsernameOrPhone());

    // Lấy HttpServletRequest để ghi log monitoring
    HttpServletRequest httpRequest = getHttpServletRequest();

    try {
        // Authenticate với Spring Security
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsernameOrPhone(),
                request.getPassword()
            )
        );
    } catch (BadCredentialsException e) {
        // 🛡️ Ghi log đăng nhập thất bại cho monitoring
        userMonitoringService.logLoginFailed(request.getUsernameOrPhone(), httpRequest);
        throw new BusinessException("Invalid credentials");
    }

    User user = userRepository.findByUsernameOrPhone(
        request.getUsernameOrPhone(),
        request.getUsernameOrPhone()
    ).orElseThrow(() -> new BusinessException("Invalid credentials"));

    // Kiểm tra tài khoản bị khóa
    if (user.getIsBlocked()) {
        throw new BusinessException("Account is blocked");
    }

    if (!user.getActive()) {
        throw new BusinessException("Account is inactive");
    }

    // 🛡️ Ghi log đăng nhập thành công
    userMonitoringService.logLoginSuccess(user.getId(), httpRequest);
    
    // 🚨 Check thiết bị/IP mới
    checkNewDeviceLogin(user.getId(), httpRequest);

    // Generate tokens
    UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
    String accessToken = jwtUtil.generateToken(userDetails, user.getRole().name());
    String refreshToken = jwtUtil.generateRefreshToken(userDetails);

    return mapToLoginResponse(user, accessToken, refreshToken);
}
```

**Giải thích:**
- **Spring Security Authentication**: Sử dụng AuthenticationManager
- **Monitoring Integration**: Log cả thành công và thất bại
- **New Device Detection**: Phát hiện đăng nhập từ thiết bị mới
- **Dual Token**: Access token + Refresh token

#### registerWithOtp() - Đăng Ký Với OTP
```java
@Transactional
public void registerWithOtp(RegisterRequest request) {
    // Validate uniqueness
    if (userRepository.existsByUsername(request.getUsername())) {
        throw new BusinessException("Username already exists");
    }
    if (userRepository.existsByEmail(request.getEmail())) {
        throw new BusinessException("Email already exists");
    }

    // Tạo user với trạng thái inactive
    User user = new User();
    user.setUsername(request.getUsername());
    user.setEmail(request.getEmail());
    user.setPassword(passwordEncoder.encode(request.getPassword()));
    user.setRole(UserRole.USER);
    user.setMemberTier(MemberTier.BRONZE);
    user.setPoints(0);
    user.setActive(false);  // Chưa active
    user.setIsBlocked(false);
    
    // Tạo OTP và gán vào user
    String otp = otpService.generateOtp();
    user.setOtp(otp);
    user.setOtpExpiry(LocalDateTime.now().plusMinutes(5)); // 5 phút

    user = userRepository.save(user);

    // Gửi OTP qua email
    otpService.sendOtp(otp, request.getEmail());
}
```

**Giải thích:**
- **OTP Flow**: Tạo user inactive → Gửi OTP → Verify → Active
- **Password Encoding**: BCrypt encryption
- **Default Tier**: BRONZE với 0 điểm
- **OTP Expiry**: 5 phút

---

### 15.11 CartService - Giỏ Hàng

#### addToCart() - Thêm Vào Giỏ Hàng
```java
@Transactional
public CartDto addToCart(Long userId, AddToCartRequest request) {
    // Lấy hoặc tạo cart cho user
    Cart cart = cartRepository.findByUserId(userId)
            .orElseGet(() -> {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                Cart newCart = new Cart();
                newCart.setUser(user);
                newCart.setItems(new ArrayList<>());
                return cartRepository.save(newCart);
            });
    
    // Lấy thông tin drink
    Drink drink = drinkRepository.findById(request.getDrinkId())
            .orElseThrow(() -> new ResourceNotFoundException("Drink not found"));
    
    // Tính giá: basePrice + sizePrice + toppingsPrice
    BigDecimal unitPrice = drink.getBasePrice();
    
    // Add size price
    if (request.getSizeId() != null) {
        DrinkSize size = drinkSizeRepository.findById(request.getSizeId())
                .orElseThrow(() -> new ResourceNotFoundException("Size not found"));
        unitPrice = unitPrice.add(size.getExtraPrice());
    }
    
    // Add toppings price
    List<DrinkTopping> toppings = new ArrayList<>();
    if (request.getToppingIds() != null && !request.getToppingIds().isEmpty()) {
        toppings = drinkToppingRepository.findAllById(request.getToppingIds());
        for (DrinkTopping topping : toppings) {
            unitPrice = unitPrice.add(topping.getPrice());
        }
    }
    
    BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));
    
    // Tạo cart item
    CartItem cartItem = new CartItem();
    cartItem.setCart(cart);
    cartItem.setDrink(drink);
    cartItem.setQuantity(request.getQuantity());
    cartItem.setUnitPrice(unitPrice.doubleValue());
    cartItem.setTotalPrice(totalPrice.doubleValue());
    cartItem.setToppings(toppings);
    
    cart.getItems().add(cartItem);
    cartItemRepository.save(cartItem);
    
    // 🛡️ Log activity - Thêm vào giỏ hàng
    userMonitoringService.logCartAddItem(userId, drink.getName(), request.getQuantity(), 
        RequestContextUtil.getCurrentRequest());
    
    return getCart(userId);
}
```

#### reorderFromHistory() - Đặt Lại Đơn Hàng
```java
@Transactional
public ReorderResponse reorderFromHistory(Long userId, Long orderId) {
    // Lấy đơn hàng cũ
    Order order = orderRepository.findByIdWithItems(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
    
    // Verify user owns this order
    if (!order.getUser().getId().equals(userId)) {
        throw new IllegalArgumentException("Bạn không có quyền đặt lại đơn hàng này");
    }
    
    List<ReorderResponse.ReorderItemStatus> itemStatuses = new ArrayList<>();
    boolean hasUnavailableItems = false;
    int addedCount = 0;
    
    for (OrderItem orderItem : order.getItems()) {
        // Kiểm tra drink còn bán không
        Drink drink = orderItem.getDrink();
        if (drink == null || !drink.getIsActive()) {
            // Tìm gợi ý thay thế
            List<SuggestionDto> suggestions = findSimilarDrinks(drink, orderItem.getDrinkNameSnapshot());
            itemStatuses.add(ReorderItemStatus.builder()
                    .drinkName(orderItem.getDrinkNameSnapshot())
                    .drinkAvailable(false)
                    .addedToCart(false)
                    .suggestions(suggestions)
                    .build());
            hasUnavailableItems = true;
            continue;
        }
        
        // Kiểm tra size và toppings còn không
        // Thêm vào giỏ hàng với các item còn available
        // ...
        addedCount++;
    }
    
    return ReorderResponse.builder()
            .cart(getCart(userId))
            .itemStatuses(itemStatuses)
            .hasUnavailableItems(hasUnavailableItems)
            .message(hasUnavailableItems ? 
                "Một số món không còn bán" : "Đã thêm tất cả món vào giỏ hàng")
            .build();
}
```

**Giải thích:**
- **Smart Reorder**: Kiểm tra món còn bán không, gợi ý thay thế
- **Partial Success**: Thêm được món nào thì thêm, báo lỗi món không có
- **Suggestions**: Tìm món tương tự trong cùng category

---

### 15.12 LiveChatService - Chat Realtime

#### startConversation() - Bắt Đầu Hội Thoại
```java
@Transactional
public ConversationDto startConversation(String username, StartConversationRequest request) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

    // Validate storeId - bắt buộc phải chọn chi nhánh
    if (request.getStoreId() == null) {
        throw new BusinessException("Vui lòng chọn chi nhánh để được tư vấn");
    }
    
    Store store = storeRepository.findById(request.getStoreId())
        .orElseThrow(() -> new ResourceNotFoundException("Store", "id", request.getStoreId()));

    // Kiểm tra conversation đang active với store này
    var existingConversation = conversationRepository.findActiveByUserIdAndStoreId(user.getId(), store.getId());
    if (existingConversation.isPresent()) {
        // Tiếp tục conversation cũ
        ChatConversation conversation = existingConversation.get();
        
        if (request.getInitialMessage() != null) {
            // Thêm tin nhắn mới
            LiveChatMessage message = new LiveChatMessage();
            message.setConversation(conversation);
            message.setSender(user);
            message.setContent(request.getInitialMessage());
            message.setSenderType(SenderType.USER);
            messageRepository.save(message);
            
            // Notify qua WebSocket
            webSocketService.notifyNewMessage(conversation.getId(), mapMessageToDto(message));
            
            // Push notification cho Manager
            sendPushToManagersAndAdmin(store, user, "💬 Tin nhắn tư vấn mới", 
                    user.getFullName() + ": " + truncateMessage(request.getInitialMessage()), 
                    conversation.getId());
        }
        
        return mapToDto(conversation);
    }

    // Tạo conversation mới
    ChatConversation conversation = new ChatConversation();
    conversation.setUser(user);
    conversation.setStore(store);
    conversation.setStatus(ConversationStatus.WAITING);
    conversation.setSubject(request.getSubject());
    conversation = conversationRepository.save(conversation);

    // Notify managers về conversation mới
    webSocketService.notifyNewConversation(mapToDto(conversation));
    sendPushToManagersAndAdmin(store, user, "💬 Yêu cầu tư vấn mới", 
            user.getFullName() + " cần tư vấn tại " + store.getStoreName(), 
            conversation.getId());

    return mapToDto(conversation);
}
```

#### sendMessage() - Gửi Tin Nhắn
```java
@Transactional
public MessageDto sendMessage(String username, SendMessageRequest request) {
    User sender = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

    ChatConversation conversation = conversationRepository.findById(request.getConversationId())
        .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", request.getConversationId()));

    // Kiểm tra conversation đã đóng chưa
    if (conversation.getStatus() == ConversationStatus.CLOSED) {
        throw new BusinessException("Cuộc hội thoại đã đóng");
    }

    // Validate quyền gửi tin nhắn
    boolean isUser = conversation.getUser().getId().equals(sender.getId());
    boolean isAdmin = sender.getRole() == UserRole.ADMIN;
    boolean isManager = sender.getRole() == UserRole.MANAGER;
    
    // Manager phải quản lý store của conversation này
    if (isManager && !isAdmin && !canManagerAccessConversation(sender, conversation)) {
        throw new BusinessException("Bạn không có quyền tư vấn cho chi nhánh này");
    }

    // Tạo tin nhắn
    LiveChatMessage message = new LiveChatMessage();
    message.setConversation(conversation);
    message.setSender(sender);
    message.setContent(request.getContent());
    message.setSenderType((isManager || isAdmin) ? SenderType.MANAGER : SenderType.USER);
    message = messageRepository.save(message);

    // Cập nhật unread count
    if (isUser) {
        conversation.setUnreadManager(conversation.getUnreadManager() + 1);
    } else {
        conversation.setUnreadUser(conversation.getUnreadUser() + 1);
        // Manager tiếp nhận conversation
        if (conversation.getStatus() == ConversationStatus.WAITING) {
            conversation.setStatus(ConversationStatus.ACTIVE);
            conversation.setManager(sender);
        }
    }
    conversationRepository.save(conversation);

    // Notify qua WebSocket
    MessageDto dto = mapMessageToDto(message);
    webSocketService.notifyNewMessage(conversation.getId(), dto);

    // Push notification
    if (isUser) {
        sendPushToManagersAndAdmin(conversation.getStore(), sender, "💬 Tin nhắn mới", ...);
    } else {
        sendPushToUser(conversation.getUser(), "💬 Phản hồi từ nhân viên", ...);
    }

    return dto;
}
```

**Giải thích:**
- **Store-based Routing**: Conversation gắn với store cụ thể
- **Manager Access Control**: Manager chỉ xem conversation của stores được gán
- **Conversation Status**: WAITING → ACTIVE → CLOSED
- **Dual Notification**: WebSocket realtime + Push notification

---

### 15.13 ForecastService - Dự Báo Doanh Thu

#### calculateRevenueForecast() - Dự Báo Doanh Thu
```java
@Transactional(readOnly = true)
public RevenueForecast calculateRevenueForecast() {
    RevenueForecast forecast = new RevenueForecast();
    
    // Lấy dữ liệu lịch sử theo ngày trong tuần (có trọng số)
    // Tuần gần nhất có trọng số cao hơn (RECENT_WEIGHT = 3, OLDER_WEIGHT = 1)
    Map<DayOfWeek, BigDecimal> weightedAvgRevenue = getWeightedAvgRevenueByDayOfWeek();
    
    // Dự báo hôm nay
    LocalDate today = LocalDate.now();
    BigDecimal todayForecast = weightedAvgRevenue.getOrDefault(today.getDayOfWeek(), BigDecimal.ZERO);
    
    // Áp dụng hệ số mùa (tháng 6-7 nóng → +20%)
    todayForecast = applySeasonalFactor(todayForecast, today);
    
    // Áp dụng hệ số ngày đặc biệt (Valentine, Giáng sinh → +30%)
    todayForecast = applySpecialDateFactor(todayForecast, today);
    
    // Điều chỉnh theo tiến độ trong ngày (real-time adjustment)
    int currentHour = LocalDateTime.now().getHour();
    if (currentHour >= 8 && currentHour < 22) {
        BigDecimal todayActual = getTodayRevenue();
        double progressRatio = (currentHour - 8) / 14.0; // 8h-22h = 14 tiếng
        
        if (progressRatio > 0.2 && todayActual.compareTo(BigDecimal.ZERO) > 0) {
            // Blend giữa dự báo và thực tế
            BigDecimal projectedFromActual = todayActual.divide(BigDecimal.valueOf(progressRatio), 2, RoundingMode.HALF_UP);
            
            double actualWeight = Math.min(progressRatio * 1.5, 0.8); // Max 80%
            todayForecast = projectedFromActual.multiply(BigDecimal.valueOf(actualWeight))
                .add(todayForecast.multiply(BigDecimal.valueOf(1 - actualWeight)));
        }
    }
    
    forecast.setTodayForecast(todayForecast);
    // ... tính toán tương tự cho tomorrow, week, month
    
    return forecast;
}
```

**Giải thích:**
- **Weighted Moving Average**: Tuần gần nhất có trọng số cao hơn
- **Seasonal Factors**: Điều chỉnh theo mùa (hè nóng → bán nhiều hơn)
- **Special Dates**: Ngày lễ tăng 30%
- **Real-time Adjustment**: Blend dự báo với thực tế trong ngày

---

### 15.14 MemberTierService - Hạng Thành Viên

#### calculateTierDiscount() - Tính Giảm Giá Theo Hạng
```java
public BigDecimal calculateTierDiscount(MemberTier tier, BigDecimal orderTotal) {
    TierBenefits benefits = tierConfig.getBenefits(tier);
    if (benefits.getDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
        return orderTotal.multiply(benefits.getDiscountPercent())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
    return BigDecimal.ZERO;
}
```

#### isEligibleForFreeShipping() - Kiểm Tra Miễn Phí Ship
```java
public boolean isEligibleForFreeShipping(MemberTier tier, BigDecimal orderTotal) {
    TierBenefits benefits = tierConfig.getBenefits(tier);
    if (!benefits.isFreeShipping()) {
        return false;
    }
    return orderTotal.compareTo(benefits.getFreeShippingMinOrder()) >= 0;
}
```

#### checkAndUpgradeTier() - Kiểm Tra & Nâng Hạng
```java
@Transactional
public MemberTier checkAndUpgradeTier(String username) {
    User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    
    MemberTier newTier = tierConfig.getTierByPoints(user.getPoints());
    
    if (newTier != user.getMemberTier()) {
        MemberTier oldTier = user.getMemberTier();
        user.setMemberTier(newTier);
        userRepository.save(user);
        
        // 🎖️ Gửi thông báo khi lên cấp
        sendTierUpgradeNotification(user, oldTier, newTier);
    }
    
    return user.getMemberTier();
}
```

**Giải thích:**
- **4 Tiers**: BRONZE → SILVER → GOLD → PLATINUM
- **Points-based**: Tự động nâng hạng khi đủ điểm
- **Benefits**: Discount %, free shipping, birthday voucher, priority support
- **Notification**: Push notification khi lên hạng

---

### 15.15 ReviewService - Đánh Giá Sản Phẩm

#### createReview() - Tạo Đánh Giá
```java
@Transactional
public ReviewDto createReview(String username, CreateReviewRequest request) {
    User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    
    // Validate order belongs to user and is DONE
    Order order = orderRepository.findById(request.getOrderId())
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    
    if (!order.getUser().getId().equals(user.getId())) {
        throw new BadRequestException("Order does not belong to this user");
    }
    
    if (order.getStatus() != OrderStatus.DONE) {
        throw new BadRequestException("Can only review completed orders");
    }
    
    // Check if already reviewed
    if (reviewRepository.existsByUserIdAndOrderItemId(user.getId(), request.getOrderItemId())) {
        throw new BadRequestException("You have already reviewed this item");
    }
    
    // Create review
    Review review = new Review();
    review.setUser(user);
    review.setDrink(orderItem.getDrink());
    review.setOrder(order);
    review.setOrderItem(orderItem);
    review.setRating(request.getRating());
    review.setComment(request.getComment());
    review.setIsAnonymous(request.getIsAnonymous());
    
    review = reviewRepository.save(review);
    
    // 🎁 Cộng 1 điểm vòng quay khi đánh giá
    userRepository.addPoints(user.getId(), 1);
    
    return toDto(review);
}
```

#### backupUserReviews() - Backup Reviews Khi Xóa User
```java
@Transactional
public void backupUserReviews(User user) {
    List<Review> reviews = reviewRepository.findByUserId(user.getId());
    
    for (Review review : reviews) {
        DeletedUserReviewBackup backup = DeletedUserReviewBackup.builder()
                .deletedUserId(user.getId())
                .deletedUsername(user.getUsername())
                .deletedUserFullname(user.getFullName())
                .originalReviewId(review.getId())
                .drinkId(review.getDrink().getId())
                .drinkName(review.getDrink().getName())
                .rating(review.getRating())
                .comment(review.getComment())
                .reviewCreatedAt(review.getCreatedAt())
                .build();
        
        deletedUserReviewBackupRepository.save(backup);
    }
}
```

**Giải thích:**
- **Order Validation**: Chỉ review được đơn hàng DONE của mình
- **One Review Per Item**: Không cho review trùng
- **Anonymous Option**: Có thể đánh giá ẩn danh
- **Reward Points**: Cộng điểm khi đánh giá
- **Backup on Delete**: Giữ lại reviews khi user xóa tài khoản

---

## 16. API REFERENCE

### 9.1 Manager Dashboard Overview
```
┌─────────────────────────────────────────────────────────────────────┐
│                      MANAGER DASHBOARD                              │
├─────────────────────────────────────────────────────────────────────┤
│  📊 METRICS CHÍNH:                                                 │
│  ├── 💰 Doanh thu hôm nay/tuần/tháng                               │
│  ├── 📦 Số đơn hàng mới/đang xử lý/hoàn thành                      │
│  ├── 👥 Số khách hàng mới/khách hàng quay lại                      │
│  ├── 🍵 Top sản phẩm bán chạy                                      │
│  ├── ⭐ Đánh giá trung bình                                        │
│  ├── 💬 Số tin nhắn chat chưa đọc                                  │
│  ├── 🎫 Voucher được sử dụng nhiều nhất                           │
│  └── 📈 Biểu đồ xu hướng doanh thu                                │
└─────────────────────────────────────────────────────────────────────┘
```

### 9.2 Luồng Lấy Dashboard Data
```
┌─────────────────────────────────────────────────────────────────────┐
│                    DASHBOARD DATA FLOW                              │
└─────────────────────────────────────────────────────────────────────┘

    ┌──────────────┐
    │ Manager App  │
    └──────┬───────┘
           │ GET /api/manager/dashboard?period=TODAY
           ▼
    ┌──────────────────────────────────────────────────────────────┐
    │                   ManagerController                          │
    │  @GetMapping("/dashboard")                                   │
    └──────────┬───────────────────────────────────────────────────┘
               ▼
    ┌──────────────────────────────────────────────────────────────┐
    │                    ManagerService                            │
    │                                                              │
    │  1. getCurrentManager() → Lấy manager hiện tại               │
    │  2. getManagedStoreIds() → Lấy danh sách stores              │
    │  3. Parallel queries:                                        │
    │     ├── getTotalRevenue(storeIds, period)                    │
    │     ├── getOrderCounts(storeIds, period)                     │
    │     ├── getCustomerStats(storeIds, period)                   │
    │     ├── getTopProducts(storeIds, period)                     │
    │     ├── getAverageRating(storeIds, period)                   │
    │     ├── getUnreadChatCount(storeIds)                         │
    │     ├── getVoucherUsage(storeIds, period)                    │
    │     └── getRevenueChart(storeIds, period)                    │
    │                                                              │
    │  4. Aggregate và format data                                 │
    │  5. Return DashboardSummaryDto                               │
    └──────────────────────────────────────────────────────────────┘
```

---

## 10. DỰ BÁO & PHÂN TÍCH

### 10.1 Revenue Forecasting
```
┌─────────────────────────────────────────────────────────────────────┐
│                      DỰ BÁO DOANH THU                              │
├─────────────────────────────────────────────────────────────────────┤
│  🎯 MỤC TIÊU:                                                      │
│  ├── Dự báo doanh thu 7 ngày tới                                   │
│  ├── Dự báo doanh thu tháng tới                                    │
│  ├── Phân tích xu hướng theo mùa                                   │
│  ├── Dự báo nhu cầu sản phẩm                                       │
│  └── Khuyến nghị chiến lược kinh doanh                             │
│                                                                     │
│  🧮 THUẬT TOÁN:                                                    │
│  ├── Linear Regression cho xu hướng cơ bản                         │
│  ├── Moving Average cho làm mượt dữ liệu                           │
│  ├── Seasonal Adjustment cho yếu tố mùa vụ                         │
│  ├── Weighted Average (gần đây có trọng số cao hơn)               │
│  └── External Factors (ngày lễ, sự kiện, thời tiết)               │
└─────────────────────────────────────────────────────────────────────┘
```

### 10.2 Peak Hours Analysis
```
┌─────────────────────────────────────────────────────────────────────┐
│                    PHÂN TÍCH GIỜ CAO ĐIỂM                          │
├─────────────────────────────────────────────────────────────────────┤
│  📊 PHÂN TÍCH THEO GIỜ:                                            │
│  ├── 06:00-09:00: Giờ sáng (cà phê, bánh mì)                      │
│  ├── 11:00-14:00: Giờ trưa (trà sữa, sinh tố)                     │
│  ├── 15:00-17:00: Giờ chiều (đồ uống giải khát)                   │
│  ├── 19:00-21:00: Giờ tối (đồ uống thư giãn)                      │
│  └── 21:00-23:00: Giờ khuya (trà sữa, dessert)                    │
│                                                                     │
│  🎯 KHUYẾN NGHỊ:                                                   │
│  ├── Tăng nhân sự trong giờ cao điểm                               │
│  ├── Chuẩn bị nguyên liệu trước giờ rush                          │
│  ├── Tạo voucher giờ thấp điểm để kích thích                      │
│  └── Optimize menu theo từng khung giờ                             │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 11. WHITELIST IP - BẢO MẬT NÂNG CAO

### 11.1 Tổng Quan Whitelist IP
```
┌─────────────────────────────────────────────────────────────────────┐
│                      WHITELIST IP SYSTEM                           │
├─────────────────────────────────────────────────────────────────────┤
│  🛡️ MỤC ĐÍCH:                                                     │
│  ├── Chỉ cho phép IP tin cậy truy cập chức năng Manager/Admin      │
│  ├── Bảo vệ khỏi các cuộc tấn công từ IP lạ                       │
│  ├── Kiểm soát truy cập từ các địa điểm cụ thể                     │
│  ├── Audit trail cho tất cả hoạt động quản trị                     │
│  └── Compliance với các yêu cầu bảo mật doanh nghiệp               │
│                                                                     │
│  ⚙️ CẤU HÌNH:                                                      │
│  ├── Bật/tắt tính năng whitelist                                   │
│  ├── Áp dụng cho ADMIN only hoặc cả MANAGER                        │
│  ├── Whitelist theo IP cụ thể hoặc IP range                        │
│  ├── Thời gian hiệu lực của từng IP                               │
│  └── Ghi chú mô tả cho từng IP entry                              │
└─────────────────────────────────────────────────────────────────────┘
```### 11.2 L
uồng Kiểm Tra Whitelist
```
┌─────────────────────────────────────────────────────────────────────┐
│                    WHITELIST CHECK FLOW                            │
└─────────────────────────────────────────────────────────────────────┘

    ┌──────────────┐
    │ Manager/Admin│
    │ Request      │
    └──────┬───────┘
           │ API Request với IP = 192.168.1.100
           ▼
    ┌──────────────────────────────────────────────────────────────┐
    │                 WhitelistIPFilter                            │
    │                                                              │
    │  1. Extract IP từ request                                    │
    │     - X-Forwarded-For header (nếu có proxy)                 │
    │     - X-Real-IP header                                       │
    │     - request.getRemoteAddr()                                │
    │                                                              │
    │  2. Kiểm tra user role từ JWT                                │
    │     - Nếu USER: bỏ qua whitelist check                      │
    │     - Nếu MANAGER/ADMIN: tiếp tục check                      │
    │                                                              │
    │  3. Kiểm tra tính năng có bật không                          │
    │     - Nếu tắt: cho phép tất cả                              │
    │     - Nếu bật: tiếp tục validate                            │
    │                                                              │
    │  4. whitelistedIPService.isIPWhitelisted(ip)                 │
    │     - Query database tìm IP                                  │
    │     - Kiểm tra thời gian hiệu lực                           │
    │     - Kiểm tra IP range (nếu có)                            │
    │                                                              │
    │  5. Kết quả:                                                 │
    │     ✅ IP trong whitelist → Cho phép tiếp tục               │
    │     ❌ IP không trong whitelist → HTTP 403 Forbidden        │
    └──────────────────────────────────────────────────────────────┘
```

---

## 12. BLOCKED IP - CHẶN IP ĐỘC HẠI

### 12.1 Tổng Quan Blocked IP System
```
┌─────────────────────────────────────────────────────────────────────┐
│                      BLOCKED IP SYSTEM                             │
├─────────────────────────────────────────────────────────────────────┤
│  🚫 MỤC ĐÍCH:                                                      │
│  ├── Tự động chặn IP có hành vi bất thường                         │
│  ├── Chặn thủ công IP độc hại được báo cáo                         │
│  ├── Ngăn chặn brute force attacks                                 │
│  ├── Bảo vệ khỏi DDoS và spam requests                            │
│  └── Blacklist IP từ các threat intelligence feeds                 │
│                                                                     │
│  🤖 TỰ ĐỘNG CHẶN KHI:                                             │
│  ├── 5+ lần đăng nhập thất bại trong 10 phút                      │
│  ├── 10+ requests trong 1 phút (rate limiting)                     │
│  ├── Cố gắng truy cập endpoint không tồn tại                       │
│  ├── Gửi payload độc hại (SQL injection, XSS)                     │
│  └── Hành vi bot được phát hiện                                   │
└─────────────────────────────────────────────────────────────────────┘
```

### 12.2 Luồng Auto-Block IP
```
┌─────────────────────────────────────────────────────────────────────┐
│                      AUTO-BLOCK IP FLOW                            │
└─────────────────────────────────────────────────────────────────────┘

    ┌──────────────┐
    │ Suspicious   │
    │ Activity     │
    │ Detected     │
    └──────┬───────┘
           │ VD: 5 lần login failed từ IP 192.168.1.200
           ▼
    ┌──────────────────────────────────────────────────────────────┐
    │                           ↓                                  │
    │  Trong quá trình xử lý request:                              │
    │  ├── UserMonitoringService phát hiện hành vi bất thường     │
    │  ├── VD: 5+ login failed, spam requests, brute force        │
    │  └── Gọi BlockedIPService.autoBlockIP()                     │
    │                           ↓                                  │
    │  ┌─────────────────────────────────────────────────────┐    │
    │  │              BlockedIPService                       │    │
    │  │                                                     │    │
    │  │  1. Kiểm tra IP đã bị block chưa                    │    │
    │  │  2. Tạo BlockedIP entity:                           │    │
    │  │     - ip_address = "192.168.1.200"                  │    │
    │  │     - reason = "Multiple failed login attempts"     │    │
    │  │     - block_type = "AUTO"                           │    │
    │  │     - blocked_until = now + 24 hours                │    │
    │  │                                                     │    │
    │  │  3. Lưu vào database                                │    │
    │  │  4. Gửi alert cho Admin                             │    │
    │  │  5. Log security event                              │    │
    │  └─────────────────────────────────────────────────────┘    │
    └──────────────────────────────────────────────────────────────┘
```

---

## 13. HỆ THỐNG GIÁM SÁT (MONITORING)

### 13.1 Tổng Quan Hệ Thống Monitoring
```
┌─────────────────────────────────────────────────────────────────────┐
│                    USER MONITORING SYSTEM                          │
├─────────────────────────────────────────────────────────────────────┤
│  🎯 MỤC ĐÍCH:                                                      │
│  ├── Theo dõi hành vi người dùng realtime                          │
│  ├── Phát hiện hoạt động bất thường và rủi ro                      │
│  ├── Tạo cảnh báo tự động cho Admin                               │
│  ├── Audit trail cho compliance                                    │
│  └── Phân tích xu hướng và pattern                                │
│                                                                     │
│  📊 METRICS THEO DÕI:                                             │
│  ├── Login/Logout activities                                       │
│  ├── Order creation và payment                                     │
│  ├── Failed authentication attempts                                │
│  ├── API usage patterns                                            │
│  ├── Suspicious behaviors                                          │
│  └── Risk score calculation                                        │
└─────────────────────────────────────────────────────────────────────┘
```

### 13.2 Risk Score Calculation
```
┌─────────────────────────────────────────────────────────────────────┐
│                      RISK SCORE ALGORITHM                          │
├─────────────────────────────────────────────────────────────────────┤
│  📈 TÍNH ĐIỂM RỦI RO (0-100):                                     │
│                                                                     │
│  Base Score = 0                                                     │
│                                                                     │
│  ➕ TĂNG ĐIỂM KHI:                                                 │
│  ├── Failed login: +10 points                                      │
│  ├── Multiple devices: +5 points                                   │
│  ├── Unusual hours: +3 points                                      │
│  ├── High order frequency: +5 points                               │
│  ├── Payment failures: +8 points                                   │
│  ├── Suspicious IP: +15 points                                     │
│  └── Reported by other users: +20 points                           │
│                                                                     │
│  ➖ GIẢM ĐIỂM KHI:                                                 │
│  ├── Successful activities: -2 points/day                          │
│  ├── Long-term user: -5 points                                     │
│  ├── Verified account: -10 points                                  │
│  └── Positive reviews: -3 points                                   │
│                                                                     │
│  🚨 CẢNH BÁO LEVELS:                                              │
│  ├── 0-30: LOW (Xanh lá)                                          │
│  ├── 31-60: MEDIUM (Vàng)                                         │
│  ├── 61-80: HIGH (Cam)                                            │
│  └── 81-100: CRITICAL (Đỏ) → Auto-block                           │
└─────────────────────────────────────────────────────────────────────┘
```##
# 13.3 Monitoring Dashboard
```
┌─────────────────────────────────────────────────────────────────────┐
│                      MONITORING DASHBOARD                           │
├─────────────────────────────────────────────────────────────────────┤
│  📊 TỔNG QUAN 24H:                                                │
│  ├── 👥 Total Users Active: 1,234                                 │
│  ├── 🚨 New Alerts: 5 (3 Medium, 2 High)                         │
│  ├── 🔒 Auto-blocked IPs: 12                                      │
│  ├── 📈 Risk Score Average: 23.5                                  │
│  └── 🎯 False Positive Rate: 2.1%                                 │
│                                                                     │
│  🔥 REALTIME ALERTS:                                              │
│  ├── [HIGH] User #1234 - Multiple failed payments                 │
│  ├── [MED] IP 192.168.1.100 - Unusual activity pattern           │
│  ├── [HIGH] User #5678 - Risk score exceeded 75                   │
│  └── [CRIT] IP 10.0.0.50 - Brute force detected                  │
│                                                                     │
│  📋 TOP RISK USERS:                                               │
│  ├── User #1111 (Score: 85) - Multiple violations                 │
│  ├── User #2222 (Score: 72) - Suspicious payment pattern         │
│  ├── User #3333 (Score: 68) - Unusual login times                │
│  └── User #4444 (Score: 65) - High order frequency               │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 14. LUỒNG LOGIC CHI TIẾT

### 14.1 Sequence Diagram - Cập Nhật Trạng Thái Đơn Hàng
```
Manager App    ManagerController    ManagerService    OrderService    Database
     │                │                   │               │            │
     │ PUT /orders/123/status?status=MAKING                │            │
     ├────────────────▶│                   │               │            │
     │                │ @PreAuthorize     │               │            │
     │                │ hasRole('MANAGER') │               │            │
     │                ├───────────────────▶│               │            │
     │                │                   │ getCurrentManager()        │
     │                │                   ├───────────────────────────▶│
     │                │                   │               │            │
     │                │                   │ getManagedStoreIds()       │
     │                │                   ├───────────────────────────▶│
     │                │                   │               │            │
     │                │                   │ validateStoreAccess()      │
     │                │                   │               │            │
     │                │                   │ updateOrderStatus()        │
     │                │                   ├───────────────▶│            │
     │                │                   │               │ save()     │
     │                │                   │               ├───────────▶│
     │                │                   │               │            │
     │                │                   │               │ notify()   │
     │                │                   │               │ WebSocket  │
     │                │                   │               │ Push       │
     │                │                   │               │ Email      │
     │                │ ResponseEntity    │               │            │
     │◀────────────────┤◀──────────────────┤◀──────────────┤            │
     │                │                   │               │            │
```

---

## 15. GIẢI THÍCH CODE

### 15.1 ManagerService - Core Business Logic

#### getCurrentManager()
```java
/**
 * Lấy thông tin Manager hiện tại từ SecurityContext
 * Sử dụng findByUsernameWithManagedStores để eager load stores
 */
private User getCurrentManager() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    return userRepository.findByUsernameWithManagedStores(username)
        .orElseThrow(() -> new ResourceNotFoundException("Manager not found: " + username));
}
```

**Giải thích:**
- Lấy username từ JWT token đã được authenticate
- Sử dụng custom query `findByUsernameWithManagedStores` để tránh N+1 problem
- Eager load `managedStores` để kiểm tra quyền truy cập store

#### getManagedStoreIds() - Phân Quyền Store
```java
/**
 * Logic phân quyền store quan trọng nhất trong hệ thống
 * ADMIN: return null (quản lý tất cả)
 * MANAGER có stores: return list store IDs
 * MANAGER không có stores: return empty list (không xem được gì)
 */
private List<Long> getManagedStoreIds(User manager) {
    // ADMIN luôn quản lý tất cả
    if (manager.getRole() == UserRole.ADMIN) {
        return null; // null = xem tất cả stores
    }
    
    // MANAGER phải có stores được gán
    Set<Store> managedStores = manager.getManagedStores();
    if (managedStores == null || managedStores.isEmpty()) {
        log.warn("Manager {} has no assigned stores", manager.getUsername());
        return new ArrayList<>(); // Empty list = không xem được gì
    }
    
    return managedStores.stream()
        .map(Store::getId)
        .collect(Collectors.toList());
}
```

**Giải thích:**
- **null**: ADMIN có quyền xem tất cả stores
- **Empty List**: Manager chưa được gán store nào → Dashboard empty
- **List<Long>**: Manager chỉ xem được stores trong list này

#### validateStoreAccess() - Kiểm Tra Quyền Truy Cập
```java
/**
 * Kiểm tra Manager có quyền truy cập store cụ thể không
 * Được gọi trước mọi thao tác liên quan đến store
 */
private void validateStoreAccess(User manager, Long storeId) {
    // ADMIN có quyền truy cập tất cả
    if (manager.getRole() == UserRole.ADMIN) {
        return;
    }
    
    // Sử dụng method canManageStore() từ User entity
    if (!manager.canManageStore(storeId)) {
        log.error("Manager {} cannot access store {}. Managed stores: {}", 
            manager.getUsername(), storeId, 
            manager.getManagedStores().stream().map(Store::getId).collect(Collectors.toList()));
        throw new BusinessException("Bạn không có quyền quản lý đơn hàng của chi nhánh này");
    }
}
```

#### updateOrderStatus() - Cập Nhật Trạng Thái Đơn Hàng
```java
@Transactional
public OrderDto updateOrderStatus(Long orderId, OrderStatus newStatus) {
    User manager = getCurrentManager();
    List<Long> storeIds = getManagedStoreIds(manager);
    
    log.info("=== UPDATE ORDER STATUS ===");
    log.info("Manager: {}, Role: {}", manager.getUsername(), manager.getRole());
    log.info("Order ID: {}, New Status: {}", orderId, newStatus);
    log.info("Managed Store IDs: {}", storeIds == null ? "ALL (ADMIN)" : storeIds);
    
    // Lấy order để kiểm tra quyền
    var order = orderRepository.findById(orderId)
        .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    
    // Nếu Manager không có store nào → không có quyền
    if (storeIds != null && storeIds.isEmpty()) {
        throw new BusinessException("Bạn chưa được gán quản lý chi nhánh nào");
    }
    
    // Kiểm tra quyền truy cập store (ADMIN bỏ qua)
    if (storeIds != null) {
        validateStoreAccess(manager, order.getStore().getId());
    }
    
    return orderService.updateOrderStatus(orderId, newStatus);
}
```

#### getDashboardSummary() - Dashboard Logic
```java
@Transactional(readOnly = true)
public DashboardSummaryDto getDashboardSummary() {
    User manager = getCurrentManager();
    List<Long> storeIds = getManagedStoreIds(manager);
    boolean isAdmin = manager.getRole() == UserRole.ADMIN;
    
    // Nếu Manager không có store nào → trả về empty dashboard
    if (storeIds != null && storeIds.isEmpty()) {
        log.warn("Manager {} has no assigned stores, returning empty dashboard", manager.getUsername());
        DashboardSummaryDto emptyDashboard = new DashboardSummaryDto();
        emptyDashboard.setTotalRevenue(BigDecimal.ZERO);
        // ... set all fields to 0/empty
        return emptyDashboard;
    }
    
    // Logic tính toán metrics khác nhau cho ADMIN vs MANAGER
    if (storeIds == null) {
        // ADMIN - xem tất cả + backup revenue từ user đã xóa
        totalRevenue = entityManager.createQuery(totalRevenueQuery, BigDecimal.class)
            .setParameter("status", OrderStatus.DONE)
            .getSingleResult();
        
        // ADMIN mới cộng backup revenue
        BigDecimal backupRevenue = getBackupTotalRevenue();
        totalRevenue = totalRevenue.add(backupRevenue);
    } else {
        // Manager - chỉ xem stores được gán (KHÔNG cộng backup)
        totalRevenue = entityManager.createQuery(revenueQuery, BigDecimal.class)
            .setParameter("status", OrderStatus.DONE)
            .setParameter("storeIds", storeIds)
            .getSingleResult();
    }
    
    return summary;
}
```

**Key Points:**
- **ADMIN**: Xem tất cả + backup revenue từ user đã xóa
- **MANAGER**: Chỉ xem stores được gán, không có backup
- **Empty Dashboard**: Manager chưa được gán store nào

### 15.2 WhitelistIPFilter - Security Logic

#### doFilterInternal() - Main Filter Logic
```java
@Override
protected void doFilterInternal(HttpServletRequest request, 
                               HttpServletResponse response, 
                               FilterChain filterChain) throws ServletException, IOException {
    
    // 1. Kiểm tra tính năng có bật không
    if (!whitelistCheckEnabled) {
        filterChain.doFilter(request, response);
        return;
    }

    // 2. Lấy authentication từ SecurityContext (sau JwtAuthFilter)
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    // 3. Chỉ áp dụng cho ADMIN/MANAGER, USER thoải mái
    if (authentication == null || !isAdminOrManager(authentication)) {
        filterChain.doFilter(request, response);
        return;
    }

    // 4. Extract IP từ headers
    String clientIP = getClientIP(request);
    String normalizedIP = normalizeIP(clientIP);

    // 5. Kiểm tra IP có trong whitelist database
    if (!whitelistedIPService.isIPWhitelisted(normalizedIP)) {
        log.warn("🚫 WHITELIST CHECK FAILED | User: {} | Role: {} | IP: {} | Path: {}",
                authentication.getName(), getRole(authentication), clientIP, request.getRequestURI());

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"success\":false,\"error\":\"IP_NOT_WHITELISTED\"," +
                "\"message\":\"IP của bạn chưa được cấp quyền truy cập Admin/Manager\"," +
                "\"yourIP\":\"" + clientIP + "\"}"
        );
        return;
    }

    // 6. Cho phép tiếp tục
    filterChain.doFilter(request, response);
}
```

#### getClientIP() - Extract Real IP
```java
/**
 * Lấy IP thực của client, xử lý proxy headers
 */
private String getClientIP(HttpServletRequest request) {
    String[] headers = {
        "X-Forwarded-For",    // Load balancer/proxy
        "X-Real-IP",          // Nginx proxy
        "Proxy-Client-IP",    // Apache proxy
        "WL-Proxy-Client-IP", // WebLogic
        "HTTP_X_FORWARDED_FOR",
        "HTTP_CLIENT_IP"
    };

    for (String header : headers) {
        String ip = request.getHeader(header);
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For có thể chứa nhiều IP: "client, proxy1, proxy2"
            return ip.split(",")[0].trim(); // Lấy IP đầu tiên (client)
        }
    }

    return request.getRemoteAddr(); // Fallback to direct connection IP
}
```

### 15.3 BlockedIPFilter - IP Blocking Logic

#### doFilterInternal() - Block Check
```java
@Override
protected void doFilterInternal(HttpServletRequest request,
                               HttpServletResponse response,
                               FilterChain filterChain) throws ServletException, IOException {

    String clientIP = getClientIP(request);
    String normalizedIP = normalizeIP(clientIP);
    
    // Check nếu IP bị block (kiểm tra cả IP gốc và normalized)
    boolean isBlocked = blockedIPService.checkAndIncrementIfBlocked(clientIP);
    
    // Nếu IP gốc không bị block, kiểm tra IP normalized (IPv6 → IPv4)
    if (!isBlocked && !clientIP.equals(normalizedIP)) {
        isBlocked = blockedIPService.checkAndIncrementIfBlocked(normalizedIP);
    }
    
    if (isBlocked) {
        log.warn("🚫 BLOCKED REQUEST | IP: {} | Path: {} | Method: {}", 
                clientIP, request.getRequestURI(), request.getMethod());

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
            "{\"success\":false,\"error\":\"IP_BLOCKED\"," +
            "\"message\":\"IP của bạn đã bị chặn do vi phạm chính sách sử dụng\"," +
            "\"blockedIP\":\"" + clientIP + "\"}"
        );
        return;
    }
    
    filterChain.doFilter(request, response);
}
```

### 15.4 WhitelistedIPService - Database Operations

#### isIPWhitelisted() - Check Whitelist
```java
/**
 * Kiểm tra IP có trong whitelist không
 * Sử dụng custom query để check active whitelist
 */
public boolean isIPWhitelisted(String ipAddress) {
    if (ipAddress == null) return false;
    
    String normalizedIP = normalizeIP(ipAddress);
    return whitelistedIPRepository.isIPWhitelisted(normalizedIP);
}
```

#### addToWhitelist() - Add IP
```java
@Transactional
public WhitelistedIP addToWhitelist(String ipAddress, String description, Long addedById) {
    log.info("🔓 Adding IP to whitelist: {} by user: {}", ipAddress, addedById);

    String normalizedIP = normalizeIP(ipAddress.trim());

    // Kiểm tra đã tồn tại chưa
    if (whitelistedIPRepository.existsByIpAddress(normalizedIP)) {
        // Nếu inactive, kích hoạt lại
        List<WhitelistedIP> all = whitelistedIPRepository.findAll();
        for (WhitelistedIP w : all) {
            if (w.getIpAddress().equals(normalizedIP)) {
                w.setIsActive(true);
                w.setDescription(description);
                w.setAddedById(addedById);
                return whitelistedIPRepository.save(w);
            }
        }
    }

    // Tạo mới
    WhitelistedIP whitelistedIP = WhitelistedIP.builder()
            .ipAddress(normalizedIP)
            .description(description)
            .addedById(addedById)
            .isActive(true)
            .build();

    return whitelistedIPRepository.save(whitelistedIP);
}
```

### 15.5 BlockedIPService - Auto Block Logic

#### blockIP() - Manual/Auto Block
```java
@Transactional
public BlockedIP blockIP(BlockIPRequest request, Long blockedById) {
    log.info("🚫 Blocking IP: {} by user: {} | Type: {} | Duration: {}h", 
        request.getIpAddress(), blockedById, request.getBlockType(), request.getDurationHours());
    
    String ipAddress = request.getIpAddress().trim();
    
    // Kiểm tra IP đã bị block chưa
    Optional<BlockedIP> existing = blockedIPRepository.findActiveBlockedIP(ipAddress, Instant.now());
    if (existing.isPresent()) {
        throw new RuntimeException("IP này đã bị chặn");
    }
    
    // Validate block type và tính thời gian hết hạn
    BlockedIP.BlockType blockType = BlockedIP.BlockType.valueOf(request.getBlockType());
    
    Instant blockedUntil = null;
    if (blockType == BlockedIP.BlockType.TEMPORARY) {
        if (request.getDurationHours() <= 0) {
            throw new RuntimeException("Thời gian block tạm thời phải lớn hơn 0");
        }
        blockedUntil = Instant.now().plus(request.getDurationHours(), ChronoUnit.HOURS);
    }
    
    BlockedIP blockedIP = BlockedIP.builder()
            .ipAddress(ipAddress)
            .blockType(blockType)
            .reason(request.getReason())
            .blockedById(blockedById)
            .blockedUntil(blockedUntil)
            .isActive(true)
            .relatedUserId(request.getRelatedUserId())
            .alertId(request.getAlertId())
            .blockedRequestsCount(0L)
            .build();
    
    return blockedIPRepository.save(blockedIP);
}
```

#### cleanupExpiredBlocks() - Auto Cleanup
```java
/**
 * Tự động gỡ chặn các IP hết hạn (chạy mỗi phút)
 */
@Scheduled(fixedRate = 60000)
@Transactional
public void cleanupExpiredBlocks() {
    List<BlockedIP> expired = blockedIPRepository.findExpiredBlocks(Instant.now());
    if (!expired.isEmpty()) {
        log.info("🔄 Auto-unblocking {} expired IPs", expired.size());
        expired.forEach(ip -> {
            ip.setIsActive(false);
            ip.setUnblockedAt(Instant.now());
            ip.setUnblockReason("Tự động gỡ - Hết hạn");
        });
        blockedIPRepository.saveAll(expired);
    }
}
```

### 15.6 Security Filter Chain Order

```java
// Thứ tự chạy filters (từ cao đến thấp priority)
@Order(Ordered.HIGHEST_PRECEDENCE - 1)     // BlockedIPFilter (chạy đầu tiên)
@Order(Ordered.HIGHEST_PRECEDENCE)         // JwtAuthenticationFilter
@Order(Ordered.LOWEST_PRECEDENCE - 10)     // WhitelistIPFilter (chạy cuối)
```

**Luồng Security:**
1. **BlockedIPFilter**: Chặn IP bị block ngay lập tức
2. **JwtAuthenticationFilter**: Xác thực JWT và set SecurityContext
3. **WhitelistIPFilter**: Kiểm tra IP whitelist cho ADMIN/MANAGER

**Tại sao thứ tự này quan trọng:**
- BlockedIP phải chạy trước để chặn IP độc hại
- JWT phải chạy trước Whitelist để biết user role
- Whitelist chạy cuối để kiểm tra quyền truy cập Admin/Manager---

##
 16. API REFERENCE

### 16.1 Manager APIs

| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| GET | `/api/manager/dashboard` | Dashboard tổng quan | MANAGER, ADMIN |
| GET | `/api/manager/orders` | Danh sách đơn hàng | MANAGER, ADMIN |
| PUT | `/api/manager/orders/{id}/status` | Cập nhật trạng thái đơn hàng | MANAGER, ADMIN |
| GET | `/api/manager/users` | Danh sách người dùng | MANAGER, ADMIN |
| PUT | `/api/manager/users/{id}/block` | Khóa/mở khóa user | MANAGER, ADMIN |
| PUT | `/api/manager/users/{id}/promote` | Nâng cấp User → Manager | ADMIN |
| POST | `/api/manager/users/{id}/stores/{storeId}` | Gán store cho Manager | ADMIN |

### 16.2 Product Management APIs

| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| GET | `/api/drinks` | Danh sách sản phẩm | MANAGER, ADMIN |
| POST | `/api/drinks` | Tạo sản phẩm mới | MANAGER, ADMIN |
| PUT | `/api/drinks/{id}` | Cập nhật sản phẩm | MANAGER, ADMIN |
| DELETE | `/api/drinks/{id}` | Xóa sản phẩm | MANAGER, ADMIN |
| POST | `/api/drinks/{id}/image` | Upload hình ảnh | MANAGER, ADMIN |
| GET | `/api/categories` | Danh sách danh mục | ALL |
| POST | `/api/categories` | Tạo danh mục | ADMIN |

### 16.3 Voucher Management APIs

| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| GET | `/api/promotions` | Danh sách voucher | MANAGER, ADMIN |
| POST | `/api/promotions` | Tạo voucher mới | MANAGER, ADMIN |
| PUT | `/api/promotions/{id}` | Cập nhật voucher | MANAGER, ADMIN |
| DELETE | `/api/promotions/{id}` | Xóa voucher | MANAGER, ADMIN |
| GET | `/api/promotions/{id}/usage` | Thống kê sử dụng voucher | MANAGER, ADMIN |
| POST | `/api/promotions/validate` | Validate mã voucher | USER |

### 16.4 Live Chat APIs

| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| GET | `/api/chat/conversations` | Danh sách cuộc hội thoại | MANAGER, ADMIN |
| POST | `/api/chat/conversations` | Tạo cuộc hội thoại mới | USER |
| GET | `/api/chat/conversations/{id}/messages` | Lịch sử tin nhắn | ALL |
| POST | `/api/chat/messages` | Gửi tin nhắn | ALL |
| PUT | `/api/chat/conversations/{id}/close` | Đóng cuộc hội thoại | MANAGER, ADMIN |

### 16.5 Security APIs

| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| GET | `/api/whitelist-ips` | Danh sách Whitelist IP | ADMIN |
| POST | `/api/whitelist-ips` | Thêm IP vào whitelist | ADMIN |
| DELETE | `/api/whitelist-ips/{id}` | Xóa IP khỏi whitelist | ADMIN |
| GET | `/api/blocked-ips` | Danh sách Blocked IP | ADMIN |
| POST | `/api/blocked-ips` | Chặn IP thủ công | ADMIN |
| DELETE | `/api/blocked-ips/{id}` | Bỏ chặn IP | ADMIN |

### 16.6 Monitoring APIs

| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| GET | `/api/monitoring/dashboard` | Dashboard giám sát | ADMIN |
| GET | `/api/monitoring/activities` | Log hoạt động | ADMIN |
| GET | `/api/monitoring/alerts` | Danh sách cảnh báo | ADMIN |
| PUT | `/api/monitoring/alerts/{id}/handle` | Xử lý cảnh báo | ADMIN |
| GET | `/api/monitoring/risk-scores` | Điểm rủi ro users | ADMIN |

---

## 17. CÂU HỎI PHỎNG VẤN

### 17.1 Câu Hỏi Cơ Bản

**Q1: Giải thích luồng cập nhật trạng thái đơn hàng trong hệ thống?**

**A1:** Luồng bao gồm các bước:
1. Manager gửi PUT request với JWT token
2. Security filters kiểm tra: BlockedIP → JWT Auth → WhitelistIP
3. ManagerController validate input và gọi ManagerService
4. ManagerService kiểm tra quyền truy cập store của Manager
5. OrderService validate transition và cập nhật database
6. Gửi notifications qua WebSocket, Push, Email

**Q2: Phân biệt quyền hạn giữa ADMIN và MANAGER?**

**A2:**
- **ADMIN**: Quản lý toàn hệ thống, tất cả stores, tạo/xóa Manager, quản lý IP security
- **MANAGER**: Chỉ quản lý stores được gán, không thể khóa Manager khác, không có quyền security

**Q3: Whitelist IP hoạt động như thế nào?**

**A3:** 
- Filter kiểm tra role user từ JWT
- Chỉ áp dụng cho MANAGER/ADMIN
- Extract IP từ headers (X-Forwarded-For, X-Real-IP)
- Query database kiểm tra IP có trong whitelist
- Trả về 403 nếu IP không được phép

### 17.2 Câu Hỏi Nâng Cao

**Q4: Làm thế nào để tối ưu performance cho Dashboard API khi có nhiều stores?**

**A4:**
- Sử dụng parallel queries với CompletableFuture
- Cache kết quả với Redis (TTL 5-10 phút)
- Database indexing trên store_id, created_at
- Pagination cho large datasets
- Aggregate queries thay vì multiple single queries

**Q5: Giải thích thuật toán Risk Score calculation?**

**A5:**
- Base score = 0, tăng/giảm theo activities
- Failed login +10, successful activities -2/day
- Weighted recent activities (7 ngày gần đây có trọng số cao)
- External factors: IP reputation, device fingerprint
- Auto-block khi score > 80, alert khi > 60

**Q6: Xử lý race condition khi multiple Managers cập nhật cùng 1 đơn hàng?**

**A6:**
- Database-level locking với SELECT FOR UPDATE
- Optimistic locking với version field
- Retry mechanism với exponential backoff
- Event sourcing để track all state changes
- WebSocket broadcast để sync realtime

**Q7: Scaling strategy cho Live Chat system?**

**A7:**
- WebSocket clustering với Redis pub/sub
- Message queue (RabbitMQ) cho reliable delivery
- Database sharding theo conversation_id
- CDN cho file/image sharing
- Auto-scaling based on concurrent connections