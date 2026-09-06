-- NAV XSD Parser Tool initial database schema for MySQL 5.7+.
-- Use utf8mb4 character set/collation for UTF-8 operation.

create table if not exists xml_file (
    id bigint not null auto_increment primary key,
    file_name varchar(500) not null,
    original_file_name varchar(500),
    file_path varchar(2000) not null,
    file_size_bytes bigint not null default 0,
    form_type varchar(100),
    form_version varchar(100),
    root_element varchar(255),
    namespace_uri varchar(1000),
    user_note varchar(1000),
    source_type varchar(50) not null default 'UNKNOWN',
    status varchar(50) not null default 'REGISTERED',
    large_file_mode boolean not null default false,
    archived boolean not null default false,
    archived_at datetime,
    archived_by varchar(255),
    created_at datetime not null default current_timestamp,
    created_by varchar(255),
    updated_at datetime,
    updated_by varchar(255),
    constraint uk_xml_file_file_name unique (file_name)
) character set utf8mb4 collate utf8mb4_unicode_ci;

create index idx_xml_file_created_at on xml_file(created_at);
create index idx_xml_file_status on xml_file(status);
create index idx_xml_file_form on xml_file(form_type, form_version);

create table if not exists xml_file_session (
    id bigint not null auto_increment primary key,
    xml_file_id bigint not null,
    session_id varchar(100) not null,
    active boolean not null default true,
    created_at datetime not null default current_timestamp,
    created_by varchar(255),
    closed_at datetime,
    closed_by varchar(255),
    constraint uk_xml_file_session_session_id unique (session_id),
    constraint fk_xml_file_session_file foreign key (xml_file_id) references xml_file(id)
) character set utf8mb4 collate utf8mb4_unicode_ci;

create index idx_xml_file_session_file on xml_file_session(xml_file_id);
create index idx_xml_file_session_active on xml_file_session(active);

create table if not exists processing_job (
    id bigint not null auto_increment primary key,
    job_id varchar(100) not null,
    xml_file_id bigint,
    job_type varchar(100) not null,
    status varchar(50) not null,
    progress_percent integer,
    progress_message varchar(2000),
    started_at datetime,
    finished_at datetime,
    requested_cancel_at datetime,
    error_message longtext,
    created_at datetime not null default current_timestamp,
    created_by varchar(255),
    updated_at datetime,
    updated_by varchar(255),
    constraint uk_processing_job_job_id unique (job_id),
    constraint fk_processing_job_file foreign key (xml_file_id) references xml_file(id)
) character set utf8mb4 collate utf8mb4_unicode_ci;

create index idx_processing_job_status on processing_job(status);
create index idx_processing_job_file on processing_job(xml_file_id);

create table if not exists operation_audit_log (
    id bigint not null auto_increment primary key,
    operation_type varchar(100) not null,
    xml_file_id bigint,
    job_id varchar(100),
    revision_id bigint,
    username varchar(255),
    result varchar(50),
    message varchar(2000),
    details_json longtext,
    created_at datetime not null default current_timestamp,
    constraint fk_audit_log_file foreign key (xml_file_id) references xml_file(id)
) character set utf8mb4 collate utf8mb4_unicode_ci;

create index idx_audit_log_created_at on operation_audit_log(created_at);
create index idx_audit_log_xml_file on operation_audit_log(xml_file_id);
create index idx_audit_log_operation on operation_audit_log(operation_type);

create table if not exists xpath_validation_request (
    id varchar(36) not null primary key,
    request_id varchar(18) not null unique,
    request_timestamp_utc datetime not null,
    form_name varchar(20) not null,
    form_version varchar(10) not null,
    validator_status varchar(10) not null,
    result_status varchar(10),
    create_result_mode varchar(10) not null,
    session_id varchar(64) not null,
    result longtext,
    result_file_path varchar(1024),
    error_count integer,
    technical_error_message longtext,
    created_at datetime not null,
    created_by varchar(64),
    updated_at datetime not null,
    updated_by varchar(64)
) character set utf8mb4 collate utf8mb4_unicode_ci;

create index idx_xpath_validation_request_created_at on xpath_validation_request(created_at);
create unique index idx_xpath_validation_request_request_id on xpath_validation_request(request_id);

create table if not exists xpath_validation_error (
    id varchar(36) not null primary key,
    request_entity_id varchar(36) not null,
    request_id varchar(18) not null,
    error_code varchar(64),
    error_message longtext,
    severity varchar(32),
    dynamic_page_index varchar(64),
    element_id varchar(255),
    rule_id varchar(64),
    path longtext,
    created_at datetime not null,
    created_by varchar(64)
) character set utf8mb4 collate utf8mb4_unicode_ci;

create index idx_xpath_validation_error_request_entity_id on xpath_validation_error(request_entity_id);
create index idx_xpath_validation_error_request_id on xpath_validation_error(request_id);

create table if not exists xpath_validation_request_journal (
    id varchar(36) not null primary key,
    request_entity_id varchar(36) not null,
    request_id varchar(18) not null,
    event_timestamp_utc datetime not null,
    old_validator_status varchar(10),
    new_validator_status varchar(10),
    old_result_status varchar(10),
    new_result_status varchar(10),
    message longtext,
    session_id varchar(64),
    created_at datetime not null,
    created_by varchar(64)
) character set utf8mb4 collate utf8mb4_unicode_ci;

create index idx_xpath_validation_request_journal_request_entity_id on xpath_validation_request_journal(request_entity_id);
create index idx_xpath_validation_request_journal_request_id on xpath_validation_request_journal(request_id);
