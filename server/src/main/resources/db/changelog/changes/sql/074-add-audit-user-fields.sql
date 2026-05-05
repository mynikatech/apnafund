CREATE OR REPLACE FUNCTION update_user_audit_fields(
    p_userId INT,
    p_createdByUserId INT DEFAULT NULL,
    p_updatedByUserId INT DEFAULT NULL
)
RETURNS VOID
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE "users"
    SET
        -- set createdBy only if it’s currently NULL (don’t overwrite history)
        "createdByUserId" = COALESCE("createdByUserId", p_createdByUserId),

        -- always update updatedBy if provided
        "updatedByUserId" = COALESCE(p_updatedByUserId, "updatedByUserId"),

        -- bump updatedAt to now (epoch millis)
        "updatedAt" = (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
    WHERE "userId" = p_userId;
END;
$$;