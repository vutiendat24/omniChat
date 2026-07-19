# ADR 002: Lựa chọn Thuật toán RS256 cho JSON Web Token (JWT)

**Ngày tạo:** 2026-07-19
**Trạng thái:** Accepted

## Bối cảnh (Context)
Hệ thống Omnichannel bao gồm nhiều Microservices giao tiếp thông qua API Gateway. Người dùng cuối đăng nhập thông qua `auth-service` và nhận được một chuỗi Token để uỷ quyền. Hệ thống quyết định dùng JWT để cấp quyền.
Vấn đề là chúng ta nên dùng thuật toán mã hoá đối xứng (`HS256`) hay bất đối xứng (`RS256`) để ký và xác thực token này tại tầng Gateway và các services đằng sau?

## Quyết định (Decision)
Quyết định sử dụng **RS256 (RSA Signature with SHA-256)** là thuật toán tiêu chuẩn cho JWT trong dự án này.
1. **Auth Service**: Là dịch vụ duy nhất giữ **Private Key**. Nó dùng Private Key để ký (sign) token khi user đăng nhập.
2. **API Gateway & Các microservices khác**: Sẽ chỉ được cấp phát **Public Key** (có thể qua Config Server hoặc qua endpoint JWKS). Chúng dùng Public Key này chỉ để đọc và xác minh (verify) token là hợp lệ, chứ không thể tự tạo token mới được.

## Hậu quả (Consequences)
**Pros:**
- Bảo mật cấp cao: Không phải chia sẻ bí mật (secret) ra toàn hệ thống. Kể cả khi có lỗ hổng ở API Gateway khiến Public Key bị lộ, kẻ tấn công cũng không thể giả mạo token.
- Phân tách trách nhiệm rõ ràng: Chỉ duy nhất 1 service tạo token, các service còn lại làm nhiệm vụ xác minh.
- Phù hợp với chuẩn SSO (Single Sign-On) và OAuth2 hiện đại.

**Cons:**
- Tốc độ tính toán của thuật toán bất đối xứng RS256 chậm hơn một chút so với HS256.
- Chiều dài của Token sinh ra sẽ dài hơn, tốn nhiều băng thông hơn.
- Triển khai phức tạp hơn: Phải tạo cặp khoá Public/Private (Keypair), lưu trữ an toàn thay vì chỉ dùng một chuỗi String đơn giản.
