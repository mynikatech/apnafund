package com.mynikatech.apnafund.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.mynikatech.apnafund.data.model.LoanDetails
import com.mynikatech.apnafund.data.model.LoanDetailsWithMemberNames
import com.mynikatech.apnafund.data.model.Loans
import com.mynikatech.apnafund.data.model.UserLoanDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface LoansDao {

    @Insert
    suspend fun addLoan(loan: Loans)

    @Insert
    suspend fun createLoan(loan: Loans): Long

    @Insert
    suspend fun insertLoanDetails(details: LoanDetails)

    @Update
    suspend fun updateLoanDetails(details: LoanDetails)

    @Query("SELECT * from loans")
    fun getAllLoans(): Flow<List<Loans>>

    @Query("SELECT * from loans WHERE fundId = :fundId")
    fun getAllLoansforFund(fundId: Int): Flow<List<Loans>>

    @Update
    suspend fun updateLoan(loan: Loans)

    @Delete
    suspend fun deleteLoan(loan: Loans)

    @Query(
        """SELECT SUM(loanAmount) FROM loans 
        WHERE borrowerId = :userId AND fundId = :fundId
        """
    )
    suspend fun getTotalLoanAmount(userId: Int, fundId: Int): Double

    @Query(
        """SELECT 
            SUM(ld.currPrincipal + (ld.totalInterest - ld.currTotalIntPaid)) AS totalOutstandingAmount
            FROM loan_details ld
            JOIN Loans l ON ld.loanId = l.loanId
            WHERE l.borrowerId = :userId AND l.fundId = :fundId
        """
    )
    suspend fun getTotalPendingAmount(userId: Int, fundId: Int): Double

    @Query(
        """SELECT 
            SUM(ld.currTotalIntPaid) AS totalCurrIntPaid
            FROM loan_details ld
            JOIN Loans l ON ld.loanId = l.loanId
            WHERE l.borrowerId = :userId AND l.fundId = :fundId
        """
    )
    suspend fun getTotalCurrIntPaid(userId: Int, fundId: Int): Double


    @Query(
        """SELECT 
            SUM(ld.currPrincipal + (ld.totalInterest - ld.currTotalIntPaid)) AS totalOutstandingAmount,
            SUM(ld.currTotalIntPaid) AS totalCurrIntPaid
            FROM loan_details ld
            JOIN Loans l ON ld.loanId = l.loanId
            WHERE l.borrowerId = :userId AND l.fundId = :fundId
        """
    )
    suspend fun getUserLoanDetails(userId: Int, fundId: Int): UserLoanDetails



    @Transaction
    suspend fun insertLoanWithDetails(loan: Loans, laonDetails: LoanDetails) {
        val loanId = createLoan(loan).toInt()
        val detailsWithLoanId = laonDetails.copy(loanId = loanId)
        insertLoanDetails(detailsWithLoanId)
    }


    @Transaction
    suspend fun updateLoanWithDetails(loan: Loans, loanDetails: LoanDetails) {
        updateLoan(loan)
        val detailsWithLoanId = loanDetails.copy(loanId = loanDetails.loanDetailsId)
        updateLoanDetails(detailsWithLoanId)
    }

    @Query(
        """SELECT CAST(COUNT(1) AS BIT) FROM loans 
        WHERE borrowerId = :userId AND fundId = :fundId"""
    )
    suspend fun isLoanforUserforFund(userId: Int, fundId: Int): Boolean


    @Query(
        """
    SELECT 

        l.fundId,
        l.loanId,
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
        d.currTotalIntPaid,
        d.totalInterest,
        d.loanDetailsId,

        u.firstName,
        u.lastName,
        u.userId

    FROM loans l
    INNER JOIN loan_details d ON l.loanId = d.loanId
    INNER JOIN users u ON l.borrowerId = u.userId
    WHERE l.fundId = :fundId AND u.userId = :userId
"""
    )
    suspend fun getAllLoanDetailsForFundForUser(
        fundId: Int, userId: Int
    ): List<LoanDetailsWithMemberNames>


}