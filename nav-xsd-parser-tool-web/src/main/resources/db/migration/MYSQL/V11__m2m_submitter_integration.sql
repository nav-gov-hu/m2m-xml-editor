-- Sprint 11 v27 - NAV M2M Submitter integration tables.
-- UUID fields are stored as varchar(36) to keep the submitter module portable across supported databases.

create table if not exists m2m_submission (
    id varchar(36) primary key,
    interface_type varchar(40) not null,
    bizonylat_tipus varchar(100),
    bizonylat_verzio varchar(50),
    gateway_mode varchar(20) default 'MOCK' not null,
    xml_file_name varchar(512),
    xml_storage_path varchar(2000),
    xml_sha256_hex varchar(128),
    xml_file_size bigint,
    compression varchar(30),
    nav_file_id varchar(200),
    nav_ugy_azonosito varchar(200),
    nav_erkeztetesi_szam varchar(200),
    nav_status varchar(100),
    internal_status varchar(50) not null,
    result_code varchar(100),
    result_message longtext,
    message_id varchar(100),
    correlation_id varchar(100),
    created_at datetime not null,
    updated_at datetime not null
);

create table if not exists m2m_attachment (
    id varchar(36) primary key,
    submission_id varchar(36) not null,
    original_file_name varchar(512) not null,
    storage_path varchar(2000),
    sha256_hex varchar(128),
    file_size bigint,
    nav_file_id varchar(200),
    xml_reference_present boolean not null default false,
    created_at datetime not null,
    constraint fk_m2m_attachment_submission foreign key (submission_id) references m2m_submission(id)
);

create table if not exists m2m_xml_attachment_reference (
    id varchar(36) primary key,
    submission_id varchar(36) not null,
    element_name varchar(100) not null,
    file_id varchar(200),
    file_name varchar(512),
    file_size bigint,
    sequence_no integer not null,
    created_at datetime not null,
    constraint fk_m2m_xml_attachment_reference_submission foreign key (submission_id) references m2m_submission(id)
);

create table if not exists m2m_submission_event (
    id varchar(36) primary key,
    submission_id varchar(36) not null,
    event_type varchar(100) not null,
    nav_operation varchar(100),
    request_message_id varchar(100),
    response_code varchar(100),
    response_payload longtext,
    request_headers longtext,
    request_payload longtext,
    response_headers longtext,
    config_snapshot longtext,
    created_at datetime not null,
    constraint fk_m2m_event_submission foreign key (submission_id) references m2m_submission(id)
);

create table if not exists m2m_proxy_settings (
    id bigint primary key,
    enabled boolean not null default false,
    proxy_url varchar(1000),
    proxy_port integer,
    proxy_username varchar(512),
    proxy_password varchar(2000),
    ssl_verification_disabled boolean not null default false,
    trust_store_path varchar(1000),
    trust_store_password varchar(2000),
    trust_store_type varchar(30) default 'JKS',
    updated_at datetime
);

insert into m2m_proxy_settings (id, enabled, proxy_url, proxy_port, proxy_username, proxy_password, ssl_verification_disabled, trust_store_path, trust_store_password, trust_store_type, updated_at)
select 1, false, null, null, null, null, false, null, null, 'JKS', current_timestamp
where not exists (select 1 from m2m_proxy_settings where id = 1);

create table if not exists m2m_submission_file (
    id varchar(36) primary key,
    submission_id varchar(36),
    file_role varchar(40) not null,
    original_file_name varchar(512) not null,
    stored_file_name varchar(512),
    storage_path varchar(2000),
    file_size bigint,
    mime_type varchar(255),
    sha256_hex varchar(128),
    nav_file_id varchar(200),
    upload_status varchar(80),
    sequence_no integer not null default 0,
    external_file_id varchar(200),
    created_at datetime not null,
    updated_at datetime,
    constraint fk_m2m_submission_file_submission foreign key (submission_id) references m2m_submission(id)
);

create index idx_m2m_submission_status on m2m_submission(internal_status);
create index idx_m2m_submission_ugy on m2m_submission(nav_ugy_azonosito);
create index idx_m2m_attachment_submission on m2m_attachment(submission_id);
create index idx_m2m_submission_file_submission on m2m_submission_file(submission_id);
create index idx_m2m_submission_file_role on m2m_submission_file(file_role);
create index idx_m2m_submission_file_nav_file on m2m_submission_file(nav_file_id);
