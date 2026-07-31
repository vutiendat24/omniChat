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
### 7. Chỉ số phi chức năng
- **Tài nguyên:** Chi phí RAM cho mỗi kết nối (Connection Memory Footprint) phải được tối ưu hóa (dưới 50KB/kết nối).
- **Scale:** Hỗ trợ tối thiểu **10,000 kết nối đồng thời (Concurrent Connections)** trên mỗi Node (pod 1GB RAM) mà không làm tăng CPU vượt mức 60%. Lateny cho quá trình bắt tay kết nối (Handshake) < 100ms.

## MOD-REAL-02: Định tuyến và Đẩy sự kiện cá nhân (Targeted Event Push)

### 1. Mô tả nghiệp vụ đầy đủ
Chức năng chịu trách nhiệm truyền tải các sự kiện có tính chất cá nhân (chỉ dành riêng cho 1 Agent/User cụ thể) từ backend xuống trình duyệt của họ ngay lập tức (độ trễ miligiây).
1. **Lắng nghe sự kiện:** Hệ thống đăng ký (subscribe) lắng nghe các topic từ Message Broker (Kafka hoặc RabbitMQ). Ví dụ: sự kiện "Có tin nhắn 1-1 mới gửi đích danh cho Agent A", hoặc "Agent A được chỉ định xử lý một hội thoại mới".
2. **Trích xuất thông tin người nhận:** Khi nhận được sự kiện, hệ thống đọc dữ liệu để xác định xem sự kiện này cần gửi cho ai (trích xuất `target_user_id`).
3. **Tìm kiếm Session:** Truy vấn cấu trúc dữ liệu mapping nội bộ (Map<UserId, Set<SessionId>>) để tìm ra TẤT CẢ các WebSocket session hiện đang mở của `target_user_id`.
4. **Phân phối:** 
   - Nếu tìm thấy session: Tiến hành đẩy gói dữ liệu JSON (Payload) qua các session đó xuống Client.
   - Nếu không tìm thấy session nào tại Node hiện tại: Hệ thống bỏ qua (vì có thể user đang offline, hoặc user đang kết nối ở một Node M10 khác - trường hợp kết nối ở Node khác sẽ được xử lý qua MOD-REAL-04).

### 2. Input
- **Dữ liệu đầu vào:** Thông điệp từ Message Broker, có định dạng chuẩn (thường gồm EventType, TargetUserId, và Payload).
- **Nguồn kích hoạt:** Các module nội bộ khác (M04 - Conversation, M09 - Routing) đẩy sự kiện vào Broker.

### 3. Output
- **Kết quả trả về:** Gói tin WebSocket (Frame) chứa dữ liệu JSON được đẩy thẳng xuống trình duyệt của Agent thông qua giao thức WebSocket/STOMP.
- **Nơi lưu:** Không lưu trữ dữ liệu vào Database tại bước này (vì dữ liệu đã được lưu bởi các Module nghiệp vụ trước đó).
- **Nơi gửi tiếp:** Client (Frontend).

### 4. Business Rule / Ràng buộc
- **Cấu trúc dữ liệu đẩy (Event Payload):** Dữ liệu đẩy xuống Frontend phải có trường `eventType` rõ ràng (Ví dụ: `NEW_MESSAGE`, `CONVERSATION_ASSIGNED`, `MESSAGE_DELIVERY_SUCCESS`) để Frontend có thể định tuyến logic hiển thị phù hợp (Render tin nhắn mới, hoặc đổi màu tích xanh).
- **Không đảm bảo độ tin cậy tuyệt đối (At-most-once delivery qua Socket):** Việc đẩy dữ liệu qua WebSocket mang tính chất "Cố gắng hết sức" (Best effort). Nếu ngay tại khoảnh khắc đẩy dữ liệu mà mạng bị rớt, tin nhắn đó có thể không tới UI. Bù lại, Frontend phải có cơ chế HTTP Polling định kỳ hoặc chủ động fetch API lấy lịch sử mỗi khi reconnect để đảm bảo tính đồng bộ dữ liệu.

### 5. Edge Case cần xử lý
- **Người nhận đang Offline:** Nếu `target_user_id` hiện không có kết nối nào. Module đơn giản là sẽ bỏ qua (Drop/Ignore) và không đẩy lỗi gì. Vì thực tế dữ liệu gốc đã nằm an toàn trong Database của M07.
- **Payload quá lớn:** Nếu sự kiện chứa cả nội dung Base64 của ảnh (điều này KHÔNG nên làm), Frame WebSocket có thể bị phình to làm chậm luồng mạng. -> **Xử lý:** M10 chỉ đẩy tín hiệu (Signal) hoặc dữ liệu siêu nhẹ (Ví dụ: Nội dung text, Message ID, URL Ảnh). Frontend sẽ dùng URL để tự tải ảnh, không truyền ảnh qua WebSocket.

### 6. Acceptance Criteria
- **Given** Agent `A` đang mở 2 tab trình duyệt OmniChat (có 2 WebSocket session).
- **And** Module M09 phân bổ một hội thoại mới cho Agent `A` và đẩy event vào Broker.
- **When** M10 nhận được event đó.
- **Then** M10 định tuyến và đẩy dữ liệu xuống CẢ 2 tab trình duyệt của Agent `A` gần như tức thời.
- **And** Giao diện ở cả 2 tab đều hiển thị có một hội thoại mới xuất hiện (Re-render).

### 7. Chỉ số phi chức năng
- **Độ trễ (Latency):** Thời gian từ khi M10 nhận được event từ Kafka cho đến khi bắt đầu ghi Frame xuống TCP socket của Client phải dưới `< 10ms`.

## MOD-REAL-03: Phát sóng dữ liệu nhóm (Group/Room Broadcast)

### 1. Mô tả nghiệp vụ đầy đủ
Trong bối cảnh Livestream, một phiên live có thể có hàng chục Agent cùng trực và hàng ngàn khách hàng bình luận liên tục. Việc đẩy từng bình luận dưới dạng "cá nhân" là không hiệu quả. Do đó cần mô hình Pub/Sub nội bộ theo Room (Phòng).
1. **Tham gia phòng (Join Room):** Khi Agent click vào giao diện Livestream Cụ thể (VD: Live ID = 123), Frontend gửi một lệnh `SUBSCRIBE` (nếu dùng STOMP) hoặc thông điệp `{ "action": "join_room", "roomId": "123" }` qua WebSocket.
2. **Đăng ký (Subscribe):** Node M10 lưu session của Agent này vào một danh sách `Room_123_Subscribers`.
3. **Phát sóng (Broadcast):** Khi M10 nhận được một loạt các Comment mới từ module M05 (Livestream Aggregator) thuộc `Room_123`. M10 không cần tìm xem ai là người xử lý, mà M10 chỉ việc lặp qua danh sách `Room_123_Subscribers` và đẩy bản tin xuống cho tất cả Agent trong danh sách đó.
4. **Rời phòng (Leave Room):** Khi Agent chuyển sang trang khác, Frontend gửi lệnh `UNSUBSCRIBE` để M10 gỡ session ra khỏi danh sách của Room.

### 2. Input
- **Dữ liệu đầu vào:**
  - Lệnh Join/Leave Room từ WebSocket Client.
  - Sự kiện "Có Comment Livestream mới" từ Message Broker (chứa `roomId`).
- **Nguồn kích hoạt:** Hành động điều hướng trên UI của Agent và luồng dữ liệu từ mạng xã hội.

### 3. Output
- **Kết quả trả về:** Dữ liệu Comment được Broadcast xuống nhóm (Group) các Agent đang quan tâm.
- **Nơi lưu:** Cấu trúc dữ liệu `Map<RoomId, Set<SessionId>>` nằm trên RAM.

### 4. Business Rule / Ràng buộc
- **Throttling (Gộp gói):** Với Livestream siêu lớn (VD: 100 comments/giây), nếu cứ mỗi comment lại đẩy 1 frame WebSocket thì cả Frontend và Browser sẽ bị "treo" (Lag). 
  -> **Xử lý:** M10 cần thực hiện gom nhóm (Batching). Cứ mỗi `500ms`, gom tất cả comment mới trong khoảng thời gian đó thành 1 mảng JSON Array và đẩy xuống 1 lần duy nhất. Frontend nhận mảng và render 1 lượt để tối ưu hiệu năng (DOM update).

### 5. Edge Case cần xử lý
- **Memory Leak do quên Leave Room:** Agent tắt trình duyệt đột ngột, không kịp gửi lệnh Leave Room. 
  -> **Xử lý:** Khi sự kiện Disconnection (MOD-REAL-01) xảy ra, hệ thống tự động quét và dọn dẹp (Remove) `session_id` đó khỏi mọi Room mà nó đang tham gia.
- **Bảo mật kênh (Authorization):** Tránh việc một Agent tự ý gửi lệnh `SUBSCRIBE` vào một Room (Livestream) của Tenant/Shop khác mà họ không có quyền.
  -> **Xử lý:** Trước khi cho phép Join Room, hệ thống phải kiểm tra xem `tenant_id` của Agent có khớp với chủ sở hữu của Room (Livestream) đó không.

### 6. Acceptance Criteria
- **Given** Có 3 Agent (A, B, C) đang cùng xem (Join Room) Livestream "Ngày hội Sale".
- **When** Một khách hàng bình luận "Chốt đơn".
- **Then** Node M10 đóng gói sự kiện "Comment mới".
- **And** Đẩy thông điệp xuống đồng loạt cho cả 3 trình duyệt của A, B, và C thông qua các WebSocket Session tương ứng.
- **And** Agent D (không join room) sẽ không nhận được thông báo này.

### 7. Chỉ số phi chức năng
- **Băng thông mạng (Network Throughput):** Áp dụng Batching giúp giảm thiểu đáng kể Header Overhead của WebSocket/TCP, tiết kiệm tài nguyên mạng và CPU của Client trong các đợt bão bình luận.

## MOD-REAL-04: Multi-instance Pub/Sub (Redis Pub/Sub Sync)

### 1. Mô tả nghiệp vụ đầy đủ
Khi hệ thống scale out, module M10 sẽ chạy trên nhiều Node (VD: Node 1, Node 2, Node 3). Giả sử Agent A kết nối vào Node 1, Agent B kết nối vào Node 2. Nếu Kafka đẩy sự kiện "Gửi tin nhắn cho Agent B" vào Node 1, Node 1 sẽ không tìm thấy session của Agent B trong bộ nhớ của nó.
Để giải quyết bài toán này, hệ thống áp dụng cơ chế Redis Pub/Sub:
1. **Lắng nghe toàn cục:** Tất cả các Node M10 đều subscribe vào một kênh Redis chung, ví dụ `omnichat.realtime.sync`.
2. **Kênh liên lạc ngang hàng (Peer-to-peer sync):**
   - Khi Node 1 nhận một event từ Kafka báo cần gửi cho Agent B, Node 1 tìm trong RAM của nó nhưng không thấy Agent B.
   - Node 1 sẽ publish event đó vào kênh Redis `omnichat.realtime.sync` với metadata `{ "target": "Agent B", "payload": "..." }`.
   - Node 2 (cũng đang lắng nghe kênh này) nhận được event. Node 2 tra cứu trong RAM thấy đang giữ WebSocket của Agent B.
   - Node 2 tiến hành đẩy Payload xuống trình duyệt của Agent B thông qua WebSocket của nó.

### 2. Input
- **Dữ liệu đầu vào:** Thông điệp (Message) được đẩy từ một Node M10 khác thông qua Redis Pub/Sub.
- **Nguồn kích hoạt:** Các instance của M10 giao tiếp nội bộ với nhau.

### 3. Output
- **Kết quả trả về:** Gói tin đến được đúng Node đang giữ kết nối của User và được đẩy xuống Client.
- **Nơi lưu:** Redis (đóng vai trò Broker trung gian siêu nhẹ, không lưu trữ dài hạn).

### 4. Business Rule / Ràng buộc
- **Chỉ Publish khi cần thiết:** Không phải lúc nào cũng vứt mọi thứ vào Redis Pub/Sub. Node nhận được event từ Kafka trước tiên LUÔN phải check Local RAM. Chỉ khi nào KHÔNG tìm thấy (hoặc tìm thấy thiếu) thì mới đẩy phần còn lại qua Redis.
- **Tối ưu Network:** Payload truyền qua Redis Pub/Sub cần được nén (Ví dụ: MessagePack hoặc GZIP) nếu kích thước lớn.

### 5. Edge Case cần xử lý
- **Lặp vô tận (Infinite Loop):** Nếu cấu hình sai, Node 1 gửi vào Redis, Node 2 nhận được nhưng cũng không có session, Node 2 lại gửi vào Redis... 
  -> **Xử lý:** Mọi thông điệp Redis Pub/Sub phải đánh dấu `TTL=1` (Chỉ nhảy 1 bước). Các node nhận được từ Redis Pub/Sub tuyệt đối không được publish ngược lại vào Redis.

### 6. Acceptance Criteria
- **Given** Hệ thống M10 đang chạy 2 Node. Agent `A` đang nối vào Node 1. Agent `B` đang nối vào Node 2.
- **When** Một event Kafka yêu cầu gửi tin cho Agent `B` được Node 1 lấy ra khỏi queue.
- **Then** Node 1 tìm trong RAM không thấy `B`, liền đẩy thông điệp vào Redis Pub/Sub.
- **And** Node 2 nhận được từ Redis, tìm thấy `B` trong RAM và đẩy thành công qua WebSocket.

### 7. Chỉ số phi chức năng
- **Scale-out:** Kiến trúc này cho phép M10 scale ngang lên đến hàng chục node một cách dễ dàng (Horizontally Scalable) mà không làm rớt bản tin.

## MOD-REAL-05: Đồng bộ trạng thái kết nối (Presence/Status Sync)

### 1. Mô tả nghiệp vụ đầy đủ
Chức năng cho phép hệ thống luôn nhận thức được (Awareness) trạng thái Online/Offline của các Agent theo thời gian thực. Điều này vô cùng quan trọng đối với Module Routing (M09) để quyết định có chia hội thoại cho Agent đó hay không.
1. **Lắng nghe sự thay đổi:** Khi một kết nối WebSocket được mở (Connected) hoặc đóng (Disconnected/Timeout).
2. **Cập nhật Local & Redis:** Node M10 cập nhật bộ đếm trong Local RAM, đồng thời cập nhật trạng thái lên Redis (VD: dùng cấu trúc `SET` hoặc `HASH` để lưu danh sách online).
3. **Phát sự kiện toàn cục:**
   - Nếu Agent vừa mở kết nối đầu tiên (chuyển từ Offline -> Online), M10 đẩy sự kiện `AgentOnlineEvent` vào Kafka.
   - Nếu Agent vừa ngắt kết nối cuối cùng (chuyển từ Online -> Offline), M10 đẩy sự kiện `AgentOfflineEvent` vào Kafka.

### 2. Input
- **Dữ liệu đầu vào:** Sự kiện ngắt/kết nối của tầng giao thức TCP/WebSocket (từ framework/thư viện Socket).
- **Nguồn kích hoạt:** Hành vi của trình duyệt (Browser) hoặc sự cố mạng.

### 3. Output
- **Kết quả trả về:** Cập nhật trạng thái Agent trên Redis và phát sự kiện lên Kafka.
- **Nơi lưu:** Redis (Bảng trạng thái Online của Agent).
- **Nơi gửi tiếp:** Gửi sự kiện cho Kafka để module M09 (Routing) và UI của các Admin biết.

### 4. Business Rule / Ràng buộc
- **Quản lý đa phiên (Multi-session):** Agent mở 3 tab thì có 3 session. Khi đóng 1 tab, Agent VẪN đang Online (còn 2 tab). M10 chỉ phát sự kiện `AgentOfflineEvent` khi session CUỐI CÙNG của Agent đó bị ngắt.

### 5. Edge Case cần xử lý
- **Crash Node:** Nếu Node M10 bị sập đột ngột (Crash/OOM), nó sẽ không kịp bắt sự kiện Disconnect để phát `AgentOfflineEvent`. Dẫn đến hiện tượng "Ghost Online" (Agent đã mất kết nối nhưng hệ thống vẫn báo Online).
  -> **Xử lý:** M10 cần dùng cơ chế Redis Key Expiration (TTL ngắn, ví dụ 3 phút) kết hợp với Heartbeat. Cứ mỗi phút Node M10 sẽ gia hạn (Touch) TTL trên Redis. Nếu Node sập, sau 3 phút key trên Redis tự bốc hơi. Module khác có thể lắng nghe sự kiện `KeyExpired` của Redis để đánh dấu Offline.

### 6. Acceptance Criteria
- **Given** Agent `A` đang mở 2 tab OmniChat.
- **When** Agent `A` đóng tab thứ 1.
- **Then** Hệ thống chỉ gỡ 1 session khỏi RAM, KHÔNG phát sự kiện Offline.
- **When** Agent `A` đóng nốt tab thứ 2.
- **Then** Hệ thống gỡ session cuối cùng, cập nhật Redis, và phát sự kiện `AgentOfflineEvent`.

### 7. Chỉ số phi chức năng
- **Tính nhất quán (Consistency):** Trạng thái Presence phải hội tụ (Eventual Consistency) chậm nhất trong vòng 3-5 phút (ngay cả khi có sự cố sập node) để không làm sai lệch thuật toán Routing của hệ thống.
