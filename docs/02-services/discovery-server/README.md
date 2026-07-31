# Discovery Server (Eureka)

## Giới thiệu
Module `discovery-server` cung cấp khả năng Service Registry và Service Discovery cho hệ thống Omnichannel. Dựa trên thư viện Netflix Eureka, nó giúp các microservices tự động đăng ký sự hiện diện của mình, và tra cứu (khám phá) địa chỉ của các microservices khác để giao tiếp với nhau mà không cần cấu hình IP tĩnh.

## Yêu cầu môi trường (Prerequisites)
- Java 21
- Maven

## Cài đặt (Installation)
Chạy lệnh build tại thư mục `backend`:
```bash
mvn clean install -pl discovery-server -am
```

## Biến môi trường (Environment Variables)
- `SPRING_SECURITY_USER_NAME`: Username cho trang Dashboard và Basic Auth (Mặc định: `eureka`).
- `SPRING_SECURITY_USER_PASSWORD`: Password cho trang Dashboard và Basic Auth (Mặc định: `eureka_password`).

## Cấu hình Quan trọng (Renewal / Eviction Interval / Self-Preservation)
1. **Self-Preservation (`eureka.server.enable-self-preservation`)**:
   - Chế độ "tự bảo vệ". Khi mạng bị chập chờn khiến Eureka không nhận được heartbeat từ nhiều client, nó sẽ ngưng việc xoá (evict) các client ra khỏi danh sách để tránh tình trạng loại bỏ hàng loạt dịch vụ đang sống.
   - **Môi trường Dev**: Hiện đang cấu hình là `false` để khi bạn tắt nóng một service ở local, Eureka sẽ nhanh chóng gỡ bỏ nó khỏi danh sách.
   - **Môi trường Production**: **BẮT BUỘC** bật thành `true` để hệ thống ổn định trong trường hợp lỗi mạng tạm thời.

2. **Eviction Interval (`eureka.server.eviction-interval-timer-in-ms`)**:
   - Thời gian giữa mỗi lần chạy tiến trình dọn dẹp các service đã chết. 
   - Đang set `5000` (5 giây) để dev nhìn kết quả nhanh. Ở Production nên để mặc định `60000` (60 giây).

*(Lưu ý: Các Client khi đăng ký vào Eureka cũng sẽ có 2 tham số là `lease-renewal-interval-in-seconds` - khoảng thời gian gửi heartbeat, và `lease-expiration-duration-in-seconds` - thời gian chờ tối đa trước khi bị đánh dấu là chết. Mặc định là 30s và 90s)*.

## Chạy ứng dụng (Running the app)
```bash
cd backend
mvn spring-boot:run -pl discovery-server
```
Ứng dụng sẽ chạy tại port `8761`. Bạn có thể truy cập Eureka Dashboard tại:
`http://localhost:8761` (đăng nhập bằng `eureka` / `eureka_password`).

### Kiểm tra sức khỏe (Health Check)
```bash
curl http://localhost:8761/actuator/health
```
*(Lưu ý API health check mở public theo chuẩn Kubernetes Probe)*
