DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_namespace WHERE nspname = '${schemaName}'
  ) THEN
    EXECUTE format('CREATE SCHEMA %I', '${schemaName}');
  END IF;
END$$;