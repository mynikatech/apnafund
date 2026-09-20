-- 0000-ec2-ai-bootstrap.sql

-- ============================================================
-- Create AI Roles
-- ============================================================

DO $$
BEGIN

  IF NOT EXISTS (
      SELECT 1
      FROM pg_roles
      WHERE rolname = '${app_role}'
  ) THEN
    EXECUTE format(
      'CREATE ROLE %I NOLOGIN INHERIT;',
      '${app_role}'
    );
  END IF;

  IF NOT EXISTS (
      SELECT 1
      FROM pg_roles
      WHERE rolname = '${app_user}'
  ) THEN
    EXECUTE format(
      'CREATE ROLE %I LOGIN PASSWORD %L INHERIT NOCREATEDB NOCREATEROLE NOREPLICATION;',
      '${app_user}',
      '${app_pass}'
    );
  END IF;

  IF NOT EXISTS (
      SELECT 1
      FROM pg_roles
      WHERE rolname = '${deploy_user}'
  ) THEN
    EXECUTE format(
      'CREATE ROLE %I LOGIN PASSWORD %L INHERIT NOCREATEDB NOCREATEROLE NOREPLICATION;',
      '${deploy_user}',
      '${deploy_pass}'
    );
  END IF;

END$$;

-- ============================================================
-- Create Schema
-- ============================================================

DO $$
BEGIN

  IF NOT EXISTS (
      SELECT 1
      FROM pg_namespace
      WHERE nspname = '${target_schema}'
  ) THEN

    EXECUTE format(
      'CREATE SCHEMA %I AUTHORIZATION %I;',
      '${target_schema}',
      '${app_role}'
    );

  ELSE

    RAISE NOTICE
      'Schema % already exists, skipping.',
      '${target_schema}';

  END IF;

END$$;


-- ============================================================
-- Role Membership
-- ============================================================

DO $$
BEGIN

  EXECUTE format(
    'GRANT %I TO %I;',
    '${app_role}',
    '${app_user}'
  );

  EXECUTE format(
    'GRANT %I TO %I;',
    '${app_role}',
    '${deploy_user}'
  );

END$$;


-- ============================================================
-- Schema Security
-- ============================================================

DO $$
BEGIN

  EXECUTE format(
    'REVOKE CREATE ON SCHEMA %I FROM PUBLIC;',
    '${target_schema}'
  );

  EXECUTE format(
    'GRANT USAGE, CREATE ON SCHEMA %I TO %I;',
    '${target_schema}',
    '${app_role}'
  );

END$$;


-- ============================================================
-- Default Privileges for App User
-- ============================================================

DO $$
BEGIN

  EXECUTE format(
    'ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA %I
       GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO %I;',
    '${app_role}',
    '${target_schema}',
    '${app_user}'
  );

  EXECUTE format(
    'ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA %I
       GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO %I;',
    '${app_role}',
    '${target_schema}',
    '${app_user}'
  );

END$$;


-- ============================================================
-- Default Privileges for Deploy User
-- ============================================================

DO $$
BEGIN

  EXECUTE format(
    'ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA %I
       GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO %I;',
    '${deploy_user}',
    '${target_schema}',
    '${app_user}'
  );

  EXECUTE format(
    'ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA %I
       GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO %I;',
    '${deploy_user}',
    '${target_schema}',
    '${app_user}'
  );

END$$;


