INSERT INTO "type" ("typeCode","typeDescription","status")
VALUES
('FUND_CLOSED','Fund Closed Notification','ACTIVE')
ON CONFLICT ("typeCode") DO UPDATE
SET "typeDescription" = EXCLUDED."typeDescription",
    "status" = EXCLUDED."status";