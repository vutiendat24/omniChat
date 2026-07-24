# Yêu cầu chi tiết: M07 - Conversation & Inbox

## MOD-CONV-01: Tạo mới hội thoại (Create Conversation)

### 1. Mô tả nghiệp vụ đầy đủ
Luồng xử lý chính khi hệ thống tạo một phiên hội thoại mới:
1. **Tiếp nhận sự kiện:** Hệ thống nhận được một sự kiện `MessageReceivedEvent` (từ tin nhắn 1-1 qua M03) hoặc `LivestreamCommentEvent` (khi agent/khách hàng chủ động inbox từ 1 comment qua M05).
2. **Kiểm tra hội thoại tồn tại:** Hệ thống truy vấn CSDL để kiểm tra xem giữa `Customer ID` (hoặc `Channel Identity ID`) và `Page/Channel ID` hiện tại đã có một cuộc hội thoại (Conversation) nào đang ở trạng thái `OPEN` hoặc `PENDING` hay chưa.
3. **Quyết định tạo mới:**
   - Nếu đã có hội thoại `OPEN`/`PENDING`: Bỏ qua việc tạo mới hội thoại, chuyển sự kiện sang luồng *Lưu trữ & đồng bộ tin nhắn (MOD-CONV-03)*.
   - Nếu chưa có hoặc hội thoại gần nhất đã `CLOSED` / `RESOLVED`: Tiến hành tạo một bản ghi Conversation mới.
4. **Khởi tạo thông tin hội thoại:** Tạo bản ghi với trạng thái mặc định là `OPEN`, gán thẻ (nếu có auto-tagging), lưu thời gian bắt đầu (Created At) để làm mốc tính SLA.
5. **Gửi sự kiện phân bổ:** Phát một sự kiện `ConversationCreatedEvent` tới Message Broker để module Routing & Assignment (M09) nhận diện và tiến hành phân bổ cho Agent phù hợp.
6. **Lưu tin nhắn đầu tiên:** Kích hoạt chức năng *Lưu trữ & đồng bộ tin nhắn (MOD-CONV-03)* để đính kèm tin nhắn gốc vào hội thoại vừa tạo.

### 2. Input
- **Message Payload:** Chứa các thông tin cốt lõi gồm:
  - `channel_id` / `page_id`: ID của kênh nhận tin nhắn.
  - `customer_id` / `sender_id`: ID định danh khách hàng trên hệ thống (từ M06) hoặc trên kênh.
  - `message_type`: Loại tin nhắn (VD: `MESSAGE_1_1`, `PRIVATE_REPLY_FROM_COMMENT`).
  - `timestamp`: Thời gian gửi tin nhắn.
- **Nguồn:** Nhận thông qua Message Broker (Kafka/RabbitMQ) phát ra từ module `M03 - Channel Integration` hoặc `M05 - Livestream Chat Aggregator`.

### 3. Output
- **Kết quả trả về:** Một bản ghi `Conversation` được lưu thành công vào Database với một `conversation_id` duy nhất.
- **Nơi lưu / Gửi tiếp:** 
  - Lưu vào Database (bảng `conversations`).
  - Xuất sự kiện `ConversationCreatedEvent` (chứa `conversation_id`, `tenant_id`, `channel_id`) đẩy lên Message Broker cho M09 (Routing) và M10 (Realtime Delivery) xử lý tiếp.

### 4. Business Rule / Ràng buộc
- **Tính duy nhất của hội thoại mở:** Tại mọi thời điểm, giữa một `Customer` và một `Channel` (Page/OA) chỉ có tối đa **MỘT** hội thoại ở trạng thái `OPEN` hoặc `PENDING`.
- **Giới hạn thời gian (Session Window):** Nếu tin nhắn đến sau khi hội thoại cũ đã bị đóng (`CLOSED`), bắt buộc phải tạo hội thoại mới (để tính toán lại SLA và báo cáo chính xác).

### 5. Edge Case cần xử lý
- **Trùng lặp sự kiện (Duplicate Event):** Hệ thống Message Broker có thể gửi sự kiện đến 2 lần. Cần cơ chế Idempotency Key (dựa vào `message_id` của tin nhắn gốc) để đảm bảo không tạo 2 hội thoại trùng lặp trong cùng một tích tắc.
- **Race Condition:** Khách hàng spam nhiều tin nhắn cùng một lúc. Hệ thống cần sử dụng cơ chế khóa (Distributed Lock, ví dụ Redis Lock dựa trên `customer_id` + `channel_id`) để đảm bảo chỉ có 1 thread tạo hội thoại mới, các thread còn lại phải đi vào nhánh "hội thoại đã tồn tại".
- **Lỗi lưu DB:** Nếu DB bị lỗi khi tạo hội thoại, tin nhắn có nguy cơ bị rơi vào khoảng không. Cần cơ chế lưu trữ DLQ (Dead Letter Queue) trên Kafka để thử lại (Retry).

### 6. Acceptance Criteria
- **Given** khách hàng `C` nhắn tin vào kênh `K`.
- **And** hiện tại không có hội thoại nào giữa `C` và `K` đang ở trạng thái `OPEN`.
- **When** hệ thống nhận được sự kiện tin nhắn mới.
- **Then** một hội thoại mới mang trạng thái `OPEN` được tạo ra trong hệ thống.
- **And** sự kiện `ConversationCreatedEvent` được đẩy vào Message Broker.

- **Given** khách hàng `C` nhắn tin vào kênh `K`.
- **And** hiện tại ĐÃ có hội thoại `H` đang ở trạng thái `OPEN`.
- **When** hệ thống nhận được sự kiện tin nhắn mới.
- **Then** hệ thống KHÔNG tạo hội thoại mới.
- **And** tiếp tục dùng hội thoại `H` để lưu tin nhắn.

### 7. Chỉ số phi chức năng
- **Độ trễ tối đa (Latency):** Việc kiểm tra sự tồn tại và tạo mới hội thoại, cộng thêm phát event phải diễn ra dưới `< 50ms` để đảm bảo luồng tin nhắn realtime.
- **Throughput kỳ vọng:** Có thể xử lý đồng thời `200 - 500 requests/second` trong các đợt livestream bùng nổ mà không bị dính lỗi Race Condition.


## MOD-CONV-02: Cập nhật trạng thái hội thoại (Update Conversation Status)

### 1. Mô tả nghiệp vụ đầy đủ
Luồng xử lý khi trạng thái của một hội thoại (Conversation) bị thay đổi (Bởi Agent, Admin hoặc Hệ thống tự động). Hệ thống hỗ trợ các trạng thái cơ bản: OPEN (Đang mở/Đang chat), PENDING (Chờ xử lý/Đang tạm hoãn), RESOLVED (Đã giải quyết/Đóng), SPAM (Đánh dấu spam).
1. **Tiếp nhận yêu cầu cập nhật:** Hệ thống nhận API Request cập nhật trạng thái từ giao diện UI của Agent/Admin, hoặc từ một Cronjob (Ví dụ: tự động Resolve sau 24h không có tin nhắn mới).
2. **Kiểm tra quyền:** Xác thực người thực hiện có quyền thay đổi trạng thái hội thoại này hay không (Agent chỉ được đổi trạng thái hội thoại mà họ được phân công, Admin được đổi tất cả).
3. **Kiểm tra luồng trạng thái hợp lệ (State Machine):** 
   - Hội thoại đang RESOLVED nếu có tin nhắn mới từ khách hàng sẽ tự động chuyển về OPEN (hoặc tạo hội thoại mới tùy theo rule của MOD-CONV-01).
   - Không được chuyển từ SPAM về OPEN trừ khi là Admin thao tác.
4. **Thực thi cập nhật:** Cập nhật trường status của bản ghi Conversation trong CSDL. Lưu lại thời điểm đóng (closed_at) nếu trạng thái chuyển sang RESOLVED.
5. **Ghi nhận Audit Log:** Lưu lại lịch sử ai là người đổi trạng thái, từ trạng thái nào sang trạng thái nào để phục vụ tra cứu.
6. **Đẩy sự kiện:** Gửi sự kiện ConversationStatusUpdatedEvent vào Message Broker để các module khác biết (VD: M10 đẩy sự kiện xuống Frontend để UI Agent tự động đóng tab chat, M11 cập nhật báo cáo SLA).

### 2. Input
- **Dữ liệu đầu vào (REST API Request):**
  - conversation_id: ID của hội thoại.
  - status: Trạng thái mới muốn chuyển sang (OPEN, PENDING, RESOLVED, SPAM).
  - 
eason (Tùy chọn): Ghi chú lý do đóng hội thoại.
- **Nguồn kích hoạt:** Agent UI, Admin UI, hoặc Hệ thống (Auto-close cronjob).

### 3. Output
- **Kết quả trả về:** Dữ liệu chi tiết của hội thoại sau khi cập nhật thành công (HTTP 200 OK).
- **Nơi lưu:** CSDL của Module M07 (Cập nhật bảng Conversation và ghi thêm 1 dòng vào bảng Conversation_History).
- **Nơi gửi tiếp:** Gửi event vào Message Broker.

### 4. Business Rule / Ràng buộc
- **Cập nhật SLA:** Nếu hội thoại chuyển sang RESOLVED, hệ thống phải chốt thời gian Resolution Time (Thời gian tính từ lúc OPEN đến lúc RESOLVED), trừ đi khoảng thời gian nằm ở trạng thái PENDING hoặc ngoài giờ làm việc.
- **Xử lý Agent Capacity:** Khi hội thoại được chuyển sang RESOLVED hoặc SPAM, hệ thống phân bổ (M09 - Routing) phải giảm số lượng 'hội thoại đang xử lý (Active capacity)' của Agent đó xuống 1 đơn vị để dọn chỗ cho hội thoại mới.

### 5. Edge Case cần xử lý
- **Đóng hội thoại đồng thời:** 2 Agent cùng lúc thao tác Đóng hội thoại. -> **Xử lý:** Áp dụng Optimistic Locking (dùng trường ersion ở bảng Conversation). Agent nào gọi sau sẽ bị lỗi HTTP 409 Conflict.
- **Tự động đóng (Auto-resolve):** Khách hàng im lặng quá lâu (VD: 24h), Agent quên đóng. -> **Xử lý:** Job chạy ngầm hàng giờ sẽ quét các hội thoại OPEN không có tương tác > 24h và ép chuyển sang RESOLVED (kèm user thực hiện là SYSTEM).

### 6. Acceptance Criteria
- **Given** Một hội thoại đang ở trạng thái OPEN và được gán cho Agent A.
- **When** Agent A bấm nút 'Giải quyết' (Chuyển sang RESOLVED).
- **Then** Trạng thái hội thoại trong CSDL cập nhật thành RESOLVED.
- **And** Thời gian closed_at được lưu lại.
- **And** Sự kiện ConversationStatusUpdatedEvent được đẩy đi thành công.
- **And** Giao diện của Agent A tự động cập nhật và đóng cửa sổ chat.

- **Given** Agent B (không phụ trách hội thoại) cố tình gọi API đổi trạng thái hội thoại của Agent A.
- **When** API tiếp nhận yêu cầu.
- **Then** Hệ thống trả về lỗi HTTP 403 Forbidden.

### 7. Chỉ số phi chức năng
- **Toàn vẹn dữ liệu:** Trạng thái phải đồng nhất với hệ thống tính toán năng suất (M09). Giao dịch (Transaction) cập nhật DB và đẩy Event vào Kafka phải đảm bảo tính nhất quán (Sử dụng Outbox Pattern nếu cần).



## MOD-CONV-03: Lưu trữ & đồng bộ tin nhắn (Save & Sync Message)

### 1. Mô tả nghiệp vụ đầy đủ
Chức năng chịu trách nhiệm lưu trữ an toàn các tin nhắn của hội thoại (cả Inbound từ khách hàng và Outbound từ Agent hoặc Bot) vào cơ sở dữ liệu. Nó đóng vai trò là nguồn sự thật (Source of Truth) cho lịch sử chat.
1. **Tiếp nhận tin nhắn:** Hệ thống nhận một tin nhắn mới (Inbound từ M03/M05 qua Message Broker, hoặc Outbound từ Agent qua API gửi tin).
2. **Xác định hội thoại:** Liên kết tin nhắn với `conversation_id` đang `OPEN` tương ứng. Nếu không tìm thấy, hệ thống đẩy lại sự kiện để MOD-CONV-01 tạo hội thoại mới.
3. **Phân loại & Lưu trữ:**
   - Xác định loại tin nhắn (Text, Image, Video, File, System Event).
   - Lưu trữ vào bảng `Message`. Payload của tin nhắn được lưu dưới dạng JSON (để dễ dàng mở rộng định dạng như Carousel, Quick Replies, Card).
   - Cập nhật trường `last_message_at` và `last_message_preview` trên bảng `Conversation` để UI có thể hiển thị chính xác ngoài danh sách inbox.
4. **Đồng bộ Realtime:** Sau khi lưu thành công, hệ thống phát sinh sự kiện `MessageSavedEvent` đẩy vào Message Broker (Kafka) để M10 (Realtime Delivery) nhận và bắn xuống WebSocket cho các Agent đang phụ trách hội thoại.
5. **Xử lý Outbox Pattern:** Đảm bảo việc lưu vào Database và đẩy Message vào Kafka là một giao dịch đồng nhất (nhất quán dữ liệu). Nếu Kafka sập, dữ liệu lưu ở DB vẫn đảm bảo được gửi đi sau đó.

### 2. Input
- **Dữ liệu đầu vào (từ Broker hoặc API):**
  - `conversation_id`
  - `sender_type` (`CUSTOMER`, `AGENT`, `SYSTEM`, `BOT`)
  - `sender_id`
  - `message_type` (`TEXT`, `IMAGE`, `ATTACHMENT`, ...)
  - `payload` (JSON chứa nội dung cụ thể: text, url, metadata...)
  - `channel_message_id` (ID tin nhắn gốc trên nền tảng Zalo/Facebook để map khi có callback cập nhật trạng thái delivery).

### 3. Output
- **Kết quả trả về:** Bản ghi `Message` được tạo thành công.
- **Nơi lưu:** Bảng `Message` trong CSDL của M07. Cập nhật bảng `Conversation`.
- **Nơi gửi tiếp:** Gửi `MessageSavedEvent` vào Message Broker (Kafka).

### 4. Business Rule / Ràng buộc
- **Bảo toàn thứ tự (Message Ordering):** Tin nhắn phải được lưu theo đúng thứ tự thời gian sinh ra. Sử dụng `created_at` (với độ chính xác miligiây) hoặc Sequence ID làm tiêu chí sắp xếp.
- **Dữ liệu đính kèm (Attachments):** Nếu tin nhắn chứa File/Ảnh, M07 chỉ lưu đường dẫn URL, không lưu Binary trực tiếp trong bảng Message.
- **Chỉ đọc (Read-only):** Lịch sử tin nhắn sau khi được lưu sẽ không cho phép sửa đổi nội dung bởi Agent, đảm bảo tính pháp lý của phiên tư vấn (ngoại trừ trường hợp khách hàng thu hồi tin nhắn từ phía mạng xã hội - lúc này chỉ đổi trạng thái tin nhắn thành `UNSENT`).

### 5. Edge Case cần xử lý
- **Tin nhắn bị lặp (Duplicate Message):** Do mạng xã hội gửi lại webhook nhiều lần. -> **Xử lý:** Lưu trữ phải đảm bảo Idempotent. M07 cần kiểm tra `channel_message_id` (ID do Facebook/Zalo cấp). Nếu đã tồn tại thì bỏ qua không lưu nữa.
- **Nhận tin nhắn out-of-order (Sai thứ tự):** Mạng chập chờn khiến tin nhắn A đến sau tin nhắn B (mặc dù A gửi trước). -> **Xử lý:** M07 lưu theo Timestamp gốc (`origin_created_at`) gửi từ mạng xã hội (nếu có).
- **Khách hàng xóa tin nhắn:** Có webhook báo xóa từ mạng xã hội. -> **Xử lý:** Tìm `Message` qua `channel_message_id`, cập nhật payload thành `"Tin nhắn đã bị thu hồi"`, cờ `is_deleted = true`, và bắn sự kiện để UI cập nhật realtime.

### 6. Acceptance Criteria
- **Given** Một hội thoại đang `OPEN`.
- **When** Module M03 đẩy một tin nhắn text Inbound vào.
- **Then** Tin nhắn được lưu vào Database của M07.
- **And** `last_message_at` của hội thoại được cập nhật.
- **And** Không có lỗi nếu tin nhắn đó bị đẩy trùng `channel_message_id`.
- **And** `MessageSavedEvent` được sinh ra để báo cho UI.

- **Given** Khách hàng thu hồi một tin nhắn cũ.
- **When** M07 nhận lệnh thu hồi có chứa `channel_message_id`.
- **Then** Bản ghi tin nhắn được cập nhật trạng thái đã xóa.
- **And** UI Agent nhận được tín hiệu làm mờ nội dung tin nhắn đó đi.

### 7. Chỉ số phi chức năng
- **Hiệu năng:** API lưu tin nhắn hoặc luồng tiêu thụ (consume) message phải đạt độ trễ `< 20ms` để đảm bảo trải nghiệm chat realtime mượt mà.
- **Lưu trữ:** Phân vùng (Partition) bảng `Message` theo tháng hoặc tuần để tránh bảng bị phình to gây chậm truy vấn lịch sử sau thời gian dài.



## MOD-CONV-04: Lọc và tìm kiếm hội thoại (Filter & Search Inbox)

### 1. Mô tả nghiệp vụ đầy đủ
Chức năng cho phép Agent và Admin tìm kiếm, lọc danh sách các hội thoại trong Unified Inbox một cách nhanh chóng để dễ dàng quản lý khối lượng lớn tin nhắn.
1. **Truy vấn danh sách:** Hệ thống nhận yêu cầu lấy danh sách hội thoại có phân trang (Pagination).
2. **Áp dụng bộ lọc (Filters):**
   - Theo trạng thái (`OPEN`, `PENDING`, `RESOLVED`, `SPAM`).
   - Theo kênh (Facebook Page A, Zalo OA B, ...).
   - Theo thời gian cập nhật gần nhất (`last_message_at`).
   - Theo Agent phụ trách (Chỉ Admin mới có quyền xem chéo hội thoại của Agent khác).
   - Theo nhãn dán (Tag).
3. **Tìm kiếm toàn văn (Full-text Search):**
   - Tìm theo Tên khách hàng (Customer Name).
   - Tìm theo số điện thoại (nếu có).
4. **Sắp xếp (Sorting):** Mặc định sắp xếp theo `last_message_at` giảm dần (tin nhắn mới nhất lên đầu). Có thể sắp xếp theo `created_at` (hội thoại mở lâu nhất chưa xử lý).

### 2. Input
- **Dữ liệu đầu vào (Query Params):**
  - `status`, `channel_id`, `agent_id`, `tag_id`, `search_keyword`.
  - `page`, `size` (hoặc `cursor` cho Infinite Scroll).
- **Nguồn kích hoạt:** Agent / Admin UI.

### 3. Output
- **Kết quả trả về:** JSON bao gồm danh sách hội thoại (Metadata: tên khách hàng, avatar, preview tin nhắn cuối, trạng thái, thời gian) và thông tin phân trang (Tổng số trang, tổng số bản ghi).
- **Nơi lưu:** Không có (Thao tác Read-only).

### 4. Business Rule / Ràng buộc
- **Phân quyền dữ liệu (Data Isolation):**
  - **Agent:** Chỉ được xem các hội thoại đang được gán cho chính mình (`assignee_id = current_user_id`) hoặc các hội thoại nằm trong hàng đợi chung (Queue) chưa ai nhận.
  - **Admin:** Được xem toàn bộ hội thoại của Tenant.
- **Tối ưu hóa tìm kiếm:** Các trường dùng để filter (status, channel_id, assignee_id) bắt buộc phải được đánh Index trong CSDL. Việc tìm kiếm Full-text bằng `%LIKE%` trên SQL có thể gây chậm, cân nhắc sử dụng ElasticSearch nếu dữ liệu quá lớn (Phase sau).

### 5. Edge Case cần xử lý
- **Lọc kết hợp nhiều điều kiện rỗng:** Nếu user không chọn filter nào, trả về danh sách hội thoại `OPEN` của Agent đó theo mặc định.
- **Offset Pagination quá lớn:** Tránh lỗi "Deep Pagination" khi user request `page=1000`. Cần giới hạn số trang tối đa hoặc sử dụng Cursor-based Pagination.

### 6. Acceptance Criteria
- **Given** Agent `A` đang quản lý 5 hội thoại `OPEN` và 10 hội thoại `RESOLVED`.
- **When** Agent `A` vào màn hình Inbox (mặc định).
- **Then** Hệ thống chỉ hiển thị 5 hội thoại `OPEN`.
- **When** Agent `A` chọn filter trạng thái = `RESOLVED`.
- **Then** Hệ thống hiển thị 10 hội thoại đã đóng của Agent `A`.

- **Given** Agent `A` thử đổi URL param thành `agent_id=B` để xem lén hội thoại.
- **When** Gửi request.
- **Then** Hệ thống bỏ qua param này và vẫn chỉ trả về dữ liệu của `A` (hoặc trả 403).

### 7. Chỉ số phi chức năng
- **Tốc độ truy vấn:** API lấy danh sách hội thoại phải phản hồi dưới `< 100ms` kể cả khi có 1 triệu bản ghi trong bảng (Nhờ đánh Index đúng).


## MOD-CONV-05: Gắn thẻ hội thoại (Conversation Tagging)

### 1. Mô tả nghiệp vụ đầy đủ
Cho phép Agent đánh dấu, phân loại hội thoại bằng các thẻ (Tags) để dễ dàng thống kê và lọc (VD: Thẻ "Khách VIP", "Khiếu nại", "Chốt đơn").
1. **Lấy danh sách thẻ:** Hệ thống cung cấp API lấy các thẻ hiện có của Tenant.
2. **Gắn/Gỡ thẻ (Attach/Detach):** Agent chọn thẻ từ UI để gắn vào hội thoại hoặc bấm X để gỡ thẻ.
3. **Tạo thẻ mới (Tùy chọn):** Nếu có quyền, Agent/Admin có thể gõ tên thẻ mới và hệ thống sẽ tự động tạo thẻ đó trong từ điển thẻ (Tag Dictionary) của Tenant, sau đó gắn vào hội thoại.
4. **Lưu trữ:** Cập nhật bảng trung gian `Conversation_Tag`.

### 2. Input
- **Dữ liệu đầu vào:** `conversation_id`, `tag_id` (hoặc `tag_name` nếu tạo mới), hành động (`ADD` / `REMOVE`).
- **Nguồn kích hoạt:** Agent UI.

### 3. Output
- **Kết quả trả về:** Hội thoại được cập nhật danh sách thẻ thành công.
- **Nơi lưu:** Bảng `tags` và `conversation_tags` trong M07.

### 4. Business Rule / Ràng buộc
- **Giới hạn số lượng thẻ:** Mỗi hội thoại chỉ được gắn tối đa 10 thẻ để tránh rối mắt trên UI.
- **Thẻ theo Tenant:** Thẻ (Tag) là dữ liệu riêng biệt của từng Tenant. Không được hiển thị thẻ của Tenant A cho Tenant B.

### 5. Edge Case cần xử lý
- **Gắn trùng thẻ:** Agent gửi request gắn 1 thẻ đã có sẵn trong hội thoại. -> **Xử lý:** Bỏ qua không báo lỗi (Idempotent), trả về danh sách thẻ hiện tại.
- **Xóa thẻ đang được dùng:** Admin xóa một thẻ khỏi hệ thống (từ điển). -> **Xử lý:** Các hội thoại đã gắn thẻ này trước đây sẽ tự động mất thẻ (Xóa cascade trong bảng trung gian `conversation_tags`).

### 6. Acceptance Criteria
- **Given** Hội thoại chưa có thẻ nào.
- **When** Agent gọi API gắn thẻ "Khẩn cấp".
- **Then** Hội thoại hiển thị thẻ "Khẩn cấp".
- **And** Có thể dùng bộ lọc (MOD-CONV-04) tìm ra hội thoại này bằng thẻ "Khẩn cấp".


## MOD-CONV-06: Quản lý mẫu tin nhắn nhanh (Quick Reply Templates)

### 1. Mô tả nghiệp vụ đầy đủ
Quản lý các câu trả lời soạn sẵn (Canned Responses) giúp Agent tăng tốc độ phản hồi.
1. **Tạo mới/Cập nhật mẫu:** Admin hoặc Agent có quyền được tạo các mẫu câu trả lời. Gồm: `shortcut` (phím tắt, VD: `/banggia`), và `content` (Nội dung chi tiết).
2. **Liệt kê mẫu:** Hệ thống cung cấp API để UI load danh sách các mẫu. Khi Agent gõ `/` trong khung chat, UI sẽ filter danh sách này.
3. **Phân quyền sử dụng:** 
   - Mẫu Global: Do Admin tạo, mọi Agent đều dùng được.
   - Mẫu Cá nhân: Do Agent tự tạo, chỉ Agent đó thấy và dùng được.

### 2. Input
- **Dữ liệu đầu vào:** `shortcut` (String, viết liền không dấu), `content` (String, có thể chứa ký tự xuống dòng), `is_global` (Boolean).
- **Nguồn kích hoạt:** Màn hình cài đặt của Admin/Agent.

### 3. Output
- **Nơi lưu:** Bảng `Quick_Replies` trong cơ sở dữ liệu.

### 4. Business Rule / Ràng buộc
- **Độ dài mẫu:** `shortcut` tối đa 20 ký tự. `content` tối đa 2000 ký tự.
- **Tính duy nhất:** Trong cùng một phạm vi (Tenant đối với Global, hoặc Agent đối với Cá nhân), `shortcut` không được phép trùng lặp.

### 5. Edge Case cần xử lý
- **Lỗi trùng lặp Shortcut:** -> Trả về HTTP 400 kèm thông báo "Phím tắt này đã tồn tại".

### 6. Acceptance Criteria
- **Given** Admin tạo mẫu với shortcut `/hello` và nội dung "Xin chào, tôi có thể giúp gì?".
- **When** Một Agent bất kỳ của Tenant lấy danh sách Quick Replies.
- **Then** Thấy mẫu `/hello` trả về trong danh sách.


## MOD-CONV-07: Theo dõi thời gian phản hồi (SLA Tracking)

### 1. Mô tả nghiệp vụ đầy đủ
Chức năng tự động tính toán và cảnh báo thời gian Agent phản hồi khách hàng để đảm bảo cam kết chất lượng (SLA - Service Level Agreement).
1. **Xác định thời điểm tính:**
   - **First Response Time (FRT):** Tính từ lúc hội thoại được chuyển sang `OPEN` cho đến khi Agent gửi tin nhắn Outbound ĐẦU TIÊN.
   - **Resolution Time:** Tính từ lúc `OPEN` đến lúc chuyển sang `RESOLVED`.
2. **Đối chiếu chính sách:** Dựa vào SLA Policy (thiết lập ở M02), hệ thống biết được hạn chót (Deadline) để phản hồi là bao nhiêu phút.
3. **Cảnh báo (Warning/Breach):**
   - Khi gần đến hạn chót (VD: còn 5 phút), bắn sự kiện cảnh báo `SLA_WARNING` (M10 đẩy xuống UI báo đỏ).
   - Khi quá hạn chót, đánh dấu hội thoại là `SLA_BREACHED` và tính vào báo cáo phạt.
4. **Tạm dừng SLA:** Nếu hội thoại chuyển sang `PENDING` (chờ khách hàng cung cấp thêm thông tin), đồng hồ đếm ngược SLA sẽ tạm dừng (Pause).

### 2. Input
- **Sự kiện kích hoạt:** `ConversationCreatedEvent`, `MessageSavedEvent` (chiều Outbound), `ConversationStatusUpdatedEvent`.
- **Dữ liệu SLA:** Lấy từ cấu hình Tenant (M02).

### 3. Output
- **Nơi lưu:** Cập nhật các trường `first_responded_at`, `is_sla_breached` vào bảng `Conversation`.
- **Nơi gửi tiếp:** Gửi Event cảnh báo nếu cần.

### 4. Business Rule / Ràng buộc
- **Giờ làm việc (Business Hours):** Nếu tin nhắn đến lúc 10h tối (ngoài giờ làm việc), thời gian SLA không được đếm cho đến khi bắt đầu ca làm việc sáng hôm sau (phụ thuộc cấu hình M02).
- **Auto-reply không tính là First Response:** Tin nhắn tự động từ Bot/Hệ thống (Lời chào) KHÔNG được tính là First Response của Agent. Chỉ khi Agent thực sự gửi tin nhắn mới tính.

### 5. Edge Case cần xử lý
- **Thay đổi SLA Policy giữa chừng:** Admin sửa chính sách từ 15 phút thành 5 phút. -> **Xử lý:** Chỉ áp dụng cho các hội thoại tạo mới sau thời điểm sửa. Hội thoại cũ giữ nguyên SLA cũ lúc khởi tạo.

### 6. Acceptance Criteria
- **Given** Chính sách SLA là phản hồi lần đầu trong 15 phút.
- **When** Khách hàng nhắn tin lúc 10:00 AM (Hội thoại tạo).
- **And** Agent gửi tin nhắn phản hồi lúc 10:10 AM.
- **Then** Hệ thống ghi nhận `first_responded_at = 10:10 AM`.
- **And** Hội thoại ĐẠT chuẩn SLA (Không bị breach).

- **Given** Khách hàng nhắn lúc 10:00 AM.
- **When** Đến 10:16 AM Agent mới phản hồi.
- **Then** Hệ thống đánh dấu cờ `is_sla_breached = true`.


## MOD-CONV-08: Gửi tin nhắn riêng tư từ bình luận (Private Replies)

### 1. Mô tả nghiệp vụ đầy đủ
Luồng xử lý khi Agent muốn inbox trực tiếp cho một khách hàng vừa bình luận trên Livestream:
1. **Tiếp nhận hành động:** Agent nhấn nút "Nhắn tin riêng" (Private Reply) bên cạnh một bình luận của khách hàng trên giao diện Livestream Chat (UI).
2. **Tạo hội thoại tạm (hoặc cập nhật):** Hệ thống M07 nhận yêu cầu, kiểm tra nếu chưa có hội thoại Messenger 1-1 nào với khách hàng này, tạo ra một hội thoại mới mang tính chất "chờ kết nối" (pending reply).
3. **Gửi tin nhắn qua M03:** M07 đóng gói nội dung tin nhắn và gửi lệnh sang M03 (Channel Integration). Lệnh này đính kèm `comment_id` gốc.
4. **Gửi API Facebook:** M03 sử dụng Facebook Graph API gửi POST tới endpoint `/{comment_id}/private_replies` để gửi tin nhắn.
5. **Đồng bộ trạng thái:** 
   - Nếu Facebook trả về thành công: M07 lưu tin nhắn vào lịch sử hội thoại (chiều Outbound), đánh dấu bình luận này "Đã inbox".
   - Nếu thất bại (hết hạn, lỗi API): Trả lỗi về UI cho Agent biết.
6. **Mở khóa hội thoại:** Khi khách hàng reply lại tin nhắn inbox này, Facebook bắn webhook Message inbound. Hội thoại 1-1 chính thức được kích hoạt, tuân theo luật 24h.

### 2. Input
- **Dữ liệu đầu vào từ Agent (UI):**
  - `comment_id`: ID của bình luận gốc trên Facebook Livestream.
  - `page_id`: ID của Fanpage đang livestream.
  - `agent_id`: ID của nhân viên đang thao tác.
  - `message_text`: Nội dung tin nhắn muốn gửi (văn bản).
- **Nguồn:** Request từ giao diện Frontend gọi API của M07.

### 3. Output
- **Kết quả trả về cho Frontend:** Trạng thái gửi tin (Thành công / Thất bại kèm lý do).
- **Nơi lưu / Gửi tiếp:**
  - Lệnh gửi tin đẩy qua Kafka tới M03 để thực thi gọi Facebook API.
  - Bản ghi tin nhắn Outbound được lưu vào bảng `messages` thuộc `conversations` tương ứng.

### 4. Business Rule / Ràng buộc
- **Giới hạn thời gian (7 days rule):** Chỉ cho phép gửi Private Reply trong vòng **7 ngày** tính từ lúc khách hàng comment. Nếu comment đã quá 7 ngày, nút "Nhắn tin riêng" trên UI phải bị mờ (disabled).
- **Giới hạn tần suất (1 per comment):** Một `comment_id` chỉ được phép dùng để gửi Private Reply **đúng 1 lần duy nhất**. Hệ thống M07 phải lưu trạng thái `has_replied_privately = true` cho comment đó để chặn các lần gửi sau.
- **Loại Fanpage:** Chỉ áp dụng cho Facebook Fanpage, KHÔNG áp dụng cho Facebook Group hay trang cá nhân.

### 5. Edge Case cần xử lý
- **Lỗi từ Facebook API:** Facebook trả về lỗi (do page bị block tính năng gửi tin, hoặc comment đã bị khách xóa). Cần bắt lỗi và hiển thị rõ ràng lên UI.
- **Khách hàng chặn tin nhắn người lạ:** Tin nhắn không tới được inbox của khách. Ghi nhận lỗi `Delivery Failed` vào log của tin nhắn.
- **Agent click liên tục (Double Click):** UI hoặc Backend cần có cơ chế Debounce/Lock tạm thời theo `comment_id` để tránh gửi 2 request Private Reply cùng lúc dẫn đến lỗi 1 per comment của Facebook.

### 6. Acceptance Criteria
- **Given** Khách hàng để lại comment có `comment_id = 123` cách đây 2 ngày.
- **And** Comment này chưa từng được gửi Private Reply.
- **When** Agent nhập tin nhắn và bấm "Gửi tin riêng".
- **Then** Hệ thống gửi thành công qua Facebook API.
- **And** Giao diện cập nhật comment thành "Đã gửi tin riêng", và vô hiệu hóa nút gửi lại.

- **Given** Khách hàng để lại comment cách đây 8 ngày.
- **When** Agent cố gắng gửi tin riêng.
- **Then** Hệ thống từ chối và báo lỗi "Đã quá thời hạn 7 ngày cho phép của Facebook".

### 7. Chỉ số phi chức năng
- **Độ trễ:** Thời gian từ lúc Agent bấm gửi đến khi nhận được phản hồi thành công (kể cả thời gian gọi qua Facebook) nên < 2 giây.
