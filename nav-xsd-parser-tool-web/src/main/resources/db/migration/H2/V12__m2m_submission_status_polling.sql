-- Sprint 11 v40 - M2M submission terminal/intermediate status and polling metadata.

alter table m2m_submission add column m2m_submit_marked_at timestamp;
alter table m2m_submission add column m2m_submitted_at timestamp;
alter table m2m_submission add column m2m_finalized_at timestamp;
alter table m2m_submission add column m2m_next_poll_at timestamp;
alter table m2m_submission add column m2m_last_poll_at timestamp;
alter table m2m_submission add column m2m_poll_attempts integer default 0;
alter table m2m_submission add column m2m_terminal boolean not null default false;
alter table m2m_submission add column m2m_resubmittable boolean not null default true;

create index if not exists idx_m2m_submission_poll on m2m_submission(m2m_terminal, m2m_next_poll_at, internal_status);
