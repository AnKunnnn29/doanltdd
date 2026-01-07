# CẬP NHẬT CHỨC NĂNG GROUP ORDER

## Ngày cập nhật: $(date)

### Các thay đổi đã thực hiện:

#### 1. Cải tiến AddToGroupOrderActivity
- **Thêm chọn topping**: Người dùng có thể chọn nhiều topping cho đồ uống
- **Thêm chọn voucher**: Tích hợp hệ thống voucher (cả voucher thường và spin voucher)
- **Cải thiện UI**: Thêm các view mới cho topping và voucher selection

#### 2. Cập nhật Model
- **AddGroupOrderItemRequest**: Thêm fields `promotionCode` và `spinVoucherCode`
- Hỗ trợ gửi thông tin voucher khi thêm item vào group order

#### 3. Cập nhật Layout
- **activity_add_to_group_order.xml**: 
  - Thêm LinearLayout cho toppings
  - Thêm Button và TextView cho voucher selection
  - Cải thiện layout structure

#### 4. Tính năng mới:
- **Chọn topping**: CheckBox cho từng topping với giá
- **Chọn voucher**: Dialog hiển thị cả voucher thường và spin voucher
- **Tính giá tự động**: Cập nhật giá theo size, topping, và số lượng
- **Hiển thị voucher**: Hiển thị voucher đã chọn với thông tin giảm giá

### Luồng hoạt động mới:

1. **Vào AddToGroupOrderActivity**
2. **Chọn size** (như cũ)
3. **Chọn topping** (mới) - có thể chọn nhiều
4. **Chọn số lượng** (như cũ)
5. **Chọn voucher** (mới) - hiển thị dialog với voucher available
6. **Nhập ghi chú** (như cũ)
7. **Thêm vào đơn nhóm** - gửi kèm thông tin topping và voucher

### API Changes:
- AddGroupOrderItemRequest bây giờ bao gồm:
  - `toppingIds: List<Long>?`
  - `promotionCode: String?`
  - `spinVoucherCode: String?`

### Files đã thay đổi:
1. `AddToGroupOrderActivity.kt` - Thêm logic chọn topping và voucher
2. `GroupOrderModels.kt` - Cập nhật AddGroupOrderItemRequest
3. `activity_add_to_group_order.xml` - Thêm UI elements

### Lưu ý:
- Voucher được áp dụng per-item thay vì per-order (để demo)
- Trong thực tế, voucher thường áp dụng ở checkout level
- Backend cần hỗ trợ các field mới trong AddGroupOrderItemRequest