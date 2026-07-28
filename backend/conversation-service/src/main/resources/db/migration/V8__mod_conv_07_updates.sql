ALTER TABLE conversations
ADD COLUMN sla_due_at DATETIME NULL AFTER is_sla_breached,
ADD COLUMN sla_paused_at DATETIME NULL AFTER sla_due_at,
ADD COLUMN first_responded_at DATETIME NULL AFTER sla_paused_at;
