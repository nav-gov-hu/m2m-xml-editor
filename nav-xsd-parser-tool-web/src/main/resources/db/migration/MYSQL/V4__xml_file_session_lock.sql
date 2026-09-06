create table if not exists xml_file_lock (
    id bigint not null auto_increment primary key,
    xml_file_id bigint not null,
    locked_by varchar(255) not null,
    locked_at datetime not null,
    lock_expires_at datetime not null,
    lock_token varchar(100) not null,
    status varchar(50) not null,
    created_at datetime not null default current_timestamp,
    updated_at datetime,
    constraint uk_xml_file_lock_file unique (xml_file_id),
    constraint uk_xml_file_lock_token unique (lock_token),
    constraint fk_xml_file_lock_file foreign key (xml_file_id) references xml_file(id)
) character set utf8mb4 collate utf8mb4_unicode_ci;

create index idx_xml_file_lock_status on xml_file_lock(status);
create index idx_xml_file_lock_expires on xml_file_lock(lock_expires_at);

alter table xml_file_session add column read_only boolean not null default false;
alter table xml_file_session add column lock_token varchar(100) null;
alter table xml_file_session add column close_reason varchar(255) null;

create index idx_xml_file_session_created_by_active on xml_file_session(created_by, active);
