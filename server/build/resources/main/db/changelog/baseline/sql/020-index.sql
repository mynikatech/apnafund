
DROP INDEX IF EXISTS "index_type_typeCode";

CREATE UNIQUE INDEX "index_type_typeCode" ON "type" ("typeCode");

CREATE INDEX IF NOT EXISTS idx_users_email_lower ON users ((lower("emailId")));

CREATE INDEX IF NOT EXISTS idx_deposits_fund ON "deposits"("fundId");
CREATE INDEX IF NOT EXISTS idx_deposits_user ON "deposits"("depositorId");
CREATE INDEX IF NOT EXISTS idx_deposits_month_year ON "deposits"("depositYear","depositMonth");

CREATE INDEX IF NOT EXISTS idx_user_roles_user ON user_roles("userId");
CREATE INDEX IF NOT EXISTS idx_user_roles_role ON user_roles("roleId");
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_roles_user_role ON user_roles("userId","roleId");

CREATE INDEX IF NOT EXISTS idx_fund_members_fund_user ON fund_members("fundId","userId");

CREATE UNIQUE INDEX IF NOT EXISTS uq_funddetails_fund ON fund_details ("fundId");

CREATE INDEX IF NOT EXISTS idx_loans_fund ON loans("fundId");
CREATE INDEX IF NOT EXISTS idx_loan_emis_loan ON loan_emi("loanId");
CREATE INDEX IF NOT EXISTS idx_loan_emis_month_year ON loan_emi("emiYear","emiMonth");

CREATE INDEX IF NOT EXISTS idx_user_notifications_user ON user_notifications("userId");
CREATE INDEX IF NOT EXISTS idx_user_notifications_status ON user_notifications("status");

CREATE INDEX IF NOT EXISTS idx_uph_user ON user_password_history("userId");
CREATE INDEX IF NOT EXISTS idx_uph_changedat ON user_password_history("changedAt");

CREATE INDEX IF NOT EXISTS idx_upn_user      ON user_pin_history("userId");
CREATE INDEX IF NOT EXISTS idx_upn_changedat ON user_pin_history("changedAt");

CREATE UNIQUE INDEX IF NOT EXISTS idx_uq_deposits_fund_user_month_year on deposits(fundId, depositorId, depositMonth, depositYear)
CREATE UNIQUE INDEX IF NOT EXISTS idx_uq_loan_emis_loan_month_year on loan_emi(loanId, emiMonth, emiYear)
CREATE UNIQUE INDEX IF NOT EXISTS idx_uq_users_emailid_key on users(emailId)

