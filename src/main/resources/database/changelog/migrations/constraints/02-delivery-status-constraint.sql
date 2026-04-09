--comment: add OFFLINE and SUCCESS to the deliveries status check constraint
ALTER TABLE notifications.deliveries
    DROP CONSTRAINT chk_deliveries_status;
ALTER TABLE notifications.deliveries
    ADD CONSTRAINT chk_deliveries_status
        CHECK (status IN ('PENDING', 'IN_PROGRESS', 'DELIVERED', 'FAILED', 'RATE_LIMITED', 'INVALID_RECIPIENT', 'CHANNEL_DISABLED', 'TIMEOUT', 'OFFLINE', 'SUCCESS'));
--rollback ALTER TABLE notifications.deliveries DROP CONSTRAINT chk_deliveries_status;
--rollback ALTER TABLE notifications.deliveries ADD CONSTRAINT chk_deliveries_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'DELIVERED', 'FAILED', 'RATE_LIMITED', 'INVALID_RECIPIENT', 'CHANNEL_DISABLED', 'TIMEOUT'));