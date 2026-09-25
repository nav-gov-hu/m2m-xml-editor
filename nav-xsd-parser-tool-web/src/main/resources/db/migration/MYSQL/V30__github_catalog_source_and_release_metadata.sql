alter table github_template_release add column form_name varchar(2000) null;
alter table github_template_release add column valid_from timestamp null;
alter table github_template_release add column valid_to timestamp null;
alter table github_template_release add column disabled_flag boolean not null default false;
alter table github_template_release add column local_imported_flag boolean not null default false;
insert ignore into system_configuration(config_key, config_value, updated_at, updated_by) values ('nav.xsdparsertool.github-schema-updater.catalog-source-mode','GITHUB_API',current_timestamp,'migration');
insert ignore into system_configuration(config_key, config_value, updated_at, updated_by) values ('nav.xsdparsertool.github-schema-updater.catalog-repository','catalog',current_timestamp,'migration');
insert ignore into system_configuration(config_key, config_value, updated_at, updated_by) values ('nav.xsdparsertool.github-schema-updater.catalog-branch','main',current_timestamp,'migration');
insert ignore into system_configuration(config_key, config_value, updated_at, updated_by) values ('nav.xsdparsertool.github-schema-updater.catalog-xml-url-template','https://raw.githubusercontent.com/{owner}/{repo}/{branch}/content/artifact-catalog.xml',current_timestamp,'migration');
