CREATE OR REPLACE FUNCTION get_users_for_moderator(p_user_id INT)
RETURNS TABLE (
    userId INT,
    firstName TEXT,
    lastName TEXT,
    emailId TEXT,
    phoneNumber TEXT,
    status TEXT,
    passwordHash TEXT,
    createdDate TEXT,
    isPinSet BOOLEAN,
    hashPIN TEXT,
    firebaseUserId TEXT,
    userCode TEXT
)
LANGUAGE sql AS $$

SELECT DISTINCT u."userId",
       u."firstName",
       u."lastName",
       u."emailId",
       u."phoneNumber",
       u."status",
       u."passwordHash",
       u."createdDate",
       u."isPinSet",
       u."hashPIN",
       u."firebaseUserId",
       u."userCode"
FROM users u
JOIN group_members gm ON gm."userId" = u."userId"
WHERE gm."groupId" IN (
    SELECT g."groupId"
    FROM "groups" g
    WHERE g."moderator" = 57
);

$$;