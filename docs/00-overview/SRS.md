# Software Requirements Specification (SRS)

## 1. Giới thiệu
Tài liệu này mô tả chi tiết các yêu cầu phần mềm cho dự án Quản lý kênh mạng xã hội tập trung.

## 2. Yêu cầu chức năng (Functional Requirements)

### 2.1 Quản lý kết nối (Integration)
- **FR-INT-01:** Đăng nhập hệ thống qua Google/Email.
- **FR-INT-02:** Xác thực OAuth2 để kết nối tài khoản mạng xã hội.
- **FR-INT-03:** Gia hạn/làm mới (Refresh) token tự động.

### 2.2 Hộp thư hợp nhất (Unified Inbox)
- **FR-INB-01:** Hiển thị danh sách hội thoại từ tất cả các kênh theo thời gian thực (Sử dụng Webhooks).
- **FR-INB-02:** Cho phép lọc hội thoại theo kênh, theo trạng thái.
- **FR-INB-03:** Hỗ trợ gửi text, hình ảnh, file đính kèm.
- **FR-INB-04:** Tính năng tạo tin nhắn mẫu (Quick replies).

### 2.3 Lên lịch xuất bản (Publishing)
- **FR-PUB-01:** Trình soạn thảo bài viết với xem trước (Preview) cho từng mạng xã hội.
- **FR-PUB-02:** Đăng bài ngay lập tức hoặc lên lịch đăng theo giờ tự chọn.
- **FR-PUB-03:** Xem lịch tổng quan các bài viết (Calendar view).

### 2.4 Quản lý người dùng và phân quyền (RBAC)
- **FR-USR-01:** Admin có thể tạo tài khoản nhân viên.
- **FR-USR-02:** Gán quyền truy cập cho nhân viên vào từng Fanpage/Kênh cụ thể.

## 3. Yêu cầu phi chức năng (Non-Functional Requirements)
- **NFR-01 (Performance):** Hệ thống có thể xử lý đồng thời số lượng tin nhắn lớn.
- **NFR-02 (Availability):** Uptime đạt 99.9%.
- **NFR-03 (Security):** Dữ liệu token mạng xã hội của khách hàng phải được mã hóa.
- **NFR-04 (Scalability):** Có khả năng scale dễ dàng khi lượng hội thoại tăng cao.
