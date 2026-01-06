# 📊 CẤU TRÚC DATABASE UTE TEA (Database Schema)

## 📋 MỤC LỤC
1. [Bảng Người Dùng & Xác Thực](#1-bảng-người-dùng--xác-thực)
2. [Bảng Sản Phẩm & Danh Mục](#2-bảng-sản-phẩm--danh-mục)
3. [Bảng Giỏ Hàng](#3-bảng-giỏ-hàng)
4. [Bảng Đơn Hàng](#4-bảng-đơn-hàng)
5. [Bảng Đặt Hàng Nhóm](#5-bảng-đặt-hàng-nhóm)
6. [Bảng Khuyến Mãi & Voucher](#6-bảng-khuyến-mãi--voucher)
7. [Bảng Đánh Giá & Review](#7-bảng-đánh-giá--review)
8. [Bảng Chat & Thông Báo](#8-bảng-chat--thông-báo)
9. [Bảng Loyalty & Gamification](#9-bảng-loyalty--gamification)
10. [Bảng Giám Sát & Bảo Mật](#10-bảng-giám-sát--bảo-mật)
11. [Bảng Cửa Hàng](#11-bảng-cửa-hàng)
12. [Bảng Backup & Archive](#12-bảng-backup--archive)

---

## 1. BẢNG NGƯỜI DÙNG & XÁC THỰC

### users
```sql
CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) UNIQUE NOT NULL,
  email VARCHAR(100) UNIQUE NOT NULL,
  phone VARCHAR(50) UNIQUE,
  password VARCHAR(255) NOT NULL,
  full_name VARCHAR(100),
  address VARCHAR(255),
  role ENUM('USER', 'MANAGER', 'ADMIN') DEFAULT 'USER',
  member_tier ENUM('BRONZE', 'SILVER', 'GOLD', 'PLATINUM') DEFAULT 'BRONZE',
  points INT DEFAULT 0,
  active BOOLEAN DEFAULT TRUE,
  is_blocked BOOLEAN DEFAULT FALSE,
  avatar_url VARCHAR(255),
  otp VARCHAR(10),
  otp_expiry DATETIME,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  INDEX idx_username (username),
  INDEX idx_email (email),
  INDEX idx_phone (phone),
  INDEX idx_role (role),
  INDEX idx_member_tier (member_tier)
);
```

### manager_stores (Quản lý cửa hàng)
```sql
CREATE TABLE manager_stores (
  user_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, store_id),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE
);
```

---

## 2. BẢNG SẢN PHẨM & DANH MỤC

### drink_categories
```sql
CREATE TABLE drink_categories (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) UNIQUE NOT NULL,
  description TEXT,
  image_url VARCHAR(500),
  display_order INT DEFAULT 0,
  is_active BOOLEAN DEFAULT TRUE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  INDEX idx_name (name),
  INDEX idx_display_order (display_order)
);
```

### drinks
```sql
CREATE TABLE drinks (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  description TEXT,
  image_url VARCHAR(255),
  base_price DECIMAL(10,2) NOT NULL,
  category_id BIGINT,
  is_active BOOLEAN DEFAULT TRUE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  FOREIGN KEY (category_id) REFERENCES drink_categories(id),
  INDEX idx_name (name),
  INDEX idx_category_id (category_id),
  INDEX idx_is_active (is_active)
);
```

### drink_sizes
```sql
CREATE TABLE drink_sizes (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  drink_id BIGINT NOT NULL,
  size_name VARCHAR(50) NOT NULL,
  extra_price DECIMAL(10,2) DEFAULT 0.00,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  FOREIGN KEY (drink_id) REFERENCES drinks(id) ON DELETE CASCADE,
  INDEX idx_drink_id (drink_id)
);
```

### drink_toppings
```sql
CREATE TABLE drink_toppings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  drink_id BIGINT,
  topping_name VARCHAR(100) NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  is_active BOOLEAN DEFAULT TRUE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  FOREIGN KEY (drink_id) REFERENCES drinks(id) ON DELETE SET NULL,
  INDEX idx_drink_id (drink_id),
  INDEX idx_is_active (is_active)
);
```

---

## 3. BẢNG GIỎ HÀNG

### carts
```sql
CREATE TABLE carts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_user_id (user_id)
);
```

### cart_items
```sql
CREATE TABLE cart_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  cart_id BIGINT NOT NULL,
  drink_id BIGINT NOT NULL,
  size_id BIGINT,
  quantity INT NOT NULL,
  unit_price DOUBLE NOT NULL,
  total_price DOUBLE NOT NULL,
  price DECIMAL(10,2) DEFAULT 0.00,
  note VARCHAR(255),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  
  FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
  FOREIGN KEY (drink_id) REFERENCES drinks(id),
  FOREIGN KEY (size_id) REFERENCES drink_sizes(id),
  INDEX idx_cart_id (cart_id),
  INDEX idx_drink_id (drink_id)
);
```

### cart_item_toppings
```sql
CREATE TABLE cart_item_toppings (
  cart_item_id BIGINT NOT NULL,
  topping_id BIGINT NOT NULL,
  PRIMARY KEY (cart_item_id, topping_id),
  FOREIGN KEY (cart_item_id) REFERENCES cart_items(id) ON DELETE CASCADE,
  FOREIGN KEY (topping_id) REFERENCES drink_toppings(id) ON DELETE CASCADE
);
```

---

## 4. BẢNG ĐƠN HÀNG

### orders
```sql
CREATE TABLE orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  type ENUM('DELIVERY', 'PICKUP') NOT NULL,
  address VARCHAR(255),
  pickup_time DATETIME,
  status ENUM('PENDING', 'MAKING', 'SHIPPING', 'READY', 'DONE', 'CANCELED') DEFAULT 'PENDING',
  total_price DECIMAL(10,2) NOT NULL,
  shipping_fee DECIMAL(10,2) DEFAULT 0.00,
  discount DECIMAL(10,2) DEFAULT 0.00,
  final_price DECIMAL(10,2) NOT NULL,
  payment_method ENUM('COD', 'VNPAY', 'VIETQR', 'MOMO', 'PAYPAL') NOT NULL,
  promotion_id BIGINT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (store_id) REFERENCES stores(id),
  FOREIGN KEY (promotion_id) REFERENCES promotions(id),
  INDEX idx_user_id (user_id),
  INDEX idx_store_id (store_id),
  INDEX idx_status (status),
  INDEX idx_payment_method (payment_method),
  INDEX idx_created_at (created_at)
);
```

### order_items
```sql
CREATE TABLE order_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  drink_id BIGINT NOT NULL,
  drink_name_snapshot VARCHAR(100),
  size_name_snapshot VARCHAR(50),
  quantity INT NOT NULL,
  item_price DECIMAL(10,2) NOT NULL,
  note VARCHAR(255),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
  FOREIGN KEY (drink_id) REFERENCES drinks(id),
  INDEX idx_order_id (order_id),
  INDEX idx_drink_id (drink_id)
);
```

### order_item_toppings
```sql
CREATE TABLE order_item_toppings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_item_id BIGINT NOT NULL,
  topping_name_snapshot VARCHAR(100),
  price_snapshot DECIMAL(10,2) NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  FOREIGN KEY (order_item_id) REFERENCES order_items(id) ON DELETE CASCADE,
  INDEX idx_order_item_id (order_item_id)
);
```

---

## 5. BẢNG ĐẶT HÀNG NHÓM

### group_orders
```sql
CREATE TABLE group_orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  host_user_id BIGINT NOT NULL,
  invite_code VARCHAR(10) UNIQUE NOT NULL,
  name VARCHAR(100),
  status ENUM('OPEN', 'LOCKED', 'COMPLETED', 'CANCELLED', 'EXPIRED') DEFAULT 'OPEN',
  store_id BIGINT,
  order_type ENUM('DELIVERY', 'PICKUP'),
  delivery_address VARCHAR(255),
  expires_at DATETIME,
  max_members INT DEFAULT 10,
  final_order_id BIGINT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  FOREIGN KEY (host_user_id) REFERENCES users(id),
  FOREIGN KEY (store_id) REFERENCES stores(id),
  FOREIGN KEY (final_order_id) REFERENCES orders(id),
  INDEX idx_invite_code (invite_code),
  INDEX idx_status (status),
  INDEX idx_host_user_id (host_user_id)
);
```

### group_order_members
```sql
CREATE TABLE group_order_members (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  group_order_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  is_host BOOLEAN DEFAULT FALSE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  UNIQUE KEY unique_member (group_order_id, user_id),
  FOREIGN KEY (group_order_id) REFERENCES group_orders(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_group_order_id (group_order_id),
  INDEX idx_user_id (user_id)
);
```

### group_order_items
```sql
CREATE TABLE group_order_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  group_order_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  drink_id BIGINT NOT NULL,
  drink_name_snapshot VARCHAR(100) NOT NULL,
  size_name VARCHAR(20),
  quantity INT DEFAULT 1,
  unit_price DECIMAL(10,2) NOT NULL,
  item_price DECIMAL(10,2) NOT NULL,
  note VARCHAR(255),
  topping_ids VARCHAR(500),
  toppings_snapshot VARCHAR(500),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  FOREIGN KEY (group_order_id) REFERENCES group_orders(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (drink_id) REFERENCES drinks(id),
  INDEX idx_group_order_id (group_order_id),
  INDEX idx_user_id (user_id)
);
```

### group_chat_messages
```sql
CREATE TABLE group_chat_messages (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  group_order_id BIGINT NOT NULL,
  sender_id BIGINT NOT NULL,
  content VARCHAR(1000) NOT NULL,
  message_type ENUM('TEXT', 'SYSTEM', 'ITEM_ADDED', 'ITEM_REMOVED') DEFAULT 'TEXT',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  FOREIGN KEY (group_order_id) REFERENCES group_orders(id) ON DELETE CASCADE,
  FOREIGN KEY (sender_id) REFERENCES users(id),
  INDEX idx_group_order_id (group_order_id),
  INDEX idx_sender_id (sender_id),
  INDEX idx_created_at (created_at)
);
```


---

## 6. BẢNG KHUYẾN MÃI & VOUCHER

### promotions
```sql
CREATE TABLE promotions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(50) UNIQUE NOT NULL,
  description VARCHAR(255),
  discount_type ENUM('PERCENT', 'FIXED') NOT NULL,
  discount_value DECIMAL(10,2) NOT NULL,
  start_date DATETIME NOT NULL,
  end_date DATETIME NOT NULL,
  min_order_value DECIMAL(10,2) DEFAULT 0.00,
  max_discount_amount DECIMAL(10,2),
  min_order_amount DECIMAL(10,2) DEFAULT 0.00,
  usage_limit INT,
  used_count INT DEFAULT 0,
  is_active BOOLEAN DEFAULT TRUE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  INDEX idx_code (code),
  INDEX idx_is_active (is_active),
  INDEX idx_start_date (start_date),
  INDEX idx_end_date (end_date)
);
```

### promotion_usage
```sql
CREATE TABLE promotion_usage (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  promotion_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  order_id BIGINT,
  used_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  
  UNIQUE KEY unique_usage (promotion_id, user_id),
  FOREIGN KEY (promotion_id) REFERENCES promotions(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (order_id) REFERENCES orders(id),
  INDEX idx_promotion_id (promotion_id),
  INDEX idx_user_id (user_id),
  INDEX idx_used_at (used_at)
);
```

### spin_rewards
```sql
CREATE TABLE spin_rewards (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  voucher_code VARCHAR(10) UNIQUE NOT NULL,
  discount_percent INT NOT NULL,
  points_used INT DEFAULT 5,
  is_used BOOLEAN DEFAULT FALSE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_user_id (user_id),
  INDEX idx_is_used (is_used)
);
```

---

## 7. BẢNG ĐÁNH GIÁ & REVIEW

### reviews
```sql
CREATE TABLE reviews (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  drink_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  order_item_id BIGINT NOT NULL,
  rating INT NOT NULL,
  comment TEXT,
  is_anonymous BOOLEAN DEFAULT FALSE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  UNIQUE KEY unique_review (user_id, order_item_id),
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (drink_id) REFERENCES drinks(id),
  FOREIGN KEY (order_id) REFERENCES orders(id),
  FOREIGN KEY (order_item_id) REFERENCES order_items(id),
  INDEX idx_drink_id (drink_id),
  INDEX idx_rating (rating),
  INDEX idx_created_at (created_at)
);
```

---

## 8. BẢNG CHAT & THÔNG BÁO

### chat_conversations
```sql
CREATE TABLE chat_conversations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  manager_id BIGINT,
  store_id BIGINT,
  status ENUM('WAITING', 'ACTIVE', 'RESOLVED', 'CLOSED') DEFAULT 'WAITING',
  subject VARCHAR(255),
  last_message VARCHAR(500),
  unread_user INT DEFAULT 0,
  unread_manager INT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (manager_id) REFERENCES users(id),
  FOREIGN KEY (store_id) REFERENCES stores(id),
  INDEX idx_user_id (user_id),
  INDEX idx_manager_id (manager_id),
  INDEX idx_status (status),
  INDEX idx_created_at (created_at)
);
```

### live_chat_messages
```sql
CREATE TABLE live_chat_messages (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  conversation_id BIGINT NOT NULL,
  sender_id BIGINT NOT NULL,
  content VARCHAR(2000) NOT NULL,
  sender_type ENUM('USER', 'MANAGER', 'SYSTEM') NOT NULL,
  is_read BOOLEAN DEFAULT FALSE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  FOREIGN KEY (conversation_id) REFERENCES chat_conversations(id) ON DELETE CASCADE,
  FOREIGN KEY (sender_id) REFERENCES users(id),
  INDEX idx_conversation_id (conversation_id),
  INDEX idx_sender_id (sender_id),
  INDEX idx_is_read (is_read),
  INDEX idx_created_at (created_at)
);
```

### notifications
```sql
CREATE TABLE notifications (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(255) NOT NULL,
  content TEXT NOT NULL,
  type ENUM('ORDER_NEW', 'ORDER_STATUS', 'PAYMENT_SUCCESS', 'PROMOTION', 'SYSTEM', 'CUSTOM', 'LIVE_CHAT', 'GROUP_CHAT', 'SPIN_VOUCHER', 'CHALLENGE_COMPLETE', 'TIER_UPGRADE', 'SECURITY_ALERT', 'USER_BLOCKED', 'HIGH_RISK_USER') NOT NULL,
  is_read BOOLEAN DEFAULT FALSE,
  related_id BIGINT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_user_id (user_id),
  INDEX idx_type (type),
  INDEX idx_is_read (is_read),
  INDEX idx_created_at (created_at)
);
```

---

## 9. BẢNG LOYALTY & GAMIFICATION

### challenges
```sql
CREATE TABLE challenges (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  description VARCHAR(500),
  challenge_type ENUM('SAME_PRODUCT_IN_ORDER', 'MULTIPLE_ORDERS', 'TOTAL_SPENDING') NOT NULL,
  required_quantity INT NOT NULL,
  reward_points INT NOT NULL,
  is_active BOOLEAN DEFAULT TRUE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  INDEX idx_challenge_type (challenge_type),
  INDEX idx_is_active (is_active)
);
```

### challenge_completions
```sql
CREATE TABLE challenge_completions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  challenge_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  points_earned INT NOT NULL,
  drink_name VARCHAR(100),
  quantity_achieved INT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (challenge_id) REFERENCES challenges(id),
  FOREIGN KEY (order_id) REFERENCES orders(id),
  INDEX idx_user_id (user_id),
  INDEX idx_challenge_id (challenge_id),
  INDEX idx_created_at (created_at)
);
```

---

## 10. BẢNG GIÁM SÁT & BẢO MẬT

### user_activity_logs
```sql
CREATE TABLE user_activity_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  activity_type ENUM('LOGIN_SUCCESS', 'LOGIN_FAILED', 'LOGOUT', 'PASSWORD_CHANGE', 'PASSWORD_RESET_REQUEST', 'ORDER_CREATE', 'ORDER_VIEW', 'ORDER_CANCEL', 'ORDER_CANCEL_MULTIPLE', 'ORDER_STATUS_UPDATE', 'PAYMENT_SUCCESS', 'PAYMENT_FAILED', 'PAYMENT_FAILED_MULTIPLE', 'PROMOTION_USE', 'PROMOTION_ABUSE_ATTEMPT', 'CART_ADD_ITEM', 'CART_REMOVE_ITEM', 'CART_UPDATE_QUANTITY', 'CART_CLEAR', 'PRODUCT_VIEW', 'PRODUCT_SEARCH', 'PROFILE_VIEW', 'PROFILE_UPDATE', 'AVATAR_UPDATE', 'RATE_LIMIT_HIT', 'SPAM_REQUEST', 'ACCOUNT_BLOCKED', 'ACCOUNT_UNBLOCKED', 'BRUTE_FORCE_ATTEMPT', 'UNUSUAL_LOCATION', 'DEVICE_CHANGE', 'GROUP_ORDER_CREATE', 'GROUP_ORDER_JOIN', 'GROUP_ORDER_LEAVE', 'GROUP_ORDER_CHAT', 'LIVE_CHAT_START', 'LIVE_CHAT_MESSAGE', 'API_ERROR', 'SECURITY_VIOLATION') NOT NULL,
  description VARCHAR(500),
  risk_level ENUM('NORMAL', 'WARNING', 'SUSPICIOUS', 'CRITICAL') DEFAULT 'NORMAL',
  ip_address VARCHAR(50),
  device_info VARCHAR(255),
  user_agent VARCHAR(500),
  endpoint VARCHAR(255),
  request_method VARCHAR(10),
  response_status INT,
  related_id BIGINT,
  extra_data TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  
  FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_activity_user_id (user_id),
  INDEX idx_activity_type (activity_type),
  INDEX idx_activity_risk_level (risk_level),
  INDEX idx_activity_created_at (created_at)
);
```

### user_risk_scores
```sql
CREATE TABLE user_risk_scores (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT UNIQUE NOT NULL,
  total_score INT DEFAULT 0,
  risk_level ENUM('NORMAL', 'WARNING', 'SUSPICIOUS', 'CRITICAL') DEFAULT 'NORMAL',
  login_failed_count INT DEFAULT 0,
  order_cancel_count INT DEFAULT 0,
  payment_failed_count INT DEFAULT 0,
  rate_limit_hit_count INT DEFAULT 0,
  promotion_abuse_count INT DEFAULT 0,
  spam_request_count INT DEFAULT 0,
  last_score_reset DATETIME,
  admin_note TEXT,
  noted_by BIGINT,
  noted_at DATETIME,
  auto_blocked BOOLEAN DEFAULT FALSE,
  auto_blocked_at DATETIME,
  auto_blocked_reason VARCHAR(500),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (noted_by) REFERENCES users(id),
  INDEX idx_risk_level (risk_level),
  INDEX idx_auto_blocked (auto_blocked)
);
```

### blocked_ips
```sql
CREATE TABLE blocked_ips (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  ip_address VARCHAR(50) NOT NULL,
  block_type ENUM('TEMPORARY', 'PERMANENT', 'AUTO') NOT NULL,
  reason VARCHAR(255),
  blocked_by_id BIGINT,
  blocked_until DATETIME,
  is_active BOOLEAN DEFAULT TRUE,
  unblocked_at DATETIME,
  unblocked_by_id BIGINT,
  unblock_reason VARCHAR(255),
  alert_id BIGINT,
  related_user_id BIGINT,
  blocked_requests_count BIGINT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  
  FOREIGN KEY (blocked_by_id) REFERENCES users(id),
  FOREIGN KEY (unblocked_by_id) REFERENCES users(id),
  FOREIGN KEY (related_user_id) REFERENCES users(id),
  INDEX idx_ip_address (ip_address),
  INDEX idx_is_active (is_active),
  INDEX idx_block_type (block_type)
);
```

### whitelisted_ips
```sql
CREATE TABLE whitelisted_ips (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  ip_address VARCHAR(50) NOT NULL,
  description VARCHAR(255),
  added_by_id BIGINT,
  is_active BOOLEAN DEFAULT TRUE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  FOREIGN KEY (added_by_id) REFERENCES users(id),
  INDEX idx_ip_address (ip_address),
  INDEX idx_is_active (is_active)
);
```

**Mô tả bảng whitelisted_ips:**
- **ip_address**: Địa chỉ IP được phép truy cập với quyền Admin/Manager
- **description**: Mô tả IP (VD: "IP văn phòng", "IP nhà quản lý")
- **added_by_id**: ID của Admin đã thêm IP này
- **is_active**: Trạng thái hoạt động của IP trong whitelist

**Lưu ý:**
- Khi `whitelist.check.enabled=true` trong application.properties:
  - Admin/Manager chỉ có thể truy cập từ IP trong whitelist
  - User thường KHÔNG bị ảnh hưởng bởi whitelist
- Whitelist IP khác với Blocked IP:
  - Blocked IP: Chặn IP không cho truy cập
  - Whitelist IP: Chỉ cho phép IP trong danh sách truy cập với quyền cao

### monitoring_alerts
```sql
CREATE TABLE monitoring_alerts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  alert_type ENUM('LOGIN_ANOMALY', 'ORDER_ABUSE', 'PAYMENT_FRAUD', 'PROMOTION_ABUSE', 'RATE_LIMIT_EXCEEDED', 'SPAM_DETECTED', 'BRUTE_FORCE', 'HIGH_RISK_SCORE', 'AUTO_BLOCKED', 'SECURITY_VIOLATION') NOT NULL,
  severity ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL,
  title VARCHAR(255) NOT NULL,
  message TEXT NOT NULL,
  status ENUM('PENDING', 'REVIEWING', 'RESOLVED', 'DISMISSED') DEFAULT 'PENDING',
  handled_by BIGINT,
  handled_at DATETIME,
  handler_note TEXT,
  action_taken ENUM('NONE', 'WARNING_SENT', 'TEMP_BLOCKED', 'PERM_BLOCKED', 'MONITORED', 'FALSE_POSITIVE'),
  activity_log_id BIGINT,
  notification_sent BOOLEAN DEFAULT FALSE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (handled_by) REFERENCES users(id),
  INDEX idx_alert_user_id (user_id),
  INDEX idx_alert_severity (severity),
  INDEX idx_alert_status (status),
  INDEX idx_alert_created_at (created_at)
);
```

---

## 11. BẢNG CỬA HÀNG

### stores
```sql
CREATE TABLE stores (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  store_name VARCHAR(100) NOT NULL,
  address VARCHAR(255) NOT NULL,
  latitude DECIMAL(10,7),
  longitude DECIMAL(10,7),
  open_time TIME,
  close_time TIME,
  phone VARCHAR(20),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  INDEX idx_store_name (store_name),
  INDEX idx_address (address)
);
```

---

## 12. BẢNG BACKUP & ARCHIVE

### deleted_user_order_backup
```sql
CREATE TABLE deleted_user_order_backup (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  deleted_user_id BIGINT,
  deleted_username VARCHAR(100),
  deleted_user_phone VARCHAR(255),
  original_order_id BIGINT,
  store_id BIGINT,
  order_type ENUM('DELIVERY', 'PICKUP'),
  order_status ENUM('PENDING', 'MAKING', 'SHIPPING', 'READY', 'DONE', 'CANCELED'),
  total_price DECIMAL(10,2),
  discount DECIMAL(10,2),
  final_price DECIMAL(10,2),
  payment_method ENUM('COD', 'VNPAY', 'VIETQR', 'MOMO', 'PAYPAL'),
  order_created_at DATETIME,
  backup_created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  order_items_json TEXT,
  note VARCHAR(500),
  
  FOREIGN KEY (store_id) REFERENCES stores(id),
  INDEX idx_deleted_user_id (deleted_user_id),
  INDEX idx_original_order_id (original_order_id),
  INDEX idx_backup_created_at (backup_created_at)
);
```

### deleted_user_review_backup
```sql
CREATE TABLE deleted_user_review_backup (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  deleted_user_id BIGINT,
  deleted_username VARCHAR(100),
  deleted_user_fullname VARCHAR(255),
  original_review_id BIGINT,
  drink_id BIGINT,
  drink_name VARCHAR(255),
  order_id BIGINT,
  order_item_id BIGINT,
  rating INT NOT NULL,
  comment TEXT,
  is_anonymous BOOLEAN,
  review_created_at DATETIME,
  backup_created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  
  INDEX idx_deleted_user_id (deleted_user_id),
  INDEX idx_original_review_id (original_review_id),
  INDEX idx_drink_id (drink_id),
  INDEX idx_backup_created_at (backup_created_at)
);
```

---

## 📊 TÓNG KẾT SCHEMA

**Tổng số bảng: 31+**

### Phân loại:
- **Người dùng & Xác thực**: 2 bảng
- **Sản phẩm & Danh mục**: 4 bảng
- **Giỏ hàng**: 3 bảng
- **Đơn hàng**: 3 bảng
- **Đặt hàng nhóm**: 4 bảng
- **Khuyến mãi**: 3 bảng
- **Đánh giá**: 1 bảng
- **Chat & Thông báo**: 3 bảng
- **Loyalty**: 2 bảng
- **Giám sát & Bảo mật**: 5 bảng (bao gồm whitelisted_ips)
- **Cửa hàng**: 1 bảng
- **Backup**: 2 bảng

### Các Enum Types:
- **UserRole**: USER, MANAGER, ADMIN
- **MemberTier**: BRONZE, SILVER, GOLD, PLATINUM
- **OrderStatus**: PENDING, MAKING, SHIPPING, READY, DONE, CANCELED
- **OrderType**: DELIVERY, PICKUP
- **PaymentMethod**: COD, VNPAY, VIETQR, MOMO, PAYPAL
- **DiscountType**: PERCENT, FIXED
- **GroupOrderStatus**: OPEN, LOCKED, COMPLETED, CANCELLED, EXPIRED
- **ConversationStatus**: WAITING, ACTIVE, RESOLVED, CLOSED
- **NotificationType**: 13 loại
- **ChallengeType**: SAME_PRODUCT_IN_ORDER, MULTIPLE_ORDERS, TOTAL_SPENDING
- **RiskLevel**: NORMAL, WARNING, SUSPICIOUS, CRITICAL
- **AlertType**: 10 loại
- **AlertSeverity**: LOW, MEDIUM, HIGH, CRITICAL
- **AlertStatus**: PENDING, REVIEWING, RESOLVED, DISMISSED
- **ActionTaken**: 6 loại
- **SenderType**: USER, MANAGER, SYSTEM
- **GroupChatMessageType**: TEXT, SYSTEM, ITEM_ADDED, ITEM_REMOVED

### Relationships:
- **One-to-Many**: User → Orders, User → Carts, Drink → Sizes/Toppings, Order → OrderItems, GroupOrder → Members/Items/Messages
- **Many-to-Many**: User ↔ Stores (manager_stores), CartItem ↔ Toppings, PromotionUsage
- **One-to-One**: User ↔ UserRiskScore, Cart ↔ User