ALTER TABLE conversations
ADD COLUMN customer_name VARCHAR(255) NULL,
ADD COLUMN customer_phone VARCHAR(50) NULL,
ADD COLUMN customer_avatar VARCHAR(500) NULL;

CREATE INDEX idx_conversations_customer_name ON conversations(customer_name);
CREATE INDEX idx_conversations_customer_phone ON conversations(customer_phone);
