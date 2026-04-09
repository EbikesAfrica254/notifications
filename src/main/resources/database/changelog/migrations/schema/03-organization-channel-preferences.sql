--comment: create organization_channel_preferences table to store org-level channel gates per notification category
CREATE TABLE notifications.organization_channel_preferences (
                                                                id              UUID NOT NULL DEFAULT gen_random_uuid(),
                                                                category        VARCHAR(50) NOT NULL,
                                                                channel         VARCHAR(50) NOT NULL,
                                                                enabled         BOOLEAN NOT NULL DEFAULT true,
                                                                created_at      TIMESTAMPTZ NOT NULL,
                                                                organization_id VARCHAR(36) NOT NULL,
                                                                updated_at      TIMESTAMPTZ,
                                                                version         INTEGER NOT NULL DEFAULT 0,
                                                                CONSTRAINT pk_org_channel_prefs PRIMARY KEY (id),
                                                                CONSTRAINT uq_org_channel_prefs UNIQUE (organization_id, channel, category),
                                                                CONSTRAINT chk_org_channel CHECK (channel IN ('EMAIL', 'SMS', 'SSE', 'WHATSAPP')),
                                                                CONSTRAINT chk_org_category CHECK (category IN ('TRANSACTIONAL', 'SECURITY', 'OPERATIONAL', 'MARKETING'))
);

--comment: create indexes for the organization_channel_preferences table
CREATE INDEX idx_org_prefs_lookup ON notifications.organization_channel_preferences(organization_id, channel, category);

--comment: add table and column comments for organization_channel_preferences
COMMENT ON TABLE notifications.organization_channel_preferences IS 'Organization-level channel gates per notification category. Evaluated before user preferences. If disabled at org level, no user preference can override.';
COMMENT ON COLUMN notifications.organization_channel_preferences.id IS 'Primary key - auto-generated UUID';
COMMENT ON COLUMN notifications.organization_channel_preferences.category IS 'Notification category - TRANSACTIONAL, SECURITY, OPERATIONAL, MARKETING';
COMMENT ON COLUMN notifications.organization_channel_preferences.channel IS 'Notification channel - EMAIL, SMS, SSE, WHATSAPP - validated at application and DB layer';
COMMENT ON COLUMN notifications.organization_channel_preferences.enabled IS 'Whether this channel is permitted for this category within the organization';
COMMENT ON COLUMN notifications.organization_channel_preferences.created_at IS 'Record creation timestamp in UTC';
COMMENT ON COLUMN notifications.organization_channel_preferences.organization_id IS 'Organization that owns this preference - stored as VARCHAR(36) to accommodate UUID string representations';
COMMENT ON COLUMN notifications.organization_channel_preferences.updated_at IS 'Last modification timestamp in UTC - null until first update';
COMMENT ON COLUMN notifications.organization_channel_preferences.version IS 'Optimistic locking version - incremented on every update. Used for cache invalidation.';