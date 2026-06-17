INSERT INTO Type
(
    "typeCode",
    "typeDescription",
    "status"
)
SELECT
    'FUND_MEMBER_PROMOTED',
    'Fund member promoted to moderator',
    'ACTIVE'
WHERE NOT EXISTS
(
    SELECT 1
    FROM Type
    WHERE "typeCode" = 'FUND_MEMBER_PROMOTED'
);

INSERT INTO Type
(
    "typeCode",
    "typeDescription",
    "status"
)
SELECT
    'FUND_MODERATOR_REMOVED',
    'Fund moderator role removed',
    'ACTIVE'
WHERE NOT EXISTS
(
    SELECT 1
    FROM Type
    WHERE "typeCode" = 'FUND_MODERATOR_REMOVED'
);

INSERT INTO Type
(
    "typeCode",
    "typeDescription",
    "status"
)
SELECT
    'FUND_MEMBER_REMOVED',
    'Fund member removed from fund',
    'ACTIVE'
WHERE NOT EXISTS
(
    SELECT 1
    FROM Type
    WHERE "typeCode" = 'FUND_MEMBER_REMOVED'
);

INSERT INTO Type
(
    "typeCode",
    "typeDescription",
    "status"
)
SELECT
    'FUND_MEMBER_ADDED',
    'Fund member added to fund',
    'ACTIVE'
WHERE NOT EXISTS
(
    SELECT 1
    FROM Type
    WHERE "typeCode" = 'FUND_MEMBER_ADDED'
);