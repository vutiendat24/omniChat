-- ==========================================
-- Routing Service - V1 Migration
-- Task 4.1.1.1: Create agents and agent_routing_profiles tables
-- Database: routing_db (routing_service)
-- ==========================================

CREATE TABLE agents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    role ENUM('ADMIN', 'SUPERVISOR', 'AGENT') DEFAULT 'AGENT',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE agent_routing_profiles (
    agent_id BIGINT PRIMARY KEY,
    status ENUM('ONLINE', 'BUSY', 'OFFLINE') DEFAULT 'OFFLINE',
    current_workload INT DEFAULT 0,
    max_capacity INT DEFAULT 10,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_routing_profile_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed data: Insert a default agent for development/testing
INSERT INTO agents (full_name, email, role) VALUES
    ('Admin User', 'admin@omnichat.com', 'ADMIN'),
    ('Agent One', 'agent1@omnichat.com', 'AGENT'),
    ('Agent Two', 'agent2@omnichat.com', 'AGENT');

INSERT INTO agent_routing_profiles (agent_id, status, current_workload, max_capacity) VALUES
    (1, 'ONLINE', 0, 10),
    (2, 'OFFLINE', 0, 10),
    (3, 'OFFLINE', 0, 10);
