ALTER TABLE secto.credit_transaction
    ADD COLUMN analysis_result_id UUID;

ALTER TABLE secto.credit_transaction
    ADD CONSTRAINT fk_credit_transaction_analysis_result
    FOREIGN KEY (analysis_result_id) REFERENCES secto.analysis_result(id)
    ON DELETE SET NULL;
