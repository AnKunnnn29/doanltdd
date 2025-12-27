# 📱 Hướng dẫn cập nhật URL cho App Android

## Sau khi deploy backend lên Render.com

Sửa file `app/src/main/java/com/example/doan/Network/RetrofitClient.kt`:

```kotlin
companion object {
    // ✅ URL Production (sau khi deploy lên Render)
    private const val BASE_URL = "https://utetea-backend.onrender.com/api/"
    
    // ❌ URL cũ (chỉ dùng khi dev local)
    // private const val BASE_URL = "http://192.168.1.63:8080/api/"
    // private const val BASE_URL = "http://10.0.2.2:8080/api/"
}
```

## Lưu ý quan trọng

1. **HTTPS** - Render dùng HTTPS, không phải HTTP
2. **Không cần port** - Render tự động dùng port 443
3. **Đường dẫn /api/** - Giữ nguyên như cũ

## Build APK để cài trên điện thoại

1. Android Studio → Build → Build Bundle(s) / APK(s) → Build APK(s)
2. File APK sẽ ở: `app/build/outputs/apk/debug/app-debug.apk`
3. Copy file APK sang điện thoại và cài đặt

## Cho phép cài app từ nguồn không xác định

1. Vào Cài đặt điện thoại
2. Bảo mật → Nguồn không xác định → Bật
3. Hoặc khi cài sẽ có popup hỏi → Cho phép
