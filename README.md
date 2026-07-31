# OmniChat - Omnichannel Customer Support Platform

OmniChat là một nền tảng quản lý chăm sóc khách hàng đa kênh (Omnichannel) được thiết kế theo mô hình SaaS B2B. Mục tiêu của dự án là giải quyết bài toán phân tán dữ liệu và quá tải luồng giao tiếp khi doanh nghiệp phải vận hành đồng thời nhiều kênh như Facebook, Zalo, và Website Livechat. Hệ thống tập trung toàn bộ tin nhắn về một hòm thư duy nhất và tự động hóa việc phân bổ công việc cho nhân viên.

## Vấn đề và Giải pháp

Trong quá trình vận hành bộ phận CSKH, doanh nghiệp thường gặp phải các vấn đề:
- Mất mát thông tin: Nhân viên chuyển đổi liên tục giữa các nền tảng gây sót tin nhắn.
- Phân bổ công việc không đồng đều: Thiếu cơ chế điều phối tự động dẫn đến tình trạng quá tải cục bộ.
- Khó khăn trong việc scale: Kiến trúc nguyên khối truyền thống gặp điểm nghẽn (bottleneck) khi lượng truy cập đồng thời tăng cao.

OmniChat giải quyết các vấn đề trên thông qua:
- Hòm thư tập trung (Centralized Inbox): Tích hợp webhook từ các nền tảng mạng xã hội, đưa dữ liệu về một giao diện duy nhất thời gian thực.
- Định tuyến tự động (Smart Routing): Áp dụng thuật toán phân bổ (Round-robin hoặc Skill-based) để điều phối luồng tin nhắn đến đúng nhân viên đang rảnh.
- Kiến trúc phân tán (Microservices): Tách biệt các domain (auth, conversation, routing, integration...) để cho phép hệ thống mở rộng linh hoạt theo tải thực tế.

## Phân quyền hệ thống (Role-Based Access Control)

Hệ thống được thiết kế theo mô hình Multi-tenant (Đa khách thuê), cô lập dữ liệu giữa các doanh nghiệp và phân tách quyền hạn (RBAC) chi tiết thành 2 cấp độ:

**Cấp độ Hệ thống (System Level):**
- **Super Admin:** Quản trị viên cao nhất của nền tảng. Có quyền giám sát toàn bộ hạ tầng, khởi tạo tài khoản cho các doanh nghiệp (Tenant provisioning), quản lý các gói cước dịch vụ (Subscription Plans) và cấu hình core system.

**Cấp độ Doanh nghiệp thuê (Tenant Level):**
- **Tenant Owner:** Chủ sở hữu không gian làm việc (Workspace). Nắm toàn quyền quản trị của doanh nghiệp bao gồm việc thanh toán, gia hạn gói cước, và quản lý các thiết lập tích hợp.
- **Tenant Admin / Manager:** Quản lý cấp trung. Có quyền thiết lập quy tắc định tuyến (Routing rules), phân ca làm việc, giám sát chất lượng dịch vụ và xem các báo cáo tổng quan. Không có quyền can thiệp vào thanh toán (Billing).
- **Agent:** Nhân viên CSKH. Nhiệm vụ chính là tiếp nhận hội thoại, phản hồi khách hàng thông qua hòm thư tập trung, và thao tác cập nhật dữ liệu khách hàng (CRM).
- **Viewer / QA (Giám sát viên):** Vai trò chỉ xem (Read-only). Thường dành cho đội ngũ đảm bảo chất lượng (QA) để đọc lại lịch sử chat, đánh giá CSAT hoặc xem báo cáo hiệu suất mà không thể thay đổi cài đặt hệ thống.

## Các chức năng cốt lõi

- Hòm thư tập trung đa kênh: Quản lý toàn bộ tin nhắn từ Facebook, Zalo, Website tại một giao diện duy nhất, cập nhật theo thời gian thực.
- Tự động phân bổ công việc: Hệ thống nhận diện và tự động chuyển tin nhắn cho nhân viên đang rảnh hoặc có chuyên môn phù hợp, giúp tránh quá tải.
- Quản lý hồ sơ khách hàng (Mini CRM): Tự động nhận diện và liên kết lịch sử chat của khách hàng dù họ liên hệ từ nhiều nền tảng khác nhau.
- Tự động hóa phản hồi: Hỗ trợ dùng phím tắt để gửi nhanh câu trả lời mẫu và tự động phản hồi khách hàng khi ngoài giờ làm việc.
- Báo cáo và giám sát hiệu suất: Đo lường chi tiết thời gian phản hồi (Response Time) và thời gian giải quyết vấn đề (Resolution Time) của đội ngũ.

## Chi tiết thiết kế kỹ thuật (Technical Implementation)

- **Hòm thư đa kênh:** `Integration Service` mở các endpoint Webhook để hứng payload từ API của Zalo/Facebook. Dữ liệu được chuẩn hóa thành `MessageEvent` rồi đẩy vào `Kafka`. `Conversation Service` consume event này, lưu vào MySQL và kích hoạt `WebSocket Service` push data qua giao thức STOMP trực tiếp xuống trình duyệt của Agent.
- **Định tuyến (Routing) tự động:** Khi `Routing Service` nhận được event hội thoại mới từ Kafka, nó query vào `Redis` để lấy danh sách Agent đang Online. Tùy theo thuật toán cấu hình (như Round-robin), service tính toán tải hiện tại của từng Agent để chọn ra người rảnh nhất và update (assign) ID vào hội thoại.
- **Hợp nhất hồ sơ CRM:** Dựa vào các định danh (như Số điện thoại hoặc Email trích xuất từ chat), `Customer Service` tra cứu chéo trong database. Nếu trùng khớp, hệ thống hợp nhất các ID mạng xã hội (Facebook PSID, Zalo User ID) thành một Customer Entity duy nhất trong MySQL.

## Công nghệ sử dụng
Dự án được xây dựng dựa trên kiến trúc **Microservices**, sử dụng các công nghệ hiện đại để đảm bảo hiệu suất và độ chịu tải:

### Backend Architecture
- **Framework:** Java / Spring Boot 3.x
- **Microservices Stack:** Spring Cloud (Eureka Discovery, Config Server, API Gateway)
- **Security:** Spring Security, JWT (Access & Refresh Tokens)
- **Database:** MySQL (Relational Data), Redis (Caching & Session)
- **Message Broker:** Apache Kafka (Event-driven communication giữa các service)
- **Real-time Communication:** Spring WebSocket, STOMP
- **API Documentation:** OpenAPI / Swagger

### Infrastructure & DevOps
- **Containerization:** Docker & Docker Compose
- **Database Migration:** Flyway
- **Build Tool:** Maven

##  Hướng dẫn khởi chạy hệ thống (Local)

1. Clone repository và truy cập vào thư mục dự án:
```bash
git clone https://github.com/yourusername/omnichat.git
cd omnichat
```

2. Build source code (bỏ qua bước test):
```bash
mvn clean install -DskipTests
```

3. Khởi động hạ tầng (MySQL, Redis, Kafka, Zookeeper) và các dịch vụ qua Docker Compose:
```bash
cd backend
docker compose up -d
```
API Gateway sẽ được khởi chạy tại cổng 8080. Đảm bảo toàn bộ container báo trạng thái healthy trước khi thực hiện request.
