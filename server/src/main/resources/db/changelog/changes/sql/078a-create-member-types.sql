DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_type t
        JOIN pg_namespace n
          ON n.oid = t.typnamespace
        WHERE t.typname = 'memberRole'
          AND n.nspname = current_schema()
    ) THEN
        CREATE TYPE "memberRole" AS ENUM (
            'PRIMARY_MODERATOR',
            'MODERATOR',
            'MEMBER'
        );
    END IF;

   IF NOT EXISTS (
       SELECT 1
       FROM pg_type t
       JOIN pg_namespace n
         ON n.oid = t.typnamespace
       WHERE t.typname = 'memberStatus'
         AND n.nspname = current_schema()
   ) THEN
        CREATE TYPE "memberStatus" AS ENUM (
            'ACTIVE',
            'INACTIVE'
        );
    END IF;
END
$$;