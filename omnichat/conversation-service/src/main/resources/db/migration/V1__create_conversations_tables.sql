CREATE TABLE conversations (
    id VARCHAR(36) PRIMARY KEY,
    channel_identity_id VARCHAR(36) NOT NULL,
    channel_connection_id BIGINT NOT NULL,
    assigned_agent_id BIGINT NULL,
    status ENUM('UNASSIGNED', 'OPEN', 'CLOSED') DEFAULT 'UNASSIGNED',
    last_activity_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_sla_breached BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_agent_status_activity (assigned_agent_id, status, last_activity_at DESC),
    INDEX idx_sla_monitor (status, last_activity_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE messages (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    sender_type ENUM('CUSTOMER', 'AGENT', 'SYSTEM') NOT NULL,
    sender_id VARCHAR(255) NULL,
    content_text TEXT,
    content_attachments JSON,
    status ENUM('SENT', 'DELIVERED', 'READ', 'FAILED') DEFAULT 'SENT',
    sent_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_msg_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE RESTRICT,
    INDEX idx_conversation_sent (conversation_id, sent_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE conversation_tags (
    conversation_id VARCHAR(36) NOT NULL,
    tag_id INT NOT NULL,
    assigned_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (conversation_id, tag_id),
    CONSTRAINT fk_ct_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
