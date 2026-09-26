alter table github_template_release add column if not exists form_name varchar(2000);
alter table github_template_release add column if not exists valid_from timestamp;
alter table github_template_release add column if not exists valid_to timestamp;
alter table github_template_release add column if not exists disabled_flag boolean not null default false;
alter table github_template_release add column if not exists local_imported_flag boolean not null default false;
insert into system_configuration(config_key, config_value, updated_at, updated_by) values ('nav.xsdparsertool.github-schema-updater.catalog-source-mode','GITHUB_API',current_timestamp,'migration') on conflict (config_key) do nothing;
insert into system_configuration(config_key, config_value, updated_at, updated_by) values ('nav.xsdparsertool.github-schema-updater.catalog-repository','catalog',current_timestamp,'migration') on conflict (config_key) do nothing;
insert into system_configuration(config_key, config_value, updated_at, updated_by) values ('nav.xsdparsertool.github-schema-updater.catalog-branch','main',current_timestamp,'migration') on conflict (config_key) do nothing;
insert into system_configuration(config_key, config_value, updated_at, updated_by) values ('nav.xsdparsertool.github-schema-updater.catalog-xml-url-template','https://raw.githubusercontent.com/{owner}/{repo}/{branch}/content/artifact-catalog.xml',current_timestamp,'migration') on conflict (config_key) do nothing;
