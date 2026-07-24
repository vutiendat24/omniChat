# Danh sách chức năng: M07 - Conversation & Inbox

Dưới đây là danh sách các chức năng cụ thể thuộc module **M07 - Conversation & Inbox** (Unified Inbox gom tin nhắn từ cả messaging 1-1 và livestream).

## 1. MOD-CONV-01: Tạo mới hội thoại (Create Conversation)
- **Mô tả ngắn:** Tạo một phiên hội thoại mới khi có tin nhắn/tương tác đầu tiên từ khách hàng thông qua một kênh bất kỳ (tin nhắn 1-1 hoặc comment livestream được chuyển thành chat).
- **Actor:** Hệ thống (Nhận sự kiện từ M03 - Channel Integration hoặc M05 - Livestream Chat Aggregator).

## 2. MOD-CONV-02: Cập nhật trạng thái hội thoại (Update Conversation Status)
- **Mô tả ngắn:** Cho phép thay đổi trạng thái của hội thoại (Mở, Đóng/Resolved, Pending/Chờ xử lý, Spam).
- **Actor:** Agent, Admin, Hệ thống (tự động đóng sau X giờ không tương tác).

## 3. MOD-CONV-03: Lưu trữ & đồng bộ tin nhắn (Save & Sync Message)
- **Mô tả ngắn:** Lưu trữ chi tiết nội dung tin nhắn inbound (khách hàng gửi) và outbound (agent phản hồi) vào lịch sử của hội thoại tương ứng.
- **Actor:** Hệ thống.

## 4. MOD-CONV-04: Lọc và tìm kiếm hội thoại (Filter & Search Inbox)
- **Mô tả ngắn:** Cung cấp giao diện Unified Inbox cho phép liệt kê và tìm kiếm hội thoại theo nhiều tiêu chí (trạng thái, kênh, thời gian, thẻ tag, hoặc agent đang phụ trách).
- **Actor:** Agent, Admin.

## 5. MOD-CONV-05: Gắn thẻ hội thoại (Conversation Tagging)
- **Mô tả ngắn:** Cho phép agent gắn các thẻ (tags) vào hội thoại để phân loại vấn đề (VD: Hỗ trợ kỹ thuật, Khiếu nại, Mua hàng).
- **Actor:** Agent, Admin, Hệ thống (Auto-tagging).

## 6. MOD-CONV-06: Quản lý mẫu tin nhắn nhanh (Quick Reply Templates)
- **Mô tả ngắn:** Quản lý (Tạo, xem, sửa, xóa) các mẫu câu trả lời soạn sẵn để agent có thể sử dụng nhanh trong quá trình chat với khách hàng.
- **Actor:** Admin, Agent.

## 7. MOD-CONV-07: Theo dõi thời gian phản hồi (SLA Tracking)
- **Mô tả ngắn:** Theo dõi và đếm ngược thời gian phản hồi của agent dựa trên SLA Policy (từ M02); đánh dấu hội thoại là "sắp trễ" (Warning) hoặc "đã trễ" (Breached).
- **Actor:** Hệ thống.

## 8. MOD-CONV-08: Gửi tin nhắn riêng tư từ bình luận (Private Replies)
- **Mô tả ngắn:** Cho phép Agent gửi tin nhắn Inbox (Messenger) trực tiếp cho khách hàng đã để lại bình luận trên Livestream Facebook thông qua `comment_id`.
- **Actor:** Agent.
