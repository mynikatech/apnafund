-- 0000-ec2-dev-bootstrap.sql (corrected for Liquibase ${} substitution)
-- 1) Create roles (NOLOGIN, APP, DEPLOY)
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '${app_role}') THEN
    EXECUTE format('CREATE ROLE %I NOLOGIN INHERIT;', '${app_role}');
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '${app_user}') THEN
    EXECUTE format(
      'CREATE ROLE %I LOGIN PASSWORD %L INHERIT NOCREATEDB NOCREATEROLE NOREPLICATION;',
      '${app_user}', '${app_pass}'
    );
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '${deploy_user}') THEN
    EXECUTE format(
      'CREATE ROLE %I LOGIN PASSWORD %L INHERIT NOCREATEDB NOCREATEROLE NOREPLICATION;',
      '${deploy_user}', '${deploy_pass}'
    );
  END IF;
END$$;

-- 2) Create schema owned by app role
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = '${target_schema}') THEN
    -- use quoted identifier to preserve case if needed
    EXECUTE format('CREATE SCHEMA %I AUTHORIZATION %I;', '${target_schema}', '${app_role}');
  ELSE
    RAISE NOTICE 'Schema % already exists, skipping.', '${target_schema}';
  END IF;
END$$;

-- 3) Assign role memberships
DO $$
BEGIN
  EXECUTE format('GRANT %I TO %I;', '${app_role}', '${app_user}');
  EXECUTE format('GRANT %I TO %I;', '${app_role}', '${deploy_user}');
END$$;

-- 4) Lock down PUBLIC and grant schema-level privileges
DO $$
BEGIN
  EXECUTE format('REVOKE CREATE ON SCHEMA %I FROM PUBLIC;', '${target_schema}');
  EXECUTE format('GRANT USAGE, CREATE ON SCHEMA %I TO %I;', '${target_schema}', '${app_role}');
END$$;


-- 5) Default privileges
DO $$
BEGIN
  EXECUTE format(
    'ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA %I
       GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO %I;',
   '${app_role}', '${target_schema}', '${app_user}'
  );
  EXECUTE format(
    'ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA %I
       GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO %I;',
    '${app_role}', '${target_schema}', '${app_user}'
  );
END$$;

DO $$
BEGIN
  EXECUTE format(
    'ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA %I
     GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO %I;',
    '${deploy_user}', '${target_schema}', '${app_user}'
  );

  EXECUTE format(
    'ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA %I
     GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO %I;',
    '${deploy_user}', '${target_schema}', '${app_user}'
  );
END$$;

