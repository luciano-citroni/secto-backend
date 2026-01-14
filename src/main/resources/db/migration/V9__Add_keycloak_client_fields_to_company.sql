SET search_path TO secto, public;

-- Adicionar campos client_id e client_secret na tabela company
ALTER TABLE company ADD COLUMN client_id TEXT;
ALTER TABLE company ADD COLUMN client_secret TEXT;

-- Criar índices para melhor performance
CREATE INDEX idx_company_client_id ON company(client_id);