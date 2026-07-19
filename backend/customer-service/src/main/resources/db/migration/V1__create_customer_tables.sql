-- ==========================================
-- Customer Service - V1 Migration
-- Task 5.1.1.1: Create customers and channel_identities tables
-- Database: customer_service
-- ==========================================

CREATE TABLE customers (
    id VARCHAR(36) PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20),
    address TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_phone (phone_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE channel_identities (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL,
    platform ENUM('FACEBOOK', 'ZALO', 'SHOPEE', 'TIKTOK') NOT NULL,
    external_user_id VARCHAR(255) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_identity_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE RESTRICT,
    UNIQUE INDEX uniq_platform_external (platform, external_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
