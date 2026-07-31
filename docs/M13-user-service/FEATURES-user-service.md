# Module: M13 - User Service

> 📖 **Tài liệu tham khảo:** Xem thêm [Chiến lược quản lý tài khoản (Account Management Strategy)](ACCOUNT_MANAGEMENT_STRATEGY.md) để hiểu rõ về phân cấp Role (Hierarchy) và RBAC.

## Danh sách chức năng

### 1. Quản lý Hồ sơ người dùng
- **Mã chức năng:** MOD-USR-01
- **Tên chức năng:** Quản lý Hồ sơ cá nhân (User Profile Management)
- **Mô tả ngắn:** Cập nhật thông tin cá nhân (Tên, Ảnh đại diện, Đổi mật khẩu).
- **Actor:** Tất cả người dùng.

### 2. Quản lý Thành viên Workspace
- **Mã chức năng:** MOD-USR-02
- **Tên chức năng:** Quản lý Thành viên (Workspace Member Management)
- **Mô tả ngắn:** Mời, thêm, khóa, và xóa (soft-delete) các thành viên trong Tenant dựa trên Role Hierarchy.
- **Actor:** Owner, Admin, Manager (Tùy thuộc vào Role Hierarchy).

### 3. Quản lý Vai trò
- **Mã chức năng:** MOD-USR-03
- **Tên chức năng:** Quản lý Vai trò (Role Management)
- **Mô tả ngắn:** Khởi tạo, cập nhật, đổi tên và xóa các vai trò (Role) trong hệ thống làm cơ sở để phân quyền. Thiết lập Level cho Role Hierarchy.
- **Actor:** Platform Admin, Owner.

### 4. Phân quyền RBAC
- **Mã chức năng:** MOD-USR-04
- **Tên chức năng:** Quản lý Phân quyền (Permission / RBAC Management)
- **Mô tả ngắn:** Định nghĩa các quyền hạn cụ thể (Permissions) và gán các quyền này vào từng Vai trò (Role).
- **Actor:** Platform Admin, Owner.

### 5. Gán Quyền (Assign Role)
- **Mã chức năng:** MOD-USR-05
- **Tên chức năng:** Gán và Đổi Vai trò (Assign/Change Role)
- **Mô tả ngắn:** Cấp hoặc thay đổi Role cho một thành viên. Chỉ có thể gán Role có cấp độ (Level) thấp hơn Level của người thao tác.
- **Actor:** Owner, Admin, Manager.

### 6. Chuyển quyền sở hữu (Transfer Ownership)
- **Mã chức năng:** MOD-USR-06
- **Tên chức năng:** Chuyển quyền Chủ sở hữu (Transfer Ownership)
- **Mô tả ngắn:** Owner hiện tại chuyển quyền sở hữu toàn bộ Tenant cho một Admin khác. Sau khi chuyển, Owner cũ sẽ bị hạ cấp thành Admin.
- **Actor:** Owner.
