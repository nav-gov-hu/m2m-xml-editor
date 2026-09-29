create table application_release_lock (
    lock_id integer not null primary key,
    application_release varchar(255),
    updated_at timestamp not null,
    updated_by varchar(255) not null
);

insert into application_release_lock(lock_id, application_release, updated_at, updated_by)
values (1, null, current_timestamp, 'migration');
