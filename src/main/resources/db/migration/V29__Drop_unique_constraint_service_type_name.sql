DO $$
BEGIN
    IF to_regclass('secto.service_type') IS NOT NULL THEN
        IF EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'service_type_name_key'
              AND conrelid = 'secto.service_type'::regclass
        ) THEN
            ALTER TABLE secto.service_type
                DROP CONSTRAINT service_type_name_key;
        END IF;
    END IF;

    IF to_regclass('public.service_type') IS NOT NULL THEN
        IF EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'service_type_name_key'
              AND conrelid = 'public.service_type'::regclass
        ) THEN
            ALTER TABLE public.service_type
                DROP CONSTRAINT service_type_name_key;
        END IF;
    END IF;
END $$;
