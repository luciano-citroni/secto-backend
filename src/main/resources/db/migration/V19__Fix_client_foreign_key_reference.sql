-- Corrigir foreign key se já existe com referência incorreta
DO $$
BEGIN
    -- Remover constraint se existir com referência incorreta
    IF EXISTS (SELECT 1 FROM information_schema.table_constraints 
               WHERE constraint_name = 'fk_analysis_result_client' 
               AND table_name = 'analysis_result') THEN
        ALTER TABLE analysis_result DROP CONSTRAINT fk_analysis_result_client;
    END IF;
    
    -- Recriar com referência correta ao schema
    ALTER TABLE analysis_result 
    ADD CONSTRAINT fk_analysis_result_client 
    FOREIGN KEY (client_id) REFERENCES secto.client(id);
END $$;