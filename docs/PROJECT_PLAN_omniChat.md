# Kế Hoạch Triển Khai (Project Plan) - OmniChat

Tài liệu này đóng vai trò là "Bản đồ công việc" (Roadmap/Task list) cho quá trình phát triển (coding).
Thứ tự các module dưới đây được sắp xếp theo đúng thứ tự phụ thuộc (Dependency Graph) trong kiến trúc Microservices đã định nghĩa tại [HLD.md](file:///d:/Project/omniChat/docs/HLD.md). Các module nền tảng sẽ được phát triển trước.

---

## 1. auth-service (M01 - Identity & Access)
*Module nền tảng, không phụ thuộc vào bất kỳ service nghiệp vụ nào.*
- [ ] Lên danh sách chức năng (Features) và yêu cầu chi tiết (Requirements).
- [ ] Lập trình các chức năng cốt lõi (Đăng nhập, JWT, RBAC, Google SSO).

## 2. tenant-service (M02 - Tenant & Organization)
*Phụ thuộc vào: `auth-service`*
- [ ] **MOD-TENANT-01:** Tạo mới Tenant (Onboarding) - [Xem Yêu cầu](file:///d:/Project/omniChat/docs/M02-tenant-organization/REQUIREMENTS-tenant-organization.md#mod-tenant-01---tạo-mới-tenant-onboarding)
- [ ] **MOD-TENANT-02:** Cập nhật hồ sơ Tenant - [Xem Yêu cầu](file:///d:/Project/omniChat/docs/M02-tenant-organization/REQUIREMENTS-tenant-organization.md#mod-tenant-02---cập-nhật-hồ-sơ-tenant)
- [ ] **MOD-TENANT-03:** Quản lý trạng thái Tenant - [Xem Yêu cầu](file:///d:/Project/omniChat/docs/M02-tenant-organization/REQUIREMENTS-tenant-organization.md#mod-tenant-03---quản-lý-trạng-thái-tenant)
- [ ] **MOD-TENANT-04:** Tạo mới Team - [Xem Yêu cầu](file:///d:/Project/omniChat/docs/M02-tenant-organization/REQUIREMENTS-tenant-organization.md#mod-tenant-04---tạo-mới-team)
- [ ] **MOD-TENANT-05:** Cập nhật thông tin Team - [Xem Yêu cầu](file:///d:/Project/omniChat/docs/M02-tenant-organization/REQUIREMENTS-tenant-organization.md#mod-tenant-05---cập-nhật-thông-tin-team)
- [ ] **MOD-TENANT-06:** Xóa/Vô hiệu hóa Team - [Xem Yêu cầu](file:///d:/Project/omniChat/docs/M02-tenant-organization/REQUIREMENTS-tenant-organization.md#mod-tenant-06---xóavô-hiệu-hóa-team)
- [ ] **MOD-TENANT-07:** Thêm/Mời thành viên vào Tenant - [Xem Yêu cầu](file:///d:/Project/omniChat/docs/M02-tenant-organization/REQUIREMENTS-tenant-organization.md#mod-tenant-07---thêmmời-thành-viên-vào-tenant)
- [ ] **MOD-TENANT-08:** Gán thành viên vào Team - [Xem Yêu cầu](file:///d:/Project/omniChat/docs/M02-tenant-organization/REQUIREMENTS-tenant-organization.md#mod-tenant-08---gán-thành-viên-vào-team)
- [ ] **MOD-TENANT-09:** Hủy tư cách thành viên - [Xem Yêu cầu](file:///d:/Project/omniChat/docs/M02-tenant-organization/REQUIREMENTS-tenant-organization.md#mod-tenant-09---hủy-tư-cách-thành-viên-remove-member)
- [ ] **MOD-TENANT-10:** Cấu hình giờ làm việc *(Chưa viết Requirements)*
- [ ] **MOD-TENANT-11:** Cấu hình chính sách SLA *(Chưa viết Requirements)*

## 3. customer-service (M06 - Customer Management)
*Phụ thuộc vào: `auth-service`, `tenant-service`*
- [ ] Lên danh sách chức năng (Features) và yêu cầu chi tiết (Requirements).
- [ ] Lập trình quản lý hồ sơ KH, merge profile đa kênh.

## 4. integration-service (M03 - Channel Integration)
*Phụ thuộc vào: `auth-service`, `tenant-service`*
- [ ] Lên danh sách chức năng (Features) và yêu cầu chi tiết (Requirements).
- [ ] Lập trình kết nối Webhook 1-1 (Facebook, Zalo) và quản lý Channel Token.

## 5. livestream-service (M04, M05 - Livestream)
*Phụ thuộc vào: `auth-service`, `tenant-service`*
- [ ] Lên danh sách chức năng (Features) và yêu cầu chi tiết (Requirements).
- [ ] Lập trình kết nối API/WSS Livestream (TikTok, Shopee, FB) & Gom nhóm comment.

## 6. conversation-service (M07, M08 - Inbox & Moderation)
*Phụ thuộc vào: `integration-service`, `livestream-service`, `customer-service`*
- [ ] Lên danh sách chức năng (Features) và yêu cầu chi tiết (Requirements).
- [ ] Lập trình core Unified Inbox (lưu tin nhắn, tải lịch sử hội thoại).
- [ ] Lập trình bộ lọc Spam realtime.

## 7. routing-service (M09 - Routing & Assignment)
*Phụ thuộc vào: `conversation-service`*
- [ ] Lên danh sách chức năng (Features) và yêu cầu chi tiết (Requirements).
- [ ] Lập trình luồng phân bổ hội thoại tự động (Round-robin) & quản lý State Agent.

## 8. realtime-service (M10 - Realtime Delivery)
*Phụ thuộc vào: `conversation-service`, `routing-service`*
- [ ] Lên danh sách chức năng (Features) và yêu cầu chi tiết (Requirements).
- [ ] Lập trình WebSocket Gateway để push tin nhắn mới tới giao diện Agent.

## 9. analytics-service (M11 - Analytics & Reporting)
*Phụ thuộc vào: Các service lõi sinh event.*
- [ ] Lên danh sách chức năng (Features) và yêu cầu chi tiết (Requirements).
- [ ] Lập trình luồng Event Sourcing, query thống kê báo cáo realtime.

## 10. notification-service (M12 - Notification)
*Phụ thuộc vào: Các service lõi sinh event.*
- [ ] Lên danh sách chức năng (Features) và yêu cầu chi tiết (Requirements).
- [ ] Lập trình Push Notification, gửi Email cảnh báo background.
