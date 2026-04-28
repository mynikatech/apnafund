INSERT INTO type ("typeCode", "typeDescription", "status") VALUES
('LOAN_CLOSURE_REQUESTED', 'Loan closure request notification', 'ACTIVE'),
('LOAN_CLOSURE_REJECTED', 'Loan closure rejected notification', 'ACTIVE'),
('LOAN_CLOSED', 'Loan closed notification', 'ACTIVE')
ON CONFLICT ("typeCode") DO NOTHING;