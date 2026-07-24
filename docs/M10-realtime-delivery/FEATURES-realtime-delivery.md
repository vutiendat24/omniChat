# Danh sách chức năng: M10 - Realtime Delivery

Dưới đây là danh sách các chức năng cụ thể thuộc module **M10 - Realtime Delivery** (Đảm bảo việc push dữ liệu realtime từ Backend xuống Frontend UI).

## 1. MOD-REAL-01: Quản lý kết nối WebSocket (Connection Management)
- **Mô tả ngắn:** Khởi tạo, duy trì (Heartbeat) và ngắt kết nối WebSocket/STOMP an toàn với các client (Agent/Admin UI); đồng thời liên kết Connection ID với User ID.
- **Actor:** UI Client (Trình duyệt của Agent/Admin).

## 2. MOD-REAL-02: Định tuyến và Đẩy sự kiện cá nhân (Targeted Event Push)
- **Mô tả ngắn:** Bắt các sự kiện từ Message Broker (như có tin nhắn 1-1 mới, hội thoại được gán cho agent) và định tuyến để push xuống chính xác kết nối WebSocket của Agent đích.
- **Actor:** Hệ thống (Bắt sự kiện từ M07, M09, M12).

## 3. MOD-REAL-03: Phát sóng dữ liệu nhóm (Group/Room Broadcast)
- **Mô tả ngắn:** Cho phép phát sóng (Broadcast) dữ liệu khối lượng lớn (như Comment Livestream đang nhảy liên tục) tới tất cả các Agent đang theo dõi chung một Room/Phiên Livestream cụ thể.
- **Actor:** Hệ thống (Bắt sự kiện từ M05).

## 4. MOD-REAL-04: Multi-instance Pub/Sub (Redis Pub/Sub Sync)
- **Mô tả ngắn:** Đảm bảo khả năng đẩy dữ liệu realtime thông suốt khi module M10 scale ra nhiều instance (nodes), bằng cách sử dụng Redis Pub/Sub để tìm đúng node đang giữ kết nối của người dùng.
- **Actor:** Hệ thống.

## 5. MOD-REAL-05: Đồng bộ trạng thái kết nối (Presence/Status Sync)
- **Mô tả ngắn:** Lắng nghe khi một Agent ngắt/mất kết nối WebSocket đột ngột và phát ra sự kiện báo trạng thái "Offline" cho hệ thống (để Routing M09 biết đường ngừng phân bổ hội thoại).
- **Actor:** Hệ thống (M10 phát event đi).
