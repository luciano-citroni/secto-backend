-- Adicionar campos de representante ao cliente
ALTER TABLE secto.client ADD COLUMN representative_name VARCHAR(200);
ALTER TABLE secto.client ADD COLUMN representative_cpf VARCHAR(11);
