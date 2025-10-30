package com.mynikatech.apnafund.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.mynikatech.apnafund.data.model.FundDetails
import com.mynikatech.apnafund.data.model.LoanCmplDetails
import com.mynikatech.apnafund.data.model.LoanDetails
import com.mynikatech.apnafund.data.model.LoanEmiWithMemberNames
import com.mynikatech.apnafund.data.model.LoanEmis
import com.mynikatech.apnafund.data.model.Loans
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanEmisDao {

    @Insert
    suspend fun addLoanDetails(loanEmis: LoanEmis)

    @Query("SELECT * from loan_emi")
    fun getAllLoanEmis(): Flow<List<LoanEmis>>

    @Query("SELECT * from loan_emi WHERE loanId = :loanId")
    fun getLoanEmisforLoan(loanId: Int): Flow<List<LoanEmis>>

    @Update
    suspend fun updateLoan(loan: Loans)

    @Update
    suspend fun updateLoanEmis(loanEmis: LoanEmis)

    @Update
    suspend fun updateLoanDetails(loanDetails: LoanDetails)

    @Query("SELECT * from fund_details WHERE fundId =:fundId")
    suspend fun getFundDetails(fundId: Int): FundDetails

    @Update
    suspend fun updateFundDetails(details: FundDetails)

    @Delete
    suspend fun deleteLoanEmis(loanEmis: LoanEmis)

    @Query(
        """SELECT l.*, ld.* from loan_details ld
            INNER JOIN loans l ON ld.loanId = l.loanId
            WHERE ld.loanId = :loanId"""
    )
    fun getLoanDetailsforLoan(loanId: Int): LoanCmplDetails

    @Query(
        """
    SELECT 
        e.loanEmiId,
        e.loanId,
        e.emiMonth,
        e.emiYear,
        e.emiDepositedDate,
        e.emiDepositedAmount,
        e.prepaymentAmount,
        e.lateFee,

        l.fundId,
        l.loanNumber,
        l.borrowerId,
        l.issuedDate,
        l.period,
        l.loanAmount,
        l.maturityDate,
        l.rateOfInterest,
        l.status,

        d.emiInterest,
        d.currPrincipal,
        d.totalInterest,
        d.currTotalIntPaid,

        u.firstName,
        u.lastName,
        u.userId

    FROM loans l
    INNER JOIN loan_details d ON l.loanId = d.loanId
    INNER JOIN users u ON l.borrowerId = u.userId
    LEFT JOIN loan_emi e 
        ON l.loanId = e.loanId 
        AND e.emiMonth = :month 
        AND e.emiYear = :year
    WHERE l.fundId = :fundId AND l.status = "ACTIVE"
"""
    )
    suspend fun getAllLoanEmisForFundForMonthYear(
        fundId: Int,
        month: String,
        year: String
    ): List<LoanEmiWithMemberNames>

    @Transaction
    suspend fun saveOrUpdateAllLoanEmisAndFetch(
        loanEmis: List<LoanEmis>,
        fundId: Int,
        month: String,
        year: String
    ): List<LoanEmiWithMemberNames> {
        // Get current FundDetails object
        // calculate totalCurrentDeposit, totalCurrentLateFee,totalExpectedMaturityAmount, totalCurrAmount
        // for Fund and update Fund Details
        var isLoanClosed = false
        val fundDtls = getFundDetails(fundId)
        var totalCurrentInterest = fundDtls.totalCurrentInterestCollected
        var totalCurrentLateFee = fundDtls.totalCurrentLateFee
        var totalCurrAmount = fundDtls.totalCurrAmount
        var totalExpectedMaturityAmount = fundDtls.totalExpectedMaturityAmount
        for (emis in loanEmis) {
            val existing = getloanEmisForLoan(emis.loanId, emis.emiMonth, emis.emiYear)
            if (existing != null) {
                updateLoanEmis(emis.copy(loanEmiId = existing.loanEmiId))
                if (existing.emiDepositedAmount < emis.emiDepositedAmount) {
                    totalCurrentInterest += (emis.emiDepositedAmount - existing.emiDepositedAmount)
                    totalCurrAmount += (emis.emiDepositedAmount - existing.emiDepositedAmount)
                } else if (existing.emiDepositedAmount > emis.emiDepositedAmount) {
                    totalCurrentInterest -= (existing.emiDepositedAmount - emis.emiDepositedAmount)
                    totalCurrAmount -= (existing.emiDepositedAmount - emis.emiDepositedAmount)
                } else {
                    //do nothing.
                }
                if (existing.lateFee < emis.lateFee) {
                    totalCurrentLateFee += emis.lateFee - existing.lateFee
                    totalExpectedMaturityAmount += emis.lateFee - existing.lateFee
                    totalCurrAmount += emis.lateFee - existing.lateFee
                } else if (existing.lateFee > emis.lateFee) {
                    totalCurrentLateFee -= existing.lateFee - emis.lateFee
                    totalExpectedMaturityAmount -= existing.lateFee - emis.lateFee
                    totalCurrAmount -= existing.lateFee - emis.lateFee
                } else {
                    //do nothing.
                }
            } else {
                addLoanDetails(emis)
                totalCurrentInterest += emis.emiDepositedAmount
                totalCurrentLateFee += emis.lateFee
                totalCurrAmount += emis.emiDepositedAmount + emis.lateFee
                totalExpectedMaturityAmount += emis.emiDepositedAmount + emis.lateFee
            }
            val updatedFundDetls = FundDetails(
                fundDetailsId = fundDtls.fundDetailsId,
                fundId = fundId,
                totalExpectedDeposit = fundDtls.totalExpectedDeposit,
                totalCurrentDeposit = fundDtls.totalCurrentDeposit,
                totalCurrentLateFee = totalCurrentLateFee,
                totalCurrentInterestCollected = totalCurrentInterest,
                totalExpectedMaturityAmount = totalExpectedMaturityAmount,
                totalCurrAmount = totalCurrAmount
            )
            updateFundDetails(updatedFundDetls)
            // get loan details and update it as well
            val loanDetls = getLoanDetailsforLoan(emis.loanId)
            var currBal = loanDetls.currPrincipal
            // Calculate new emiInterest
            if (emis.prepaymentAmount > 0 ) {
                if (loanDetls.currPrincipal == loanDetls.origPrincipal) {
                    currBal = loanDetls.origPrincipal - emis.prepaymentAmount
                } else {
                    currBal = loanDetls.currPrincipal - emis.prepaymentAmount
                }
                if (currBal == 0.0) {
                    // all principal is paid off, need to close the loan
                    isLoanClosed = true
                }
            }
            val updtEmiInt = (currBal * loanDetls.rateOfInterest * 1 / 12) / 100
            val interestPaidDelta = emis.emiDepositedAmount - (existing?.emiDepositedAmount ?: 0.0)
            val isPrepayment = emis.prepaymentAmount > 0
            val shouldUpdate = isPrepayment || interestPaidDelta > 0.0

            if (shouldUpdate) {
                val updatedLoanDetails = LoanDetails(
                    loanDetailsId = loanDetls.loanDetailsId,
                    loanId = loanDetls.loanId,
                    origPrincipal = loanDetls.origPrincipal,
                    totalInterest = loanDetls.totalInterest,
                    totalAmount = loanDetls.totalAmount,
                    emiInterest = if (isPrepayment) updtEmiInt else loanDetls.emiInterest,
                    currTotalIntPaid = loanDetls.currTotalIntPaid + interestPaidDelta,
                    currPrincipal = if (isPrepayment) currBal else loanDetls.currPrincipal
                )
                updateLoanDetails(updatedLoanDetails)
            }
            if (isLoanClosed) {
                val updatedLoan = Loans(
                    loanId = loanDetls.loanId,
                    loanNumber = loanDetls.loanNumber,
                    borrowerId = loanDetls.borrowerId,
                    issuedDate = loanDetls.issuedDate,
                    period = loanDetls.period,
                    loanAmount = loanDetls.loanAmount,
                    maturityDate = loanDetls.maturityDate,
                    rateOfInterest = loanDetls.rateOfInterest,
                    status = "Closed",
                    fundId = loanDetls.fundId
                )
                updateLoan(updatedLoan)
            }
        }
        return getAllLoanEmisForFundForMonthYear(fundId, month, year)
    }

    @Query(
        """
        SELECT * FROM loan_emi
        WHERE loanId = :loanId 
        AND emiMonth = :emiMonth
        AND emiYear = :emiYear
        LIMIT 1
    """
    )
    suspend fun getloanEmisForLoan(loanId: Int, emiMonth: String, emiYear: String): LoanEmis?
    @Query(
        """
    SELECT 
        e.loanEmiId,
        e.loanId,
        e.emiMonth,
        e.emiYear,
        e.emiDepositedDate,
        e.emiDepositedAmount,
        e.prepaymentAmount,
        e.lateFee,

        l.fundId,
        l.loanNumber,
        l.borrowerId,
        l.issuedDate,
        l.period,
        l.loanAmount,
        l.maturityDate,
        l.rateOfInterest,
        l.status,

        d.emiInterest,
        d.currPrincipal,
        d.totalInterest,
        d.currTotalIntPaid,

        u.firstName,
        u.lastName,
        u.userId

    FROM loans l
    INNER JOIN loan_details d ON l.loanId = d.loanId
    INNER JOIN users u ON l.borrowerId = u.userId
    LEFT JOIN loan_emi e 
        ON l.loanId = e.loanId 
    WHERE l.loanId = :loanId 
""")
    suspend fun getAllloanEmisForLoan(loanId: Int): List<LoanEmiWithMemberNames>?



}