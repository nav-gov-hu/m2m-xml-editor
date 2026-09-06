CREATE TABLE system_configuration (
    config_key VARCHAR2(255 CHAR) NOT NULL,
    config_value CLOB,
    updated_at TIMESTAMP NOT NULL,
    updated_by VARCHAR2(255 CHAR),
    CONSTRAINT pk_system_configuration PRIMARY KEY (config_key)
);

INSERT ALL
 INTO system_configuration(config_key,config_value,updated_at,updated_by) VALUES ('nav.xsdparsertool.security.password-policy.minimum-length','14',CURRENT_TIMESTAMP,'migration')
 INTO system_configuration(config_key,config_value,updated_at,updated_by) VALUES ('nav.xsdparsertool.security.password-policy.maximum-length','128',CURRENT_TIMESTAMP,'migration')
 INTO system_configuration(config_key,config_value,updated_at,updated_by) VALUES ('nav.xsdparsertool.security.password-policy.history-size','5',CURRENT_TIMESTAMP,'migration')
 INTO system_configuration(config_key,config_value,updated_at,updated_by) VALUES ('nav.xsdparsertool.security.password-policy.maximum-failed-attempts','5',CURRENT_TIMESTAMP,'migration')
 INTO system_configuration(config_key,config_value,updated_at,updated_by) VALUES ('nav.xsdparsertool.security.password-policy.lock-duration','15m',CURRENT_TIMESTAMP,'migration')
 INTO system_configuration(config_key,config_value,updated_at,updated_by) VALUES ('nav.xsdparsertool.security.password-policy.forbidden-passwords','jelszo,jelszó,password,password1,123456,admin',CURRENT_TIMESTAMP,'migration')
 INTO system_configuration(config_key,config_value,updated_at,updated_by) VALUES ('nav.xsdparsertool.ad-role.groups.ADMIN','NAV-SET-ADMIN',CURRENT_TIMESTAMP,'migration')
 INTO system_configuration(config_key,config_value,updated_at,updated_by) VALUES ('nav.xsdparsertool.ad-role.groups.OPERATOR','NAV-SET-OPERATOR',CURRENT_TIMESTAMP,'migration')
 INTO system_configuration(config_key,config_value,updated_at,updated_by) VALUES ('nav.xsdparsertool.ad-role.groups.VIEWER','NAV-SET-VIEWER',CURRENT_TIMESTAMP,'migration')
 INTO system_configuration(config_key,config_value,updated_at,updated_by) VALUES ('nav.xsdparsertool.xsd-validation.max-errors','500',CURRENT_TIMESTAMP,'migration')
 INTO system_configuration(config_key,config_value,updated_at,updated_by) VALUES ('nav.xsdparsertool.xml-file.large-file.threshold','20 MB',CURRENT_TIMESTAMP,'migration')
 INTO system_configuration(config_key,config_value,updated_at,updated_by) VALUES ('nav.m2m.mock-mode','true',CURRENT_TIMESTAMP,'migration')
 INTO system_configuration(config_key,config_value,updated_at,updated_by) VALUES ('nav.m2m.status-poll.enabled','true',CURRENT_TIMESTAMP,'migration')
 INTO system_configuration(config_key,config_value,updated_at,updated_by) VALUES ('nav.xsdparsertool.github-schema-updater.enabled','true',CURRENT_TIMESTAMP,'migration')
 INTO system_configuration(config_key,config_value,updated_at,updated_by) VALUES ('nav.xsdparsertool.github-schema-updater.organization','nav-gov-hu-templates',CURRENT_TIMESTAMP,'migration')
SELECT 1 FROM dual;
