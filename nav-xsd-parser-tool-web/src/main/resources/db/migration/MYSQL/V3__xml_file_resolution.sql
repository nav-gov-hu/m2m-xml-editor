-- XML file header/resource resolution metadata for MySQL 5.7+.
-- Long XML paths/schema locations are TEXT to avoid MySQL row size limit with utf8mb4.

alter table xml_file add column schema_location text null;
alter table xml_file add column no_namespace_schema_location text null;
alter table xml_file add column xsd_path text null;
alter table xml_file add column uimodel_path text null;
alter table xml_file add column xpath_rules_path text null;
alter table xml_file add column resolution_status varchar(50) null;
alter table xml_file add column resolution_message text null;

create index idx_xml_file_resolution_status on xml_file(resolution_status);
