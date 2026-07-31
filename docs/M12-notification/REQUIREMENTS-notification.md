# Module: M12 - Notification (Yêu cầu chi tiết)

## 1. MOD-NOTIF-01: Gửi thông báo In-app (In-app Notification)

### 1. Mô tả nghiệp vụ đầy đủ
Chức năng này tiếp nhận các sự kiện phát sinh từ hệ thống, lưu trữ trạng thái đọc/chưa đọc và gửi tín hiệu hiển thị lên "quả chuông" thông báo trên giao diện của người dùng.
- **Bước 1:** Hệ thống (hoặc một module khác như M07 - Conversation) phát sinh một sự kiện cần thông báo (VD: "Có tin nhắn mới từ khách hàng A", "Hội thoại B sắp trễ SLA") và đẩy event vào Kafka.
- **Bước 2:** Module Notification tiêu thụ (consume) event này từ Kafka.
- **Bước 3:** Module kiểm tra Cấu hình nhận thông báo (Preferences) của user đích xem họ có bật thông báo In-app cho sự kiện này không.
- **Bước 4:** Nếu hợp lệ, hệ thống tạo bản ghi thông báo trong Database với trạng thái `UNREAD`.
- **Bước 5:** Module Notification gọi API (hoặc đẩy event) sang **M10 (Realtime Delivery)** để bắn tín hiệu WebSocket xuống giao diện trình duyệt của user.
- **Bước 6:** Khi user click vào thông báo, Frontend gọi API đánh dấu thông báo là `READ`.

### 2. Input
- **Event từ Kafka:** JSON object chứa `eventType`, `targetUserId`, và `payload` (thông tin hội thoại/khách hàng).
- **API Đánh dấu đã đọc (`PUT /api/v1/notifications/{id}/read`):** Yêu cầu cập nhật trạng thái đã đọc.

### 3. Output
- **Lưu trữ:** Lưu vào bảng `NOTIFICATION_INBOX` (có thể dùng NoSQL như MongoDB để tăng tốc đọc/ghi nếu lượng thông báo lớn).
- **Gửi tiếp:** Forward event `NotificationPushEvent` sang module M10 để đẩy qua WebSocket.

### 4. Business rule (Ràng buộc)
- **Giới hạn lưu trữ:** Mỗi user chỉ lưu tối đa 500 thông báo In-app gần nhất hoặc tự động dọn dẹp các thông báo quá 30 ngày (cơ chế TTL - Time To Live).
- **Grouping:** Thông báo In-app nên được phân nhóm nếu có quá nhiều thông báo tương tự trong thời gian ngắn (VD: "Bạn có 5 tin nhắn mới" thay vì báo 5 lần độc lập lặp lại).

### 5. Edge case cần xử lý
- **User đang offline (Không có kết nối WebSocket):** Hệ thống vẫn lưu vào DB bình thường. Khi user đăng nhập, UI sẽ gọi API `GET /api/v1/notifications` để kéo danh sách thông báo chưa đọc.
- **Bão thông báo (Spike events):** Hàng ngàn comment nhảy cùng lúc khi Livestream, cần áp dụng Debounce hoặc Rate Limiting phía backend trước khi lưu và push WebSocket để tránh quá tải DB.

### 6. Acceptance criteria
- **Given** Agent đang mở giao diện làm việc. **When** có hội thoại mới được assign cho Agent. **Then** "Quả chuông" trên góc phải rung lên, hiển thị chấm đỏ kèm nội dung thông báo.
- **Given** Agent có 3 thông báo chưa đọc. **When** Agent click vào nút "Đánh dấu tất cả đã đọc" (Mark all as read). **Then** API cập nhật thành công, số chấm đỏ hoàn toàn biến mất.

### 7. Chỉ số phi chức năng
- **Latency:** Thời gian kể từ lúc sinh event (M07) đến lúc M10 nhận lệnh push thành công phải < 500ms.

---

## 2. MOD-NOTIF-02: Gửi thông báo Email (Email Notification)

### 1. Mô tả nghiệp vụ đầy đủ
Sử dụng nhà cung cấp dịch vụ Email thứ ba (như AWS SES, SendGrid) để gửi email đến người dùng.
- **Bước 1:** Lắng nghe Kafka event hoặc Cronjob kích hoạt các báo cáo định kỳ.
- **Bước 2:** Kiểm tra Preferences xem người dùng có đăng ký nhận loại Email này không.
- **Bước 3:** Lấy dữ liệu thô kết hợp với Template engine để render ra nội dung HTML hoàn chỉnh.
- **Bước 4:** Gọi API của 3rd-party Email Provider để gửi.
- **Bước 5:** Lưu log lịch sử gửi (thành công/thất bại).

### 2. Input
- **Dữ liệu kích hoạt:** Sự kiện từ Message Broker (VD: Quên mật khẩu, Báo cáo hàng tuần, Invite Member) kèm `recipientEmail` và `templateData`.

### 3. Output
- **Hành động:** HTTP POST Request tới API của Email Provider.
- **Lưu trữ:** Bảng `EMAIL_DELIVERY_LOG`.

### 4. Business rule (Ràng buộc)
- **Unsubscribe:** Mọi email mang tính cập nhật hệ thống hoặc tiếp thị không bắt buộc đều phải có link "Hủy đăng ký".
- **Rate Limiting:** Cần có cơ chế kiểm soát số lượng email gửi đi cho mỗi Tenant/ngày theo gói Subscription để tránh bị vượt chi phí.

### 5. Edge case cần xử lý
- **Email provider bị lỗi (Timeout/Down):** Thực hiện cơ chế Retry (sử dụng Queue với Exponential Backoff) tối đa 3 lần.
- **Bị đánh dấu Spam (Bounced Email):** Lắng nghe Webhook phản hồi từ Email Provider; nếu email bị bounce (không tồn tại), tự động ngừng gửi email cho địa chỉ này vĩnh viễn để bảo vệ uy tín domain (Reputation).

### 6. Acceptance criteria
- **Given** hệ thống xuất báo cáo tuần lúc 8h sáng Thứ Hai. **When** trigger được kích hoạt. **Then** Quản lý nhận được một email định dạng HTML chứa biểu đồ và số liệu, đúng theo template quy định.

### 7. Chỉ số phi chức năng
- **Throughput:** Có khả năng xử lý Async để gửi hàng ngàn email báo cáo mà không block luồng chính. Email giao dịch (OTP, Quên pass) phải đến nơi < 5 giây.

---

## 3. MOD-NOTIF-03: Gửi thông báo đẩy Web/Mobile (Web/Mobile Push Notification)

### 1. Mô tả nghiệp vụ đầy đủ
Gửi thông báo đẩy ra màn hình khóa của điện thoại hoặc góc màn hình máy tính thông qua Firebase Cloud Messaging (FCM) hoặc Apple Push Notification Service (APNs).
- **Bước 1:** Khi user đăng nhập trên trình duyệt/App, Client xin quyền hiển thị thông báo và gửi `DeviceToken` về Backend.
- **Bước 2:** Khi có sự kiện cần thông báo, kiểm tra xem user có bật Push Notification không.
- **Bước 3:** Truy xuất toàn bộ `DeviceToken` hợp lệ của user (một user có thể đăng nhập nhiều máy).
- **Bước 4:** Gửi payload (tiêu đề, icon, link deep-link) lên FCM Server. FCM sẽ tự động đẩy xuống máy khách.

### 2. Input
- **API Lưu Token (`POST /api/v1/notifications/device-tokens`):** `deviceToken`, `deviceOs`.
- **Trigger gửi:** Event từ Kafka.

### 3. Output
- **Lưu trữ:** Cập nhật bảng `USER_DEVICE_TOKEN`.
- **Gửi tiếp:** Gọi HTTP request tới Google FCM API.

### 4. Business rule (Ràng buộc)
- Dọn dẹp Token hết hạn: Khi user Đăng xuất (Logout) ở hệ thống Identity, phải kích hoạt event để xóa `DeviceToken` tương ứng, tránh việc gửi nhầm thông báo bảo mật vào máy của người khác (VD: khi mượn máy).

### 5. Edge case cần xử lý
- **Token bị vô hiệu hóa (User gỡ app hoặc clear data trình duyệt):** Khi gửi lên FCM, FCM sẽ trả về mã lỗi `UNREGISTERED`. Backend phải bắt lỗi này và xóa soft/hard token khỏi database ngay lập tức.

### 6. Acceptance criteria
- **Given** Agent đã cấp quyền Push và đang làm việc ở một tab khác (không mở OmniChat). **When** có tin nhắn gấp tới. **Then** Trình duyệt bật thông báo đẩy hệ thống, khi click vào thông báo sẽ focus chuyển thẳng vào tab OmniChat.

### 7. Chỉ số phi chức năng
- API đẩy lệnh lên FCM cần phản hồi nhanh < 300ms (Async HTTP Call).

---

## 4. MOD-NOTIF-04: Cấu hình tùy chọn thông báo (Notification Preferences)

### 1. Mô tả nghiệp vụ đầy đủ
Cung cấp API để hiển thị giao diện cài đặt, cho phép người dùng quyết định nhận thông báo nào qua kênh nào.
- **Bước 1:** Frontend gọi API lấy danh sách cấu hình.
- **Bước 2:** Hệ thống trả về ma trận (VD: "Tin nhắn 1-1 mới" -> [x] In-app, [ ] Email, [x] Push).
- **Bước 3:** User chỉnh sửa và lưu cấu hình mới.

### 2. Input
- **API Cập nhật (`PUT /api/v1/notifications/preferences`):** JSON body chứa ma trận Tên sự kiện và Cờ trạng thái các kênh.

### 3. Output
- **Lưu trữ:** Lưu vào bảng `USER_PREFERENCE`.

### 4. Business rule (Ràng buộc)
- **Thông báo bắt buộc:** Một số thông báo hệ thống mang tính bảo mật (Đổi mật khẩu, Cảnh báo khóa tài khoản) không cho phép tắt dưới bất kỳ hình thức nào.

### 5. Edge case cần xử lý
- **User mới tạo chưa có record cấu hình:** Khi API được gọi, Backend tự động trả về bộ cấu hình mặc định (Default Template) của hệ thống thay vì báo lỗi Null hoặc NotFound.

### 6. Acceptance criteria
- **Given** Agent A đã tắt nhận Email cho sự kiện "Tin nhắn mới". **When** khách hàng gửi tin. **Then** Agent A nhận được In-app notification nhưng hệ thống bỏ qua việc gửi Email, không làm đầy hòm thư của Agent.

### 7. Chỉ số phi chức năng
- **Caching:** Áp dụng Redis Caching cho bảng Preferences vì data này được query liên tục mỗi một mili-giây khi chuẩn bị gửi thông báo đi.

---

## 5. MOD-NOTIF-05: Quản lý Mẫu thông báo (Notification Templates)

### 1. Mô tả nghiệp vụ đầy đủ
Khởi tạo kho giao diện mẫu, nơi chứa nội dung HTML của Email và nội dung Text của Push notification, dễ dàng sửa đổi mà không phải can thiệp hay deploy lại source code.
- **Bước 1:** Quản trị viên (Admin) truy cập trang Quản lý Template.
- **Bước 2:** Tạo/Sửa mã HTML cho email, chèn các placeholder động (VD: `Xin chào {{customer_name}}, đơn hàng {{order_id}} của bạn...`).
- **Bước 3:** Khi có lệnh gửi, hệ thống sử dụng Template Engine (Thymeleaf, Handlebars) để binding data payload vào placeholder.

### 2. Input
- **API Tạo/Sửa (`POST/PUT /api/v1/notifications/templates`):** `templateCode` (VD: `WELCOME_EMAIL`), `subjectTemplate`, `bodyTemplate`, `language`.

### 3. Output
- **Lưu trữ:** Bảng `NOTIFICATION_TEMPLATE`.

### 4. Business rule (Ràng buộc)
- `templateCode` và `language` kết hợp lại phải là duy nhất (Unique Constraint), hỗ trợ đa ngôn ngữ (Localization) khi gửi thông báo.
- **Bảo mật:** Dù là Admin nhập, nội dung HTML phải qua bộ lọc chống XSS (Sanitization) để ngăn chặn rủi ro Inject mã độc vào ứng dụng Mail của khách hàng.

### 5. Edge case cần xử lý
- **Thiếu biến Binding:** Nếu template định nghĩa biến `{{discount_code}}` nhưng payload gửi tới lại thiếu field này, hệ thống phải tự động hiển thị chuỗi rỗng hoặc giá trị mặc định thay vì báo lỗi Crash toàn bộ luồng gửi email.

### 6. Acceptance criteria
- **Given** một template Báo cáo cuối ngày. **When** hệ thống chạy Job gửi báo cáo. **Then** Email nhận được có giao diện đẹp mắt, bảng biểu chuẩn HTML và dữ liệu thật được điền chính xác vào các biến `{{...}}`.

### 7. Chỉ số phi chức năng
- Load HTML Template từ Memory Cache để tối đa hóa Throughput. Tránh query DB lấy template cho từng email một khi gửi số lượng lớn.
