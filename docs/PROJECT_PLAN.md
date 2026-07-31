# Kế hoạch triển khai OmniChat (Project Plan)

> **Cập nhật:** 2026-07-31
> **Thứ tự ưu tiên:** Được sắp xếp theo cây phụ thuộc (Dependency) giữa các Bounded Context. Agent vui lòng đọc file này đầu mỗi phiên làm việc để nắm tiến độ và thiết kế chi tiết.

## 1. M00 - Platform Infrastructure
*Phụ thuộc: (Không có)*
- [ ] Thiết lập hệ thống (Config Server, API Gateway, Service Discovery).

## 2. M13 - User Service
*Phụ thuộc: M00*
- [x] MOD-USR-01: Quản lý Hồ sơ cá nhân (User Profile Management) 🔗 [Chi tiết](M13-user-service/REQUIREMENTS-user-service.md)
- [x] MOD-USR-02: Quản lý Thành viên Workspace (Workspace Member Management) 🔗 [Chi tiết](M13-user-service/REQUIREMENTS-user-service.md)
- [x] MOD-USR-03: Quản lý Vai trò (Role Management) 🔗 [Chi tiết](M13-user-service/REQUIREMENTS-user-service.md)
- [x] MOD-USR-04: Quản lý Phân quyền (Permission / RBAC Management) 🔗 [Chi tiết](M13-user-service/REQUIREMENTS-user-service.md)
- [x] MOD-USR-05: Gán và Đổi Vai trò (Assign/Change Role) 🔗 [Chi tiết](M13-user-service/REQUIREMENTS-user-service.md)
- [ ] MOD-USR-06: Chuyển quyền Chủ sở hữu (Transfer Ownership) 🔗 [Chi tiết](M13-user-service/REQUIREMENTS-user-service.md)

## 3. M01 - Identity & Access
*Phụ thuộc: M00, M13*
- [x] MOD-IAM-01: Đăng ký tài khoản hệ thống (User Registration) 🔗 [Chi tiết](M01-identity-access/REQUIREMENTS-identity-access.md)
- [x] MOD-IAM-02: Đăng nhập hệ thống (Local Login) 🔗 [Chi tiết](M01-identity-access/REQUIREMENTS-identity-access.md)
- [x] MOD-IAM-03: Đăng nhập qua Google (Google OAuth2 SSO) 🔗 [Chi tiết](M01-identity-access/REQUIREMENTS-identity-access.md)
- [x] MOD-IAM-04: Làm mới Token (Refresh JWT) 🔗 [Chi tiết](M01-identity-access/REQUIREMENTS-identity-access.md)
- [x] MOD-IAM-05: Đăng xuất (Logout & Blacklist Token) 🔗 [Chi tiết](M01-identity-access/REQUIREMENTS-identity-access.md)
- [x] MOD-IAM-08: Lấy thông tin tài khoản hiện tại (Get Current Profile / Introspect) 🔗 [Chi tiết](M01-identity-access/REQUIREMENTS-identity-access.md)

## 4. M02 - Tenant & Organization
*Phụ thuộc: M13*
- [x] MOD-TENANT-01: Tạo mới Tenant (Onboarding) 🔗 [Chi tiết](M02-tenant-organization/REQUIREMENTS-tenant-organization.md)
- [x] MOD-TENANT-02: Cập nhật hồ sơ Tenant 🔗 [Chi tiết](M02-tenant-organization/REQUIREMENTS-tenant-organization.md)
- [x] MOD-TENANT-03: Quản lý trạng thái Tenant 🔗 [Chi tiết](M02-tenant-organization/REQUIREMENTS-tenant-organization.md)
- [x] MOD-TENANT-04: Tạo mới Team 🔗 [Chi tiết](M02-tenant-organization/REQUIREMENTS-tenant-organization.md)
- [x] MOD-TENANT-05: Cập nhật thông tin Team 🔗 [Chi tiết](M02-tenant-organization/REQUIREMENTS-tenant-organization.md)
- [x] MOD-TENANT-06: Xóa/Vô hiệu hóa Team 🔗 [Chi tiết](M02-tenant-organization/REQUIREMENTS-tenant-organization.md)
- [x] MOD-TENANT-07: Thêm/Mời thành viên vào Tenant 🔗 [Chi tiết](M02-tenant-organization/REQUIREMENTS-tenant-organization.md)
- [x] MOD-TENANT-08: Gán thành viên vào Team 🔗 [Chi tiết](M02-tenant-organization/REQUIREMENTS-tenant-organization.md)
- [x] MOD-TENANT-09: Hủy tư cách thành viên (Remove Member) 🔗 [Chi tiết](M02-tenant-organization/REQUIREMENTS-tenant-organization.md)
- [x] MOD-TENANT-10: Cấu hình giờ làm việc (Business Hours) 🔗 [Chi tiết](M02-tenant-organization/REQUIREMENTS-tenant-organization.md)
- [ ] MOD-TENANT-11: Cấu hình chính sách SLA (SLA Policy) 🔗 [Chi tiết](M02-tenant-organization/REQUIREMENTS-tenant-organization.md)

## 5. M08 - Spam Filter & Moderation
*Phụ thuộc: M02*
- [ ] *Đang chờ thiết kế chi tiết (Drafting...)*

## 6. M06 - Customer Management
*Phụ thuộc: M01, M02*
- [ ] *Đang chờ thiết kế chi tiết (Drafting...)*

## 7. M03 - Channel Integration
*Phụ thuộc: M01, M02*
- [x] MOD-CI-01: Kết nối kênh (OAuth2 Connect) 🔗 [Chi tiết](M03-channel-integration/REQUIREMENTS-channel-integration.md)
- [x] MOD-CI-02: Ngắt kết nối (Disconnect Channel) 🔗 [Chi tiết](M03-channel-integration/REQUIREMENTS-channel-integration.md)
- [x] MOD-CI-03: Tự động làm mới Token (Auto-refresh Token) 🔗 [Chi tiết](M03-channel-integration/REQUIREMENTS-channel-integration.md)
- [x] MOD-CI-04: Tiếp nhận Webhook (Receive Webhook) 🔗 [Chi tiết](M03-channel-integration/REQUIREMENTS-channel-integration.md)
- [x] MOD-CI-05: Xác thực Webhook (Verify Webhook Signature) 🔗 [Chi tiết](M03-channel-integration/REQUIREMENTS-channel-integration.md)
- [x] MOD-CI-06: Chuẩn hóa dữ liệu (Inbound Normalization) 🔗 [Chi tiết](M03-channel-integration/REQUIREMENTS-channel-integration.md)
- [x] MOD-CI-07: Gửi tin nhắn (Outbound Delivery) 🔗 [Chi tiết](M03-channel-integration/REQUIREMENTS-channel-integration.md)

## 8. M04 - Livestream Connector
*Phụ thuộc: M01, M02*
- [ ] *Đang chờ thiết kế chi tiết (Drafting...)*

## 9. M05 - Livestream Chat Aggregator
*Phụ thuộc: M04*
- [ ] *Đang chờ thiết kế chi tiết (Drafting...)*

## 10. M07 - Conversation & Inbox
*Phụ thuộc: M03, M05, M06*
- [x] MOD-CONV-01: Tạo mới hội thoại (Create Conversation) 🔗 [Chi tiết](M07-conversation-inbox/REQUIREMENTS-conversation-inbox.md)
- [x] MOD-CONV-02: Cập nhật trạng thái hội thoại (Update Conversation Status) 🔗 [Chi tiết](M07-conversation-inbox/REQUIREMENTS-conversation-inbox.md)
- [x] MOD-CONV-03: Lưu trữ & đồng bộ tin nhắn (Save & Sync Message) 🔗 [Chi tiết](M07-conversation-inbox/REQUIREMENTS-conversation-inbox.md)
- [x] MOD-CONV-04: Lọc và tìm kiếm hội thoại (Filter & Search Inbox) 🔗 [Chi tiết](M07-conversation-inbox/REQUIREMENTS-conversation-inbox.md)
- [x] MOD-CONV-05: Gắn thẻ hội thoại (Conversation Tagging) 🔗 [Chi tiết](M07-conversation-inbox/REQUIREMENTS-conversation-inbox.md)
- [x] MOD-CONV-06: Quản lý mẫu tin nhắn nhanh (Quick Reply Templates) 🔗 [Chi tiết](M07-conversation-inbox/REQUIREMENTS-conversation-inbox.md)
- [x] MOD-CONV-07: Theo dõi thời gian phản hồi (SLA Tracking) 🔗 [Chi tiết](M07-conversation-inbox/REQUIREMENTS-conversation-inbox.md)
- [x] MOD-CONV-08: Gửi tin nhắn riêng tư từ bình luận (Private Replies) 🔗 [Chi tiết](M07-conversation-inbox/REQUIREMENTS-conversation-inbox.md)

## 11. M09 - Routing & Assignment
*Phụ thuộc: M01, M07*
- [ ] *Đang chờ thiết kế chi tiết (Drafting...)*

## 12. M11 - Analytics & Reporting
*Phụ thuộc: M07, M02*
- [ ] *Đang chờ thiết kế chi tiết (Drafting...)*

## 13. M10 - Realtime Delivery
*Phụ thuộc: M07, M09*
- [x] MOD-REAL-01: Quản lý kết nối WebSocket (Connection Management) 🔗 [Chi tiết](M10-realtime-delivery/REQUIREMENTS-realtime-delivery.md)
- [x] MOD-REAL-02: Định tuyến và Đẩy sự kiện cá nhân (Targeted Event Push) 🔗 [Chi tiết](M10-realtime-delivery/REQUIREMENTS-realtime-delivery.md)
- [x] MOD-REAL-03: Phát sóng dữ liệu nhóm (Group/Room Broadcast) 🔗 [Chi tiết](M10-realtime-delivery/REQUIREMENTS-realtime-delivery.md)
- [x] MOD-REAL-04: Multi-instance Pub/Sub (Redis Pub/Sub Sync) 🔗 [Chi tiết](M10-realtime-delivery/REQUIREMENTS-realtime-delivery.md)
- [x] MOD-REAL-05: Đồng bộ trạng thái kết nối (Presence/Status Sync) 🔗 [Chi tiết](M10-realtime-delivery/REQUIREMENTS-realtime-delivery.md)

## 14. M12 - Notification
*Phụ thuộc: M07, M10*
- [ ] MOD-NOTIF-01: Gửi thông báo In-app (In-app Notification) 🔗 [Chi tiết](M12-notification/REQUIREMENTS-notification.md)
- [ ] MOD-NOTIF-02: Gửi thông báo Email (Email Notification) 🔗 [Chi tiết](M12-notification/REQUIREMENTS-notification.md)
- [ ] MOD-NOTIF-03: Gửi thông báo đẩy (Web/Mobile Push Notification) 🔗 [Chi tiết](M12-notification/REQUIREMENTS-notification.md)
- [ ] MOD-NOTIF-04: Cấu hình tùy chọn thông báo (Notification Preferences) 🔗 [Chi tiết](M12-notification/REQUIREMENTS-notification.md)
- [ ] MOD-NOTIF-05: Quản lý Mẫu thông báo (Notification Templates) 🔗 [Chi tiết](M12-notification/REQUIREMENTS-notification.md)
