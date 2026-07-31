CREATE TABLE quick_replies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    agent_id BIGINT,
    shortcut VARCHAR(20) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    is_global BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_shortcut_global (tenant_id, shortcut, is_global),
    UNIQUE KEY uk_agent_shortcut (agent_id, shortcut)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
