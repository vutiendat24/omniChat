# Yêu cầu chi tiết: M01 - Identity & Access

## MOD-IAM-01: Đăng ký tài khoản hệ thống (User Registration)

### 1. Mô tả nghiệp vụ đầy đủ
Luồng xử lý chính khi người dùng (thường là chủ shop tương lai) tự đăng ký tài khoản mới trên hệ thống OmniChat:
1. **Truy cập form đăng ký:** Người dùng truy cập trang Đăng ký (Sign Up) và nhập các thông tin cá nhân cơ bản (Email, Họ tên, Mật khẩu, Xác nhận mật khẩu).
2. **Xác thực dữ liệu đầu vào (Validation):** Hệ thống kiểm tra định dạng email, độ mạnh của mật khẩu và sự trùng khớp của mật khẩu xác nhận.
3. **Kiểm tra trùng lặp:** Hệ thống truy vấn CSDL để đảm bảo Email này chưa từng được đăng ký trong hệ thống (Unique Email).
4. **Mã hóa mật khẩu:** Mật khẩu gốc (plaintext) được mã hóa một chiều (Hashing) bằng thuật toán chuẩn bảo mật (VD: Bcrypt hoặc Argon2).
5. **Lưu trữ dữ liệu:** Tạo bản ghi người dùng mới trong database với trạng thái `PENDING_VERIFICATION` (Chờ xác thực).
6. **Khởi tạo Token xác thực:** Hệ thống tạo ra một chuỗi ngẫu nhiên (Verification Token) duy nhất, gắn với User ID vừa tạo, lưu vào Cache (Redis) hoặc Database với thời gian hết hạn (VD: 24 giờ).
7. **Gửi Email xác thực:** Hệ thống (thông qua Module Notification hoặc trực tiếp qua SMTP) gửi một email chứa đường link xác thực tài khoản tới email của người dùng.
8. **Xác thực Email (Verify):** Người dùng bấm vào link trong email. Hệ thống kiểm tra Token:
   - Nếu hợp lệ: Đổi trạng thái tài khoản thành `ACTIVE`, xóa Token khỏi Cache, thông báo đăng ký hoàn tất và chuyển hướng về trang Đăng nhập.
   - Nếu hết hạn/không hợp lệ: Thông báo lỗi và cho phép người dùng nhấn nút "Gửi lại link xác thực".

### 2. Input
- **Dữ liệu đầu vào (REST API Request):**
  - `email` (String, định dạng email chuẩn, Bắt buộc).
  - `fullName` (String, tên hiển thị, Bắt buộc).
  - `password` (String, mật khẩu, Bắt buộc).
- **Nguồn kích hoạt:** Từ Frontend Web App (Trang Landing Page / Auth Page).

### 3. Output
- **Kết quả trả về (API Response):**
  - Trả về mã HTTP `201 Created` kèm thông báo "Vui lòng kiểm tra email để kích hoạt tài khoản". (Lưu ý: Không trả về Access Token lúc này để ép người dùng phải xác thực email).
- **Nơi lưu:** Database (Bảng `users` thuộc module M01).
- **Nơi gửi tiếp:** Publish event `user.registered` (chứa `userId`, `email`, `verificationToken`) lên Message Broker (Kafka/RabbitMQ) để Module Notification tiếp nhận và gửi email.

### 4. Business rule / Ràng buộc
- **Tính duy nhất:** `email` phải là duy nhất trên toàn hệ thống (Global Unique).
- **Quy tắc Mật khẩu (Password Policy):** Mật khẩu tối thiểu 8 ký tự, bao gồm ít nhất 1 chữ hoa, 1 chữ thường, 1 chữ số và 1 ký tự đặc biệt (`!@#$%^&*`).
- **Rate Limit (Chống Spam):** Giới hạn hành vi đăng ký: Tối đa 5 lần đăng ký / 1 IP / 1 Giờ để chống tạo tài khoản hàng loạt (Bot attack).
- **Thời hạn Token:** Verification Token chỉ có hiệu lực trong vòng 24 giờ kể từ lúc tạo.

### 5. Edge case cần xử lý
- **Email đã tồn tại nhưng chưa xác thực (PENDING):** Nếu người dùng dùng lại email đã đăng ký nhưng chưa click link kích hoạt. Hệ thống KHÔNG báo lỗi "Email đã tồn tại", mà sẽ tự động tạo lại Verification Token mới và gửi lại email kích hoạt, thông báo "Chúng tôi đã gửi lại link xác thực vào email của bạn".
- **Gửi Email thất bại:** Việc lưu DB thành công nhưng push message vào Broker bị lỗi hoặc SMTP server down -> Áp dụng Outbox Pattern để lưu tạm event vào DB và có worker retry gửi sau. Giao diện vẫn báo đăng ký thành công và người dùng có thể dùng chức năng "Gửi lại email" sau đó.
- **Race Condition khi đăng ký:** Hai request gửi cùng lúc với cùng 1 email -> Dựa vào Unique Constraint ở Database để catch `DuplicateKeyException` ở request thứ hai và báo lỗi.

### 6. Acceptance Criteria
- **Scenario 1: Đăng ký thành công**
  - **Given** người dùng điền thông tin hợp lệ và email chưa tồn tại trong hệ thống.
  - **When** người dùng nhấn nút Đăng ký.
  - **Then** hệ thống lưu tài khoản với trạng thái `PENDING_VERIFICATION`.
  - **And** trả về thông báo yêu cầu kiểm tra email.
  - **And** một email chứa link kích hoạt được gửi đi.
- **Scenario 2: Đăng ký với Email đã kích hoạt**
  - **Given** email "seller@example.com" đã tồn tại và có trạng thái `ACTIVE`.
  - **When** người dùng cố gắng đăng ký tài khoản mới với email này.
  - **Then** hệ thống trả về mã `409 Conflict` kèm thông báo "Email này đã được sử dụng".
- **Scenario 3: Xác thực tài khoản thành công**
  - **Given** người dùng nhận được email xác thực chứa Token hợp lệ.
  - **When** người dùng click vào đường link xác thực.
  - **Then** hệ thống đổi trạng thái tài khoản thành `ACTIVE`.
  - **And** điều hướng người dùng tới trang Đăng nhập kèm thông báo "Xác thực thành công".

### 7. Chỉ số phi chức năng
- **Bảo mật:** Mật khẩu BẮT BUỘC phải được hash, tuyệt đối không lưu plaintext. Tránh tấn công Timing Attack khi so sánh/kiểm tra email.
- **Thời gian phản hồi (Latency):** API đăng ký phải phản hồi dưới 500ms (Việc gửi email phải được xử lý bất đồng bộ, không block luồng request của user).

## MOD-IAM-02: Đăng nhập hệ thống (Local Login)

### 1. Mô tả nghiệp vụ đầy đủ
Luồng xử lý chính khi người dùng đăng nhập vào hệ thống bằng tài khoản đã đăng ký (Email và Mật khẩu):
1. **Truy cập form đăng nhập:** Người dùng truy cập trang Đăng nhập (Login) và nhập Email cùng Mật khẩu.
2. **Xác thực dữ liệu đầu vào:** Hệ thống kiểm tra định dạng email và mật khẩu không được bỏ trống.
3. **Kiểm tra tài khoản tồn tại:** Truy vấn CSDL để tìm người dùng theo Email. Nếu không tồn tại, trả về lỗi chung (VD: "Email hoặc mật khẩu không chính xác" để tránh rò rỉ thông tin).
4. **Xác thực trạng thái tài khoản:** Nếu tài khoản đang ở trạng thái `PENDING_VERIFICATION`, yêu cầu xác thực email trước. Nếu tài khoản bị `SUSPENDED` hoặc `LOCKED`, từ chối đăng nhập.
5. **Kiểm tra Mật khẩu:** So sánh (Verify) mật khẩu người dùng nhập vào với mã băm (Hash) lưu trong CSDL. 
   - Nếu sai: tăng biến đếm số lần đăng nhập thất bại. Nếu sai quá số lần quy định, khóa tài khoản (Lock account).
   - Nếu đúng: reset biến đếm số lần đăng nhập thất bại về 0.
6. **Khởi tạo Token (JWT):** Tạo ra 2 token:
   - **Access Token:** Có thời hạn ngắn (VD: 15-30 phút), chứa thông tin cơ bản của user (`userId`, `role`, `tenantId`).
   - **Refresh Token:** Có thời hạn dài (VD: 7-30 ngày), dùng để cấp lại Access Token mới. Lưu mã băm của Refresh Token vào CSDL (hoặc Redis) để quản lý thiết bị/phiên đăng nhập.
7. **Trả về kết quả:** Hệ thống trả về Access Token và Refresh Token cho Frontend (có thể lưu ở HttpOnly Cookie hoặc Response Body tùy chiến lược bảo mật của Frontend).

### 2. Input
- **Dữ liệu đầu vào (REST API Request):**
  - `email` (String, định dạng email chuẩn, Bắt buộc).
  - `password` (String, mật khẩu, Bắt buộc).
- **Nguồn kích hoạt:** Từ Frontend Web App (Trang Login).

### 3. Output
- **Kết quả trả về (API Response):**
  - Trả về mã HTTP `200 OK`.
  - Body/Header chứa: `accessToken`, `refreshToken`, `expiresIn`.
- **Nơi lưu:** Redis hoặc CSDL (Lưu thông tin session đăng nhập / Refresh Token hash, cập nhật `last_login_at`, reset `failed_login_attempts`).
- **Nơi gửi tiếp:** Không có.

### 4. Business rule / Ràng buộc
- **Chống Brute-force (Rate Limit & Lock Account):** Nếu nhập sai mật khẩu quá 5 lần liên tiếp, tài khoản sẽ bị tạm khóa (LOCKED) trong vòng 15-30 phút.
- **Trạng thái tài khoản:** Chỉ tài khoản có trạng thái `ACTIVE` mới được phép đăng nhập. `PENDING_VERIFICATION` hoặc `SUSPENDED` sẽ bị từ chối.
- **Bảo mật phản hồi:** Luôn trả về thông báo lỗi chung chung (VD: "Tài khoản hoặc mật khẩu không chính xác") nhằm tránh lộ lọt thông tin tài khoản có tồn tại hay không.
- **Concurrent Sessions (Phiên đăng nhập đồng thời):** Cho phép người dùng đăng nhập trên nhiều thiết bị. Mỗi lần đăng nhập sinh ra một Refresh Token mới.

### 5. Edge case cần xử lý
- **Tài khoản đang bị tạm khóa (LOCKED):** Dù người dùng nhập đúng mật khẩu trong thời gian bị khóa, hệ thống vẫn từ chối và trả về lỗi "Tài khoản đang bị tạm khóa, vui lòng thử lại sau".
- **Khoảng trắng ở đầu/cuối:** Tự động trim khoảng trắng ở email trước khi truy vấn, nhưng đối với password thì không.
- **Trùng lặp yêu cầu (Double-click):** Nếu nhấn đăng nhập nhiều lần liên tục, hệ thống tạo nhiều phiên hoặc trả về kết quả giống nhau (idempotency không nghiêm ngặt nhưng không gây lỗi).

### 6. Acceptance Criteria
- **Scenario 1: Đăng nhập thành công**
  - **Given** người dùng có tài khoản hợp lệ (`ACTIVE`) và nhập đúng email, mật khẩu.
  - **When** người dùng gửi yêu cầu đăng nhập.
  - **Then** hệ thống trả về mã `200 OK` kèm theo `accessToken` và `refreshToken`.
  - **And** reset số lần đăng nhập sai về 0.
- **Scenario 2: Sai thông tin đăng nhập**
  - **Given** người dùng nhập sai email hoặc mật khẩu.
  - **When** người dùng gửi yêu cầu đăng nhập.
  - **Then** hệ thống trả về mã `401 Unauthorized` với thông báo "Tài khoản hoặc mật khẩu không chính xác".
  - **And** số lần đăng nhập sai tăng lên 1 (nếu email tồn tại).
- **Scenario 3: Khóa tài khoản do Brute-force**
  - **Given** người dùng đã nhập sai mật khẩu 4 lần trước đó.
  - **When** người dùng nhập sai mật khẩu lần thứ 5.
  - **Then** hệ thống trả về mã `429 Too Many Requests` hoặc `403 Forbidden` với thông báo "Tài khoản đã bị tạm khóa do nhập sai nhiều lần".
  - **And** đánh dấu tài khoản là `LOCKED` với thời gian hết hạn khóa.
- **Scenario 4: Đăng nhập với tài khoản chưa xác thực**
  - **Given** tài khoản đang ở trạng thái `PENDING_VERIFICATION`.
  - **When** người dùng nhập đúng email và mật khẩu.
  - **Then** hệ thống trả về mã `403 Forbidden` với thông báo "Vui lòng xác thực email trước khi đăng nhập".

### 7. Chỉ số phi chức năng
- **Bảo mật:** Quá trình so sánh mật khẩu phải chống Timing Attack. Tránh tiết lộ danh sách user qua thời gian phản hồi.
- **Thời gian phản hồi (Latency):** API đăng nhập phản hồi dưới 300ms.

## MOD-IAM-03: Đăng nhập bằng Google SSO (Google OAuth2 SSO)

### 1. Mô tả nghiệp vụ đầy đủ
Luồng xử lý chính khi người dùng sử dụng Google để đăng nhập hoặc đăng ký tài khoản:
1. **Khởi tạo luồng OAuth2:** Người dùng bấm nút "Đăng nhập bằng Google" trên giao diện. Frontend chuyển hướng người dùng đến trang xác thực của Google hoặc gọi API Backend để lấy URL xác thực.
2. **Xác thực và cấp quyền:** Người dùng đăng nhập tài khoản Google và cấp quyền truy cập thông tin cơ bản (email, profile).
3. **Nhận Authorization Code:** Sau khi cấp quyền thành công, Google chuyển hướng (redirect) về hệ thống kèm theo một `code` (Authorization Code) và `state` (để chống CSRF).
4. **Xác thực Code và Lấy Token:** Backend nhận `code` từ Frontend (hoặc qua Redirect URI), gửi request đến Google Token API để đổi lấy Google Access Token và ID Token.
5. **Lấy thông tin người dùng:** Backend sử dụng token (hoặc decode ID Token) để lấy thông tin người dùng từ Google (Email, Tên, Avatar).
6. **Xử lý tài khoản trong hệ thống:** 
   - Truy vấn CSDL theo email nhận được từ Google.
   - **Trường hợp 1 (Email chưa tồn tại):** Tự động tạo một tài khoản mới với email, họ tên lấy từ Google, trạng thái `ACTIVE` (vì email Google đã được Google xác thực) và không cần mật khẩu.
   - **Trường hợp 2 (Email đã tồn tại):** Liên kết (link account) tài khoản Google này vào tài khoản đã có sẵn trong hệ thống để người dùng có thể đăng nhập bằng cả 2 cách.
7. **Khởi tạo Token (JWT):** Tạo hệ thống Access Token và Refresh Token nội bộ của OmniChat cho user đó.
8. **Trả về kết quả:** Trả về Access Token và Refresh Token cho Frontend để bắt đầu phiên làm việc.

### 2. Input
- **Dữ liệu đầu vào (REST API Request):**
  - `code` (String, Authorization Code do Google cấp, Bắt buộc).
  - Hoặc `credential` (ID Token của Google nếu dùng luồng Google One Tap).
- **Nguồn kích hoạt:** Từ Frontend Web App gửi lên sau khi Google redirect về.

### 3. Output
- **Kết quả trả về (API Response):**
  - Trả về mã HTTP `200 OK`.
  - Body/Header chứa: `accessToken`, `refreshToken`, `expiresIn`.
  - Trả về thông tin cơ bản của user (id, name, email, avatar).
- **Nơi lưu:** CSDL (Bảng `users` để thêm mới hoặc cập nhật thông tin; Bảng `linked_accounts` để lưu Google Provider ID). Redis (Lưu session / Refresh Token).
- **Nơi gửi tiếp:** Publish event `user.registered` (nếu là user mới hoàn toàn) để gửi email welcome (Không gửi email verify).

### 4. Business rule / Ràng buộc
- **Bảo mật State (CSRF):** Luồng OAuth cần sử dụng tham số `state` để ngăn chặn tấn công CSRF.
- **Xác thực Email:** Tài khoản tạo từ Google SSO mặc định có trạng thái `ACTIVE` mà không cần trải qua bước xác thực email.
- **Không yêu cầu mật khẩu:** User đăng ký qua Google SSO có thể để trống trường mật khẩu (hoặc có cờ `auth_provider = GOOGLE`).

### 5. Edge case cần xử lý
- **Google Token hết hạn hoặc không hợp lệ:** Trả về `401 Unauthorized` và yêu cầu người dùng đăng nhập lại từ đầu.
- **Email Google trùng với Local Account nhưng bị khóa (LOCKED/SUSPENDED):** Chặn đăng nhập và thông báo "Tài khoản của bạn đã bị khóa".
- **Chặn thu thập Email từ Google:** Báo lỗi `400 Bad Request` "Không thể lấy email từ Google".
- **Race Condition khi tự động tạo tài khoản:** 2 request SSO cùng lúc cho 1 email chưa tồn tại -> Bắt `DuplicateKeyException` và rơi vào Trường hợp 2 (Link account) cho request đến sau.

### 6. Acceptance Criteria
- **Scenario 1: Đăng nhập Google với người dùng mới**
  - **Given** người dùng chưa có tài khoản trong hệ thống.
  - **When** người dùng thực hiện đăng nhập qua Google thành công.
  - **Then** hệ thống tự động tạo một tài khoản `ACTIVE` với email của Google.
  - **And** trả về mã `200 OK` kèm `accessToken` và `refreshToken`.
- **Scenario 2: Đăng nhập Google với người dùng đã tồn tại (Local)**
  - **Given** người dùng đã đăng ký tài khoản bằng email "user@gmail.com" và mật khẩu.
  - **When** người dùng thực hiện đăng nhập qua Google với đúng email "user@gmail.com".
  - **Then** hệ thống liên kết tài khoản Google này vào tài khoản đã có.
  - **And** trả về mã `200 OK` kèm `accessToken` và `refreshToken`.
- **Scenario 3: Đăng nhập với tài khoản bị khóa**
  - **Given** tài khoản "user@gmail.com" trong hệ thống đang bị `SUSPENDED`.
  - **When** người dùng thực hiện đăng nhập qua Google với email này.
  - **Then** hệ thống từ chối đăng nhập và trả về mã `403 Forbidden` kèm thông báo "Tài khoản của bạn đã bị khóa".

### 7. Chỉ số phi chức năng
- **Bảo mật:** Secret Key của Google Client phải được lưu trữ an toàn.
- **Thời gian phản hồi:** Tùy thuộc vào độ trễ của API Google, nhưng phần xử lý logic nội bộ (sau khi nhận code) phải dưới 500ms.

## MOD-IAM-04: Làm mới Token (Refresh JWT)

### 1. Mô tả nghiệp vụ đầy đủ
Luồng xử lý khi Access Token của người dùng hết hạn và Frontend tự động yêu cầu cấp token mới bằng Refresh Token:
1. **Frontend nhận diện lỗi:** Khi Access Token hết hạn, API trả về `401 Unauthorized`.
2. **Gửi yêu cầu làm mới:** Frontend sử dụng Refresh Token hiện tại để gọi API cấp lại Access Token mới.
3. **Xác thực Refresh Token:** Backend kiểm tra:
   - Tính hợp lệ của chữ ký (Signature).
   - Thời hạn (Expiration).
   - Sự tồn tại của token (hoặc mã băm của nó) trong CSDL / Redis (chưa bị thu hồi hoặc đăng xuất).
   - **Refresh Token Rotation:** Kiểm tra xem token này đã từng được sử dụng chưa. Nếu đã sử dụng (Reuse/Replay attack), lập tức đánh dấu tài khoản hoặc chuỗi session này có rủi ro và thu hồi TOÀN BỘ các Refresh Token của phiên đó.
4. **Cấp phát Token mới:** 
   - Nếu hợp lệ, hệ thống sẽ tạo một Access Token mới.
   - Đồng thời, tạo một Refresh Token mới (Refresh Token Rotation) và lưu trữ mã băm mới, xóa hoặc đánh dấu mã băm cũ là đã sử dụng.
5. **Trả kết quả:** Trả về bộ đôi Access Token và Refresh Token mới cho Frontend.

### 2. Input
- **Dữ liệu đầu vào (REST API Request):**
  - `refreshToken` (String, Bắt buộc). Có thể truyền qua HttpOnly Cookie hoặc Request Body tùy chiến lược.
- **Nguồn kích hoạt:** Tự động từ Frontend Web App / Mobile App khi nhận HTTP 401.

### 3. Output
- **Kết quả trả về (API Response):**
  - Trả về mã HTTP `200 OK`.
  - Body/Header chứa: `accessToken` mới, `refreshToken` mới, `expiresIn`.
- **Nơi lưu:** Redis hoặc CSDL (Lưu thông tin Refresh Token mới, vô hiệu hóa/đánh dấu đã sử dụng cho Refresh Token cũ).
- **Nơi gửi tiếp:** Không có.

### 4. Business rule / Ràng buộc
- **Refresh Token Rotation (Luân chuyển 1 lần dùng):** Mỗi Refresh Token CHỈ được sử dụng đúng 1 lần. Khi sử dụng thành công, nó sẽ bị vô hiệu hóa và thay bằng một Refresh Token mới.
- **Giới hạn thời gian (Absolute TTL vs Inactivity TTL):** 
  - Refresh Token có thể có thời gian sống tuyệt đối (VD: 30 ngày kể từ lúc đăng nhập).
  - Hoặc thời gian sống do không hoạt động (VD: 7 ngày Inactivity).
- **Trạng thái tài khoản:** Nếu tài khoản đã bị khóa (LOCKED) hoặc vô hiệu hóa (SUSPENDED) từ phía Admin, yêu cầu làm mới token sẽ bị từ chối dù token vẫn còn hạn.

### 5. Edge case cần xử lý
- **Replay Attack (Token reuse):** Một kẻ tấn công đánh cắp Refresh Token cũ và cố gắng sử dụng. Hệ thống phát hiện token này "đã được sử dụng" -> Lập tức thu hồi TOÀN BỘ các token thuộc họ (family) session đó. Yêu cầu user phải đăng nhập lại bằng mật khẩu.
- **Refresh Token bị sửa đổi/không hợp lệ:** Trả về `401 Unauthorized` hoặc `400 Bad Request`.
- **Concurrent Requests (Race condition):** Khi Frontend gọi nhiều API cùng lúc, có thể dẫn đến việc nhiều request bị lỗi 401 và cùng gọi Refresh API cùng lúc với cùng 1 Refresh Token. Hệ thống cần có cơ chế (VD: Redis Distributed Lock cho userId+sessionId hoặc grace period 30 giây) để cho phép các request đồng thời dùng lại token cũ trong một khoảng thời gian cực ngắn mà không kích hoạt cơ chế chống Replay Attack oan.

### 6. Acceptance Criteria
- **Scenario 1: Refresh Token thành công**
  - **Given** người dùng gửi một Refresh Token còn hạn và hợp lệ (chưa từng sử dụng).
  - **When** gọi API cấp lại token.
  - **Then** hệ thống tạo và lưu trữ `refreshToken` mới, vô hiệu hóa `refreshToken` cũ.
  - **And** trả về `200 OK` kèm `accessToken` và `refreshToken` mới.
- **Scenario 2: Refresh Token hết hạn hoặc không hợp lệ**
  - **Given** người dùng gửi Refresh Token đã hết thời gian sống hoặc sai định dạng.
  - **When** gọi API cấp lại token.
  - **Then** hệ thống trả về `401 Unauthorized` kèm thông báo lỗi.
  - **And** yêu cầu Frontend điều hướng người dùng về trang Đăng nhập.
- **Scenario 3: Phát hiện tái sử dụng Refresh Token (Replay Attack)**
  - **Given** một Refresh Token cũ (đã từng được sử dụng để lấy token mới) lại được gửi lên.
  - **When** gọi API cấp lại token.
  - **Then** hệ thống phát hiện hành vi tái sử dụng.
  - **And** thu hồi tất cả các Refresh Token liên quan đến phiên làm việc này.
  - **And** trả về `401 Unauthorized`.
- **Scenario 4: Tài khoản bị khóa khi đang có Refresh Token hợp lệ**
  - **Given** Refresh Token hợp lệ nhưng tài khoản đã bị `SUSPENDED` hoặc `LOCKED`.
  - **When** gọi API cấp lại token.
  - **Then** hệ thống từ chối và trả về `403 Forbidden`.

### 7. Chỉ số phi chức năng
- **Hiệu năng:** Quá trình kiểm tra và cấp phát token mới diễn ra rất nhanh (< 100ms) để không làm gián đoạn trải nghiệm người dùng.
- **Khóa phân tán (Distributed Lock):** Bắt buộc phải xử lý concurrency tốt khi thiết kế Refresh Token Rotation.

## MOD-IAM-05: Đăng xuất (Logout & Blacklist Token)

### 1. Mô tả nghiệp vụ đầy đủ
Luồng xử lý khi người dùng chủ động đăng xuất khỏi hệ thống hoặc Admin yêu cầu đăng xuất bắt buộc (Force Logout):
1. **Tiếp nhận yêu cầu:** 
   - **Đăng xuất thông thường:** Người dùng nhấn nút "Đăng xuất" trên giao diện. Frontend gửi request lên API mang theo `accessToken` (qua Header) và `refreshToken` (qua Cookie/Body).
   - **Force Logout:** Quản trị viên (hoặc hệ thống) yêu cầu đăng xuất người dùng khỏi tất cả các thiết bị.
2. **Hủy Refresh Token:** 
   - Với đăng xuất thông thường: Backend tìm mã băm của `refreshToken` trong CSDL/Redis tương ứng với phiên hiện tại và xóa/đánh dấu là vô hiệu lực. Các phiên trên thiết bị khác vẫn hoạt động bình thường.
   - Với Force Logout: Backend vô hiệu hóa toàn bộ Refresh Token của `userId` đó.
3. **Đưa Access Token vào Blacklist (Danh sách đen):**
   - Access Token là dạng Stateless (JWT), không thể thu hồi trực tiếp từ DB. Do đó, Backend sẽ trích xuất `jti` (JWT ID) hoặc chữ ký của `accessToken` và lưu vào Redis (Blacklist) với TTL (Time-To-Live) bằng chính thời gian sống còn lại của Access Token đó.
   - Mọi request API tiếp theo có mang Access Token này sẽ bị filter của Gateway hoặc Auth Service chặn lại khi kiểm tra thấy nó nằm trong Blacklist.
4. **Phát sự kiện (Broadcast Event):** 
   - Nếu hệ thống có kết nối Realtime (WebSocket), Backend publish sự kiện `TokenBlacklistedEvent` (hoặc `UserLoggedOutEvent`) qua Kafka/Redis PubSub.
   - Module Realtime (M10) nhận sự kiện và tiến hành ngắt kết nối WebSocket của phiên (hoặc toàn bộ phiên nếu là Force Logout) ngay lập tức.
5. **Trả kết quả:** Trả về HTTP 200 OK cho Frontend. Frontend tiến hành xóa token ở LocalStorage/Cookie và điều hướng về trang Đăng nhập.

### 2. Input
- **Dữ liệu đầu vào (REST API Request):**
  - Header: `Authorization: Bearer <accessToken>` (Bắt buộc).
  - Body/Cookie: `refreshToken` (Tùy chọn, để hủy đúng phiên).
  - Tham số `allDevices=true` (Tùy chọn, dùng cho Force Logout).
- **Nguồn kích hoạt:** Từ Frontend Web App / Mobile App, hoặc từ Admin Portal.

### 3. Output
- **Kết quả trả về (API Response):**
  - Trả về mã HTTP `200 OK` (Hoặc `204 No Content`).
  - Xóa Cookie (Set-Cookie `refreshToken` với Max-Age=0) nếu dùng HttpOnly Cookie.
- **Nơi lưu:** Redis (Thêm `accessToken` vào Blacklist, xóa `refreshToken`). CSDL (Cập nhật trạng thái Refresh Token nếu lưu ở DB).
- **Nơi gửi tiếp:** Publish event `TokenBlacklistedEvent` qua Message Broker.

### 4. Business rule / Ràng buộc
- **TTL của Blacklist:** JWT trong Redis Blacklist không lưu vĩnh viễn, mà chỉ lưu bằng thời gian hết hạn (`exp`) của chính JWT đó trừ đi thời gian hiện tại, để tiết kiệm bộ nhớ Redis.
- **Idempotent:** Việc gọi API Đăng xuất nhiều lần cho cùng một token sẽ không gây ra lỗi (trả về 200 OK).

### 5. Edge case cần xử lý
- **Access Token đã hết hạn:** Nếu user gọi API đăng xuất nhưng Access Token đã hết hạn, hệ thống trả về `401` hoặc có thể êm xuôi trả về `200` vì mục đích cuối cùng vẫn là đăng xuất (tùy thiết kế, thường nên trả về `200` để Frontend xóa data).
- **Không truyền Refresh Token:** Nếu Frontend không truyền được Refresh Token (bị mất), hệ thống vẫn phải đưa Access Token hiện tại vào Blacklist để ngắt quyền truy cập ngay lập tức.
- **Cache Redis bị lỗi (Down):** Hệ thống sẽ không thể check/thêm Blacklist. Tùy thuộc vào chính sách chịu lỗi (Degraded Mode), có thể log lại lỗi và vẫn cho phép xóa Refresh Token trong DB.

### 6. Acceptance Criteria
- **Scenario 1: Đăng xuất một thiết bị thành công**
  - **Given** người dùng đang đăng nhập với `accessToken` và `refreshToken` hợp lệ.
  - **When** gửi yêu cầu POST `/auth/logout`.
  - **Then** hệ thống xóa `refreshToken` của phiên này.
  - **And** thêm `accessToken` vào Redis Blacklist với TTL tương ứng.
  - **And** trả về `200 OK`.
- **Scenario 2: Truy cập sau khi đăng xuất**
  - **Given** người dùng đã đăng xuất thành công và `accessToken` nằm trong Blacklist.
  - **When** người dùng dùng lại `accessToken` đó để gọi một API yêu cầu quyền đăng nhập (VD: Lấy profile).
  - **Then** hệ thống từ chối và trả về `401 Unauthorized`.
- **Scenario 3: Đăng xuất tất cả thiết bị (Force Logout)**
  - **Given** người dùng đang đăng nhập trên Điện thoại và Máy tính.
  - **When** gửi yêu cầu POST `/auth/logout?allDevices=true`.
  - **Then** hệ thống vô hiệu hóa TẤT CẢ `refreshToken` của người dùng.
  - **And** thêm `accessToken` hiện tại vào Blacklist.
  - **And** phát event ngắt kết nối WebSocket (nếu có).

### 7. Chỉ số phi chức năng
- **Hiệu năng Blacklist:** Việc kiểm tra Blacklist trên Redis ở mọi request API phải cực kỳ nhanh (< 5ms) để không làm chậm toàn bộ hệ thống (thường đặt ở API Gateway).
- **Bộ nhớ Redis:** Sử dụng cơ chế tự động hết hạn (TTL) của Redis để không làm phình to RAM của server cache.

## MOD-IAM-06: Quản lý Vai trò (Role Management)

### 1. Mô tả nghiệp vụ đầy đủ
Luồng xử lý CRUD đối với Vai trò (Role) trong hệ thống. Role là tập hợp các quyền (Permissions) và được dùng để gán cho User (Super Admin gán Role hệ thống, hoặc Tenant Admin gán Role trong Tenant của mình).
1. **Lấy danh sách Role (Read):** Super Admin (hoặc người có quyền tương đương) truy cập giao diện Quản lý Vai trò. Hệ thống truy xuất danh sách các Role (gồm ID, tên, mô tả, có phải là role hệ thống mặc định không).
2. **Tạo Role mới (Create):** Quản trị viên nhập Tên role và Mô tả. Hệ thống kiểm tra tên role có bị trùng không, sau đó lưu vào CSDL.
3. **Cập nhật Role (Update):** Quản trị viên có thể đổi tên và mô tả của một Role tự tạo. Các Role mặc định của hệ thống (System Defined như Super Admin, Tenant Owner) sẽ bị khóa, không cho phép đổi tên.
4. **Xóa Role (Delete):** Quản trị viên yêu cầu xóa một Role. Hệ thống kiểm tra xem Role này có đang được gán cho bất kỳ User nào không.
   - Nếu đang có User sử dụng: Chặn thao tác xóa và báo lỗi.
   - Nếu không có User nào sử dụng (và không phải Role hệ thống): Tiến hành xóa (Hard delete hoặc Soft delete).

### 2. Input
- **Dữ liệu đầu vào (REST API Request):**
  - **Create:** `roleName` (String, Bắt buộc, Unique), `description` (String, Tùy chọn).
  - **Update:** `roleId` (UUID, Bắt buộc), `roleName` (String), `description` (String).
  - **Delete:** `roleId` (UUID, Bắt buộc).
- **Nguồn kích hoạt:** Từ Frontend Admin Portal.

### 3. Output
- **Kết quả trả về (API Response):**
  - Create: Trả về mã HTTP `201 Created` kèm thông tin Role vừa tạo.
  - Update: Trả về mã HTTP `200 OK` kèm thông tin Role đã cập nhật.
  - Delete: Trả về mã HTTP `204 No Content` hoặc `200 OK`.
- **Nơi lưu:** CSDL (Bảng `roles`).
- **Nơi gửi tiếp:** Không có.

### 4. Business rule / Ràng buộc
- **Role hệ thống (System Defined):** Các Role cốt lõi như `SUPER_ADMIN`, `TENANT_OWNER` được đánh dấu là System Defined (`is_system = true`). Không thể sửa tên, không thể xóa.
- **Tính duy nhất:** Tên Role (`roleName`) hoặc Mã Role (`roleCode`) phải là duy nhất.
- **Ràng buộc khóa ngoại (Constraint):** Tuyệt đối KHÔNG cho phép xóa một Role nếu đang có ít nhất 1 bản ghi trong bảng `user_roles` (hoặc bảng mapping tương đương) trỏ tới Role này.

### 5. Edge case cần xử lý
- **Sửa/Tạo trùng tên:** Khi tạo mới hoặc đổi tên Role sang một tên đã tồn tại -> Trả về lỗi `409 Conflict`.
- **Phân trang & Tìm kiếm:** Khi lấy danh sách Role, hỗ trợ phân trang (Pagination) và tìm kiếm theo tên để tránh tải lượng dữ liệu lớn.

### 6. Acceptance Criteria
- **Scenario 1: Tạo Role mới thành công**
  - **Given** Admin đăng nhập với quyền quản lý Role và nhập "Customer Service" chưa tồn tại.
  - **When** gọi API tạo Role.
  - **Then** hệ thống lưu Role mới vào CSDL.
  - **And** trả về mã `201 Created` kèm dữ liệu Role.
- **Scenario 2: Sửa Role hệ thống**
  - **Given** Admin cố gắng sửa tên Role `SUPER_ADMIN` (có cờ `is_system = true`).
  - **When** gọi API cập nhật Role.
  - **Then** hệ thống từ chối và trả về mã `403 Forbidden` với thông báo "Không thể sửa vai trò mặc định của hệ thống".
- **Scenario 3: Xóa Role đang được sử dụng**
  - **Given** Role "Agent" đang được gán cho ít nhất 1 nhân viên.
  - **When** Admin gọi API xóa Role "Agent".
  - **Then** hệ thống từ chối và trả về mã `400 Bad Request` hoặc `409 Conflict` kèm thông báo "Không thể xóa vai trò đang được gán cho người dùng".
- **Scenario 4: Xóa Role thành công**
  - **Given** Role "Test Role" không có bất kỳ ai được gán.
  - **When** Admin gọi API xóa Role này.
  - **Then** hệ thống xóa (hoặc ẩn) Role khỏi CSDL.
  - **And** trả về mã `200 OK` hoặc `204 No Content`.

### 7. Chỉ số phi chức năng
- **Bảo mật:** API CRUD Role yêu cầu người gọi phải có Permission phân quyền quản trị (Authorization).
- **Audit Log:** Mọi thao tác Create, Update, Delete Role đều phải được ghi log (Audit Trail) để truy vết.

## MOD-IAM-07: Quản lý Phân quyền (Permission / RBAC Management)

### 1. Mô tả nghiệp vụ đầy đủ
Hệ thống sử dụng mô hình RBAC (Role-Based Access Control). Quyền hạn cụ thể (Permission - ví dụ: `VIEW_INBOX`, `REPLY_MESSAGE`, `MANAGE_TENANT`) được gắn vào Vai trò (Role), và Vai trò được gắn cho Người dùng.
1. **Lấy danh sách Permission (Read):** Lấy toàn bộ danh sách các Permission hiện có trong hệ thống (các Permission này thường được định nghĩa sẵn trong Code/DB bởi developer và ít khi được tạo mới qua UI).
2. **Gán Permission cho Role (Assign/Update):** Quản trị viên chọn một Role (ví dụ: "Agent"), sau đó chọn một tập hợp các Permissions tương ứng từ danh sách để gán cho Role này. Hành động này sẽ thay thế (hoặc thêm/bớt) các quyền cũ của Role đó.
3. **Phân quyền theo Tenant:** Một user có thể tham gia nhiều Tenant (Không gian làm việc). RBAC phải nhận diện được ngữ cảnh: User U có Role R trong Tenant T. Khi gọi API, phải lấy được danh sách Permission của U **tại Tenant T** đang truy cập.

### 2. Input
- **Dữ liệu đầu vào (REST API Request):**
  - **Gán Permission:** `roleId` (UUID, Bắt buộc), `permissionIds` (Array of UUID/String, Bắt buộc - danh sách các quyền sẽ gắn vào role).
- **Nguồn kích hoạt:** Từ Frontend Admin Portal (Giao diện Quản lý Role & Phân quyền).

### 3. Output
- **Kết quả trả về (API Response):**
  - Trả về mã HTTP `200 OK` nếu gán thành công.
- **Nơi lưu:** CSDL (Bảng trung gian `role_permissions`).
- **Nơi gửi tiếp:** Tùy chọn: Publish event `role_permissions.updated` để các service khác clear cache (nếu cache permission).

### 4. Business rule / Ràng buộc
- **Role hệ thống (Super Admin):** Role `SUPER_ADMIN` mặc định có tất cả quyền trong hệ thống. Hệ thống chặn việc thay đổi tập quyền của Role này.
- **Tính trọn vẹn (Replace all):** Khi gọi API gán quyền cho 1 Role, danh sách `permissionIds` gửi lên sẽ GHI ĐÈ hoàn toàn (Replace) các quyền hiện tại của Role đó.
- **Scope (Phạm vi):** Có 2 loại Permission: `SYSTEM` (áp dụng toàn hệ thống, VD: Quản lý Tenant) và `TENANT` (chỉ có ý nghĩa trong 1 Tenant cụ thể, VD: Gửi tin nhắn). Frontend cần phân nhóm rõ ràng để tránh nhầm lẫn khi cấp quyền.

### 5. Edge case cần xử lý
- **Cascade khi xóa Permission:** Mặc dù ít xảy ra (chủ yếu qua DB migration), nhưng nếu 1 Permission bị xóa khỏi hệ thống, các bản ghi mapping trong `role_permissions` liên quan phải bị xóa theo (Cascade Delete) để tránh lỗi dữ liệu.
- **Hiệu lực tức thời vs Cache:** Khi Admin cập nhật quyền của Role X, các User đang online sử dụng Role X sẽ bị ảnh hưởng thế nào? Nếu permission được check tại Gateway qua JWT (mang theo mảng permissions), User có thể phải đăng nhập lại hoặc Gateway phải check trực tiếp từ Redis thay vì tin tưởng JWT hoàn toàn. (Giải pháp: Cache User Permissions trên Redis và invalidate cache khi Role thay đổi).

### 6. Acceptance Criteria
- **Scenario 1: Gán quyền cho Role thành công**
  - **Given** Role "Customer Service" đang có quyền `VIEW_INBOX`.
  - **When** Admin gọi API gán danh sách quyền mới gồm `VIEW_INBOX` và `REPLY_MESSAGE`.
  - **Then** hệ thống ghi nhận Role này có 2 quyền trên vào CSDL (bảng `role_permissions`).
  - **And** trả về `200 OK`.
- **Scenario 2: Sửa quyền của Role hệ thống**
  - **Given** Admin cố gắng gán/sửa quyền cho Role `SUPER_ADMIN`.
  - **When** gọi API gán quyền.
  - **Then** hệ thống từ chối và trả về `403 Forbidden` với thông báo "Không thể thay đổi quyền của vai trò hệ thống".
- **Scenario 3: Xóa trắng quyền của một Role**
  - **Given** Role "Guest" đang có một số quyền.
  - **When** Admin gọi API gán quyền với danh sách `permissionIds` rỗng `[]`.
  - **Then** hệ thống xóa tất cả các quyền của Role này trong bảng `role_permissions`.
  - **And** trả về `200 OK`.

### 7. Chỉ số phi chức năng
- **Hiệu năng:** Việc lấy danh sách quyền của User khi đăng nhập / verify ở Gateway phải nhanh, thường dùng Cache trên Redis để giảm tải DB.

## MOD-IAM-08: Lấy thông tin tài khoản hiện tại (Get Current Profile / Introspect)

### 1. Mô tả nghiệp vụ đầy đủ
Luồng xử lý khi Frontend cần lấy thông tin chi tiết của người dùng đang đăng nhập (thường gọi khi khởi tạo ứng dụng - App Initialization hoặc reload trang):
1. **Tiếp nhận yêu cầu:** Frontend gọi API lấy thông tin profile, gửi kèm `accessToken` qua Header.
2. **Kiểm tra và Xác thực Token:** 
   - Backend (hoặc API Gateway) xác thực chữ ký (Signature) và thời hạn của JWT.
   - Kiểm tra xem token này có nằm trong Blacklist (đã bị đăng xuất) hay không.
3. **Lấy dữ liệu người dùng:**
   - Trích xuất `userId` từ JWT.
   - Truy vấn CSDL để lấy thông tin cá nhân (Tên, Email, Avatar, Trạng thái tài khoản).
   - Truy vấn danh sách các Tenant (Workspace) mà người dùng đang tham gia.
   - Truy vấn danh sách Role và Permissions của người dùng. (Nếu hệ thống áp dụng Permission theo Tenant, danh sách quyền sẽ được gom nhóm theo từng `tenantId`).
4. **Trả kết quả:** Tổng hợp tất cả dữ liệu và trả về cho Frontend để render UI (hiển thị Avatar, Tên, ẩn/hiện các menu dựa trên Permission, danh sách Tenant để chuyển đổi).

### 2. Input
- **Dữ liệu đầu vào (REST API Request):**
  - Header: `Authorization: Bearer <accessToken>` (Bắt buộc).
- **Nguồn kích hoạt:** Từ Frontend Web App / Mobile App (Thường gọi ở endpoint `/auth/me` hoặc `/users/me`).

### 3. Output
- **Kết quả trả về (API Response):**
  - Trả về mã HTTP `200 OK`.
  - Body chứa: 
    - `id`, `email`, `fullName`, `avatar`, `status`.
    - `tenants`: Mảng danh sách các Tenant mà user là thành viên (kèm Role tương ứng trong từng Tenant).
    - `globalRoles` / `globalPermissions`: Các quyền cấp hệ thống (nếu có).
- **Nơi lưu:** Không có (Thao tác Read-only).
- **Nơi gửi tiếp:** Không có.

### 4. Business rule / Ràng buộc
- **Trạng thái tài khoản (Status Sync):** Mặc dù Access Token còn hạn, nhưng nếu Admin vừa khóa tài khoản (chuyển sang `SUSPENDED` hoặc `LOCKED`), API này phải phát hiện và trả về lỗi `403 Forbidden` (hoặc báo token không hợp lệ) để ép Frontend đăng xuất người dùng ra ngay lập tức.
- **Phân tách ngữ cảnh Tenant:** Output cần trả về cấu trúc rõ ràng giữa "Quyền toàn cục" (VD: Quyền tạo Tenant, Quyền quản trị hệ thống) và "Quyền trong từng Tenant" (VD: Tenant A có quyền `VIEW_INBOX`, Tenant B có quyền `OWNER`) để Frontend dễ dàng xử lý logic Role-guard.

### 5. Edge case cần xử lý
- **Access Token hết hạn hoặc không hợp lệ:** Trả về `401 Unauthorized` ngay lập tức. Frontend sẽ tự động trigger quy trình Refresh Token (MOD-IAM-04).
- **Token nằm trong Blacklist:** Dù token chưa hết hạn nhưng đã bị đăng xuất trước đó, hệ thống chặn lại và trả về `401 Unauthorized`.
- **Tài khoản bị xóa cứng (Hard Delete):** Token còn hạn nhưng truy vấn DB không thấy `userId` (Rất hiếm) -> Trả về `401 Unauthorized`.

### 6. Acceptance Criteria
- **Scenario 1: Lấy Profile thành công**
  - **Given** người dùng gửi request với Access Token hợp lệ của tài khoản `ACTIVE`.
  - **When** gọi API `/auth/me`.
  - **Then** hệ thống trả về HTTP `200 OK`.
  - **And** Body chứa thông tin cơ bản, danh sách Tenants tham gia và các Permissions hiện có.
- **Scenario 2: Token đã bị đưa vào Blacklist**
  - **Given** người dùng gửi request với Access Token đã bị đăng xuất (nằm trong Redis Blacklist).
  - **When** gọi API lấy Profile.
  - **Then** hệ thống chặn request ở middleware/Gateway.
  - **And** trả về mã `401 Unauthorized`.
- **Scenario 3: Tài khoản đã bị khóa dù Token còn hạn**
  - **Given** người dùng gửi request với Access Token hợp lệ, nhưng trạng thái tài khoản trong DB là `SUSPENDED`.
  - **When** gọi API lấy Profile.
  - **Then** hệ thống phát hiện trạng thái không hợp lệ.
  - **And** trả về mã `403 Forbidden` (hoặc 401) kèm thông báo "Tài khoản của bạn đã bị khóa".

### 7. Chỉ số phi chức năng
- **Hiệu năng (Latency):** Vì API này được gọi thường xuyên lúc load ứng dụng, thời gian phản hồi cần tối ưu (< 100ms). Cân nhắc dùng Cache (Redis) để lưu Profile/Permissions của User nếu câu truy vấn JOIN quá nhiều bảng, và xóa Cache khi có event thay đổi thông tin (VD: Cập nhật Role, đổi tên).
