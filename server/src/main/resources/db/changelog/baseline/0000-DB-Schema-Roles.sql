
-- Helper: create NOLOGIN role if missing
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'apnafund_app_dev') THEN
    CREATE ROLE apnafund_app_dev NOLOGIN INHERIT;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'apnafund_app_prod') THEN
    CREATE ROLE apnafund_app_prod NOLOGIN INHERIT;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'apnafund_app_test') THEN
    CREATE ROLE apnafund_app_test NOLOGIN INHERIT;
  END IF;
END$$;

-- Helper: create LOGIN user role if missing (passwords below!)
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'apnafund_dev_user') THEN
    CREATE ROLE apnafund_dev_user
      LOGIN INHERIT NOCREATEDB NOCREATEROLE NOREPLICATION
      PASSWORD 'CHANGEME_DEV';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'apnafund_prod_user') THEN
    CREATE ROLE apnafund_prod_user
      LOGIN INHERIT NOCREATEDB NOCREATEROLE NOREPLICATION
      PASSWORD 'CHANGEME_PROD';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'apnafund_test_user') THEN
    CREATE ROLE apnafund_test_user
      LOGIN INHERIT NOCREATEDB NOCREATEROLE NOREPLICATION
      PASSWORD 'CHANGEME_TEST';
  END IF;
END$$;

-- Membership: app users inherit from their environment app role
GRANT apnafund_app_dev  TO apnafund_dev_user;
GRANT apnafund_app_prod TO apnafund_prod_user;
GRANT apnafund_app_test TO apnafund_test_user;

DO $$
DECLARE
  db text := current_database();
BEGIN
  EXECUTE format(
    'ALTER ROLE %I IN DATABASE %I SET search_path = %I, public',
    'apnafund_dev_user', db, 'ApnaFundDev'
  );
  EXECUTE format(
    'ALTER ROLE %I IN DATABASE %I SET search_path = %I, public',
    'apnafund_prod_user', db, 'ApnaFund'
  );
  EXECUTE format(
    'ALTER ROLE %I IN DATABASE %I SET search_path = %I, public',
    'apnafund_test_user', db, 'ApnaFundTest'
  );
END$$;

-- Optional: keep their search_path tidy (current DB only)
DO $$
DECLARE
  db text := current_database();
BEGIN
  EXECUTE format(
    'GRANT CONNECT ON DATABASE %I TO %I, %I, %I',
    db, 'apnafund_dev_user', 'apnafund_prod_user', 'apnafund_test_user'
  );
END$$;


-- ============================
-- DEV SCHEMA
-- ============================

-- Create schema and set owner to the NOLOGIN app role
CREATE SCHEMA IF NOT EXISTS "ApnaFundDev" AUTHORIZATION apnafund_app_dev;

-- Safety: don’t let PUBLIC create in this schema
REVOKE CREATE ON SCHEMA "ApnaFundDev" FROM PUBLIC;

-- App role should be able to create objects
GRANT USAGE, CREATE ON SCHEMA "ApnaFundDev" TO apnafund_app_dev;

-- App login also needs usage; creation can be via membership of app role
GRANT USAGE ON SCHEMA "ApnaFundDev" TO apnafund_dev_user;

-- Default privileges for future objects (must be issued by the schema owner!)
ALTER DEFAULT PRIVILEGES FOR ROLE apnafund_app_dev IN SCHEMA "ApnaFundDev"
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO apnafund_app_dev;

ALTER DEFAULT PRIVILEGES FOR ROLE apnafund_app_dev IN SCHEMA "ApnaFundDev"
  GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO apnafund_app_dev;

-- Also let the login user read/write via membership (or grant directly if you prefer)
ALTER DEFAULT PRIVILEGES FOR ROLE apnafund_app_dev IN SCHEMA "ApnaFundDev"
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO apnafund_dev_user;

ALTER DEFAULT PRIVILEGES FOR ROLE apnafund_app_dev IN SCHEMA "ApnaFundDev"
  GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO apnafund_dev_user;


-- ============================
-- PROD SCHEMA
-- ============================

CREATE SCHEMA IF NOT EXISTS "ApnaFund" AUTHORIZATION apnafund_app_prod;
REVOKE CREATE ON SCHEMA "ApnaFund" FROM PUBLIC;

GRANT USAGE, CREATE ON SCHEMA "ApnaFund" TO apnafund_app_prod;
GRANT USAGE ON SCHEMA "ApnaFund" TO apnafund_prod_user;

ALTER DEFAULT PRIVILEGES FOR ROLE apnafund_app_prod IN SCHEMA "ApnaFund"
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO apnafund_app_prod;

ALTER DEFAULT PRIVILEGES FOR ROLE apnafund_app_prod IN SCHEMA "ApnaFund"
  GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO apnafund_app_prod;

ALTER DEFAULT PRIVILEGES FOR ROLE apnafund_app_prod IN SCHEMA "ApnaFund"
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO apnafund_prod_user;

ALTER DEFAULT PRIVILEGES FOR ROLE apnafund_app_prod IN SCHEMA "ApnaFund"
  GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO apnafund_prod_user;


-- ============================
-- TEST SCHEMA
-- ============================

CREATE SCHEMA IF NOT EXISTS "ApnaFundTest" AUTHORIZATION apnafund_app_test;
REVOKE CREATE ON SCHEMA "ApnaFundTest" FROM PUBLIC;

GRANT USAGE, CREATE ON SCHEMA "ApnaFundTest" TO apnafund_app_test;
GRANT USAGE ON SCHEMA "ApnaFundTest" TO apnafund_test_user;

ALTER DEFAULT PRIVILEGES FOR ROLE apnafund_app_test IN SCHEMA "ApnaFundTest"
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO apnafund_app_test;

ALTER DEFAULT PRIVILEGES FOR ROLE apnafund_app_test IN SCHEMA "ApnaFundTest"
  GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO apnafund_app_test;

ALTER DEFAULT PRIVILEGES FOR ROLE apnafund_app_test IN SCHEMA "ApnaFundTest"
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO apnafund_test_user;

ALTER DEFAULT PRIVILEGES FOR ROLE apnafund_app_test IN SCHEMA "ApnaFundTest"
  GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO apnafund_test_user;



 DO $$
 DECLARE r record;
 BEGIN
   FOR r IN
     SELECT schemaname, tablename
     FROM   pg_tables
     WHERE  schemaname = 'ApnaFundTest'
   LOOP
     EXECUTE format('ALTER TABLE %I.%I OWNER TO %I', r.schemaname, r.tablename, 'apnafund_test_user');
   END LOOP;
 END$$;

 DO $$
 DECLARE r record;
 BEGIN
   FOR r IN
     SELECT sequence_schema, sequence_name
     FROM   information_schema.sequences
     WHERE  sequence_schema = 'ApnaFundTest'
   LOOP
     EXECUTE format('ALTER SEQUENCE %I.%I OWNER TO %I', r.sequence_schema, r.sequence_name, 'apnafund_test_user');
   END LOOP;
 END$$;

 DO $$
 DECLARE r record;
 BEGIN
   FOR r IN
     SELECT table_schema, table_name
     FROM   information_schema.views
     WHERE  table_schema = 'ApnaFundTest'
   LOOP
     EXECUTE format('ALTER VIEW %I.%I OWNER TO %I', r.table_schema, r.table_name, 'apnafund_test_user');
   END LOOP;
 END$$;

 DO $$
 DECLARE r record;
 BEGIN
   FOR r IN
     SELECT schemaname, matviewname
     FROM   pg_matviews
     WHERE  schemaname = 'ApnaFundTest'
   LOOP
     EXECUTE format('ALTER MATERIALIZED VIEW %I.%I OWNER TO %I', r.schemaname, r.matviewname, 'apnafund_test_user');
   END LOOP;
 END$$;


 ALTER SCHEMA "ApnaFundTest" OWNER TO apnafund_test_user;

 ALTER DEFAULT PRIVILEGES
 FOR ROLE apnafund_dev_deploy
 IN SCHEMA "apnafunddev"
 GRANT SELECT, INSERT, UPDATE, DELETE
 ON TABLES TO apnafund_app_dev, apnafund_dev_user;

 ALTER DEFAULT PRIVILEGES
 FOR ROLE apnafund_dev_deploy
 IN SCHEMA "apnafunddev"
 GRANT USAGE, SELECT, UPDATE
 ON SEQUENCES TO apnafund_app_dev, apnafund_dev_user;

 ALTER DEFAULT PRIVILEGES
 FOR ROLE apnafund_prod_deploy
 IN SCHEMA "ApnaFund"
 GRANT SELECT, INSERT, UPDATE, DELETE
 ON TABLES TO apnafund_app_prod, apnafund_prod_user;

 ALTER DEFAULT PRIVILEGES
 FOR ROLE apnafund_prod_deploy
 IN SCHEMA "ApnaFund"
 GRANT USAGE, SELECT, UPDATE
 ON SEQUENCES TO apnafund_app_prod, apnafund_prod_user;

 ALTER DEFAULT PRIVILEGES
 FOR ROLE apnafund_test_deploy
 IN SCHEMA "ApnaFundTest"
 GRANT SELECT, INSERT, UPDATE, DELETE
 ON TABLES TO apnafund_app_test, apnafund_test_user;

 ALTER DEFAULT PRIVILEGES
 FOR ROLE apnafund_test_deploy
 IN SCHEMA "ApnaFundTest"
 GRANT USAGE, SELECT, UPDATE
 ON SEQUENCES TO apnafund_app_test, apnafund_test_user;


-- ============================
-- OPTIONAL: If you already created objects under a wrong owner,
-- you can transfer ownership to the app role per schema:
-- (Uncomment and run *carefully* if needed)
-- ============================

-- ALTER TABLE   "ApnaFundDev".*    OWNER TO apnafund_app_dev;   -- expand per table
-- ALTER SEQUENCE "ApnaFundDev".*    OWNER TO apnafund_app_dev;
-- ALTER FUNCTION "ApnaFundDev".*    OWNER TO apnafund_app_dev;
