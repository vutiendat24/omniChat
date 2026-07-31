CREATE TABLE private_reply_records (
    comment_id VARCHAR(255) PRIMARY KEY,
    page_id VARCHAR(255) NOT NULL,
    conversation_id VARCHAR(36) NOT NULL,
    message_id VARCHAR(36) NOT NULL,
    agent_id VARCHAR(255) NOT NULL,
    status ENUM('PENDING', 'SUCCESS', 'FAILED') DEFAULT 'PENDING',
    error_message TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_page_comment (page_id, comment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
