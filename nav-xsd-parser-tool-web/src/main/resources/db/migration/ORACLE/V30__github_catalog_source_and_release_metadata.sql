alter table github_template_release add (form_name varchar2(2000));
alter table github_template_release add (valid_from timestamp);
alter table github_template_release add (valid_to timestamp);
alter table github_template_release add (disabled_flag number(1) default 0 not null);
alter table github_template_release add (local_imported_flag number(1) default 0 not null);
merge into system_configuration t using (select 'nav.xsdparsertool.github-schema-updater.catalog-source-mode' config_key, 'GITHUB_API' config_value from dual) s on (t.config_key=s.config_key) when not matched then insert (config_key,config_value,updated_at,updated_by) values (s.config_key,s.config_value,current_timestamp,'migration');
merge into system_configuration t using (select 'nav.xsdparsertool.github-schema-updater.catalog-repository' config_key, 'catalog' config_value from dual) s on (t.config_key=s.config_key) when not matched then insert (config_key,config_value,updated_at,updated_by) values (s.config_key,s.config_value,current_timestamp,'migration');
merge into system_configuration t using (select 'nav.xsdparsertool.github-schema-updater.catalog-branch' config_key, 'main' config_value from dual) s on (t.config_key=s.config_key) when not matched then insert (config_key,config_value,updated_at,updated_by) values (s.config_key,s.config_value,current_timestamp,'migration');
merge into system_configuration t using (select 'nav.xsdparsertool.github-schema-updater.catalog-xml-url-template' config_key, 'https://raw.githubusercontent.com/{owner}/{repo}/{branch}/content/artifact-catalog.xml' config_value from dual) s on (t.config_key=s.config_key) when not matched then insert (config_key,config_value,updated_at,updated_by) values (s.config_key,s.config_value,current_timestamp,'migration');
