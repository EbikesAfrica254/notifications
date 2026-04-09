--comment: relax templates version constraint to allow version=0 on insert (Hibernate @Version)
ALTER TABLE notifications.templates
    DROP CONSTRAINT chk_templates_version_positive,
    ADD CONSTRAINT chk_templates_version_non_negative CHECK (version >= 0);
--rollback ALTER TABLE notifications.templates DROP CONSTRAINT chk_templates_version_non_negative;
--rollback ALTER TABLE notifications.templates ADD CONSTRAINT chk_templates_version_positive CHECK (version >= 1);