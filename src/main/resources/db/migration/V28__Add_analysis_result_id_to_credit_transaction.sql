ALTER TABLE secto.credit_transaction
    ADD COLUMN IF NOT EXISTS analysis_result_id UUID;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_credit_transaction_analysis_result'
    ) THEN
        RETURN;
    END IF;

    IF to_regclass('secto.analysis_result') IS NOT NULL THEN
        ALTER TABLE secto.credit_transaction
            ADD CONSTRAINT fk_credit_transaction_analysis_result
            FOREIGN KEY (analysis_result_id) REFERENCES secto.analysis_result(id)
            ON DELETE SET NULL;
    ELSIF to_regclass('public.analysis_result') IS NOT NULL THEN
        ALTER TABLE secto.credit_transaction
            ADD CONSTRAINT fk_credit_transaction_analysis_result
            FOREIGN KEY (analysis_result_id) REFERENCES public.analysis_result(id)
            ON DELETE SET NULL;
    ELSE
        RAISE EXCEPTION 'Tabela analysis_result não encontrada em secto nem public';
    END IF;
END $$;
