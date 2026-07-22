# Danh sách chức năng: M02 - Tenant & Organization

**Mục tiêu:** Quản lý cấu trúc tổ chức multi-tenant: shop, team, cấu hình tenant.

## 1. Quản lý Tenant (Shop/Doanh nghiệp)

- **Mã chức năng:** MOD-TENANT-01
- **Tên chức năng:** Tạo mới Tenant (Onboarding)
- **Mô tả ngắn:** Khởi tạo một không gian làm việc (workspace) hoàn toàn độc lập cho một khách hàng, cửa hàng hoặc doanh nghiệp mới sử dụng nền tảng.
- **Actor:** Super Admin / System (tự động kích hoạt khi khách hàng đăng ký thành công).

- **Mã chức năng:** MOD-TENANT-02
- **Tên chức năng:** Cập nhật hồ sơ Tenant
- **Mô tả ngắn:** Chỉnh sửa các thông tin cơ bản của shop như tên thương hiệu, logo, ngành nghề kinh doanh, và thông tin liên hệ.
- **Actor:** Tenant Admin / Super Admin.

- **Mã chức năng:** MOD-TENANT-03
- **Tên chức năng:** Quản lý trạng thái Tenant
- **Mô tả ngắn:** Cho phép vô hiệu hóa (tạm khóa) hoặc kích hoạt lại một shop, thường áp dụng khi shop hết hạn thanh toán (subscription) hoặc vi phạm chính sách.
- **Actor:** Super Admin.

## 2. Quản lý Team (Nhóm/Phòng ban)

- **Mã chức năng:** MOD-TENANT-04
- **Tên chức năng:** Tạo mới Team
- **Mô tả ngắn:** Phân chia nhân sự trong shop thành các nhóm nhỏ (VD: Nhóm CSKH Ca Sáng, Nhóm Chốt Đơn Livestream) để thuận tiện cho việc quản lý và phân bổ hội thoại tự động.
- **Actor:** Tenant Admin.

- **Mã chức năng:** MOD-TENANT-05
- **Tên chức năng:** Cập nhật thông tin Team
- **Mô tả ngắn:** Thay đổi tên hiển thị hoặc mô tả của một team đang hoạt động.
- **Actor:** Tenant Admin.

- **Mã chức năng:** MOD-TENANT-06
- **Tên chức năng:** Xóa/Vô hiệu hóa Team
- **Mô tả ngắn:** Xóa một nhóm không còn hoạt động, đồng thời xử lý gỡ bỏ liên kết của các thành viên đang thuộc nhóm đó.
- **Actor:** Tenant Admin.

## 3. Quản lý Thành viên (Member Management)

- **Mã chức năng:** MOD-TENANT-07
- **Tên chức năng:** Thêm/Mời thành viên vào Tenant
- **Mô tả ngắn:** Gửi lời mời hoặc trực tiếp cấp quyền cho một nhân viên (User từ module Identity) tham gia vào không gian làm việc của shop với một vai trò (Role) cụ thể.
- **Actor:** Tenant Admin.

- **Mã chức năng:** MOD-TENANT-08
- **Tên chức năng:** Gán thành viên vào Team
- **Mô tả ngắn:** Điều phối một hoặc nhiều nhân viên vào một nhóm làm việc cụ thể để chịu trách nhiệm xử lý các kênh hoặc luồng công việc của nhóm đó.
- **Actor:** Tenant Admin.

- **Mã chức năng:** MOD-TENANT-09
- **Tên chức năng:** Hủy tư cách thành viên (Remove Member)
- **Mô tả ngắn:** Gỡ bỏ một nhân viên khỏi team, hoặc xóa hoàn toàn nhân viên đó khỏi shop, lập tức thu hồi mọi quyền truy cập vào dữ liệu của shop.
- **Actor:** Tenant Admin.

## 4. Cấu hình nghiệp vụ Tenant (Tenant Settings)

- **Mã chức năng:** MOD-TENANT-10
- **Tên chức năng:** Cấu hình giờ làm việc (Business Hours)
- **Mô tả ngắn:** Thiết lập các khung giờ làm việc tiêu chuẩn của shop trong tuần. Dữ liệu này dùng để kích hoạt tin nhắn tự động (out-of-office) hoặc tạm dừng đếm thời gian SLA.
- **Actor:** Tenant Admin.

- **Mã chức năng:** MOD-TENANT-11
- **Tên chức năng:** Cấu hình chính sách SLA (SLA Policy)
- **Mô tả ngắn:** Định nghĩa các mốc thời gian cam kết dịch vụ (VD: Thời gian phản hồi lần đầu tiên - First Response Time, Thời gian giải quyết - Resolution Time) để đánh giá hiệu suất của Agent.
- **Actor:** Tenant Admin.
