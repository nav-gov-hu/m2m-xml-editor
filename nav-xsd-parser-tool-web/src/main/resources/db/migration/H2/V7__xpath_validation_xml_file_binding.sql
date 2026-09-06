alter table xpath_validation_request add column xml_file_id bigint;
alter table xpath_validation_request add column xml_file_session_id varchar(100);
alter table xpath_validation_request add column processing_job_id varchar(100);
create index idx_xpath_validation_request_xml_file on xpath_validation_request(xml_file_id);
