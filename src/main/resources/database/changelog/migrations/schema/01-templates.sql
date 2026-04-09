--comment: create the templates table to store message templates with variable placeholders
CREATE TABLE notifications.templates (
     id                   UUID         NOT NULL DEFAULT gen_random_uuid(),
     body_template        TEXT         NOT NULL,
     channel              VARCHAR(50)  NOT NULL,
     content_type         VARCHAR(50)  NOT NULL,
     created_at           TIMESTAMPTZ  NOT NULL,
     created_by           VARCHAR(100) NOT NULL,
     is_active            BOOLEAN      NOT NULL DEFAULT true,
     name                 VARCHAR(100) NOT NULL,
     subject              VARCHAR(500),
     updated_at           TIMESTAMPTZ,
     updated_by           VARCHAR(100),
     variable_definitions JSONB        NOT NULL DEFAULT '[]',
     version              INTEGER      NOT NULL DEFAULT 1,

     CONSTRAINT pk_templates
         PRIMARY KEY (id),

     CONSTRAINT uq_templates_name
         UNIQUE (name),

     CONSTRAINT chk_templates_name_format
         CHECK (name ~ '^[A-Z][A-Z0-9_]{2,99}$'),

     CONSTRAINT chk_templates_subject_email_required
         CHECK (
             (channel = 'EMAIL' AND subject IS NOT NULL AND trim(subject) <> '')
                 OR (channel <> 'EMAIL' AND subject IS NULL)
             ),

     CONSTRAINT chk_templates_html_email_only
         CHECK (
             content_type <> 'HTML' OR channel = 'EMAIL'
             ),

     CONSTRAINT chk_templates_body_length
         CHECK (
             (channel = 'EMAIL' AND char_length(body_template) <= 102400) OR
             (channel = 'SMS'   AND char_length(body_template) <= 320)   OR
             (channel = 'SSE'   AND char_length(body_template) <= 4096) OR
             (channel = 'WHATSAPP'  AND char_length(body_template) <= 1024)
         ),

     CONSTRAINT chk_templates_variable_definitions_is_array
         CHECK (jsonb_typeof(variable_definitions) = 'array'),

     CONSTRAINT chk_templates_version_positive
         CHECK (version >= 1)
);

--comment: create indexes for templates table
CREATE INDEX idx_templates_lookup  ON notifications.templates (name, is_active);
CREATE INDEX idx_templates_channel ON notifications.templates (channel, is_active);
CREATE INDEX idx_templates_vars    ON notifications.templates USING GIN (variable_definitions);

--comment: add table and column comments for templates
COMMENT ON TABLE notifications.templates IS
    'Message templates with Thymeleaf inline expression placeholders ([[${variable}]] syntax). '
        'Rendered content snapshot stored in notifications table for immutability. '
        'Template writes require elevated privileges and maker-checker approval.';

COMMENT ON COLUMN notifications.templates.id                   IS 'Primary key - auto-generated UUID';
COMMENT ON COLUMN notifications.templates.body_template        IS 'Template content using Thymeleaf inline syntax: [[${variableName}]] for text, [(${variableName})] for unescaped HTML. Must only reference variables declared in variable_definitions. Size limits enforced per channel: SMS 320, SSE 4096, EMAIL 102400 characters';
COMMENT ON COLUMN notifications.templates.channel              IS 'Delivery channel - EMAIL, SMS, SSE - validated at application layer';
COMMENT ON COLUMN notifications.templates.content_type         IS 'Content format - PLAIN_TEXT, HTML - HTML is restricted to EMAIL channel only';
COMMENT ON COLUMN notifications.templates.created_at           IS 'Record creation timestamp in UTC';
COMMENT ON COLUMN notifications.templates.created_by           IS 'Identity of the operator who created this template';
COMMENT ON COLUMN notifications.templates.is_active            IS 'Whether template is available for use - soft disable flag';
COMMENT ON COLUMN notifications.templates.name                 IS 'Unique template identifier for lookup. Format: SCREAMING_SNAKE_CASE, 3-100 characters, must begin with an uppercase letter';
COMMENT ON COLUMN notifications.templates.subject              IS 'Email subject line - required for EMAIL channel, must be null for all other channels';
COMMENT ON COLUMN notifications.templates.updated_at           IS 'Last modification timestamp in UTC';
COMMENT ON COLUMN notifications.templates.updated_by           IS 'Identity of the operator who last modified this template';
COMMENT ON COLUMN notifications.templates.variable_definitions IS 'JSONB array declaring all variables referenced in body_template and subject. Each element: {name: string, type: string, required: boolean, description: string}. Render-time validation must reject calls that do not supply all required variables or reference undeclared names';
COMMENT ON COLUMN notifications.templates.version              IS 'Monotonically incrementing version counter, incremented on every update. Allows callers to detect template changes and supports pinning in notification snapshots';