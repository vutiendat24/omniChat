# Module: M12 - Notification

## Danh sách chức năng

### 1. Gửi thông báo trong ứng dụng
- **Mã chức năng:** MOD-NOTIF-01
- **Tên chức năng:** Gửi thông báo In-app (In-app Notification)
- **Mô tả ngắn:** Tiếp nhận các sự kiện hệ thống (hội thoại mới, SLA sắp trễ, được gán việc) và chuyển đổi thành thông báo hiển thị trực tiếp trên giao diện chuông báo của Web/App thông qua module Realtime Delivery (M10).
- **Actor:** Hệ thống (System).

### 2. Gửi thông báo qua Email
- **Mã chức năng:** MOD-NOTIF-02
- **Tên chức năng:** Gửi thông báo Email (Email Notification)
- **Mô tả ngắn:** Tự động gửi email chứa các cảnh báo quan trọng (tài khoản vi phạm, SLA trễ diện rộng, gửi báo cáo hàng tuần) tới địa chỉ email của Agent hoặc Admin.
- **Actor:** Hệ thống (System).

### 3. Gửi thông báo đẩy Web/Mobile
- **Mã chức năng:** MOD-NOTIF-03
- **Tên chức năng:** Gửi thông báo đẩy (Web/Mobile Push Notification)
- **Mô tả ngắn:** Đẩy cảnh báo ra ngoài màn hình thiết bị (thông qua trình duyệt hoặc OS) ngay cả khi Agent đang thu nhỏ trình duyệt để đảm bảo không bỏ lỡ tin nhắn khách hàng.
- **Actor:** Hệ thống (System).

### 4. Cấu hình nhận thông báo cá nhân
- **Mã chức năng:** MOD-NOTIF-04
- **Tên chức năng:** Cấu hình tùy chọn thông báo (Notification Preferences)
- **Mô tả ngắn:** Cho phép từng người dùng (Agent/Admin) bật, tắt các loại sự kiện nhận thông báo và tùy chỉnh kênh nhận (nhận qua In-app, nhận qua Email, hay nhận qua Push) để tránh bị làm phiền.
- **Actor:** Agent, Admin, Owner.

### 5. Quản lý mẫu thông báo
- **Mã chức năng:** MOD-NOTIF-05
- **Tên chức năng:** Quản lý Mẫu thông báo (Notification Templates)
- **Mô tả ngắn:** Cho phép quản trị viên hệ thống định nghĩa và tùy chỉnh các tiêu đề, nội dung mẫu cho email hoặc thông báo (hỗ trợ chèn các biến động như tên khách hàng, tên kênh).
- **Actor:** Platform Admin, Owner.
