create table if not exists system_secret (
 secret_key varchar(255) primary key,
 encrypted_value longtext not null,
 encryption_version int not null,
 updated_at timestamp(6) not null,
 updated_by varchar(255)
) engine=InnoDB;
create table if not exists trusted_certificate (
 id bigint not null auto_increment primary key,
 certificate_alias varchar(255) not null,
 subject_dn text not null,
 issuer_dn text not null,
 serial_number varchar(255) not null,
 sha256_fingerprint varchar(128) not null unique,
 valid_from timestamp(6) not null,
 valid_until timestamp(6) not null,
 source_host varchar(512),
 source_port int,
 status varchar(64) not null,
 certificate_der longblob not null,
 created_at timestamp(6) not null,
 created_by varchar(255)
) engine=InnoDB;
