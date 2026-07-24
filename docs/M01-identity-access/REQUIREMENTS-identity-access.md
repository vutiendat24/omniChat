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
