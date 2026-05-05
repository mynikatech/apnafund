DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users'
          AND column_name = 'isinvited'
    ) THEN
        ALTER TABLE users RENAME COLUMN "isinvited" TO "isInvited";
    END IF;
END $$;