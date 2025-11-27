# 🧪 Hướng Dẫn Test App

## ✅ Đã Sửa Lỗi Crash

### Vấn đề đã sửa:
1. ❌ **Theme không đúng**: Manifest dùng `Theme.AppCompat.NoActionBar` nhưng app dùng `Theme.DoAn` (Material3)
2. ✅ **Đã sửa**: Tất cả activities giờ dùng `@style/Theme.DoAn`
3. ✅ **Xóa code thừa**: Bỏ `getSupportActionBar().hide()` vì theme đã là NoActionBar

## 🚀 Cách Test

### 1. Build App
```bash
.\gradlew.bat clean assembleDebug
```

### 2. Cài Đặt
```bash
.\gradlew.bat installDebug
```

### 3. Xem Log (nếu vẫn crash)
```bash
adb logcat | Select-String "AndroidRuntime"
```

## 📱 Flow Test

### Test 1: Khởi động lần đầu
1. Mở app
2. Thấy **Splash Screen** (2 giây)
3. Chuyển đến **Welcome Screen**
4. Thấy 3 nút: Đăng nhập, Đăng ký, Khách

### Test 2: Đăng nhập
1. Nhấn "ĐĂNG NHẬP"
2. Thấy màn hình đăng nhập đẹp
3. Nhập username và password
4. Nhấn "ĐĂNG NHẬP"
5. Nếu thành công → MainActivity

### Test 3: Đăng ký
1. Từ Welcome, nhấn "ĐĂNG KÝ"
2. Thấy màn hình đăng ký
3. Điền thông tin
4. Nhấn "ĐĂNG KÝ NGAY"
5. Thành công → Quay lại Login

### Test 4: Chế độ khách
1. Từ Welcome, nhấn "Tiếp tục với tư cách khách"
2. Vào MainActivity ngay

### Test 5: Đã đăng nhập
1. Đóng app
2. Mở lại
3. Splash → MainActivity (bỏ qua Welcome)

## 🐛 Nếu Vẫn Crash

### Kiểm tra Log
```bash
adb logcat -c  # Clear log
adb logcat | Select-String "FATAL"
```

### Các lỗi thường gặp:

1. **ClassNotFoundException**
   - Kiểm tra tên package trong AndroidManifest
   - Rebuild: `.\gradlew.bat clean build`

2. **ResourceNotFoundException**
   - Kiểm tra R.layout.activity_splash tồn tại
   - Kiểm tra R.anim.fade_in tồn tại

3. **NullPointerException**
   - Kiểm tra findViewById() có đúng ID không
   - Kiểm tra layout XML có đúng không

## 📋 Checklist

- [x] Theme đã sửa thành Theme.DoAn
- [x] Xóa getSupportActionBar().hide()
- [x] Tất cả animation files tồn tại
- [x] Tất cả layout files tồn tại
- [x] AndroidManifest đúng
- [x] Build thành công

## 🎯 Kết Quả Mong Đợi

✅ App mở không crash
✅ Thấy Splash screen đẹp
✅ Chuyển đến Welcome screen
✅ Có thể nhấn các nút
✅ Animation mượt mà

## 💡 Tips Debug

### Xem tất cả log của app
```bash
adb logcat | Select-String "com.example.doan"
```

### Xem crash log
```bash
adb logcat *:E
```

### Clear data app
```bash
adb shell pm clear com.example.doan
```

---

**Nếu vẫn gặp vấn đề, hãy gửi log crash để tôi xem!**
