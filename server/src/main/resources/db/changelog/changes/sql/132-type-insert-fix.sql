INSERT INTO type ("typeCode", "typeDescription", "status")
VALUES
    ('LOAN_REQUESTED', 'Loan approval requested', 'ACTIVE'),
    ('LOAN_APPROVED', 'Loan approved', 'ACTIVE'),
    ('LOAN_REJECTED', 'Loan rejected', 'ACTIVE'),
    ('GROUP_APPROVED', 'Group approved', 'ACTIVE'),
    ('GROUP_REJECTED', 'Group rejected', 'ACTIVE')
ON CONFLICT ("typeCode") DO NOTHING;