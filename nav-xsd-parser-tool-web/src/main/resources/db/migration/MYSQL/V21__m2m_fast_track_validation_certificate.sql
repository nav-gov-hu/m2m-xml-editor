alter table m2m_submission add column nav_validacio_payload_sha256 varchar(64);
alter table m2m_submission add column fast_track_submission_used boolean not null default false;
