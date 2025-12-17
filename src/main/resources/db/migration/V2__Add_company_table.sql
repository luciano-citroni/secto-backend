SET search_path TO secto, public;

-- Limpar dados existentes para permitir adicionar colunas NOT NULL de FK sem erros
TRUNCATE TABLE script_item, script, service_sub_type, service_type CASCADE;

-- Tabela de empresas (Company)
CREATE TABLE company (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Atualizar service_type
ALTER TABLE service_type ADD COLUMN company_id UUID NOT NULL;
ALTER TABLE service_type ADD CONSTRAINT fk_service_type_company FOREIGN KEY (company_id) REFERENCES company(id) ON DELETE CASCADE;
CREATE INDEX idx_service_type_company_id ON service_type(company_id);

-- Atualizar service_sub_type
ALTER TABLE service_sub_type ADD COLUMN company_id UUID NOT NULL;
ALTER TABLE service_sub_type ADD CONSTRAINT fk_service_sub_type_company FOREIGN KEY (company_id) REFERENCES company(id) ON DELETE CASCADE;
CREATE INDEX idx_service_sub_type_company_id ON service_sub_type(company_id);

-- Atualizar script
ALTER TABLE script ADD COLUMN company_id UUID NOT NULL;
ALTER TABLE script ADD CONSTRAINT fk_script_company FOREIGN KEY (company_id) REFERENCES company(id) ON DELETE CASCADE;
CREATE INDEX idx_script_company_id ON script(company_id);
