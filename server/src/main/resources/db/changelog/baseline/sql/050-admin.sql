
CREATE OR REPLACE PROCEDURE pr_approve_moderator_and_group(
  IN p_userId int,
  IN p_roleId int,
  IN p_groupId int
)
LANGUAGE plpgsql AS $$
BEGIN
  UPDATE user_roles
     SET "status" = 'ACTIVE'
   WHERE "userId" = p_userId AND "roleId" = p_roleId;

  UPDATE "groups"
     SET "status" = 'ACTIVE'
   WHERE "groupId" = p_groupId;

  -- add moderator as group member if not already present
  INSERT INTO group_members("userId","groupId","joiningDate")
  SELECT p_userId, p_groupId, to_char(now(), 'YYYY-MM-DD')
  WHERE NOT EXISTS (
    SELECT 1 FROM group_members gm
    WHERE gm."userId" = p_userId AND gm."groupId" = p_groupId
  );
END $$;

CREATE OR REPLACE PROCEDURE pr_reject_moderator_and_group(
  IN p_userId int,
  IN p_roleId int,
  IN p_groupId int
)
LANGUAGE plpgsql AS $$
BEGIN
  UPDATE user_roles
     SET "status" = 'REJECTED'
   WHERE "userId" = p_userId AND "roleId" = p_roleId;

  UPDATE "groups"
     SET "status" = 'REJECTED'
   WHERE "groupId" = p_groupId;
END $$;

-- Ensure unique membership (recommended)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'uq_group_members_user_group' AND conrelid = 'group_members'::regclass
  ) THEN
    ALTER TABLE group_members
      ADD CONSTRAINT uq_group_members_user_group UNIQUE ("userId","groupId");
  END IF;
END$$;

-- ----------------------------------------------------------------------------
-- Approve moderator & group:
--  - sets user role status to 'ACTIVE'
--  - sets group moderator to userId and group status to 'ACTIVE'
--  - upserts membership for the user into the group (joiningDate = CURRENT_DATE if absent)
-- ----------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE approve_moderator_and_group(
  p_user_id  int,
  p_role_id  int,
  p_group_id int
)
LANGUAGE plpgsql
AS $$
BEGIN
  -- user role -> ACTIVE
  UPDATE user_roles
     SET "status" = 'ACTIVE'
   WHERE "userId" = p_user_id AND "roleId" = p_role_id;

  -- group -> ACTIVE + set moderator
  UPDATE "groups"
     SET "moderator" = p_user_id,
         "status"    = 'ACTIVE'
   WHERE "groupId" = p_group_id;

  -- upsert membership
  INSERT INTO group_members("userId","groupId","joiningDate")
  VALUES (p_user_id, p_group_id, CURRENT_DATE)
  ON CONFLICT ("userId","groupId")
  DO UPDATE SET "joiningDate" = COALESCE(group_members."joiningDate", EXCLUDED."joiningDate");
END $$;

-- ----------------------------------------------------------------------------
-- Reject moderator & group:
--  - sets user role status to 'REJECTED'
--  - NULLs group moderator and sets group status to 'REJECTED'
--  - (optional) remove membership if present
-- ----------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE reject_moderator_and_group(
  p_user_id  int,
  p_role_id  int,
  p_group_id int
)
LANGUAGE plpgsql
AS $$
BEGIN
  UPDATE user_roles
     SET "status" = 'REJECTED'
   WHERE "userId" = p_user_id AND "roleId" = p_role_id;

  UPDATE "groups"
     SET "moderator" = NULL,
         "status"    = 'REJECTED'
   WHERE "groupId" = p_group_id;

  -- If you prefer to keep the membership, comment this out
  DELETE FROM group_members
   WHERE "userId" = p_user_id AND "groupId" = p_group_id;
END $$;

-- ----------------------------------------------------------------------------
-- Update user role status (generic)
-- ----------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE update_user_role_status(
  p_user_id int,
  p_role_id int,
  p_status  text
)
LANGUAGE plpgsql
AS $$
BEGIN
  UPDATE user_roles
     SET "status" = p_status
   WHERE "userId" = p_user_id AND "roleId" = p_role_id;
END $$;

-- ----------------------------------------------------------------------------
-- Update group status (generic)
-- ----------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE update_group_status(
  p_group_id int,
  p_status   text
)
LANGUAGE plpgsql
AS $$
BEGIN
  UPDATE "groups"
     SET "status" = p_status
   WHERE "groupId" = p_group_id;
END $$;

-- ----------------------------------------------------------------------------
-- Add group member (admin) -> returns groupMemberId
--  - inserts if not exists, otherwise returns existing id
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION add_group_member_admin(
  p_user_id int,
  p_group_id int,
  p_joining_date date
) RETURNS int
LANGUAGE plpgsql
AS $$
DECLARE v_id int;
BEGIN
  INSERT INTO group_members("userId","groupId","joiningDate")
  VALUES (p_user_id, p_group_id, p_joining_date)
  ON CONFLICT ("userId","groupId") DO UPDATE
     SET "joiningDate" = COALESCE(EXCLUDED."joiningDate", group_members."joiningDate")
  RETURNING "groupMemberId" INTO v_id;

  -- Just in case ON CONFLICT did UPDATE but didn't RETURN, make sure v_id is filled:
  IF v_id IS NULL THEN
     SELECT "groupMemberId" INTO v_id
       FROM group_members
      WHERE "userId" = p_user_id AND "groupId" = p_group_id
      LIMIT 1;
  END IF;

  RETURN v_id;
END $$;
