-- M2M response field storage and attachment validity metadata.
alter table m2m_submission add (nav_befogadas_idopontja timestamp);
alter table m2m_submission add (nav_megjegyzes clob);
alter table m2m_submission add (nav_validacios_hibak clob);
alter table m2m_submission add (nav_response_body clob);
alter table m2m_submission add (nav_http_status integer);
alter table m2m_submission add (submission_started_at timestamp);
alter table m2m_submission add (submission_finished_at timestamp);
alter table m2m_submission add (submission_duration_ms number(19));
alter table m2m_attachment add (nav_uploaded_at timestamp);
alter table m2m_attachment add (nav_expires_at timestamp);
alter table m2m_attachment add (nav_last_refreshed_at timestamp);
alter table m2m_attachment add (nav_upload_result_code varchar2(100 char));
alter table m2m_attachment add (nav_upload_result_message clob);
