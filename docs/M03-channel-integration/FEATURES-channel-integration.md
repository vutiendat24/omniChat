# Module: M03 - Channel Integration

## Danh sách chức năng

### 1. Kết nối kênh mạng xã hội
- **Mã chức năng:** MOD-CI-01
- **Tên chức năng:** Kết nối kênh (OAuth2 Connect)
- **Mô tả ngắn:** Thực hiện luồng OAuth2 để cấp quyền và kết nối các trang/kênh tin nhắn 1-1 (Facebook Messenger, Zalo OA, TikTok, Instagram DM) của shop vào hệ thống.
- **Actor:** Quản lý shop / Quản trị viên (Admin)

### 2. Ngắt kết nối kênh
- **Mã chức năng:** MOD-CI-02
- **Tên chức năng:** Ngắt kết nối (Disconnect Channel)
- **Mô tả ngắn:** Hủy bỏ kết nối của một kênh mạng xã hội hiện tại trên hệ thống và vô hiệu hóa access token tương ứng để dừng việc đồng bộ tin nhắn.
- **Actor:** Quản lý shop / Quản trị viên (Admin)

### 3. Tự động làm mới Token
- **Mã chức năng:** MOD-CI-03
- **Tên chức năng:** Tự động làm mới Token (Auto-refresh Token)
- **Mô tả ngắn:** Tiến trình chạy ngầm định kỳ để kiểm tra và tự động làm mới access token của các kênh đã kết nối trước khi hết hạn, giúp duy trì kết nối ổn định.
- **Actor:** Hệ thống (System/Cronjob)

### 4. Tiếp nhận Webhook Inbound
- **Mã chức năng:** MOD-CI-04
- **Tên chức năng:** Tiếp nhận Webhook (Receive Webhook)
- **Mô tả ngắn:** Cung cấp các API endpoint để trực tiếp nhận dữ liệu sự kiện realtime (tin nhắn đến, thay đổi trạng thái gửi...) do nền tảng mạng xã hội đẩy về.
- **Actor:** Nền tảng mạng xã hội (Facebook, Zalo, TikTok...)

### 5. Xác thực Webhook
- **Mã chức năng:** MOD-CI-05
- **Tên chức năng:** Xác thực Webhook (Verify Webhook Signature)
- **Mô tả ngắn:** Kiểm tra chữ ký bảo mật (signature validation) của payload từ nền tảng mạng xã hội gửi đến nhằm đảm bảo nguồn gốc webhook là hợp lệ và an toàn.
- **Actor:** Hệ thống (System)

### 6. Chuẩn hóa tin nhắn Inbound
- **Mã chức năng:** MOD-CI-06
- **Tên chức năng:** Chuẩn hóa dữ liệu (Inbound Normalization)
- **Mô tả ngắn:** Chuyển đổi dữ liệu thô (raw payload) mang tính đặc thù của từng nền tảng thành một định dạng tin nhắn chuẩn chung (unified format) của OmniChat.
- **Actor:** Hệ thống (System)

### 7. Gửi tin nhắn Outbound
- **Mã chức năng:** MOD-CI-07
- **Tên chức năng:** Gửi tin nhắn (Outbound Delivery)
- **Mô tả ngắn:** Tiếp nhận định dạng tin nhắn chuẩn từ các module bên trong hệ thống, chuyển đổi sang cấu trúc riêng của nền tảng đích và gọi API để gửi tin nhắn đến người dùng cuối.
- **Actor:** Agent, Hệ thống (thông qua module Conversation & Inbox)
