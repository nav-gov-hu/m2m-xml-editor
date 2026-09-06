alter table m2m_submission add (nav_validacio_payload_sha256 varchar2(64));
alter table m2m_submission add (fast_track_submission_used number(1) default 0 not null);
