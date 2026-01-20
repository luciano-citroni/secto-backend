ALTER TABLE analysis_result ADD COLUMN script_id UUID;
ALTER TABLE analysis_result ADD CONSTRAINT fk_analysis_result_script FOREIGN KEY (script_id) REFERENCES secto.script(id);

-- Optional: Drop the text columns if they existed (safeguard)
-- ALTER TABLE public.analysis_result DROP COLUMN IF EXISTS script_name;
-- ALTER TABLE public.analysis_result DROP COLUMN IF EXISTS service_type_name;
-- ALTER TABLE public.analysis_result DROP COLUMN IF EXISTS service_sub_type_name;
