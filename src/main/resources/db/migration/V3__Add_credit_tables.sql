SET search_path TO secto, public;

-- Tabela de créditos da empresa
CREATE TABLE company_credit (
    id UUID PRIMARY KEY,
    credit_amount NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabela de transações de crédito
CREATE TABLE credit_transaction (
    id UUID PRIMARY KEY,
    company_credit_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (company_credit_id) REFERENCES company_credit(id) ON DELETE CASCADE
);

-- Atualizar tabela company com novos campos
ALTER TABLE company ADD COLUMN owner_id UUID;
ALTER TABLE company ADD COLUMN company_credit_id UUID;

-- Adicionar constraints
ALTER TABLE company ADD CONSTRAINT fk_company_company_credit FOREIGN KEY (company_credit_id) REFERENCES company_credit(id) ON DELETE SET NULL;

-- Índices
CREATE INDEX idx_credit_transaction_company_credit_id ON credit_transaction(company_credit_id);
CREATE INDEX idx_company_company_credit_id ON company(company_credit_id);
