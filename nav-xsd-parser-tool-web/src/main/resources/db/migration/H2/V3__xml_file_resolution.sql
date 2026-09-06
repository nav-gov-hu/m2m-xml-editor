-- XML file header/resource resolution metadata for H2.

alter table xml_file add column if not exists schema_location varchar(2000);
alter table xml_file add column if not exists no_namespace_schema_location varchar(2000);
alter table xml_file add column if not exists xsd_path varchar(2000);
alter table xml_file add column if not exists uimodel_path varchar(2000);
alter table xml_file add column if not exists xpath_rules_path varchar(2000);
alter table xml_file add column if not exists resolution_status varchar(50);
alter table xml_file add column if not exists resolution_message varchar(2000);

create index if not exists idx_xml_file_resolution_status on xml_file(resolution_status);
