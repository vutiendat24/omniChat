CREATE TABLE tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    color VARCHAR(7) DEFAULT '#000000',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_name (tenant_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE conversation_tags MODIFY COLUMN tag_id BIGINT NOT NULL;

ALTER TABLE conversation_tags ADD CONSTRAINT fk_ct_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE;

