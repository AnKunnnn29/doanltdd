# ⚡ QUICK TEST GUIDE

## 🚀 Test ngay trong 5 phút!

### Bước 1: Chạy Backend (2 phút)

```bash
cd Backend_UTEtea
.\mvnw.cmd spring-boot:run
```

Đợi đến khi thấy:
```
Started BackendApplication in X.XXX seconds
```

Backend đang chạy tại: `http://localhost:8080`

---

### Bước 2: Test Backend với Browser (1 phút)

Mở browser, truy cập:
```
http://localhost:8080/swagger-ui.html
```

Test API Login:
1. Tìm endpoint `POST /api/auth/login`
2. Click "Try it out"
3. Nhập:
```json
{
  "usernameOrPhone": "ute_student_01",
  "password": "123456"
}
```
4. Click "Execute"
5. Kiểm tra response có `"success": true` và có `token`

✅ Backend hoạt động tốt!

---

### Bước 3: Cấu hình Android App (1 phút)

#### A. Test trên Emulator
File `RetrofitClient.java` đã được cấu hình sẵn:
```java
private static final String BASE_URL = "http://10.0.2.2:8080/api/";
```
✅ Không cần thay đổi gì!

#### B. Test trên thiết bị thật
1. Tìm IP máy tính:
```bash
ipconfig
```
Tìm dòng `IPv4 Address`, ví dụ: `192.168.1.100`

2. Sửa `RetrofitClient.java`:
```java
private static final String BASE_URL = "http://192.168.1.100:8080/api/";
```

3. Đảm bảo điện thoại và máy tính cùng WiFi

---

### Bước 4: Build và Run App (1 phút)

1. Mở Android Studio
2. Build project: `Build > Make Project`
3. Run app: `Run > Run 'app'`

---

### Bước 5: Test Login (30 giây)

1. Mở app
2. Nhập:
   - Username: `ute_student_01`
   - Password: `123456`
3. Click "Đăng nhập"

✅ Nếu login thành công → App đã kết nối với backend!

---

## 🧪 Test các tính năng khác

### Test lấy danh sách món
```java
// Trong HomeFragment hoặc bất kỳ đâu
ApiService apiService = RetrofitClient.getInstance(this).getApiService();

apiService.getDrinks().enqueue(new Callback<ApiResponse<List<Drink>>>() {
    @Override
    public void onResponse(Call<ApiResponse<List<Drink>>> call, Response<ApiResponse<List<Drink>>> response) {
        if (response.isSuccessful() && response.body() != null) {
            ApiResponse<List<Drink>> apiResponse = response.body();
            if (apiResponse.isSuccess()) {
                List<Drink> drinks = apiResponse.getData();
                Log.d("TEST", "Số món: " + drinks.size()); // Should be 16
            }
        }
    }
    
    @Override
    public void onFailure(Call<ApiResponse<List<Drink>>> call, Throwable t) {
        Log.e("TEST", "Error: " + t.getMessage());
    }
});
```

### Test lấy danh sách cửa hàng
```java
apiService.getStores().enqueue(new Callback<ApiResponse<List<Store>>>() {
    @Override
    public void onResponse(Call<ApiResponse<List<Store>>> call, Response<ApiResponse<List<Store>>> response) {
        if (response.isSuccessful() && response.body() != null) {
            ApiResponse<List<Store>> apiResponse = response.body();
            if (apiResponse.isSuccess()) {
                List<Store> stores = apiResponse.getData();
                Log.d("TEST", "Số cửa hàng: " + stores.size()); // Should be 2
            }
        }
    }
    
    @Override
    public void onFailure(Call<ApiResponse<List<Store>>> call, Throwable t) {
        Log.e("TEST", "Error: " + t.getMessage());
    }
});
```

---

## 🐛 Troubleshooting nhanh

### Lỗi: Unable to resolve host
**Giải pháp:**
1. Kiểm tra backend đã chạy: `http://localhost:8080/swagger-ui.html`
2. Kiểm tra BASE_URL trong `RetrofitClient.java`
3. Nếu dùng thiết bị thật, kiểm tra cùng WiFi

### Lỗi: Connection timeout
**Giải pháp:**
1. Tắt firewall tạm thời
2. Kiểm tra backend logs có lỗi không
3. Thử tăng timeout trong `RetrofitClient.java`

### Lỗi: 401 Unauthorized
**Giải pháp:**
1. Login lại để lấy token mới
2. Kiểm tra `AuthInterceptor` hoạt động đúng
3. Check token trong SharedPreferences

---

## 📊 Expected Results

### Backend logs khi app connect:
```
2025-11-27 18:30:00.123  INFO --- [nio-8080-exec-1] c.u.b.controller.AuthController : Login attempt for user: ute_student_01
2025-11-27 18:30:00.456  INFO --- [nio-8080-exec-1] c.u.b.controller.AuthController : Login successful for user: ute_student_01
```

### Android Logcat:
```
D/OkHttp: --> POST http://10.0.2.2:8080/api/auth/login
D/OkHttp: {"usernameOrPhone":"ute_student_01","password":"123456"}
D/OkHttp: <-- 200 OK http://10.0.2.2:8080/api/auth/login (234ms)
D/OkHttp: {"success":true,"message":"Login successful","data":{...}}
```

---

## ✅ Checklist

- [ ] Backend đang chạy tại port 8080
- [ ] Swagger UI accessible
- [ ] BASE_URL đã cấu hình đúng
- [ ] App build thành công
- [ ] Login thành công
- [ ] Có thể lấy danh sách drinks
- [ ] Có thể lấy danh sách stores

---

## 🎯 Tài khoản test

```
Username: ute_student_01  | Password: 123456 | Role: USER (BRONZE)
Username: ute_student_02  | Password: 123456 | Role: USER (SILVER)
Username: ute_student_03  | Password: 123456 | Role: USER (GOLD)
Username: manager_ute     | Password: 123456 | Role: MANAGER
```

---

## 📞 Cần giúp đỡ?

1. Đọc `ANDROID-API-SETUP.md` để hiểu chi tiết
2. Xem `EXAMPLE-USAGE.md` để có code examples
3. Check `CHANGES-SUMMARY.md` để biết đã thay đổi gì
4. Xem backend logs để debug

---

**Chúc bạn test thành công! 🎉**
