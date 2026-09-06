ALTER TABLE app_user DROP COLUMN authentication_source;
DELETE FROM system_configuration WHERE config_key LIKE 'nav.xsdparsertool.ad-role.%' OR config_key = 'nav.xsdparsertool.security.authentication-mode' OR config_key LIKE 'nav.xsdparsertool.security.active-directory.%';
