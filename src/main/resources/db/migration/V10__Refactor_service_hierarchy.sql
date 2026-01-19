SET search_path TO secto, public;

ALTER TABLE script DROP CONSTRAINT IF EXISTS script_service_sub_type_id_fkey;
-- Note: Constraint names might vary if auto-generated, but V1 specified names explicitly or implicit ones.
-- V1: 
-- FOREIGN KEY (service_type_id) REFERENCES service_type(id) ON DELETE CASCADE
-- FOREIGN KEY (service_sub_type_id) REFERENCES service_sub_type(id) ON DELETE CASCADE

-- Postgres default naming is table_column_fkey.
-- script: script_service_sub_type_id_fkey
-- service_sub_type: service_sub_type_service_type_id_fkey

ALTER TABLE service_sub_type DROP CONSTRAINT IF EXISTS service_sub_type_service_type_id_fkey;

-- 1. Modify Script to point to ServiceType instead of ServiceSubType
ALTER TABLE script DROP COLUMN service_sub_type_id;
ALTER TABLE script ADD COLUMN service_type_id UUID;
ALTER TABLE script ADD CONSTRAINT fk_script_service_type 
    FOREIGN KEY (service_type_id) REFERENCES service_type(id) ON DELETE CASCADE;
CREATE INDEX idx_script_service_type_id ON script(service_type_id);

-- 2. Modify ServiceType to point to ServiceSubType instead of vice versa
-- Remove service_type_id from service_sub_type
ALTER TABLE service_sub_type DROP COLUMN service_type_id;

-- Add service_sub_type_id to service_type
ALTER TABLE service_type ADD COLUMN service_sub_type_id UUID;
ALTER TABLE service_type ADD CONSTRAINT fk_service_type_service_sub_type 
    FOREIGN KEY (service_sub_type_id) REFERENCES service_sub_type(id) ON DELETE CASCADE;
CREATE INDEX idx_service_type_service_sub_type_id ON service_type(service_sub_type_id);
