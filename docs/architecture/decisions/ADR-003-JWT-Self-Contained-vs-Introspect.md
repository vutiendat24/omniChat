# ADR 003: Cơ chế Kiểm tra Tính hợp lệ JWT (Self-contained vs Introspect)

**Ngày tạo:** 2026-07-19
**Trạng thái:** Accepted

## Bối cảnh (Context)
Hệ thống Omnichannel sử dụng JWT cho việc phân quyền các request. Gateway là cửa ngõ chính nên nó sẽ nhận và phải quyết định có cho phép request đi tiếp hay không. Khi có chức năng Logout, ta yêu cầu token hiện tại phải bị vô hiệu hoá (Blacklist).
Ta cần quyết định:
1. **Self-contained**: Gateway tự đọc token bằng Public Key RS256 và tự truy xuất thẳng Redis để check Blacklist.
2. **Introspect Endpoint**: Gateway nhận token, rồi gọi HTTP POST sang `auth-service` (endpoint `/introspect`) để uỷ quyền xác minh 100%.

## Quyết định (Decision)
Quyết định áp dụng **mô hình Hybrid**:
1. **API Gateway sẽ đóng vai trò Self-contained kết hợp check Redis trực tiếp**: Vì Gateway đã tích hợp Redis cho Rate Limiting, ta tận dụng nó để check Blacklist. Điều này giữ nguyên khả năng xác thực tự động (RS256 local) + loại bỏ độ trễ mạng (Network Hop) khi không cần gọi HTTP sang auth-service.
2. **Auth Service vẫn cung cấp `/auth/introspect`**: Để sử dụng cho các trường hợp khác, ví dụ một Service con nội bộ muốn kiểm chứng Token chéo nhưng service đó không kết nối vào Redis, nó có thể gọi API này.

## Hậu quả (Consequences)
**Pros:**
- Hiệu suất (Performance) là ưu tiên số 1: Giảm được overhead gọi API nội bộ trên từng request người dùng gửi tới.
- Thiết kế module hoá cao: Các service đằng sau Gateway chỉ cần tin tưởng vào cặp Header `X-User-Id` và `X-User-Roles` do Gateway đóng dấu vào, không cần bận tâm về JWT hay kiểm tra lại.

**Cons:**
- Logic check Redis (Blacklist) bị "rò rỉ" sang tầng Gateway thay vì gói gọn 100% trong Auth Service. Nhưng điều này là chấp nhận được trong kiến trúc API Gateway hiện đại.
