package com.mynikatech.apnafund.server.loans

import com.mynikatech.apnafund.net.dto.*
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.kotlin.BindKotlin
import org.jdbi.v3.sqlobject.kotlin.RegisterKotlinMapper
import org.jdbi.v3.sqlobject.statement.SqlQuery

@RegisterKotlinMapper(LoansDto::class)
@RegisterKotlinMapper(LoanDetailsDto::class)
@RegisterKotlinMapper(LoanEmisDto::class)
@RegisterKotlinMapper(LoanEmiWithMemberNamesDto::class)
@RegisterKotlinMapper(LoanDetailsWithMemberNamesDto::class)
@RegisterKotlinMapper(LoanCmplDetailsDto::class)
@RegisterKotlinMapper(UserLoanDetailsDto::class)
interface LoansSql {

    // ---- Loans ----
    @SqlQuery("""SELECT * FROM get_loans()""")
    fun getLoans(): List<LoansDto>

    @SqlQuery("""SELECT * FROM get_loans_for_fund(:fundId)""")
    fun getLoansForFund(@Bind("fundId") fundId: Int): List<LoansDto>

    @SqlQuery("""SELECT * FROM get_loan_details_with_names_for_fund(:fundId)""")
    fun getLoanDetailsWithNamesForFund(@Bind("fundId") fundId: Int): List<LoanDetailsWithMemberNamesDto>

    @SqlQuery("""SELECT * FROM get_loan_complete(:loanId)""")
    fun getLoanComplete(@Bind("loanId") loanId: Int): List<LoanCmplDetailsDto>

    @SqlQuery("""
    SELECT add_loan(
        :loanNumber,:borrowerId,:issuedDate,:period,
        :loanAmount,:maturityDate,:rateOfInterest,
        :hasVariableInterestRate,:revisedLoanInterestRate,:interestRateRevisionAfterMonths,
        :status,:fundId
    )
    """)
    fun addLoan(@BindKotlin l: LoansDto): Int

    @SqlQuery("""
    SELECT update_loan(
        :loanId,:loanNumber,:borrowerId,:issuedDate,:period,
        :loanAmount,:maturityDate,:rateOfInterest,
        :hasVariableInterestRate,:revisedLoanInterestRate,:interestRateRevisionAfterMonths,
        :status,:fundId
    )
    """)
    fun updateLoan(@Bind("loanId") loanId: Int, @BindKotlin l: LoansDto): Boolean

    @SqlQuery("""SELECT delete_loan(:loanId)""")
    fun deleteLoan(@Bind("loanId") loanId: Int): Boolean

    @SqlQuery("""
    SELECT insert_loan_with_details(
        :loanNumber,:borrowerId,:issuedDate,:period,
        :loanAmount,:maturityDate,:rateOfInterest,
        :hasVariableInterestRate,:revisedLoanInterestRate,:interestRateRevisionAfterMonths,
        :status,:fundId,
        :origPrincipal,:totalInterest,:totalAmount,
        :emiInterest,:currTotalIntPaid,:currPrincipal
    )
    """)
    fun insertLoanWithDetails(
        @BindKotlin loan: LoansDto,
        @BindKotlin details: LoanDetailsDto
    ): Int

    @SqlQuery("""
    SELECT update_loan_with_details(
        :loanId,:loanNumber,:borrowerId,:issuedDate,:period,
        :loanAmount,:maturityDate,:rateOfInterest,
        :hasVariableInterestRate,:revisedLoanInterestRate,:interestRateRevisionAfterMonths,
        :status,:fundId,
        :loanDetailsId,:origPrincipal,:totalInterest,:totalAmount,
        :emiInterest,:currTotalIntPaid,:currPrincipal
    )
    """)
    fun updateLoanWithDetails(
        @BindKotlin loan: LoansDto,
        @BindKotlin details: LoanDetailsDto
    ): Boolean

    @SqlQuery("""SELECT is_loan_for_user_for_fund(:userId,:fundId)""")
    fun isLoanForUserForFund(@Bind("userId") userId: Int, @Bind("fundId") fundId: Int): Boolean

    @SqlQuery("""SELECT get_total_loan_amount(:userId,:fundId)""")
    fun getTotalLoanAmount(@Bind("userId") userId: Int, @Bind("fundId") fundId: Int): Double?

    @SqlQuery("""SELECT get_total_pending_amount(:userId,:fundId)""")
    fun getTotalPendingAmount(@Bind("userId") userId: Int, @Bind("fundId") fundId: Int): Double?

    @SqlQuery("""SELECT get_total_curr_int_paid(:userId,:fundId)""")
    fun getTotalCurrIntPaid(@Bind("userId") userId: Int, @Bind("fundId") fundId: Int): Double?

    @SqlQuery("""SELECT * FROM get_user_loan_details(:userId,:fundId)""")
    fun getUserLoanDetails(@Bind("userId") userId: Int, @Bind("fundId") fundId: Int): List<UserLoanDetailsDto>

    // ---- EMIs ----
    @SqlQuery("""SELECT * FROM get_all_loan_emis()""")
    fun getAllLoanEmis(): List<LoanEmisDto>

    @SqlQuery("""SELECT * FROM get_loan_emis_for_loan(:loanId)""")
    fun getLoanEmisForLoan(@Bind("loanId") loanId: Int): List<LoanEmisDto>

    @SqlQuery("""SELECT * FROM get_loan_emi_for_loan_month_year(:loanId,:month,:year)""")
    fun getLoanEmiForLoanMonthYear(@Bind("loanId") loanId: Int, @Bind("month") month: String, @Bind("year") year: String): List<LoanEmisDto>

    @SqlQuery("""SELECT add_loan_emi(:loanId,:emiMonth,:emiYear,CAST(:emiDepositedDate AS date),:emiDepositedAmount,:prepaymentAmount,:lateFee)""")
    fun addLoanEmi(@BindKotlin e: LoanEmisDto): Int

    @SqlQuery("""SELECT update_loan_emi(:loanEmiId,:loanId,:emiMonth,:emiYear,CAST(:emiDepositedDate AS date),:emiDepositedAmount,:prepaymentAmount,:lateFee)""")
    fun updateLoanEmi(@Bind("loanEmiId") loanEmiId: Int, @BindKotlin e: LoanEmisDto): Boolean

    @SqlQuery("""SELECT delete_loan_emi(:loanEmiId)""")
    fun deleteLoanEmi(@Bind("loanEmiId") loanEmiId: Int): Boolean

    @SqlQuery("""SELECT * FROM get_all_loan_emis_with_names_for_fund_month_year(:fundId,:month,:year)""")
    fun getAllLoanEmisWithNamesForFundMonthYear(@Bind("fundId") fundId: Int, @Bind("month") month: String, @Bind("year") year: String): List<LoanEmiWithMemberNamesDto>

    @SqlQuery("""SELECT * FROM get_all_loan_emis_with_names_for_loan(:loanId)""")
    fun getAllLoanEmisWithNamesForLoan(@Bind("loanId") loanId: Int): List<LoanEmiWithMemberNamesDto>

    // The big one: DB handles the whole transactional logic and returns the fresh list
    @SqlQuery("""SELECT * FROM save_or_update_all_loan_emis_and_fetch(:fundId,:month,:year, :emis::jsonb)""")
    fun saveOrUpdateAllLoanEmisAndFetch(
        @Bind("fundId") fundId: Int,
        @Bind("month") month: String,
        @Bind("year") year: String,
        @Bind("emis") emisJson: String
    ): List<LoanEmiWithMemberNamesDto>

    // ---- Details ----
    @SqlQuery("""SELECT * FROM get_loan_details(:loanId)""")
    fun getLoanDetails(@Bind("loanId") loanId: Int): List<LoanDetailsDto>

    @SqlQuery("""SELECT upsert_loan_details(:loanId,:origPrincipal,:totalInterest,:totalAmount,:emiInterest,:currTotalIntPaid,:currPrincipal)""")
    fun upsertLoanDetails(@BindKotlin d: LoanDetailsDto): Int

    @SqlQuery("""SELECT insert_loan_details(:loanId,:origPrincipal,:totalInterest,:totalAmount,:emiInterest,:currTotalIntPaid,:currPrincipal)""")
    fun insertLoanDetails(@BindKotlin d: LoanDetailsDto): Int

    @SqlQuery("""SELECT update_loan_details(:loanDetailsId,:loanId,:origPrincipal,:totalInterest,:totalAmount,:emiInterest,:currTotalIntPaid,:currPrincipal)""")
    fun updateLoanDetails(@BindKotlin d: LoanDetailsDto): Boolean

    @SqlQuery("""SELECT delete_loan_details(:loanDetailsId)""")
    fun deleteLoanDetails(@Bind("loanDetailsId") loanDetailsId: Int): Boolean

    @SqlQuery("""SELECT * FROM get_loan_details_with_names_for_fund_user(:fundId,:userId)""")
    fun getLoanDetailsWithNamesForFundUser(@Bind("fundId") fundId: Int, @Bind("userId") userId: Int): List<LoanDetailsWithMemberNamesDto>

    // ---------------- APPROVE LOAN ----------------
    @SqlQuery("""
        SELECT approve_loan_request(
            :approvalId,
            :approvedBy
        )
    """)
    fun approveLoanRequest(
        @Bind("approvalId") approvalId: Int,
        @Bind("approvedBy") approvedBy: Int
    ): Boolean


    // ---------------- REJECT LOAN ----------------
    @SqlQuery("""
        SELECT reject_loan_request(
            :approvalId,
            :rejectedBy,
            :reason
        )
    """)
    fun rejectLoanRequest(
        @Bind("approvalId") approvalId: Int,
        @Bind("rejectedBy") rejectedBy: Int,
        @Bind("reason") reason: String
    ): Boolean

    @SqlQuery("""
    SELECT * FROM get_loan_by_id(:loanId)
    """)
    fun getLoanById(
        @Bind("loanId") loanId: Int
    ): LoansDto?

    @SqlQuery(
        """
    SELECT close_loan_by_approval(
        :loanId,
        :approvedBy
    )
    """
    )
    fun closeLoanByApproval(
        @Bind("loanId") loanId: Int,
        @Bind("approvedBy") approvedBy: Int
    ): Boolean

    @SqlQuery("""
    SELECT has_pending_loan_closure_request(:loanId)
""")
    fun hasPendingClosureRequest(
        @Bind("loanId") loanId: Int
    ): Boolean


}
