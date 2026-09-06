create table xml_file_revision (
    id bigserial primary key,
    xml_file_id bigint not null references xml_file(id),
    revision_no integer not null,
    save_type varchar(50) not null,
    target_file_path text,
    backup_file_path text,
    diff_summary text,
    change_count integer not null default 0,
    xsd_validation_requested boolean not null default false,
    xsd_validation_status varchar(50),
    user_note varchar(1000),
    created_at timestamp not null,
    created_by varchar(255)
);
create table xml_file_diff_entry (
    id bigserial primary key,
    revision_id bigint not null references xml_file_revision(id),
    xml_file_id bigint not null references xml_file(id),
    change_type varchar(50) not null,
    xml_path text,
    old_value text,
    new_value text,
    display_label varchar(1000)
);
create index idx_xml_file_revision_file on xml_file_revision(xml_file_id);
create index idx_xml_file_diff_revision on xml_file_diff_entry(revision_id);
create index idx_xml_file_diff_file on xml_file_diff_entry(xml_file_id);
