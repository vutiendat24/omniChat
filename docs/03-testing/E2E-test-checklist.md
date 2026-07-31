# E2E Test Checklist — OmniChat

> **Base URL:** `http://localhost:8080` (qua API Gateway)
> **Thứ tự test:** Chạy từ trên xuống vì các module phụ thuộc nhau
> **Trạng thái:** `[ ]` chưa test · `[✅]` pass · `[❌]` fail

---

## 📋 Tổng quan

| # | Module | Service | Endpoints |
|---|---|---|---|
| 1 | **Authentication** | auth-service | Register, Login, Verify, Token, Me |
| 2 | **Tenant / Organization** | tenant-service | Tenant, Team, Member, Business Hours |
| 3 | **Conversation / Inbox** | conversation-service | Conversations, Messages, Tags, Quick Replies |
| 4 | **Channel Integration** | integration-service | Facebook, Zalo Webhooks, Channel |
| 5 | **Agent & Notification** | routing-service / notification-service | Agent Status, Email |
| 6 | **Frontend E2E** | React App | Các luồng UI |

---

## 🔧 Infrastructure Health Check

> Chạy trước tất cả các test bên dưới

```bash
# Kiểm tra tất cả services healthy
docker ps --format "table {{.Names}}\t{{.Status}}"

# Kiểm tra API Gateway
curl -s http://localhost:8080/actuator/health | python3 -m json.tool

# Mở Eureka dashboard: http://localhost:8761
```

---

## 🔐 Module 1 — Authentication (`auth-service`)

> **Prerequisite:** Các containers đang chạy healthy

### 1.1 Register (Đăng ký)

- [ ] **TC-AUTH-001**: Register thành công
```bash
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "agent1@test.com",
    "password": "Test@12345",
    "confirmPassword": "Test@12345",
    "fullName": "Agent One"
  }' | python3 -m json.tool
```
> ✅ Expected: `201` + message "Vui lòng kiểm tra email để kích hoạt tài khoản."

- [ ] **TC-AUTH-002**: Register email đã tồn tại
```bash
# Gọi lại lệnh register trên với cùng email → phải trả 400
```
> ✅ Expected: `400` "Email is already in use!"

- [ ] **TC-AUTH-003**: Validation — thiếu confirmPassword
```bash
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "x@x.com", "password": "Test@12345", "fullName": "X"}'
```
> ✅ Expected: `400` Validation Error

---

### 1.2 Verify Email (Kích hoạt tài khoản)

> Khi chưa có SMTP, kích hoạt thủ công qua MySQL:

```bash
# Bước 1: Lấy token từ DB
docker exec omnichat-mysql mysql -uroot -ppassword omnichat_auth \
  -e "SELECT token FROM verification_tokens WHERE user_id = (SELECT id FROM users WHERE email='agent1@test.com');"

# Bước 2: Gán vào biến
TOKEN="<token-from-db>"
```

- [ ] **TC-AUTH-004**: Verify email thành công
```bash
curl -s "http://localhost:8080/api/v1/auth/verify?token=$TOKEN" | python3 -m json.tool
```
> ✅ Expected: `200` "Xác thực thành công. Bạn có thể đăng nhập."

- [ ] **TC-AUTH-005**: Verify token không hợp lệ
```bash
curl -s "http://localhost:8080/api/v1/auth/verify?token=fake-token-123"
```
> ✅ Expected: `400` hoặc `500` Invalid token

---

### 1.3 Login

- [ ] **TC-AUTH-006**: Login thành công (sau khi verify)
```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "agent1@test.com", "password": "Test@12345"}' | python3 -m json.tool

# Lưu lại để dùng cho các module sau
ACCESS_TOKEN="<accessToken từ response>"
REFRESH_TOKEN="<refreshToken từ response>"
```
> ✅ Expected: `200` + `accessToken`, `refreshToken`, `expiresIn`

- [ ] **TC-AUTH-007**: Login sai mật khẩu
```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "agent1@test.com", "password": "WrongPass"}'
```
> ✅ Expected: `401` "Tài khoản hoặc mật khẩu không chính xác"

- [ ] **TC-AUTH-008**: Login trước khi verify email
```bash
# Đăng ký tài khoản mới không verify, rồi login ngay
```
> ✅ Expected: `403` "Vui lòng xác thực email trước khi đăng nhập"

---

### 1.4 Token Management

- [ ] **TC-AUTH-009**: Get current profile (`/me`)
```bash
curl -s http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer $ACCESS_TOKEN" | python3 -m json.tool
```
> ✅ Expected: `200` + email, fullName, status

- [ ] **TC-AUTH-010**: Refresh token
```bash
curl -s -X POST "http://localhost:8080/api/v1/auth/refresh?token=$REFRESH_TOKEN" | python3 -m json.tool
NEW_ACCESS_TOKEN="<accessToken mới>"
```
> ✅ Expected: `200` + token mới

- [ ] **TC-AUTH-011**: Logout
```bash
curl -s -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -G --data-urlencode "refreshToken=$REFRESH_TOKEN"
```
> ✅ Expected: `200` OK (no body)

- [ ] **TC-AUTH-012**: Dùng token sau khi logout (Blacklist check)
```bash
curl -s http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```
> ✅ Expected: `401` Token has been revoked

---

### 1.5 Dummy Service + Gateway JWT Flow

- [ ] **TC-AUTH-013**: Gọi Dummy Service với JWT hợp lệ
```bash
# Đăng nhập lại để lấy token mới
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "agent1@test.com", "password": "Test@12345"}'

ACCESS_TOKEN="<token mới>"

curl -s http://localhost:8080/api/v1/dummy/ping \
  -H "Authorization: Bearer $ACCESS_TOKEN" | python3 -m json.tool
```
> ✅ Expected: `200` + `{"status":"pong","userId":"...","authenticated":"true"}`

- [ ] **TC-AUTH-014**: Gọi Dummy Service KHÔNG có JWT
```bash
curl -s -i http://localhost:8080/api/v1/dummy/ping
```
> ✅ Expected: `401` Unauthorized

- [ ] **TC-AUTH-015**: Config Server Hot-Reload
```bash
# Bước 1: Sửa dummy.message trong native-config
# Bước 2: Trigger refresh
curl -s -X POST http://localhost:8082/actuator/refresh
# Bước 3: Kiểm tra giá trị mới
curl -s http://localhost:8080/api/v1/dummy/ping -H "Authorization: Bearer $ACCESS_TOKEN"
```
> ✅ Expected: `message` field thay đổi theo config mới

---

## 🏢 Module 2 — Tenant & Organization (`tenant-service`)

> **Prerequisite:** Có `ACCESS_TOKEN` hợp lệ từ Module 1

### 2.1 Tenant

- [ ] **TC-TENANT-001**: Tạo tenant mới
```bash
curl -s -X POST http://localhost:8080/api/v1/tenants \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Công ty ABC",
    "subdomain": "abc",
    "ownerEmail": "agent1@test.com"
  }' | python3 -m json.tool

TENANT_ID="<id từ response>"
```
> ✅ Expected: `201` + tenant object

---

### 2.2 Team

- [ ] **TC-TEAM-001**: Tạo team
```bash
curl -s -X POST http://localhost:8080/api/v1/tenants/$TENANT_ID/teams \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "Support Team", "description": "Nhóm hỗ trợ khách hàng"}' | python3 -m json.tool

TEAM_ID="<id từ response>"
```
> ✅ Expected: `201` + team object

- [ ] **TC-TEAM-002**: Cập nhật team
```bash
curl -s -X PUT http://localhost:8080/api/v1/tenants/$TENANT_ID/teams/$TEAM_ID \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "VIP Support Team", "description": "Updated"}'
```
> ✅ Expected: `200`

- [ ] **TC-TEAM-003**: Thêm member vào team
```bash
AGENT_USER_ID="<id của agent từ DB>"
curl -s -X POST http://localhost:8080/api/v1/tenants/$TENANT_ID/teams/$TEAM_ID/members \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"userId": '$AGENT_USER_ID'}'
```
> ✅ Expected: `200`

- [ ] **TC-TEAM-004**: Xóa team
```bash
curl -s -X DELETE http://localhost:8080/api/v1/tenants/$TENANT_ID/teams/$TEAM_ID \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```
> ✅ Expected: `204` No Content

---

### 2.3 Tenant Members

- [ ] **TC-MEMBER-001**: Mời thành viên
```bash
curl -s -X POST http://localhost:8080/api/v1/tenants/$TENANT_ID/members/invite \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"email": "newagent@test.com", "role": "AGENT"}'
```
> ✅ Expected: `200`

- [ ] **TC-MEMBER-002**: Xóa thành viên khỏi tenant
```bash
curl -s -X DELETE http://localhost:8080/api/v1/tenants/$TENANT_ID/members/$AGENT_USER_ID \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```
> ✅ Expected: `204`

---

### 2.4 Business Hours (Giờ làm việc)

- [ ] **TC-BH-001**: Lấy giờ làm việc
```bash
curl -s http://localhost:8080/api/v1/tenants/$TENANT_ID/business-hours \
  -H "Authorization: Bearer $ACCESS_TOKEN" | python3 -m json.tool
```
> ✅ Expected: `200` + schedule list

- [ ] **TC-BH-002**: Cập nhật giờ làm việc
```bash
curl -s -X PUT http://localhost:8080/api/v1/tenants/$TENANT_ID/business-hours \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "timezone": "Asia/Ho_Chi_Minh",
    "days": [
      {"day": "MONDAY", "open": true, "openTime": "08:00", "closeTime": "17:00"},
      {"day": "TUESDAY", "open": true, "openTime": "08:00", "closeTime": "17:00"},
      {"day": "SATURDAY", "open": false}
    ]
  }'
```
> ✅ Expected: `200`

---

## 💬 Module 3 — Conversation & Inbox (`conversation-service`)

> **Prerequisite:** Có tenant. Conversation có thể tạo từ webhook (Module 4) hoặc insert thủ công vào DB.

### 3.1 Conversations

- [ ] **TC-CONV-001**: Lấy danh sách conversations
```bash
curl -s "http://localhost:8080/api/v1/conversations?tenantId=$TENANT_ID" \
  -H "Authorization: Bearer $ACCESS_TOKEN" | python3 -m json.tool

CONV_ID="<id từ response[0]>"
```
> ✅ Expected: `200` + paginated list

- [ ] **TC-CONV-002**: Lấy messages của conversation
```bash
curl -s "http://localhost:8080/api/v1/conversations/$CONV_ID/messages" \
  -H "Authorization: Bearer $ACCESS_TOKEN" | python3 -m json.tool
```
> ✅ Expected: `200` + message list

- [ ] **TC-CONV-003**: Gửi tin nhắn nội bộ (private note)
```bash
curl -s -X POST http://localhost:8080/api/v1/conversations/$CONV_ID/messages \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"content": "Ghi chú nội bộ", "type": "NOTE"}'
```
> ✅ Expected: `201`

- [ ] **TC-CONV-004**: Assign conversation cho agent
```bash
curl -s -X PATCH http://localhost:8080/api/v1/conversations/$CONV_ID/assign \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"agentId": '$AGENT_USER_ID'}'
```
> ✅ Expected: `200`

- [ ] **TC-CONV-005**: Đổi status conversation → RESOLVED
```bash
curl -s -X PATCH http://localhost:8080/api/v1/conversations/$CONV_ID/status \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "RESOLVED"}'
```
> ✅ Expected: `200`

---

### 3.2 Tags

- [ ] **TC-TAG-001**: Tạo tag
```bash
curl -s -X POST http://localhost:8080/api/v1/tags \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "urgent", "color": "#ef4444", "tenantId": '$TENANT_ID'}' | python3 -m json.tool

TAG_ID="<id từ response>"
```
> ✅ Expected: `201`

- [ ] **TC-TAG-002**: Gán tag vào conversation
```bash
curl -s -X POST http://localhost:8080/api/v1/conversations/$CONV_ID/tags \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"tagIds": ['$TAG_ID']}'
```
> ✅ Expected: `200`

- [ ] **TC-TAG-003**: Lấy danh sách tags
```bash
curl -s "http://localhost:8080/api/v1/tags?tenantId=$TENANT_ID" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```
> ✅ Expected: `200` + tag list

- [ ] **TC-TAG-004**: Xóa tag
```bash
curl -s -X DELETE http://localhost:8080/api/v1/tags/$TAG_ID \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```
> ✅ Expected: `204`

---

### 3.3 Quick Replies (Câu trả lời nhanh)

- [ ] **TC-QR-001**: Tạo quick reply
```bash
curl -s -X POST http://localhost:8080/api/v1/quick-replies \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "shortcut": "/hi",
    "content": "Xin chào! Tôi có thể giúp gì cho bạn?",
    "tenantId": '$TENANT_ID'
  }' | python3 -m json.tool

QR_ID="<id từ response>"
```
> ✅ Expected: `201`

- [ ] **TC-QR-002**: Lấy danh sách quick replies
```bash
curl -s "http://localhost:8080/api/v1/quick-replies?tenantId=$TENANT_ID" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```
> ✅ Expected: `200` + list

- [ ] **TC-QR-003**: Cập nhật quick reply
```bash
curl -s -X PUT http://localhost:8080/api/v1/quick-replies/$QR_ID \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"shortcut": "/hello", "content": "Xin chào quý khách!", "tenantId": '$TENANT_ID'}'
```
> ✅ Expected: `200`

- [ ] **TC-QR-004**: Xóa quick reply
```bash
curl -s -X DELETE http://localhost:8080/api/v1/quick-replies/$QR_ID \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```
> ✅ Expected: `204`

---

## 🔗 Module 4 — Channel Integration (`integration-service`)

> **Prerequisite:** Đã có Facebook App Secret + Page Access Token trong `.env`

### 4.1 Facebook Webhook

- [ ] **TC-FB-001**: Verify webhook (Facebook gọi khi setup)
```bash
VERIFY_TOKEN="dfsdfsdfsdfsdfsdf-sdfsdf-sfsdfsdf655sdf46-sdfsdf5565sd5f6"
curl -s "http://localhost:8080/webhook/raw/facebook?\
hub.mode=subscribe&\
hub.verify_token=$VERIFY_TOKEN&\
hub.challenge=CHALLENGE_ACCEPTED"
```
> ✅ Expected: `200` echo lại "CHALLENGE_ACCEPTED"

- [ ] **TC-FB-002**: Simulate Facebook message webhook
```bash
curl -s -X POST http://localhost:8080/webhook/raw/facebook \
  -H "Content-Type: application/json" \
  -d '{
    "object": "page",
    "entry": [{
      "messaging": [{
        "sender": {"id": "123456789"},
        "message": {"text": "Hello from Facebook!"}
      }]
    }]
  }'
```
> ✅ Expected: `200` (message được xử lý, tạo conversation mới)

---

### 4.2 Channel Connection

- [ ] **TC-CHANNEL-001**: Lấy OAuth URL để kết nối Facebook
```bash
curl -s "http://localhost:8080/api/v1/channels/connect/url?tenantId=$TENANT_ID&channelType=FACEBOOK" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```
> ✅ Expected: `200` + `{"url": "https://facebook.com/..."}`

- [ ] **TC-CHANNEL-002**: Disconnect channel
```bash
CHANNEL_ID="<id channel đã kết nối>"
curl -s -X POST "http://localhost:8080/api/v1/channels/$CHANNEL_ID/disconnect" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```
> ✅ Expected: `200`

---

## 🤖 Module 5 — Agent Status & Notification

### 5.1 Agent Status (`routing-service`)

- [ ] **TC-AGENT-001**: Cập nhật trạng thái agent
```bash
AGENT_ID="<agentId>"
curl -s -X PATCH "http://localhost:8080/api/v1/agents/$AGENT_ID/status" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "ONLINE"}'
```
> ✅ Expected: `200`

- [ ] **TC-AGENT-002**: Lấy trạng thái agent
```bash
curl -s "http://localhost:8080/api/v1/agents/$AGENT_ID/status" \
  -H "Authorization: Bearer $ACCESS_TOKEN" | python3 -m json.tool
```
> ✅ Expected: `200` + `{"status": "ONLINE"}`

---

### 5.2 Email Notification (`notification-service`)

> **Prerequisite:** Đã cấu hình `MAIL_USERNAME` + `MAIL_PASSWORD` trong `.env` và rebuild notification-service

- [ ] **TC-NOTIF-001**: Email verify được gửi sau khi register
```bash
# Đăng ký với email thật của bạn
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "your-real-email@gmail.com",
    "password": "Test@12345",
    "confirmPassword": "Test@12345",
    "fullName": "Real Test"
  }'
# Kiểm tra hòm thư Gmail
```
> ✅ Expected: Nhận email với nút "Xác thực tài khoản" (gradient tím, thiết kế đẹp)

- [ ] **TC-NOTIF-002**: Kiểm tra log notification-service
```bash
docker logs omnichat-notification-service --tail 30 -f
```
> ✅ Expected: `INFO: Verification email sent successfully to: your-real-email@gmail.com`

- [ ] **TC-NOTIF-003**: Google SSO user KHÔNG nhận email verify
```bash
# Login qua Google → kiểm tra log notification-service
# Phải thấy: "Skipping verification email for Google SSO user"
docker logs omnichat-notification-service --tail 10
```
> ✅ Expected: Không gửi email, log có "Skipping verification email"

---

## 🖥️ Module 6 — Frontend E2E (React App)

> **Prerequisite:** `cd fontend && npm run dev` đang chạy ở port 5173

### 6.1 Auth Flow

- [ ] **TC-FE-001**: Mở trang Login → điền thông tin → Login thành công
  - URL: `http://localhost:5173/login`
  - ✅ Expected: Redirect vào Dashboard/Inbox

- [ ] **TC-FE-002**: Login sai mật khẩu → hiện thông báo lỗi rõ ràng
  - ✅ Expected: Error message hiển thị ngay, không crash

### 6.2 Inbox / Conversation

- [ ] **TC-FE-003**: Mở Inbox → xem danh sách conversation bên trái
  - URL: `http://localhost:5173/inbox`
  - ✅ Expected: ConversationList hiển thị đúng

- [ ] **TC-FE-004**: Click vào conversation → xem messages bên phải
  - ✅ Expected: MessageList load đúng messages

- [ ] **TC-FE-005**: Gõ tin nhắn vào MessageComposer → nhấn Send
  - ✅ Expected: Tin nhắn xuất hiện ngay trong chat

- [ ] **TC-FE-006**: Gõ `/` trong MessageComposer → Quick Reply dropdown xuất hiện
  - ✅ Expected: Hiện danh sách quick replies

### 6.3 Real-time (WebSocket)

- [ ] **TC-FE-007**: Mở 2 tab browser cùng conversation
  - Gửi tin ở tab 1 → tab 2 nhận ngay không cần refresh
  - ✅ Expected: Real-time delivery < 1 giây

---

## 📊 Theo dõi tiến độ

| Module | Tổng TC | Pass | Fail | Ghi chú |
|---|---|---|---|---|
| 1. Authentication | 15 | | | |
| 2. Tenant / Org | 10 | | | |
| 3. Conversation | 10 | | | Cần channel hoặc tạo thủ công |
| 4. Integration | 4 | | | Cần FB token |
| 5. Agent / Notify | 5 | | | Cần SMTP setup |
| 6. Frontend E2E | 7 | | | Cần `npm run dev` |
| **Tổng** | **51** | | | |

---

## 🚨 Notes & Prerequisites

> **Verify Email thủ công** (khi chưa có SMTP):
> ```bash
> docker exec omnichat-mysql mysql -uroot -ppassword omnichat_auth \
>   -e "UPDATE users SET status='ACTIVE' WHERE email='agent1@test.com';"
> ```

> **Thứ tự setup trước khi test:**
> 1. `cd backend && docker compose up -d` — đảm bảo tất cả containers healthy
> 2. Điền `MAIL_USERNAME` + `MAIL_PASSWORD` vào `.env` (cho Module 5.2)
> 3. Điền `FACEBOOK_APP_SECRET` + `PAGE_ACCESS_TOKEN` vào `.env` (cho Module 4)
> 4. `cd fontend && npm run dev` (cho Module 6)
