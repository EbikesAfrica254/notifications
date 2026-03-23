--comment: create the user_channel_preferences table to store individual user opt-in/out preferences per channel and category
CREATE TABLE notifications.user_channel_preferences (
                                                        id              UUID NOT NULL DEFAULT gen_random_uuid(),
                                                        category        VARCHAR(50) NOT NULL,
                                                        channel         VARCHAR(50) NOT NULL,
                                                        enabled         BOOLEAN NOT NULL DEFAULT true,
                                                        created_at      TIMESTAMPTZ NOT NULL,
                                                        organization_id VARCHAR(36) NOT NULL,
                                                        updated_at      TIMESTAMPTZ,
                                                        user_id         VARCHAR(36) NOT NULL,
                                                        version         INTEGER NOT NULL DEFAULT 0,
                                                        CONSTRAINT pk_user_channel_prefs PRIMARY KEY (id),
                                                        CONSTRAINT uq_user_channel_prefs UNIQUE (user_id, organization_id, channel, category),
                                                        CONSTRAINT chk_user_channel CHECK (channel IN ('EMAIL', 'SMS', 'SSE', 'WHATSAPP')),
                                                        CONSTRAINT chk_user_category CHECK (category IN ('TRANSACTIONAL', 'SECURITY', 'OPERATIONAL', 'MARKETING'))
);

--comment: create indexes for user_channel_preferences table
CREATE INDEX idx_user_prefs_lookup ON notifications.user_channel_preferences(user_id, organization_id, channel, category);

--comment: add table and column comments for user_channel_preferences
COMMENT ON TABLE notifications.user_channel_preferences IS 'User-level opt-in/out preferences per channel and category within an organization. Evaluated after org-level gate. SECURITY and TRANSACTIONAL categories are non-suppressible and bypassed at the application layer regardless of this value.';
COMMENT ON COLUMN notifications.user_channel_preferences.id IS 'Primary key - auto-generated UUID';
COMMENT ON COLUMN notifications.user_channel_preferences.category IS 'Notification category - TRANSACTIONAL, SECURITY, OPERATIONAL, MARKETING';
COMMENT ON COLUMN notifications.user_channel_preferences.channel IS 'Notification channel - EMAIL, SMS, SSE, WHATSAPP - validated at application and DB layer';
COMMENT ON COLUMN notifications.user_channel_preferences.enabled IS 'Whether the user has opted in to this channel for this category. Defaults to true - record only created on explicit opt-out or opt-in change.';
COMMENT ON COLUMN notifications.user_channel_preferences.created_at IS 'Record creation timestamp in UTC';
COMMENT ON COLUMN notifications.user_channel_preferences.organization_id IS 'Organization context for this preference - stored as VARCHAR(36) to accommodate UUID string representations';
COMMENT ON COLUMN notifications.user_channel_preferences.updated_at IS 'Last modification timestamp in UTC - null until first update';
COMMENT ON COLUMN notifications.user_channel_preferences.user_id IS 'User that owns this preference - stored as VARCHAR(36) to accommodate UUID string representations';
COMMENT ON COLUMN notifications.user_channel_preferences.version IS 'Optimistic locking version - incremented on every update. Used for cache invalidation.';