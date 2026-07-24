# Module: M01 - Identity & Access

## Danh sách chức năng

### 1. Đăng ký tài khoản
- **Mã chức năng:** MOD-IAM-01
- **Tên chức năng:** Đăng ký tài khoản hệ thống (User Registration)
- **Mô tả ngắn:** Cho phép người dùng mới tạo tài khoản trên hệ thống bằng Email và Mật khẩu, kèm theo quy trình xác thực email.
- **Actor:** Người dùng vô danh (Guest) / Chủ shop tương lai.

### 2. Đăng nhập truyền thống
- **Mã chức năng:** MOD-IAM-02
- **Tên chức năng:** Đăng nhập hệ thống (Local Login)
- **Mô tả ngắn:** Xác thực người dùng thông qua Email và Mật khẩu đã đăng ký, nếu thành công sẽ trả về bộ Access Token & Refresh Token (JWT).
- **Actor:** Người dùng (Người dùng thông thường, Agent, Quản lý).

### 3. Đăng nhập bằng Google SSO
- **Mã chức năng:** MOD-IAM-03
- **Tên chức năng:** Đăng nhập qua Google (Google OAuth2 SSO)
- **Mô tả ngắn:** Cho phép người dùng đăng nhập hoặc đăng ký tài khoản nhanh chóng thông qua tài khoản Google thay vì dùng mật khẩu.
- **Actor:** Người dùng.

### 4. Quản lý Token (JWT)
- **Mã chức năng:** MOD-IAM-04
- **Tên chức năng:** Làm mới Token (Refresh JWT)
- **Mô tả ngắn:** Sử dụng Refresh Token còn hạn để cấp đổi một Access Token mới mà không bắt người dùng phải đăng nhập lại, đảm bảo phiên làm việc xuyên suốt.
- **Actor:** Hệ thống Web/App (Frontend Client).

### 5. Đăng xuất và Thu hồi Token
- **Mã chức năng:** MOD-IAM-05
- **Tên chức năng:** Đăng xuất (Logout & Blacklist Token)
- **Mô tả ngắn:** Chấm dứt phiên làm việc của người dùng bằng cách hủy bỏ Refresh Token hiện tại và đưa Access Token vào danh sách đen (Blacklist) để không thể sử dụng tiếp.
- **Actor:** Người dùng.

### 6. Quản lý Vai trò
- **Mã chức năng:** MOD-IAM-06
- **Tên chức năng:** Quản lý Vai trò (Role Management)
- **Mô tả ngắn:** Khởi tạo, cập nhật và xóa các vai trò (Role) trong hệ thống (VD: Super Admin, Shop Owner, Agent) làm cơ sở để phân quyền.
- **Actor:** Super Admin.

### 7. Phân quyền RBAC
- **Mã chức năng:** MOD-IAM-07
- **Tên chức năng:** Quản lý Phân quyền (Permission / RBAC Management)
- **Mô tả ngắn:** Định nghĩa các quyền hạn cụ thể (Permissions) và gán các quyền này vào từng Vai trò (Role) để kiểm soát quyền truy cập tài nguyên.
- **Actor:** Super Admin.

### 8. Lấy thông tin cá nhân
- **Mã chức năng:** MOD-IAM-08
- **Tên chức năng:** Lấy thông tin tài khoản hiện tại (Get Current Profile / Introspect)
- **Mô tả ngắn:** Giải mã JWT Token và truy xuất thông tin chi tiết của user đang đăng nhập (Tên, Email, Vai trò, Quyền hạn) để hiển thị lên giao diện.
- **Actor:** Người dùng / Hệ thống Frontend.
