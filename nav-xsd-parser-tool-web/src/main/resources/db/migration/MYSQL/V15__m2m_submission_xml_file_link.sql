ALTER TABLE m2m_submission ADD COLUMN xml_file_id BIGINT;
CREATE INDEX idx_m2m_submission_xml_file_id ON m2m_submission(xml_file_id);
