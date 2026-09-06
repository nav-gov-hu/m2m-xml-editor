create table if not exists github_template_repository (
    repository_name varchar(255) primary key,
    description_text varchar(2000),
    repository_updated_at timestamp null,
    repository_url varchar(1000),
    archived_flag boolean not null,
    last_synced_at timestamp not null
);
create table if not exists github_template_release (
    id bigint auto_increment primary key,
    repository_name varchar(255) not null,
    release_tag varchar(255) not null,
    last_synced_at timestamp not null,
    constraint uk_github_template_release_repo_tag unique (repository_name, release_tag),
    constraint fk_github_template_release_repo foreign key (repository_name) references github_template_repository(repository_name) on delete cascade
);
create index idx_github_template_release_repo on github_template_release(repository_name);
create table if not exists github_template_sync_state (
    id bigint primary key,
    organization_name varchar(255) not null,
    last_checked_at timestamp null,
    last_successful_sync_at timestamp null,
    repository_count integer not null
);
