alter table xml_file_lock add column lock_browser_session_id varchar(100) null;
alter table xml_file_lock add column lock_client_ip varchar(100) null;
alter table xml_file_lock add column lock_user_agent varchar(1000) null;

alter table xml_file_session add column browser_session_id varchar(100) null;
alter table xml_file_session add column client_ip varchar(100) null;
alter table xml_file_session add column user_agent varchar(1000) null;

update xml_file_lock
set status = 'EXPIRED', updated_at = current_timestamp
where status = 'ACTIVE' and (lock_browser_session_id is null or lock_browser_session_id = '');

update xml_file_session
set active = false, closed_at = current_timestamp, close_reason = 'Rendszerfrissítés miatt lezárt böngésző-munkamenet alapú zárolás bevezetésekor.'
where active = true and (browser_session_id is null or browser_session_id = '');

create index idx_xml_file_lock_browser_session on xml_file_lock(lock_browser_session_id);
create index idx_xml_file_session_browser_session on xml_file_session(browser_session_id);
