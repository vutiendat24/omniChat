# Config Server

## Giới thiệu
Module `config-server` đóng vai trò là Centralized Configuration Server cho hệ thống Omnichannel Chat/Comment Management. Tất cả các microservices khác sẽ gọi tới đây để lấy cấu hình (thay vì lưu trực tiếp trong mã nguồn hoặc file local của từng service).

## Yêu cầu môi trường (Prerequisites)
- Java 21
- Maven
- (Khuyến nghị) `keytool` để sinh hoặc cập nhật file Keystore.

## Cài đặt (Installation)
1. Cài đặt các thư viện cần thiết tại thư mục `backend`:
```bash
mvn clean install -pl config-server -am
```
2. Cần có file `keystore.p12` đặt tại `config-server/src/main/resources/`. Nếu chưa có, bạn có thể tự sinh bằng câu lệnh sau (đã có sẵn trong JDK):
```bash
keytool -genkeypair -alias config-server-key -keyalg RSA -keysize 4096 -sigalg SHA512withRSA -dname "CN=Config Server,OU=OmniChat,O=OmniChat,L=HCM,C=VN" -keypass secret -keystore keystore.p12 -storepass secret -storetype PKCS12
```

## Biến môi trường (Environment Variables)
Cấu hình mặc định sử dụng profile `native` đọc từ `classpath:/config-repo`.
Các cấu hình quan trọng có thể ghi đè qua biến môi trường (hoặc `.env` nếu chạy Docker):

- `SPRING_PROFILES_ACTIVE`: Đặt thành `git` nếu muốn dùng Git Repo backend.
- `SPRING_CLOUD_CONFIG_SERVER_GIT_URI`: URL của Git repository.
- `SPRING_SECURITY_USER_NAME`: Username cho Basic Auth (Mặc định: `admin`).
- `SPRING_SECURITY_USER_PASSWORD`: Password cho Basic Auth (Mặc định: `admin_password`).
- `ENCRYPT_KEYSTORE_PASSWORD`: Mật khẩu của keystore (Mặc định: `secret`).

## Chạy ứng dụng (Running the app)
```bash
cd backend
mvn spring-boot:run -pl config-server
```
Ứng dụng sẽ chạy tại port `8888`.

### Kiểm tra sức khỏe (Health Check)
```bash
curl -u admin:admin_password http://localhost:8888/actuator/health
```

### Sinh chuỗi mã hoá (Encryption)
Để mã hoá một thông tin nhạy cảm:
```bash
curl -u admin:admin_password -X POST http://localhost:8888/encrypt -d "my-secret-password"
```
Kết quả trả về sẽ là chuỗi đã mã hoá. Bạn có thể chép chuỗi này vào Git Repo bằng định dạng `{cipher}chuỗi_mã_hoá`.
