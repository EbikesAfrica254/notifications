--comment: drop next_retry_at column and its index from deliveries table - retry timing is broker-driven

--rollback ALTER TABLE notifications.deliveries ADD COLUMN next_retry_at TIMESTAMPTZ;
--rollback COMMENT ON COLUMN notifications.deliveries.next_retry_at IS 'Calculated timestamp for next retry using exponential backoff';
--rollback CREATE INDEX idx_deliveries_retry_queue ON notifications.deliveries(status, next_retry_at);

DROP INDEX IF EXISTS notifications.idx_deliveries_retry_queue;

ALTER TABLE notifications.deliveries
    DROP COLUMN IF EXISTS next_retry_at;