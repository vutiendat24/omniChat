# Kế Hoạch Triển Khai Dự Án OmniChat

File này tổng hợp tiến độ triển khai các module và chức năng theo đúng thứ tự phụ thuộc (Dependency) được định nghĩa trong `MODULES.md`. Agent sẽ dùng file này để làm việc theo trình tự.

## 1. M00 — Platform Infrastructure
*(Không có phụ thuộc - Nền tảng lõi)*
- [ ] Thiết lập hệ thống cơ bản (Chờ cập nhật REQUIREMENTS)

## 2. M01 — Identity & Access
*(Phụ thuộc: M00)*
- [x] MOD-IAM-01: Đăng ký tài khoản hệ thống (User Registration) - [REQUIREMENTS](M01-identity-access/REQUIREMENTS-identity-access.md)

## 3. M02 — Tenant & Organization
*(Phụ thuộc: M01)*
- [ ] MOD-TENANT-01: Tạo mới Tenant (Onboarding) - [REQUIREMENTS](M02-tenant-organization/REQUIREMENTS-tenant-organization.md#mod-tenant-01---tạo-mới-tenant-onboarding)
- [ ] MOD-TENANT-02: Cập nhật hồ sơ Tenant - [REQUIREMENTS](M02-tenant-organization/REQUIREMENTS-tenant-organization.md#mod-tenant-02---cập-nhật-hồ-sơ-tenant)
- [ ] MOD-TENANT-03: Quản lý trạng thái Tenant - [REQUIREMENTS](M02-tenant-organization/REQUIREMENTS-tenant-organization.md#mod-tenant-03---quản-lý-trạng-thái-tenant)
- [ ] MOD-TENANT-04: Tạo mới Team - [REQUIREMENTS](M02-tenant-organization/REQUIREMENTS-tenant-organization.md#mod-tenant-04---tạo-mới-team)
- [ ] MOD-TENANT-05: Cập nhật thông tin Team - [REQUIREMENTS](M02-tenant-organization/REQUIREMENTS-tenant-organization.md#mod-tenant-05---cập-nhật-thông-tin-team)
- [ ] MOD-TENANT-06: Xóa/Vô hiệu hóa Team - [REQUIREMENTS](M02-tenant-organization/REQUIREMENTS-tenant-organization.md#mod-tenant-06---xóavô-hiệu-hóa-team)
- [ ] MOD-TENANT-07: Thêm/Mời thành viên vào Tenant - [REQUIREMENTS](M02-tenant-organization/REQUIREMENTS-tenant-organization.md#mod-tenant-07---thêmmời-thành-viên-vào-tenant)
- [ ] MOD-TENANT-08: Gán thành viên vào Team - [REQUIREMENTS](M02-tenant-organization/REQUIREMENTS-tenant-organization.md#mod-tenant-08---gán-thành-viên-vào-team)
- [ ] MOD-TENANT-09: Hủy tư cách thành viên (Remove Member) - [REQUIREMENTS](M02-tenant-organization/REQUIREMENTS-tenant-organization.md)
- [ ] MOD-TENANT-10: Cấu hình giờ làm việc (Business Hours) - [REQUIREMENTS](M02-tenant-organization/REQUIREMENTS-tenant-organization.md)
- [ ] MOD-TENANT-11: Cấu hình chính sách SLA (SLA Policy) - [REQUIREMENTS](M02-tenant-organization/REQUIREMENTS-tenant-organization.md)

## 4. Các Module giao tiếp nền tảng
*(Phụ thuộc: M01, M02)*

### M03 — Channel Integration
- [ ] MOD-CI-01: Kết nối kênh (OAuth2 Connect) - [REQUIREMENTS](M03-channel-integration/REQUIREMENTS-channel-integration.md#mod-ci-01-kết-nối-kênh-oauth2-connect)
- [ ] MOD-CI-02: Ngắt kết nối kênh (Disconnect Channel) - [REQUIREMENTS](M03-channel-integration/REQUIREMENTS-channel-integration.md)
- [ ] MOD-CI-03: Tự động làm mới Token (Auto-refresh Token) - [REQUIREMENTS](M03-channel-integration/REQUIREMENTS-channel-integration.md)
- [ ] MOD-CI-04: Tiếp nhận Webhook Inbound - [REQUIREMENTS](M03-channel-integration/REQUIREMENTS-channel-integration.md)
- [ ] MOD-CI-05: Xác thực Webhook (Verify Webhook Signature) - [REQUIREMENTS](M03-channel-integration/REQUIREMENTS-channel-integration.md)
- [ ] MOD-CI-06: Chuẩn hóa dữ liệu Inbound (Inbound Normalization) - [REQUIREMENTS](M03-channel-integration/REQUIREMENTS-channel-integration.md)
- [ ] MOD-CI-07: Gửi tin nhắn Outbound (Outbound Delivery) - [REQUIREMENTS](M03-channel-integration/REQUIREMENTS-channel-integration.md)

### M04 — Livestream Connector
- [ ] Khởi tạo Module Livestream Connector (Chờ cập nhật REQUIREMENTS)

### M06 — Customer Management
- [ ] Khởi tạo Module Customer Management (Chờ cập nhật REQUIREMENTS)

### M08 — Spam Filter & Moderation
- [ ] Khởi tạo Module Spam Filter & Moderation (Chờ cập nhật REQUIREMENTS)

## 5. M05 — Livestream Chat Aggregator
*(Phụ thuộc: M04)*
- [ ] Khởi tạo Module Livestream Chat Aggregator (Chờ cập nhật REQUIREMENTS)

## 6. M07 — Conversation & Inbox
*(Phụ thuộc: M03, M05, M06)*
- [ ] Khởi tạo Module Conversation & Inbox (Chờ cập nhật REQUIREMENTS)

## 7. M09 — Routing & Assignment
*(Phụ thuộc: M01, M07)*
- [ ] Khởi tạo Module Routing & Assignment (Chờ cập nhật REQUIREMENTS)

## 8. M11 — Analytics & Reporting
*(Phụ thuộc: M07, M02)*
- [ ] Khởi tạo Module Analytics & Reporting (Chờ cập nhật REQUIREMENTS)

## 9. M10 — Realtime Delivery
*(Phụ thuộc: M07, M09)*
- [ ] Khởi tạo Module Realtime Delivery (Chờ cập nhật REQUIREMENTS)

## 10. M12 — Notification
*(Phụ thuộc: M07, M10)*
- [ ] Khởi tạo Module Notification (Chờ cập nhật REQUIREMENTS)
