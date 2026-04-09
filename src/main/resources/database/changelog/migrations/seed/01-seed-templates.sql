-- ACCOUNT_VERIFICATION
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
             'ACCOUNT_VERIFICATION',
             'EMAIL',
             'HTML',
             '[(${organizationName})]: Verify your email address',
             '<!DOCTYPE html><html lang="en" xmlns="http://www.w3.org/1999/xhtml"><head><meta content="text/html; charset=UTF-8" http-equiv="Content-Type"><meta content="width=device-width,initial-scale=1" name="viewport"><meta content="" name="x-apple-disable-message-reformatting"><title>Verify Your Email Address</title><style>@media only screen and (max-width:600px){.wrapper{width:100%!important}.container{width:100%!important}.mobile-padding{padding-left:20px!important;padding-right:20px!important}.button-a{width:100%!important;display:block!important}}body{margin:0;padding:0;background-color:#f5f5f5}table{border-collapse:collapse}img{border:0;display:block}a{color:#2d7a3e}</style></head><body style="margin:0;padding:0;background-color:#f5f5f5;font-family:Poppins,Arial,sans-serif;-webkit-text-size-adjust:100%;-ms-text-size-adjust:100%"><div style="display:none;font-size:1px;line-height:1px;max-height:0;max-width:0;opacity:0;overflow:hidden">Click to verify your email. Expires in [[${expirationTime}]].</div><table border="0" cellpadding="0" cellspacing="0" role="presentation" style="width:100%;margin:0;padding:0;background-color:#f5f5f5;border-collapse:collapse" width="100%"><tr><td align="center" style="padding:40px 0"><table border="0" cellpadding="0" cellspacing="0" role="presentation" style="background-color:#fff;width:100%;max-width:600px;margin:0 auto;border-collapse:collapse" width="600" class="container"><tr><td align="center" style="padding:40px 30px;background-color:#2d7a3e"><img th:alt="${organizationName}" alt="" height="40" th:src="${logoUrl}" src="" style="display:block;border:0;height:40px;width:40px;margin:0 auto" width="40"></td></tr><tr><td style="padding:48px 60px;font-family:Poppins,Arial,sans-serif;font-size:16px;line-height:24px;color:#4a4a4a" class="mobile-padding"><h1 style="margin:0 0 24px 0;font-family:Poppins,Arial,sans-serif;font-size:28px;line-height:36px;color:#1a1a1a;font-weight:600">Verify Your Email Address</h1><p style="margin:0 0 16px 0;font-family:Poppins,Arial,sans-serif;font-size:16px;line-height:24px;color:#4a4a4a">Hi <strong style="font-weight:600">[[${username}]]</strong>,</p><p style="margin:0 0 24px 0;font-family:Poppins,Arial,sans-serif;font-size:16px;line-height:24px;color:#4a4a4a">Welcome! To complete your registration and start using your account, please verify your email address by clicking the button below.</p><table border="0" cellpadding="0" cellspacing="0" role="presentation" style="width:100%;margin:0 0 24px 0;border-collapse:collapse" width="100%"><tr><td align="center" style="padding:0"><table border="0" cellpadding="0" cellspacing="0" role="presentation" style="border-collapse:collapse"><tr><td align="center" style="border-radius:6px;background-color:#2d7a3e"><a th:href="${verificationLink}" href="#" style="display:inline-block;padding:14px 32px;font-family:Poppins,Arial,sans-serif;font-size:16px;font-weight:600;color:#fff;text-decoration:none;background-color:#2d7a3e;border-radius:6px;text-align:center;line-height:20px" target="_blank" class="button-a">Verify Email</a></td></tr></table></td></tr></table><p style="margin:0 0 16px 0;font-family:Poppins,Arial,sans-serif;font-size:14px;line-height:20px;color:#6b6b6b">Or copy and paste this link in your browser:</p><p style="margin:0 0 24px 0;font-family:Poppins,Arial,sans-serif;font-size:14px;line-height:20px;color:#2d7a3e;word-break:break-all;overflow-wrap:break-word"><a th:href="${verificationLink}" href="#" style="color:#2d7a3e;text-decoration:underline;word-break:break-all" target="_blank">[(${verificationLink})]</a></p><table border="0" cellpadding="0" cellspacing="0" role="presentation" style="width:100%;background-color:#f9f9f9;border-left:4px solid #2d7a3e;margin:24px 0;border-collapse:collapse" width="100%"><tr><td style="padding:16px 20px;font-family:Poppins,Arial,sans-serif;font-size:14px;line-height:20px;color:#4a4a4a"><strong style="font-weight:600">This link will expire in [[${expirationTime}]] for security purposes.</strong></td></tr></table><p style="margin:0;font-family:Poppins,Arial,sans-serif;font-size:14px;line-height:20px;color:#6b6b6b">If you didn''t create an account with us, please ignore this email or contact our support team if you have concerns.</p></td></tr><tr><td style="padding:32px 60px;background-color:#f9f9f9;border-top:1px solid #e5e5e5;font-family:Poppins,Arial,sans-serif;text-align:center" class="mobile-padding"><p style="margin:0 0 16px 0;font-family:Poppins,Arial,sans-serif;font-size:14px;line-height:20px;color:#6b6b6b;text-align:center"><strong style="color:#4a4a4a;font-weight:600">[[${organizationName}]]</strong><br><span style="color:#6b6b6b">[[${organizationAddress}]]</span></p><p style="margin:0 0 16px 0;font-family:Poppins,Arial,sans-serif;font-size:14px;line-height:20px;color:#6b6b6b;text-align:center">Need help? <a th:href="${supportUrl}" href="#" style="color:#2d7a3e;text-decoration:underline" target="_blank">Contact Support</a></p><p style="margin:0;font-family:Poppins,Arial,sans-serif;font-size:12px;line-height:18px;color:#999;text-align:center">This is an automated message, please do not reply to this email.<br>© [[${currentYear}]] [[${organizationName}]]. All rights reserved.</p></td></tr></table></td></tr></table></body></html>',
             true,
             NOW(),
             'system:liquibase-seed',
             '[
               {"name":"logoUrl","type":"STRING","description":"Publicly accessible URL for the organisation logo image","required":true,"sensitive":false},
               {"name":"organizationName","type":"STRING","description":"Display name of the organisation sending the notification","required":true,"sensitive":false},
               {"name":"organizationAddress","type":"STRING","description":"Physical or postal address of the organisation","required":true,"sensitive":false},
               {"name":"username","type":"STRING","description":"Display name of the recipient user","required":true,"sensitive":false},
               {"name":"verificationLink","type":"STRING","description":"System-generated email verification URL containing an embedded authentication token. Rendered unescaped — must only ever be populated from internal token generation, never from user input","required":true,"sensitive":true},
               {"name":"expirationTime","type":"STRING","description":"Human-readable expiry duration for the verification link, e.g. 15 minutes","required":true,"sensitive":false},
               {"name":"supportUrl","type":"STRING","description":"Publicly accessible URL to the organisation support page","required":true,"sensitive":false},
               {"name":"currentYear","type":"STRING","description":"Current calendar year used in the copyright notice","required":true,"sensitive":false}
             ]'
         );

-- PASSWORD_RESET
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
             'PASSWORD_RESET',
             'EMAIL',
             'HTML',
             '[(${organizationName})]: Password Reset Request',
             '<!DOCTYPE html><html lang="en" xmlns="http://www.w3.org/1999/xhtml"><head><meta content="text/html; charset=UTF-8" http-equiv="Content-Type"><meta content="width=device-width,initial-scale=1" name="viewport"><meta content="" name="x-apple-disable-message-reformatting"><title>Reset Your Password</title><style>@media only screen and (max-width:600px){.wrapper{width:100%!important}.container{width:100%!important}.mobile-padding{padding-left:20px!important;padding-right:20px!important}.button-a{width:100%!important;display:block!important}}body{margin:0;padding:0;background-color:#f5f5f5}table{border-collapse:collapse}img{border:0;display:block}a{color:#2d7a3e}</style></head><body style="margin:0;padding:0;background-color:#f5f5f5;font-family:Poppins,Arial,sans-serif;-webkit-text-size-adjust:100%;-ms-text-size-adjust:100%"><div style="display:none;font-size:1px;line-height:1px;max-height:0;max-width:0;opacity:0;overflow:hidden">Reset your password securely. Expires in [[${expirationTime}]].</div><table border="0" cellpadding="0" cellspacing="0" role="presentation" style="width:100%;margin:0;padding:0;background-color:#f5f5f5;border-collapse:collapse" width="100%"><tr><td align="center" style="padding:40px 0"><table border="0" cellpadding="0" cellspacing="0" role="presentation" style="background-color:#fff;width:100%;max-width:600px;margin:0 auto;border-collapse:collapse" width="600" class="container"><tr><td align="center" style="padding:40px 30px;background-color:#2d7a3e"><img th:alt="${organizationName}" alt="" height="40" th:src="${logoUrl}" src="" style="display:block;border:0;height:40px;width:40px;margin:0 auto" width="40"></td></tr><tr><td style="padding:48px 60px;font-family:Poppins,Arial,sans-serif;font-size:16px;line-height:24px;color:#4a4a4a" class="mobile-padding"><h1 style="margin:0 0 24px 0;font-family:Poppins,Arial,sans-serif;font-size:28px;line-height:36px;color:#1a1a1a;font-weight:600">Reset Your Password</h1><p style="margin:0 0 16px 0;font-family:Poppins,Arial,sans-serif;font-size:16px;line-height:24px;color:#4a4a4a">Hi <strong style="font-weight:600">[[${username}]]</strong>,</p><p style="margin:0 0 24px 0;font-family:Poppins,Arial,sans-serif;font-size:16px;line-height:24px;color:#4a4a4a">We received a request to reset your password. Click the button below to create a new password for your account.</p><table border="0" cellpadding="0" cellspacing="0" role="presentation" style="width:100%;margin:0 0 24px 0;border-collapse:collapse" width="100%"><tr><td align="center" style="padding:0"><table border="0" cellpadding="0" cellspacing="0" role="presentation" style="border-collapse:collapse"><tr><td align="center" style="border-radius:6px;background-color:#2d7a3e"><a th:href="${resetLink}" href="#" style="display:inline-block;padding:14px 32px;font-family:Poppins,Arial,sans-serif;font-size:16px;font-weight:600;color:#fff;text-decoration:none;background-color:#2d7a3e;border-radius:6px;text-align:center;line-height:20px" target="_blank" class="button-a">Reset Password</a></td></tr></table></td></tr></table><p style="margin:0 0 16px 0;font-family:Poppins,Arial,sans-serif;font-size:14px;line-height:20px;color:#6b6b6b">Or copy and paste this link in your browser:</p><p style="margin:0 0 24px 0;font-family:Poppins,Arial,sans-serif;font-size:14px;line-height:20px;color:#2d7a3e;word-break:break-all;overflow-wrap:break-word"><a th:href="${resetLink}" href="#" style="color:#2d7a3e;text-decoration:underline;word-break:break-all" target="_blank">[(${resetLink})]</a></p><table border="0" cellpadding="0" cellspacing="0" role="presentation" style="width:100%;background-color:#f9f9f9;border-left:4px solid #2d7a3e;margin:0 0 24px 0;border-collapse:collapse" width="100%"><tr><td style="padding:16px 20px;font-family:Poppins,Arial,sans-serif;font-size:14px;line-height:20px;color:#4a4a4a">This link will expire in <strong style="font-weight:600">[[${expirationTime}]]</strong> for security purposes.</td></tr></table><table border="0" cellpadding="0" cellspacing="0" role="presentation" style="width:100%;background-color:#fff3cd;border-left:4px solid #ffc107;margin:24px 0;border-collapse:collapse" width="100%"><tr><td style="padding:16px 20px;font-family:Poppins,Arial,sans-serif;font-size:14px;line-height:20px;color:#856404">If you did not request a password reset, please ignore this email. Your account remains secure.</td></tr></table><p style="margin:0;font-family:Poppins,Arial,sans-serif;font-size:14px;line-height:20px;color:#6b6b6b">For security, we recommend choosing a strong password that you have not used before.</p></td></tr><tr><td style="padding:32px 60px;background-color:#f9f9f9;border-top:1px solid #e5e5e5;font-family:Poppins,Arial,sans-serif;text-align:center" class="mobile-padding"><p style="margin:0 0 16px 0;font-family:Poppins,Arial,sans-serif;font-size:14px;line-height:20px;color:#6b6b6b;text-align:center"><strong style="color:#4a4a4a;font-weight:600">[[${organizationName}]]</strong><br><span style="color:#6b6b6b">[[${organizationAddress}]]</span></p><p style="margin:0 0 16px 0;font-family:Poppins,Arial,sans-serif;font-size:14px;line-height:20px;color:#6b6b6b;text-align:center">Need help? <a th:href="${supportUrl}" href="#" style="color:#2d7a3e;text-decoration:underline" target="_blank">Contact Support</a></p><p style="margin:0;font-family:Poppins,Arial,sans-serif;font-size:12px;line-height:18px;color:#999;text-align:center">This is an automated message, please do not reply to this email.<br>© [[${currentYear}]] [[${organizationName}]]. All rights reserved.</p></td></tr></table></td></tr></table></body></html>',
             true,
             NOW(),
             'system:liquibase-seed',
             '[
               {"name":"logoUrl","type":"STRING","description":"Publicly accessible URL for the organisation logo image","required":true,"sensitive":false},
               {"name":"organizationName","type":"STRING","description":"Display name of the organisation sending the notification","required":true,"sensitive":false},
               {"name":"organizationAddress","type":"STRING","description":"Physical or postal address of the organisation","required":true,"sensitive":false},
               {"name":"username","type":"STRING","description":"Display name of the recipient user","required":true,"sensitive":false},
               {"name":"resetLink","type":"STRING","description":"System-generated password reset URL containing an embedded authentication token. Rendered unescaped — must only ever be populated from internal token generation, never from user input","required":true,"sensitive":true},
               {"name":"expirationTime","type":"STRING","description":"Human-readable expiry duration for the reset link, e.g. 15 minutes","required":true,"sensitive":false},
               {"name":"supportUrl","type":"STRING","description":"Publicly accessible URL to the organisation support page","required":true,"sensitive":false},
               {"name":"currentYear","type":"STRING","description":"Current calendar year used in the copyright notice","required":true,"sensitive":false}
             ]'
         );

-- PHONE_VERIFICATION
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
             'PHONE_VERIFICATION',
             'SMS',
             'PLAIN_TEXT',
             NULL,
             '[(${organizationName})]: Hi [(${username})], your verification code is [(${verificationCode})]. Valid until [(${verificationExpiry})]. Do not share this code.',
             true,
             NOW(),
             'system:liquibase-seed',
             '[
               {"name":"organizationName","type":"STRING","description":"Display name of the organisation sending the notification","required":true,"sensitive":false},
               {"name":"username","type":"STRING","description":"Display name of the recipient user","required":true,"sensitive":false},
               {"name":"verificationCode","type":"STRING","description":"One-time verification code. Must not appear in logs or audit records","required":true,"sensitive":true},
               {"name":"verificationExpiry","type":"STRING","description":"Human-readable expiry time for the verification code, e.g. 14:35 EAT","required":true,"sensitive":false}
             ]'
         );

--rollback DELETE FROM notifications.templates WHERE name IN ('ACCOUNT_VERIFICATION', 'PASSWORD_RESET', 'PHONE_VERIFICATION');