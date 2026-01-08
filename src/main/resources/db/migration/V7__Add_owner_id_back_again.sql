SET search_path TO secto, public;

ALTER TABLE company ADD COLUMN owner_id UUID;