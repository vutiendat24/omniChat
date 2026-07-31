ALTER TABLE channel_connections
ADD COLUMN tenant_id VARCHAR(255) NOT NULL,
ADD COLUMN channel_id VARCHAR(255) NOT NULL,
ADD COLUMN avatar_url TEXT;

-- Create unique index to ensure one channel can only be connected to one tenant
CREATE UNIQUE INDEX idx_channel_tenant ON channel_connections(channel_id, tenant_id);
