create table xml_file_revision (
    id bigint not null auto_increment primary key,
    xml_file_id bigint not null,
    revision_no int not null,
    save_type varchar(50) not null,
    target_file_path text,
    backup_file_path text,
    diff_summary text,
    change_count int not null default 0,
    xsd_validation_requested boolean not null default false,
    xsd_validation_status varchar(50),
    user_note varchar(1000),
    created_at datetime(6) not null,
    created_by varchar(255),
    constraint fk_xml_file_revision_file foreign key (xml_file_id) references xml_file(id)
) character set utf8mb4 collate utf8mb4_unicode_ci;
create table xml_file_diff_entry (
    id bigint not null auto_increment primary key,
    revision_id bigint not null,
    xml_file_id bigint not null,
    change_type varchar(50) not null,
    xml_path text,
    old_value longtext,
    new_value longtext,
    display_label varchar(1000),
    constraint fk_xml_file_diff_revision foreign key (revision_id) references xml_file_revision(id),
    constraint fk_xml_file_diff_file foreign key (xml_file_id) references xml_file(id)
) character set utf8mb4 collate utf8mb4_unicode_ci;
create index idx_xml_file_revision_file on xml_file_revision(xml_file_id);
create index idx_xml_file_diff_revision on xml_file_diff_entry(revision_id);
create index idx_xml_file_diff_file on xml_file_diff_entry(xml_file_id);
