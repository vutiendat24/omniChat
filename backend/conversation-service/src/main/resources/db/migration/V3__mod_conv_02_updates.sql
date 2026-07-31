ALTER TABLE conversations
ADD COLUMN version BIGINT DEFAULT 0,
ADD COLUMN closed_at TIMESTAMP NULL;

CREATE TABLE conversation_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    changed_by VARCHAR(255) NOT NULL,
    old_status VARCHAR(50) NOT NULL,
    new_status VARCHAR(50) NOT NULL,
    reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
