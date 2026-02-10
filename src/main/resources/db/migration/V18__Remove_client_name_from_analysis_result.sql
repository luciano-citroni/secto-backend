-- Remover coluna client_name da tabela analysis_result
-- Agora usamos apenas o relacionamento com a tabela client
ALTER TABLE analysis_result DROP COLUMN client_name;