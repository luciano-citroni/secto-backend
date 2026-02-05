CREATE TABLE secto.credit_packages (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    identifier VARCHAR(50) NOT NULL UNIQUE,
    price_in_cents BIGINT NOT NULL,
    credits INTEGER NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

INSERT INTO secto.credit_packages (id, name, identifier, price_in_cents, credits, active) VALUES 
(gen_random_uuid(), 'Pacote Básico', 'basic', 5000, 50, true),
(gen_random_uuid(), 'Pacote Profissional', 'pro', 10000, 110, true),
(gen_random_uuid(), 'Pacote Enterprise', 'enterprise', 20000, 240, true),
(gen_random_uuid(), 'Teste 1 Real', 'test_one', 100, 1, true);
