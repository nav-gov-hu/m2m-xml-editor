create table github_proxy_settings (
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
