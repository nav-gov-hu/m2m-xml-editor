-- M2M response field storage and attachment validity metadata.
alter table m2m_submission add column nav_befogadas_idopontja timestamp;
alter table m2m_submission add column nav_megjegyzes text;
alter table m2m_submission add column nav_validacios_hibak text;
alter table m2m_submission add column nav_response_body text;
alter table m2m_submission add column nav_http_status integer;
alter table m2m_submission add column submission_started_at timestamp;
alter table m2m_submission add column submission_finished_at timestamp;
alter table m2m_submission add column submission_duration_ms bigint;
alter table m2m_attachment add column nav_uploaded_at timestamp;
alter table m2m_attachment add column nav_expires_at timestamp;
alter table m2m_attachment add column nav_last_refreshed_at timestamp;
alter table m2m_attachment add column nav_upload_result_code varchar(100);
alter table m2m_attachment add column nav_upload_result_message text;
