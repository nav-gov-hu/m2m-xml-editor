create table if not exists github_proxy_settings (
    id bigint primary key,
    enabled boolean not null,
    proxy_url varchar(1000),
    proxy_port integer,
    proxy_username varchar(512),
    proxy_password varchar(2000),
    ssl_verification_disabled boolean not null,
    trust_store_path varchar(1000),
    trust_store_password varchar(2000),
    trust_store_type varchar(30),
    updated_at timestamp
);


