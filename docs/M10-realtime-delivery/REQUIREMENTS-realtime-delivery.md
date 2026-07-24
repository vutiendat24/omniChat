# Yêu cầu chi tiết: M10 - Realtime Delivery

## MOD-REAL-01: Quản lý kết nối WebSocket (Connection Management)

### 1. Mô tả nghiệp vụ đầy đủ
Luồng xử lý chính khi thiết lập và duy trì kết nối WebSocket giữa Frontend UI (Agent/Admin) và hệ thống Backend:
1. **Yêu cầu kết nối:** Trình duyệt của Agent gửi một yêu cầu Upgrade HTTP sang giao thức WebSocket (hoặc STOMP over WebSocket) tới API Gateway, sau đó định tuyến thẳng vào một node của module `M10`. Yêu cầu này phải mang theo Access Token (JWT) để xác thực.
2. **Xác thực (Authentication):** Node `M10` tiếp nhận yêu cầu, tiến hành giải mã và xác thực JWT (sử dụng thư viện bảo mật chung chia sẻ từ M01). 
   - Nếu Token không hợp lệ hoặc hết hạn: Từ chối kết nối (HTTP 401 Unauthorized hoặc ngắt socket).
   - Nếu Token hợp lệ: Trích xuất `user_id`, `tenant_id` từ Token và chấp nhận kết nối (HTTP 101 Switching Protocols).
3. **Lưu trữ Session (Session Mapping):** Hệ thống tạo một `session_id` (hoặc lấy ID do thư viện cấp), sau đó ánh xạ (mapping) giữa `user_id` và tập hợp các `session_id` của user đó (vì một user có thể mở nhiều tab trình duyệt). Thông tin mapping được lưu tại bộ nhớ cục bộ (Local In-Memory) của node đó và đồng bộ lên Redis.
4. **Duy trì kết nối (Heartbeat):** Để tránh việc kết nối bị ngắt tĩnh (Idle Timeout) bởi các thiết bị mạng trung gian (Load Balancer, Proxy), hệ thống và Client phải ping/pong định kỳ (Heartbeat).
5. **Ngắt kết nối (Disconnection):** Khi Client chủ động đóng tab hoặc mất mạng, sự kiện ngắt kết nối xảy ra. `M10` sẽ xóa `session_id` tương ứng khỏi bộ nhớ/Redis. Nếu đây là session cuối cùng của `user_id` đó, `M10` phát một sự kiện `UserOfflineEvent` lên Message Broker.

### 2. Input
- **Dữ liệu/Tham số đầu vào:**
  - Đường dẫn kết nối (VD: `ws://api.omnichat.com/ws`).
  - JWT Access Token (Thường truyền qua Query Parameter `?token=...` vì API WebSocket chuẩn của trình duyệt không hỗ trợ custom Headers).
- **Nguồn:** Giao diện Frontend Web/App do Agent/Admin sử dụng.

### 3. Output
- **Kết quả trả về:** 
  - Khởi tạo kết nối WebSocket thành công. Bắn thông điệp `CONNECTED` về cho Client.
- **Nơi lưu:** 
  - Lưu cấu trúc dữ liệu `Map<UserId, Set<SessionId>>` trong bộ nhớ RAM của node M10 đang giữ kết nối.
  - Lưu `Set<UserId>` đang online vào Redis (để phục vụ cho MOD-REAL-04 và M09).

### 4. Business Rule / Ràng buộc
- **Multi-Tab Support (Nhiều phiên trên 1 tài khoản):** Một `user_id` được phép mở kết nối từ nhiều trình duyệt/tab khác nhau. Khi có sự kiện gửi đến `user_id`, hệ thống phải push xuống TẤT CẢ các `session_id` đang mở của user đó.
- **Heartbeat Timeout:** Nếu sau **60 giây** máy chủ không nhận được bản tin Ping hoặc bất kỳ gói tin nào từ Client, máy chủ sẽ chủ động đóng kết nối (Force Disconnect) để giải phóng tài nguyên.
- **Rate Limiting trên Socket:** Không cho phép Client gửi ngược dữ liệu (spam) quá nhiều lên WebSocket. M10 chủ yếu là kênh ĐẨY (Push) từ server xuống.

### 5. Edge Case cần xử lý
- **Reconnection Storm (Bão kết nối lại):** Khi server M10 deploy lại hoặc load balancer rớt, toàn bộ 10,000 kết nối bị ngắt. Sau đó, tất cả client đồng loạt kết nối lại trong cùng 1 giây gây nghẽn (DDoS cục bộ). 
  -> **Xử lý:** Backend giới hạn tốc độ accept connection mới. Bắt buộc Frontend Client phải implement thuật toán **Exponential Backoff** (thử kết nối lại sau 1s, 2s, 4s, 8s, cộng thêm độ trễ ngẫu nhiên Jitter).
- **Vấn đề Token hết hạn trong lúc đang kết nối:** JWT chỉ dùng lúc khởi tạo (Handshake). Tuy nhiên, nếu sau 24h JWT hết hạn mà kết nối vẫn đang giữ, thì về lý thuyết kết nối đó vẫn sống. 
  -> **Xử lý:** M10 cần lắng nghe sự kiện `TokenBlacklistedEvent` hoặc `UserLoggedOutEvent` từ Message Broker để chủ động force close các WebSocket session của user đó.
- **Giới hạn số kết nối (C10K Problem):** Cấu hình HĐH (Ulimit, File Descriptors) nếu không đủ sẽ báo lỗi "Too many open files" khi số kết nối > 1024. Cần cấu hình OS và giới hạn cấu hình ở tầng ứng dụng (VD: Max 10,000 conns/node).

### 6. Acceptance Criteria
- **Given** Agent gửi yêu cầu kết nối WebSocket với Token hợp lệ.
- **When** Server tiếp nhận.
- **Then** Kết nối WebSocket được mở thành công.
- **And** `user_id` của Agent được ghi nhận vào danh sách Đang Online trên Redis.

- **Given** Agent cung cấp Token đã hết hạn hoặc không hợp lệ.
- **When** Agent cố gắng kết nối.
- **Then** Server từ chối kết nối (Đóng Socket ngay lập tức hoặc trả về mã lỗi 401 lúc Handshake).

- **Given** Agent đang giữ kết nối bình thường, sau đó rút dây mạng.
- **When** 60 giây trôi qua không có Heartbeat.
- **Then** Server tự động ngắt kết nối và dọn dẹp bộ nhớ.

### 7. Chỉ số phi chức năng
- **Tài nguyên:** Chi phí RAM cho mỗi kết nối (Connection Memory Footprint) phải được tối ưu hóa (dưới 50KB/kết nối).
- **Scale:** Hỗ trợ tối thiểu **10,000 kết nối đồng thời (Concurrent Connections)** trên mỗi Node (pod 1GB RAM) mà không làm tăng CPU vượt mức 60%. Lateny cho quá trình bắt tay kết nối (Handshake) < 100ms.
