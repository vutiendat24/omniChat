# Module: M13 - User Service (Yêu cầu chi tiết)

> **Lưu ý:** Các chức năng dưới đây được triển khai theo các thiết kế trong tài liệu [Chiến lược quản lý tài khoản](ACCOUNT_MANAGEMENT_STRATEGY.md).

## 1. MOD-USR-01: Quản lý Hồ sơ cá nhân (User Profile Management)

### 1. Mô tả nghiệp vụ đầy đủ
Chức năng này cho phép tất cả người dùng trong hệ thống (Platform Account hoặc Tenant Account) cập nhật các thông tin cá nhân cơ bản của mình (họ tên, ảnh đại diện) và thay đổi mật khẩu an toàn.
- **Bước 1:** Người dùng truy cập trang "Cài đặt tài khoản" trên giao diện Web/App. Frontend gọi API để lấy thông tin profile hiện tại (thông qua API `GET /api/v1/users/me` hoặc JWT claims).
- **Bước 2 (Cập nhật thông tin):** Người dùng chỉnh sửa họ tên hoặc thay đổi ảnh đại diện (ảnh được upload trực tiếp qua Storage/S3 service và trả về URL). Hệ thống nhận request cập nhật chứa URL này.
- **Bước 3 (Đổi mật khẩu):** (Nếu có nhu cầu) Người dùng nhập mật khẩu cũ và mật khẩu mới. Hệ thống truy vấn DB, xác thực mật khẩu cũ bằng BCrypt và lưu mã băm của mật khẩu mới.
- **Bước 4:** Hệ thống lưu thông tin mới vào cơ sở dữ liệu.
- **Bước 5:** Hệ thống sinh sự kiện (Kafka event) để thông báo cho các module khác (như module Conversation) đồng bộ hóa dữ liệu cache nếu cần (VD: Cập nhật tên Agent hiển thị trên tin nhắn).

### 2. Input
- **API Cập nhật thông tin cơ bản (`PUT /api/v1/users/me`):**
  - `fullName` (String, Bắt buộc): Họ và tên đầy đủ.
  - `avatarUrl` (String, Tùy chọn): Link ảnh đại diện hợp lệ.
- **API Đổi mật khẩu (`PUT /api/v1/users/me/password`):**
  - `oldPassword` (String, Bắt buộc): Mật khẩu hiện hành.
  - `newPassword` (String, Bắt buộc): Mật khẩu mới.
- **Nguồn:** Gửi từ Frontend (Admin Dashboard), định danh bằng `Authorization: Bearer <JWT_Token>`.

### 3. Output
- **Kết quả trả về:** JSON object chứa thông tin user mới nhất, mã trạng thái HTTP 200 OK.
- **Nơi lưu trữ:** Lưu trực tiếp vào bảng `USER` (Database `omnichat_user` hoặc `omnichat_auth` tùy thiết kế database chung).
- **Nơi gửi tiếp:** Gửi event `UserProfileUpdatedEvent(userId, fullName, avatarUrl)` vào Kafka topic `omnichat.user.events`.

### 4. Business rule (Ràng buộc)
- `fullName`: Độ dài từ 2 đến 100 ký tự. Chặn các ký tự HTML/Script nguy hiểm (XSS prevention).
- `avatarUrl`: Phải là định dạng URL hợp lệ (bắt đầu bằng http/https) trỏ tới hệ thống CDN/Storage nội bộ.
- **Ràng buộc đổi mật khẩu:**
  - Mật khẩu mới phải tuân thủ chuẩn an toàn: tối thiểu 8 ký tự, có chứa ít nhất 1 chữ hoa, 1 chữ thường, 1 số và 1 ký tự đặc biệt.
  - Không được phép sử dụng lại mật khẩu giống hệt mật khẩu cũ.
- Tự bảo vệ: Người dùng không thể tự thay đổi `status` (Trạng thái) hoặc `role` (Quyền) của chính mình thông qua API này.

### 5. Edge case cần xử lý
- **Chống Brute-force mật khẩu:** Nếu nhập sai mật khẩu cũ quá 5 lần, hệ thống sẽ phạt và tạm khóa tài khoản (LOCKED) trong vòng 30 phút. Người dùng bị force logout.
- **Tài khoản đang bị khóa (LOCKED / SUSPENDED):** Nếu vì lý do nào đó token vẫn còn sống nhưng DB báo user đã bị khóa, API phải từ chối mọi thay đổi và trả về HTTP 403 Forbidden.
- **Phiên đăng nhập khác:** Khi một người dùng đổi mật khẩu thành công trên thiết bị A, hệ thống sẽ vô hiệu hóa tất cả các Refresh Token đang tồn tại trên các thiết bị khác (B, C) để ép chúng đăng nhập lại bằng mật khẩu mới.
- **Lỗi hệ thống lưu trữ (Kafka/DB down):** Bọc trong `@Transactional` để đảm bảo tính nhất quán (Consistency). Nếu Kafka không gửi được event, có thể thiết kế Retry cơ bản hoặc dùng Outbox Pattern.

### 6. Acceptance criteria
- **Given** người dùng (Agent A) đang đăng nhập hợp lệ.
- **When** gửi request đổi `fullName` thành tên mới và có chứa mã độc XSS `<script>alert(1)</script>`.
- **Then** hệ thống trả về mã 400 Bad Request, từ chối lưu.
- **Given** Agent A thực hiện đổi mật khẩu.
- **When** nhập sai mật khẩu cũ.
- **Then** hệ thống trả về 400 Bad Request kèm số lần thử còn lại.
- **Given** Agent A thực hiện đổi mật khẩu.
- **When** nhập đúng mật khẩu cũ và mật khẩu mới hợp lệ.
- **Then** hệ thống lưu thành công (200 OK), đồng thời các thiết bị khác đang đăng nhập tài khoản của Agent A sẽ bị yêu cầu đăng nhập lại ở lần gửi request tiếp theo.

### 7. Chỉ số phi chức năng
- **Latency (Độ trễ):** API cập nhật thông tin xử lý dưới 200ms. API đổi mật khẩu (bao gồm thời gian verify và hash BCrypt) xử lý dưới 800ms.
- **Security:** Hành động đổi mật khẩu phải được ghi nhận vào bảng Audit Log (bao gồm UserId, Action="CHANGE_PASSWORD", Timestamp, IP).

---

## 2. MOD-USR-02: Quản lý Thành viên Workspace (Workspace Member Management)

### 1. Mô tả nghiệp vụ đầy đủ
Cho phép Owner, Admin hoặc Manager quản lý (Mời, khóa, xóa) các thành viên trong Tenant của mình. Quá trình xử lý phải tuân thủ nghiêm ngặt Role Hierarchy (chỉ được thao tác trên người có Level thấp hơn).
- **Bước 1:** Quản trị viên truy cập màn hình "Thành viên". Hệ thống gọi API lấy danh sách thành viên trong Tenant (có phân trang, tìm kiếm, lọc theo Role).
- **Bước 2 (Mời thành viên):** Quản trị viên nhập Email và chọn Role cần cấp. Hệ thống kiểm tra xem Role được chọn có Level thấp hơn Level của người mời hay không. Nếu hợp lệ, hệ thống tạo bản ghi User (nếu chưa tồn tại) và bản ghi WorkspaceMember với trạng thái PENDING. Sau đó gửi Email chứa link kích hoạt.
- **Bước 3 (Khóa/Xóa thành viên):** Quản trị viên chọn thành viên cần xử lý. Hệ thống kiểm tra điều kiện quyền (Permission check) VÀ cấp bậc (Actor.Level > Target.Level).
- **Bước 4:** Xóa mềm (Soft Delete) bản ghi WorkspaceMember hoặc đổi trạng thái thành INACTIVE.
- **Bước 5:** Ghi nhận lịch sử thao tác vào Audit Log.

### 2. Input
- **API Mời thành viên (`POST /api/v1/workspaces/{id}/members/invite`):** `email` (Bắt buộc, chuẩn định dạng), `roleId` (Bắt buộc).
- **API Khóa/Xóa (`DELETE /api/v1/workspaces/{id}/members/{userId}`):** `userId` của mục tiêu.
- **Nguồn:** Request từ Frontend Dashboard, đính kèm JWT Token và header `X-Tenant-ID`.

### 3. Output
- **Kết quả trả về:** JSON kết quả thao tác, mã HTTP 200 OK hoặc HTTP 403 Forbidden nếu vi phạm Hierarchy.
- **Nơi lưu trữ:** Cập nhật vào bảng `TENANT_MEMBER` (hoặc `WORKSPACE_MEMBER`).
- **Nơi gửi tiếp:** Sinh event `MemberInvitedEvent` hoặc `MemberRemovedEvent` vào Kafka để module Notification gửi email, và module Routing/Conversation ngưng phân bổ tin nhắn mới cho Agent vừa bị xóa.

### 4. Business rule (Ràng buộc)
- **Role Hierarchy:** Người thực hiện (Actor) phải có `Level > Level của Role sắp gán`, VÀ `Level > Level của thành viên bị tác động` (Target).
- **Cấm mời Owner:** Không được phép mời một người vào làm Owner (Role Level 100). Mỗi Tenant chỉ có duy nhất 1 Owner và chỉ được thay đổi qua quy trình Transfer Ownership.

### 5. Edge case cần xử lý
- **Tự xóa chính mình:** Actor truyền `userId` của chính mình để khóa/xóa -> Hệ thống lập tức chặn (Self-destruction prevention).
- **Target là Owner:** Hệ thống từ chối mọi nỗ lực khóa/xóa Owner (ngay cả từ Platform Admin), trừ khi bản thân toàn bộ Tenant đó bị xóa.
- **Xử lý tài sản tồn đọng:** Khi một Agent bị khóa/xóa, các hội thoại đang được gán (assign) cho Agent đó phải được tự động nhả ra (unassign) hoặc đẩy về hàng đợi chung. (Bắt Kafka event ở Conversation Service để thực thi).

### 6. Acceptance criteria
- **Given** Admin (Level 80) thao tác. **When** gửi request xóa Manager (Level 60). **Then** thành công, trạng thái member đổi thành INACTIVE.
- **Given** Admin (Level 80). **When** cố gắng xóa Admin khác (Level 80) hoặc Owner (Level 100). **Then** nhận lỗi HTTP 403 Forbidden.
- **Given** Manager (Level 60) mời thành viên. **When** chọn Role để mời là Admin (Level 80). **Then** nhận lỗi HTTP 403 Forbidden.

### 7. Chỉ số phi chức năng
- **Data Integrity:** Xóa thành viên phải là Xóa mềm (sử dụng `@SQLDelete` và `@Where` của Hibernate), tuyệt đối không dùng lệnh `DELETE` SQL để giữ toàn vẹn lịch sử chat.
- **Latency:** API xử lý logic Hierarchy và lưu DB phải hoàn tất < 300ms.

---

## 3. MOD-USR-03: Quản lý Vai trò (Role Management)

### 1. Mô tả nghiệp vụ đầy đủ
Chức năng cho phép hệ thống (Platform Admin) hoặc chủ Workspace (Owner) tạo mới, chỉnh sửa và định nghĩa Cấp bậc (Level) cho các Vai trò (Role).
- **Bước 1:** Hiển thị danh sách các Role hiện có trong Tenant (bao gồm cả Role mặc định của Platform).
- **Bước 2 (Tạo Role mới):** Người dùng nhập tên Role, mô tả và cấu hình Level (phải nhỏ hơn Level của người tạo).
- **Bước 3 (Sửa/Xóa Role):** Chọn Role cần chỉnh sửa. Chỉ được xóa Role nếu không có bất kỳ thành viên nào đang giữ Role này.

### 2. Input
- **API Tạo/Sửa Role (`POST/PUT /api/v1/roles`):** `name`, `description`, `level`.

### 3. Output
- **Lưu trữ:** Lưu vào bảng `ROLE` trong Database.

### 4. Business rule (Ràng buộc)
- `name` của Role phải là duy nhất (Unique) trong phạm vi một Tenant.
- `level` của Role mới tạo phải NHỎ HƠN `level` của người thực hiện tạo (Actor).
- Các Role mặc định của hệ thống (System Default Roles như OWNER, ADMIN) không được phép sửa tên, sửa level hoặc xóa.
- Không cho phép xóa Role nếu đang có >= 1 User được gán Role này.

### 5. Edge case cần xử lý
- **Concurrency (Tranh chấp):** 2 Admin cùng lúc tạo Role với trùng tên trong 1 mili-giây -> Cần thiết lập Unique Constraint `(tenant_id, name)` trong Database để quăng lỗi `DataIntegrityViolationException`.

### 6. Acceptance criteria
- **Given** Role "Marketing" đang có 5 thành viên được gán. **When** Admin gọi API xóa Role này. **Then** HTTP 409 Conflict, hệ thống báo lỗi yêu cầu chuyển Role của 5 thành viên sang Role khác trước khi xóa.

### 7. Chỉ số phi chức năng
- **Caching:** Danh sách Role rất ít khi thay đổi nhưng lại được query liên tục để check quyền, cần áp dụng Cache (Redis) và tự động invalidate khi có cập nhật.

---

## 4. MOD-USR-04: Phân quyền RBAC (Permission Management)

### 1. Mô tả nghiệp vụ đầy đủ
Chức năng này dùng để định nghĩa và gán các hạt quyền (Permissions) cụ thể vào các Role.
- **Bước 1:** Giao diện hiển thị ma trận gồm các Role (cột) và Permissions (hàng).
- **Bước 2:** Quản trị viên tick/untick để bật tắt các quyền cho một Role cụ thể (VD: Gán quyền `REPORT_VIEW` cho Role `MANAGER`).
- **Bước 3:** Hệ thống xác nhận và cập nhật danh sách Permission của Role xuống Database.

### 2. Input
- **API Gán quyền (`PUT /api/v1/roles/{id}/permissions`):** Mảng chứa các `permissionId`.

### 3. Output
- **Lưu trữ:** Bảng mapping `ROLE_PERMISSION` (Xóa mapping cũ, Insert mapping mới trong cùng 1 Transaction).

### 4. Business rule (Ràng buộc)
- **Principle of Least Privilege:** Một Role mới được tạo ra mặc định không có bất kỳ quyền nào.
- Quản trị viên chỉ có thể cấp cho Role khác những Permission mà chính bản thân quản trị viên đó đang sở hữu. (Không thể cấp quyền mà mình không có).

### 5. Edge case cần xử lý
- **Đồng bộ hóa Token (Token Invalidation):** JWT là stateless và chứa Role bên trong. Ngay khi Role bị thay đổi Permission, làm sao hệ thống biết? -> Thiết kế Gateway/AuthFilter phải check thêm danh sách Permission bị thu hồi qua Redis Cache, thay vì chỉ tin tưởng 100% vào chuỗi JWT.

### 6. Acceptance criteria
- **Given** Role AGENT vừa bị quản trị viên tước quyền `USER_DELETE`. **When** Agent (vẫn đang giữ Token cũ còn hạn) gọi API xóa user. **Then** AuthFilter bắt được sự thay đổi qua Cache và chặn request (403 Forbidden).

---

## 5. MOD-USR-05: Gán Quyền (Assign / Change Role)

### 1. Mô tả nghiệp vụ đầy đủ
Cập nhật hoặc thay đổi Role của một thành viên đã nằm trong Tenant.
- **Bước 1:** Quản trị viên chọn thành viên cần đổi Role và chọn Role mới từ Dropdown list.
- **Bước 2:** Hệ thống thực hiện 2 kiểm tra cấp bậc (Hierarchy Check):
  1. `Actor.Level > Target_CurrentRole.Level` (Có quyền tác động lên người này không?)
  2. `Actor.Level > Target_NewRole.Level` (Có quyền cấp Role mức này không?)
- **Bước 3:** Cập nhật bảng liên kết thành viên.

### 2. Input
- **API Đổi Role (`PATCH /api/v1/workspaces/{id}/members/{userId}/role`):** `newRoleId`.

### 3. Output
- **Lưu trữ:** Cập nhật bản ghi trong bảng `TENANT_MEMBER`.
- **Gửi tiếp:** Kafka event `UserRoleChangedEvent`.

### 4. Business rule (Ràng buộc)
- Không được phép cấp Role OWNER thông qua luồng API này.
- Nghiêm cấm tự đổi Role của chính bản thân mình (Self-elevation).

### 5. Edge case cần xử lý
- Thay đổi Role khi User mục tiêu (Target) đang online: Hệ thống phải publish event qua WebSocket (Realtime Delivery) để ép UI của Target reload, lấy Token mới tương ứng với quyền hạn mới.

### 6. Acceptance criteria
- **Given** Admin (Level 80) đổi quyền cho Agent (Level 20). **When** cấp Role mới là Manager (Level 60). **Then** Thành công (200 OK).
- **Given** Admin (Level 80). **When** tự thăng cấp chính mình thành Owner (Level 100). **Then** Thất bại (403 Forbidden).

---

## 6. MOD-USR-06: Chuyển quyền sở hữu (Transfer Ownership)

### 1. Mô tả nghiệp vụ đầy đủ
Chuyển vị trí Owner cao nhất cho một Admin khác trong Tenant, đảm bảo nguyên tắc Tenant luôn có duy nhất 1 Owner.
- **Bước 1:** Owner hiện hành truy cập Cài đặt -> Transfer Ownership.
- **Bước 2:** Chọn 1 Admin hợp lệ từ danh sách thành viên để trao quyền.
- **Bước 3:** Hệ thống bắt buộc xác thực bảo mật (nhập mật khẩu hoặc mã OTP) để xác nhận hành động cực kỳ nhạy cảm này.
- **Bước 4:** Xử lý Transaction cập nhật DB: Hạ cấp Owner cũ thành Admin (Level 80) VÀ thăng cấp Admin được chọn thành Owner (Level 100).

### 2. Input
- **API Transfer (`POST /api/v1/workspaces/{id}/transfer-ownership`):** `newOwnerUserId`, `password` (Dùng để verify lại danh tính người thao tác).

### 3. Output
- **Lưu trữ:** Swap role của 2 bản ghi trong bảng `TENANT_MEMBER`.

### 4. Business rule (Ràng buộc)
- CHỈ CÓ User đang mang Role Owner mới có quyền gọi API này. Không ai khác có thể gọi, kể cả Platform Admin (trừ phi dùng script can thiệp thẳng DB).
- Người nhận chuyển nhượng phải đang là thành viên của Tenant và đang ở trạng thái ACTIVE.

### 5. Edge case cần xử lý
- **Database crash giữa chừng:** Bắt buộc sử dụng `@Transactional` bao bọc toàn bộ khối lệnh. Đảm bảo nguyên tắc ACID: Không bao giờ được phép xảy ra tình trạng có 2 Owner hoặc 0 Owner sau sự cố.

### 6. Acceptance criteria
- **Given** Owner A chuyển quyền cho Admin B. **When** A nhập đúng mật khẩu xác nhận. **Then** B trở thành Owner mới, A trở thành Admin, và mọi quyền lực đặc biệt của A lập tức bị thu hồi ở các phiên làm việc hiện tại.
