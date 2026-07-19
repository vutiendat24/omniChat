# Business Requirements Document (BRD)

## 1. Tổng quan dự án (Executive Summary)
Dự án nhằm xây dựng một hệ thống phần mềm quản lý nhiều kênh mạng xã hội (Facebook, Zalo, Instagram, TikTok) tại một nơi duy nhất nhằm tối ưu quy trình làm việc của doanh nghiệp.

## 2. Mục tiêu kinh doanh (Business Objectives)
- Tăng 50% hiệu suất làm việc của đội ngũ CSKH thông qua việc sử dụng hộp thư hợp nhất.
- Giảm thiểu 90% tỷ lệ bỏ sót tin nhắn và bình luận của khách hàng.
- Cung cấp công cụ đo lường hiệu quả (Analytics) chính xác, realtime.

## 3. Phạm vi dự án (Scope)
**Trong phạm vi (In-scope):**
- Tích hợp API các nền tảng: Facebook Fanpage, Zalo Official Account (ZOA), Instagram Business, Tiktok.
- Tính năng Unified Inbox (Đồng bộ Chat & Comment).
- Tính năng lên lịch đăng bài (Post Scheduling).
- Quản lý phân quyền nội bộ (Role-based access control).

**Ngoài phạm vi (Out-of-scope cho Phase 1):**
- Tích hợp với các hệ thống ERP, CRM bên thứ ba (Salesforce, SAP,...).
- Tích hợp Sàn thương mại điện tử (Shopee, Lazada, Tiktok Shop).

## 4. Yêu cầu nghiệp vụ cấp cao
- **BR-01:** Hệ thống phải cho phép kết nối nhiều Fanpage, ZOA vào cùng một tài khoản hệ thống.
- **BR-02:** Nhận và gửi tin nhắn/bình luận theo thời gian thực (Real-time).
- **BR-03:** Phân bổ hội thoại tự động cho nhân viên CSKH.
- **BR-04:** Cung cấp báo cáo thống kê lượt tương tác, phản hồi theo nhân viên.
