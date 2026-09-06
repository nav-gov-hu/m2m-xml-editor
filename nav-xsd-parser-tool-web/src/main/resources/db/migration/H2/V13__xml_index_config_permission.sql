insert into app_role (role_code, role_name)
select 'XML_INDEX_CONFIG_MANAGE', 'XML index konfiguráció kezelése'
where not exists (select 1 from app_role where role_code = 'XML_INDEX_CONFIG_MANAGE');
