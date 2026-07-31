# Yêu cầu chi tiết: M02 - Tenant & Organization

## MOD-TENANT-01 - Tạo mới Tenant (Onboarding)

### 1. Mô tả nghiệp vụ đầy đủ
Luồng xử lý chính khi tạo một Tenant (Shop/Doanh nghiệp) mới trên hệ thống:
1. **Tiếp nhận yêu cầu:** Super Admin (hoặc System thông qua luồng khách hàng tự đăng ký) gửi yêu cầu tạo Tenant mới.
2. **Xác thực dữ liệu:** Hệ thống kiểm tra tính hợp lệ của dữ liệu đầu vào (tên shop, domain/slug đăng ký, gói cước).
3. **Lưu trữ dữ liệu:** Khởi tạo bản ghi Tenant mới trong cơ sở dữ liệu của module `Tenant & Organization`.
4. **Tích hợp tài khoản (Identity):** Gọi API/Gửi Event sang Module M01 (Identity & Access) để tạo tài khoản người dùng tương ứng với `ownerEmail` (nếu chưa có) và gán Role `Tenant_Owner` cho tài khoản này đối với Tenant vừa tạo.
5. **Cấu hình mặc định:** Khởi tạo các cấu hình mặc định cơ bản cho Tenant (Giờ làm việc mặc định 8h-17h, SLA mặc định).
6. **Publish sự kiện:** Gửi sự kiện `tenant.created` lên Message Broker để các module khác (như Customer Management, Channel Integration) chuẩn bị không gian dữ liệu (tạo thư mục, khởi tạo cache, hoặc schema...).

### 2. Input
- **Dữ liệu đầu vào (REST API Request):**
  - `tenantName` (Tên shop/doanh nghiệp, kiểu String, Bắt buộc)
  - `slug` hoặc `domain` (Mã định danh duy nhất của shop dùng cho URL đăng nhập, kiểu String, Bắt buộc)
  - `ownerEmail` (Email của chủ sở hữu, kiểu String, Bắt buộc)
  - `ownerName` (Tên người chủ sở hữu, kiểu String, Bắt buộc)
  - `planId` (Mã gói cước đăng ký: Trial, Basic, Pro..., kiểu String, Tùy chọn - mặc định là Trial)
- **Nguồn kích hoạt:** Từ giao diện Admin Dashboard (Super Admin) hoặc từ hệ thống Landing Page (Billing/Onboarding bên ngoài).

### 3. Output
- **Kết quả trả về (API Response):**
  - Trả về mã HTTP `201 Created`
  - Body: `tenantId` (UUID), `status` (ACTIVE), `createdAt`
- **Nơi lưu:** Database (Bảng `tenant` thuộc module M02).
- **Nơi gửi tiếp:** Publish Kafka/RabbitMQ Event `tenant.created` chứa `{ tenantId, slug, planId, createdAt }`.

### 4. Business rule / Ràng buộc
- **Tính duy nhất:** `slug` phải là duy nhất trên toàn hệ thống (Unique constraint). Chỉ chứa chữ cái viết thường, số và dấu gạch ngang (Regex: `^[a-z0-9-]+$`), không chứa khoảng trắng, độ dài từ 3-50 ký tự.
- **Quy tắc Email Chủ sở hữu:** `ownerEmail` phải đúng định dạng chuẩn. Một email có thể được làm Owner của nhiều Tenant (tùy vào mô hình kinh doanh cho phép một người tạo nhiều shop), nhưng với mỗi Tenant sẽ được cấp quyền độc lập.
- **Rate Limit (Chống Spam):** API tạo Tenant (nếu là luồng public/khách tự đăng ký) bị giới hạn tối đa tạo 3 Tenant / 1 IP / 1 Giờ.

### 5. Edge case cần xử lý
- **Race Condition khi đăng ký Slug trùng nhau:** Hai user nhập cùng 1 `slug` và submit cùng lúc -> Dựa vào Unique Constraint ở cấp độ Database, query nào đến sau sẽ bị lỗi `DuplicateKeyException`, hệ thống cần catch lỗi này và trả về `HTTP 409 Conflict`.
- **Mất kết nối với Module M01 (Identity):** Tạo Tenant ở M02 thành công nhưng gọi sang M01 để tạo/phân quyền tài khoản Owner bị lỗi timeout/down -> Phải áp dụng Saga Pattern để rollback: Xóa Tenant vừa tạo hoặc đưa về trạng thái `FAILED`, tránh có Tenant "rác" không ai truy cập được.
- **Message Broker down:** Không publish được sự kiện `tenant.created` -> Áp dụng Outbox Pattern: Lưu event vào bảng `outbox` cùng transaction lúc tạo Tenant, sau đó có job quét liên tục để retry gửi event.

### 6. Acceptance criteria
- **Scenario 1: Tạo Tenant thành công**
  - **Given** thông tin đăng ký hợp lệ và `slug` chưa tồn tại trong hệ thống.
  - **When** gửi request POST đến API tạo Tenant.
  - **Then** hệ thống trả về mã `201 Created` kèm `tenantId`.
  - **And** một bản ghi Tenant được tạo trong Database với trạng thái `ACTIVE`.
  - **And** sự kiện `tenant.created` được đẩy vào Message Broker.
- **Scenario 2: Trùng mã định danh (slug)**
  - **Given** `slug` "happy-shop" đã được đăng ký bởi người khác.
  - **When** user gửi request tạo Tenant mới với `slug` là "happy-shop".
  - **Then** hệ thống trả về mã `409 Conflict` với thông báo lỗi "Mã định danh (slug) đã được sử dụng".
- **Scenario 3: Rollback khi Identity Service gặp sự cố**
  - **Given** Module M01 (Identity) đang bị sập.
  - **When** gửi request tạo Tenant hợp lệ.
  - **Then** hệ thống trả về lỗi `500 Internal Server Error` (hoặc `503`).
  - **And** không có bản ghi Tenant nào được lưu cố định trong hệ thống (đã rollback thành công).

### 7. Chỉ số phi chức năng
- **Thời gian phản hồi (Latency):** Việc xử lý API nội bộ hoàn thành < 2000ms (2 giây) do bao gồm việc gọi sang service khác và thiết lập ban đầu.
- **Tính nhất quán (Consistency):** Yêu cầu tính nhất quán cuối cùng (Eventual Consistency) qua các module khác, nhưng đòi hỏi tính nhất quán ngay lập tức (Strong Consistency) giữa bản ghi Tenant (M02) và tài khoản Owner (M01).
- **Audit Logging:** Mọi hành động khởi tạo Tenant mới đều phải được lưu log lại (IP thực hiện, thời gian, dữ liệu input) vào hệ thống Log tập trung.

## MOD-TENANT-02 - Cập nhật hồ sơ Tenant

### 1. Mô tả nghiệp vụ đầy đủ
Luồng xử lý chính khi cập nhật thông tin hồ sơ của Tenant đang hoạt động:
1. **Truy cập trang cài đặt:** Người dùng (Tenant Admin) đăng nhập vào hệ thống, điều hướng tới phần "Cài đặt chung" (General Settings) của cửa hàng/doanh nghiệp.
2. **Hiển thị thông tin hiện tại:** Hệ thống gọi API lấy dữ liệu hồ sơ Tenant hiện tại (tên thương hiệu, logo, ngành nghề, email/số điện thoại liên hệ) và hiển thị lên form.
3. **Chỉnh sửa và Gửi yêu cầu:** Người dùng thay đổi thông tin (VD: đổi tên shop, upload logo mới) và nhấn "Lưu".
4. **Xác thực và Phân quyền:** Hệ thống kiểm tra quyền truy cập của người dùng (phải là Tenant Admin hoặc Super Admin), sau đó validate dữ liệu đầu vào.
5. **Cập nhật dữ liệu:** Lưu các thông tin mới vào bản ghi Tenant trong cơ sở dữ liệu.
6. **Publish sự kiện:** Gửi sự kiện `tenant.updated` lên Message Broker để thông báo cho các module khác (nếu cần đồng bộ thông tin định danh mới, ví dụ cập nhật tên shop trên các thông báo gửi khách hàng).

### 2. Input
- **Dữ liệu đầu vào (REST API Request):**
  - `tenantId` (UUID, lấy từ token hoặc URL param, Bắt buộc).
  - `tenantName` (Tên shop/thương hiệu, kiểu String, Tùy chọn).
  - `logoUrl` (Đường dẫn hình ảnh logo, kiểu String, Tùy chọn).
  - `industry` (Ngành nghề kinh doanh, kiểu String, Tùy chọn).
  - `contactEmail` (Email liên hệ CSKH, kiểu String, Tùy chọn).
  - `contactPhone` (Số điện thoại liên hệ CSKH, kiểu String, Tùy chọn).
  - `address` (Địa chỉ, kiểu String, Tùy chọn).
- **Nguồn kích hoạt:** Frontend Web App / Admin Dashboard gọi từ phía người dùng.

### 3. Output
- **Kết quả trả về (API Response):**
  - Trả về mã HTTP `200 OK`.
  - Body: Trả về đối tượng Tenant đã được cập nhật (chứa các trường mới, `updatedAt`).
- **Nơi lưu:** Database (Bảng `tenant` thuộc module M02).
- **Nơi gửi tiếp:** Publish Kafka/RabbitMQ Event `tenant.updated` chứa `{ tenantId, updatedFields, updatedAt }`.

### 4. Business rule / Ràng buộc
- **Phân quyền (Authorization):** Chỉ có `Tenant Admin` của chính Tenant đó, hoặc `Super Admin` hệ thống mới có quyền thực hiện API này.
- **Validate Logo:** Nếu cập nhật `logoUrl`, URL phải đúng định dạng và an toàn. (File tải lên đã qua API upload riêng, giới hạn kích thước).
- **Quy tắc độ dài:** `tenantName` tối thiểu 2 ký tự, tối đa 100 ký tự.
- **Không cho phép sửa định danh:** `slug` (mã định danh gốc của shop) không được phép chỉnh sửa qua API này để tránh gãy các liên kết/đường dẫn đang hoạt động.

### 5. Edge case cần xử lý
- **Lỗi từ dịch vụ lưu trữ file:** Nếu file logo bị lỗi hoặc bị xóa từ storage ngoài (S3/Cloudinary) trước khi bấm Lưu, API vẫn lưu URL, nhưng Frontend/Client nên xử lý fallback (hiển thị logo mặc định) khi load ảnh bị lỗi 404.
- **Cập nhật đồng thời (Concurrency Update):** Hai Admin cùng mở form và chỉnh sửa cùng lúc.
  -> Áp dụng Optimistic Locking (dùng trường `version` hoặc `updatedAt` trong request/DB). Nếu Admin 2 gửi request lưu với `version` cũ, trả về lỗi `HTTP 409 Conflict` yêu cầu tải lại dữ liệu mới nhất.
- **Thử cập nhật trường cấm:** Nếu Payload request cố tình truyền thêm trường `slug` hoặc `status` để lén lút cập nhật -> API phải bỏ qua các trường đó (Ignore) hoặc báo lỗi `HTTP 400 Bad Request` để tránh rủi ro bảo mật.

### 6. Acceptance criteria
- **Scenario 1: Cập nhật thành công tên và logo**
  - **Given** người dùng là Tenant Admin đang ở trang Cài đặt chung.
  - **When** nhập Tên shop mới, điền Logo URL hợp lệ và nhấn Lưu.
  - **Then** hệ thống trả về HTTP `200 OK`.
  - **And** thông tin trong CSDL thay đổi đúng với dữ liệu mới.
- **Scenario 2: Thiếu quyền truy cập**
  - **Given** người dùng chỉ có quyền Agent (Nhân viên CSKH).
  - **When** cố ý gửi request cập nhật thông tin Tenant của shop.
  - **Then** hệ thống trả về lỗi HTTP `403 Forbidden` với thông báo "Bạn không có quyền thực hiện hành động này".
- **Scenario 3: Xung đột dữ liệu (Concurrency)**
  - **Given** Admin A và Admin B cùng mở trang cập nhật hồ sơ với dữ liệu gốc (Version 1).
  - **When** Admin A lưu thành công (lên Version 2), sau đó Admin B nhấn Lưu (vẫn gửi kèm Version 1).
  - **Then** hệ thống trả về lỗi HTTP `409 Conflict` báo hiệu dữ liệu đã bị thay đổi bởi người khác, cần tải lại trang.

### 7. Chỉ số phi chức năng
- **Thời gian phản hồi (Latency):** API cập nhật xử lý nhanh gọn < 500ms.
- **Audit Logging:** Mọi hành động cập nhật thông tin hồ sơ phải được lưu lại trong Audit Log của Tenant, ghi rõ `userId` thực hiện, thời gian, và danh sách các trường trước/sau khi đổi (before-and-after snapshot).

## MOD-TENANT-03 - Quản lý trạng thái Tenant

### 1. Mô tả nghiệp vụ đầy đủ
Luồng xử lý chính khi thay đổi trạng thái hoạt động của một Tenant (Tạm khóa / Kích hoạt lại):
1. **Tiếp nhận yêu cầu:** Super Admin truy cập trang Quản trị hệ thống (Admin Portal), chọn một Tenant cụ thể và thực hiện hành động đổi trạng thái (Deactivate / Activate). Hành động này cũng có thể được kích hoạt tự động từ Module Billing/Subscription khi Tenant hết hạn gói cước.
2. **Xác nhận thay đổi:** Hệ thống hiển thị hộp thoại xác nhận, yêu cầu nhập lý do thay đổi trạng thái (VD: "Hết hạn thanh toán", "Vi phạm chính sách").
3. **Kiểm tra hợp lệ:** Hệ thống kiểm tra quyền (phải là Super Admin hoặc system call).
4. **Cập nhật trạng thái:** Hệ thống cập nhật trường `status` (VD: từ `ACTIVE` sang `INACTIVE` hoặc ngược lại) trong cơ sở dữ liệu và lưu lại lý do.
5. **Vô hiệu hóa phiên đăng nhập (Nếu khóa):** Nếu trạng thái mới là `INACTIVE` (Khóa/Vô hiệu hóa), hệ thống gọi API/gửi Event sang Module Identity (M01) để thu hồi token và vô hiệu hóa các phiên đăng nhập hiện tại của tất cả người dùng thuộc Tenant này.
6. **Publish sự kiện:** Gửi sự kiện `tenant.status_changed` lên Message Broker để các module khác nhận biết (VD: Module Channel ngừng nhận tin nhắn webhook từ mạng xã hội, Module Billing ghi nhận thay đổi).

### 2. Input
- **Dữ liệu đầu vào (REST API Request):**
  - `tenantId` (UUID, lấy từ URL param, Bắt buộc).
  - `status` (Trạng thái mới: `ACTIVE` hoặc `INACTIVE`/`SUSPENDED`, kiểu Enum, Bắt buộc).
  - `reason` (Lý do thay đổi trạng thái, kiểu String, Bắt buộc khi chuyển sang INACTIVE).
- **Nguồn kích hoạt:**
  - Admin Portal (Do Super Admin thao tác tay).
  - Internal Event/API Call từ Module Billing (Kích hoạt tự động khi hết hạn/thanh toán thành công).

### 3. Output
- **Kết quả trả về (API Response):**
  - Trả về mã HTTP `200 OK`.
  - Body: Trả về đối tượng Tenant đã cập nhật `status`, `statusReason` và `updatedAt`.
- **Nơi lưu:** Database (Bảng `tenant` thuộc module M02, cập nhật cột `status` và `statusReason`).
- **Nơi gửi tiếp:**
  - Publish Kafka/RabbitMQ Event `tenant.status_changed` chứa `{ tenantId, oldStatus, newStatus, reason, updatedAt }`.
  - Gọi API nội bộ sang Module M01 để kill session.

### 4. Business rule / Ràng buộc
- **Phân quyền (Authorization):** Chức năng này chỉ dành cho `Super Admin` hệ thống hoặc Server-to-Server call (System). `Tenant Admin` không được phép tự khóa hay mở khóa Tenant của mình qua API này.
- **Trạng thái hợp lệ:** Không thể chuyển từ trạng thái A sang chính trạng thái A (VD: đang `INACTIVE` thì không thể request chuyển sang `INACTIVE` tiếp).
- **Chặn truy cập khi bị khóa:** Khi Tenant bị chuyển sang `INACTIVE`, mọi API request thuộc Tenant đó từ Frontend/Mobile App của người dùng (Agent/Admin) đều phải bị chặn ở API Gateway hoặc Middleware và trả về lỗi `403 Forbidden` hoặc `423 Locked`.
- **Duy trì dữ liệu:** Khóa Tenant không có nghĩa là xóa dữ liệu. Toàn bộ tin nhắn, khách hàng, cấu hình vẫn được giữ nguyên trong DB để chờ khôi phục (trừ khi có chính sách tự động xóa dữ liệu quá hạn riêng rẽ).

### 5. Edge case cần xử lý
- **Lỗi gọi sang Identity Module (M01):** Nếu đổi trạng thái thành công trong M02 nhưng gọi API vô hiệu hóa session bên M01 thất bại (do timeout) -> Hệ thống vẫn tính là khóa thành công ở cấp Tenant (M02), dùng Event `tenant.status_changed` làm giải pháp fallback để M01 bắt được message từ Broker và kill session sau (Eventual Consistency).
- **Webhook đến từ nền tảng thứ ba khi đang khóa:** Khi Tenant bị vô hiệu hóa, nếu có khách hàng nhắn tin tới Fanpage/Zalo của Tenant, Module webhook vẫn sẽ nhận được request. Module webhook cần check cache (trạng thái Tenant) -> nếu `INACTIVE`, có thể trả lỗi về cho nền tảng thứ ba hoặc lưu tạm vào Dead Letter Queue, nhưng tuyệt đối không xử lý phân bổ xuống Agent để tránh lỗi hệ thống.

### 6. Acceptance criteria
- **Scenario 1: Super Admin vô hiệu hóa Tenant thành công**
  - **Given** Tenant X đang ở trạng thái `ACTIVE` và người dùng là Super Admin.
  - **When** gửi request cập nhật trạng thái Tenant X thành `INACTIVE` với lý do "Hết hạn sử dụng".
  - **Then** hệ thống trả về mã HTTP `200 OK`.
  - **And** trạng thái trong Database chuyển thành `INACTIVE`.
  - **And** sự kiện `tenant.status_changed` được publish.
  - **And** mọi người dùng của Tenant X sẽ bị đăng xuất hoặc nhận lỗi `403/423` khi thao tác API tiếp theo.
- **Scenario 2: Tenant Admin cố gắng đổi trạng thái**
  - **Given** người dùng là Tenant Admin của Tenant X.
  - **When** gửi request đổi trạng thái Tenant của chính mình thành `INACTIVE`.
  - **Then** hệ thống trả về mã HTTP `403 Forbidden` (hoặc `401 Unauthorized`).
- **Scenario 3: Kích hoạt lại Tenant (Unban/Reactivate)**
  - **Given** Tenant X đang bị khóa (`INACTIVE`).
  - **When** Super Admin gửi request đổi trạng thái về `ACTIVE`.
  - **Then** hệ thống cập nhật thành công, các user của Tenant X có thể đăng nhập và sử dụng hệ thống bình thường.

### 7. Chỉ số phi chức năng
- **Tính nhất quán:** Việc khóa Tenant phải có hiệu lực thực tế trên toàn hệ thống trong vòng tối đa 3-5 giây để ngăn chặn kịp thời việc sử dụng trái phép (Strong/Fast Eventual Consistency thông qua Redis cache invalidation).
- **Audit Logging:** Hành động khóa/mở khóa là hành động nhạy cảm cấp hệ thống. Phải lưu chi tiết IP, tài khoản Super Admin thực hiện, Tenant bị tác động và lý do vào System Audit Log (tách biệt với Audit Log của riêng Tenant).

## MOD-TENANT-04 - Tạo mới Team

### 1. Mô tả nghiệp vụ đầy đủ
Luồng xử lý chính khi tạo một Team (Nhóm/Phòng ban) mới trong một Tenant:
1. **Truy cập quản lý nhóm:** Người dùng (Tenant Admin) đăng nhập vào hệ thống, điều hướng tới phần Quản lý tổ chức (Organization) -> Quản lý Team.
2. **Khởi tạo form:** Nhấn nút "Tạo mới Team", hệ thống hiển thị form nhập thông tin nhóm.
3. **Điền thông tin:** Người dùng nhập các thông tin cần thiết như Tên Team (Bắt buộc), Mô tả nhóm (Tùy chọn).
4. **Xác thực và kiểm tra giới hạn:** Hệ thống xác thực quyền của người dùng (phải là Tenant Admin). Sau đó kiểm tra giới hạn số lượng Team tối đa cho phép dựa trên Gói cước (Subscription Plan) hiện tại của Tenant.
5. **Kiểm tra tính hợp lệ:** Kiểm tra tính duy nhất của tên Team trong phạm vi Tenant đó.
6. **Khởi tạo bản ghi:** Lưu thông tin Team mới vào bảng `team` trong cơ sở dữ liệu.
7. **Trả về kết quả:** Hệ thống hiển thị thông báo tạo thành công và tự động cập nhật lại danh sách Team trên giao diện.

### 2. Input
- **Dữ liệu đầu vào (REST API Request):**
  - `tenantId` (UUID, lấy từ token xác thực hoặc context hiện tại, Bắt buộc).
  - `teamName` (Tên nhóm, kiểu String, Bắt buộc).
  - `description` (Mô tả chức năng/nhiệm vụ của nhóm, kiểu String, Tùy chọn).
- **Nguồn kích hoạt:** Frontend Web App (Thao tác trực tiếp của Tenant Admin).

### 3. Output
- **Kết quả trả về (API Response):**
  - Trả về mã HTTP `201 Created`.
  - Body: Trả về đối tượng Team vừa tạo gồm `teamId` (UUID), `teamName`, `description`, `createdAt`.
- **Nơi lưu:** Database (Bảng `team` thuộc module M02, có khóa ngoại tham chiếu đến `tenantId`).
- **Nơi gửi tiếp:** Tạm thời không cần gửi event sang Message Broker, trừ khi hệ thống có module phân bổ hội thoại tự động (Routing) cần cache danh sách team ngay lập tức (nếu có, publish sự kiện `team.created`).

### 4. Business rule / Ràng buộc
- **Phân quyền (Authorization):** Chỉ có `Tenant Admin` mới được phép tạo Team. Nhân viên bình thường (Agent) không có quyền truy cập chức năng này.
- **Tính duy nhất:** `teamName` phải là duy nhất (Unique) trong phạm vi của một `tenantId`. Hệ thống kiểm tra trùng lặp không phân biệt chữ hoa chữ thường (Case-insensitive) (VD: "Ca Sáng" và "ca sáng" bị coi là trùng).
- **Giới hạn số lượng (Quota):** Số lượng Team tối đa được phép tạo phụ thuộc vào Gói cước (Subscription Plan) mà Tenant đang sử dụng (VD: Gói Basic tối đa 3 team, Gói Pro không giới hạn). 
  - **Quản lý Gói (Database Design):** Hệ thống sử dụng bảng `plans` để lưu thông tin các gói cước bao gồm: `id` (VARCHAR), `name` (Tên gói), `max_teams` (Số lượng team tối đa, nếu là -1 hoặc NULL tức là không giới hạn), `max_users` (Số lượng user tối đa), v.v.
  - Khi check Quota, hệ thống sẽ query bảng `plans` dựa trên `plan_id` của Tenant để lấy ra `max_teams` và so sánh.
- **Quy tắc tên:** `teamName` có độ dài từ 3 đến 50 ký tự, không chứa các ký tự đặc biệt nguy hiểm (chấp nhận chữ cái, số, khoảng trắng, gạch ngang, gạch dưới).

### 5. Edge case cần xử lý
- **Vượt quá giới hạn gói cước (Quota Exceeded):** Nếu Tenant đã đạt giới hạn số lượng Team của gói cước, API từ chối lưu và trả về lỗi `403 Forbidden` (hoặc `402 Payment Required`) kèm mã lỗi nghiệp vụ để Frontend hiển thị popup yêu cầu nâng cấp gói.
- **Trùng tên Team do Race Condition:** Hai Admin của cùng một Tenant mở form và tạo Team với cùng một tên "Team CSKH" gần như cùng lúc -> Sử dụng Unique Constraint `(tenantId, teamName)` tại Database để chặn request đến sau, catch lỗi `DuplicateKeyException` và trả về `409 Conflict`.

### 6. Acceptance criteria
- **Scenario 1: Tạo Team thành công**
  - **Given** người dùng là Tenant Admin và Tenant chưa đạt giới hạn số lượng Team.
  - **When** gửi request tạo Team với `teamName` hợp lệ (chưa tồn tại).
  - **Then** hệ thống trả về mã `201 Created` kèm `teamId`.
  - **And** bản ghi Team mới được lưu vào CSDL đúng với `tenantId` đang thao tác.
- **Scenario 2: Tạo Team trùng tên**
  - **Given** trong Tenant hiện tại đã tồn tại một team tên "Ca Sáng".
  - **When** người dùng cố gắng tạo một team mới với tên "ca sáng".
  - **Then** hệ thống trả về mã `409 Conflict` kèm thông báo lỗi "Tên nhóm đã tồn tại trong hệ thống".
- **Scenario 3: Vượt quá giới hạn số lượng Team**
  - **Given** gói cước của Tenant chỉ cho phép tối đa 3 team và Tenant đã có đủ 3 team.
  - **When** người dùng điền đủ thông tin hợp lệ và gửi request tạo Team thứ 4.
  - **Then** hệ thống chặn lại và trả về lỗi `403 Forbidden` kèm thông báo "Bạn đã đạt giới hạn số lượng nhóm của gói cước hiện tại. Vui lòng nâng cấp gói".

### 7. Chỉ số phi chức năng
- **Thời gian phản hồi (Latency):** API tạo Team phải được xử lý nhanh gọn < 300ms.
- **Audit Logging:** Hành động tạo Team thành công phải được ghi nhận vào Audit Log của Tenant (lưu vết người dùng tạo, thời gian, tên team mới) để phục vụ tra cứu sau này.

## MOD-TENANT-05 - Cập nhật thông tin Team

### 1. Mô tả nghiệp vụ đầy đủ
Luồng xử lý chính khi cập nhật thông tin của một Team (Nhóm/Phòng ban) đang hoạt động:
1. **Truy cập quản lý nhóm:** Người dùng (Tenant Admin) đăng nhập vào hệ thống, điều hướng tới phần Quản lý tổ chức -> Quản lý Team, chọn một Team cụ thể từ danh sách và nhấn "Chỉnh sửa".
2. **Hiển thị form:** Hệ thống lấy thông tin hiện tại của Team (tên, mô tả) và hiển thị lên form chỉnh sửa.
3. **Cập nhật dữ liệu:** Người dùng thay đổi tên Team hoặc mô tả và nhấn "Lưu".
4. **Xác thực và phân quyền:** Hệ thống kiểm tra quyền (phải là Tenant Admin) và kiểm tra tính hợp lệ của dữ liệu đầu vào.
5. **Kiểm tra trùng lặp:** Hệ thống kiểm tra xem tên Team mới có bị trùng với một Team khác đang có trong cùng Tenant hay không (loại trừ chính bản ghi của Team đang sửa).
6. **Lưu thay đổi:** Cập nhật dữ liệu mới vào cơ sở dữ liệu.
7. **Publish sự kiện (Tùy chọn):** Bắn sự kiện `team.updated` lên Message Broker để các service khác đồng bộ thông tin nếu có tích hợp cache/routing hội thoại dựa trên tên Team.

### 2. Input
- **Dữ liệu đầu vào (REST API Request):**
  - `tenantId` (UUID, lấy từ token xác thực hoặc context, Bắt buộc).
  - `teamId` (UUID, lấy từ URL param, Bắt buộc).
  - `teamName` (Tên nhóm mới, kiểu String, Bắt buộc).
  - `description` (Mô tả chức năng/nhiệm vụ, kiểu String, Tùy chọn).
- **Nguồn kích hoạt:** Frontend Web App (Thao tác trực tiếp của Tenant Admin).

### 3. Output
- **Kết quả trả về (API Response):**
  - Trả về mã HTTP `200 OK`.
  - Body: Trả về đối tượng Team đã được cập nhật (`teamId`, `teamName`, `description`, `updatedAt`).
- **Nơi lưu:** Database (Bảng `team` thuộc module M02).
- **Nơi gửi tiếp:** Publish Kafka/RabbitMQ Event `team.updated` chứa `{ tenantId, teamId, updatedFields, updatedAt }`.

### 4. Business rule / Ràng buộc
- **Phân quyền (Authorization):** Chỉ `Tenant Admin` mới có quyền chỉnh sửa thông tin Team.
- **Tính duy nhất:** `teamName` sửa lại phải duy nhất trong phạm vi của một `tenantId`. Loại trừ chính `teamId` đang được cập nhật khi kiểm tra Unique dưới CSDL (Không phân biệt hoa thường - Case insensitive).
- **Quy tắc tên:** `teamName` không được để trống, độ dài từ 3-50 ký tự, không chứa ký tự đặc biệt nguy hiểm.
- **Không thay đổi ID:** `teamId` là định danh gốc, tuyệt đối không được thay đổi qua thao tác cập nhật này.

### 5. Edge case cần xử lý
- **Trùng tên Team:** Nếu Admin đổi tên Team thành "Ca Sáng" nhưng Tenant đã có một team khác mang tên "Ca Sáng", hệ thống phải bắt lỗi Unique Constraint và trả về lỗi `409 Conflict`.
- **Team không tồn tại hoặc đã bị xóa:** Nếu Admin đang mở form sửa Team, nhưng một Admin khác đã xóa Team đó trước đó -> Khi nhấn Lưu, DB không tìm thấy bản ghi. Trả về `404 Not Found` kèm thông báo "Nhóm này không tồn tại hoặc đã bị xóa".
- **Cập nhật đồng thời (Concurrency Update):** Hai Admin cùng mở form sửa một Team. Áp dụng Optimistic Locking (dựa vào trường `version` hoặc `updatedAt`), người lưu sau sẽ nhận lỗi `409 Conflict` nếu version không khớp, yêu cầu tải lại dữ liệu mới nhất.

### 6. Acceptance criteria
- **Scenario 1: Cập nhật thành công tên và mô tả**
  - **Given** người dùng là Tenant Admin và đang ở trang chỉnh sửa Team "CSKH".
  - **When** đổi tên thành "CSKH Chuyên Sâu", nhập mô tả mới và nhấn Lưu.
  - **Then** hệ thống trả về HTTP `200 OK`.
  - **And** thông tin trong DB được cập nhật đúng với dữ liệu mới.
- **Scenario 2: Đổi tên trùng với một nhóm khác**
  - **Given** trong hệ thống đã có Team "Telesale" và Team "CSKH".
  - **When** người dùng sửa Team "CSKH" và đổi tên thành "Telesale".
  - **Then** hệ thống trả về mã `409 Conflict` kèm thông báo lỗi "Tên nhóm đã tồn tại, vui lòng chọn tên khác".
- **Scenario 3: Sửa Team đã bị xóa bởi người khác**
  - **Given** Admin A đang mở form sửa thông tin của Team X.
  - **When** Admin B xóa Team X thành công. Sau đó Admin A nhấn Lưu thông tin thay đổi.
  - **Then** hệ thống trả về lỗi HTTP `404 Not Found` với thông báo "Nhóm không tồn tại hoặc đã bị xóa".

### 7. Chỉ số phi chức năng
- **Thời gian phản hồi:** API xử lý nhanh gọn < 300ms.
- **Audit Logging:** Ghi log thay đổi thông tin (lưu vết ai đổi, đổi từ tên gì sang tên gì, thời gian) vào Audit Log của Tenant để phục vụ truy vết khi có khiếu nại.

## MOD-TENANT-06 - Xóa/Vô hiệu hóa Team

### 1. Mô tả nghiệp vụ đầy đủ
Luồng xử lý chính khi thực hiện xóa (hoặc vô hiệu hóa) một Team:
1. **Truy cập quản lý nhóm:** Người dùng (Tenant Admin) đăng nhập vào hệ thống, điều hướng tới phần Quản lý tổ chức -> Quản lý Team, chọn một Team cụ thể từ danh sách và nhấn nút "Xóa".
2. **Hiển thị cảnh báo:** Hệ thống hiển thị hộp thoại xác nhận (Confirmation Dialog) cảnh báo về việc xóa nhóm sẽ tự động gỡ liên kết của tất cả các thành viên đang thuộc nhóm đó, và có thể ảnh hưởng đến các quy tắc phân bổ hội thoại (Routing rules) đang trỏ về nhóm.
3. **Xác nhận xóa:** Người dùng đồng ý và xác nhận xóa.
4. **Kiểm tra hợp lệ:** Hệ thống kiểm tra quyền (phải là Tenant Admin) và kiểm tra xem Team đó có phải là "Team mặc định" (Default Team) không (nếu hệ thống có quy định này, không cho phép xóa).
5. **Thực thi xóa (Database Transaction):**
   - **Xử lý liên kết:** Hệ thống gỡ bỏ liên kết giữa các User (Agent/Member) và Team đang bị xóa bằng cách xóa các bản ghi trong bảng trung gian `user_team`.
   - **Xóa Team:** Cập nhật trạng thái Team thành `DELETED` (Soft delete) hoặc xóa vật lý bản ghi khỏi CSDL (Hard delete, tùy thuộc vào chính sách lưu trữ). Thường ưu tiên Soft Delete để giữ lại lịch sử báo cáo (Report) cũ của Team.
6. **Publish sự kiện:** Gửi sự kiện `team.deleted` lên Message Broker để các module khác (như Routing, Chat) gỡ bỏ các cấu hình hoặc nhãn liên quan đến Team này.

### 2. Input
- **Dữ liệu đầu vào (REST API Request):**
  - `tenantId` (UUID, lấy từ token xác thực hoặc context, Bắt buộc).
  - `teamId` (UUID, lấy từ URL param, Bắt buộc).
- **Nguồn kích hoạt:** Frontend Web App (Thao tác trực tiếp của Tenant Admin).

### 3. Output
- **Kết quả trả về (API Response):**
  - Trả về mã HTTP `204 No Content` (nếu xóa thành công và không cần trả data) hoặc `200 OK` (kèm trạng thái đã xóa).
- **Nơi lưu:** Database (Bảng `team` và bảng `user_team` thuộc module M02).
- **Nơi gửi tiếp:** Publish Kafka/RabbitMQ Event `team.deleted` chứa `{ tenantId, teamId, deletedAt }`.

### 4. Business rule / Ràng buộc
- **Phân quyền (Authorization):** Chỉ `Tenant Admin` mới có quyền xóa Team.
- **Team mặc định (Default Team):** Nếu business rule quy định "Mỗi Tenant phải có ít nhất 1 Team mặc định", hệ thống không cho phép xóa Team cuối cùng hoặc Team được đánh dấu là Default.
- **Xử lý thành viên (Members):** Khi Team bị xóa, các nhân viên (Agent) thuộc Team này sẽ tự động bị gỡ khỏi Team. Tuy nhiên, tài khoản của họ trong hệ thống (thuộc Tenant) vẫn tồn tại và hoạt động bình thường, chỉ là họ không còn thuộc nhóm đó nữa.
- **Hội thoại đang xử lý:** Nếu có các đoạn chat/ticket đang được assign cho Team này (nhưng chưa nhận bởi cụ thể Agent nào), hệ thống có thể cần bắn event để Module Chat chuyển các hội thoại đó về trạng thái "Unassigned" hoặc gán lại vào luồng phân bổ mặc định.

### 5. Edge case cần xử lý
- **Team không tồn tại:** Nếu Admin bấm xóa một Team đã bị Admin khác xóa trước đó vài giây -> DB không tìm thấy bản ghi, hệ thống trả về lỗi `404 Not Found`.
- **Lỗi khi gỡ liên kết thành viên (Partial Failure):** Nếu thao tác xóa bản ghi Team thành công nhưng thao tác xóa bản ghi trong bảng trung gian `user_team` bị lỗi (do kết nối chập chờn) -> Bắt buộc phải áp dụng Transaction của Database để đảm bảo tính nguyên tử (Atomicity). Hoặc rollback tất cả, hoặc commit thành công tất cả.

### 6. Acceptance criteria
- **Scenario 1: Xóa Team thành công**
  - **Given** người dùng là Tenant Admin và đang chọn xóa Team "CSKH Ca Đêm" (không phải Team mặc định).
  - **When** xác nhận xóa Team.
  - **Then** hệ thống trả về mã `204 No Content` (hoặc `200 OK`).
  - **And** Team đó không còn xuất hiện trên giao diện danh sách.
  - **And** tất cả nhân viên thuộc Team "CSKH Ca Đêm" bị gỡ liên kết khỏi nhóm này (bản ghi trong `user_team` bị xóa).
  - **And** sự kiện `team.deleted` được bắn lên Message Broker.
- **Scenario 2: Xóa Team mặc định**
  - **Given** Team "Chung" là Team mặc định của Tenant (không thể xóa).
  - **When** người dùng cố gắng gọi API xóa Team "Chung".
  - **Then** hệ thống chặn lại và trả về lỗi `400 Bad Request` với thông báo "Không thể xóa nhóm mặc định của cửa hàng".
- **Scenario 3: Xóa Team đã bị xóa trước đó**
  - **Given** Team X đã bị Admin A xóa thành công.
  - **When** Admin B (đang mở tab cũ chưa reload) bấm xóa Team X.
  - **Then** hệ thống trả về mã `404 Not Found` kèm thông báo "Nhóm không tồn tại hoặc đã bị xóa".

### 7. Chỉ số phi chức năng
- **Thời gian phản hồi:** API xử lý xóa và gỡ liên kết phải hoàn thành < 500ms (cần chú ý tối ưu hiệu suất transaction nếu Team có hàng ngàn member).
- **Audit Logging:** Ghi log thao tác xóa Team (người thực hiện, thời gian, ID và Tên Team bị xóa) vào Audit Log của Tenant để phục hồi hoặc tra cứu khi cần.

## MOD-TENANT-07 - Thêm/Mời thành viên vào Tenant

### 1. Mô tả nghiệp vụ đầy đủ
Luồng xử lý chính khi Tenant Admin muốn thêm một nhân viên mới vào không gian làm việc (Tenant) của mình:
1. **Truy cập quản lý nhân viên:** Tenant Admin đăng nhập vào hệ thống, điều hướng tới phần Quản lý tổ chức -> Quản lý Nhân viên (Members), nhấn nút "Mời thành viên".
2. **Nhập thông tin:** Hệ thống hiển thị form yêu cầu nhập Email của người được mời và chọn Vai trò (Role - VD: Agent, Manager, Admin).
3. **Kiểm tra giới hạn (Quota):** Hệ thống kiểm tra số lượng thành viên tối đa cho phép của gói cước (Subscription Plan) hiện tại. Nếu vượt quá, API sẽ chặn thao tác.
4. **Kiểm tra tài khoản Identity (M01):**
   - Hệ thống M02 gọi API sang Module Identity (M01) để kiểm tra xem Email này đã đăng ký tài khoản trên hệ thống chung chưa.
   - Nếu chưa có: M01 hỗ trợ tạo một tài khoản tạm thời (Pending/Invited state) không có mật khẩu.
   - Nếu đã có: Chuyển sang bước tiếp theo.
5. **Tạo bản ghi liên kết:** Tạo một bản ghi trong bảng `tenant_member` ở M02 liên kết giữa `tenantId` và `userId` (hoặc email) với trạng thái `PENDING` (chờ xác nhận).
6. **Gửi email mời:** Hệ thống thông qua Module Notification/Email Service gửi một email chứa đường link lời mời (kèm token xác thực một lần) tới email của người được mời.
7. **Người dùng xác nhận:** Người được mời bấm vào link trong email.
   - Nếu là người dùng mới (chưa có pass), hệ thống yêu cầu thiết lập mật khẩu.
   - Khi hoàn tất, trạng thái trong `tenant_member` chuyển thành `ACTIVE`, người dùng chính thức có quyền truy cập vào Tenant với Role đã được cấp.

### 2. Input
- **Dữ liệu đầu vào (REST API Request):**
  - `tenantId` (UUID, lấy từ token xác thực, Bắt buộc).
  - `email` (Email của người được mời, kiểu String đúng định dạng, Bắt buộc).
  - `roleId` (ID của vai trò được phân quyền, kiểu UUID hoặc String, Bắt buộc).
- **Nguồn kích hoạt:** Frontend Web App (Thao tác của Tenant Admin).

### 3. Output
- **Kết quả trả về (API Response):**
  - Trả về mã HTTP `200 OK` (hoặc `201 Created`).
  - Body: `{ message: "Lời mời đã được gửi thành công", status: "PENDING" }`.
- **Nơi lưu:** Database (Bảng `tenant_member` thuộc module M02, và có thể là bảng `user` bên M01 nếu là email mới).
- **Nơi gửi tiếp:** Đẩy message vào Queue (RabbitMQ/Kafka) để worker phía sau lấy ra gọi API gửi Email, tránh block request.

### 4. Business rule / Ràng buộc
- **Phân quyền (Authorization):** Chỉ `Tenant Admin` hoặc người có quyền tương đương (Owner) mới được phép mời thành viên.
- **Giới hạn số lượng (Quota):** Tổng số lượng thành viên (tính cả `ACTIVE` và `PENDING`) không được vượt quá giới hạn của gói cước hiện tại (VD: Gói Basic tối đa 5 user).
- **Thời hạn lời mời:** Token trong đường link mời qua email có thời hạn (VD: 48 giờ). Hết thời hạn, link sẽ vô hiệu. Admin phải vào giao diện nhấn "Gửi lại lời mời" (Resend Invitation).
- **Tính duy nhất:** Một Email không thể được mời 2 lần vào cùng 1 Tenant nếu trạng thái của người đó đang là `ACTIVE` hoặc đang là `PENDING` (lời mời trước chưa hết hạn).

### 5. Edge case cần xử lý
- **Lỗi gọi sang Module Identity (M01):** Mạng chập chờn khi M02 gọi API sang M01 để check/create user -> Sử dụng Retry pattern (tối đa 3 lần). Nếu vẫn lỗi, trả về HTTP `500` hoặc `503 Service Unavailable` và yêu cầu Admin thử lại.
- **Dịch vụ Email bị lỗi:** Lưu dữ liệu lời mời thành công nhưng đẩy queue hoặc gọi API gửi email thất bại -> Vẫn giữ trạng thái `PENDING` trong DB, nhưng hiển thị cảnh báo/thông báo nhỏ cho Admin trên giao diện để họ có thể bấm "Gửi lại" sau.
- **Mời chính mình:** Tenant Admin nhập email của chính mình -> API bắt lỗi và trả về `400 Bad Request` "Bạn không thể tự mời chính mình vào không gian làm việc này".

### 6. Acceptance criteria
- **Scenario 1: Mời thành viên thành công (Email mới)**
  - **Given** người dùng là Tenant Admin và Tenant còn trống Quota giới hạn.
  - **When** gửi request mời email "new-agent@example.com" với Role "Agent".
  - **Then** hệ thống trả về mã `200 OK`.
  - **And** bản ghi thành viên được tạo trong DB với trạng thái `PENDING`.
  - **And** một email chứa link mời được gửi tới "new-agent@example.com".
- **Scenario 2: Vượt quá giới hạn thành viên**
  - **Given** Tenant đang sử dụng gói Basic giới hạn 5 thành viên và hiện đã có 5 thành viên (gồm cả Active và Pending).
  - **When** Tenant Admin cố gắng gửi lời mời thứ 6.
  - **Then** hệ thống chặn lại và trả về lỗi `403 Forbidden` kèm thông báo "Bạn đã đạt giới hạn thành viên của gói cước, vui lòng nâng cấp".
- **Scenario 3: Mời một người đã có trong Tenant**
  - **Given** Email "agent@example.com" đã là thành viên `ACTIVE` của Tenant.
  - **When** Tenant Admin điền email này vào form Mời thành viên và gửi.
  - **Then** hệ thống trả về lỗi `409 Conflict` kèm thông báo "Người dùng này đã là thành viên của cửa hàng".
- **Scenario 4: Người dùng bấm vào link mời hết hạn**
  - **Given** lời mời đã được gửi cách đây 3 ngày (token hết hạn).
  - **When** người dùng bấm vào link trong email.
  - **Then** hệ thống hiển thị trang lỗi "Đường dẫn lời mời đã hết hạn hoặc không hợp lệ" và yêu cầu liên hệ lại Admin.

### 7. Chỉ số phi chức năng
- **Thời gian phản hồi:** API xử lý việc tạo lời mời và giao tiếp với M01 phải hoàn thành < 1000ms. Việc gửi Email thực tế bắt buộc chạy Background Job / Asynchronous Queue để đảm bảo trải nghiệm Frontend không bị đơ.
- **Audit Logging:** Ghi log thao tác (ai mời, mời email nào, cấp role gì, kết quả, thời gian) vào Audit Log của Tenant.

## MOD-TENANT-08 - Gán thành viên vào Team

### 1. Mô tả nghiệp vụ đầy đủ
Luồng xử lý chính khi Tenant Admin muốn gán (assign) một hoặc nhiều thành viên vào một Team cụ thể:
1. **Truy cập giao diện gán:**
   - Cách 1: Từ màn hình "Quản lý Team", chọn một Team và nhấn "Thêm thành viên".
   - Cách 2: Từ màn hình "Quản lý Nhân viên", tick chọn một/nhiều nhân viên và nhấn hành động "Thêm vào nhóm".
2. **Chọn thông tin:** Người dùng cung cấp danh sách các `userId` (nhân viên) và danh sách các `teamId` (nhóm) cần gán liên kết.
3. **Kiểm tra hợp lệ:** Hệ thống kiểm tra quyền (phải là Tenant Admin) và validate:
   - Các `userId` phải tồn tại, đang thuộc Tenant và có trạng thái `ACTIVE`.
   - `teamId` phải tồn tại trong Tenant và không bị xóa (`DELETED`).
4. **Cập nhật liên kết:** Hệ thống lưu các bản ghi liên kết mới vào bảng trung gian `user_team` (cặp `userId` - `teamId`). Nếu bản ghi nào đã tồn tại, hệ thống sẽ tự động bỏ qua (Cơ chế Upsert/Ignore).
5. **Publish sự kiện:** Gửi sự kiện `team.members_assigned` lên Message Broker để các module khác (như Module Chat, Module Routing) có thể cập nhật cache danh sách nhân sự của Team, phục vụ cho luồng phân bổ hội thoại tự động.

### 2. Input
- **Dữ liệu đầu vào (REST API Request):**
  - `tenantId` (UUID, lấy từ token xác thực, Bắt buộc).
  - `teamId` (UUID, ID của nhóm cần gán, Bắt buộc).
  - `userIds` (Mảng UUID, danh sách các user cần gán vào nhóm, Bắt buộc, tối thiểu 1 phần tử).
- **Nguồn kích hoạt:** Frontend Web App (Thao tác của Tenant Admin).

### 3. Output
- **Kết quả trả về (API Response):**
  - Trả về mã HTTP `200 OK` (hoặc `201 Created`).
  - Body: `{ message: "Gán thành viên thành công", addedCount: 2, ignoredCount: 0 }`.
- **Nơi lưu:** Database (Bảng trung gian `user_team` hoặc tương đương thuộc module M02).
- **Nơi gửi tiếp:** Publish Kafka/RabbitMQ Event `team.members_assigned` chứa `{ tenantId, teamId, userIds, assignedAt }`.

### 4. Business rule / Ràng buộc
- **Phân quyền (Authorization):** Chỉ `Tenant Admin` mới được phép gán người dùng vào Team.
- **Trạng thái User hợp lệ:** Chỉ được phép gán các User có trạng thái `ACTIVE` trong Tenant. User đang `PENDING` (chờ xác nhận email) hoặc `INACTIVE` (bị khóa) không thể được gán vào Team.
- **Giới hạn số lượng gán (Batch limit):** Để tránh quá tải API và Database, mỗi request chỉ cho phép gán tối đa 50 user vào một Team. Nếu muốn gán nhiều hơn, Frontend cần chia nhỏ (pagination/batching request).
- **Cho phép đa nhóm:** Một User có thể thuộc nhiều Team khác nhau trong cùng một Tenant (VD: một người vừa ở nhóm "CSKH", vừa ở nhóm "Telesale").

### 5. Edge case cần xử lý
- **Thành viên đã có trong nhóm (Idempotency):** Nếu trong danh sách `userIds` gửi lên có chứa một người đã nằm trong nhóm đó rồi, API không được trả về lỗi HTTP (không quăng `500` hay `409`) mà chỉ cần bỏ qua người đó, và tiếp tục xử lý những người còn lại. Số lượng bị bỏ qua được trả về trong trường `ignoredCount`.
- **User không thuộc Tenant (Bảo mật):** Nếu Payload cố tình truyền một `userId` hợp lệ của hệ thống nhưng user đó không thuộc `tenantId` hiện tại -> Trả về lỗi `400 Bad Request` "Một số thành viên không thuộc cửa hàng này" (Ngăn chặn lỗ hổng IDOR).
- **Team không tồn tại:** Trả về lỗi `404 Not Found` nếu `teamId` không hợp lệ hoặc đã bị xóa.

### 6. Acceptance criteria
- **Scenario 1: Gán thành viên thành công**
  - **Given** người dùng là Tenant Admin, chọn Team "Ca Sáng" và 2 User hợp lệ chưa có trong nhóm.
  - **When** gửi request gán.
  - **Then** hệ thống trả về mã `200 OK`.
  - **And** 2 bản ghi mới được tạo trong bảng `user_team`.
  - **And** event `team.members_assigned` được bắn lên broker.
- **Scenario 2: Gán thành viên đã tồn tại trong nhóm**
  - **Given** User X đã nằm trong Team "Ca Sáng".
  - **When** Tenant Admin tiếp tục gửi request gán User X vào Team "Ca Sáng" cùng với User Y (chưa có trong nhóm).
  - **Then** hệ thống trả về mã `200 OK` (Body: `addedCount`: 1, `ignoredCount`: 1).
  - **And** chỉ có User Y được thêm vào nhóm, hệ thống không bị lỗi crash do insert trùng dữ liệu.
- **Scenario 3: Gán User trạng thái PENDING**
  - **Given** User Z vừa được mời nhưng chưa xác nhận email (trạng thái `PENDING`).
  - **When** Admin chọn User Z và gán vào Team.
  - **Then** hệ thống trả về lỗi `400 Bad Request` kèm thông báo "Chỉ có thể gán các thành viên đã kích hoạt tài khoản".
- **Scenario 4: Request chứa quá số lượng cho phép**
  - **Given** Tenant Admin chọn 60 user.
  - **When** gửi request gán vào nhóm.
  - **Then** hệ thống chặn lại, trả về mã `413 Payload Too Large` hoặc `400 Bad Request` kèm thông báo "Chỉ được gán tối đa 50 thành viên trong một thao tác".

### 7. Chỉ số phi chức năng
- **Thời gian phản hồi:** API xử lý (kể cả khi gán max 50 user) phải hoàn thành < 500ms. Khuyến nghị sử dụng Batch Insert (như `INSERT ... VALUES (), ()...`) ở tầng Repository để tối ưu I/O Database thay vì insert từng dòng.
- **Audit Logging:** Ghi log thao tác (lưu vết ai thực hiện, gán những userId nào, vào teamId nào, thời gian) vào Audit Log của Tenant.

## MOD-TENANT-09 - Hủy tư cách thành viên (Remove Member)

### 1. Mô tả nghiệp vụ đầy đủ
Luồng xử lý chính khi Tenant Admin muốn xóa (remove) hoàn toàn một nhân viên khỏi Tenant (không gian làm việc):
1. **Truy cập quản lý nhân viên:** Người dùng (Tenant Admin) đăng nhập, điều hướng tới phần Quản lý tổ chức -> Quản lý Nhân viên.
2. **Chọn nhân viên:** Chọn một nhân viên cụ thể từ danh sách và nhấn nút "Xóa khỏi cửa hàng" (Remove from workspace).
3. **Hiển thị cảnh báo:** Hệ thống hiển thị hộp thoại xác nhận cảnh báo rằng thao tác này sẽ ngay lập tức thu hồi quyền truy cập của nhân viên vào dữ liệu Tenant, đồng thời gỡ nhân viên đó khỏi tất cả các Team hiện tại.
4. **Xác nhận xóa:** Người dùng đồng ý và xác nhận xóa.
5. **Kiểm tra hợp lệ:** Hệ thống kiểm tra quyền của người thực hiện, và đảm bảo nhân viên bị xóa không phải là Owner duy nhất của Tenant.
6. **Thực thi xóa (Database Transaction):**
   - Xóa (hoặc Soft Delete) bản ghi liên kết trong bảng `tenant_member`.
   - Tự động gỡ nhân viên đó khỏi tất cả các Team đang tham gia (xóa các bản ghi liên quan của nhân viên này trong bảng `user_team`).
7. **Vô hiệu hóa phiên đăng nhập:** Gọi API/Gửi event sang Module Identity (M01) để thu hồi token/session của nhân viên này đối với Tenant hiện tại.
8. **Publish sự kiện:** Gửi sự kiện `tenant.member_removed` lên Message Broker để các module khác (VD: Module Routing, Customer Management) cập nhật lại danh sách nhân sự để ngừng phân bổ hội thoại mới.

### 2. Input
- **Dữ liệu đầu vào (REST API Request):**
  - `tenantId` (UUID, lấy từ token xác thực, Bắt buộc).
  - `userId` (UUID, ID của nhân viên cần xóa, Bắt buộc).
- **Nguồn kích hoạt:** Frontend Web App (Thao tác trực tiếp của Tenant Admin).

### 3. Output
- **Kết quả trả về (API Response):**
  - Trả về mã HTTP `204 No Content` (nếu xóa thành công).
- **Nơi lưu:** Database (Bảng `tenant_member` và `user_team` thuộc module M02).
- **Nơi gửi tiếp:** 
  - Gọi API sang Module M01 để xử lý kill session.
  - Publish Kafka/RabbitMQ Event `tenant.member_removed` chứa `{ tenantId, userId, removedAt }`.

### 4. Business rule / Ràng buộc
- **Phân quyền (Authorization):** Chỉ `Tenant Admin` hoặc `Tenant Owner` mới được phép xóa thành viên. `Tenant Admin` không được phép xóa `Tenant Owner`.
- **Owner duy nhất:** Không được phép xóa `Tenant Owner` nếu đó là Owner duy nhất của Tenant. (Mỗi Tenant luôn phải có ít nhất 1 Owner).
- **Hội thoại chưa hoàn tất:** Nếu nhân viên bị xóa đang có các đoạn chat hoặc ticket đang xử lý dở dang (Assigned), hệ thống có policy tự động chuyển các hội thoại đó về trạng thái `Unassigned` hoặc trả về hàng đợi của Team để người khác xử lý (thông qua việc module Chat consume sự kiện `tenant.member_removed`).
- **Trạng thái tài khoản Identity:** Việc xóa nhân viên khỏi Tenant không đồng nghĩa với việc xóa hoàn toàn tài khoản của họ khỏi hệ thống chung (Module M01). Họ vẫn có thể đăng nhập vào hệ thống và truy cập các Tenant khác mà họ đang tham gia.

### 5. Edge case cần xử lý
- **Xóa chính mình:** Nếu Tenant Admin cố tình gọi API truyền `userId` là chính mình -> Hệ thống chặn và trả lỗi `400 Bad Request` "Bạn không thể tự xóa chính mình. Vui lòng sử dụng tính năng Rời khỏi cửa hàng (Leave Workspace)".
- **Lỗi gọi sang Identity Module (M01):** Nếu M01 đang sập và không kill được session qua API nội bộ, hệ thống vẫn ưu tiên thực thi xóa quyền trong Database (M02) thành công. Frontend của user bị xóa khi gọi API lấy data sẽ tự động bị chặn ở tầng Authorization (Middleware) do không còn Role trong Tenant, đảm bảo tính bảo mật (Fallback security).

### 6. Acceptance criteria
- **Scenario 1: Xóa thành viên thành công**
  - **Given** người dùng là Tenant Admin và muốn xóa User X (Role: Agent).
  - **When** gửi request xóa User X.
  - **Then** hệ thống trả về mã `204 No Content`.
  - **And** bản ghi của User X trong bảng `tenant_member` và các bản ghi trong `user_team` bị xóa.
  - **And** sự kiện `tenant.member_removed` được bắn lên broker.
  - **And** nếu User X đang online, mọi API request tiếp theo của họ vào Tenant này sẽ bị trả về `403 Forbidden`.
- **Scenario 2: Xóa Owner duy nhất**
  - **Given** Tenant chỉ có 1 Owner là User Y.
  - **When** User Y gửi request tự xóa mình (hoặc một Admin khác cố xóa User Y).
  - **Then** hệ thống chặn lại và trả về lỗi `400 Bad Request` kèm thông báo "Không thể xóa chủ sở hữu duy nhất của cửa hàng".
- **Scenario 3: Admin xóa một Admin khác**
  - **Given** User A và User B đều là Tenant Admin. User A muốn xóa User B.
  - **When** User A gửi request xóa User B.
  - **Then** thao tác thành công, trả về mã `204 No Content`.

### 7. Chỉ số phi chức năng
- **Thời gian phản hồi:** API xử lý xóa và giao tiếp nội bộ phải hoàn thành < 800ms. Bắt buộc sử dụng Transaction để xóa liên kết bảng `tenant_member` và `user_team` một cách nguyên tử.
- **Audit Logging:** Hành động xóa thành viên phải được lưu vết (người thực hiện, người bị xóa, thời gian) vào Audit Log của Tenant phục vụ tra soát bảo mật.

## MOD-TENANT-10 - Cấu hình giờ làm việc (Business Hours)

### 1. Mô tả nghiệp vụ đầy đủ
Luồng xử lý chính khi Tenant Admin muốn thiết lập thời gian làm việc tiêu chuẩn của không gian làm việc:
1. **Truy cập cấu hình:** Người dùng (Tenant Admin) truy cập vào Cài đặt tổ chức -> Giờ làm việc.
2. **Lấy cấu hình hiện tại:** Hệ thống gọi API để lấy thiết lập hiện hành (hoặc thiết lập mặc định 24/7 nếu chưa từng cấu hình).
3. **Chỉnh sửa:** Người dùng chọn múi giờ (Timezone) và tùy chỉnh các ngày làm việc trong tuần (từ Thứ Hai đến Chủ Nhật). Với mỗi ngày, có thể chọn là "Ngày nghỉ" hoặc cấu hình nhiều "Ca làm việc" (Ví dụ: 08:00 - 12:00 và 13:30 - 17:30).
4. **Lưu cấu hình:** Người dùng ấn "Lưu".
5. **Kiểm tra hợp lệ:** Hệ thống kiểm tra múi giờ có hợp lệ không, các ca làm việc trong cùng một ngày có bị giao nhau (overlap) hoặc giờ kết thúc trước giờ bắt đầu hay không.
6. **Lưu trữ:** Lưu xuống cơ sở dữ liệu `tenant-service` (cập nhật hoặc tạo mới bản ghi `business_hours` của Tenant).
7. **Publish sự kiện:** Gửi sự kiện `tenant.business_hours_updated` qua Message Broker để các module khác (VD: `livestream-service`, `conversation-service`) lấy cấu hình mới nhằm kích hoạt tin nhắn OOO (Out of Office) hoặc tạm dừng đồng hồ SLA.

### 2. Input
- **Dữ liệu đầu vào (REST API Request):**
  - `tenantId` (UUID, lấy từ token xác thực, Bắt buộc).
  - Payload JSON bao gồm:
    - `timezone`: Chuỗi (VD: "Asia/Ho_Chi_Minh").
    - `schedule`: Danh sách 7 phần tử (đại diện cho 7 ngày trong tuần). Mỗi phần tử gồm:
      - `dayOfWeek`: Enum (MONDAY -> SUNDAY).
      - `isDayOff`: Boolean (True nếu là ngày nghỉ).
      - `shifts`: Danh sách các ca làm việc (chỉ bắt buộc nếu `isDayOff` = false), mỗi ca gồm `startTime` và `endTime` (định dạng `HH:mm`).
- **Nguồn kích hoạt:** Frontend Web App (Thao tác của Tenant Admin).

### 3. Output
- **Kết quả trả về (API Response):**
  - Trả về mã HTTP `200 OK` kèm theo JSON cấu hình giờ làm việc vừa được cập nhật.
- **Nơi lưu:** Database (`tenant-service`, bảng `business_hours`).
- **Nơi gửi tiếp:** Publish Kafka/RabbitMQ Event `tenant.business_hours_updated` chứa toàn bộ payload cấu hình giờ làm việc của Tenant.

### 4. Business rule / Ràng buộc
- **Phân quyền (Authorization):** Chỉ `Tenant Admin` hoặc `Tenant Owner` mới được phép cấu hình.
- **Timezone hợp lệ:** Tham số `timezone` phải thuộc chuẩn IANA Timezone (VD: `Asia/Ho_Chi_Minh`, `UTC`).
- **Định dạng giờ:** `startTime` và `endTime` phải tuân theo định dạng chuẩn ISO-8601 (24-hour) `HH:mm` (VD: `08:00`, `17:30`).
- **Logic thời gian:** `endTime` của một ca bắt buộc phải lớn hơn (sau) `startTime`.
- **Ràng buộc ca làm việc:** Nếu `isDayOff` là `false`, mảng `shifts` không được phép rỗng.
- **Không giao nhau (No Overlap):** Các khoảng thời gian (`shifts`) trong cùng một ngày không được phép chồng chéo lên nhau (VD: Ca 1 từ `08:00 - 12:00` thì ca 2 không thể là `11:00 - 15:00`).

### 5. Edge case cần xử lý
- **Lỗi giờ giao nhau (Overlap):** Nếu user cố tình submit các ca làm việc chồng chéo (qua API), hệ thống chặn lại ở mức Validate và trả về HTTP `400 Bad Request` kèm thông điệp báo lỗi rõ ngày và ca nào bị trùng.
- **Giờ kết thúc trước giờ bắt đầu:** Trả về `400 Bad Request`.
- **Cấu hình mặc định:** Nếu chưa từng cấu hình, API Get `business_hours` tự động trả về mặc định là múi giờ UTC, 7 ngày đều là ngày làm việc (không có day off), và 1 ca duy nhất `00:00 - 23:59` (hoặc `24/7`).
- **Nhiều request đồng thời (Race Condition):** Thiết kế API cập nhật theo hình thức Upsert (Update or Insert) bằng cơ chế khóa bản ghi (Optimistic Locking hoặc Unique Constraint `tenant_id`) để tránh tạo ra 2 bản ghi cấu hình cho cùng một Tenant.

### 6. Acceptance criteria
- **Scenario 1: Xem giờ làm việc mặc định**
  - **Given** Tenant chưa từng cài đặt giờ làm việc.
  - **When** Tenant Admin gọi API GET `/api/v1/tenants/{tenantId}/business-hours`.
  - **Then** Trả về HTTP `200 OK` cùng payload cấu hình mặc định (7 ngày làm việc, `00:00` - `23:59`).
- **Scenario 2: Cập nhật giờ làm việc thành công**
  - **Given** Tenant Admin nhập `timezone` là "Asia/Ho_Chi_Minh", Thứ 2 đến Thứ 6 làm việc `08:00-12:00` và `13:00-17:00`, Thứ 7 - CN là ngày nghỉ (`isDayOff = true`).
  - **When** gọi API PUT/PATCH.
  - **Then** Trả về HTTP `200 OK`. Dữ liệu được lưu chính xác trong database. Sự kiện `tenant.business_hours_updated` được phát hành.
- **Scenario 3: Validation lỗi ca làm việc**
  - **Given** Tenant Admin thiết lập Thứ Hai có ca 1 `08:00 - 12:00` và ca 2 `10:00 - 15:00`.
  - **When** gọi API PUT/PATCH cập nhật.
  - **Then** Trả về HTTP `400 Bad Request` với message "Ca làm việc bị trùng lặp trong ngày Thứ Hai". Dữ liệu cũ không thay đổi.

### 7. Chỉ số phi chức năng
- **Thời gian phản hồi:** API Get và Cập nhật xử lý < 200ms.
- **Khả năng chịu tải (Read-Heavy):** Endpoint lấy giờ làm việc không được thiết kế để gọi liên tục từ các module khác mỗi khi có tin nhắn mới. Dữ liệu này phải được Event-Driven sang các module khác (như Chat/Livestream) để lưu vào Redis Cache / In-memory, đảm bảo hệ thống không bị thắt cổ chai ở `tenant-service`.
