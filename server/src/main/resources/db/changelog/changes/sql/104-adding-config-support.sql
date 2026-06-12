 DROP FUNCTION IF EXISTS get_app_config_boolean(TEXT);
 DROP FUNCTION IF EXISTS get_all_app_config();
 DROP FUNCTION IF EXISTS get_app_configs(TEXT[]);
CREATE TABLE IF NOT EXISTS app_config (

    "configId" SERIAL PRIMARY KEY,

    "configKey" TEXT NOT NULL UNIQUE,

    "configValue" TEXT,

    "description" TEXT,

    "isActive" BOOLEAN NOT NULL DEFAULT TRUE,

    "createdAt" BIGINT NOT NULL
        DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000),

    "updatedAt" BIGINT,

    "updatedBy" INT
);

INSERT INTO app_config
 (
     "configKey",
     "configValue",
     "description"
 )
 VALUES
 (
     'ENABLE_EMAIL_VERIFICATION',
     'false',
     'Enable email verification emails'
 )
 ON CONFLICT ("configKey")
 DO UPDATE SET
     "configValue" = EXCLUDED."configValue",
     "description" = EXCLUDED."description";



 CREATE OR REPLACE FUNCTION get_app_config_boolean(
     p_config_key TEXT
 )
 RETURNS BOOLEAN
 LANGUAGE sql
 STABLE
 AS $$

     SELECT COALESCE(
         (
             SELECT LOWER("configValue") = 'true'
             FROM app_config
             WHERE "configKey" = p_config_key
               AND "isActive" = TRUE
             LIMIT 1
         ),
         FALSE
     );

 $$;

 CREATE OR REPLACE FUNCTION get_all_app_config()
 RETURNS TABLE (
     "configKey" TEXT,
     "configValue" TEXT
 )
 LANGUAGE sql
 STABLE
 AS $$

     SELECT
         "configKey",
         "configValue"
     FROM app_config
     WHERE "isActive" = TRUE
     ORDER BY "configKey";

 $$;

 CREATE OR REPLACE FUNCTION get_app_configs(
     p_config_keys TEXT[]
 )
 RETURNS TABLE (
     "configKey" TEXT,
     "configValue" TEXT
 )
 LANGUAGE sql
 STABLE
 AS $$

     SELECT
         "configKey",
         "configValue"
     FROM app_config
     WHERE "configKey" = ANY(p_config_keys)
       AND "isActive" = TRUE
     ORDER BY "configKey";

 $$;