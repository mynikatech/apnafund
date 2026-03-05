ALTER DEFAULT PRIVILEGES
FOR ROLE apnafund_dev_deploy
IN SCHEMA "apnafunddev"
GRANT SELECT, INSERT, UPDATE, DELETE
ON TABLES
TO apnafund_appdev, apnafund_app_role_dev;

ALTER DEFAULT PRIVILEGES
FOR ROLE apnafund_dev_deploy
IN SCHEMA "apnafunddev"
GRANT USAGE, SELECT, UPDATE
ON SEQUENCES
TO apnafund_appdev, apnafund_app_role_dev;

ALTER DEFAULT PRIVILEGES
FOR ROLE apnafund_dev_deploy
IN SCHEMA "apnafunddev"
GRANT EXECUTE
ON FUNCTIONS
TO apnafund_appdev, apnafund_app_role_dev;

GRANT SELECT, INSERT, UPDATE, DELETE
ON TABLE "apnafunddev".verification_tokens
TO apnafund_appdev, apnafund_app_role_dev;

GRANT EXECUTE
ON FUNCTION "apnafunddev".create_verification_token(
    integer,
    text,
    text,
    text,
    timestamp without time zone
)
TO apnafund_appdev, apnafund_app_role_dev;

GRANT EXECUTE
ON FUNCTION "apnafunddev".verify_verification_token(
    text,
    text,
    text
)
TO apnafund_appdev, apnafund_app_role_dev;

GRANT EXECUTE
ON FUNCTION "apnafunddev".mark_user_email_verified(
    integer
)
TO apnafund_appdev, apnafund_app_role_dev;

GRANT EXECUTE
ON FUNCTION "apnafunddev".is_email_verified(
    integer
)
TO apnafund_appdev, apnafund_app_role_dev;

