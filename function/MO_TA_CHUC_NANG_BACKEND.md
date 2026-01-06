# MÔ TẢ CÁC CHỨC NĂNG BACKEND - ỨNG DỤNG UTE TEA

## GIỚI THIỆU TỔNG QUAN

**UTE Tea Backend** là REST API server được xây dựng bằng Spring Boot 3.5.7, phục vụ cho ứng dụng đặt trà sữa trực tuyến. Backend sử dụng các công nghệ:
- **Java 17** - Ngôn ngữ lập trình chính
- **Spring Boot 3.5.7** - Framework backend
- **Spring Security + JWT** - Xác thực và phân quyền
- **Spring Data JPA + Hibernate** - ORM và truy cập database
- **MySQL 8.0** - Cơ sở dữ liệu

---

## CHỨC NĂNG 1: ĐĂNG KÝ TÀI KHOẢN VỚI XÁC THỰC OTP

### 1.1 Mô tả chức năng
Cho phép người dùng đăng ký tài khoản mới với quy trình xác thực 2 bước:
- Bước 1: Nhập thông tin đăng ký (username, email, password, fullName, phone, address)
- Bước 2: Nhận mã OTP qua email và xác thực để kích hoạt tài khoản

### 1.2 Giao diện API
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/api/auth/register-with-otp` | Đăng ký và gửi OTP qua email |
| POST | `/api/auth/otp-verify` | Xác thực OTP và kích hoạt tài khoản |
| POST | `/api/auth/resend-otp` | Gửi lại mã OTP nếu hết hạn |

**Request Body (register-with-otp):**
```json
{
    "username": "user01",
    "email": "user@gmail.com",
    "password": "123456",
    "fullName": "Nguyen Van A",
    "phone": "0909123456",
    "address": "123 ABC Street"
}
```

### 1.3 Giải thích code thực hiện

**File: AuthService.java**

```java
@Transactional
public void registerWithOtp(RegisterRequest request) {
    // 1. Validate: Kiểm tra username và email đã tồn tại chưa
    if (userRepository.existsByUsername(request.getUsername())) {
        throw new BusinessException("Username already exists");
    }
    if (request.getEmail() != null && !request.getEmail().isEmpty() 
        && userRepository.existsByEmail(request.getEmail())) {
        throw new BusinessException("Email already exists");
    }

    // 2. Tạo đối tượng User mới
    User user = new User();
    user.setUsername(request.getUsername());
    user.setEmail(request.getEmail());
    user.setPhone(request.getPhone());
    // Mã hóa mật khẩu bằng BCrypt trước khi lưu
    user.setPassword(passwordEncoder.encode(request.getPassword()));
    user.setFullName(request.getFullName() != null ? request.getFullName() : request.getUsername());
    user.setAddress(request.getAddress());
    user.setRole(UserRole.USER);           // Mặc định là USER
    user.setMemberTier(MemberTier.BRONZE); // Hạng thành viên mặc định
    user.setPoints(0);
    user.setActive(false);  // CHƯA kích hoạt - phải xác thực OTP
    user.setIsBlocked(false);
    
    // 3. Tạo mã OTP 6 số và set thời hạn 5 phút
    String otp = otpService.generateOtp();
    user.setOtp(otp);
    user.setOtpExpiry(java.time.LocalDateTime.now().plusMinutes(5));

    // 4. Lưu user vào database
    user = userRepository.save(user);

    // 5. Gửi OTP qua email
    otpService.sendOtp(otp, request.getEmail());
}
```

**Giải thích:**
- `@Transactional`: Đảm bảo tất cả thao tác database được thực hiện trong một transaction
- `passwordEncoder.encode()`: Mã hóa mật khẩu bằng BCrypt (cost factor 10)
- `user.setActive(false)`: Tài khoản chưa được kích hoạt cho đến khi xác thực OTP
- `otpService.generateOtp()`: Tạo mã OTP 6 số ngẫu nhiên
- `otpService.sendOtp()`: Gửi OTP qua email sử dụng SendGrid

---

## CHỨC NĂNG 2: ĐĂNG NHẬP VÀ XÁC THỰC JWT

### 2.1 Mô tả chức năng
Cho phép người dùng đăng nhập bằng username/phone và password, trả về JWT token để xác thực các request tiếp theo.

### 2.2 Giao diện API
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/api/auth/login` | Đăng nhập với username/phone + password |
| POST | `/api/auth/refresh-token` | Làm mới access token |

**Request Body:**
```json
{
    "usernameOrPhone": "user01",
    "password": "123456"
}
```

**Response:**
```json
{
    "success": true,
    "message": "Login successful",
    "data": {
        "id": 1,
        "username": "user01",
        "fullName": "Nguyen Van A",
        "role": "USER",
        "memberTier": "BRONZE",
        "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    }
}
```

### 2.3 Giải thích code thực hiện

**File: AuthService.java**

```java
@Transactional
public LoginResponse login(LoginRequest request) {
    try {
        // 1. Xác thực với Spring Security Authentication Manager
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsernameOrPhone(),
                request.getPassword()
            )
        );
    } catch (BadCredentialsException e) {
        // Ghi log đăng nhập thất bại cho mục đích monitoring
        userMonitoringService.logLoginFailed(request.getUsernameOrPhone(), httpRequest);
        throw new BusinessException("Invalid credentials");
    }

    // 2. Tìm user trong database bằng username hoặc phone
    User user = userRepository.findByUsernameOrPhone(
        request.getUsernameOrPhone(),
        request.getUsernameOrPhone()
    ).orElseThrow(() -> new BusinessException("Invalid credentials"));

    // 3. Kiểm tra trạng thái tài khoản
    if (user.getIsBlocked()) {
        throw new BusinessException("Account is blocked");
    }
    if (!user.getActive()) {
        throw new BusinessException("Account is inactive");
    }

    // 4. Ghi log đăng nhập thành công
    userMonitoringService.logLoginSuccess(user.getId(), httpRequest);

    // 5. Tạo JWT Access Token và Refresh Token
    UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
    String accessToken = jwtUtil.generateToken(userDetails, user.getRole().name());
    String refreshToken = jwtUtil.generateRefreshToken(userDetails);

    // 6. Trả về response với thông tin user và tokens
    return mapToLoginResponse(user, accessToken, refreshToken);
}
```

**Giải thích:**
- `authenticationManager.authenticate()`: Sử dụng Spring Security để xác thực username/password
- `userRepository.findByUsernameOrPhone()`: Cho phép đăng nhập bằng cả username hoặc số điện thoại
- `jwtUtil.generateToken()`: Tạo JWT access token với thời hạn 24 giờ
- `jwtUtil.generateRefreshToken()`: Tạo refresh token để làm mới access token khi hết hạn

---

## CHỨC NĂNG 3: QUẢN LÝ MENU SẢN PHẨM (DRINKS)

### 3.1 Mô tả chức năng
Hiển thị danh sách các món nước, cho phép tìm kiếm, xem chi tiết sản phẩm với các size và topping đi kèm.

### 3.2 Giao diện API
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/api/drinks` | Lấy danh sách tất cả món nước |
| GET | `/api/drinks/{id}` | Chi tiết một món nước |
| GET | `/api/drinks/search?keyword=...` | Tìm kiếm món theo tên |
| GET | `/api/categories` | Lấy danh mục |
| GET | `/api/categories/{id}/drinks` | Lấy sản phẩm theo danh mục |

**Response (GET /api/drinks):**
```json
{
    "success": true,
    "data": [
        {
            "id": 1,
            "name": "Trà sữa Houjicha Classic",
            "description": "Trà sữa đậm đà vị trà rang",
            "imageUrl": "https://...",
            "basePrice": 35000,
            "categoryId": 1,
            "categoryName": "Milk Tea",
            "sizes": [
                { "id": 1, "sizeName": "M", "extraPrice": 0 },
                { "id": 2, "sizeName": "L", "extraPrice": 5000 },
                { "id": 3, "sizeName": "Jumbo", "extraPrice": 10000 }
            ],
            "toppings": [
                { "id": 1, "toppingName": "Trân châu đen", "price": 10000 },
                { "id": 2, "toppingName": "Thạch dừa", "price": 8000 }
            ]
        }
    ]
}
```

### 3.3 Giải thích code thực hiện

**File: DrinkService.java**

```java
@Transactional(readOnly = true)
public List<DrinkDto> getAllActiveDrinks() {
    // 1. Query drinks với sizes và category (JOIN FETCH để tránh N+1 query)
    List<Drink> drinks = drinkRepository.findByIsActiveTrueWithSizesAndCategory();
    
    if (drinks.isEmpty()) {
        return List.of();
    }
    
    // 2. Batch load toppings cho tất cả drinks (giảm số lượng query)
    List<Long> drinkIds = drinks.stream().map(Drink::getId).collect(Collectors.toList());
    Map<Long, List<DrinkTopping>> toppingsMap = loadToppingsForDrinks(drinkIds);
    
    // 3. Load global toppings (toppings dùng chung cho mọi drink)
    List<DrinkTopping> globalToppings = drinkToppingRepository
        .findByDrinkIdIsNullAndIsActiveTrue();
    
    // 4. Map entity sang DTO
    return drinks.stream()
        .map(drink -> mapToDtoOptimized(drink, toppingsMap.get(drink.getId()), globalToppings))
        .collect(Collectors.toList());
}

@Transactional(readOnly = true)
public List<DrinkDto> searchDrinks(String keyword) {
    // Input sanitization: validate và làm sạch keyword
    if (keyword == null || keyword.trim().isEmpty()) {
        return List.of();
    }
    
    // Sanitize: loại bỏ ký tự đặc biệt, giữ lại chữ Việt
    String sanitized = keyword.replaceAll(
        "[^a-zA-Z0-9\\sàáạảã...đĐ]", "");
    if (sanitized.length() > 100) {
        sanitized = sanitized.substring(0, 100);
    }
    
    // Tìm kiếm với JOIN FETCH
    List<Drink> drinks = drinkRepository.searchByNameWithSizesAndCategory(sanitized);
    // ... (tương tự như trên)
}
```

**Giải thích:**
- `@Transactional(readOnly = true)`: Tối ưu performance cho các truy vấn chỉ đọc
- `findByIsActiveTrueWithSizesAndCategory()`: Sử dụng JOIN FETCH để load sizes và category trong 1 query, tránh N+1 query problem
- `loadToppingsForDrinks(drinkIds)`: Batch load toppings cho nhiều drinks cùng lúc
- Input sanitization: Làm sạch input để tránh SQL injection và XSS

---

## CHỨC NĂNG 4: QUẢN LÝ GIỎ HÀNG (CART)

### 4.1 Mô tả chức năng
Cho phép người dùng thêm, sửa, xóa sản phẩm trong giỏ hàng trước khi đặt hàng.

### 4.2 Giao diện API
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/api/cart/add` | Thêm sản phẩm vào giỏ |
| GET | `/api/cart` | Lấy giỏ hàng hiện tại |
| PUT | `/api/cart/items/{cartItemId}?quantity=...` | Cập nhật số lượng |
| DELETE | `/api/cart/items/{cartItemId}` | Xóa sản phẩm khỏi giỏ |
| DELETE | `/api/cart/clear` | Xóa toàn bộ giỏ hàng |

**Request Body (add to cart):**
```json
{
    "drinkId": 1,
    "sizeId": 2,
    "quantity": 2,
    "toppingIds": [1, 3],
    "note": "Ít đường"
}
```

### 4.3 Giải thích code thực hiện

**File: CartService.java**

```java
@Transactional
public CartDto addToCart(Long userId, AddToCartRequest request) {
    // 1. Lấy hoặc tạo cart cho user
    Cart cart = cartRepository.findByUserId(userId)
        .orElseGet(() -> {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            Cart newCart = new Cart();
            newCart.setUser(user);
            newCart.setItems(new ArrayList<>());
            return cartRepository.save(newCart);
        });
    
    // 2. Lấy thông tin drink
    Drink drink = drinkRepository.findById(request.getDrinkId())
        .orElseThrow(() -> new ResourceNotFoundException("Drink not found"));
    
    // 3. Tính giá = basePrice + sizeExtraPrice + sum(toppingPrices)
    BigDecimal unitPrice = drink.getBasePrice();
    
    // Cộng thêm giá size (nếu có)
    if (request.getSizeId() != null && request.getSizeId() > 0) {
        DrinkSize size = drinkSizeRepository.findById(request.getSizeId())
            .orElseThrow(() -> new ResourceNotFoundException("Size not found"));
        unitPrice = unitPrice.add(size.getExtraPrice());
    }
    
    // Cộng thêm giá các topping
    List<DrinkTopping> toppings = new ArrayList<>();
    if (request.getToppingIds() != null && !request.getToppingIds().isEmpty()) {
        toppings = drinkToppingRepository.findAllById(request.getToppingIds());
        for (DrinkTopping topping : toppings) {
            unitPrice = unitPrice.add(topping.getPrice());
        }
    }
    
    // 4. Tính tổng tiền = unitPrice * quantity
    BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));
    
    // 5. Tạo cart item và lưu
    CartItem cartItem = new CartItem();
    cartItem.setCart(cart);
    cartItem.setDrink(drink);
    cartItem.setQuantity(request.getQuantity());
    cartItem.setUnitPrice(unitPrice.doubleValue());
    cartItem.setTotalPrice(totalPrice.doubleValue());
    cartItem.setToppings(toppings);
    cartItem.setNote(request.getNote());
    
    cart.getItems().add(cartItem);
    cartItemRepository.save(cartItem);
    
    // 6. Log activity cho monitoring
    userMonitoringService.logCartAddItem(userId, drink.getName(), request.getQuantity(), null);
    
    return getCart(userId);
}
```

**Giải thích:**
- `cartRepository.findByUserId().orElseGet()`: Tìm cart hiện có hoặc tạo mới nếu chưa có
- Công thức tính giá: `itemPrice = (basePrice + sizeExtra + toppingPrices) × quantity`
- `userMonitoringService.logCartAddItem()`: Ghi log hoạt động để theo dõi hành vi người dùng

---

## CHỨC NĂNG 5: TẠO ĐƠN HÀNG (ORDER)

### 5.1 Mô tả chức năng
Cho phép người dùng tạo đơn hàng mới với các tùy chọn:
- Chọn loại đơn: DELIVERY (giao hàng) hoặc PICKUP (tự đến lấy)
- Chọn cửa hàng
- Áp dụng mã giảm giá hoặc voucher từ vòng quay
- Chọn phương thức thanh toán (COD, VNPay)

### 5.2 Giao diện API
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/api/orders` | Tạo đơn hàng mới |
| GET | `/api/orders/my` | Lịch sử đơn hàng của tôi |
| GET | `/api/orders/{orderId}` | Chi tiết đơn hàng |
| GET | `/api/orders/my/current` | Đơn hàng đang xử lý |

**Request Body:**
```json
{
    "storeId": 1,
    "type": "DELIVERY",
    "address": "KTX khu A, UTE",
    "paymentMethod": "COD",
    "promotionCode": "STUDENT20",
    "items": [
        {
            "drinkId": 1,
            "sizeName": "L",
            "quantity": 2,
            "note": "Ít đường",
            "toppingIds": [1, 3]
        }
    ]
}
```

### 5.3 Giải thích code thực hiện

**File: OrderService.java**

```java
@Transactional
public OrderDto createOrder(String username, OrderRequest request) {
    // BƯỚC 1: VALIDATE REQUEST
    validateOrderRequest(request);  // Kiểm tra số lượng items, quantity hợp lệ

    // BƯỚC 2: Validate user
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    
    if (!user.getActive()) {
        throw new BusinessException("User account is inactive", HttpStatus.FORBIDDEN);
    }
    
    // Security: Check rate limit (20 đơn/giờ per user)
    rateLimitService.checkOrderRateLimit(user.getId());

    // BƯỚC 3: Validate store
    Store store = storeRepository.findById(request.getStoreId())
        .orElseThrow(() -> new ResourceNotFoundException("Store", "id", request.getStoreId()));

    // BƯỚC 4: Tạo Order entity
    Order order = new Order();
    order.setUser(user);
    order.setStore(store);
    order.setType(request.getType());
    order.setStatus(OrderStatus.PENDING);  // Trạng thái ban đầu: PENDING
    order.setPaymentMethod(request.getPaymentMethod());

    // BƯỚC 5: Xử lý từng item trong đơn hàng
    BigDecimal totalPrice = BigDecimal.ZERO;
    Set<OrderItem> items = new HashSet<>();

    for (OrderItemRequest itemReq : request.getItems()) {
        Drink drink = drinkRepository.findById(itemReq.getDrinkId())
            .orElseThrow(() -> new ResourceNotFoundException("Drink", "id", itemReq.getDrinkId()));

        if (!drink.getIsActive()) {
            throw new BusinessException("Drink '" + drink.getName() + "' is not available");
        }

        // Tính giá item = basePrice + sizeExtra + toppings
        BigDecimal itemPrice = drink.getBasePrice();
        
        // Thêm giá size
        List<DrinkSize> sizes = drinkSizeRepository.findByDrinkId(drink.getId());
        for (DrinkSize size : sizes) {
            if (size.getSizeName().equals(itemReq.getSizeName())) {
                itemPrice = itemPrice.add(size.getExtraPrice());
                break;
            }
        }

        // Thêm giá toppings
        if (itemReq.getToppingIds() != null) {
            for (Long toppingId : itemReq.getToppingIds()) {
                DrinkTopping topping = drinkToppingRepository.findByIdWithDrink(toppingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Topping", "id", toppingId));
                itemPrice = itemPrice.add(topping.getPrice());
            }
        }

        // Nhân với số lượng
        itemPrice = itemPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
        totalPrice = totalPrice.add(itemPrice);

        // Tạo OrderItem với snapshot thông tin
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setDrink(drink);
        item.setDrinkNameSnapshot(drink.getName());    // Lưu snapshot tên
        item.setSizeNameSnapshot(itemReq.getSizeName());
        item.setQuantity(itemReq.getQuantity());
        item.setItemPrice(itemPrice);
        items.add(item);
    }

    order.setItems(items);
    order.setTotalPrice(totalPrice);

    // BƯỚC 6: Áp dụng giảm giá (promotion hoặc spin voucher)
    BigDecimal discount = BigDecimal.ZERO;
    
    if (request.getPromotionCode() != null) {
        Promotion promotion = promotionRepository.findByCodeForUpdate(request.getPromotionCode())
            .orElseThrow(() -> new BusinessException("Mã voucher không hợp lệ"));
        
        // Validate promotion
        if (!promotion.getIsActive()) throw new BusinessException("Mã đã bị vô hiệu hóa");
        if (promotion.getStartDate().isAfter(LocalDateTime.now())) 
            throw new BusinessException("Mã chưa có hiệu lực");
        if (promotion.getEndDate().isBefore(LocalDateTime.now())) 
            throw new BusinessException("Mã đã hết hạn");
        
        // Tính discount
        if (promotion.getDiscountType() == DiscountType.PERCENT) {
            discount = totalPrice.multiply(promotion.getDiscountValue())
                .divide(BigDecimal.valueOf(100));
            if (discount.compareTo(promotion.getMaxDiscountAmount()) > 0) {
                discount = promotion.getMaxDiscountAmount();  // Giới hạn discount tối đa
            }
        } else {
            discount = promotion.getDiscountValue();  // Giảm số tiền cố định
        }
        
        // Tăng số lần sử dụng
        promotion.setUsedCount(promotion.getUsedCount() + 1);
        promotionRepository.save(promotion);
    }

    // BƯỚC 7: Áp dụng thêm discount theo Member Tier (cộng dồn)
    BigDecimal tierDiscount = memberTierService.calculateTierDiscount(user.getMemberTier(), totalPrice);
    discount = discount.add(tierDiscount);

    order.setDiscount(discount);
    order.setFinalPrice(totalPrice.subtract(discount));

    // BƯỚC 8: Lưu order
    order = orderRepository.save(order);

    // BƯỚC 9: Gửi thông báo
    orderWebSocketService.notifyNewOrder(orderDto);     // WebSocket real-time
    emailService.sendOrderConfirmationEmail(order);     // Email xác nhận
    sendNotificationToManagers(order);                   // Push notification đến Manager

    return orderDto;
}
```

**Giải thích:**
- `validateOrderRequest()`: Validate tất cả input từ client (số lượng items, quantity, drinkId...)
- `rateLimitService.checkOrderRateLimit()`: Giới hạn 20 đơn/giờ để chống spam
- **Snapshot pattern**: Lưu `drinkNameSnapshot`, `sizeNameSnapshot` để giữ nguyên thông tin tại thời điểm đặt hàng, không bị ảnh hưởng khi admin sửa tên món
- `findByCodeForUpdate()`: Sử dụng PESSIMISTIC_WRITE lock để tránh race condition khi nhiều người dùng cùng voucher
- **Công thức tính giá**:
  ```
  itemPrice = (basePrice + sizeExtra + toppingPrices) × quantity
  totalPrice = sum(itemPrice)
  discount = voucherDiscount + tierDiscount
  finalPrice = totalPrice - discount
  ```

---

## CHỨC NĂNG 6: QUẢN LÝ MÃ GIẢM GIÁ (PROMOTION)

### 6.1 Mô tả chức năng
Hệ thống khuyến mãi linh hoạt hỗ trợ:
- Giảm theo % hoặc số tiền cố định
- Giới hạn số lần sử dụng
- Đơn hàng tối thiểu
- Thời gian hiệu lực

### 6.2 Giao diện API
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/api/promotions` | Lấy danh sách khuyến mãi đang hoạt động |
| GET | `/api/promotions/validate?code=...&orderAmount=...` | Kiểm tra mã voucher |
| POST | `/api/promotions/manager` | Tạo voucher mới (Manager) |
| PUT | `/api/promotions/manager/{id}` | Cập nhật voucher (Manager) |
| DELETE | `/api/promotions/manager/{id}` | Xóa/vô hiệu voucher (Manager) |

### 6.3 Giải thích code thực hiện

**File: PromotionService.java**

```java
/**
 * Validate voucher - CORE METHOD
 */
private void validatePromotion(Promotion promotion, BigDecimal orderAmount, Long userId) {
    LocalDateTime now = LocalDateTime.now();
    
    // 1. Check active
    if (!promotion.getIsActive()) {
        throw new BusinessException("Mã voucher đã bị vô hiệu hóa");
    }
    
    // 2. Check thời gian
    if (now.isBefore(promotion.getStartDate())) {
        throw new BusinessException("Mã voucher chưa có hiệu lực");
    }
    if (now.isAfter(promotion.getEndDate())) {
        throw new VoucherExpiredException("Mã voucher đã hết hạn vào " + promotion.getEndDate());
    }
    
    // 3. Check usage limit (tổng số lần dùng)
    if (promotion.getUsageLimit() != null && 
        promotion.getUsedCount() >= promotion.getUsageLimit()) {
        throw new BusinessException("Mã voucher đã hết lượt sử dụng");
    }
    
    // 4. Check user đã dùng voucher này chưa (mỗi user chỉ dùng 1 lần)
    if (userId != null) {
        boolean hasUsed = promotionUsageRepository.existsByPromotionIdAndUserId(
            promotion.getId(), userId);
        if (hasUsed) {
            throw new VoucherAlreadyUsedException("Bạn đã sử dụng mã voucher này rồi");
        }
    }
    
    // 5. Check minimum order value
    if (orderAmount != null && orderAmount.compareTo(promotion.getMinOrderValue()) < 0) {
        throw new BusinessException(String.format(
            "Giá trị đơn hàng tối thiểu là %s VND cho mã voucher này", 
            promotion.getMinOrderValue()));
    }
}

@Transactional
public PromotionDto createPromotion(CreatePromotionRequest request) {
    // Validate code uniqueness
    if (promotionRepository.findByCode(request.getCode()).isPresent()) {
        throw new BusinessException("Mã voucher đã tồn tại: " + request.getCode());
    }
    
    // Validate dates
    if (request.getEndDate().isBefore(request.getStartDate())) {
        throw new BusinessException("Ngày kết thúc phải sau ngày bắt đầu");
    }
    
    // Validate discount value for PERCENT type (không vượt quá 100%)
    if (request.getDiscountType() == DiscountType.PERCENT) {
        if (request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException("Giá trị giảm giá % không được vượt quá 100%");
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
        oneSignalService.sendToAll(
            "🎉 Voucher mới dành cho bạn!",
            promotion.getDescription() + " - Mã: " + promotion.getCode(),
            NotificationType.PROMOTION, promotion.getId());
    }
    
    return promotionMapper.toDto(promotion);
}
```

**Giải thích:**
- `validatePromotion()`: Kiểm tra đầy đủ các điều kiện của voucher
- `VoucherExpiredException`, `VoucherAlreadyUsedException`: Custom exception để xử lý các trường hợp cụ thể
- `promotionUsageRepository.existsByPromotionIdAndUserId()`: Kiểm tra user đã dùng voucher chưa
- `oneSignalService.sendToAll()`: Gửi push notification đến tất cả người dùng khi có voucher mới

---

## CHỨC NĂNG 7: ĐÁNH GIÁ SẢN PHẨM (REVIEW)

### 7.1 Mô tả chức năng
Cho phép người dùng đánh giá sản phẩm sau khi đơn hàng hoàn thành:
- Đánh giá 1-5 sao
- Viết nhận xét
- Có thể đánh giá ẩn danh

### 7.2 Giao diện API
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/api/reviews` | Tạo đánh giá mới |
| GET | `/api/reviews/drink/{drinkId}` | Đánh giá của một sản phẩm |
| GET | `/api/reviews/drink/{drinkId}/summary` | Thống kê đánh giá |
| GET | `/api/reviews/my-reviews` | Đánh giá của tôi |
| GET | `/api/reviews/can-review/{orderItemId}` | Kiểm tra có thể đánh giá không |

**Request Body:**
```json
{
    "orderId": 1,
    "orderItemId": 5,
    "rating": 5,
    "comment": "Rất ngon, sẽ quay lại!",
    "isAnonymous": false
}
```

### 7.3 Giải thích code thực hiện

**File: ReviewService.java**

```java
@Transactional
public ReviewDto createReview(String username, CreateReviewRequest request) {
    // 1. Validate user
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    
    // 2. Validate order belongs to user và đã DONE
    Order order = orderRepository.findById(request.getOrderId())
        .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    
    if (!order.getUser().getId().equals(user.getId())) {
        throw new BadRequestException("Order does not belong to this user");
    }
    
    if (order.getStatus() != OrderStatus.DONE) {
        throw new BadRequestException("Can only review completed orders");
    }
    
    // 3. Validate order item belongs to order
    OrderItem orderItem = orderItemRepository.findById(request.getOrderItemId())
        .orElseThrow(() -> new ResourceNotFoundException("Order item not found"));
    
    if (!orderItem.getOrder().getId().equals(order.getId())) {
        throw new BadRequestException("Order item does not belong to this order");
    }
    
    // 4. Check if already reviewed (mỗi order item chỉ được review 1 lần)
    if (reviewRepository.existsByUserIdAndOrderItemId(user.getId(), request.getOrderItemId())) {
        throw new BadRequestException("You have already reviewed this item");
    }
    
    // 5. Create review
    Review review = new Review();
    review.setUser(user);
    review.setDrink(orderItem.getDrink());
    review.setOrder(order);
    review.setOrderItem(orderItem);
    review.setRating(request.getRating());
    review.setComment(request.getComment());
    review.setIsAnonymous(request.getIsAnonymous() != null ? request.getIsAnonymous() : false);
    
    review = reviewRepository.save(review);
    
    return toDto(review);
}

public DrinkRatingSummary getDrinkRatingSummary(Long drinkId) {
    // Tính điểm trung bình
    Double avgRating = reviewRepository.getAverageRatingByDrinkId(drinkId);
    Long totalReviews = reviewRepository.countByDrinkId(drinkId);
    
    // Phân phối số sao (1-5 sao)
    Map<Integer, Long> distribution = new HashMap<>();
    for (int i = 1; i <= 5; i++) {
        distribution.put(i, 0L);
    }
    
    List<Object[]> rawDistribution = reviewRepository.getRatingDistributionByDrinkId(drinkId);
    for (Object[] row : rawDistribution) {
        Integer rating = (Integer) row[0];
        Long count = (Long) row[1];
        distribution.put(rating, count);
    }
    
    return DrinkRatingSummary.builder()
        .drinkId(drinkId)
        .averageRating(avgRating != null ? avgRating : 0.0)
        .totalReviews(totalReviews != null ? totalReviews : 0L)
        .ratingDistribution(distribution)  // {1: 5, 2: 3, 3: 10, 4: 25, 5: 100}
        .build();
}
```

**Giải thích:**
- **Validation chain**: Kiểm tra user → order → orderItem → đã review chưa
- `order.getStatus() != OrderStatus.DONE`: Chỉ được đánh giá khi đơn hàng đã hoàn thành
- `existsByUserIdAndOrderItemId()`: Mỗi user chỉ được đánh giá mỗi item 1 lần
- `getDrinkRatingSummary()`: Thống kê điểm trung bình và phân phối số sao

---

## CHỨC NĂNG 8: VÒNG QUAY MAY MẮN (LOYALTY SPIN)

### 8.1 Mô tả chức năng
Hệ thống tích điểm và vòng quay may mắn:
- Mỗi đơn hàng hoàn thành được 1 điểm
- 5 điểm = 1 lượt quay
- Phần thưởng: 0%, 10%, 20%, 50%, 100% giảm giá

### 8.2 Giao diện API
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/api/loyalty/points` | Xem điểm hiện tại |
| POST | `/api/loyalty/spin` | Quay vòng xoay |
| GET | `/api/loyalty/rewards` | Voucher chưa sử dụng |
| GET | `/api/loyalty/tier/benefits` | Quyền lợi hạng thành viên |

### 8.3 Giải thích code thực hiện

**File: LoyaltyService.java**

```java
private static final List<Integer> WHEEL_ITEMS = Arrays.asList(0, 10, 20, 50, 100);
private static final String VOUCHER_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

@Transactional
public SpinWheelResponse spinWheel(String username) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    
    // 1. Check rate limit (10 lần/ngày per user) - chống spam
    rateLimitService.checkSpinRateLimit(user.getId());
    
    // 2. Kiểm tra đủ điểm không (cần 5 điểm)
    if (user.getPoints() < pointsToSpin) {
        throw new BadRequestException("Không đủ điểm để quay. Cần " + pointsToSpin + " điểm.");
    }
    
    // 3. Random vị trí trúng với tỷ lệ khác nhau
    int winIndex = getRandomWinIndex();
    int discountPercent = WHEEL_ITEMS.get(winIndex);
    
    // 4. Trừ điểm
    user.setPoints(user.getPoints() - pointsToSpin);
    userRepository.save(user);
    
    // 5. Tạo mã voucher 10 ký tự unique
    String voucherCode = generateUniqueVoucherCode();
    
    // 6. Lưu phần thưởng
    SpinReward reward = new SpinReward();
    reward.setUser(user);
    reward.setVoucherCode(voucherCode);
    reward.setDiscountPercent(discountPercent);
    reward.setPointsUsed(pointsToSpin);
    reward.setIsUsed(discountPercent == 0);  // 0% = không có mã, đánh dấu đã dùng
    reward = spinRewardRepository.save(reward);
    
    return SpinWheelResponse.builder()
        .rewardId(reward.getId())
        .voucherCode(discountPercent > 0 ? voucherCode : null)
        .discountPercent(discountPercent)
        .winIndex(winIndex)
        .wheelItems(WHEEL_ITEMS)
        .remainingPoints(user.getPoints())
        .message(getMessage(discountPercent, voucherCode))
        .build();
}

/**
 * Random với tỷ lệ:
 * 0% - 30% (không trúng)
 * 10% - 35%
 * 20% - 20%
 * 50% - 10%
 * 100% - 5%
 */
private int getRandomWinIndex() {
    Random random = new Random();
    int rand = random.nextInt(100);
    
    if (rand < 30) return 0;       // 0% (30%)
    else if (rand < 65) return 1;  // 10% (35%)
    else if (rand < 85) return 2;  // 20% (20%)
    else if (rand < 95) return 3;  // 50% (10%)
    else return 4;                  // 100% (5%)
}

/**
 * Tạo mã voucher unique 10 ký tự
 */
private String generateUniqueVoucherCode() {
    SecureRandom random = new SecureRandom();
    String code;
    int attempts = 0;
    do {
        StringBuilder sb = new StringBuilder(voucherLength);
        for (int i = 0; i < voucherLength; i++) {
            sb.append(VOUCHER_CHARS.charAt(random.nextInt(VOUCHER_CHARS.length())));
        }
        code = sb.toString();
        attempts++;
    } while (spinRewardRepository.existsByVoucherCode(code) && attempts < 100);
    
    return code;
}
```

**Giải thích:**
- `rateLimitService.checkSpinRateLimit()`: Giới hạn 10 lần quay/ngày để chống lạm dụng
- `getRandomWinIndex()`: Tỷ lệ trúng thưởng được thiết kế để cân bằng (phần lớn trúng 0-20%)
- `SecureRandom`: Sử dụng random bảo mật thay vì `Random` thông thường
- `generateUniqueVoucherCode()`: Tạo mã 10 ký tự unique, retry nếu trùng

---

## CHỨC NĂNG 9: QUẢN LÝ THÔNG BÁO (NOTIFICATION)

### 9.1 Mô tả chức năng
Hệ thống thông báo push notification và in-app:
- Thông báo trạng thái đơn hàng (đang pha chế, đang giao, hoàn thành)
- Thông báo khuyến mãi mới
- Thông báo hệ thống

### 9.2 Giao diện API
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/api/notifications/my` | Danh sách thông báo của tôi |
| PUT | `/api/notifications/{id}/read` | Đánh dấu đã đọc |
| PUT | `/api/notifications/read-all` | Đánh dấu tất cả đã đọc |
| DELETE | `/api/notifications/{id}` | Xóa thông báo |
| GET | `/api/notifications/unread-count` | Đếm thông báo chưa đọc |

### 9.3 Giải thích code thực hiện

**File: NotificationService.java**

```java
@Transactional
public Notification saveNotification(Long userId, String title, String content, 
                                     NotificationType type, Long relatedId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

    Notification notification = Notification.builder()
        .user(user)
        .title(title)
        .content(content)
        .type(type)              // ORDER_NEW, ORDER_STATUS, PROMOTION, SYSTEM
        .relatedId(relatedId)   // ID của order/promotion liên quan
        .isRead(false)
        .build();

    notification = notificationRepository.save(notification);
    log.info("Saved notification for user {}: {}", userId, title);
    return notification;
}

@Transactional
public int markAllAsRead(Long userId) {
    int updated = notificationRepository.markAllAsReadByUserId(userId);
    log.info("Marked {} notifications as read for user {}", updated, userId);
    return updated;
}
```

**File: OneSignalService.java** (Push notification)

```java
/**
 * Gửi notification đến user cụ thể qua OneSignal
 */
public void sendToUser(String userId, String title, String content, 
                       NotificationType type, Long relatedId) {
    try {
        // 1. Lưu vào database
        notificationService.saveNotification(Long.parseLong(userId), title, content, type, relatedId);
        
        // 2. Gửi push notification qua OneSignal API
        Map<String, Object> notification = new HashMap<>();
        notification.put("app_id", oneSignalAppId);
        notification.put("include_external_user_ids", Arrays.asList(userId));
        notification.put("headings", Map.of("en", title));
        notification.put("contents", Map.of("en", content));
        notification.put("data", Map.of("type", type.name(), "relatedId", relatedId));
        
        // POST đến OneSignal API
        restTemplate.postForEntity(ONESIGNAL_API_URL, notification, String.class);
        
    } catch (Exception e) {
        log.error("Failed to send push notification to user {}", userId, e);
    }
}
```

**File: OrderService.java** (Gửi notification khi cập nhật đơn hàng)

```java
private void sendNotificationToUser(Order order, OrderStatus status) {
    String userId = String.valueOf(order.getUser().getId());
    String title = "Cập nhật đơn hàng #" + order.getId();
    String content = "";

    switch (status) {
        case MAKING:
            content = "Quán đang pha chế đồ uống cho bạn.";
            break;
        case SHIPPING:
            content = "Shipper đang giao trà sữa đến cho bạn!";
            break;
        case READY:
            content = "Đồ uống đã sẵn sàng tại quầy để bạn lấy.";
            break;
        case DONE:
            content = "Đơn hàng hoàn tất. Chúc bạn ngon miệng!";
            break;
        case CANCELED:
            content = "Đơn hàng của bạn đã bị hủy.";
            break;
        default:
            return;
    }

    oneSignalService.sendToUser(userId, title, content, NotificationType.ORDER_STATUS, order.getId());
}
```

**Giải thích:**
- `NotificationType`: Enum phân loại thông báo (ORDER_NEW, ORDER_STATUS, PROMOTION, SYSTEM)
- `relatedId`: ID của đối tượng liên quan để app có thể navigate đến đúng màn hình
- **Dual save**: Lưu vào database (in-app) + gửi push notification (OneSignal)
- `markAllAsReadByUserId()`: Bulk update để đánh dấu tất cả đã đọc

---

## CHỨC NĂNG 10: DASHBOARD QUẢN LÝ (MANAGER)

### 10.1 Mô tả chức năng
Bảng điều khiển dành cho Manager/Admin:
- Thống kê doanh thu tổng quan
- Quản lý đơn hàng
- Quản lý người dùng
- Báo cáo doanh thu theo ngày/tháng
- Top sản phẩm bán chạy

### 10.2 Giao diện API
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/api/manager/summary` | Dashboard tổng quan |
| GET | `/api/manager/orders?status=...` | Danh sách đơn hàng |
| PUT | `/api/manager/orders/{id}/status` | Cập nhật trạng thái đơn |
| GET | `/api/manager/users` | Danh sách người dùng |
| PUT | `/api/manager/users/{id}/block` | Khóa/mở khóa user |
| GET | `/api/manager/statistics/revenue` | Thống kê doanh thu |

### 10.3 Giải thích code thực hiện

**File: ManagerService.java**

```java
@Transactional(readOnly = true)
public DashboardSummaryDto getDashboardSummary() {
    User manager = getCurrentManager();
    List<Long> storeIds = getManagedStoreIds(manager);  // Manager chỉ xem store được gán
    boolean isAdmin = manager.getRole() == UserRole.ADMIN;
    
    Long pendingOrders, completedOrders, canceledOrders, totalOrders;
    BigDecimal totalRevenue;
    
    if (storeIds == null) {
        // ADMIN - xem tất cả
        pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);
        completedOrders = orderRepository.countByStatus(OrderStatus.DONE);
        canceledOrders = orderRepository.countByStatus(OrderStatus.CANCELED);
        totalOrders = orderRepository.count();
        
        // Tính tổng doanh thu từ đơn DONE
        String totalRevenueQuery = "SELECT COALESCE(SUM(o.finalPrice), 0) FROM Order o WHERE o.status = :status";
        totalRevenue = entityManager.createQuery(totalRevenueQuery, BigDecimal.class)
            .setParameter("status", OrderStatus.DONE)
            .getSingleResult();
        
        // ADMIN còn cộng thêm doanh thu từ backup (user đã xóa tài khoản)
        BigDecimal backupRevenue = getBackupTotalRevenue();
        totalRevenue = totalRevenue.add(backupRevenue);
        
    } else {
        // Store Manager - chỉ xem stores được gán
        pendingOrders = orderRepository.countByStoreIdInAndStatus(storeIds, OrderStatus.PENDING);
        completedOrders = orderRepository.countByStoreIdInAndStatus(storeIds, OrderStatus.DONE);
        canceledOrders = orderRepository.countByStoreIdInAndStatus(storeIds, OrderStatus.CANCELED);
        
        String revenueQuery = "SELECT COALESCE(SUM(o.finalPrice), 0) FROM Order o " +
            "WHERE o.status = :status AND o.store.id IN :storeIds";
        totalRevenue = entityManager.createQuery(revenueQuery, BigDecimal.class)
            .setParameter("status", OrderStatus.DONE)
            .setParameter("storeIds", storeIds)
            .getSingleResult();
    }
    
    // Top 5 sản phẩm bán chạy
    String topDrinksQuery = """
        SELECT oi.drinkNameSnapshot,
               SUM(oi.quantity) as totalQuantity,
               SUM(oi.itemPrice) as totalRevenue
        FROM OrderItem oi
        JOIN oi.order o
        WHERE o.status = :status
        GROUP BY oi.drinkNameSnapshot
        ORDER BY SUM(oi.quantity) DESC
        """;
    List<Object[]> topDrinksResults = entityManager.createQuery(topDrinksQuery, Object[].class)
        .setParameter("status", OrderStatus.DONE)
        .setMaxResults(5)
        .getResultList();
    
    List<TopSellingDrinkDto> topSellingDrinks = new ArrayList<>();
    for (Object[] row : topDrinksResults) {
        String drinkName = (String) row[0];
        Long totalSold = ((Number) row[1]).longValue();
        BigDecimal revenue = (BigDecimal) row[2];
        topSellingDrinks.add(new TopSellingDrinkDto(drinkName, totalSold, revenue));
    }
    
    // Build response
    DashboardSummaryDto summary = new DashboardSummaryDto();
    summary.setTotalRevenue(totalRevenue);
    summary.setTotalOrders(totalOrders);
    summary.setPendingOrders(pendingOrders);
    summary.setCompletedOrders(completedOrders);
    summary.setCanceledOrders(canceledOrders);
    summary.setTopSellingDrinks(topSellingDrinks);
    summary.setIsAdmin(isAdmin);
    
    return summary;
}

/**
 * Thống kê doanh thu theo ngày/tháng
 */
@Transactional(readOnly = true)
public RevenueStatisticsDto getRevenueStatistics(Integer days, Integer months) {
    RevenueStatisticsDto stats = new RevenueStatisticsDto();
    
    // Doanh thu theo ngày (7 ngày gần nhất)
    if (days != null && days > 0) {
        Instant cutoffDate = Instant.now().minus(days, ChronoUnit.DAYS);
        
        String dailyQuery = """
            SELECT CAST(o.created_at AS DATE) as order_date, 
                   COALESCE(SUM(o.final_price), 0) as revenue,
                   COUNT(o.id) as order_count
            FROM orders o 
            WHERE o.status = 'DONE' AND o.created_at >= :cutoffDate
            GROUP BY CAST(o.created_at AS DATE)
            ORDER BY order_date ASC
            """;
        
        List<Object[]> dailyResults = entityManager.createNativeQuery(dailyQuery)
            .setParameter("cutoffDate", Timestamp.from(cutoffDate))
            .getResultList();
        
        List<DailyRevenue> dailyRevenues = new ArrayList<>();
        for (Object[] row : dailyResults) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            BigDecimal revenue = new BigDecimal(row[1].toString());
            Long orderCount = ((Number) row[2]).longValue();
            dailyRevenues.add(new DailyRevenue(date, revenue, orderCount));
        }
        stats.setDailyRevenues(dailyRevenues);
    }
    
    return stats;
}
```

**Giải thích:**
- `getCurrentManager()`: Lấy thông tin Manager đang đăng nhập từ Security Context
- `getManagedStoreIds()`: Admin xem tất cả, Manager chỉ xem store được gán
- **JPQL vs Native Query**: 
  - JPQL: Dùng cho các query đơn giản với entity
  - Native SQL: Dùng cho các query phức tạp với GROUP BY, DATE functions
- `getBackupTotalRevenue()`: Cộng thêm doanh thu từ user đã xóa tài khoản (chỉ Admin)
- `@Transactional(readOnly = true)`: Tối ưu performance cho read-only queries

---

## CHỨC NĂNG 11: QUẢN LÝ HỒ SƠ CÁ NHÂN (USER PROFILE)

### 11.1 Mô tả chức năng
Cho phép người dùng:
- Xem và cập nhật thông tin cá nhân
- Thay đổi ảnh đại diện
- Đổi mật khẩu
- Xóa tài khoản (với backup dữ liệu)

### 11.2 Giao diện API
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/api/me` | Lấy thông tin hồ sơ |
| PUT | `/api/me` | Cập nhật hồ sơ |
| POST | `/api/me/avatar` | Upload ảnh đại diện |
| PUT | `/api/me/change-password` | Đổi mật khẩu |
| DELETE | `/api/me` | Xóa tài khoản |

### 11.3 Giải thích code thực hiện

**File: UserProfileService.java**

```java
@Transactional
public UserProfileDto updateProfile(String username, UpdateProfileRequest request) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

    // Check if phone is being changed and already exists
    if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException("Phone number already exists");
        }
        user.setPhone(request.getPhone());
    }

    if (request.getFullName() != null) {
        user.setFullName(request.getFullName());
    }
    if (request.getAddress() != null) {
        user.setAddress(request.getAddress());
    }
    if (request.getEmail() != null) {
        user.setEmail(request.getEmail());
    }

    user = userRepository.save(user);
    
    // Log activity cho monitoring
    userMonitoringService.logProfileUpdate(user.getId(), "phone, fullName, address, email", null);
    
    return mapToProfileDto(user);
}

@Transactional
public void changePassword(String username, ChangePasswordRequest request) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

    // Kiểm tra mật khẩu cũ có khớp không
    if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
        throw new BusinessException("Mật khẩu cũ không chính xác");
    }

    // Kiểm tra mật khẩu mới và xác nhận
    if (!request.getNewPassword().equals(request.getConfirmPassword())) {
        throw new BusinessException("Mật khẩu xác nhận không khớp");
    }

    // Mã hóa mật khẩu mới và lưu
    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    userRepository.save(user);
    
    // Log activity (quan trọng cho security audit)
    userMonitoringService.logPasswordChange(user.getId(), null);
}

@Transactional
public void deleteAccount(String username) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new BusinessException("User not found"));

    Long userId = user.getId();

    // BƯỚC 1: Backup orders DONE để manager vẫn quản lý được doanh thu
    backupUserOrders(user);
    
    // BƯỚC 2: Backup reviews để vẫn hiển thị đánh giá sản phẩm
    backupUserReviews(user);
    
    // BƯỚC 3: Xóa dữ liệu theo thứ tự (tránh lỗi FK constraint)
    cartRepository.deleteByUserId(userId);
    reviewRepository.deleteByUserId(userId);
    spinRewardRepository.deleteByUserId(userId);
    groupOrderItemRepository.deleteByUserId(userId);
    groupOrderMemberRepository.deleteByUserId(userId);
    groupChatMessageRepository.deleteByHostUserId(userId);
    groupOrderRepository.deleteByHostUserId(userId);
    chatConversationRepository.deleteByUserId(userId);
    orderRepository.deleteByUserId(userId);
    
    // BƯỚC 4: Cuối cùng xóa user
    userRepository.delete(user);
}

/**
 * Backup orders DONE vào bảng DeletedUserOrderBackup
 */
private void backupUserOrders(User user) {
    List<Order> orders = orderRepository.findByUserIdWithItemsOrderByCreatedAtDesc(user.getId());
    
    for (Order order : orders) {
        if (order.getStatus() != OrderStatus.DONE) continue;  // Chỉ backup đơn đã hoàn thành
        
        DeletedUserOrderBackup backup = DeletedUserOrderBackup.builder()
            .deletedUserId(user.getId())
            .deletedUsername(user.getUsername())
            .deletedUserPhone(user.getPhone())
            .originalOrderId(order.getId())
            .store(order.getStore())
            .orderType(order.getType())
            .orderStatus(order.getStatus())
            .totalPrice(order.getTotalPrice())
            .discount(order.getDiscount())
            .finalPrice(order.getFinalPrice())
            .paymentMethod(order.getPaymentMethod())
            .orderCreatedAt(order.getCreatedAt())
            .orderItemsJson(createOrderItemsJson(order))  // JSON chi tiết items
            .note("Auto backup when user deleted account")
            .build();
        
        deletedUserOrderBackupRepository.save(backup);
    }
}
```

**Giải thích:**
- `passwordEncoder.matches()`: So sánh mật khẩu người dùng nhập với mật khẩu đã hash trong DB
- **Delete với backup**: Khi user xóa tài khoản, dữ liệu đơn hàng được backup để:
  - Manager vẫn tính được doanh thu
  - Reviews vẫn hiển thị cho sản phẩm
- **Delete order**: Xóa theo thứ tự từ bảng con đến bảng cha để tránh FK constraint error
- `createOrderItemsJson()`: Lưu chi tiết order items dưới dạng JSON để backup đầy đủ thông tin

---

## TỔNG KẾT

Backend UTE Tea được thiết kế với các đặc điểm:

### Bảo mật
- JWT Authentication với Access + Refresh Token
- BCrypt password hashing
- Rate limiting để chống spam/abuse
- Input validation và sanitization

### Hiệu năng
- JOIN FETCH để tránh N+1 query
- Pagination cho large datasets
- Database-level filtering thay vì load tất cả vào memory

### Kiến trúc
- Layered Architecture: Controller → Service → Repository
- DTO pattern để tách biệt entity và response
- Transaction management với @Transactional

### Monitoring & Logging
- Activity logging cho security audit
- Structured logging với SLF4J
- User activity monitoring

---

*Tài liệu được tạo tự động - Dự án UTE Tea Backend*
