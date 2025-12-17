CREATE SCHEMA IF NOT EXISTS secto;

SET search_path TO secto, public;

-- Tabela principal de tipos de serviço
CREATE TABLE service_type (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabela de subtipos de serviço
CREATE TABLE service_sub_type (
    id UUID PRIMARY KEY,
    service_type_id UUID NOT NULL,
    name TEXT NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (service_type_id) REFERENCES service_type(id) ON DELETE CASCADE,
    UNIQUE(service_type_id, name)
);

-- Tabela de scripts de serviço
CREATE TABLE script (
    id UUID PRIMARY KEY,
    service_sub_type_id UUID NOT NULL,
    name TEXT NOT NULL,
    status BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (service_sub_type_id) REFERENCES service_sub_type(id) ON DELETE CASCADE,
    UNIQUE(service_sub_type_id, name)
);

CREATE TABLE script_item (
    id UUID PRIMARY KEY,
    script_id UUID NOT NULL,
    question TEXT NOT NULL,
    answer TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (script_id) REFERENCES script(id) ON DELETE CASCADE
);

-- Índices para melhor performance
CREATE INDEX idx_service_sub_type_service_type_id ON service_sub_type(service_type_id);
CREATE INDEX idx_script_service_sub_type_id ON script(service_sub_type_id);
CREATE INDEX idx_script_status ON script(status);
CREATE INDEX idx_script_item_script_id ON script_item(script_id);