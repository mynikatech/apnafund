INSERT INTO "type" ("typeCode","typeDescription","status")
VALUES
('FUND_CREATED','Fund Created Notification','ACTIVE'),
('FUND_MEMBER_ADDED','Fund Member Added Notification','ACTIVE')
ON CONFLICT ("typeCode") DO NOTHING;