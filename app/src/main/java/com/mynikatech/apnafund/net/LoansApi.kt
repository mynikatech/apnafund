package com.mynikatech.apnafund.net

import com.mynikatech.apnafund.data.model.LoanDetails
import com.mynikatech.apnafund.data.model.LoanEmis
import com.mynikatech.apnafund.data.model.Loans
import com.mynikatech.apnafund.net.dto.LoanClosureRequestDto
import com.mynikatech.apnafund.net.dto.LoanCmplDetailsDto
import com.mynikatech.apnafund.net.dto.LoanDetailsDto
import com.mynikatech.apnafund.net.dto.LoanDetailsWithMemberNamesDto
import com.mynikatech.apnafund.net.dto.LoanEmiWithMemberNamesDto
import com.mynikatech.apnafund.net.dto.LoanEmisDto
import com.mynikatech.apnafund.net.dto.LoansDto
import com.mynikatech.apnafund.net.dto.PendingLoanEmiDto
import com.mynikatech.apnafund.net.dto.UserLoanDetailsDto
import kotlinx.coroutines.flow.Flow

interface LoansApi {
    // Loans (core)
    suspend fun getAllLoans(): Flow<List<LoansDto>>
    fun getAllLoansforFund(fundId: Int): Flow<List<LoansDto>>
    suspend fun listByFund(fundId: Int): List<LoanDetailsWithMemberNamesDto>
    suspend fun get(id: Int): LoanCmplDetailsDto?
    suspend fun addLoan(dto: LoansDto): Int
    suspend fun createLoan(loan: Loans): Long
    suspend fun updateLoan(id: Int, dto: LoansDto): Boolean
    suspend fun deleteLoan(id: Int): Boolean

    // EMIs
    fun getAllLoanEmis(): Flow<List<LoanEmisDto>>
    suspend fun addLoanEmi(dto: LoanEmisDto): Int
    fun getLoanEmisforLoan(loanId: Int): Flow<List<LoanEmisDto>>
    suspend fun updateLoanEmis(loanEmis: LoanEmis): Boolean
    suspend fun deleteLoanEmis(loanEmis: LoanEmis): Boolean
    suspend fun getAllLoanEmisForFundForMonthYear(
        fundId: Int,
        month: String,
        year: String
    ): List<LoanEmiWithMemberNamesDto>

    suspend fun saveOrUpdateAllLoanEmisAndFetch(
        loanEmis: List<LoanEmisDto>,
        fundId: Int,
        month: String,
        year: String
    ): List<LoanEmiWithMemberNamesDto>

    suspend fun getloanEmisForLoan(loanId: Int, emiMonth: String, emiYear: String): LoanEmisDto?
    suspend fun getAllloanEmisForLoan(loanId: Int): List<LoanEmiWithMemberNamesDto>?

    // Details row (optional if your server exposes it separately)
    suspend fun getAllLoanDetails(loanId: Int): LoanDetailsDto?
    suspend fun upsertDetails(dto: LoanDetailsDto): Int
    suspend fun addLoanDetails(details: LoanDetails)
    suspend fun updateLoanDetails(details: LoanDetails)
    suspend fun deleteLoanDetails(loanDetails: LoanDetails)
    suspend fun getAllLoanDetailsForFundForUser(
        fundId: Int, userId: Int
    ): List<LoanDetailsWithMemberNamesDto>

    suspend fun getLoanDetailsforLoan(loanId: Int): LoanCmplDetailsDto
    suspend fun isLoanforUserforFund(userId: Int, fundId: Int): Boolean
    suspend fun insertLoanWithDetails(loan: Loans, laonDetails: LoanDetails, requestorId: Int)
    suspend fun updateLoanWithDetails(loan: Loans, loanDetails: LoanDetails)
    suspend fun getTotalLoanAmount(userId: Int, fundId: Int): Double
    suspend fun getTotalPendingAmount(userId: Int, fundId: Int): Double
    suspend fun getTotalCurrIntPaid(userId: Int, fundId: Int): Double
    suspend fun getUserLoanDetails(userId: Int, fundId: Int): UserLoanDetailsDto

    suspend fun requestLoanClosure(loanClosureRequest: LoanClosureRequestDto)
    suspend fun hasPendingClosureRequest(loanId: Int): Boolean
    suspend fun closeLoanDirect(loanId: Int, closureDate: String, userId: Int)
    suspend fun deleteLoan(loanId: Int, userId: Int)
    suspend fun getPendingLoanEmisForClosure(
        loanId: Int,
        month: String,
        year: String
    ): List<PendingLoanEmiDto>

}
