create table xml_file_lock_release_request (
    id bigint auto_increment primary key,
    xml_file_id bigint not null,
    requester_username varchar(255) not null,
    requester_browser_session_id varchar(100),
    owner_username varchar(255) not null,
    owner_browser_session_id varchar(100),
    status varchar(50) not null,
    message varchar(1000),
    response_message varchar(1000),
    requested_at timestamp not null,
    responded_at timestamp,
    closed_by varchar(255),
    force_closed_at timestamp,
    constraint fk_lock_release_request_xml_file foreign key (xml_file_id) references xml_file(id)
);

create index idx_lock_release_owner_status on xml_file_lock_release_request(owner_username, owner_browser_session_id, status);
create index idx_lock_release_requester on xml_file_lock_release_request(requester_username, requester_browser_session_id);
