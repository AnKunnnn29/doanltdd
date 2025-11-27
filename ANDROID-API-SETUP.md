# 📱 UTE TEA ANDROID - API SETUP GUIDE

## 🎯 Tổng quan

Android app đã được cấu hình để kết nối với Spring Boot backend API. Tài liệu này hướng dẫn cách sử dụng và test.

---

## ⚙️ Cấu hình

### 1. Base URL Configuration

File: `Network/RetrofitClient.java`

```java
// Để test trên emulator
private static final String BASE_URL = "http://10.0.2.2:8080/api/";

// Để test trên thiết bị thật (thay YOUR_IP bằng IP máy tính)
private static final String BASE_URL = "http://192.168.1.100:8080/api/";
```

**Cách tìm IP máy tính:**
- Windows: Mở CMD, gõ `ipconfig`, tìm IPv4 Address
- Mac/Linux: Mở Terminal, gõ `ifconfig`, tìm inet

---

## 🔧 Các thay đổi đã thực hiện

### 1. Models đã cập nhật

#### ✅ ApiResponse.java (MỚI)
Wrapper class cho tất cả API responses từ backend:
```java
{
  "success": true,
  "message": "Success",
  "data": { ... }
}
```

#### ✅ LoginRequest.java
- Đổi `username` → `usernameOrPhone` (có thể dùng username hoặc số điện thoại)

#### ✅ LoginResponse.java
Thêm các fields:
- `fullName`
- `phone`
- `role` (USER/MANAGER)
- `memberTier` (BRONZE/SILVER/GOLD)
- `token` (JWT token)

#### ✅ RegisterRequest.java
Thêm các fields:
- `phone` (bắt buộc)
- `fullName`
- `address`

#### ✅ RegisterResponse.java
Trả về thông tin user đã đăng ký

#### ✅ Drink.java (MỚI)
Model cho món nước với:
- Thông tin cơ bản (id, name, description, imageUrl, basePrice)
- Category info
- Sizes (M, L, Jumbo)
- Toppings

#### ✅ DrinkSize.java (MỚI)
Model cho size món:
- sizeName (M, L, Jumbo)
- extraPrice (giá thêm)

#### ✅ DrinkTopping.java (MỚI)
Model cho topping:
- toppingName
- price

#### ✅ Category.java (MỚI)
Model cho loại đồ uống:
- Milk Tea, Fruit Tea, Macchiato, Special

#### ✅ Store.java
Cập nhật fields:
- `storeName`
- `latitude`, `longitude`
- `openTime`, `closeTime`
- `phone`

### 2. Network Layer

#### ✅ ApiService.java
Đã thêm các endpoints:

**Authentication:**
- `POST /auth/login` - Đăng nhập
- `POST /auth/register` - Đăng ký
- `GET /auth/health` - Health check

**Categories:**
- `GET /categories` - Lấy danh sách categories
- `GET /categories/{id}` - Chi tiết category

**Drinks:**
- `GET /drinks` - Lấy tất cả món
- `GET /drinks/{id}` - Chi tiết món
- `GET /drinks/search?keyword=` - Tìm kiếm món

**Stores:**
- `GET /stores` - Lấy danh sách cửa hàng
- `GET /stores/{id}` - Chi tiết cửa hàng

**Orders:**
- `GET /orders/user/{userId}` - Lịch sử đơn hàng
- `GET /orders/user/{userId}/current` - Đơn hiện tại
- `GET /orders/{orderId}` - Chi tiết đơn

#### ✅ RetrofitClient.java
- Thêm `AuthInterceptor` để tự động thêm JWT token vào header
- Thêm logging để debug
- Timeout 30 giây

#### ✅ AuthInterceptor.java (MỚI)
Tự động thêm JWT token vào mọi request:
```
Authorization: Bearer <token>
```

### 3. Utils

#### ✅ SessionManager.java (MỚI)
Quản lý session và lưu thông tin user:
- `saveLoginSession()` - Lưu thông tin sau khi login
- `logout()` - Xóa session
- `isLoggedIn()` - Kiểm tra đã login chưa
- `getToken()` - Lấy JWT token
- `getUserId()`, `getUsername()`, etc. - Lấy thông tin user

---

## 📝 Cách sử dụng

### 1. Login

```java
// Trong LoginActivity.java
SessionManager sessionManager = new SessionManager(this);
ApiService apiService = RetrofitClient.getInstance(this).getApiService();

LoginRequest request = new LoginRequest("ute_student_01", "123456");

apiService.login(request).enqueue(new Callback<ApiResponse<LoginResponse>>() {
    @Override
    public void onResponse(Call<ApiResponse<LoginResponse>> call, Response<ApiResponse<LoginResponse>> response) {
        if (response.isSuccessful() && response.body() != null) {
            ApiResponse<LoginResponse> apiResponse = response.body();
            
            if (apiResponse.isSuccess()) {
                LoginResponse data = apiResponse.getData();
                
                // Lưu session
                sessionManager.saveLoginSession(
                    data.getUserId(),
                    data.getUsername(),
                    data.getFullName(),
                    data.getPhone(),
                    data.getRole(),
                    data.getMemberTier(),
                    data.getToken()
                );
                
                // Chuyển màn hình
                Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(LoginActivity.this, apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    public void onFailure(Call<ApiResponse<LoginResponse>> call, Throwable t) {
        Toast.makeText(LoginActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
    }
});
```

### 2. Register

```java
RegisterRequest request = new RegisterRequest(
    "student123",           // username
    "0909123456",          // phone
    "123456",              // password
    "Nguyen Van A",        // fullName
    "KTX UTE, Thu Duc"     // address
);

apiService.register(request).enqueue(new Callback<ApiResponse<RegisterResponse>>() {
    @Override
    public void onResponse(Call<ApiResponse<RegisterResponse>> call, Response<ApiResponse<RegisterResponse>> response) {
        if (response.isSuccessful() && response.body() != null) {
            ApiResponse<RegisterResponse> apiResponse = response.body();
            
            if (apiResponse.isSuccess()) {
                Toast.makeText(RegisterActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                // Chuyển về màn hình login
                finish();
            } else {
                Toast.makeText(RegisterActivity.this, apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    public void onFailure(Call<ApiResponse<RegisterResponse>> call, Throwable t) {
        Toast.makeText(RegisterActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
    }
});
```

### 3. Lấy danh sách món (Drinks)

```java
ApiService apiService = RetrofitClient.getInstance(this).getApiService();

apiService.getDrinks().enqueue(new Callback<ApiResponse<List<Drink>>>() {
    @Override
    public void onResponse(Call<ApiResponse<List<Drink>>> call, Response<ApiResponse<List<Drink>>> response) {
        if (response.isSuccessful() && response.body() != null) {
            ApiResponse<List<Drink>> apiResponse = response.body();
            
            if (apiResponse.isSuccess()) {
                List<Drink> drinks = apiResponse.getData();
                // Hiển thị danh sách món
                adapter.setDrinks(drinks);
            }
        }
    }
    
    @Override
    public void onFailure(Call<ApiResponse<List<Drink>>> call, Throwable t) {
        Toast.makeText(HomeFragment.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
    }
});
```

### 4. Lấy danh sách cửa hàng

```java
apiService.getStores().enqueue(new Callback<ApiResponse<List<Store>>>() {
    @Override
    public void onResponse(Call<ApiResponse<List<Store>>> call, Response<ApiResponse<List<Store>>> response) {
        if (response.isSuccessful() && response.body() != null) {
            ApiResponse<List<Store>> apiResponse = response.body();
            
            if (apiResponse.isSuccess()) {
                List<Store> stores = apiResponse.getData();
                // Hiển thị danh sách cửa hàng
                adapter.setStores(stores);
            }
        }
    }
    
    @Override
    public void onFailure(Call<ApiResponse<List<Store>>> call, Throwable t) {
        Toast.makeText(StoreFragment.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
    }
});
```

### 5. Load ảnh với Glide

```java
String imageUrl = RetrofitClient.getBaseUrl() + drink.getImageUrl();

Glide.with(context)
    .load(imageUrl)
    .placeholder(R.drawable.placeholder)
    .error(R.drawable.error_image)
    .into(imageView);
```

### 6. Kiểm tra login status

```java
SessionManager sessionManager = new SessionManager(this);

if (!sessionManager.isLoggedIn()) {
    // Chuyển về màn hình login
    startActivity(new Intent(this, LoginActivity.class));
    finish();
    return;
}

// Lấy thông tin user
int userId = sessionManager.getUserId();
String username = sessionManager.getUsername();
String fullName = sessionManager.getFullName();
boolean isManager = sessionManager.isManager();
```

### 7. Logout

```java
SessionManager sessionManager = new SessionManager(this);
sessionManager.logout();

// Chuyển về màn hình login
startActivity(new Intent(this, LoginActivity.class));
finish();
```

---

## 🧪 Testing

### 1. Chạy Backend
```bash
cd Backend_UTEtea
.\mvnw.cmd spring-boot:run
```

Backend chạy tại: `http://localhost:8080`

### 2. Test API với Browser
```
http://localhost:8080/swagger-ui.html
```

### 3. Test trên Emulator
- Base URL: `http://10.0.2.2:8080/api/`
- Build và run app
- Test login với:
  - Username: `ute_student_01`
  - Password: `123456`

### 4. Test trên thiết bị thật
- Tìm IP máy tính: `ipconfig` (Windows)
- Cập nhật BASE_URL: `http://YOUR_IP:8080/api/`
- Đảm bảo điện thoại và máy tính cùng WiFi
- Build và run app

---

## 🐛 Troubleshooting

### Lỗi: Unable to resolve host
**Nguyên nhân:** Không kết nối được backend

**Giải pháp:**
1. Kiểm tra backend đã chạy chưa
2. Kiểm tra BASE_URL đúng chưa
3. Kiểm tra firewall cho phép port 8080
4. Ping thử: `ping YOUR_IP`

### Lỗi: 401 Unauthorized
**Nguyên nhân:** Token không hợp lệ hoặc hết hạn

**Giải pháp:**
1. Logout và login lại
2. Kiểm tra token trong SharedPreferences
3. Kiểm tra AuthInterceptor hoạt động đúng

### Lỗi: Connection timeout
**Nguyên nhân:** Backend chậm hoặc không phản hồi

**Giải pháp:**
1. Tăng timeout trong RetrofitClient
2. Kiểm tra backend logs
3. Test API với Postman/Swagger

### Ảnh không load
**Nguyên nhân:** URL ảnh không đúng

**Giải pháp:**
1. Kiểm tra imageUrl từ API
2. Sử dụng `RetrofitClient.getBaseUrl() + drink.getImageUrl()`
3. Kiểm tra ảnh có tồn tại trong `Backend_UTEtea/assets/drinks/`

---

## 📚 Tài khoản test

```
Username: ute_student_01  | Password: 123456 | Role: USER (BRONZE)
Username: ute_student_02  | Password: 123456 | Role: USER (SILVER)
Username: ute_student_03  | Password: 123456 | Role: USER (GOLD)
Username: manager_ute     | Password: 123456 | Role: MANAGER
```

---

## 🎉 Hoàn thành!

Android app đã sẵn sàng kết nối với backend API. Bắt đầu code các Activity/Fragment để sử dụng API!

**Next steps:**
1. Cập nhật LoginActivity để sử dụng API mới
2. Cập nhật RegisterActivity
3. Cập nhật HomeFragment để hiển thị drinks
4. Cập nhật StoreFragment để hiển thị stores
5. Implement OrderFragment để tạo đơn hàng

---

**Happy Coding! 🚀**
