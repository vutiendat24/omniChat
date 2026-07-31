# E2E Test Checklist

Tài liệu này chứa các lệnh `curl` để kiểm thử thủ công toàn bộ luồng request qua API Gateway xuống tới Dummy Service và Auth Service.

## 1. Đăng ký & Đăng nhập lấy JWT

Đầu tiên, bạn cần gọi API đăng ký (Register) để tạo user mới, sau đó đăng nhập (Login) lấy Access Token. Các request này đi qua Gateway cổng 8080.

**Register:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```

**Login:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```
*Ghi lại chuỗi `accessToken` trong JSON response.*

## 2. Gọi Dummy Service qua Gateway kèm JWT

Sử dụng Access Token lấy được ở bước 1, gọi vào API `/api/v1/dummy/ping`.

```bash
curl -X GET http://localhost:8080/api/v1/dummy/ping \
  -H "Authorization: Bearer <NHẬP_ACCESS_TOKEN_VÀO_ĐÂY>"
```

**Kết quả mong đợi:**
Bạn sẽ nhận được HTTP 200 OK và JSON trả về:
```json
{
  "status": "pong",
  "message": "Default Message",
  "userId": "1",
  "roles": "[ROLE_AGENT]",
  "authenticated": "true"
}
```
*Lưu ý: `userId` và `roles` đã được Gateway tự động bóc tách từ JWT và tiêm vào Header `X-User-Id` và `X-User-Roles` trước khi gửi xuống Dummy Service.*

## 3. Gọi Dummy Service KHÔNG kèm JWT (Lỗi 401)

Kiểm tra xem Gateway có thực sự chặn các request không có token hoặc token sai hay không:

```bash
curl -i -X GET http://localhost:8080/api/v1/dummy/ping
```

**Kết quả mong đợi:**
HTTP 401 Unauthorized
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Missing or invalid Authorization header"
}
```

## 4. Test Config Server Hot-Reload

Thử thay đổi file config và yêu cầu Dummy Service refresh mà không cần khởi động lại.

1. Chỉnh sửa thuộc tính `dummy.message` trong file cấu hình tại git repo hoặc local folder (thư mục `native-config` nếu đang chạy native). Ví dụ đổi thành `dummy.message=Updated Message from Config`.
2. Gửi request POST tới endpoint refresh của Dummy Service (phải gửi qua cổng thật 8082 của Dummy Service do actuator chưa được mở qua Gateway, hoặc port-forward trong Docker):
```bash
curl -X POST http://localhost:8082/actuator/refresh
```
3. Gọi lại API GET `/ping` (bước 2) và kiểm tra.

**Kết quả mong đợi:**
Trường `message` trong JSON trả về sẽ chuyển từ `Default Message` sang `Updated Message from Config`.

## 5. Logout và Kiểm tra Blacklist

Kiểm tra cơ chế Redis Blacklist của Gateway.

**Logout:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer <NHẬP_ACCESS_TOKEN_VÀO_ĐÂY>"
```

**Truy cập lại Dummy Service bằng Token cũ:**
```bash
curl -i -X GET http://localhost:8080/api/v1/dummy/ping \
  -H "Authorization: Bearer <NHẬP_ACCESS_TOKEN_VÀO_ĐÂY>"
```

**Kết quả mong đợi:**
Gateway kiểm tra Redis thấy token bị thu hồi, trả về lỗi 401:
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Token has been revoked or logged out"
}
```
