# TÀI LIỆU ĐẶC TẢ YÊU CẦU SẢN PHẨM (PRD)
## HỆ THỐNG QUẢN LÝ CHAT ĐA KÊNH TẬP TRUNG (OMNICHANNEL CHAT MANAGEMENT SYSTEM)

| Thông tin tài liệu | Chi tiết |
| :--- | :--- |
| **Tên dự án** | Hệ thống Quản lý Chat Đa Kênh Tập Trung (Omnichannel Chat Management - OCM) |
| **Phiên bản** | 1.0.0 |
| **Ngày khởi tạo** | 21/06/2026 |
| **Tác giả** | Business Analyst Team |
| **Trạng thái** | Sẵn sàng phê duyệt (Ready for Review) |

---

### 1. TỔNG QUAN DỰ ÁN & MỤC TIÊU (PROJECT OVERVIEW & OBJECTIVES)

#### 1.1. Bối cảnh dự án (Background)
Trong kỷ nguyên Social Commerce (Thương mại điện tử qua mạng xã hội), các doanh nghiệp và chủ cửa hàng thường triển khai bán hàng đồng thời trên nhiều nền tảng lớn như TikTok Shop, Facebook Fanpage, Zalo Official Account (OA), và Shopee. Việc quản lý tin nhắn và tương tác với khách hàng bị phân tán, dẫn đến các vấn đề nghiêm trọng:
* Bỏ sót tin nhắn của khách hàng, làm giảm tỷ lệ chuyển đổi đơn hàng.
* Thời gian phản hồi (SLA) lâu do nhân viên phải chuyển đổi liên tục giữa các ứng dụng/tab trình duyệt.
* Không có cơ chế phân phối, điều hướng tin nhắn thông minh đến từng nhân viên tư vấn một cách tự động và công bằng.
* Thiếu dữ liệu báo cáo tập trung để đánh giá hiệu suất của nhân viên tư vấn (Agent) và hiệu quả của từng kênh bán hàng.

Hệ thống OCM ra đời nhằm giải quyết triệt để các bài toán trên bằng cách gom tất cả các cuộc hội thoại về một giao diện duy nhất và tự động hóa quy trình phân phối công việc.

#### 1.2. Mục tiêu sản phẩm (Product Objectives)
* **Hợp nhất dữ liệu (Unified Inbox):** Kết nối API chính thức của 4 nền tảng (TikTok, Facebook, Zalo, Shopee) để tiếp nhận và phản hồi tin nhắn theo thời gian thực (Real-time).
* **Tối ưu hóa quy trình (Smart Routing):** Tự động điều hướng cuộc hội thoại đến đúng nhân viên tư vấn dựa trên các quy tắc (Rules) được thiết lập linh hoạt.
* **Nâng cao năng suất:** Cung cấp các công cụ bổ trợ như Tin nhắn nhanh (Quick Replies), Gắn nhãn phân loại (Tagging), và Hồ sơ khách hàng thu nhỏ (Mini CRM).
* **Giám sát toàn diện (Analytics):** Cung cấp hệ thống báo cáo thời gian thực về sản lượng tin nhắn, tốc độ phản hồi, và hiệu suất làm việc của nhân viên.

#### 1.3. Chỉ số đo lường thành công (Product KPIs)
* Tốc độ đồng bộ tin nhắn từ các nền tảng về hệ thống: **< 2 giây**.
* Tỷ lệ tin nhắn bị bỏ sót (Missed chats): **0%** nhờ hệ thống cảnh báo SLA.
* Giảm thời gian phản hồi trung bình (First Response Time) của doanh nghiệp xuống **dưới 1 phút**.
* Hệ thống hoạt động ổn định với chỉ số uptime đạt **99.9%**.

---

### 2. PHẠM VI SẢN PHẨM & ĐỐI TƯỢNG SỬ DỤNG (SCOPE & ACTORS)

#### 2.1. Các Tác nhân trong hệ thống (Actors)
1.  **Quản trị viên (Admin/Chủ shop):** Chủ sở hữu hệ thống, có toàn quyền cấu hình tích hợp kênh, thiết lập luật điều hướng, quản lý tài khoản và xem toàn bộ báo cáo doanh thu/hiệu suất.
2.  **Trưởng nhóm (Supervisor/Leader):** Người giám sát trực tiếp đội ngũ tư vấn. Có quyền xem toàn bộ nội dung chat, can thiệp điều hướng thủ công, thu hồi cuộc chat, cấu hình câu trả lời mẫu và xem báo cáo hiệu suất đội nhóm.
3.  **Nhân viên tư vấn (Agent):** Người trực tiếp giao tiếp với khách hàng. Chỉ có quyền xem và xử lý các cuộc hội thoại được hệ thống phân phối hoặc được giao chỉ định.
4.  **Khách hàng (Customer):** Người gửi tin nhắn từ các nền tảng vòng ngoài (Facebook, TikTok, Zalo, Shopee). Hệ thống nhận diện khách hàng qua ID do API nền tảng cung cấp.

#### 2.2. Ma trận phân quyền chức năng (Role-Based Access Control - RBAC)

| Mã Module | Tính năng | Admin | Supervisor | Agent |
| :--- | :--- | :---: | :---: | :---: |
| **INT** | Kết nối / Hủy kết nối kênh (FB, TT, Zalo, Shopee) | R/W | R | Không |
| **CHAT** | Xem danh sách hội thoại tập trung | R | R (Tất cả) | R (Được gán) |
| **CHAT** | Gửi/Nhận tin nhắn, hình ảnh, tệp tin | R/W | R/W | R/W |
| **CHAT** | Quản lý câu trả lời mẫu (Quick Reply) | R/W | R/W | R |
| **CRM** | Chỉnh sửa thông tin khách hàng (Mini CRM) | R/W | R/W | R/W |
| **ROUT** | Cấu hình quy tắc điều hướng tự động | R/W | R | Không |
| **ROUT** | Chuyển/Thu hồi cuộc hội thoại thủ công | R/W | R/W | R (Chỉ chuyển) |
| **REP** | Xem báo cáo hiệu suất nhân viên & sản lượng | R | R | Không |

---

### 3. DANH SÁCH & ĐẶC TẢ CHI TIẾT USE CASE (USE CASE SPECIFICATIONS)

#### 3.1. Phân hệ Tích hợp Kênh (Integration Module)
* **UC-101: Kết nối/Hủy kết nối kênh Facebook Fanpage**
    * *Tác nhân:* Admin.
    * *Luồng xử lý chính:* Admin đăng nhập Facebook Auth -> Hệ thống hiển thị danh sách Page -> Admin chọn Page cần kết nối -> Hệ thống đăng ký Webhook với Facebook Graph API để nhận sự kiện tin nhắn.
* **UC-102: Kết nối/Hủy kết nối kênh TikTok Shop**
    * *Tác nhân:* Admin.
    * *Luồng xử lý chính:* Admin được điều hướng sang trang Ủy quyền của TikTok Shop Open Platform -> Đăng nhập tài khoản shop -> Đồng ý cấp quyền truy cập tin nhắn (Authorization) -> Hệ thống lưu trữ Refresh Token và thiết lập đồng bộ.
* **UC-103: Kết nối/Hủy kết nối kênh Zalo OA**
    * *Tác nhân:* Admin.
    * *Luồng xử lý chính:* Admin đăng nhập Zalo Account -> Chọn Official Account cần tích hợp -> Cấp quyền truy cập qua Zalo OpenAPI -> Hệ thống cấu hình Webhook URL để nhận tin nhắn từ người quan tâm.
* **UC-104: Kết nối/Hủy kết nối kênh Shopee**
    * *Tác nhân:* Admin.
    * *Luồng xử lý chính:* Admin chọn thị trường (Ví dụ: Vietnam) -> Đăng nhập tài khoản người bán Shopee -> Xác thực mã OTP -> Chấp nhận liên kết ứng dụng OCM -> Hệ thống đồng bộ danh mục chat của Shopee.

#### 3.2. Phân hệ Quản lý Hội thoại Tập trung (Centralized Chat Module)
* **UC-201: Tiếp nhận tin nhắn đa kênh theo thời gian thực**
    * *Tác nhân:* Hệ thống, Khách hàng.
    * *Luồng xử lý chính:* Khách hàng nhắn tin trên nền tảng nguồn -> Nền tảng bắn sự kiện Webhook về OCM -> Hệ thống phân tích nguồn (Source ID), định dạng tin nhắn -> Lưu vào Cơ sở dữ liệu (Database) -> Đẩy tin nhắn lên giao diện Omni-Inbox qua WebSocket.
* **UC-202: Phản hồi hội thoại đa phương tiện**
    * *Tác nhân:* Agent, Supervisor, Admin.
    * *Luồng xử lý chính:* Người dùng nhập nội dung/chọn ảnh tại khung chat -> Nhấn gửi -> Hệ thống gọi API tương ứng của nền tảng nguồn để đẩy tin nhắn đến khách hàng -> Hiển thị trạng thái "Đã gửi" hoặc "Thất bại".
* **UC-203: Gắn nhãn (Tagging) phân loại**
    * *Tác nhân:* Agent, Supervisor.
    * *Luồng xử lý chính:* Tại khung hội thoại, người dùng chọn biểu tượng Nhãn -> Chọn các nhãn có sẵn (ví dụ: `Cần gọi lại`, `Khách VIP`, `Khiếu nại`) hoặc tạo nhãn mới -> Hệ thống lưu thông tin nhãn vào cuộc hội thoại và cập nhật bộ lọc hiển thị.
* **UC-204: Quản lý thông tin khách hàng (Mini CRM)**
    * *Tác nhân:* Agent, Supervisor.
    * *Luồng xử lý chính:* Hệ thống tự động bốc tách thông tin định danh cơ bản của khách từ nền tảng nguồn -> Hiển thị ở thanh Sidebar bên phải khung chat -> Agent có thể bổ sung các thông tin: Số điện thoại, Địa chỉ giao hàng, Email, và Ghi chú nội bộ (Notes).

#### 3.3. Phân hệ Điều hướng & Phân phối Tin nhắn (Message Routing Module)
* **UC-301: Cấu hình luật điều hướng tự động**
    * *Tác nhân:* Admin.
    * *Luồng xử lý chính:* Admin bật/tắt các chế độ chia việc:
        * *Chế độ 1 - Round-robin:* Chia đều xoay vòng cho các Agent đang có trạng thái "Online".
        * *Chế độ 2 - Ưu tiên khách cũ (Customer Retention):* Nếu khách hàng đã từng chat trước đó, hệ thống ưu tiên phân phối thẳng cho Agent gần nhất xử lý khách đó (nếu Agent đó đang Online).
* **UC-302: Tự động phân phối cuộc hội thoại**
    * *Tác nhân:* Hệ thống.
    * *Luồng xử lý chính:* Khi cuộc hội thoại mới xuất hiện (chưa có người phụ trách) -> Hệ thống quét danh sách Agent đang hoạt động -> Áp dụng luật cấu hình ở UC-301 -> Gán thuộc tính `assigned_to` cho Agent phù hợp -> Bắn thông báo (Push Notification) cho Agent đó.
* **UC-303: Chuyển/Thu hồi cuộc hội thoại thủ công**
    * *Tác nhân:* Supervisor, Agent.
    * *Luồng xử lý chính:* Agent/Supervisor nhấn nút "Chuyển giao diện" -> Chọn Agent mục tiêu từ danh sách thành viên trực tuyến -> Nhập lý do chuyển (tùy chọn) -> Hệ thống chuyển quyền sở hữu cuộc chat sang Agent mới. Người cũ sẽ không còn quyền nhắn tin trong cuộc hội thoại đó (trừ Supervisor/Admin).

#### 3.4. Phân hệ Giám sát & Báo cáo (Analytics Module)
* **UC-401: Xem báo cáo hiệu suất Agent**
    * *Tác nhân:* Admin, Supervisor.
    * *Luồng xử lý chính:* Người dùng truy cập menu Báo cáo -> Chọn khoảng thời gian -> Hệ thống tính toán và hiển thị các chỉ số: Số hội thoại đã xử lý, Thời gian phản hồi lần đầu trung bình (FRT), Thời gian xử lý trung bình (AHT), Tỷ lệ đánh giá hài lòng (nếu có hỗ trợ).
* **UC-402: Xem báo cáo sản lượng tin nhắn đa kênh**
    * *Tác nhân:* Admin, Supervisor.
    * *Luồng xử lý chính:* Hệ thống hiển thị biểu đồ đường (Line Chart) hoặc biểu đồ cột (Bar Chart) biểu diễn lượng tin nhắn/hội thoại mới theo từng khung giờ trong ngày, phân tách rõ ràng theo các màu đại diện cho 4 nền tảng Facebook, TikTok, Zalo, Shopee.

---

### 4. YÊU CẦU CHỨC NĂNG CHI TIẾT (FUNCTIONAL REQUIREMENTS)

Các yêu cầu chức năng dưới đây được phân loại theo mã chức năng để phục vụ quá trình kiểm thử (Testing) và phát triển (Development):

#### 4.1. Nhóm chức năng Hệ thống & Kết nối (FR-INT)
* **FR-INT-001:** Hệ thống phải duy trì trạng thái kết nối Token dài hạn với Facebook, TikTok, Zalo, Shopee. Khi Token sắp hết hạn (trước 7 ngày), hệ thống phải gửi thông báo cảnh báo cho Admin qua Email/In-app Notification.
* **FR-INT-002:** Trong trường hợp API của một nền tảng bị lỗi kết nối hoặc mất tín hiệu (Disconnect), hệ thống phải tự động lưu trữ các yêu cầu vào hàng đợi (Retry Queue) và thực hiện kết nối lại tự động tối đa 5 lần, mỗi lần cách nhau 30 giây trước khi báo lỗi về cho Admin.

#### 4.2. Nhóm chức năng Hộp thư tập trung (FR-CHAT)
* **FR-CHAT-001:** Giao diện Omnichannel Inbox phải hỗ trợ bộ lọc động (Dynamic Filters) nâng cao bao gồm: Lọc theo kênh nguồn, Lọc theo trạng thái (Chưa phân phối, Đang xử lý, Đã đóng), Lọc theo nhãn khách hàng (Tags), và Lọc theo nhân viên phụ trách.
* **FR-CHAT-002:** Hệ thống phải tích hợp chức năng Tìm kiếm thông minh toàn văn (Full-text Search), cho phép tìm kiếm nhanh hội thoại dựa trên Tên khách hàng, Số điện thoại hoặc một từ khóa nội dung tin nhắn bất kỳ trong quá khứ.
* **FR-CHAT-003:** Chức năng Tin nhắn nhanh (Quick Replies): Cho phép người dùng gõ ký tự đầu bằng dấu gạch chéo `/` kèm từ khóa phím tắt (Ví dụ: `/gia`) để kích hoạt danh sách thả xuống chứa nội dung soạn sẵn, bao gồm cả văn bản và liên kết hình ảnh.

#### 4.3. Nhóm chức năng Phân phối & Điều hướng (FR-ROUT)
* **FR-ROUT-001:** Hệ thống phải hỗ trợ cơ chế quản lý trạng thái làm việc của Agent (Agent Availability Status). Agent có thể chọn các trạng thái: `Sẵn sàng` (Hệ thống tự động chia chat), `Bận` (Tạm ngưng chia chat mới, vẫn xử lý chat cũ), `Ngoại tuyến` (Ngưng toàn bộ việc điều hướng tự động).
* **FR-ROUT-002:** Quản lý cam kết chất lượng dịch vụ (SLA Breach Management): Admin có thể thiết lập thời gian phản hồi giới hạn (Ví dụ: 3 phút). Nếu một tin nhắn mới của khách hàng sau thời gian này không được Agent phản hồi, hệ thống phải thực hiện đồng thời 2 hành động:
    1. Bắn thông báo khẩn cấp màu đỏ trên màn hình của Supervisor.
    2. Tự động chuyển cuộc hội thoại đó sang trạng thái "Chưa phân phối" để hệ thống tái điều hướng cho một Agent khác đang rảnh rỗi.

#### 4.4. Nhóm chức năng Quản lý thông tin Khách hàng (FR-CRM)
* **FR-CRM-001:** Hệ thống phải tự động kiểm tra trùng lặp thông tin dựa trên trường Số điện thoại (Phone Number Deduplication). Nếu khách hàng nhắn tin từ Facebook và Zalo đều cung cấp cùng một số điện thoại, hệ thống phải cho phép Agent thực hiện tính năng "Hợp nhất hồ sơ" (Merge Profiles) để quy về một khách hàng duy nhất trong Mini CRM.

---

### 5. YÊU CẦU PHI CHỨC NĂNG (NON-FUNCTIONAL REQUIREMENTS)

#### 5.1. Hiệu năng & Khả năng mở rộng (Performance & Scalability)
* **NFR-PER-001 (Concurrency):** Hệ thống phải đáp ứng khả năng xử lý đồng thời (Concurrent Users) tối thiểu 2,000 Agent đăng nhập và xử lý chat cùng một lúc mà không xảy ra hiện tượng trễ giao diện.
* **NFR-PER-002 (Throughput):** Khả năng xử lý lượng tin nhắn đổ về từ các cổng API (Throughput) đạt tối thiểu 15,000 tin nhắn/phút trong các khung giờ cao điểm bán hàng (Flash Sale).
* **NFR-PER-003 (Latency):** Thời gian tải trang đầu tiên (Page Load Time) và chuyển đổi qua lại giữa các màn hình chức năng phải nhỏ hơn 1.5 giây.

#### 5.2. An toàn & Bảo mật thông tin (Security & Privacy)
* **NFR-SEC-001 (Data Encryption):** Toàn bộ dữ liệu truyền tải giữa người dùng và hệ thống, cũng như dữ liệu kết nối API từ các nền tảng ngoại vi, phải được mã hóa bằng giao thức HTTPS/TLS 1.3. Dữ liệu lưu trữ trong database (đặc biệt là Token, Mật khẩu, Thông tin cá nhân của khách hàng) phải được mã hóa bằng thuật toán AES-256.
* **NFR-SEC-002 (Session Management):** Phiên đăng nhập của nhân viên tư vấn sẽ tự động hết hạn sau 12 tiếng liên tục hoặc sau 30 phút nếu không có bất kỳ tương tác nào trên hệ thống (Idle Timeout) để tránh rò rỉ dữ liệu thông tin khách hàng tại cửa hàng.

#### 5.3. Độ tin cậy & Tính sẵn sàng (Reliability & Availability)
* **NFR-REL-001:** Tỷ lệ sẵn sàng của hệ thống phải đạt mức tối thiểu **99.9% (Uptime)**. Hệ thống được triển khai trên kiến trúc Cloud (như AWS hoặc Google Cloud) với cơ chế tự động mở rộng tài nguyên (Auto-scaling) và chịu lỗi (Failover) đa vùng (Multi-region).
* **NFR-REL-002 (Backup):** Hệ thống tự động sao lưu dữ liệu hội thoại và thông tin khách hàng hàng ngày (Daily Backup) vào lúc 02:00 AM và lưu trữ bản sao lưu tối thiểu trong vòng 6 tháng.

---

### 6. RÀNG BUỘC KỸ THUẬT & QUẢN LÝ RỦI RO (CONSTRAINTS & RISKS)

#### 6.1. Ràng buộc kỹ thuật bên thứ ba (Third-party Constraints)
* **Shopee & TikTok API Rate Limit:** Các nền tảng thương mại điện tử giới hạn số lượng request gọi API trong mỗi phút đối với một tài khoản cửa hàng. Đội ngũ phát triển kỹ thuật bắt buộc phải xây dựng cơ chế Hàng đợi tin nhắn (Message Queue - ví dụ sử dụng RabbitMQ hoặc Kafka) và cơ chế bộ nhớ đệm (Caching với Redis) để điều hòa lượng request, tránh bị khóa cổng kết nối (HTTP 429 Too Many Requests).
* **Chính sách nền tảng (Platform Policy Change):** Cả Facebook và Zalo đều thường xuyên thay đổi chính sách quyền riêng tư đối với dữ liệu người dùng (Ví dụ: Facebook quy định sau 7 ngày không tương tác sẽ không được chủ động nhắn tin lại cho khách hàng). Hệ thống OCM cần cập nhật liên tục giao diện để cảnh báo thời hạn phản hồi cho nhân viên đúng luật của từng nền tảng.

#### 6.2. Rủi ro dự án và Giải pháp giảm thiểu (Risks & Mitigations)
* **Rủi ro:** Trùng lặp dữ liệu tin nhắn do cơ chế Retry Webhook của các nền tảng khi mạng bị chập chờn.
    * *Giải pháp giảm thiểu:* Sử dụng trường mã định danh tin nhắn duy nhất (`Message ID` gốc từ Facebook/TikTok/Zalo/Shopee) làm khóa chính (Unique Key) hoặc áp dụng cơ chế chống trùng lặp Idempotent tại cổng tiếp nhận tin nhắn đầu vào của hệ thống.
* **Rủi ro:** Nhân viên tư vấn cố tình tải hàng loạt thông tin dữ liệu khách hàng (Số điện thoại, địa chỉ) để mang đi nơi khác.
    * *Giải pháp giảm thiểu:* Ẩn bớt các chữ số ở giữa của Số điện thoại trong Mini CRM đối với tài khoản Agent (Ví dụ: `091****888`). Chỉ có tài khoản Supervisor hoặc Admin mới có nút "Xem đầy đủ số điện thoại" và hệ thống sẽ ghi lại lịch sử nhật ký (Audit Log) mỗi khi có ai nhấn xem hoặc xuất dữ liệu.
