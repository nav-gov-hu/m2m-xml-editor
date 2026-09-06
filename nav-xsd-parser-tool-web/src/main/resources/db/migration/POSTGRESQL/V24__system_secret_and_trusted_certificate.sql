create table if not exists system_secret (
 secret_key varchar(255) primary key,
 encrypted_value text not null,
 encryption_version integer not null,
 updated_at timestamp with time zone not null,
 updated_by varchar(255)
);
create table if not exists trusted_certificate (
 id bigserial primary key,
 certificate_alias varchar(255) not null,
 subject_dn text not null,
 issuer_dn text not null,
 serial_number varchar(255) not null,
 sha256_fingerprint varchar(128) not null unique,
 valid_from timestamp with time zone not null,
 valid_until timestamp with time zone not null,
 source_host varchar(512),
 source_port integer,
 status varchar(64) not null,
 certificate_der bytea not null,
 created_at timestamp with time zone not null,
 created_by varchar(255)
);
