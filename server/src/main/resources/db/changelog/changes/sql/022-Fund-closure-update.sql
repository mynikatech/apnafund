ALTER TABLE funds
ADD COLUMN "closedAt" TIMESTAMP NULL,
ADD COLUMN "closedBy" INT NULL,
ADD COLUMN "closureReason" TEXT NULL;

CREATE OR REPLACE FUNCTION close_fund(
    p_fund_id INT,
    p_closed_by INT,
    p_reason TEXT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
BEGIN

    UPDATE funds
    SET "fundStatus" = 'CLOSED',
        "closedAt" = NOW(),
        "closedBy" = p_closed_by,
        "closureReason" = p_reason
    WHERE "fundId" = p_fund_id
      AND "fundStatus" = 'ACTIVE';

    RETURN FOUND; -- TRUE if updated, FALSE otherwise

END;
$$;