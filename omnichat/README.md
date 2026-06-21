# OmniChat Local Infrastructure

Môi trường local development sử dụng Docker Compose cho hệ thống OmniChat (Microservices).

## Yêu cầu hệ thống
- Docker
- Docker Compose

## Cấu trúc dịch vụ và Port
| Service  | Image | Port Host | Port Container | Mô tả |
| ------------- | ------------- | ------------- | ------------- | ------------- |
| MySQL 8.4 | `mysql:8.4` | `3306` | `3306` | Database chính, charset utf8mb4 |
| Redis 7 | `redis:7-alpine` | `6379` | `6379` | Cache & Data Storage tạm thời |
| Kafka | `bitnami/kafka:3.7` | `9092` | `9092` / `29092` | Event Streaming (KRaft mode) |
| Kafka UI | `provectuslabs/kafka-ui`| `8081` | `8080` | Web UI quản lý Kafka |

## Hướng dẫn sử dụng

### 1. Khởi tạo cấu hình (.env)
Tạo file `.env` từ file template:
```bash
cp .env.example .env
```
Bạn có thể tùy chỉnh các thông số trong file `.env` nếu cần thiết.

### 2. Khởi động môi trường (Start)
Chạy command sau ở thư mục chứa `docker-compose.yml` (chạy ngầm - daemon):
```bash
docker compose up -d
```
Kiểm tra trạng thái các container (chắc chắn tất cả đều ở trạng thái `Healthy`):
```bash
docker compose ps
```

### 3. Tắt môi trường (Stop)
Giữ nguyên data (volumes không bị xóa):
```bash
docker compose down
```

### 4. Reset dữ liệu (Clear Data)
Xóa toàn bộ container và data (volumes), hữu ích khi bạn muốn clean up môi trường và chạy lại từ đầu:
```bash
docker compose down -v
```

### 5. Truy cập Kafka UI
Truy cập qua trình duyệt để kiểm tra Kafka Broker:
http://localhost:8081

### Lệnh hữu ích
* Xem log của một service cụ thể:
```bash
docker compose logs -f <service_name>
# Ví dụ: docker compose logs -f kafka
```
* Vào bash/sh của container:
```bash
docker exec -it omnichat-mysql bash
```
