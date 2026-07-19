# API Gateway

## Giới thiệu
Module `api-gateway` là điểm vào (Single Point of Entry) duy nhất của toàn bộ hệ thống Omnichannel Chat/Comment. Mọi request từ Frontend / Mobile đều đi qua đây. 
Gateway đóng vai trò định tuyến (Routing), chặn đứng các request không hợp lệ thông qua JWT Authentication, giới hạn tốc độ (Rate Limiting) để tránh DDoS, và cung cấp Fallback khi các dịch vụ phía sau (Downstream services) gặp sự cố (Circuit Breaker).

## Yêu cầu môi trường (Prerequisites)
- Java 21
- Maven
- Redis (dùng cho tính năng Rate Limiting)

## Cài đặt (Installation)
Chạy lệnh build tại thư mục `backend`:
```bash
mvn clean install -pl api-gateway -am
```

## Biến môi trường (Environment Variables)
- `JWT_PUBLIC_KEY`: Chuỗi Public Key RSA để giải mã JWT (Nên cấu hình qua Config Server, hoặc biến môi trường).
- `SPRING_REDIS_HOST`: Địa chỉ Redis server (Mặc định `localhost`).
- `SPRING_REDIS_PORT`: Cổng Redis server (Mặc định `6379`).

## Luồng xử lý một Request
Khi Client gọi một API, ví dụ `GET /api/v1/auth/profile`:
1. **CORS Filter**: Gateway kiểm tra header Origin có hợp lệ không (hiện đang cấu hình `http://localhost:3000`).
2. **Global JWT Filter**: 
   - Kiểm tra xem route này có nằm trong danh sách public (`openEndpoints`) hay không.
   - Nếu không, Gateway lấy chuỗi token từ header `Authorization: Bearer <token>`.
   - Dùng Public Key để verify chữ ký và hạn sử dụng. Nếu lỗi, Gateway ném ngoại lệ 401.
   - Trích xuất thông tin người dùng (`userId`, `roles`) từ JWT, gán thêm 2 header mới `X-User-Id` và `X-User-Roles` vào request ban đầu.
3. **Rate Limiting Filter**: Đọc `X-User-Id` hoặc IP của user, sau đó hỏi Redis xem user này có đang gọi API quá số lượng cho phép không (Token Bucket). Nếu quá, trả về 429 Too Many Requests.
4. **Routing**: Gateway nhìn vào `predicates` (ở đây là `/api/v1/auth/**`), biết được cần đẩy request sang `lb://auth-service`. Nó hỏi `discovery-server` địa chỉ thực của `auth-service` rồi forward request đi.
5. **Circuit Breaker**: Trong quá trình forward, nếu `auth-service` quá tải, quá timeout (3s), Gateway sẽ ngắt mạch (Open state) và tự động gọi về `/fallback/default` để trả JSON báo lỗi hệ thống, không làm nghẽn luồng.

## Chạy ứng dụng (Running the app)
```bash
cd backend
mvn spring-boot:run -pl api-gateway
```
Ứng dụng sẽ chạy tại port `8080`.
