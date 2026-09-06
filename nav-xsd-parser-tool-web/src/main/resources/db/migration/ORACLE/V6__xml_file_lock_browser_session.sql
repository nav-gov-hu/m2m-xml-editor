alter table xml_file_lock add (lock_browser_session_id varchar2(100));
alter table xml_file_lock add (lock_client_ip varchar2(100));
alter table xml_file_lock add (lock_user_agent varchar2(1000));

alter table xml_file_session add (browser_session_id varchar2(100));
alter table xml_file_session add (client_ip varchar2(100));
alter table xml_file_session add (user_agent varchar2(1000));

update xml_file_lock
set status = 'EXPIRED', updated_at = current_timestamp
where status = 'ACTIVE' and (lock_browser_session_id is null or lock_browser_session_id = '');

update xml_file_session
set active = 0, closed_at = current_timestamp, close_reason = 'Rendszerfrissítés miatt lezárt böngésző-munkamenet alapú zárolás bevezetésekor.'
where active = 1 and (browser_session_id is null or browser_session_id = '');

create index idx_xml_file_lock_browser_session on xml_file_lock(lock_browser_session_id);
create index idx_xml_file_session_browser_session on xml_file_session(browser_session_id);
