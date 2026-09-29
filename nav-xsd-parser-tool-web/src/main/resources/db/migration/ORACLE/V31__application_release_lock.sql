create table application_release_lock (
    lock_id number(10) not null primary key,
    application_release varchar2(255 char),
    updated_at timestamp not null,
    updated_by varchar2(255 char) not null
);

insert into application_release_lock(lock_id, application_release, updated_at, updated_by)
values (1, null, current_timestamp, 'migration');
