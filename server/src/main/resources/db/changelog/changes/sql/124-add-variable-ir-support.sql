ALTER TABLE funds
ADD COLUMN IF NOT EXISTS "hasVariableInterestRate" BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE funds
ADD COLUMN IF NOT EXISTS "revisedLoanInterestRate" DOUBLE PRECISION;

ALTER TABLE funds
ADD COLUMN IF NOT EXISTS "interestRateRevisionAfterMonths" INTEGER;

ALTER TABLE loans
ADD COLUMN IF NOT EXISTS "hasVariableInterestRate" BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE loans
ADD COLUMN IF NOT EXISTS "revisedLoanInterestRate" DOUBLE PRECISION;

ALTER TABLE loans
ADD COLUMN IF NOT EXISTS "interestRateRevisionAfterMonths" INTEGER;

DO $$
BEGIN
    IF NOT EXISTS (
       SELECT 1
       FROM pg_constraint c
       JOIN pg_namespace n
           ON n.oid = c.connamespace
       WHERE c.conname = 'chk_variable_interest_rate'
         AND n.nspname = current_schema()
    ) THEN

        ALTER TABLE funds
        ADD CONSTRAINT chk_variable_interest_rate
        CHECK (
            "hasVariableInterestRate" = FALSE
            OR (
                "revisedLoanInterestRate" IS NOT NULL
                AND "interestRateRevisionAfterMonths" IS NOT NULL
                AND "revisedLoanInterestRate" > "loanInterestRate"
                AND "interestRateRevisionAfterMonths" > 0
            )
        );

    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_namespace n
            ON n.oid = c.connamespace
        WHERE c.conname = 'chk_loan_variable_interest_rate'
          AND n.nspname = current_schema()
    ) THEN

        ALTER TABLE loans
        ADD CONSTRAINT chk_loan_variable_interest_rate
        CHECK (
            "hasVariableInterestRate" = FALSE
            OR (
                "revisedLoanInterestRate" IS NOT NULL
                AND "interestRateRevisionAfterMonths" IS NOT NULL
                AND "revisedLoanInterestRate" > "rateOfInterest"
                AND "interestRateRevisionAfterMonths" > 0
            )
        );

    END IF;
END $$;


