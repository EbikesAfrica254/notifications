--comment: create a notifications table to store notification records
CREATE TABLE notifications.notifications (
     id                UUID         NOT NULL DEFAULT gen_random_uuid(),
     branch_id         VARCHAR(36),
     channel           VARCHAR(50)  NOT NULL,
     created_at        TIMESTAMPTZ  NOT NULL,
     created_by        VARCHAR(36) NOT NULL,
     message_body      TEXT         NOT NULL,
     message_subject   VARCHAR(500),
     organization_id   VARCHAR(36)         NOT NULL,
     recipient         VARCHAR(500)  NOT NULL,
     service_reference VARCHAR(100) NOT NULL,
     status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
     template_id       UUID,
     template_version  INTEGER,
     updated_at        TIMESTAMPTZ,
     updated_by        VARCHAR(36),
     variables         JSONB,

     CONSTRAINT pk_notifications
         PRIMARY KEY (id),

     CONSTRAINT uq_notifications_service_reference
         UNIQUE (service_reference),

     CONSTRAINT fk_notifications_template
         FOREIGN KEY (template_id)
             REFERENCES notifications.templates (id)
             ON DELETE RESTRICT,

     CONSTRAINT chk_notifications_subject_email_only
         CHECK (
             (channel = 'EMAIL' AND message_subject IS NOT NULL AND trim(message_subject) <> '')
                 OR (channel <> 'EMAIL' AND message_subject IS NULL)
             ),

     CONSTRAINT chk_notifications_variables_is_object
         CHECK (variables IS NULL OR jsonb_typeof(variables) = 'object'),

     CONSTRAINT chk_notifications_variables_size
         CHECK (variables IS NULL OR char_length(variables::text) <= 8192),

     CONSTRAINT chk_notifications_template_version_positive
         CHECK (template_version IS NULL OR template_version >= 1)
);

--comment: create indexes for the notifications table
CREATE INDEX idx_notifications_branch_id         ON notifications.notifications (branch_id);
CREATE INDEX idx_notifications_organization_id   ON notifications.notifications (organization_id);
CREATE INDEX idx_notifications_service_reference ON notifications.notifications (service_reference);
CREATE INDEX idx_notifications_status            ON notifications.notifications (status);

--comment: add table and column comments for notifications
COMMENT ON TABLE notifications.notifications IS
    'Notification records tracking message delivery intent across channels. '
        'Core fields are immutable after creation. Only status, failure_reason, updated_at, '
        'and updated_by are mutable post-creation via domain state transitions. '
        'Sensitive variable values (OTPs, tokens) must be redacted before persistence at the application layer.';

COMMENT ON COLUMN notifications.notifications.id                IS 'Primary key - auto-generated UUID';
COMMENT ON COLUMN notifications.notifications.branch_id         IS 'Optional branch-level tenant scope - UUID from IAM service';
COMMENT ON COLUMN notifications.notifications.channel           IS 'Delivery channel - EMAIL, SMS, WHATSAPP, SSE - validated at application layer';
COMMENT ON COLUMN notifications.notifications.created_at        IS 'Record creation timestamp in UTC - immutable';
COMMENT ON COLUMN notifications.notifications.created_by        IS 'Identity of the originating service or actor - immutable';
COMMENT ON COLUMN notifications.notifications.message_body      IS 'Rendered message content snapshot. Sensitive values (OTPs, tokens) are redacted to •••••• before storage. Immutable after creation.';
COMMENT ON COLUMN notifications.notifications.message_subject   IS 'Rendered subject line - required for EMAIL channel, must be null for all other channels. Immutable after creation.';
COMMENT ON COLUMN notifications.notifications.organization_id   IS 'Organization-level tenant scope for multi-tenant isolation - UUID from IAM service. Immutable after creation.';
COMMENT ON COLUMN notifications.notifications.recipient         IS 'Masked delivery target - email address, phone number, or user ID depending on channel. Plain value used for dispatch before persistence. Immutable after creation.';
COMMENT ON COLUMN notifications.notifications.service_reference IS 'Idempotency key to prevent duplicate sends on retry - typically messageId from publisher. Immutable after creation.';
COMMENT ON COLUMN notifications.notifications.status            IS 'Delivery status - PENDING, SENT, DELIVERED, FAILED. Valid transitions: PENDING→SENT, SENT→DELIVERED, PENDING/SENT→FAILED, FAILED→PENDING (retry only).';
COMMENT ON COLUMN notifications.notifications.template_id       IS 'Reference to template used - null for STRUCTURED messages. Immutable after creation.';
COMMENT ON COLUMN notifications.notifications.template_version  IS 'Version of the template at render time - combined with template_id forms an exact immutable snapshot reference. Null for STRUCTURED messages.';
COMMENT ON COLUMN notifications.notifications.updated_at        IS 'Timestamp of last status transition in UTC';
COMMENT ON COLUMN notifications.notifications.updated_by        IS 'Identity of actor or service that performed the last status transition';
COMMENT ON COLUMN notifications.notifications.variables         IS 'Render-time variable values used to produce message_body. Sensitive values redacted to •••••• before storage. Null for STRUCTURED messages.';