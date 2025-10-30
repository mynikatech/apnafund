CREATE SCHEMA IF NOT EXISTS "ApnaFund";     -- prod
CREATE SCHEMA IF NOT EXISTS "ApnaFundDev";  -- dev

-- App roles (no direct login)
CREATE ROLE apnafund_app_prod NOINHERIT;
CREATE ROLE apnafund_app_dev  NOINHERIT;

-- Login users (one per env)
CREATE ROLE apnafund_prod_user LOGIN PASSWORD 'apna@produ3er' IN ROLE apnafund_app_prod;
CREATE ROLE apnafund_dev_user  LOGIN PASSWORD 'apna@devu3er' IN ROLE apnafund_app_dev;

-- Lock down “public”
REVOKE CREATE ON SCHEMA public FROM PUBLIC;

-- Schema usage
GRANT USAGE ON SCHEMA "ApnaFund"    TO apnafund_app_prod;
GRANT USAGE ON SCHEMA "ApnaFundDev" TO apnafund_app_dev;

-- Existing objects
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES    IN SCHEMA "ApnaFund"    TO apnafund_app_prod;
GRANT USAGE, SELECT, UPDATE           ON ALL SEQUENCES IN SCHEMA "ApnaFund"    TO apnafund_app_prod;

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES    IN SCHEMA "ApnaFundDev" TO apnafund_app_dev;
GRANT USAGE, SELECT, UPDATE           ON ALL SEQUENCES IN SCHEMA "ApnaFundDev" TO apnafund_app_dev;

-- Future objects (so new tables/sequences get the right grants automatically)
ALTER DEFAULT PRIVILEGES IN SCHEMA "ApnaFund"
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO apnafund_app_prod;
ALTER DEFAULT PRIVILEGES IN SCHEMA "ApnaFund"
  GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO apnafund_app_prod;

ALTER DEFAULT PRIVILEGES IN SCHEMA "ApnaFundDev"
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO apnafund_app_dev;
ALTER DEFAULT PRIVILEGES IN SCHEMA "ApnaFundDev"
  GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO apnafund_app_dev;

