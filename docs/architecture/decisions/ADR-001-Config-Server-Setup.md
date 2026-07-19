# ADR 001: Khởi tạo Config Server với Git Backend và RSA Encryption

**Ngày tạo:** 2026-07-19
**Trạng thái:** Accepted

## Bối cảnh (Context)
Hệ thống Omnichannel Chat/Comment Management đang được thiết kế theo kiến trúc Microservices. Số lượng các dịch vụ (như `api-gateway`, `conversation-service`, `customer-service`...) sẽ tăng lên. Việc cấu hình các biến môi trường, chuỗi kết nối database, các thiết lập bảo mật trực tiếp trên từng service gây ra các vấn đề về phân mảnh, khó quản lý và bảo mật kém.
Yêu cầu đặt ra là phải có một dịch vụ tập trung để quản lý cấu hình, cho phép mã hoá các thông tin nhạy cảm và cho phép các microservice lấy cấu hình mới mà không cần khởi động lại.

## Quyết định (Decision)
1. **Spring Cloud Config Server**: Chọn sử dụng Spring Cloud Config cho cấu hình tập trung. Nó tích hợp sâu với Spring Boot và ecosystem của Spring Cloud (như Spring Cloud Bus).
2. **Git Backend (Primary) & Native (Fallback)**: Cấu hình mặc định môi trường Dev sẽ dùng profile `native` trỏ vào một thư mục local, nhưng kiến trúc chính thức (Production) sẽ sử dụng Git Backend để lưu trữ các file cấu hình. Lợi ích là cấu hình được version control đầy đủ, ai đổi cấu hình đều được ghi nhận (auditable).
3. **Mã hoá bằng RSA (Asymmetric Key)**: Thay vì dùng Symmetric key đơn giản, chúng ta sẽ mã hoá/giải mã thông tin bằng RSA Keystore (`PKCS12`). Nó giúp đảm bảo an toàn tối đa cho các chuỗi cấu hình chứa thông tin nhạy cảm (như mật khẩu DB, khóa API của Zalo/Facebook).
4. **Basic Auth**: Bản thân Config Server được bảo vệ bằng Spring Security (Basic Auth) nhằm tránh lộ lọt cấu hình ra ngoài khi không có xác thực.
5. **Spring Cloud Bus & RabbitMQ**: Tích hợp sẵn `spring-cloud-starter-bus-amqp` để sau này kích hoạt tính năng refresh toàn hệ thống qua AMQP (RabbitMQ) thông qua Webhook của Git.

## Kết quả (Consequences)
**Pros:**
- Quản lý tập trung, phiên bản hoá tốt cấu hình của tất cả các services qua Git.
- An toàn tuyệt đối với thông tin nhạy cảm bằng cơ chế mã hoá RSA Keystore, có thể lưu an toàn trên public/private Git Repo.
- Có khả năng refresh cấu hình realtime (hot-reload) mà không gây gián đoạn hệ thống.

**Cons:**
- Tăng độ phức tạp của hệ thống (thêm 1 service, thêm RabbitMQ cho refresh).
- Config Server trở thành Single Point of Failure (nếu không được deploy High Availability) vì tất cả các services đều gọi đến nó khi khởi động.
- Quản lý vòng đời (rotate) của Keystore RSA cần được lưu ý mỗi khi key hết hạn hoặc bị lộ.
