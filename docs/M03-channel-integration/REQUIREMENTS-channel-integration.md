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

## MOD-CI-02: Ngắt kết nối (Disconnect Channel)

### 1. Mô tả nghiệp vụ đầy đủ
Luồng xử lý khi người dùng muốn dừng đồng bộ tin nhắn và ngắt kết nối kênh mạng xã hội khỏi OmniChat:
1. **Yêu cầu ngắt kết nối:** Người dùng (Quản trị viên) chọn một kênh đang Active trên giao diện quản lý kênh và bấm "Ngắt kết nối".
2. **Cảnh báo (Warning):** Giao diện hiển thị cảnh báo yêu cầu xác nhận, đặc biệt cảnh báo nếu kênh này đang có các hội thoại (Conversation) đang mở (OPEN) chưa được xử lý xong.
3. **Xử lý ngắt kết nối nội bộ:** Khi người dùng xác nhận, Backend cập nhật trạng thái của kênh trong CSDL thành `INACTIVE` (hoặc `DISCONNECTED`) và xóa bỏ/hủy hiệu lực của `access_token` và `refresh_token`.
4. **Hủy đăng ký Webhook (Tùy chọn nền tảng):** Backend gọi API của nền tảng (Facebook/Zalo...) để hủy đăng ký Webhook cho trang/OA đó, đảm bảo nền tảng ngừng đẩy sự kiện tin nhắn mới về hệ thống.
5. **Thu hồi Token (Revoke):** (Best practice) Backend gọi API revoke token của nền tảng để vô hiệu hóa token từ phía máy chủ nền tảng.
6. **Xử lý các hội thoại đang mở:** Các hội thoại đang `OPEN` của kênh này sẽ tự động được đánh dấu (VD: Không thể trả lời thêm), do kênh đã ngắt kết nối.
7. **Trả kết quả:** Thông báo thành công và cập nhật giao diện danh sách kênh.

### 2. Input
- **Dữ liệu đầu vào (REST API Request):**
  - `channelId` (UUID, Bắt buộc - ID của kênh trong hệ thống OmniChat).
- **Nguồn kích hoạt:** Từ Frontend Admin Portal.

### 3. Output
- **Kết quả trả về (API Response):**
  - Trả về mã HTTP `200 OK`.
- **Nơi lưu:** CSDL (Bảng `channels` cập nhật `status = INACTIVE` và clear token).
- **Nơi gửi tiếp:** Tùy chọn: Publish event `channel.disconnected` để các service khác (như `routing-service` hoặc `conversation-service`) biết và ngưng assign hội thoại mới cho kênh này.

### 4. Business rule / Ràng buộc
- **Quyền hạn:** Tương tự kết nối kênh, chỉ người có quyền Quản lý kênh mới được phép ngắt kết nối.
- **Chỉ ngắt kết nối kênh Active:** Chỉ có thể gọi API này cho kênh đang có trạng thái `ACTIVE` hoặc `ERROR` (lỗi đồng bộ).

### 5. Edge case cần xử lý
- **Lỗi từ API nền tảng:** Khi gọi API revoke token hoặc hủy webhook, nếu nền tảng trả về lỗi (do token đã hết hạn trước đó, hoặc nền tảng down), hệ thống **VẪN PHẢI** hoàn tất việc ngắt kết nối ở phía nội bộ (cập nhật DB thành INACTIVE) để người dùng không bị kẹt ở trạng thái không thể ngắt kết nối. Ghi log lại lỗi của nền tảng để audit.
- **Hội thoại đang mở:** Ngay khi ngắt kết nối, các hội thoại thuộc kênh này vẫn tồn tại trong DB để tra cứu lịch sử, nhưng giao diện chat phải chặn Agent gửi tin nhắn mới (vô hiệu hóa ô nhập liệu) kèm thông báo "Kênh này đã bị ngắt kết nối".

### 6. Acceptance Criteria
- **Scenario 1: Ngắt kết nối kênh thành công**
  - **Given** Kênh "Zalo Shop" đang `ACTIVE`.
  - **When** Admin bấm xác nhận ngắt kết nối kênh này.
  - **Then** hệ thống gọi API Zalo để revoke token/webhook (nếu có thể).
  - **And** cập nhật trạng thái kênh thành `INACTIVE` trong CSDL.
  - **And** xóa/vô hiệu hóa `access_token` nội bộ.
  - **And** trả về `200 OK` với thông báo thành công.
- **Scenario 2: Xử lý êm xuôi khi API nền tảng lỗi**
  - **Given** Kênh Facebook đang `ACTIVE` nhưng token thực tế đã bị Facebook vô hiệu hóa từ lâu.
  - **When** Admin gọi API ngắt kết nối.
  - **Then** quá trình gọi API Facebook để revoke bị lỗi.
  - **And** hệ thống bỏ qua lỗi đó, vẫn tiến hành chuyển kênh thành `INACTIVE` trong hệ thống.
  - **And** trả về `200 OK`.

### 7. Chỉ số phi chức năng
- **Toàn vẹn dữ liệu:** Không xóa bản ghi gốc của Kênh (Hard Delete), chỉ Soft Delete hoặc đổi trạng thái để giữ lại toàn bộ lịch sử hội thoại đã có.

## MOD-CI-03: Tự động làm mới Token (Auto-refresh Token)

### 1. Mô tả nghiệp vụ đầy đủ
Luồng chạy ngầm (Background Job/Cronjob) để đảm bảo các kênh mạng xã hội luôn duy trì kết nối mà không cần người dùng thao tác thủ công khi Access Token hết hạn:
1. **Lên lịch định kỳ:** Hệ thống chạy một Cronjob (ví dụ mỗi giờ 1 lần) quét toàn bộ CSDL bảng `channels` để tìm các kênh đang ở trạng thái `ACTIVE`.
2. **Lọc kênh sắp hết hạn:** Từ danh sách trên, lọc ra các kênh có Access Token chuẩn bị hết hạn trong vòng 24-48 giờ tới (hoặc một ngưỡng tùy chọn).
3. **Gọi API làm mới:** 
   - Với mỗi kênh thỏa mãn, Backend sử dụng `refresh_token` hiện tại để gọi API Refresh Token tương ứng của nền tảng (Zalo, Facebook...).
4. **Cập nhật CSDL:**
   - Nếu thành công: Nền tảng trả về `access_token` mới (và có thể cả `refresh_token` mới). Hệ thống lưu lại token mới, cập nhật `expires_at` mới và giữ kênh `ACTIVE`.
   - Nếu thất bại (VD: `refresh_token` bị thu hồi bởi user đổi mật khẩu, hoặc hết hạn tuyệt đối):
     - Hệ thống đổi trạng thái kênh thành `ERROR` hoặc `NEEDS_REAUTH`.
     - Lưu log lỗi chi tiết.
5. **Thông báo cảnh báo:** Nếu quá trình làm mới thất bại, hệ thống gửi thông báo nội bộ (In-app notification) hoặc Email cho Quản lý shop yêu cầu họ truy cập vào Cài đặt kênh để kết nối lại bằng tay.

### 2. Input
- **Dữ liệu đầu vào (Tự động truy xuất từ DB):**
  - Các bản ghi `Channel` có trạng thái `ACTIVE` và `expires_at < (Now + Threshold)`.
- **Nguồn kích hoạt:** Cronjob (Worker Scheduler) nội bộ của hệ thống (Ví dụ cấu hình bằng Node-Cron hoặc Celery).

### 3. Output
- **Nơi lưu:** CSDL (Bảng `channels` cập nhật bộ đôi token mới hoặc đổi `status` thành `ERROR`).
- **Nơi gửi tiếp:** Publish sự kiện `channel.needs_reauth` nếu thất bại, để Notification Service có thể tạo thông báo tới người dùng.

### 4. Business rule / Ràng buộc
- **Giới hạn số lần thử (Retry Policy):** Nếu gọi API nền tảng bị lỗi mạng (5xx), hệ thống nên Retry vài lần với khoảng thời gian lùi dần (Exponential Backoff). Không nên lập tức chuyển kênh sang lỗi. Chỉ đánh lỗi khi nhận mã lỗi 4xx (như token invalid).
- **Rate Limit của nền tảng:** Khi có hàng ngàn kênh sắp hết hạn, hệ thống không được gọi ồ ạt cùng lúc mà phải thiết kế hàng đợi (Queue) hoặc chia lô (Batch processing) để không vi phạm Rate Limit của Facebook/Zalo API.

### 5. Edge case cần xử lý
- **Đồng thời (Concurrency/Race Condition):** Nếu có nhiều replica của Worker chạy cùng lúc, có thể dẫn đến việc nhiều Worker cùng làm mới một kênh. **Bắt buộc** phải sử dụng Khóa phân tán (Distributed Lock qua Redis) hoặc cập nhật theo cơ chế khóa dòng (Row-level lock - `SELECT ... FOR UPDATE`) để đảm bảo một kênh chỉ được làm mới 1 lần.
- **Kênh không có Refresh Token (Long-lived Access Token):** Một số nền tảng (như Facebook Page Access Token) có thể không cấp `refresh_token` mà cấp token sống rất lâu (hoặc vô hạn). Nếu `expires_at` là null hoặc quá xa, cronjob sẽ bỏ qua.

### 6. Acceptance Criteria
- **Scenario 1: Làm mới token thành công**
  - **Given** Kênh Zalo có Access Token sẽ hết hạn vào 10 tiếng nữa.
  - **When** Cronjob quét và xử lý kênh này.
  - **Then** Hệ thống nhận được token mới từ Zalo.
  - **And** Cập nhật `access_token`, `refresh_token` và `expires_at` vào DB.
  - **And** Kênh vẫn giữ trạng thái `ACTIVE`.
- **Scenario 2: Làm mới token thất bại do lỗi phía Client**
  - **Given** Quản lý shop đã thu hồi quyền ứng dụng trên giao diện Zalo, khiến Refresh Token không hợp lệ.
  - **When** Cronjob gọi API làm mới token.
  - **Then** Zalo trả về mã lỗi 4xx.
  - **And** Hệ thống cập nhật trạng thái kênh thành `NEEDS_REAUTH`.
  - **And** Gửi thông báo đến Quản lý shop.
- **Scenario 3: Ngăn chặn xử lý đồng thời**
  - **Given** Hệ thống đang chạy 3 instances của Background Worker.
  - **When** Đến giờ chạy cronjob quét kênh hết hạn.
  - **Then** Nhờ có khóa phân tán, mỗi kênh sắp hết hạn chỉ được xử lý đúng 1 lần bởi 1 instance.

### 7. Chỉ số phi chức năng
- **Khả năng mở rộng (Scalability):** Tiến trình xử lý hàng đợi (Worker Queue) phải có khả năng scale ngang khi số lượng kênh tăng lên vài chục ngàn kênh.

## MOD-CI-04: Tiếp nhận Webhook Inbound (Receive Webhook)

### 1. Mô tả nghiệp vụ đầy đủ
Chức năng đóng vai trò là "Cửa ngõ" (Entrypoint) duy nhất cho mọi sự kiện từ bên ngoài đẩy vào hệ thống. Các mạng xã hội (Facebook, Zalo...) sẽ gửi HTTP POST request đến Webhook URL này mỗi khi có tin nhắn mới hoặc sự kiện từ người dùng cuối.
1. **Tiếp nhận Request:** Hệ thống mở các public API endpoints (ví dụ: `/webhook/facebook`, `/webhook/zalo`) để các nền tảng gọi vào. Endpoint này có cấu hình cho phép nhận các request lớn nếu có attachment.
2. **Kiểm tra tính hợp lệ cơ bản:** Đảm bảo request đúng định dạng JSON. Chưa cần phân tích sâu (trách nhiệm này của MOD-CI-06).
3. **Lưu trữ dữ liệu thô (Raw Payload) lập tức:** 
   - Đẩy toàn bộ body của request (kèm headers nếu cần) vào một Message Broker (như Kafka hoặc RabbitMQ) ở topic `webhook_events_raw`.
   - Hoặc có thể ghi trực tiếp log vào MongoDB.
   - Việc đẩy vào Kafka giúp hệ thống trả về kết quả (ACK) cho nền tảng mạng xã hội ngay lập tức mà không phải chờ xử lý logic nghiệp vụ chậm trễ.
4. **Phản hồi (Acknowledge):** Trả về HTTP `200 OK` cho nền tảng càng nhanh càng tốt.

### 2. Input
- **Dữ liệu đầu vào (REST API Request từ Nền tảng):**
  - Headers: Các thông tin header đặc trưng (VD: `X-Hub-Signature` của Facebook, `X-ZEvent-Signature` của Zalo).
  - Body (JSON): Dữ liệu sự kiện, thường chứa array nhiều tin nhắn gộp chung (batching) nếu lưu lượng cao.
- **Nguồn kích hoạt:** Máy chủ của các nền tảng mạng xã hội (Zalo, Facebook...).

### 3. Output
- **Kết quả trả về (API Response):**
  - Trả về mã HTTP `200 OK` (Văn bản thuần túy hoặc JSON rỗng).
- **Nơi lưu:** Kafka (Topic `webhook_events_raw`).
- **Nơi gửi tiếp:** Tiến trình (Consumer) xử lý tiếp theo sẽ tự kéo (pull) dữ liệu từ Kafka.

### 4. Business rule / Ràng buộc
- **Return Fast (Trả lời nhanh):** Không được thực hiện bất kỳ truy vấn CSDL (SQL) nào, không gọi API bên thứ 3 nào trong luồng nhận Webhook này. Nó chỉ có nhiệm vụ Validate sơ bộ -> Push Kafka -> Return 200. (Nếu xử lý quá lâu, Facebook/Zalo sẽ đánh giá Webhook bị lỗi và ngừng gửi tin nhắn).
- **Bảo mật:** Không áp dụng Auth (JWT) thông thường cho API này vì người gọi là máy chủ mạng xã hội (Public Endpoint). Tuy nhiên, cần hỗ trợ cơ chế Challenge-Response ban đầu (ví dụ: Facebook gửi `hub.challenge` dạng GET request để xác minh chủ sở hữu URL).

### 5. Edge case cần xử lý
- **Lưu lượng đột biến (Spike/DDoS):** Khi một shop chạy chiến dịch viral, lượng webhook có thể tăng vọt gấp 100 lần. Endpoint này cần chịu tải cực tốt, rate limit mềm dựa trên IP của nền tảng (nếu xác định được dải IP của FB/Zalo).
- **Message Broker Down:** Nếu Kafka bị sập, API webhook không đẩy được tin đi. Cần có cơ chế Fallback (VD: Ghi tạm ra file log trên ổ cứng cục bộ) hoặc đành trả về 500 để Facebook/Zalo tự thử lại (Retry) sau vài phút.
- **Payload quá lớn:** Nếu đối tác gửi JSON quá lớn vượt giới hạn cấu hình (VD: > 10MB), có thể trả về 413 Payload Too Large.

### 6. Acceptance Criteria
- **Scenario 1: Trả lời Challenge xác minh Webhook (Facebook)**
  - **Given** Facebook gửi HTTP GET tới `/webhook/facebook` với các tham số `hub.mode=subscribe`, `hub.verify_token`, `hub.challenge`.
  - **When** Hệ thống nhận request.
  - **Then** Kiểm tra `hub.verify_token` trùng với cấu hình.
  - **And** Trả về HTTP 200 kèm nội dung là mã `hub.challenge`.
- **Scenario 2: Nhận tin nhắn mới**
  - **Given** Facebook gửi HTTP POST chứa sự kiện tin nhắn mới của người dùng.
  - **When** Hệ thống nhận request.
  - **Then** Đẩy thành công dữ liệu thô vào Kafka topic `webhook_events_raw`.
  - **And** Trả về mã `200 OK` trong thời gian dưới 1 giây.
- **Scenario 3: Kafka không phản hồi**
  - **Given** Hệ thống nhận tin nhắn mới từ Zalo nhưng Kafka đang bị mất kết nối.
  - **When** API Webhook cố gắng push dữ liệu.
  - **Then** Push thất bại (Timeout).
  - **And** Ghi log lỗi nghiêm trọng và trả về mã `500 Internal Server Error` để Zalo tự retry sau.

### 7. Chỉ số phi chức năng
- **Throughput / Latency:** Cần chịu tải cao (Hàng ngàn RPS) với độ trễ tối đa (P99) phải dưới 200ms để đảm bảo không bị nền tảng cắt Webhook.

## MOD-CI-05: Xác thực Webhook (Verify Webhook Signature)

### 1. Mô tả nghiệp vụ đầy đủ
Sau khi Webhook Endpoint (MOD-CI-04) nhận dữ liệu thô và đẩy vào Message Broker (Kafka/RabbitMQ), một Worker Process (Consumer) sẽ kéo dữ liệu này ra để xử lý. Bước đầu tiên và quan trọng nhất của Worker này là xác thực chữ ký bảo mật.
1. **Lấy dữ liệu:** Worker pull (kéo) bản tin từ Kafka topic `webhook_events_raw`. Bản tin này chứa toàn bộ Payload JSON và các Headers (chứa chữ ký).
2. **Xác định nền tảng:** Dựa vào Header hoặc cấu trúc Payload để biết đây là Webhook từ Facebook, Zalo, hay TikTok.
3. **Tính toán chữ ký (HMAC):** 
   - Hệ thống dùng thuật toán băm HMAC (thường là SHA-256 hoặc SHA-1 tùy nền tảng) kết hợp với `App Secret` của ứng dụng (đã lưu trong cấu hình) để băm toàn bộ phần *Raw Body* của request.
4. **So khớp chữ ký:** 
   - So sánh chuỗi hash vừa tính được với chuỗi chữ ký nằm trong Header (VD: `X-Hub-Signature-256` của Facebook, `X-ZEvent-Signature` của Zalo).
5. **Phân loại kết quả:**
   - **Hợp lệ (Match):** Cho phép bản tin được chuyển sang bước tiếp theo (Chuẩn hóa dữ liệu - MOD-CI-06).
   - **Không hợp lệ (Mismatch):** Đánh dấu bản tin là giả mạo (Fake/Spoofed), ghi log cảnh báo bảo mật (Security Alert) và HỦY (Drop) bản tin này ngay lập tức. Không thực hiện thêm bất kỳ xử lý nào.

### 2. Input
- **Dữ liệu đầu vào (Từ Kafka Message):**
  - Raw JSON Body.
  - Các Webhook Headers (chứa Signature).
- **Nguồn kích hoạt:** Consumer Worker (Tự động kích hoạt khi có message mới trong queue).

### 3. Output
- **Kết quả trả về:** (Luồng dữ liệu tiếp tục hoặc bị hủy)
  - Hợp lệ: Truyền Payload thô sang hàm Chuẩn hóa (MOD-CI-06).
  - Không hợp lệ: Kết thúc xử lý (Ack Kafka message nhưng không làm gì thêm).
- **Nơi lưu:** Log hệ thống (Ghi nhận số lượng webhook fake). Không lưu DB.
- **Nơi gửi tiếp:** Module Chuẩn hóa dữ liệu.

### 4. Business rule / Ràng buộc
- **Bảo mật App Secret:** Chuỗi `App Secret` dùng để hash HMAC phải được lưu trữ bảo mật bằng các công cụ quản lý Secret (AWS Secrets Manager, HashiCorp Vault, hoặc biến môi trường `.env` an toàn). KHÔNG hardcode trong source code.
- **Tính toán Hash (Raw Body):** HMAC bắt buộc phải được tính trên chuỗi byte *Nguyên bản* (Raw Bytes) của HTTP Request Body trước khi parse thành Object JSON. Nếu parse ra Object rồi mới stringify lại thì hash sẽ bị sai do khoảng trắng hoặc thứ tự key bị thay đổi.

### 5. Edge case cần xử lý
- **Lỗi thuật toán mã hóa (Thuật toán lỗi thời):** Facebook trước đây dùng `X-Hub-Signature` (SHA-1), nay dùng `X-Hub-Signature-256` (SHA-256). Hệ thống nên ưu tiên dùng SHA-256 để đảm bảo an toàn.
- **Nhiều App/Platform:** Nếu OmniChat hỗ trợ tích hợp nhiều App Facebook (VD: 1 App cho Shop A, 1 App cho Shop B) thì hệ thống phải trích xuất được `app_id` từ Payload (nếu có) để lấy đúng `App Secret` ra kiểm tra chữ ký.
- **Header bị thiếu:** Nếu request không hề có Header chữ ký -> Loại bỏ ngay lập tức (Drop) mà không cần tính hash tốn CPU.

### 6. Acceptance Criteria
- **Scenario 1: Chữ ký hợp lệ (Valid Signature)**
  - **Given** một webhook message từ Zalo có `X-ZEvent-Signature` được tạo từ đúng App Secret.
  - **When** hệ thống tính toán lại HMAC bằng App Secret của OmniChat.
  - **Then** hai chuỗi hash hoàn toàn khớp nhau.
  - **And** bản tin được chuyển sang bước xử lý tiếp theo.
- **Scenario 2: Chữ ký giả mạo (Spoofed Webhook)**
  - **Given** một hacker gửi HTTP POST trực tiếp vào endpoint webhook của OmniChat với dữ liệu fake và một chữ ký ngẫu nhiên (hoặc không có chữ ký).
  - **When** hệ thống tính toán lại HMAC.
  - **Then** hai chuỗi hash không khớp nhau.
  - **And** hệ thống ghi log cảnh báo bảo mật mức độ cao (Severity: High).
  - **And** hủy (drop) bản tin đó ngay lập tức.
- **Scenario 3: Thiếu Raw Body (Parsing Error)**
  - **Given** API Gateway vô tình sửa đổi nội dung body (chuẩn hóa JSON) trước khi đưa vào Kafka.
  - **When** hệ thống tính toán HMAC trên body đã bị thay đổi.
  - **Then** chữ ký sẽ không khớp (Failed). (Note: Đây là Test case kỹ thuật để cảnh báo Developer không được can thiệp vào Raw Body tại tầng Gateway).

### 7. Chỉ số phi chức năng
- **Bảo mật thông tin (Confidentiality):** Nếu phát hiện liên tiếp (ví dụ 100 requests/phút) webhook bị sai chữ ký từ một dải IP cụ thể, hệ thống có thể kích hoạt cơ chế khóa IP đó ở tầng Firewall/WAF.
- **Hiệu năng:** Thuật toán SHA-256 chạy khá nhanh, nhưng vẫn tốn CPU. Worker xử lý cần được cấp đủ tài nguyên CPU nếu có quá nhiều webhook rác.

## MOD-CI-06: Chuẩn hóa dữ liệu (Inbound Normalization)

### 1. Mô tả nghiệp vụ đầy đủ
Sau khi Webhook hợp lệ đã lọt qua lớp xác thực chữ ký (MOD-CI-05), dữ liệu này vẫn đang ở định dạng đặc thù của từng mạng xã hội (Facebook có cấu trúc JSON khác Zalo, khác TikTok). Hệ thống cần chuẩn hóa chúng về một định dạng duy nhất (`UnifiedMessage`) trước khi lưu trữ và xử lý tiếp.
1. **Tiếp nhận Payload Hợp lệ:** Worker nhận được Raw JSON Body cùng với thông tin định danh nền tảng (Platform: Facebook/Zalo/...).
2. **Định tuyến (Routing):** Sử dụng mẫu thiết kế Strategy Pattern, hệ thống đẩy Payload này vào Class Parser tương ứng của nền tảng đó (VD: `FacebookParser`, `ZaloParser`).
3. **Phân tích và Trích xuất (Parsing):**
   - Lấy ID người gửi (Sender ID / PSID / Zalo User ID).
   - Lấy ID người nhận / ID Kênh (Recipient ID / Page ID / OA ID).
   - Trích xuất loại tin nhắn (Text, Image, Video, File, Sticker...).
   - Trích xuất nội dung văn bản (Text content) hoặc URL đính kèm (Attachments).
   - Trích xuất thời gian gửi (Timestamp gốc từ nền tảng).
4. **Bao bọc (Wrap) vào định dạng chuẩn:** Tạo ra một Object `UnifiedMessage` chứa tất cả các thông tin đã trích xuất ở trên.
5. **Đẩy vào luồng xử lý chính:** `UnifiedMessage` sau đó được đẩy vào một Message Queue nội bộ (hoặc chuyển qua module Conversation / Routing) để xử lý định tuyến cho Agent và lưu vào CSDL.

### 2. Input
- **Dữ liệu đầu vào (Từ tiến trình MOD-CI-05):**
  - Raw JSON Body.
  - Tên Platform (Enum: FACEBOOK, ZALO, TIKTOK, INSTAGRAM).
- **Nguồn kích hoạt:** Consumer Worker sau bước xác thực thành công.

### 3. Output
- **Kết quả trả về:** Một đối tượng (hoặc danh sách) `UnifiedMessage` có cấu trúc chuẩn hóa, ví dụ:
  ```json
  {
    "platform": "FACEBOOK",
    "channelId": "123456789",
    "sender": {
      "platformUserId": "987654321",
      "name": "Nguyen Van A",
      "avatar": ""
    },
    "messageType": "IMAGE",
    "content": {
      "text": "",
      "attachments": [
        {"type": "image", "url": "https://scontent..."}
      ]
    },
    "timestamp": 1700000000000,
    "rawPayloadRef": "mongodb_doc_id"
  }
  ```
- **Nơi lưu:** Không lưu trực tiếp ở đây (chuyển qua Module Conversation).
- **Nơi gửi tiếp:** Gửi tiếp đến Module `M04 - Conversation & Routing`.

### 4. Business rule / Ràng buộc
- **Không làm mất thông tin quan trọng:** Các thông tin quan trọng như ID người gửi, ID người nhận, nội dung tin nhắn không được phép parse sai hoặc bỏ sót. Nếu có loại đính kèm mới mà hệ thống chưa hỗ trợ, mặc định map vào dạng `UNSUPPORTED` và cảnh báo.
- **Tính trật tự (Ordering):** Trường `timestamp` chuẩn hóa phải sử dụng **thời gian gốc do mạng xã hội cung cấp** để sau này hiển thị đúng trật tự hội thoại, kể cả khi webhook tới trễ.

### 5. Edge case cần xử lý
- **Message loại Sự kiện (Events):** Payload không phải tin nhắn chat, mà là sự kiện "Đã xem" (Read Receipt) hoặc "Đang gõ" (Typing). Hệ thống cần parser phân biệt các loại này, tạo `UnifiedMessage` với `type = EVENT` và xử lý nhánh riêng biệt.
- **Batching của nền tảng:** Facebook thường gom (batching) nhiều sự kiện trong một payload duy nhất (dạng Array `entry`). Hệ thống phải loop qua mảng này và bung ra thành nhiều đối tượng `UnifiedMessage` độc lập.

### 6. Acceptance Criteria
- **Scenario 1: Chuẩn hóa tin nhắn Văn bản (Text)**
  - **Given** một webhook payload của Zalo chứa sự kiện gửi tin nhắn text 'Xin chào'.
  - **When** đưa vào Zalo Parser.
  - **Then** tạo ra `UnifiedMessage` có `messageType = TEXT`, `content.text = 'Xin chào'`.
- **Scenario 2: Xử lý mảng (Batch) từ Facebook**
  - **Given** webhook payload Facebook chứa mảng `entry` có 3 sự kiện tin nhắn.
  - **When** đưa vào Facebook Parser.
  - **Then** hệ thống bóc tách và trả về danh sách gồm 3 đối tượng `UnifiedMessage` riêng biệt.
- **Scenario 3: Sự kiện Đã xem (Read Receipt)**
  - **Given** webhook báo rằng người dùng đã xem tin nhắn.
  - **When** đưa vào Parser.
  - **Then** trả về `UnifiedMessage` có `type = READ_RECEIPT`, chứa ID của tin nhắn vừa được xem.

### 7. Chỉ số phi chức năng
- **Dễ bảo trì (Maintainability):** Code áp dụng Strategy Pattern hoặc Factory Method để việc thêm nền tảng mới (như Telegram, WhatsApp) không sửa đổi code của các parser hiện hữu (Open/Closed Principle).

## MOD-CI-07: Gửi tin nhắn (Outbound Delivery)

### 1. Mô tả nghiệp vụ đầy đủ
Chức năng này thực hiện tiến trình ngược lại so với Inbound. Nó nhận các yêu cầu gửi tin nhắn từ Agent (thông qua UI) hoặc từ Bot (hệ thống Auto-reply) và chuyển phát tin nhắn đó đến người dùng cuối qua API của mạng xã hội (Facebook, Zalo).
1. **Tiếp nhận yêu cầu gửi (Consume):** Module nhận một đối tượng tin nhắn ở định dạng chuẩn (`UnifiedMessage`) từ hàng đợi `outbound_messages` (do Module Conversation đẩy vào).
2. **Xác định cấu hình kênh:** 
   - Lấy `channelId` từ tin nhắn.
   - Truy vấn CSDL/Cache để lấy thông tin kết nối của kênh (Platform là gì, `access_token` hợp lệ hiện tại).
3. **Dịch ngược dữ liệu (Reverse Normalization):**
   - Tương tự Inbound, hệ thống dùng Strategy Pattern để gọi Class Builder tương ứng (VD: `FacebookMessageBuilder`, `ZaloMessageBuilder`).
   - Chuyển đổi định dạng `UnifiedMessage` (Text, Hình ảnh, Nút bấm) sang đúng cấu trúc JSON đặc thù mà API của nền tảng đó yêu cầu.
4. **Gọi API gửi tin (Dispatch):** 
   - Hệ thống dùng `access_token` gọi HTTP POST đến API Send của nền tảng (VD: `https://graph.facebook.com/v19.0/me/messages`).
5. **Xử lý kết quả (Handling Response):**
   - Thành công: Nhận ID tin nhắn gốc từ nền tảng. Phát sự kiện `MessageSentEvent` để cập nhật giao diện Agent (chuyển trạng thái tin nhắn từ "Đang gửi" sang "Đã gửi").
   - Thất bại: Phân tích mã lỗi. Nếu là lỗi tạm thời (Timeout, Rate limit), đưa vào hàng đợi Retry. Nếu là lỗi vĩnh viễn (User đã block Page, Token hết hạn, Quá cửa sổ 24h), phát sự kiện `MessageFailedEvent` kèm lý do lỗi để hiển thị lên UI.

### 2. Input
- **Dữ liệu đầu vào (Từ Kafka hoặc Internal Queue):**
  - Đối tượng `UnifiedMessage` outbound (Chứa ID người nhận, ID Kênh, Nội dung tin nhắn).
- **Nguồn kích hoạt:** Consumer Worker lắng nghe trên topic Outbound.

### 3. Output
- **Kết quả trả về:** Gửi HTTP request thành công/thất bại sang nền tảng bên thứ ba.
- **Nơi lưu:** Cập nhật ID tin nhắn nền tảng (Platform Message ID) và trạng thái tin (Sent/Failed) vào CSDL `messages`.
- **Nơi gửi tiếp:** Publish event `MessageDeliveryStatusEvent` qua PubSub/WebSocket để Frontend cập nhật tích xanh (Đã gửi).

### 4. Business rule / Ràng buộc
- **Cửa sổ 24h (24-hour Window Policy):** Các nền tảng như Facebook/Zalo có chính sách chặn gửi tin nhắn (hoặc tính phí) nếu người dùng cuối không tương tác gì trong vòng 24 giờ qua. Backend cần ưu tiên kiểm tra Policy này. Nếu vi phạm, trả lỗi ngay mà không cần gọi API (để tránh bị phạt/ban Fanpage).
- **Rate Limit Outbound:** Mạng xã hội thường giới hạn số lượng tin gửi ra mỗi giây. Cần có cơ chế Throttling/Rate Limiter tại tiến trình này để không gửi quá nhanh.
- **Bảo mật:** `access_token` được truyền trong request header/body phải lấy trực tiếp từ hệ thống bảo mật (KMS hoặc CSDL có mã hóa), không được log ra console dạng plain text.

### 5. Edge case cần xử lý
- **Lỗi Mạng / Timeout:** Khi gọi API nền tảng nhưng mất mạng hoặc timeout, hệ thống chưa biết tin nhắn đã gửi thành công chưa. Cần có cơ chế Retry (Exponential Backoff) nhưng phải sinh mã Idempotency Key (nếu nền tảng hỗ trợ) để tránh việc retry khiến khách hàng nhận 2 tin nhắn giống nhau.
- **Tài liệu đính kèm lớn (Large Attachments):** Một số API yêu cầu file/ảnh phải được upload trước (nhận Attachment ID) rồi mới gửi tin nhắn. Worker phải xử lý luồng upload file (gọi qua API upload của Zalo/Facebook) trước khi build cấu trúc JSON cuối cùng.

### 6. Acceptance Criteria
- **Scenario 1: Gửi tin nhắn Text thành công**
  - **Given** Agent soạn tin "Chào bạn" và gửi vào hàng đợi.
  - **When** Outbound Worker xử lý và dịch thành JSON gửi lên Facebook.
  - **Then** Facebook trả về mã 200 kèm `message_id`.
  - **And** hệ thống phát event cập nhật trạng thái "Đã gửi".
- **Scenario 2: Gửi tin nhắn ngoài cửa sổ 24h**
  - **Given** người dùng cuối nhắn tin từ 3 ngày trước.
  - **When** Agent cố gắng gửi tin nhắn phản hồi.
  - **Then** Facebook API trả về lỗi Policy (VD: mã lỗi 10).
  - **And** hệ thống ghi nhận trạng thái tin nhắn là "Thất bại (Quá cửa sổ 24h)" và báo cho Agent.
- **Scenario 3: Xử lý Lỗi Tạm thời (Retry)**
  - **Given** máy chủ Zalo đang quá tải trả về HTTP 503.
  - **When** Worker gọi API gửi tin nhắn.
  - **Then** Worker nhận diện mã lỗi có thể thử lại.
  - **And** đẩy bản tin vào hàng đợi Retry Delay (thử lại sau 5 giây).

### 7. Chỉ số phi chức năng
- **Idempotency (Tính lũy đẳng):** Logic gửi tin đặc biệt nhạy cảm, phải thiết kế sao cho việc Consumer consume lặp một event không bao giờ gây ra hiện tượng khách hàng nhận trùng 2 tin nhắn.
- **Giám sát (Monitoring):** Cần có Dashboard (Grafana) đo lường tỉ lệ Gửi thành công / Gửi thất bại để phát hiện sớm việc Fanpage bị block hoặc API nền tảng sập.
