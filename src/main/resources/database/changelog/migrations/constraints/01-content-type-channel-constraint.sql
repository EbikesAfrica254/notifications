--comment: rename content_type to template_content_type and replace chk_templates_html_email_only with chk_templates_content_type_channel to enforce content type and channel compatibility rules
ALTER TABLE notifications.templates
    RENAME COLUMN content_type TO template_content_type;
ALTER TABLE notifications.templates
    DROP CONSTRAINT chk_templates_html_email_only;
ALTER TABLE notifications.templates
    ADD CONSTRAINT chk_templates_content_type_channel
        CHECK (
            (template_content_type = 'HTML'       AND channel = 'EMAIL')    OR
            (template_content_type = 'JSON'       AND channel = 'WHATSAPP') OR
            template_content_type = 'PLAIN_TEXT'
            );
COMMENT ON COLUMN notifications.templates.template_content_type IS
    'Content format - PLAIN_TEXT, HTML, JSON. HTML is restricted to EMAIL channel only. JSON is restricted to WHATSAPP channel only. PLAIN_TEXT is permitted on any channel.';

--rollback ALTER TABLE notifications.templates DROP CONSTRAINT chk_templates_content_type_channel;
--rollback ALTER TABLE notifications.templates RENAME COLUMN template_content_type TO content_type;
--rollback ALTER TABLE notifications.templates ADD CONSTRAINT chk_templates_html_email_only CHECK (content_type <> 'HTML' OR channel = 'EMAIL');
--rollback COMMENT ON COLUMN notifications.templates.content_type IS 'Content format - PLAIN_TEXT, HTML - HTML is restricted to EMAIL channel only';