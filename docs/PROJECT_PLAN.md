# Kế Hoạch Triển Khai Dự Án OmniChat

File này tổng hợp tiến độ triển khai các module và chức năng theo đúng thứ tự phụ thuộc (Dependency) được định nghĩa trong `MODULES.md`. Agent sẽ dùng file này để làm việc theo trình tự.

## 1. M00 — Platform Infrastructure
*(Không có phụ thuộc - Nền tảng lõi)*
- [ ] Thiết lập hệ thống cơ bản (Chờ cập nhật REQUIREMENTS)

## 2. M01 — Identity & Access
*(Phụ thuộc: M00)*
- [x] MOD-IAM-01: Đăng ký tài khoản hệ thống (User Registration) - [REQUIREMENTS](M01-identity-access/REQUIREMENTS-identity-access.md)
- [x] MOD-IAM-02: Đăng nhập hệ thống (Local Login) - [REQUIREMENTS](M01-identity-access/REQUIREMENTS-identity-access.md)
- [x] MOD-IAM-03: Đăng nhập qua Google (Google OAuth2 SSO) - [REQUIREMENTS](M01-identity-access/REQUIREMENTS-identity-access.md)
- [x] MOD-IAM-04: Làm mới Token (Refresh JWT) - [REQUIREMENTS](M01-identity-access/REQUIREMENTS-identity-access.md)
- [x] MOD-IAM-05: Đăng xuất (Logout & Blacklist Token) - [REQUIREMENTS](M01-identity-access/REQUIREMENTS-identity-access.md)
- [x] MOD-IAM-06: Quản lý Vai trò (Role Management) - [REQUIREMENTS](M01-identity-access/REQUIREMENTS-identity-access.md)
- [x] MOD-IAM-07: Quản lý Phân quyền (Permission / RBAC Management) - [REQUIREMENTS](M01-identity-access/REQUIREMENTS-identity-access.md)
- [x] MOD-IAM-08: Lấy thông tin tài khoản hiện tại (Get Current Profile / Introspect) - [REQUIREMENTS](M01-identity-access/REQUIREMENTS-identity-access.md)

## 3. M02 — Tenant & Organization
*(Phụ thuộc: M01)*
- [x] MOD-TENANT-01: Tạo mới Tenant (Onboarding) - [REQUIREMENTS](M02-tenant-organization/REQUIREMENTS-tenant-organization.md)
- [x] MOD-TENANT-02: Cập nhật hồ sơ Tenant - [REQUIREMENTS](M02-tenant-organization/REQUIREMENTS-tenant-organization.md)
- [x] MOD-TENANT-03: Quản lý trạng thái Tenant - [REQUIREMENTS](M02-tenant-organization/REQUIREMENTS-tenant-organization.md)
- [x] MOD-TENANT-04: Tạo mới Team - [REQUIREMENTS](M02-tenant-organization/REQUIREMENTS-tenant-organization.md)
- [x] MOD-TENANT-05: Cập nhật thông tin Team - [REQUIREMENTS](M02-tenant-organization/REQUIREMENTS-tenant-organization.md)
- [x] MOD-TENANT-06: Xóa/Vô hiệu hóa Team - [REQUIREMENTS](M02-tenant-organization/REQUIREMENTS-tenant-organization.md)
- [x] MOD-TENANT-07: Thêm/Mời thành viên vào Tenant - [REQUIREMENTS](M02-tenant-organization/REQUIREMENTS-tenant-organization.md)
- [ ] MOD-TENANT-08: Gán thành viên vào Team - [REQUIREMENTS](M02-tenant-organization/REQUIREMENTS-tenant-organization.md)
- [ ] MOD-TENANT-09: Hủy tư cách thành viên (Remove Member) - [REQUIREMENTS](M02-tenant-organization/REQUIREMENTS-tenant-organization.md)
- [ ] MOD-TENANT-10: Cấu hình giờ làm việc (Business Hours) - [REQUIREMENTS](M02-tenant-organization/REQUIREMENTS-tenant-organization.md)
- [ ] MOD-TENANT-11: Cấu hình chính sách SLA (SLA Policy) - [REQUIREMENTS](M02-tenant-organization/REQUIREMENTS-tenant-organization.md)

## 4. M03 — Channel Integration
*(Phụ thuộc: M01, M02)*
- [ ] MOD-CI-01: Kết nối kênh (OAuth2 Connect) - [REQUIREMENTS](M03-channel-integration/REQUIREMENTS-channel-integration.md)
- [ ] MOD-CI-02: Ngắt kết nối kênh (Disconnect Channel) - [REQUIREMENTS](M03-channel-integration/REQUIREMENTS-channel-integration.md)
- [ ] MOD-CI-03: Tự động làm mới Token (Auto-refresh Token) - [REQUIREMENTS](M03-channel-integration/REQUIREMENTS-channel-integration.md)
- [ ] MOD-CI-04: Tiếp nhận Webhook Inbound - [REQUIREMENTS](M03-channel-integration/REQUIREMENTS-channel-integration.md)
- [ ] MOD-CI-05: Xác thực Webhook (Verify Webhook Signature) - [REQUIREMENTS](M03-channel-integration/REQUIREMENTS-channel-integration.md)
- [ ] MOD-CI-06: Chuẩn hóa dữ liệu Inbound (Inbound Normalization) - [REQUIREMENTS](M03-channel-integration/REQUIREMENTS-channel-integration.md)
- [ ] MOD-CI-07: Gửi tin nhắn Outbound (Outbound Delivery) - [REQUIREMENTS](M03-channel-integration/REQUIREMENTS-channel-integration.md)

## 5. M04 — Livestream Connector
*(Phụ thuộc: M01, M02)*
- [ ] Khởi tạo Module Livestream Connector (Chờ cập nhật REQUIREMENTS)

## 6. M06 — Customer Management
*(Phụ thuộc: M01, M02)*
- [ ] Khởi tạo Module Customer Management (Chờ cập nhật REQUIREMENTS)

## 7. M05 — Livestream Chat Aggregator
*(Phụ thuộc: M04)*
- [ ] Khởi tạo Module Livestream Chat Aggregator (Chờ cập nhật REQUIREMENTS)

## 8. M08 — Spam Filter & Moderation
*(Phụ thuộc: M02)*
- [ ] Khởi tạo Module Spam Filter & Moderation (Chờ cập nhật REQUIREMENTS)

## 9. M07 — Conversation & Inbox
*(Phụ thuộc: M03, M05, M06)*
- [ ] MOD-CONV-01: Tạo mới hội thoại (Create Conversation) - [REQUIREMENTS](M07-conversation-inbox/REQUIREMENTS-conversation-inbox.md)
- [ ] MOD-CONV-02: Cập nhật trạng thái hội thoại (Update Conversation Status) - [REQUIREMENTS](M07-conversation-inbox/REQUIREMENTS-conversation-inbox.md)
- [ ] MOD-CONV-03: Lưu trữ & đồng bộ tin nhắn (Save & Sync Message) - [REQUIREMENTS](M07-conversation-inbox/REQUIREMENTS-conversation-inbox.md)
- [ ] MOD-CONV-04: Lọc và tìm kiếm hội thoại (Filter & Search Inbox) - [REQUIREMENTS](M07-conversation-inbox/REQUIREMENTS-conversation-inbox.md)
- [ ] MOD-CONV-05: Gắn thẻ hội thoại (Conversation Tagging) - [REQUIREMENTS](M07-conversation-inbox/REQUIREMENTS-conversation-inbox.md)
- [ ] MOD-CONV-06: Quản lý mẫu tin nhắn nhanh (Quick Reply Templates) - [REQUIREMENTS](M07-conversation-inbox/REQUIREMENTS-conversation-inbox.md)
- [ ] MOD-CONV-07: Theo dõi thời gian phản hồi (SLA Tracking) - [REQUIREMENTS](M07-conversation-inbox/REQUIREMENTS-conversation-inbox.md)
- [ ] MOD-CONV-08: Gửi tin nhắn riêng tư từ bình luận (Private Replies) - [REQUIREMENTS](M07-conversation-inbox/REQUIREMENTS-conversation-inbox.md)

## 10. M09 — Routing & Assignment
*(Phụ thuộc: M01, M07)*
- [ ] Khởi tạo Module Routing & Assignment (Chờ cập nhật REQUIREMENTS)

## 11. M11 — Analytics & Reporting
*(Phụ thuộc: M07, M02)*
- [ ] Khởi tạo Module Analytics & Reporting (Chờ cập nhật REQUIREMENTS)

## 12. M10 — Realtime Delivery
*(Phụ thuộc: M07, M09)*
- [ ] MOD-REAL-01: Quản lý kết nối WebSocket (Connection Management) - [REQUIREMENTS](M10-realtime-delivery/REQUIREMENTS-realtime-delivery.md)
- [ ] MOD-REAL-02: Định tuyến và Đẩy sự kiện cá nhân (Targeted Event Push) - [REQUIREMENTS](M10-realtime-delivery/REQUIREMENTS-realtime-delivery.md)
- [ ] MOD-REAL-03: Phát sóng dữ liệu nhóm (Group/Room Broadcast) - [REQUIREMENTS](M10-realtime-delivery/REQUIREMENTS-realtime-delivery.md)
- [ ] MOD-REAL-04: Multi-instance Pub/Sub (Redis Pub/Sub Sync) - [REQUIREMENTS](M10-realtime-delivery/REQUIREMENTS-realtime-delivery.md)
- [ ] MOD-REAL-05: Đồng bộ trạng thái kết nối (Presence/Status Sync) - [REQUIREMENTS](M10-realtime-delivery/REQUIREMENTS-realtime-delivery.md)

## 13. M12 — Notification
*(Phụ thuộc: M07, M10)*
- [ ] Khởi tạo Module Notification (Chờ cập nhật REQUIREMENTS)
