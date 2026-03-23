--comment: create a deliveries table to track individual delivery attempts and retries
CREATE TABLE notifications.deliveries (
  id UUID NOT NULL DEFAULT gen_random_uuid(),
  attempt_number INTEGER NOT NULL,
  attempted_at TIMESTAMPTZ NOT NULL,
  completed_at TIMESTAMPTZ,
  cost_amount DECIMAL(10,4),
  cost_currency VARCHAR(3) DEFAULT 'KES',
  error_code VARCHAR(100),
  error_message VARCHAR(4000),
  next_retry_at TIMESTAMPTZ,
  notification_id UUID NOT NULL,
  organization_id VARCHAR(36) NOT NULL,
  provider_message_id VARCHAR(255),
  provider_response JSONB,
  status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
  CONSTRAINT pk_deliveries PRIMARY KEY (id),
  CONSTRAINT fk_deliveries_notification FOREIGN KEY (notification_id) REFERENCES notifications.notifications(id),
  CONSTRAINT chk_deliveries_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'DELIVERED', 'FAILED', 'RATE_LIMITED', 'INVALID_RECIPIENT', 'CHANNEL_DISABLED', 'TIMEOUT'))
);

--comment: create indexes for deliveries table
CREATE INDEX idx_deliveries_notification_seq ON notifications.deliveries(notification_id, attempt_number);
CREATE INDEX idx_deliveries_organization_seq ON notifications.deliveries(organization_id, notification_id);
CREATE INDEX idx_deliveries_retry_queue ON notifications.deliveries(status, next_retry_at);
CREATE INDEX idx_deliveries_time ON notifications.deliveries(attempted_at);

--comment: add table and column comments for deliveries
COMMENT ON TABLE notifications.deliveries IS 'Individual delivery attempts including retries. One record per attempt for complete audit trail.';
COMMENT ON COLUMN notifications.deliveries.id IS 'Primary key - auto-generated UUID';
COMMENT ON COLUMN notifications.deliveries.attempt_number IS '1-based sequence number - 1 for first attempt, 2 for first retry, etc';
COMMENT ON COLUMN notifications.deliveries.attempted_at IS 'When this delivery attempt started - maps to created_at from base entity';
COMMENT ON COLUMN notifications.deliveries.completed_at IS 'When attempt finished - success or final failure timestamp';
COMMENT ON COLUMN notifications.deliveries.cost_amount IS 'Cost charged for this attempt - primarily for SMS billing';
COMMENT ON COLUMN notifications.deliveries.cost_currency IS 'ISO 4217 currency code for cost_amount';
COMMENT ON COLUMN notifications.deliveries.error_code IS 'ChannelAdapter-specific error code if attempt failed';
COMMENT ON COLUMN notifications.deliveries.error_message IS 'Human-readable error description separate from provider response';
COMMENT ON COLUMN notifications.deliveries.next_retry_at IS 'Calculated timestamp for next retry using exponential backoff';
COMMENT ON COLUMN notifications.deliveries.notification_id IS 'Foreign key to parent notification';
COMMENT ON COLUMN notifications.deliveries.organization_id IS 'External organization reference - stored as VARCHAR(36) to accommodate UUID string representations';
COMMENT ON COLUMN notifications.deliveries.provider_message_id IS 'External tracking ID from provider (SES Message ID, Wasiliana reference, etc)';
COMMENT ON COLUMN notifications.deliveries.provider_response IS 'Full response from delivery provider stored as JSONB for debugging';
COMMENT ON COLUMN notifications.deliveries.status IS 'Current status - validated at application layer and constrained at DB layer';