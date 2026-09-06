-- Sprint 11 v40 - M2M submission terminal/intermediate status and polling metadata.

alter table m2m_submission add (
    m2m_submit_marked_at timestamp,
    m2m_submitted_at timestamp,
    m2m_finalized_at timestamp,
    m2m_next_poll_at timestamp,
    m2m_last_poll_at timestamp,
    m2m_poll_attempts number(10,0) default 0,
    m2m_terminal number(1,0) default 0 not null,
    m2m_resubmittable number(1,0) default 1 not null
);

create index idx_m2m_submission_poll on m2m_submission(m2m_terminal, m2m_next_poll_at, internal_status);
