# BẢNG PHÂN RÃ CÔNG VIỆC (TASK BREAKDOWN) - BACKEND OCM

Dựa trên Kiến trúc, API Contract, và Database Design, dưới đây là bảng phân rã công việc chi tiết. Tất cả các task được thiết kế đảm bảo **dưới 4 giờ**, có thể **triển khai độc lập**, và tránh tình trạng story quá lớn.

## TIÊU CHUẨN ĐỊNH NGHĨA HOÀN THÀNH (DEFINITION OF DONE - DoD) CHUNG
*(Áp dụng cho mọi Task phát triển Code)*
- Code đã được review và merge vào nhánh `develop`.
- Unit Test coverage >= 80%.
- Pass CI pipeline (Build & SonarQube scan).
- Cấu hình Flyway/Liquibase hợp lệ (nếu có đổi DB).

---

## CHI TIẾT TASK (MARKDOWN TABLE)

| Epic | Feature | Task | Subtask | Description (Mô tả) | Dependency | Est (h) | Priority | Definition of Done (DoD) riêng |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **1. Infra & Setup** | 1.1 Environments | 1.1.1 Local Dev | 1.1.1.1 Docker Compose | Tạo `docker-compose.yml` cho MySQL, Redis, Kafka. | None | 2 | P0 | Containers start thành công trên máy local. |
| **1. Infra & Setup** | 1.2 Microservices | 1.2.1 Spring Boot | 1.2.1.1 Init Projects | Khởi tạo 5 Services (Conv, Rout, Cust, Int, WS) + API Gateway. | None | 3 | P0 | Các service compile được (Java 21, Spring Boot 3). |
| **1. Infra & Setup** | 1.2 Microservices | 1.2.1 Spring Boot | 1.2.1.2 Security Setup | Cấu hình JWT filter tại API Gateway. | 1.2.1.1 | 3 | P0 | Block các request không có JWT hợp lệ. |
| **1. Infra & Setup** | 1.3 Message Broker | 1.3.1 Kafka Config | 1.3.1.1 Define Topics | Khởi tạo cấu hình các topics (`integration.message`, `conversation.assigned`, v.v) với Partitions/Retention. | 1.1.1.1 | 2 | P0 | Topic được tạo đúng config trên Kafka broker. |
| **2. Integration Svc** | 2.1 DB Migration | 2.1.1 Channel DB | 2.1.1.1 Flyway Channel | Viết script tạo bảng `channel_connections`. | 1.1.1.1 | 1 | P1 | Bảng được tạo thành công trong DB `integration`. |
| **2. Integration Svc** | 2.2 FB Webhook | 2.2.1 Verification API| 2.2.1.1 GET Endpoint | Code API GET `/webhook/fb` để Meta verify ứng dụng. | 1.2.1.1 | 2 | P0 | Trả về chuỗi `hub.challenge` chính xác. |
| **2. Integration Svc** | 2.2 FB Webhook | 2.2.2 Event Receiver| 2.2.2.1 HMAC Validation | Code hàm parse payload POST & check HMAC signature. | 2.2.1.1 | 3 | P0 | Hàm trả về boolean (Valid/Invalid). |
| **2. Integration Svc** | 2.2 FB Webhook | 2.2.2 Event Receiver| 2.2.2.2 Idempotency | Dùng Redis lưu `MessageID` ngăn chặn Webhook retry trùng lặp. | 2.2.2.1 | 2 | P0 | Ignore request nếu ID đã có trong Redis. |
| **2. Integration Svc** | 2.2 FB Webhook | 2.2.3 Kafka Publisher| 2.2.3.1 Pub Event | Publish `IntegrationMessageReceived` vào Kafka. | 2.2.2.2, 1.3.1.1| 2 | P0 | Message thấy được trên topic. |
| **3. Conv Svc** | 3.1 DB Migration | 3.1.1 Conv DB | 3.1.1.1 Flyway Conv | Tạo bảng `conversations`, `messages`, `conversation_tags` kèm Indexes. | 1.1.1.1 | 3 | P0 | DDL chạy đúng trên `conversation_db`. |
| **3. Conv Svc** | 3.2 Message Sync | 3.2.1 Event Consumer| 3.2.1.1 Cons. Webhook | Consume topic `IntegrationMessageReceived`. | 2.2.3.1 | 2 | P0 | Service bắt được message log ra console. |
| **3. Conv Svc** | 3.2 Message Sync | 3.2.2 Persist Data | 3.2.2.1 DB Save Logic | Xử lý Upsert `Conversation` và Insert `Message`. | 3.2.1.1, 3.1.1.1| 4 | P0 | Data lưu đúng vào MySQL. |
| **3. Conv Svc** | 3.2 Message Sync | 3.2.3 Route Trigger | 3.2.3.1 Pub Route Evt| Publish sự kiện `ConversationMessageReceived` (unassigned). | 3.2.2.1 | 2 | P0 | Event push lên topic thành công. |
| **3. Conv Svc** | 3.3 Core APIs | 3.3.1 Query API | 3.3.1.1 GET Convs | Implement API GET `/conversations` (Phân trang, Sorting). | 3.1.1.1 | 3 | P1 | Return DTO chuẩn spec (200 OK). |
| **3. Conv Svc** | 3.3 Core APIs | 3.3.1 Query API | 3.3.1.2 GET Messages | Implement API GET `/conversations/{id}/messages`. | 3.1.1.1 | 2 | P1 | Return list of Message DTOs. |
| **3. Conv Svc** | 3.3 Core APIs | 3.3.2 Action API | 3.3.2.1 POST Message | API gửi tin nhắn từ Agent: Validation & Save DB. | 3.1.1.1 | 3 | P0 | API return 201 Created. |
| **3. Conv Svc** | 3.3 Core APIs | 3.3.2 Action API | 3.3.2.2 External Push | Tích hợp Integration API để đẩy tin ra Facebook API. | 3.3.2.1 | 4 | P0 | Tin nhắn tới được Messenger KH. |
| **4. Routing Svc**| 4.1 DB Migration | 4.1.1 Routing DB | 4.1.1.1 Flyway Routing | Tạo bảng `agents`, `agent_routing_profiles`. | 1.1.1.1 | 2 | P0 | DDL chạy đúng trên `routing_db`. |
| **4. Routing Svc**| 4.2 Agent Status | 4.2.1 Status API | 4.2.1.1 PATCH Status | Implement API update Agent Status (DB + Redis Sync). | 4.1.1.1 | 3 | P0 | Trạng thái sync chuẩn sang Redis. |
| **4. Routing Svc**| 4.3 Assignment | 4.3.1 Event Consumer| 4.3.1.1 Cons. Conv Evt| Consume event `ConversationMessageReceived`. | 3.2.3.1 | 2 | P0 | Service nhận được event. |
| **4. Routing Svc**| 4.3 Assignment | 4.3.2 Routing Alg | 4.3.2.1 Round-robin | Chạy thuật toán tìm Agent phù hợp & Atomic lock workload (Redis). | 4.2.1.1, 4.3.1.1| 4 | P0 | Workload Redis tự tăng (+1), tìm được AgentId. |
| **4. Routing Svc**| 4.3 Assignment | 4.3.3 Route Execute | 4.3.3.1 Pub Assign Evt| Publish `RouteAssigned` lên Kafka. | 4.3.2.1 | 1 | P0 | Event nằm đúng Topic. |
| **3. Conv Svc** | 3.4 Execute Route| 3.4.1 Assign Handler | 3.4.1.1 Cons. Assign Evt| Consume `RouteAssigned` -> Update `Conversation` Status. | 4.3.3.1 | 3 | P0 | DB Conversation chuyển sang `OPEN`, gắn đúng ID. |
| **5. Customer Svc**| 5.1 DB Migration | 5.1.1 Customer DB | 5.1.1.1 Flyway Customer| Tạo bảng `customers`, `channel_identities`. | 1.1.1.1 | 2 | P1 | Bảng được tạo. |
| **5. Customer Svc**| 5.2 CRM Logic | 5.2.1 Merge Feature | 5.2.1.1 Merge Service | Implement Domain Service hợp nhất CustomerProfile. | 5.1.1.1 | 4 | P2 | Pass 100% UT cho các test case gộp data. |
| **6. WS Svc** | 6.1 WS Engine | 6.1.1 Config | 6.1.1.1 Spring WS | Setup STOMP WebSocket endpoints & Handshake Interceptor. | 1.2.1.1 | 3 | P0 | Kết nối WS bằng Postman thành công. |
| **6. WS Svc** | 6.1 WS Engine | 6.1.2 Session Mngt | 6.1.2.1 Redis Session | Lưu mapping `AgentId -> WSSessionId` vào Redis khi connect. | 6.1.1.1 | 2 | P0 | Key mapping có trong Redis. |
| **6. WS Svc** | 6.2 Push Data | 6.2.1 Kafka Bridge | 6.2.1.1 Broadcast UI | Consume `ConversationUpdated` -> Lọc AgentId -> Đẩy qua WS. | 3.4.1.1, 6.1.2.1| 4 | P0 | Agent Client nhận được JSON Payload real-time. |
