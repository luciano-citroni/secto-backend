SET search_path TO secto, public;

ALTER TABLE analysis_result ADD COLUMN company_id UUID;

ALTER TABLE analysis_result ADD CONSTRAINT fk_analysis_result_company 
    FOREIGN KEY (company_id) REFERENCES company(id) ON DELETE CASCADE;

CREATE INDEX idx_analysis_result_company_id ON analysis_result(company_id);