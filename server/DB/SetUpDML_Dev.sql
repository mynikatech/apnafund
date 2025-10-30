-- Seeds initial rows; safe to re-run due to ON CONFLICT
SET search_path TO "ApnaFundDev", public;

BEGIN;

-- USERS
INSERT INTO "users" ("userId","firstName","lastName","emailId","phoneNumber","status","createdDate","userCode","isPinSet")
VALUES (1,'Sunil','Agarwal','sunilagl@gmail.com','9004533807','ACTIVE','2025-07-16','SAAG123456',FALSE)
ON CONFLICT ("userId") DO NOTHING;

-- ROLES
INSERT INTO "roles" ("roleId","roleCode","roleDescription","status") VALUES
  (1,'ADMIN','Perform Admin Functions','ACTIVE'),
  (2,'MODERATOR','Coordinates Group Activities and manages Fund','ACTIVE'),
  (3,'MEMBER','A regular Fund Member','ACTIVE')
ON CONFLICT ("roleId") DO NOTHING;

-- USER_ROLES
INSERT INTO "user_roles" ("userRoleId","userId","roleId","status") VALUES
  (1,1,1,'ACTIVE'),
  (2,1,2,'ACTIVE'),
  (3,1,3,'ACTIVE')
ON CONFLICT ("userRoleId") DO NOTHING;

-- TYPE
INSERT INTO "type" ("typeId","typeCode","typeDescription","status") VALUES
  (1,'DEPOVERDUE','Your Deposit is Overdue','ACTIVE'),
  (2,'EMIOVERDUE','Your EMI is Overdue','ACTIVE'),
  (3,'EMIDUE','Your EMI is due','ACTIVE'),
  (4,'DEPDUE','Your Deposit is due','ACTIVE')
ON CONFLICT ("typeId") DO NOTHING;

-- PRIVILEGE
INSERT INTO "privilege" ("privilegeId","privilegeCode","privilegeDescription","status") VALUES
  (1,'ADDUSER','Privilege to add/edit a user','ACTIVE'),
  (2,'ADDGROUP','Privilege to add/edit a group','ACTIVE'),
  (3,'ADDFUND','Privilege to add/edit a fund','ACTIVE'),
  (4,'APPLYLOAN','Privilege to apply a loan','ACTIVE'),
  (5,'APPROVELOAN','Privilege to approve a loan','ACTIVE'),
  (6,'ADDGROUPMEMBER','Privilege to add member to a group','ACTIVE'),
  (7,'ADDFUNDMEMBER','Privilege to add member to a fund','ACTIVE')
ON CONFLICT ("privilegeId") DO NOTHING;

-- ROLE_PRIVILEGE
INSERT INTO "role_privilege" ("rolePrivilegeId","privilegeId","roleId","status") VALUES
  (1,1,1,'ACTIVE'),
  (2,2,1,'ACTIVE'),
  (3,3,1,'ACTIVE'),
  (4,4,1,'ACTIVE'),
  (5,5,1,'ACTIVE'),
  (6,6,1,'ACTIVE'),
  (7,7,1,'ACTIVE'),
  (8,2,2,'ACTIVE'),
  (9,3,2,'ACTIVE'),
  (10,6,2,'ACTIVE'),
  (11,7,2,'ACTIVE'),
  (12,4,3,'ACTIVE')
ON CONFLICT ("rolePrivilegeId") DO NOTHING;

COMMIT;

-- ===========
-- Reseed identities to MAX(id)+1 in this schema
-- ===========

SELECT setval(pg_get_serial_sequence('"ApnaFundDev".users', 'userId'),
              GREATEST((SELECT COALESCE(MAX("userId"),0) FROM "users") + 1, 1),
              false);

SELECT setval(pg_get_serial_sequence('"ApnaFundDev".roles','roleId'),
              GREATEST((SELECT COALESCE(MAX("roleId"),0) FROM "roles") + 1, 1),
              false);

SELECT setval(pg_get_serial_sequence('"ApnaFundDev".user_roles','userRoleId'),
              GREATEST((SELECT COALESCE(MAX("userRoleId"),0) FROM "user_roles") + 1, 1),
              false);

SELECT setval(pg_get_serial_sequence('"ApnaFundDev".type','typeId'),
              GREATEST((SELECT COALESCE(MAX("typeId"),0) FROM "type") + 1, 1),
              false);

SELECT setval(pg_get_serial_sequence('"ApnaFundDev".privilege','privilegeId'),
              GREATEST((SELECT COALESCE(MAX("privilegeId"),0) FROM "privilege") + 1, 1),
              false);

SELECT setval(pg_get_serial_sequence('"ApnaFundDev".role_privilege','rolePrivilegeId'),
              GREATEST((SELECT COALESCE(MAX("rolePrivilegeId"),0) FROM "role_privilege") + 1, 1),
              false);
