CREATE OR REPLACE FUNCTION get_types()
RETURNS TABLE (
  "typeId" int,
  "typeCode" text,
  "typeDescription" text,
  "status" text
) LANGUAGE sql AS $$
  SELECT t."typeId", t."typeCode", t."typeDescription", t."status"
    FROM "type" t
   ORDER BY t."typeId";
$$;

CREATE OR REPLACE FUNCTION get_type(p_type_id int)
RETURNS TABLE (
  "typeId" int,
  "typeCode" text,
  "typeDescription" text,
  "status" text
) LANGUAGE sql AS $$
  SELECT t."typeId", t."typeCode", t."typeDescription", t."status"
    FROM "type" t
   WHERE t."typeId" = p_type_id
   LIMIT 1;
$$;

CREATE OR REPLACE FUNCTION add_type(
  p_type_code text,
  p_type_description text,
  p_status text
) RETURNS int LANGUAGE plpgsql AS $$
DECLARE v_id int;
BEGIN
  INSERT INTO "type"("typeCode","typeDescription","status")
  VALUES (p_type_code, p_type_description, COALESCE(p_status, 'ACTIVE'))
  RETURNING "typeId" INTO v_id;
  RETURN v_id;
END $$;

CREATE OR REPLACE FUNCTION update_type(
  p_type_id int,
  p_type_code text,
  p_type_description text,
  p_status text
) RETURNS boolean LANGUAGE plpgsql AS $$
BEGIN
  UPDATE "type"
     SET "typeCode"        = COALESCE(p_type_code, "typeCode"),
         "typeDescription" = COALESCE(p_type_description, "typeDescription"),
         "status"          = COALESCE(p_status, "status")
   WHERE "typeId" = p_type_id;
  RETURN FOUND;
END $$;

CREATE OR REPLACE FUNCTION delete_type(p_type_id int)
RETURNS boolean LANGUAGE plpgsql AS $$
BEGIN
  DELETE FROM "type" WHERE "typeId" = p_type_id;
  RETURN FOUND;
END $$;