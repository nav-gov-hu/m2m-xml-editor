alter table xpath_validation_request add xml_file_id number(19);
alter table xpath_validation_request add xml_file_session_id varchar2(100 char);
alter table xpath_validation_request add processing_job_id varchar2(100 char);
create index idx_xpath_validation_request_xml_file on xpath_validation_request(xml_file_id);
