-- Sprint 11 v27 - NAV M2M Submitter integration tables for Oracle.
-- UUID fields are stored as varchar2(36 char) to keep the submitter module portable.

create table m2m_submission (
    id varchar2(36 char) primary key,
    interface_type varchar2(40 char) not null,
    bizonylat_tipus varchar2(100 char),
    bizonylat_verzio varchar2(50 char),
    gateway_mode varchar2(20 char) default 'MOCK' not null,
    xml_file_name varchar2(512 char),
    xml_storage_path varchar2(2000 char),
    xml_sha256_hex varchar2(128 char),
    xml_file_size number(19),
    compression varchar2(30 char),
    nav_file_id varchar2(200 char),
    nav_ugy_azonosito varchar2(200 char),
    nav_erkeztetesi_szam varchar2(200 char),
    nav_status varchar2(100 char),
    internal_status varchar2(50 char) not null,
    result_code varchar2(100 char),
    result_message clob,
    message_id varchar2(100 char),
    correlation_id varchar2(100 char),
    created_at timestamp not null,
    updated_at timestamp not null
);

create table m2m_attachment (
    id varchar2(36 char) primary key,
    submission_id varchar2(36 char) not null,
    original_file_name varchar2(512 char) not null,
    storage_path varchar2(2000 char),
    sha256_hex varchar2(128 char),
    file_size number(19),
    nav_file_id varchar2(200 char),
    xml_reference_present number(1) default 0 not null,
    created_at timestamp not null,
    constraint fk_m2m_attachment_submission foreign key (submission_id) references m2m_submission(id)
);

create table m2m_xml_attachment_reference (
    id varchar2(36 char) primary key,
    submission_id varchar2(36 char) not null,
    element_name varchar2(100 char) not null,
    file_id varchar2(200 char),
    file_name varchar2(512 char),
    file_size number(19),
    sequence_no number(10) not null,
    created_at timestamp not null,
    constraint fk_m2m_xml_attachment_reference_submission foreign key (submission_id) references m2m_submission(id)
);

create table m2m_submission_event (
    id varchar2(36 char) primary key,
    submission_id varchar2(36 char) not null,
    event_type varchar2(100 char) not null,
    nav_operation varchar2(100 char),
    request_message_id varchar2(100 char),
    response_code varchar2(100 char),
    response_payload clob,
    request_headers clob,
    request_payload clob,
    response_headers clob,
    config_snapshot clob,
    created_at timestamp not null,
    constraint fk_m2m_event_submission foreign key (submission_id) references m2m_submission(id)
);

create table m2m_proxy_settings (
    id number(19) primary key,
    enabled number(1) default 0 not null,
    proxy_url varchar2(1000 char),
    proxy_port number(10),
    proxy_username varchar2(512 char),
    proxy_password varchar2(2000 char),
    ssl_verification_disabled number(1) default 0 not null,
    trust_store_path varchar2(1000 char),
    trust_store_password varchar2(2000 char),
    trust_store_type varchar2(30 char) default 'JKS',
    updated_at timestamp
);

insert into m2m_proxy_settings (id, enabled, proxy_url, proxy_port, proxy_username, proxy_password, ssl_verification_disabled, trust_store_path, trust_store_password, trust_store_type, updated_at)
values (1, 0, null, null, null, null, 0, null, null, 'JKS', current_timestamp);

create table m2m_submission_file (
    id varchar2(36 char) primary key,
    submission_id varchar2(36 char),
    file_role varchar2(40 char) not null,
    original_file_name varchar2(512 char) not null,
    stored_file_name varchar2(512 char),
    storage_path varchar2(2000 char),
    file_size number(19),
    mime_type varchar2(255 char),
    sha256_hex varchar2(128 char),
    nav_file_id varchar2(200 char),
    upload_status varchar2(80 char),
    sequence_no number(10) default 0 not null,
    external_file_id varchar2(200 char),
    created_at timestamp not null,
    updated_at timestamp,
    constraint fk_m2m_submission_file_submission foreign key (submission_id) references m2m_submission(id)
);

create index idx_m2m_submission_status on m2m_submission(internal_status);
create index idx_m2m_submission_ugy on m2m_submission(nav_ugy_azonosito);
create index idx_m2m_attachment_submission on m2m_attachment(submission_id);
create index idx_m2m_submission_file_submission on m2m_submission_file(submission_id);
create index idx_m2m_submission_file_role on m2m_submission_file(file_role);
create index idx_m2m_submission_file_nav_file on m2m_submission_file(nav_file_id);
