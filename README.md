# 🍵 UTE TEA - Android App

<div align="center">

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white)
![API](https://img.shields.io/badge/Min%20SDK-24-brightgreen?style=for-the-badge)
![Target](https://img.shields.io/badge/Target%20SDK-34-blue?style=for-the-badge)
![MVVM](https://img.shields.io/badge/Architecture-MVVM-orange?style=for-the-badge)

**Ứng dụng đặt trà sữa thông minh với AI, Voice Order và Group Order**

[Tính năng](#-tính-năng) • [Cài đặt](#-cài-đặt) • [Kiến trúc](#-kiến-trúc) • [Screenshots](#-screenshots)

</div>

---

## 📱 Giới thiệu

**UTE Tea** là ứng dụng Android native được phát triển bằng **Kotlin** theo mô hình **MVVM**, mang đến trải nghiệm đặt trà sữa hoàn toàn mới với các tính năng AI tiên tiến.

### 🎯 Điểm nổi bật

- 🎤 **Voice Order** - Đặt hàng bằng giọng nói với AI Speech Recognition
- 🔮 **AI Predictive Order** - Gợi ý món dựa trên thời tiết và lịch sử
- 👥 **Group Order** - Đặt hàng nhóm với real-time collaboration
- 🎡 **Spin Wheel & Loyalty** - Hệ thống tích điểm 4 tier với vòng quay
- 💬 **Live Chat** - Hỗ trợ khách hàng real-time
- 🌸 **Seasonal Effects** - Hiệu ứng theo mùa (tuyết rơi, hoa đào, lá vàng)
- 🔐 **Biometric Login** - Đăng nhập vân tay với mã hóa KeyStore
- 🛡️ **User Monitoring** - Hệ thống giám sát cho Admin

---

## ✨ Tính năng

### 👤 Dành cho Khách hàng

#### 🏠 Trang chủ thông minh
- **Banner Carousel** với auto-scroll
- **Best Seller & For You** product carousels
- **Quick Actions**: Voice Order, Chatbot, Spin Wheel, Group Order
- **Smart Suggestion Card** - AI gợi ý món dựa trên thời tiết và lịch sử
- **Weather Integration** - Hiển thị thời tiết real-time
- **Seasonal Effects** - Hiệu ứng theo mùa

#### 🎤 Voice Order System
- **Speech-to-Text** cho đặt hàng bằng giọng nói
- **Natural Language Processing** cho tên món
- **Voice Search** trong menu
- **Vietnamese Accent Removal** trong search

#### 🔮 AI Predictive Order
- **AI Prediction** dựa trên lịch sử đặt hàng
- **Weather-based Recommendations**
- **Time-based Suggestions**
- **One-click Add to Cart**

#### 🛍️ Menu & Đặt hàng
- **Grid Layout** hiển thị toàn bộ thực đơn
- **Category Filter** với horizontal scroll
- **Search với Voice Search** support
- **Real-time Price Calculation**
- **Smart Cart** với select/unselect items
- **Combined Voucher System** (promotion + spin rewards)

#### 👥 Group Order System
- **Tạo phiên đặt hàng nhóm** với invite code 6 ký tự
- **Real-time Collaboration** với WebSocket
- **Group Chat Integration**
- **Host Controls** (lock/unlock, checkout)

#### 🎡 Loyalty & Spin Wheel
- **4-Tier Membership** (Bronze/Silver/Gold/Diamond)
- **Spin Wheel Animation** với probability system
- **Points Accumulation** và tier benefits
- **Auto Tier Upgrade** system

#### 💬 Live Chat Support
- **Real-time Messaging** với WebSocket
- **Smart Queue Management**
- **Auto Response System** cho FAQ

#### 💳 Multi-Payment System
- **COD** (Cash on Delivery)
- **VNPay** online payment
- **VietQR** payment
- **OTP Verification** trước khi đặt hàng

#### 📦 Quản lý đơn hàng
- **Smart Sorting Logic**:
  - Đơn đang xử lý: Sắp xếp theo thời gian đặt (cũ → mới)
  - Đơn hoàn thành: Sắp xếp theo thời gian (mới → cũ)
- **Reorder System** - Đặt lại đơn hàng với smart suggestions

#### 👤 Profile & Security
- **Biometric Login** (vân tay) với KeyStore encryption
- **Avatar Upload** với image compression
- **Session Management** với encrypted SharedPreferences

### 👨‍💼 Dành cho Manager

#### 📊 Dashboard & Analytics
- **Thống kê doanh thu** theo ngày/tháng
- **Order Analytics** với biểu đồ
- **Revenue Forecasting**
- **Peak Hours Analysis**

#### 🍹 Quản lý sản phẩm
- **CRUD Operations** cho đồ uống
- **Image Upload** với Cloudinary
- **Size & Topping Management**

#### 📋 Quản lý đơn hàng
- **Real-time Order Updates**
- **Status Management** (PENDING → MAKING → SHIPPING/READY → DONE)
- **Order Filtering** và search

#### 🎁 Promotion Management
- **Voucher Creation** với business rules
- **Usage Tracking** và analytics

#### 💬 Live Chat Management
- **Conversation Queue**
- **Auto Assignment** theo store
- **Response Templates**

### 🛡️ Dành cho Admin

#### 🔍 User Monitoring System
- **Real-time User Behavior Tracking**
- **Risk Score Calculation** algorithm
- **Automated Alert System**
- **Activity Logging** với audit trails
- **IP Blocking Management**

#### 📊 Monitoring Dashboard
- **User Activity Overview**
- **Risk Assessment Reports**
- **Alert Management**
- **System Health Monitoring**

---

## 🏗️ Kiến trúc & Công nghệ

### 🎯 MVVM Architecture

```
┌─────────────────────────────────────────┐
│         Activities/Fragments            │  ← UI Layer
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│         Network Layer                   │  ← API Layer
│  (RetrofitClient, ApiService, ...)     │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│         Models                          │  ← Data Layer
│  (User, Drink, Order, Cart, ...)       │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│         Adapters                        │  ← RecyclerView
│  (ProductAdapter, CartAdapter, ...)    │
└─────────────────────────────────────────┘
```

### 🚀 Tech Stack

#### 📱 Core Technologies
```
📱 Core
├── Kotlin (100%) - Modern Android development
├── Android SDK 24-34 - Wide device compatibility
├── Material Design 3 - Modern UI components
├── ViewBinding - Type-safe view references
└── MVVM Architecture - Separation of concerns

🌐 Networking
├── Retrofit 2 - Type-safe HTTP client
├── OkHttp 3 - HTTP/HTTP2 client
├── Gson - JSON serialization
├── WebSocket - Real-time communication
└── Glide - Image loading & caching

🔒 Security
├── JWT Authentication - Stateless auth
├── Biometric Authentication - Fingerprint login
├── KeyStore Encryption - Secure key storage
├── EncryptedSharedPreferences - Secure storage
└── Input Validation - Data sanitization

🎯 Advanced Features
├── Speech Recognition - Voice commands
├── WebSocket - Real-time updates
├── Push Notifications - OneSignal
├── Location Services - GPS integration
├── Weather API - Weather data
└── AI Predictions - Smart recommendations
```

### 📂 Cấu trúc thư mục

```
app/src/main/java/com/example/doan/
│
├── 📂 Activities/                    # Các màn hình chính
│   ├── MainActivity.kt              # Bottom navigation container
│   ├── LoginActivity.kt             # Login với biometric support
│   ├── CartActivity.kt              # Shopping cart với voucher system
│   ├── ProductDetailActivity.kt     # Product detail với reviews
│   ├── UserMonitoringActivity.kt    # Admin monitoring dashboard
│   ├── LiveChatActivity.kt          # Real-time chat interface
│   ├── GroupOrderActivity.kt        # Group ordering system
│   └── SpinWheelActivity.kt         # Loyalty spin wheel
│
├── 📂 Fragments/                    # UI Fragments
│   ├── HomeFragment.kt              # Home với AI suggestions
│   ├── MenuFragment.kt              # Menu với voice search
│   ├── OrderFragment.kt             # Order history với smart sorting
│   ├── AccountFragment.kt           # Profile management
│   └── Manager/                     # Manager-specific fragments
│
├── 📂 Adapters/                     # RecyclerView Adapters
│   ├── ProductAdapter.kt            # Product grid display
│   ├── CartAdapter.kt               # Cart items với selection
│   ├── OrderAdapter.kt              # Order history
│   ├── CategoryAdapter.kt           # Category horizontal scroll
│   └── ...
│
├── 📂 Models/                       # Data Classes
│   ├── User.kt, Order.kt, Drink.kt # Core models
│   ├── GroupOrderModels.kt          # Group order data
│   ├── MonitoringModels.kt          # Admin monitoring
│   └── ApiResponse.kt               # API wrappers
│
├── 📂 Network/                      # API & Networking
│   ├── ApiService.kt                # 100+ API endpoints
│   ├── RetrofitClient.kt            # HTTP client configuration
│   ├── AuthInterceptor.kt           # JWT token management
│   └── WebSocketManagers/           # Real-time communication
│
└── 📂 Utils/                        # Utility Classes
    ├── SessionManager.kt            # Encrypted session management
    ├── VoiceOrderHelper.kt          # Voice commands
    ├── PredictiveOrderHelper.kt     # AI suggestions
    └── SeasonalEffectManager.kt     # Seasonal effects
```

---

## 🚀 Tính năng độc đáo

### 🎤 Voice Order System
```kotlin
class VoiceOrderHelper {
    fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, 
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
        }
        speechRecognizer.startListening(intent)
    }
}
```

### 🔮 AI Predictive Order
```kotlin
class PredictiveOrderHelper {
    fun generateSmartSuggestion(user: User): DrinkSuggestion? {
        val orderHistory = getOrderHistory(user)
        val currentWeather = weatherHelper.getCurrentWeather()
        val timeOfDay = getCurrentTimeOfDay()
        
        // Calculate confidence score
        val suggestion = calculateBestSuggestion(
            frequentDrinks, weatherPreference, timePreference
        )
        
        return if (suggestion.confidenceScore > 0.7) {
            DrinkSuggestion(drink = suggestion.drink, reason = generateReason(suggestion))
        } else null
    }
}
```

### 🌸 Seasonal Effects System
```kotlin
class SeasonalEffectManager {
    fun getCurrentSeasonalEffect(): SeasonalEffect? {
        return when (LocalDate.now().monthValue) {
            1, 2 -> SeasonalEffect.SAKURA_PETALS      // Tết
            12 -> SeasonalEffect.SNOWFALL             // Giáng sinh
            6, 7, 8 -> SeasonalEffect.SUNSHINE_BUBBLES // Mùa hè
            9, 10, 11 -> SeasonalEffect.FALLING_LEAVES // Mùa thu
            else -> null
        }
    }
}
```

---

## 🚀 Cài đặt

### Yêu cầu hệ thống
- **Android Studio**: Arctic Fox trở lên
- **JDK**: 11 trở lên
- **Android SDK**: 24-34
- **Gradle**: 8.0+
- **Kotlin**: 1.9.22

### Bước cài đặt

```bash
# 1. Clone project
git clone <repository-url>
cd doanltdd

# 2. Cấu hình API URL
# Mở RetrofitClient.kt và cập nhật BASE_URL
private const val BASE_URL = "http://your-api-url:8080/api/"

# 3. Build project
./gradlew clean
./gradlew assembleDebug

# 4. Install to device
./gradlew installDebug
```

### Cấu hình API

**Emulator:**
```kotlin
private const val BASE_URL = "http://10.0.2.2:8080/api/"
```

**Device thật:**
```kotlin
private const val BASE_URL = "http://YOUR_IP:8080/api/"
```

---

## 📸 Screenshots

### User Interface

| Trang chủ | Menu | Giỏ hàng |
|-----------|------|----------|
| ![Home](screenshots/home.png) | ![Menu](screenshots/menu.png) | ![Cart](screenshots/cart.png) |

| Voice Order | Group Order | Spin Wheel |
|-------------|-------------|------------|
| ![Voice](screenshots/voice.png) | ![Group](screenshots/group.png) | ![Spin](screenshots/spin.png) |

### Manager Interface

| Dashboard | Quản lý đơn hàng | Analytics |
|-----------|------------------|-----------|
| ![Dashboard](screenshots/dashboard.png) | ![Orders](screenshots/orders.png) | ![Analytics](screenshots/analytics.png) |

---

## 🔧 API Integration

### Authentication
```kotlin
// Login
apiService.login(LoginRequest(username, password))

// Auto token refresh
authInterceptor.refreshToken()
```

### Real-time Features
```kotlin
// WebSocket connection
orderWebSocket.connect(orderId)
groupChatWebSocket.joinRoom(groupOrderId)
```

### Voice & AI
```kotlin
// Voice order
voiceOrderHelper.startVoiceRecognition()

// AI prediction
predictiveOrderHelper.generateSmartSuggestion(user)
```

---

## 🐛 Troubleshooting

### Lỗi kết nối API
```
❌ Unable to resolve host
```
**Giải pháp:** Kiểm tra BASE_URL trong RetrofitClient.kt

### Lỗi build Gradle
```bash
./gradlew clean
./gradlew build --refresh-dependencies
```

### Voice Order không hoạt động
- Kiểm tra microphone permission
- Đảm bảo device có Google Speech Services

---

## 📄 License

MIT License - Đồ án Lập trình Di động UTE

---

## 👥 Team

**UTE Tea Development Team**
- Android: Kotlin + MVVM Architecture
- Backend: Spring Boot API
- Database: MySQL 8.0

---

<div align="center">

**🍵 Made with ❤️ for UTE Students 🍵**

⭐ Star this repo if you like it!

</div>