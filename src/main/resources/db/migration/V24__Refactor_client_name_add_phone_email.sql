-- Combinar name + surname em full_name
ALTER TABLE secto.client ADD COLUMN full_name VARCHAR(200);

-- Migrar dados existentes: concatenar name + surname
UPDATE secto.client SET full_name = CONCAT(name, ' ', surname) WHERE full_name IS NULL;

-- Tornar full_name NOT NULL após migração
ALTER TABLE secto.client ALTER COLUMN full_name SET NOT NULL;

-- Remover colunas antigas
ALTER TABLE secto.client DROP COLUMN name;
ALTER TABLE secto.client DROP COLUMN surname;

-- Adicionar novos campos
ALTER TABLE secto.client ADD COLUMN phone VARCHAR(20);
ALTER TABLE secto.client ADD COLUMN email VARCHAR(255);
