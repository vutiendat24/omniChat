# TÀI LIỆU THIẾT KẾ API (API SPECIFICATION)
## HỆ THỐNG QUẢN LÝ CHAT ĐA KÊNH TẬP TRUNG (OCM)

Tài liệu này định nghĩa các chuẩn giao tiếp API RESTful giữa Frontend (Web/App) và Backend, cũng như các Webhook từ bên thứ ba (Facebook, Zalo, Shopee, TikTok) cho hệ thống OCM.

---

### 1. DANH SÁCH API CỐT LÕI (CORE API LIST)

| Method | Endpoint | Mô tả (Description) |
| :--- | :--- | :--- |
| **GET** | `/api/v1/conversations` | Lấy danh sách hội thoại (Hỗ trợ phân trang, lọc, sắp xếp). |
| **GET** | `/api/v1/conversations/{id}` | Lấy chi tiết một hội thoại và thông tin khách hàng. |
| **POST** | `/api/v1/conversations/{id}/messages` | Agent gửi tin nhắn mới vào hội thoại. |
| **GET** | `/api/v1/conversations/{id}/messages` | Lấy lịch sử tin nhắn của một hội thoại (Phân trang cursor-based/offset). |
| **PATCH** | `/api/v1/conversations/{id}/assign` | Gán/Điều hướng thủ công hội thoại cho một Agent. |
| **PATCH** | `/api/v1/conversations/{id}/status` | Cập nhật trạng thái hội thoại (Ví dụ: Đóng hội thoại). |
| **PATCH** | `/api/v1/agents/{id}/status` | Cập nhật trạng thái hoạt động của Agent (ONLINE/OFFLINE). |

---

### 2. QUY TẮC CHUẨN HÓA (STANDARDS & RULES)

#### 2.1. Phân trang (Pagination), Lọc (Filtering) & Sắp xếp (Sorting)
* **Pagination:** Sử dụng Query Parameters `page` (mặc định 1) và `limit` (mặc định 20, max 100). Đối với Messages, có thể dùng `cursor` để tối ưu tải lịch sử cuộn ngược.
* **Filtering:** Truyền dưới dạng Query params. VD: `?status=OPEN&channel_platform=FACEBOOK`.
* **Sorting:** Sử dụng tham số `sort`. VD: `?sort=-last_activity_at` (Dấu trừ `-` thể hiện DESC, không có dấu là ASC).

#### 2.2. Mã lỗi tiêu chuẩn (Error Codes)
* **200 OK / 201 Created:** Thành công.
* **400 Bad Request:** Lỗi dữ liệu đầu vào (Validation Error).
* **401 Unauthorized:** Thiếu token hoặc token hết hạn.
* **403 Forbidden:** Không có quyền truy cập resource (VD: Agent xem chat của người khác mà không có quyền).
* **404 Not Found:** Không tìm thấy tài nguyên (Conversation/Message ID không tồn tại).
* **429 Too Many Requests:** Vượt quá Rate Limit.
* **500 Internal Server Error:** Lỗi hệ thống Backend.

**Cấu trúc Response lỗi (Error DTO):**
```json
{
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Dữ liệu đầu vào không hợp lệ",
    "details": [
      {
        "field": "content_text",
        "issue": "Không được để trống khi không có file đính kèm."
      }
    ]
  }
}
```

---

### 3. OPENAPI 3.0 SPECIFICATION (YAML)

Dưới đây là đặc tả OpenAPI 3.0 định nghĩa chi tiết Request DTO, Response DTO, và Validation rules cho hệ thống.

```yaml
openapi: 3.0.3
info:
  title: Omnichannel Chat Management API
  version: 1.0.0
  description: RESTful API spec cho Hệ thống OCM (Core Modules).
servers:
  - url: https://api.ocm.com/api/v1
    description: Production Server

paths:
  /conversations:
    get:
      summary: Lấy danh sách hội thoại
      tags:
        - Conversations
      parameters:
        - name: page
          in: query
          schema:
            type: integer
            default: 1
        - name: limit
          in: query
          schema:
            type: integer
            default: 20
        - name: status
          in: query
          schema:
            type: string
            enum: [UNASSIGNED, OPEN, CLOSED]
        - name: sort
          in: query
          description: "Mặc định: -last_activity_at"
          schema:
            type: string
      responses:
        '200':
          description: Danh sách hội thoại phân trang
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PaginatedConversations'

  /conversations/{id}/messages:
    get:
      summary: Lấy lịch sử tin nhắn của hội thoại
      tags:
        - Messages
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
        - name: page
          in: query
          schema:
            type: integer
            default: 1
        - name: limit
          in: query
          schema:
            type: integer
            default: 50
    post:
      summary: Gửi tin nhắn mới vào hội thoại
      tags:
        - Messages
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/SendMessageRequest'
      responses:
        '201':
          description: Đã gửi tin nhắn
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Message'
        '400':
          description: Validation Error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'

  /agents/{id}/status:
    patch:
      summary: Cập nhật trạng thái Agent
      tags:
        - Agents
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: integer
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required:
                - status
              properties:
                status:
                  type: string
                  enum: [ONLINE, BUSY, OFFLINE]
      responses:
        '200':
          description: Cập nhật thành công

components:
  schemas:
    # DTO: Hội thoại
    Conversation:
      type: object
      properties:
        id:
          type: string
          format: uuid
        channel_identity_id:
          type: string
          format: uuid
        assigned_agent_id:
          type: integer
          nullable: true
        status:
          type: string
          enum: [UNASSIGNED, OPEN, CLOSED]
        is_sla_breached:
          type: boolean
        last_activity_at:
          type: string
          format: date-time
        created_at:
          type: string
          format: date-time

    # DTO: Tin nhắn
    Message:
      type: object
      properties:
        id:
          type: string
          format: uuid
        conversation_id:
          type: string
          format: uuid
        sender_type:
          type: string
          enum: [CUSTOMER, AGENT, SYSTEM]
        sender_id:
          type: string
        content_text:
          type: string
        content_attachments:
          type: array
          items:
            type: string
            format: uri
        status:
          type: string
          enum: [SENT, DELIVERED, READ, FAILED]
        sent_at:
          type: string
          format: date-time

    # DTO: Request Gửi tin nhắn
    SendMessageRequest:
      type: object
      properties:
        content_text:
          type: string
          maxLength: 2000
        content_attachments:
          type: array
          maxItems: 5
          items:
            type: string
            format: uri
      # Validation: Ít nhất phải có text hoặc attachment
      anyOf:
        - required: [content_text]
        - required: [content_attachments]

    # Wrapper Phân trang
    PaginatedConversations:
      type: object
      properties:
        data:
          type: array
          items:
            $ref: '#/components/schemas/Conversation'
        meta:
          type: object
          properties:
            current_page:
              type: integer
            total_pages:
              type: integer
            total_items:
              type: integer

    # DTO: Error
    ErrorResponse:
      type: object
      properties:
        error:
          type: object
          properties:
            code:
              type: string
            message:
              type: string
            details:
              type: array
              items:
                type: object
                properties:
                  field:
                    type: string
                  issue:
                    type: string
```
