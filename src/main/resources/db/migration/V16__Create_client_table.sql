-- Create client table
CREATE TABLE secto.client (
    id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    surname VARCHAR(100) NOT NULL,
    birth_date DATE,
    cpf VARCHAR(11) UNIQUE,
    rg VARCHAR(20),
    address VARCHAR(255),
    status BOOLEAN DEFAULT TRUE,
    company_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_client PRIMARY KEY (id),
    CONSTRAINT fk_client_company FOREIGN KEY (company_id) REFERENCES secto.company(id)
);

CREATE INDEX idx_client_company_id ON secto.client(company_id);
CREATE INDEX idx_client_cpf ON secto.client(cpf);
CREATE INDEX idx_client_status ON secto.client(status);