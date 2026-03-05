DROP FUNCTION IF EXISTS get_fund_members_with_names_for_fund(INT);

CREATE OR REPLACE FUNCTION get_fund_members_with_names_for_fund(p_fund_id int)
RETURNS TABLE (
  "fundMemberId" int,
  "userId"       int,
  "fundId"       int,
  "joiningDate"  text,
  "firstName"    text,
  "lastName"     text,
  "emailId"      text
) LANGUAGE sql STABLE AS $$
  SELECT
    fm."fundMemberId",
    fm."userId",
    fm."fundId",
    fm."joiningDate",
    u."firstName",
    COALESCE(u."lastName", '') AS "lastName",
    u."emailId"
  FROM fund_members fm
  JOIN users u  ON u."userId" = fm."userId"
  WHERE fm."fundId" = p_fund_id
  ORDER BY u."firstName", u."lastName";
$$;