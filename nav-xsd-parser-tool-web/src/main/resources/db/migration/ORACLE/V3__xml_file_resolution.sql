-- XML file header/resource resolution metadata for Oracle 19c+.

alter table xml_file add schema_location varchar2(2000 char);
alter table xml_file add no_namespace_schema_location varchar2(2000 char);
alter table xml_file add xsd_path varchar2(2000 char);
alter table xml_file add uimodel_path varchar2(2000 char);
alter table xml_file add xpath_rules_path varchar2(2000 char);
alter table xml_file add resolution_status varchar2(50 char);
alter table xml_file add resolution_message varchar2(2000 char);

create index idx_xml_file_resolution_status on xml_file(resolution_status);
