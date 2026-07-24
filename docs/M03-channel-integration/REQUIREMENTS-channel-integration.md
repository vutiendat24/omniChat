# Yêu cầu chi tiết: M03 - Channel Integration

## MOD-CI-01: Kết nối kênh (OAuth2 Connect)

### 1. Mô tả nghiệp vụ đầy đủ
Luồng OAuth2 chuẩn để cấp quyền kết nối kênh mạng xã hội (Facebook Messenger, Zalo OA, TikTok Business, Instagram DM) vào hệ thống OmniChat:
1. Người dùng (Quản lý shop/Admin) bấm nút "Kết nối" kênh tương ứng trên giao diện cài đặt kênh của OmniChat.
2. Hệ thống gọi API nội bộ tạo `state` (chứa thông tin session, tenant, user ID hiện tại) để tránh tấn công CSRF, lưu `state` này vào Cache/Redis.
3. Người dùng được redirect tới trang xác thực (OAuth2 Authorization URL) của nền tảng tương ứng (Facebook/Zalo/TikTok).
4. Người dùng đăng nhập trên nền tảng (nếu cần), chọn tài nguyên muốn kết nối (Fanpage, Zalo OA, Tài khoản TikTok), và đồng ý cấp các quyền (permissions) cần thiết (nhắn tin, đọc thông tin kênh...).
5. Nền tảng redirect người dùng về trang Callback URL của OmniChat, kèm theo authorization `code` và `state`.
6. Hệ thống (backend OmniChat) nhận callback, kiểm tra `state` khớp với dữ liệu đã lưu. Nếu hợp lệ, hệ thống gửi request POST trực tiếp tới nền tảng để đổi authorization `code` lấy `access_token` (và `refresh_token` nếu có).
7. Hệ thống sử dụng `access_token` vừa nhận được để gọi API của nền tảng lấy thông tin chi tiết của kênh (Page ID, Page Name, Avatar URL...).
8. Hệ thống lưu/cập nhật thông tin kênh, `access_token`, `refresh_token` và thời gian hết hạn (expires_at) vào database. Liên kết kênh này với Tenant/Shop hiện tại và đánh dấu kênh ở trạng thái "Active".
9. Redirect người dùng về giao diện quản lý kênh với thông báo "Kết nối thành công".

### 2. Input
- **Từ Frontend (người dùng):** Lệnh yêu cầu kết nối và loại Platform (Facebook, Zalo, TikTok...).
- **Từ nền tảng (OAuth Callback):** Tham số trên URL callback bao gồm `code` (Authorization Code), `state` (thông tin ngữ cảnh hệ thống tự tạo), và các lỗi (`error`, `error_description`) nếu có trục trặc hoặc bị từ chối.

### 3. Output
- **Với Frontend:** Authorization URL để redirect; Trạng thái kết quả kết nối (thành công/thất bại) tại trang đích sau callback.
- **Nơi lưu trữ/Dữ liệu thay đổi:** Lưu bản ghi vào bảng (hoặc collection) `Channel` với các thông tin: `channel_id` (ID gốc trên nền tảng), `platform`, `name`, `avatar`, `access_token`, `refresh_token`, `expires_at`, `tenant_id`, `status` = Active.

### 4. Business rule / ràng buộc
- **Phân quyền:** Chỉ người dùng có role được cấu hình quyền "Quản lý kênh" (như Quản lý shop, Admin) mới được phép thực hiện kết nối kênh.
- **Permissions tối thiểu (Scope):** Khi xin quyền phải yêu cầu đủ bộ scopes bắt buộc. Ví dụ: Facebook cần `pages_manage_metadata`, `pages_read_engagement`, `pages_messaging`. Nếu thiếu quyền, nền tảng sẽ không cho phép gửi/nhận tin nhắn.
- **Tính duy nhất (Unique Channel):** Một kênh cụ thể (ví dụ Fanpage ID `123456`) chỉ được phép kết nối vào 1 Tenant/Shop duy nhất trong toàn hệ thống ở một thời điểm để tránh xung đột dữ liệu. Nếu bị trùng, từ chối và báo lỗi "Kênh này đã được kết nối ở một shop khác".
- **Bảo mật State:** Tham số `state` bắt buộc phải được verify. Thời gian sống của `state` tối đa là 15 phút.

### 5. Edge case cần xử lý
- **Người dùng từ chối cấp quyền:** Nền tảng trả về callback với `error=access_denied`. Hệ thống hiển thị thông báo "Bạn đã từ chối cấp quyền, kết nối thất bại" và không lưu trữ thông tin gì.
- **Platform API lỗi hoặc timeout:** Khi gọi API đổi `code` lấy `access_token`, nếu API của nền tảng gặp lỗi (500, timeout). Cần catch lỗi, ghi log và hiển thị thông báo "Lỗi kết nối đến [Platform], vui lòng thử lại sau".
- **Kênh đã tồn tại nhưng đang bị ngắt kết nối:** Nếu `channel_id` đã tồn tại trong database cùng Tenant nhưng ở trạng thái Inactive (bị ngắt kết nối hoặc hết hạn token), tiến hành cập nhật lại `access_token` mới và đổi trạng thái thành Active.
- **Mở luồng kết nối trong Popup/Tab mới:** Frontend có thể xử lý OAuth trong popup để không gián đoạn app. Khi callback hoàn tất, popup tự đóng và gửi event về cửa sổ cha để tải lại danh sách kênh.

### 6. Acceptance Criteria
- **Given** người dùng là Quản lý shop đang thao tác ở trang quản lý kênh, **When** họ bấm kết nối Facebook, đăng nhập và hoàn thành cấp quyền thành công trên cửa sổ Facebook, **Then** hệ thống lấy được token, lưu thành công Fanpage vào cấu hình của shop, và giao diện danh sách kênh cập nhật hiển thị kênh mới với trạng thái "Hoạt động".
- **Given** Fanpage "Shop Mỹ Phẩm" đã được kết nối ở Tenant A, **When** người dùng ở Tenant B cố gắng kết nối chính Fanpage này, **Then** hệ thống bắt lỗi ở bước callback, báo "Fanpage này đã được kết nối bởi shop khác" và từ chối lưu.
- **Given** người dùng đang ở màn hình ủy quyền của Zalo, **When** họ bấm nút "Hủy / Bỏ qua", **Then** hệ thống bắt được sự kiện từ chối từ callback, đóng luồng kết nối và hiện thông báo lỗi phù hợp trên UI.

### 7. Chỉ số phi chức năng
- **Bảo mật (Mật mã học):** `access_token` và `refresh_token` là các thông tin nhạy cảm, **BẮT BUỘC** phải được mã hóa trước khi lưu xuống database (Encryption at Rest) bằng thuật toán chuẩn (ví dụ AES-256).
- **Độ trễ xử lý (Latency):** Việc xử lý OAuth Callback từ khi nhận request đến khi render xong view kết quả không nên vượt quá 2-3 giây (tùy thuộc độ trễ của API nền tảng bên thứ ba).
