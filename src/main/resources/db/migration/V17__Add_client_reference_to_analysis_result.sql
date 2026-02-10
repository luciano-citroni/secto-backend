-- Adicionar coluna client_id à tabela analysis_result
ALTER TABLE analysis_result ADD COLUMN client_id UUID;

-- Adicionar chave estrangeira para a tabela client (especificando schema)
ALTER TABLE analysis_result 
ADD CONSTRAINT fk_analysis_result_client 
FOREIGN KEY (client_id) REFERENCES secto.client(id);

-- Criar índice para melhor performance
CREATE INDEX idx_analysis_result_client_id ON analysis_result(client_id);