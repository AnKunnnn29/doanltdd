📋 BẢNG CHỨC NĂNG HỆ THỐNG UTE TEA (TỪ LỚN → NHỎ)
________________________________________
1️⃣ HỆ THỐNG XÁC THỰC & BẢO MẬT
CẤP	NHÓM CHỨC NĂNG	CHỨC NĂNG CHI TIẾT
1	Xác thực người dùng	Đăng nhập bằng username/email/sđt
		JWT Authentication
		Auto Refresh Token
		Phân quyền User / Manager / Admin
2	Đăng nhập nâng cao	Đăng nhập sinh trắc học (Biometric)
		Mã hóa token bằng Android Keystore
		Lưu session an toàn
3	Đăng ký & OTP	Đăng ký tài khoản
		Gửi OTP qua Email/SMS
		Xác thực OTP
		Resend OTP
4	Quên mật khẩu	OTP reset mật khẩu
		Nhập OTP + mật khẩu mới
5	Xác thực thanh toán	OTP xác thực số điện thoại khi checkout
________________________________________
2️⃣ QUẢN LÝ TÀI KHOẢN NGƯỜI DÙNG
CẤP	NHÓM CHỨC NĂNG	CHỨC NĂNG CHI TIẾT
1	Hồ sơ cá nhân	Xem thông tin tài khoản
		Cập nhật tên, email, sđt, địa chỉ
2	Ảnh đại diện	Chọn ảnh từ thư viện
		Nén ảnh
		Upload avatar
3	Bảo mật tài khoản	Đổi mật khẩu
		Xóa tài khoản
		Đăng xuất
________________________________________
3️⃣ TRANG CHỦ & TRẢI NGHIỆM NGƯỜI DÙNG
CẤP	NHÓM CHỨC NĂNG	CHỨC NĂNG CHI TIẾT
1	Trang chủ	Banner
		Best Seller
		For You (gợi ý cá nhân)
		Quick Actions
2	Cá nhân hóa	Hiển thị thời tiết
		Seasonal Effects theo mùa
3	Navigation	Bottom Navigation
		Drawer Menu
		Search Bar
________________________________________
4️⃣ AI & TƯƠNG TÁC THÔNG MINH
CẤP	NHÓM CHỨC NĂNG	CHỨC NĂNG CHI TIẾT
1	AI Predictive Order	Phân tích lịch sử đặt hàng
		Phân tích thời gian
		Phân tích thời tiết
		Gợi ý món + độ tin cậy
2	Voice Interaction	Voice Order
		Voice Search
		Chuẩn hóa tiếng Việt
3	Chatbot AI	Trả lời tự động
		Gợi ý sản phẩm
		Hướng dẫn đặt hàng
________________________________________
5️⃣ QUẢN LÝ SẢN PHẨM & THỰC ĐƠN
CẤP	NHÓM CHỨC NĂNG	CHỨC NĂNG CHI TIẾT
1	Thực đơn	Hiển thị sản phẩm
		Phân loại theo danh mục
		Sắp xếp theo giá
		Load more / Pagination
2	Tìm kiếm	Tìm kiếm realtime
		Gợi ý từ khóa
		Fuzzy matching
		Bỏ dấu tiếng Việt
3	Chi tiết sản phẩm	Chọn size
		Chọn topping
		Tính giá realtime
		Animation add-to-cart
		Xem review
4	Quản lý danh mục	CRUD danh mục sản phẩm
		Upload ảnh danh mục
		Sắp xếp thứ tự hiển thị
________________________________________
6️⃣ GIỎ HÀNG & THANH TOÁN
CẤP	NHÓM CHỨC NĂNG	CHỨC NĂNG CHI TIẾT
1	Giỏ hàng	Thêm / xóa sản phẩm
		Cập nhật số lượng
		Select / Unselect
		Đặt lại từ lịch sử đơn hàng
2	Giảm giá	Áp voucher
		Voucher Spin Wheel
		Tier Discount
3	Phí giao hàng	Tính phí theo khoảng cách
		Tích hợp GHN API
		Hiển thị chi tiết phí
		Free ship theo điều kiện
4	Phương thức thanh toán	COD (Tiền mặt khi nhận hàng)
		VNPay (Thanh toán online)
		MoMo (Ví điện tử)
		PayPal (Thanh toán quốc tế)
		VietQR (Quét mã QR)
5	Checkout	Tính tổng tiền (bao gồm phí ship)
		OTP xác thực
		Tạo đơn hàng
		Bill Preview trước thanh toán
________________________________________
7️⃣ ĐƠN HÀNG & THEO DÕI REALTIME
CẤP	NHÓM CHỨC NĂNG	CHỨC NĂNG CHI TIẾT
1	Trạng thái đơn	PENDING / MAKING
		SHIPPING / READY / DONE
		CANCELLED / REFUNDED
2	Realtime	WebSocket cập nhật trạng thái
		Push notification trạng thái
3	Lịch sử đơn	Filter trạng thái
		Smart sorting
		Pagination
		Đặt lại đơn hàng (Reorder)
4	Chi tiết đơn hàng	Xem thông tin đầy đủ
		Tracking timeline
		Liên hệ hỗ trợ
		Nút "Đặt lại" đơn hàng
5	Reorder (Đặt lại)	Load lại cart với size/topping/note y hệt
		Kiểm tra món/topping còn bán
		Gợi ý thay thế nếu ngưng bán
		Thông báo chi tiết kết quả
________________________________________
8️⃣ ĐẶT HÀNG NHÓM (GROUP ORDER)
CẤP	NHÓM CHỨC NĂNG	CHỨC NĂNG CHI TIẾT
1	Quản lý nhóm	Tạo nhóm
		Invite Code
		Join nhóm
2	Phiên nhóm	Thêm món chung
		Chat nhóm realtime
		Lock / Unlock
		Checkout nhóm
3	Chia sẻ	Share invite link
		QR Code join
		Notification thành viên
________________________________________
9️⃣ LOYALTY & GAMIFICATION
CẤP	NHÓM CHỨC NĂNG	CHỨC NĂNG CHI TIẾT
1	Loyalty	Tích điểm
		Hạng Bronze / Silver / Gold / Diamond
		Quy đổi điểm thành voucher
2	Spin Wheel	Quay vòng xoay
		Nhận voucher
		Lưu voucher
		Daily spin limit
3	Challenge System	Challenge đặt hàng hàng ngày
		Challenge đặt hàng hàng tuần
		Challenge đặt hàng theo số lượng
		Thưởng điểm/voucher khi hoàn thành
		Theo dõi tiến độ challenge
		Leaderboard challenge
4	Member Tier	Tier benefits
		Upgrade requirements
		Exclusive offers
________________________________________
🔟 THÔNG BÁO & PUSH NOTIFICATION
CẤP	NHÓM CHỨC NĂNG	CHỨC NĂNG CHI TIẾT
1	Push Notification	Tích hợp OneSignal
		Gửi thông báo realtime
		Thông báo trạng thái đơn hàng
		Thông báo khuyến mãi
2	In-App Notification	Lưu thông báo trong app
		Đánh dấu đã đọc/chưa đọc
		Phân loại theo type
		Xóa thông báo
3	Email Notification	Gửi email xác nhận đơn hàng
		Email thanh toán thành công
		Email reset mật khẩu
		Tích hợp SendGrid
4	Notification Types	ORDER (đơn hàng)
		PROMOTION (khuyến mãi)
		SYSTEM (hệ thống)
		GROUP_CHAT (chat nhóm)
		LIVE_CHAT (chat hỗ trợ)
________________________________________
1️⃣1️⃣ LIVE CHAT & HỖ TRỢ
CẤP	NHÓM CHỨC NĂNG	CHỨC NĂNG CHI TIẾT
1	Live Chat User	Gửi / nhận tin realtime
		WebSocket connection
		Upload ảnh trong chat
2	Live Chat Manager	Nhận & xử lý hội thoại
		Đóng hội thoại
		Quản lý nhiều chat cùng lúc
		Transfer chat giữa manager
3	Chatbot AI	Trả lời tự động
		Gợi ý sản phẩm
		Hướng dẫn đặt hàng
		Escalate to human
________________________________________
1️⃣2️⃣ ĐÁNH GIÁ & REVIEW
CẤP	NHÓM CHỨC NĂNG	CHỨC NĂNG CHI TIẾT
1	Review sản phẩm	Đánh giá sao (1-5)
		Viết bình luận
		Upload ảnh review
2	Quản lý review	Xem tất cả đánh giá
		Phản hồi review
		Xóa review không phù hợp
3	Review Analytics	Thống kê rating trung bình
		Phân tích sentiment
		Top reviewed products
________________________________________
1️⃣3️⃣ QUẢN LÝ CỬA HÀNG
CẤP	NHÓM CHỨC NĂNG	CHỨC NĂNG CHI TIẾT
1	Thông tin cửa hàng	Tên, địa chỉ, số điện thoại
		Giờ mở cửa / đóng cửa
		Ảnh cửa hàng
2	Quản lý vị trí	GPS coordinates
		Bán kính giao hàng
		Khu vực phục vụ
3	Cài đặt vận hành	Công suất tối đa (đơn/giờ)
		Thời gian chuẩn bị
		Phí giao hàng theo khu vực
________________________________________
1️⃣4️⃣ QUẢN LÝ & ĐIỀU HÀNH (MANAGER)
CẤP	NHÓM CHỨC NĂNG	CHỨC NĂNG CHI TIẾT
1	Đơn hàng	Xem & cập nhật trạng thái
		Theo dõi realtime qua WebSocket
		In hóa đơn
2	Sản phẩm	Thêm / sửa / xóa
		Upload ảnh sản phẩm
		Quản lý danh mục
		Cập nhật giá, mô tả
3	Voucher	CRUD voucher
		Theo dõi lượt dùng
		Thiết lập điều kiện áp dụng
		Voucher analytics
4	Người dùng	Khóa / mở khóa
		Nâng user → manager
		Xem chi tiết hoạt động
		User behavior analysis
5	Cửa hàng	Quản lý thông tin cửa hàng
		Cập nhật địa chỉ, giờ mở cửa
		Thiết lập khu vực giao hàng
6	Live Chat	Quản lý hội thoại
		Trả lời khách hàng
		Chat queue management
7	Thông báo	Gửi thông báo tới user
		Quản lý thông báo hệ thống
		Broadcast messages
8	Dashboard	Overview metrics
		Real-time statistics
		Performance indicators
________________________________________
1️⃣5️⃣ GIÁM SÁT & BẢO MẬT (ADMIN)
CẤP	NHÓM CHỨC NĂNG	CHỨC NĂNG CHI TIẾT
1	Monitoring	Ghi log hành vi
		Theo dõi IP
		User activity tracking
		System health monitoring
2	Risk Score	Tính điểm rủi ro
		Cảnh báo tự động
		Phát hiện hành vi bất thường
		Fraud detection
3	Phòng thủ	Block IP
		Rate limiting
		Chống spam/bot
		DDoS protection
4	Whitelist IP	Quản lý IP được phép truy cập Admin/Manager
		Thêm IP vào whitelist
		Xóa IP khỏi whitelist
		Kiểm tra IP có trong whitelist
		Lấy IP hiện tại của thiết bị
		Bật/tắt kiểm tra whitelist
5	User Management	Quản lý tất cả user
		Phân quyền Admin/Manager/User
		Khóa tài khoản nguy hiểm
		Bulk operations
6	System Config	Cấu hình hệ thống
		Feature flags
		Maintenance mode
		API rate limits
________________________________________
1️⃣6️⃣ DỰ BÁO & PHÂN TÍCH THÔNG MINH (MANAGER)
CẤP	NHÓM CHỨC NĂNG	CHỨC NĂNG CHI TIẾT
1	Dự báo doanh thu	Dự báo hôm nay (điều chỉnh theo tiến độ)
		Dự báo ngày mai và 7 ngày tới
		Dự báo tháng (30 ngày)
		Tỷ lệ tăng trưởng so với tuần trước
		Xu hướng: Tăng trưởng / Giảm / Ổn định
2	Phân tích giờ cao điểm	Thống kê đơn hàng theo từng giờ (8h-22h)
		Doanh thu trung bình theo giờ
		Mức độ cao điểm: LOW/MEDIUM/HIGH/VERY_HIGH
		Đề xuất số nhân viên cho mỗi khung giờ
3	Cảnh báo món sắp hết	Phát hiện món bán nhanh hơn bình thường
		Tốc độ bán (đơn/giờ) so với trung bình
		Mức cảnh báo: WARNING / CRITICAL
		Dự kiến hết hàng trong X giờ
4	Đề xuất nhân sự	Dự báo 7 ngày tới
		Số nhân viên đề xuất cho mỗi ngày
		Chi tiết theo từng khung giờ
		Highlight ngày cuối tuần (lượng đơn cao)
5	Cảnh báo quá tải	Phát hiện khung giờ vượt công suất
		% vượt công suất tối đa (50 đơn/giờ)
		Mức độ: WARNING / CRITICAL
		Đề xuất xử lý: tăng nhân viên + nguyên liệu
6	Dashboard thống kê	Biểu đồ doanh thu realtime
		Top sản phẩm bán chạy
		Thống kê khách hàng mới
		Phân tích theo thời tiết
7	Business Intelligence	Sales trends analysis
		Customer segmentation
		Product performance
		Seasonal patterns
________________________________________
1️⃣7️⃣ GỢI Ý THÔNG MINH (AI PREDICTIVE ORDER)
CẤP	NHÓM CHỨC NĂNG	CHỨC NĂNG CHI TIẾT
1	Phân tích hành vi	Lịch sử đặt hàng của user
		Phân tích thời gian (giờ, thứ trong tuần)
		Tần suất đặt món yêu thích
		Độ gần đây của đơn hàng
2	Thuật toán dự đoán	Score = TimeScore × 0.35 + DayScore × 0.25
		+ RecencyScore × 0.2 + FrequencyScore × 0.2
		Độ tin cậy tối thiểu 20% mới hiển thị
		Kết hợp với thời tiết (nóng → mát, lạnh → ấm)
3	Gợi ý thông minh	Dialog gợi ý món khi mở app
		Hiển thị lý do: "Bạn thường đặt vào buổi sáng"
		One-click thêm vào giỏ hàng
		Chống spam: không hiển thị lại trong 2 giờ
4	Machine Learning	Collaborative filtering
		Content-based filtering
		Hybrid recommendation
		A/B testing for recommendations
________________________________________
1️⃣8️⃣ TÍNH PHÍ GIAO HÀNG
CẤP	NHÓM CHỨC NĂNG	CHỨC NĂNG CHI TIẾT
1	Tích hợp GHN API	Tính phí theo khoảng cách thực tế
		Tích hợp Giao Hàng Nhanh API
		Tự động lấy phí ship theo địa chỉ
		Hiển thị thời gian giao hàng dự kiến
2	Điều kiện miễn phí	Free ship theo Member Tier
		SILVER: Miễn phí đơn từ 100K
		GOLD: Miễn phí đơn từ 50K
		PLATINUM: Miễn phí mọi đơn
3	Hiển thị chi tiết	Breakdown chi tiết phí giao hàng
		Hiển thị trong giỏ hàng và checkout
		Tính vào tổng tiền cuối cùng
4	Logistics	Multiple shipping providers
		Delivery time estimation
		Route optimization
		Delivery tracking
________________________________________
1️⃣9️⃣ TÍCH HỢP THANH TOÁN ONLINE
CẤP	NHÓM CHỨC NĂNG	CHỨC NĂNG CHI TIẾT
1	VNPay Integration	Thanh toán qua VNPay
		Xử lý callback payment
		Verify transaction
		Auto update order status
2	MoMo Integration	Thanh toán qua ví MoMo
		QR Code payment
		Deep link payment
		Transaction verification
3	PayPal Integration	Thanh toán quốc tế
		Multi-currency support
		Secure payment processing
		Refund handling
4	VietQR	Thanh toán qua QR Code
		Tích hợp ngân hàng Việt Nam
		Scan to pay
		Real-time verification
5	Payment Security	PCI DSS compliance
		Encryption
		Fraud detection
		Transaction monitoring
________________________________________
2️⃣0️⃣ QUẢN LÝ THỜI TIẾT & SEASONAL
CẤP	NHÓM CHỨC NĂNG	CHỨC NĂNG CHI TIẾT
1	Weather API	Lấy thông tin thời tiết realtime
		Hiển thị trên trang chủ
		Ảnh hưởng đến gợi ý món
		Weather-based notifications
2	Seasonal Effects	Hiệu ứng theo mùa
		Giao diện thay đổi theo thời tiết
		Gợi ý món phù hợp với thời tiết
		Seasonal promotions
3	Weather Forecast	Dự báo thời tiết cho manager
		Ảnh hưởng đến dự báo doanh thu
		Chuẩn bị nguyên liệu theo thời tiết
		Weather impact analysis
________________________________________
2️⃣1️⃣ TÍNH NĂNG BỔ SUNG
CẤP	NHÓM CHỨC NĂNG	CHỨC NĂNG CHI TIẾT
1	Multi-language	Hỗ trợ tiếng Việt/English
		Dynamic language switching
		Localized content
2	Offline Mode	Cache dữ liệu quan trọng
		Sync khi có internet
		Offline order queue
3	Accessibility	Screen reader support
		High contrast mode
		Font size adjustment
4	Performance	Image optimization
		Lazy loading
		Caching strategies
		Background sync
5	Analytics	User behavior tracking
		Conversion funnel
		A/B testing
		Performance metrics