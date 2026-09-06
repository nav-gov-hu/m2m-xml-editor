DELETE FROM system_secret
WHERE secret_key = 'nav.xsdparsertool.xpath-validator.db.password';

DELETE FROM system_configuration
WHERE config_key IN (
    'nav.xsdparsertool.xpath-validator.db.path',
    'nav.xsdparsertool.xpath-validator.db.username',
    'nav.xsdparsertool.xpath-validator.db.password'
);
