CREATE TABLE plans (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    max_teams INT,
    max_users INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE teams (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_team_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT uk_team_tenant_name UNIQUE (tenant_id, name)
);

ALTER TABLE tenants ADD CONSTRAINT fk_tenant_plan FOREIGN KEY (plan_id) REFERENCES plans(id);

-- Insert default plans
INSERT INTO plans (id, name, max_teams, max_users) VALUES 
('Trial', 'Trial Plan', 3, 5),
('Basic', 'Basic Plan', 3, 10),
('Pro', 'Pro Plan', -1, -1);
