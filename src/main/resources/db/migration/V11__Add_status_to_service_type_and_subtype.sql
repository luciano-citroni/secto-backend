SET search_path TO secto, public;

ALTER TABLE service_type ADD COLUMN status BOOLEAN DEFAULT TRUE;
ALTER TABLE service_sub_type ADD COLUMN status BOOLEAN DEFAULT TRUE;
