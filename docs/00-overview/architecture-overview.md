# Architecture Overview

Hệ thống OmniChat được thiết kế để xử lý lượng tương tác lớn theo thời gian thực (Real-time).

## 1. Sơ đồ kiến trúc mức cao (High-Level Architecture)

```mermaid
graph TD
    Client[Web/Mobile App] --> |HTTPS/WSS| Gateway[API Gateway]
    
    subgraph Infrastructure
        Redis[(Redis)]
        MySQL[(MySQL - Auth)]
        MQ[RabbitMQ]
    end
    
    subgraph Discovery & Config
        Config[Config Server]
        Discovery[Discovery Server]
    end
    
    subgraph Microservices
        Auth[Auth Service]
        Dummy[Dummy Service]
        Conversation[Conversation Service - TODO]
        Routing[Routing Service - TODO]
    end

    Gateway --> |Routing + JWT Check| Auth
    Gateway --> |Routing + JWT Check| Dummy
    Gateway --> |Routing + JWT Check| Conversation
    
    Auth --> |Read/Write| MySQL
    Auth --> |Blacklist| Redis
    Gateway --> |Rate Limiting| Redis
    
    Config -.-> |Provides Config| Auth
    Config -.-> |Provides Config| Gateway
    Config -.-> |Provides Config| Dummy
    
    Auth -.-> |Registers| Discovery
    Gateway -.-> |Registers| Discovery
    Dummy -.-> |Registers| Discovery
```

## 2. Luồng xử lý tin nhắn (Message Flow)
1. Khách hàng nhắn tin vào Fanpage/Zalo OA.
2. Nền tảng (Facebook/Zalo) gọi Webhook gửi dữ liệu tới API của hệ thống.
3. Hệ thống chuẩn hóa dữ liệu tin nhắn, đẩy vào hàng đợi (RabbitMQ) để xử lý bất đồng bộ.
4. Background Worker (hoặc Consumer) xử lý tin nhắn, lưu vào Database (MongoDB).
5. Hệ thống phát sự kiện qua **Redis Pub/Sub** tới các WebSocket đang kết nối.
6. Giao diện frontend của nhân viên tự động cập nhật tin nhắn mới theo thời gian thực.
