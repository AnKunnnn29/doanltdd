# 🎨 Hướng Dẫn Giao Diện UTE Tea Shop

## ✨ Tính Năng Giao Diện Mới

### 1. **Màn Hình Splash (Khởi Động)**
- Logo trà đẹp mắt với gradient cam-đỏ
- Hiển thị trong 2 giây khi mở app
- Tự động chuyển đến màn hình phù hợp:
  - Nếu đã đăng nhập → MainActivity
  - Nếu chưa đăng nhập → WelcomeActivity

### 2. **Màn Hình Welcome (Chào Mừng)**
- Thiết kế hiện đại với gradient background
- 3 tùy chọn:
  - **ĐĂNG NHẬP**: Chuyển đến màn hình đăng nhập
  - **ĐĂNG KÝ**: Chuyển đến màn hình đăng ký
  - **Tiếp tục với tư cách khách**: Vào app không cần đăng nhập

### 3. **Màn Hình Đăng Nhập**
- Card trắng bo tròn trên nền gradient
- Icon trà xinh xắn
- Input fields với icon và bo góc mềm mại
- Button gradient cam-đỏ
- Link "Đăng ký ngay" để chuyển sang đăng ký
- Animation mượt mà khi chuyển màn hình

### 4. **Màn Hình Đăng Ký**
- Thiết kế tương tự màn hình đăng nhập
- 4 trường nhập liệu:
  - Tên người dùng
  - Số điện thoại / Email
  - Mật khẩu
  - Xác nhận mật khẩu
- Button "ĐĂNG KÝ NGAY" với gradient
- Link "Đăng nhập" để quay lại

## 🎨 Màu Sắc Chủ Đạo

```xml
- Gradient chính: #FF8C00 → #FF6347 (Cam → Đỏ cam)
- Màu trà: #8B4513 (Nâu saddle)
- Màu phụ: #D2691E (Chocolate)
- Màu nền sáng: #FFFAF0
- Màu text: #4A2511 (Nâu đậm)
```

## 🚀 Cách Sử Dụng

### Đăng Nhập
1. Mở app → Màn hình Splash
2. Chọn "ĐĂNG NHẬP" trên màn hình Welcome
3. Nhập tên đăng nhập và mật khẩu
4. Nhấn "ĐĂNG NHẬP"
5. Thành công → Chuyển đến MainActivity

### Đăng Ký
1. Mở app → Màn hình Splash
2. Chọn "ĐĂNG KÝ" trên màn hình Welcome
3. Điền đầy đủ thông tin:
   - Tên người dùng
   - Số điện thoại hoặc Email
   - Mật khẩu (nhập 2 lần)
4. Nhấn "ĐĂNG KÝ NGAY"
5. Thành công → Quay lại màn hình đăng nhập

### Chế Độ Khách
1. Chọn "Tiếp tục với tư cách khách"
2. Vào app ngay lập tức
3. Có thể xem sản phẩm nhưng không thể đặt hàng

## 📱 Các Màn Hình

```
SplashActivity (Khởi động)
    ↓
WelcomeActivity (Chào mừng)
    ↓
    ├── LoginActivity (Đăng nhập) → MainActivity
    ├── RegisterActivity (Đăng ký) → LoginActivity
    └── MainActivity (Chế độ khách)
```

## 🎭 Animation

- **Fade In/Out**: Chuyển từ Splash và sau khi đăng nhập
- **Slide In/Out**: Chuyển giữa Login và Register
- **Material Design**: Ripple effect trên buttons

## 🔧 Files Đã Tạo/Cập Nhật

### Layouts
- `activity_splash.xml` - Màn hình khởi động
- `activity_welcome.xml` - Màn hình chào mừng
- `activity_login.xml` - Màn hình đăng nhập (redesigned)
- `activity_register.xml` - Màn hình đăng ký (redesigned)

### Activities
- `SplashActivity.java` - Logic màn hình khởi động
- `WelcomeActivity.java` - Logic màn hình chào mừng
- `LoginActivity.java` - Cập nhật với animation
- `RegisterActivity.java` - Giữ nguyên logic

### Drawables
- `gradient_background.xml` - Nền gradient cam-đỏ
- `button_gradient.xml` - Button gradient
- `rounded_white_background.xml` - Card trắng bo góc
- `edit_text_background.xml` - Input field background
- `ic_tea_cup.xml` - Icon logo trà
- `ic_lock.xml` - Icon khóa
- `ic_user.xml` - Icon người dùng

### Animations
- `slide_in_right.xml` - Trượt vào từ phải
- `slide_out_left.xml` - Trượt ra bên trái
- `fade_in.xml` - Mờ dần vào
- `fade_out.xml` - Mờ dần ra

### Colors
- Thêm màu trà và gradient vào `colors.xml`

## 💡 Tips

1. **Chạy app lần đầu**: Sẽ thấy Splash → Welcome
2. **Đã đăng nhập**: Splash → MainActivity (bỏ qua Welcome)
3. **Animation mượt**: Tất cả chuyển màn hình đều có hiệu ứng
4. **Material Design**: Sử dụng Material Components cho UI đẹp

## 🎯 Kết Quả

✅ Giao diện hiện đại, chuyên nghiệp
✅ Màu sắc hài hòa, phù hợp với theme trà
✅ Animation mượt mà
✅ Dễ sử dụng, trực quan
✅ Responsive trên mọi kích thước màn hình
✅ Material Design chuẩn Google

---

**Chúc bạn có trải nghiệm tuyệt vời với UTE Tea Shop! ☕**
