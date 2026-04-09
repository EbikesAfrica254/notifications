--comment: create an inbox table for idempotent event consumption with deduplication
CREATE TABLE notifications.inbox (
    event_type VARCHAR(100) NOT NULL,
    service_reference VARCHAR(255) NOT NULL,
    source_context VARCHAR(100) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at TIMESTAMPTZ,
    CONSTRAINT pk_inbox PRIMARY KEY (service_reference)
);

--comment: create indexes for the inbox table
CREATE INDEX idx_inbox_processed_at ON notifications.inbox(processed_at);
CREATE INDEX idx_inbox_source_context ON notifications.inbox(source_context);

--comment: add table and column comments for inbox
COMMENT ON TABLE notifications.inbox IS 'Inbox pattern for idempotent event consumption - deduplicates at-least-once delivered events from upstream contexts';
COMMENT ON COLUMN notifications.inbox.service_reference IS 'Primary key - unique event identifier from publisher, prevents duplicate processing';
COMMENT ON COLUMN notifications.inbox.event_type IS 'Event type name from upstream context';
COMMENT ON COLUMN notifications.inbox.source_context IS 'Originating bounded context - Assignment Strategy, Payment & Billing, Workforce Management, etc';
COMMENT ON COLUMN notifications.inbox.received_at IS 'Timestamp when event first arrived';
COMMENT ON COLUMN notifications.inbox.processed_at IS 'Timestamp when event processing completed - null if still processing or failed';