# Yêu Cầu Frontend - OmniChat

Tài liệu này tổng hợp theo trạng thái backend hiện tại của repo `omnichat`, phục vụ bắt đầu xây dựng frontend bằng React + Vite. Phạm vi chính hiện đã expose cho UI gồm: hội thoại, tin nhắn, trạng thái agent, gateway, WebSocket realtime và luồng Facebook Messenger đã tích hợp qua webhook.

## 1. TỔNG QUAN HỆ THỐNG & ENTITY CHÍNH

### 1.1 Kiến trúc module

| Module | Port trực tiếp | Qua Gateway | Vai trò |
| --- | ---: | --- | --- |
| API Gateway | `8080` | `http://localhost:8080` | Entry point cho frontend, route API sang service nội bộ |
| Conversation Service | `8081` | `/api/v1/conversations/**` | Quản lý hội thoại, message, gửi reply của agent |
| Routing Service | `8082` | `/api/v1/agents/**` | Quản lý trạng thái agent, routing profile |
| Customer Service | `8083` | Chưa expose REST | Lưu customer profile và channel identity |
| Integration Service | `8084` | `/webhook/fb/**`, `/webhook/zalo/**` | Nhận webhook Facebook/Zalo, gửi tin ra Facebook Send API hoặc Zalo OA API |
| WebSocket Service | `8085` | `/ws/**` | STOMP realtime cho inbox |

Frontend nên gọi qua Gateway:

```ts
export const API_BASE_URL = "http://localhost:8080";
export const WS_URL = "http://localhost:8080/ws";
```

### 1.2 Entities/Models cốt lõi

| Entity | Service | Field chính | Ghi chú quan hệ |
| --- | --- | --- | --- |
| `Agent` | routing-service | `id`, `fullName`, `email`, `role`, `createdAt`, `updatedAt` | 1-1 với `AgentRoutingProfile` |
| `AgentRoutingProfile` | routing-service | `agentId`, `status`, `currentWorkload`, `maxCapacity`, `updatedAt` | FK tới `Agent`; dùng để chọn agent nhận hội thoại |
| `ChannelConnection` | integration-service | `id`, `platform`, `pageName`, `accessToken`, `refreshToken`, `status`, `expiryDate` | Đại diện Page/OA/shop đã kết nối; hiện chưa có API quản trị |
| `Conversation` | conversation-service | `id`, `channelIdentityId`, `channelConnectionId`, `assignedAgentId`, `status`, `lastActivityAt`, `isSlABreached` | 1-n với `Message`; `channelConnectionId` trỏ tới Page/channel |
| `Message` | conversation-service | `id`, `conversationId`, `senderType`, `senderId`, `contentText`, `contentAttachments`, `status`, `sentAt` | Thuộc một `Conversation`; `id` có thể là Facebook MID hoặc UUID |
| `Customer` | customer-service | `id`, `fullName`, `phoneNumber`, `address`, `createdAt`, `updatedAt` | 1-n với `ChannelIdentity`; chưa expose REST |
| `ChannelIdentity` | customer-service | `id`, `customerId`, `platform`, `externalUserId`, `createdAt` | Unique theo `(platform, externalUserId)` |

### 1.3 Enum quan trọng

```ts
type Platform = "FACEBOOK" | "ZALO" | "SHOPEE" | "TIKTOK";
type ChannelConnectionStatus = "CONNECTED" | "DISCONNECTED" | "ERROR";
type ConversationStatus = "UNASSIGNED" | "OPEN" | "CLOSED";
type SenderType = "CUSTOMER" | "AGENT" | "SYSTEM";
type MessageStatus = "SENT" | "DELIVERED" | "READ" | "FAILED";
type AgentRole = "ADMIN" | "SUPERVISOR" | "AGENT";
type AgentStatus = "ONLINE" | "BUSY" | "OFFLINE";
```

## 2. PHÂN TÍCH CHỨC NĂNG & DANH SÁCH API

### 2.1 Gateway & cấu hình gọi API

| Tên chức năng | Endpoint & Method | Payload/Body | Response / Lỗi thường gặp |
| --- | --- | --- | --- |
| Health check gateway | `GET /actuator/health` | Không có | `{"status":"UP"}` |
| Proxy conversation APIs | `/api/v1/conversations/**` | Theo từng API Conversation | Trả response từ conversation-service |
| Proxy agent APIs | `/api/v1/agents/**` | Theo từng API Routing | Trả response từ routing-service |
| Proxy Facebook webhook | `/webhook/fb/**` | Chỉ Meta gọi, UI không dùng | Trả response từ integration-service |
| Proxy Zalo webhook | `/webhook/zalo/**` | Chỉ Zalo gọi, UI không dùng | Trả response từ integration-service |
| Proxy WebSocket | `/ws/**` | STOMP/SockJS handshake | Kết nối tới websocket-service |

Hiện Gateway đã permit `/api/v1/**`, `/ws/**`, `/webhook/**` để frontend dev chưa cần JWT.

### 2.2 Conversation / Inbox Module

#### TypeScript models đề xuất

```ts
export interface PageMeta {
  currentPage: number;
  totalPages: number;
  totalItems: number;
}

export interface PaginatedResponse<T> {
  data: T[];
  meta: PageMeta;
}

export interface Conversation {
  id: string;
  channelIdentityId: string;
  channelConnectionId: number;
  assignedAgentId: number | null;
  status: "UNASSIGNED" | "OPEN" | "CLOSED";
  isSlABreached: boolean;
  lastActivityAt: string;
  createdAt: string;
}

export interface Message {
  id: string;
  conversationId: string;
  senderType: "CUSTOMER" | "AGENT" | "SYSTEM";
  senderId: string | null;
  contentText: string | null;
  contentAttachments: string[] | null;
  status: "SENT" | "DELIVERED" | "READ" | "FAILED";
  sentAt: string;
}
```

| Tên chức năng | Endpoint & HTTP Method | Payload/Body cần gửi | Dữ liệu Response / Lỗi thường gặp |
| --- | --- | --- | --- |
| Lấy danh sách hội thoại | `GET /api/v1/conversations` | Query: `page?: number`, `limit?: number`, `status?: UNASSIGNED\|OPEN\|CLOSED`, `sort?: string`. Default: `page=1`, `limit=20`, `sort=-last_activity_at` | `PaginatedResponse<Conversation>` |
| Lấy tin nhắn của hội thoại | `GET /api/v1/conversations/{id}/messages` | Path: `id`. Query: `page?: number`, `limit?: number`. Default: `page=1`, `limit=50` | `PaginatedResponse<Message>`. `404 NOT_FOUND` nếu conversation không tồn tại tùy service lookup |
| Agent gửi tin nhắn | `POST /api/v1/conversations/{id}/messages` | Header: `X-Agent-Id`. Body: `{ contentText?: string; contentAttachments?: string[] }`. Bắt buộc có ít nhất một field | `201 Message`. `400 VALIDATION_FAILED`, `404 NOT_FOUND` |
| Gán/chuyển hội thoại | `PATCH /api/v1/conversations/{id}/assign` | Header: `X-Agent-Id`. Body: `{ targetAgentId: number; reason?: string }` | `200 Conversation`. `400 VALIDATION_FAILED`, `404 NOT_FOUND`, `409 INVALID_STATE` |

Ví dụ gửi message:

```bash
curl -X POST "http://localhost:8080/api/v1/conversations/{conversationId}/messages" \
  -H "Content-Type: application/json" \
  -H "X-Agent-Id: 1" \
  -d '{"contentText":"Xin chao tu OmniChat"}'
```

Lưu ý quan trọng: Backend DTO hiện dùng camelCase (`contentText`, `contentAttachments`, `targetAgentId`), không dùng snake_case.

### 2.3 Routing / Agent Module

#### TypeScript models đề xuất

```ts
export interface AgentStatusResponse {
  agentId: number;
  fullName: string;
  email: string;
  role: "ADMIN" | "SUPERVISOR" | "AGENT";
  status: "ONLINE" | "BUSY" | "OFFLINE";
  currentWorkload: number;
  maxCapacity: number;
  updatedAt: string;
}
```

| Tên chức năng | Endpoint & HTTP Method | Payload/Body cần gửi | Dữ liệu Response / Lỗi thường gặp |
| --- | --- | --- | --- |
| Lấy trạng thái agent | `GET /api/v1/agents/{id}/status` | Path: `id: number` | `AgentStatusResponse`. `404 NOT_FOUND` |
| Cập nhật trạng thái agent | `PATCH /api/v1/agents/{id}/status` | Body: `{ status: "ONLINE" \| "BUSY" \| "OFFLINE" }` | `AgentStatusResponse`. `400 VALIDATION_FAILED`, `404 NOT_FOUND` |

Seed data hiện có:

| Agent ID | Email | Role | Status mặc định |
| ---: | --- | --- | --- |
| 1 | `admin@omnichat.com` | `ADMIN` | `ONLINE` |
| 2 | `agent1@omnichat.com` | `AGENT` | `OFFLINE` |
| 3 | `agent2@omnichat.com` | `AGENT` | `OFFLINE` |

### 2.4 WebSocket / Realtime Module

Kết nối STOMP qua SockJS:

```ts
const socketUrl = "http://localhost:8080/ws?agentId=1";
```

Subscribe:

```ts
"/topic/conversations"
"/user/queue/conversations"
```

Payload chuẩn:

```ts
export type WebSocketConversationEventType =
  | "NEW_CONVERSATION"
  | "NEW_MESSAGE_UNASSIGNED"
  | "NEW_MESSAGE"
  | "CONVERSATION_ASSIGNED"
  | "CONVERSATION_TRANSFERRED_OUT"
  | "CONVERSATION_TRANSFERRED_IN";

export interface WebSocketEnvelope<T = Record<string, unknown>> {
  type: WebSocketConversationEventType;
  data: T;
  timestamp: number;
}
```

| Tên chức năng | Endpoint & Method | Payload/Body cần gửi | Response / Lỗi thường gặp |
| --- | --- | --- | --- |
| Kết nối realtime | `GET /ws?agentId={agentId}` qua STOMP/SockJS | Query bắt buộc: `agentId`. Có thể dùng header `X-Agent-Id` nếu client hỗ trợ | Handshake thành công nếu có agentId |
| Nhận broadcast inbox | Subscribe `/topic/conversations` | Không có | Nhận event new conversation/new message |
| Nhận event cá nhân | Subscribe `/user/queue/conversations` | Không có | Nhận event assign/transfer riêng theo agent |

Khuyến nghị frontend: khi nhận event WebSocket, không mutate phức tạp tại client. Hãy invalidate/refetch:

- `GET /api/v1/conversations`
- `GET /api/v1/conversations/{id}/messages` nếu đang mở hội thoại đó

Known issue backend hiện tại: `websocket-service` đang log lỗi deserialize `ConsumerRecord` khi consume Kafka event. REST qua Gateway đã hoạt động; realtime cần fix consumer tương tự các consumer đã sửa ở `conversation-service`/`integration-service` trước khi dùng production-like.

### 2.5 Integration / Facebook & Zalo Module

Các API này chủ yếu dành cho Meta, không phải UI thông thường.

| Tên chức năng | Endpoint & HTTP Method | Payload/Body cần gửi | Dữ liệu Response / Lỗi thường gặp |
| --- | --- | --- | --- |
| Meta verify webhook | `GET /webhook/fb` | Query: `hub.mode`, `hub.verify_token`, `hub.challenge` | `200` trả về challenge nếu token đúng; `403 Verification failed` nếu sai |
| Nhận webhook event | `POST /webhook/fb` | Header: `X-Hub-Signature-256`. Body: payload Facebook webhook | `200 EVENT_RECEIVED`; `403 Invalid signature`; `500 Error processing event` |
| Zalo health check | `GET /webhook/zalo` | Không có | `200 ZALO_WEBHOOK_READY` |
| Nhận Zalo webhook theo path | `POST /webhook/zalo/{channelConnectionId}` | Path: `channelConnectionId` trỏ tới bản ghi `channel_connections` platform `ZALO`, status `CONNECTED`. Body: payload webhook Zalo OA | `200 EVENT_RECEIVED`; `400` nếu channel id sai; `500 Error processing event` |
| Nhận Zalo webhook theo query | `POST /webhook/zalo?channelConnectionId={id}` | Query: `channelConnectionId`. Body giống trên | `200 EVENT_RECEIVED`; `400` nếu channel id sai; `500 Error processing event` |

UI không cần gọi các API này. Luồng đã kiểm chứng/thiết kế: Facebook Messenger hoặc Zalo OA -> Integration webhook -> Kafka -> Conversation -> UI đọc qua API. Với Zalo, cần seed `channel_connections` trước, ví dụ `platform='ZALO'`, `access_token='<OA_ACCESS_TOKEN>'`, `status='CONNECTED'`, sau đó cấu hình webhook Zalo trỏ tới `/webhook/zalo/{channelConnectionId}`.

### 2.6 Customer / CRM Module

Backend hiện có entity và DB, nhưng chưa có REST controller.

| Chức năng mong muốn | Trạng thái backend | Ghi chú frontend |
| --- | --- | --- |
| Xem hồ sơ customer | Chưa có API | UI có thể để placeholder panel |
| Cập nhật tên/SĐT/địa chỉ | Chưa có API | Cần bổ sung backend trước |
| Merge customer profile | Có service nội bộ/test, chưa expose REST | Chưa nên build flow thật |
| Xem channel identities | Chưa có API | Có thể hiển thị `channelIdentityId` từ `Conversation` tạm thời |

### 2.7 Channel Management Module

Backend hiện có `ChannelConnection` và bảng `channel_connections`, nhưng chưa có REST controller quản trị kết nối kênh.

| Chức năng mong muốn | Trạng thái backend | Ghi chú frontend |
| --- | --- | --- |
| Danh sách kênh đã kết nối | Chưa có API | Có thể mock hoặc chờ backend |
| Kết nối Facebook Page/OA OAuth | Chưa có API | Hiện token được insert DB thủ công, gồm cả Zalo OA access token |
| Ngắt kết nối kênh | Chưa có API | Chưa build chức năng thật |
| Hiển thị trạng thái token | Chưa có API | Entity có `status`, `expiryDate` |

## 3. YÊU CẦU LOGIC & VALIDATION CHO FRONTEND

### 3.1 Xác thực / phân quyền

- Hiện Gateway đã permit `/api/v1/**`, `/ws/**`, `/webhook/**`, nên frontend dev chưa cần gửi JWT.
- Backend có sẵn JWT filter trong Gateway, nhưng chưa có Auth service/login API thật trong repo.
- Các API thao tác agent đang dùng header dev:

```http
X-Agent-Id: 1
```

- UI nên thiết kế sẵn `currentAgentId` trong app state để sau này thay bằng JWT claim.
- Role hiện có: `ADMIN`, `SUPERVISOR`, `AGENT`, nhưng API chưa enforce phân quyền cụ thể ở controller.

Khuyến nghị frontend giai đoạn 1:

- Cho phép chọn agent giả lập ở topbar.
- Lưu `agentId` vào localStorage.
- Tự động gửi `X-Agent-Id` trong Axios interceptor.

### 3.2 Validation form

| Form | Field | Rule frontend | Mapping backend |
| --- | --- | --- | --- |
| Send message | `contentText` | Optional nhưng bắt buộc nếu không có attachment; max 2000 ký tự; trim blank | `SendMessageRequest.contentText` |
| Send message | `contentAttachments` | Optional; max 5 URL/string | `SendMessageRequest.contentAttachments` |
| Assign conversation | `targetAgentId` | Required, number > 0 | `TransferRequest.targetAgentId` |
| Assign conversation | `reason` | Optional string | `TransferRequest.reason` |
| Agent status | `status` | Required, one of `ONLINE`, `BUSY`, `OFFLINE` | `AgentStatusRequest.status` |
| Conversation filters | `status` | Optional, one of `UNASSIGNED`, `OPEN`, `CLOSED` | Query param |
| Pagination | `page`, `limit` | `page >= 1`, `limit <= 100` nên giới hạn ở UI | Query param |

### 3.3 Quy tắc nghiệp vụ đặc biệt

- Hội thoại mới từ Facebook vào sẽ có `status = UNASSIGNED`.
- Khi gán hội thoại cho agent, backend chuyển `assignedAgentId` và thường đưa conversation sang `OPEN`.
- Agent gửi message qua `POST /messages`; integration-service sẽ gửi ra Facebook hoặc Zalo nếu `channelConnectionId` khớp bản ghi `channel_connections`.
- Message từ khách hàng có `senderType = CUSTOMER`; message agent có `senderType = AGENT`.
- Frontend không nên tự set `status` của message sau khi gửi; backend trả response `SENT`, integration publish delivery status nội bộ.
- Conversation list nên sort mới nhất trước bằng `sort=-last_activity_at`.
- Khi nhận WebSocket event, refetch thay vì tự suy luận nhiều vì payload event không luôn đủ toàn bộ conversation/message.
- Facebook gửi reply chỉ thành công khi Page Access Token còn hợp lệ và user đã từng nhắn Page.

### 3.4 Error handling chuẩn

Backend thường trả lỗi dạng:

```ts
export interface ApiErrorResponse {
  error: {
    code: string;
    message: string;
    details?: Array<{
      field: string;
      issue: string;
    }>;
  };
}
```

Các mã lỗi cần handle ở UI:

| HTTP | Code | Cách hiển thị đề xuất |
| ---: | --- | --- |
| 400 | `VALIDATION_FAILED` | Show inline form error hoặc toast |
| 401 | `UNAUTHORIZED` | Chưa dùng trong dev; sau này redirect login |
| 404 | `NOT_FOUND` | Show empty state hoặc toast "Không tìm thấy dữ liệu" |
| 409 | `INVALID_STATE` | Toast cảnh báo trạng thái hội thoại không hợp lệ |
| 500 | tùy service | Toast lỗi hệ thống, cho phép retry |

## 4. GỢI Ý DANH SÁCH PAGES VÀ COMPONENTS

### 4.1 Routing pages

Đề xuất dùng React Router:

```txt
/                         -> redirect /inbox
/inbox                    -> InboxPage
/inbox/:conversationId    -> InboxPage với conversation đang chọn
/agents                   -> AgentStatusPage hoặc AgentPanel dev
/channels                 -> ChannelConnectionsPage placeholder
/customers/:customerId    -> CustomerProfilePage placeholder
/settings                 -> SettingsPage
```

Giai đoạn đầu, tập trung làm `/inbox` trước. Đây là màn hình chính của hệ thống: gom toàn bộ hội thoại/tin nhắn khách hàng từ Facebook, Zalo và các kênh sau này về một nơi để agent dễ quản lý.

### 4.2 Cấu trúc thư mục đề xuất

```txt
src/
  app/
    App.tsx
    router.tsx
    providers.tsx
  config/
    env.ts
  api/
    httpClient.ts
    conversations.api.ts
    agents.api.ts
  types/
    api.ts
    conversation.ts
    agent.ts
    websocket.ts
  hooks/
    useConversations.ts
    useConversationMessages.ts
    useSendMessage.ts
    useAssignConversation.ts
    useAgentStatus.ts
    useConversationSocket.ts
  layouts/
    AppShell.tsx
  pages/
    InboxPage.tsx
    AgentStatusPage.tsx
    ChannelsPage.tsx
    CustomerProfilePage.tsx
    SettingsPage.tsx
  features/
    inbox/
      components/
        ConversationList.tsx
        ConversationListItem.tsx
        ConversationFilters.tsx
        ChatHeader.tsx
        MessageList.tsx
        MessageBubble.tsx
        MessageComposer.tsx
        AssignConversationDialog.tsx
        EmptyConversationState.tsx
      utils/
        conversationFormatters.ts
    agents/
      components/
        AgentStatusSelect.tsx
        AgentSwitcher.tsx
    realtime/
      stompClient.ts
  components/
    ui/
      Button.tsx
      Input.tsx
      Select.tsx
      Dialog.tsx
      Badge.tsx
      Spinner.tsx
      Toast.tsx
```

### 4.3 UI components chính

| Component | Trách nhiệm | API/hook liên quan |
| --- | --- | --- |
| `AppShell` | Layout tổng, sidebar/nav/topbar | Không trực tiếp |
| `AgentSwitcher` | Chọn agent dev và lưu `agentId` | localStorage |
| `AgentStatusSelect` | Online/Busy/Offline | `GET/PATCH /api/v1/agents/{id}/status` |
| `ConversationFilters` | Filter status và sort | `useConversations` |
| `ConversationList` | Danh sách hội thoại, pagination/infinite scroll | `GET /api/v1/conversations` |
| `ConversationListItem` | Preview từng hội thoại, badge status/SLA | `Conversation` |
| `ChatHeader` | Thông tin hội thoại, assign button | `PATCH /assign` |
| `MessageList` | Render messages, auto scroll | `GET /messages` |
| `MessageBubble` | Render theo `senderType` | `Message` |
| `MessageComposer` | Text input, send button, validation max 2000 | `POST /messages` |
| `AssignConversationDialog` | Chọn target agent, nhập reason | `PATCH /assign` |
| `RealtimeListener` hoặc `useConversationSocket` | Subscribe STOMP, invalidate data | `/ws?agentId=` |

### 4.4 Thiết kế màn hình Inbox đa kênh

Mục tiêu UX: agent mở `/inbox` là thấy ngay danh sách toàn bộ tin nhắn/hội thoại mới từ các kênh. Khi bấm một dòng hội thoại ở cột phải, vùng nội dung chính hiển thị lịch sử trò chuyện giữa nhân viên và khách hàng đó, kèm ô nhập để phản hồi.

#### Layout desktop đề xuất

```txt
┌──────────────────────────────────────────────────────────────────────────────┐
│ Topbar: OmniChat | AgentSwitcher | AgentStatusSelect                         │
├───────────────┬──────────────────────────────────────────┬───────────────────┤
│ Sidebar nav   │ Chat panel                               │ Conversation list │
│ - Inbox       │ - ChatHeader                             │ - Search          │
│ - Channels    │ - MessageList                            │ - Filters         │
│ - Agents      │ - MessageComposer                        │ - Items newest    │
│ - Settings    │                                          │   first           │
└───────────────┴──────────────────────────────────────────┴───────────────────┘
```

Yêu cầu cụ thể:

- Cột phải là `ConversationList`, hiển thị toàn bộ hội thoại đến từ nhiều kênh. Mỗi item cần có tên/định danh khách tạm thời, badge kênh (`FACEBOOK`, `ZALO`, ...), trạng thái (`UNASSIGNED`, `OPEN`, `CLOSED`), thời gian hoạt động cuối và preview nếu có dữ liệu.
- Khi người dùng bấm vào một hội thoại ở cột phải, URL đổi sang `/inbox/{conversationId}` và `ChatPanel` gọi `GET /api/v1/conversations/{id}/messages` để hiển thị lịch sử chat.
- Tin nhắn trong `MessageList` phân biệt rõ `CUSTOMER`, `AGENT`, `SYSTEM`; bubble của khách nằm một phía, bubble của agent nằm phía còn lại, system message nằm giữa.
- `ChatHeader` hiển thị kênh, `channelIdentityId`, `channelConnectionId`, trạng thái hội thoại và agent đang được assign. Có nút gán/chuyển hội thoại nếu cần.
- `MessageComposer` luôn nằm cuối vùng chat, cho phép nhập text, enter để gửi hoặc nút gửi. Disable composer nếu chưa chọn hội thoại hoặc hội thoại `CLOSED`.
- Khi gửi tin, gọi `POST /api/v1/conversations/{id}/messages` với header `X-Agent-Id`; sau khi thành công refetch messages và conversations.
- Khi chưa chọn hội thoại, vùng chat hiển thị empty state gọn: chọn một hội thoại ở cột phải để xem lịch sử.

#### Layout responsive

- Desktop/tablet rộng: dùng 3 vùng như layout trên; cột phải rộng khoảng `320px - 380px`, chat panel chiếm phần còn lại.
- Mobile: chuyển thành 2 bước. Mặc định hiển thị `ConversationList`; khi chọn hội thoại thì chuyển sang `ChatPanel` toàn màn hình, có nút quay lại danh sách.
- Không để message composer, danh sách hội thoại hoặc bubble bị overlap khi bàn phím/viewport nhỏ. Message list phải scroll độc lập trong vùng chat.

#### Quy tắc hiển thị kênh

Backend hiện trả `channelIdentityId`; với event mới có thể có dạng:

```txt
FACEBOOK:27263207133328724
ZALO:4356639876691778517
```

Frontend nên parse prefix trước dấu `:` để suy ra platform tạm thời khi chưa có API channel management:

```ts
export function parseChannelIdentity(channelIdentityId: string) {
  const [maybePlatform, ...rest] = channelIdentityId.split(":");
  const knownPlatforms = ["FACEBOOK", "ZALO", "SHOPEE", "TIKTOK"];
  if (knownPlatforms.includes(maybePlatform)) {
    return {
      platform: maybePlatform as Platform,
      externalUserId: rest.join(":"),
    };
  }
  return {
    platform: "FACEBOOK" as Platform,
    externalUserId: channelIdentityId,
  };
}
```

Badge màu gợi ý:

| Platform | Label | Màu gợi ý |
| --- | --- | --- |
| `FACEBOOK` | Facebook | Blue |
| `ZALO` | Zalo | Cyan |
| `SHOPEE` | Shopee | Orange |
| `TIKTOK` | TikTok | Neutral/Dark |

#### Data flow cho click hội thoại

1. `InboxPage` load `GET /api/v1/conversations?sort=-last_activity_at&limit=20`.
2. User click item ở cột phải.
3. App set selected conversation qua route `/inbox/{conversationId}`.
4. `ChatPanel` load `GET /api/v1/conversations/{conversationId}/messages?limit=50`.
5. `MessageList` render lịch sử theo `sentAt`; nếu API trả newest-first thì frontend đảo thứ tự khi render để tin cũ nằm trên, tin mới nằm dưới.
6. Khi agent gửi tin thành công, invalidate/refetch:
   - `["conversationMessages", conversationId]`
   - `["conversations"]`

#### Acceptance criteria cho `/inbox`

- Hiển thị được danh sách hội thoại từ `GET /api/v1/conversations`.
- Mỗi hội thoại có badge platform; Facebook/Zalo không bị trộn nhãn.
- Click hội thoại ở cột phải mở lịch sử chat ở vùng chính.
- Hiển thị đúng bubble cho khách hàng, agent và system.
- Gửi được tin nhắn mới từ agent, request body dùng camelCase `contentText`.
- Sau khi gửi, tin mới xuất hiện trong lịch sử và hội thoại được đẩy lên đầu danh sách sau refetch.
- Có trạng thái loading, empty, error và retry cho cả conversation list và message list.
- UI dùng Gateway `http://localhost:8080`, không gọi trực tiếp service port.

### 4.5 State management đề xuất

Nhẹ và hợp lý cho giai đoạn này:

- TanStack Query cho REST cache/refetch.
- Zustand hoặc React Context cho `currentAgentId`, UI state.
- STOMP client trong custom hook, khi có event thì `queryClient.invalidateQueries`.

Query keys gợi ý:

```ts
["conversations", { page, limit, status, sort }]
["conversationMessages", conversationId, { page, limit }]
["agentStatus", agentId]
```

### 4.6 API client mẫu

```ts
import axios from "axios";

export const http = axios.create({
  baseURL: "http://localhost:8080",
  headers: {
    "Content-Type": "application/json",
  },
});

http.interceptors.request.use((config) => {
  const agentId = localStorage.getItem("agentId") ?? "1";
  config.headers["X-Agent-Id"] = agentId;
  return config;
});
```

### 4.7 Prompt triển khai giao diện

Bạn có thể dùng prompt sau để bắt đầu implement frontend:

```txt
Bạn là senior frontend engineer. Hãy xây dựng giao diện React + Vite cho OmniChat dựa trên docs/FRONTEND_REQUIREMENTS.md.

Mục tiêu giai đoạn 1: làm trang /inbox để quản lý tin nhắn đa kênh từ Facebook, Zalo và các kênh sau này về một nơi.

Yêu cầu chính:
- Dùng API Gateway tại http://localhost:8080.
- Tạo AppShell có topbar chọn agent dev và trạng thái agent.
- Trang /inbox có cột phải hiển thị danh sách toàn bộ hội thoại từ GET /api/v1/conversations?sort=-last_activity_at.
- Mỗi item hội thoại hiển thị badge platform, status, channelIdentityId, thời gian lastActivityAt.
- Khi click một hội thoại ở cột phải, route sang /inbox/:conversationId và vùng chat chính hiển thị lịch sử tin nhắn từ GET /api/v1/conversations/{id}/messages.
- Message bubble phải phân biệt CUSTOMER, AGENT, SYSTEM.
- Có MessageComposer để agent gửi tin qua POST /api/v1/conversations/{id}/messages với header X-Agent-Id và body { "contentText": "..." }.
- Sau khi gửi tin, refetch danh sách hội thoại và lịch sử tin nhắn.
- Parse platform từ channelIdentityId dạng FACEBOOK:<id> hoặc ZALO:<id>; nếu không có prefix thì tạm xem là FACEBOOK.
- Có loading, empty, error, retry state. Mobile phải dùng layout danh sách -> chat detail có nút quay lại.
- Không build login thật; dùng AgentSwitcher lưu agentId vào localStorage.

Stack đề xuất: React, TypeScript, Vite, React Router, TanStack Query, Axios. Có thể dùng component/UI library sẵn nếu repo đã có; nếu chưa có thì tự tạo component tối giản, sạch, responsive.
```

### 4.8 Ưu tiên triển khai frontend

1. Setup Vite + React Router + TanStack Query + Axios.
2. Làm `AppShell`, `AgentSwitcher`, `AgentStatusSelect`.
3. Làm `InboxPage` với list hội thoại từ Gateway.
4. Làm `MessageList` và `MessageComposer`.
5. Làm assign dialog.
6. Thêm realtime hook sau khi fix WebSocket consumer backend.
7. Bổ sung placeholder Channels/Customers để sẵn navigation.

## 5. BACKEND CAVEATS CHO FRONTEND

- REST qua Gateway đã test OK với `GET /conversations`, `GET /messages`, `GET /agents/{id}/status`, `POST /messages`.
- Auth chưa hoàn thiện; không build login thật nếu chưa bổ sung Auth service.
- Customer và Channel Management chưa có API REST; chỉ nên làm placeholder/mock.
- WebSocket route qua Gateway đã cấu hình, nhưng `websocket-service` consumer hiện cần fix lỗi deserialize Kafka `ConsumerRecord` trước khi realtime hoạt động ổn định.
- Backend response hiện dùng camelCase. Frontend không dùng snake_case khi gửi request body.
