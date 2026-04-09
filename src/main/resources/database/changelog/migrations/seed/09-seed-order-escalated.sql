-- ORDER_ESCALATED: WHATSAPP notification sent when an order is escalated and requires immediate supervisor attention
INSERT INTO notifications.templates (
    name,
    channel,
    content_type,
    subject,
    body_template,
    is_active,
    created_at,
    created_by,
    variable_definitions
) VALUES (
             'ORDER_ESCALATED',
             'WHATSAPP',
             'JSON',
             NULL,
             '{"messageType":"CTA_URL","body":"[[${organizationName}]]: Order [[${orderReference}]] has been escalated and requires your immediate attention.","urlButtons":[{"url":"[[${orderUrl}]]","display_text":"View Order"}]}',
             true,
             NOW(),
             'system:liquibase-seed',
             '[
               {"name":"orderReference","type":"STRING","description":"Human-readable order reference number","required":true,"sensitive":false},
               {"name":"orderUrl","type":"STRING","description":"Deep link to the order detail in the ops dashboard, built by the publisher","required":true,"sensitive":false},
               {"name":"organizationName","type":"STRING","description":"Display name of the organisation sending the notification","required":false,"sensitive":false}
             ]'
         );

--rollback DELETE FROM notifications.templates WHERE name = 'ORDER_ESCALATED';