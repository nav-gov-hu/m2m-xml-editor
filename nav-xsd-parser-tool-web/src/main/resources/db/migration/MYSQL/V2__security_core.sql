-- Security and authorization schema for MySQL 5.7+.
-- Use utf8mb4 character set/collation for UTF-8 operation.

create table if not exists app_user (
    id bigint not null auto_increment primary key,
    username varchar(255) not null,
    password_hash varchar(255) not null,
    display_name varchar(255),
    email varchar(255),
    enabled boolean not null default true,
    password_change_required boolean not null default false,
    created_at datetime not null default current_timestamp,
    created_by varchar(255),
    updated_at datetime,
    updated_by varchar(255),
    constraint uk_app_user_username unique (username)
) character set utf8mb4 collate utf8mb4_unicode_ci;

create table if not exists app_role (
    id bigint not null auto_increment primary key,
    role_code varchar(100) not null,
    role_name varchar(255) not null,
    constraint uk_app_role_code unique (role_code)
) character set utf8mb4 collate utf8mb4_unicode_ci;

create table if not exists app_user_role (
    user_id bigint not null,
    role_id bigint not null,
    primary key (user_id, role_id),
    constraint fk_app_user_role_user foreign key (user_id) references app_user(id),
    constraint fk_app_user_role_role foreign key (role_id) references app_role(id)
) character set utf8mb4 collate utf8mb4_unicode_ci;

create index idx_app_user_enabled on app_user(enabled);
create index idx_app_user_role_role on app_user_role(role_id);
