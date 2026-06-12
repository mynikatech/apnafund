
DROP FUNCTION IF EXISTS insert_app_usage_Log(INT, TEXT, TEXT, TEXT, TEXT);
DROP FUNCTION IF EXISTS get_app_usage_logs(INT, TEXT, INT);

CREATE TABLE IF NOT EXISTS "app_usage_logs" (

    "logId" BIGSERIAL PRIMARY KEY,

    "userId" INTEGER NOT NULL,

    "eventType" TEXT NOT NULL,

    "screenName" TEXT,

    "details" TEXT,

    "deviceInfo" TEXT,

    "createdAt" TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT "fkUsageUser"
        FOREIGN KEY ("userId")
        REFERENCES "users"("userId")
);

CREATE INDEX IF NOT EXISTS "idxUsageLogsUserId"
ON "app_usage_logs"("userId");

CREATE INDEX IF NOT EXISTS "idxUsageLogsCreatedAt"
ON "app_usage_logs"("createdAt");

CREATE INDEX IF NOT EXISTS "idxUsageLogsEventType"
ON "app_usage_logs"("eventType");

CREATE OR REPLACE FUNCTION insert_app_usage_Log(

    p_userId INTEGER,
    p_eventType TEXT,
    p_screenName TEXT DEFAULT NULL,
    p_details TEXT DEFAULT NULL,
    p_deviceInfo TEXT DEFAULT NULL

)
RETURNS BIGINT
LANGUAGE plpgsql
AS
$$

DECLARE
    v_logId BIGINT;

BEGIN

    INSERT INTO "app_usage_logs" (

        "userId",
        "eventType",
        "screenName",
        "details",
        "deviceInfo"

    )
    VALUES (

        p_userId,
        p_eventType,
        p_screenName,
        p_details,
        p_deviceInfo

    )
    RETURNING "logId"
    INTO v_logId;

    RETURN v_logId;

END;
$$;

CREATE OR REPLACE FUNCTION get_app_usage_logs(

    p_userId INTEGER DEFAULT NULL,
    p_eventType TEXT DEFAULT NULL,
    p_limit INTEGER DEFAULT 100

)
RETURNS TABLE (

    "logId" BIGINT,
    "userId" INTEGER,
    "eventType" VARCHAR,
    "screenName" VARCHAR,
    "details" TEXT,
    "deviceInfo" VARCHAR,
    "createdAt" TIMESTAMP

)
LANGUAGE plpgsql
AS
$$

BEGIN

    RETURN QUERY

    SELECT
        l."logId",
        l."userId",
        l."eventType",
        l."screenName",
        l."details",
        l."deviceInfo",
        l."createdAt"

    FROM "app_usage_logs" l

    WHERE
        (p_userId IS NULL OR l."userId" = p_userId)
        AND
        (p_eventType IS NULL OR l."eventType" = p_eventType)

    ORDER BY l."createdAt" DESC

    LIMIT p_limit;

END;
$$;