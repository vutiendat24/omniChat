# Software Design Document (SDD)

## 1. Mục đích
Tài liệu cung cấp thiết kế chi tiết về cấu trúc phần mềm, cơ sở dữ liệu và các thành phần chính của hệ thống quản lý kênh mạng xã hội.

## 2. Kiến trúc tổng thể
Hệ thống áp dụng kiến trúc Microservices (hoặc Modular Monolith tùy giai đoạn) để đảm bảo tính độc lập và khả năng mở rộng.
Các module/service chính:
- **API Gateway:** Điều hướng request, Rate limiting, Authentication.
- **Auth Module:** Quản lý User, Role, Permissions.
- **Integration Module:** Xử lý kết nối OAuth2 với các bên thứ 3 và quản lý Webhooks nhận thông báo.
- **Inbox Module:** Xử lý logic tin nhắn, lưu trữ hội thoại và giao tiếp qua WebSockets.
- **Publishing Module:** Quản lý Cron jobs, xử lý việc lên lịch đăng bài.

## 3. Thiết kế Cơ sở dữ liệu (Database Design)
- **Cơ sở dữ liệu chính (PostgreSQL/MySQL):** Lưu trữ thông tin User, Tenant, OAuth Tokens, Cấu hình kênh.
- **Cơ sở dữ liệu NoSQL (MongoDB):** Lưu trữ dữ liệu phi cấu trúc như nội dung tin nhắn, bình luận (do tính linh hoạt của định dạng tin nhắn từ các nền tảng khác nhau).
- **Bộ đệm (Redis):** Caching, Rate limiting, Pub/Sub cho WebSockets.

## 4. Thiết kế giao tiếp (Communication)
- **Client - Server:** RESTful API và WebSockets (cho Realtime chat).
- **Giao tiếp Asynchronous:** Sử dụng Message Broker (RabbitMQ / Kafka) cho các tác vụ không đồng bộ (như xử lý Webhook từ Facebook gửi về để không làm block request).
