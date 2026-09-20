package com.mynikatech.apnafund.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.mynikatech.apnafund.data.dao.LoanEmisDao
import com.mynikatech.apnafund.data.dao.LoansDao
import com.mynikatech.apnafund.data.mappers.toDto
import com.mynikatech.apnafund.data.mappers.toEntity
import com.mynikatech.apnafund.data.model.LoanDetails
import com.mynikatech.apnafund.data.model.LoanDetailsWithMemberNames
import com.mynikatech.apnafund.data.model.LoanEmiWithMemberNames
import com.mynikatech.apnafund.data.model.LoanEmis
import com.mynikatech.apnafund.data.model.Loans
import com.mynikatech.apnafund.data.model.UserLoanDetails
import com.mynikatech.apnafund.net.LoansApi
import com.mynikatech.apnafund.net.dto.LoanClosureRequestDto
import com.mynikatech.apnafund.net.dto.LoanCmplDetailsDto
import com.mynikatech.apnafund.net.dto.LoanDetailsWithMemberNamesDto
import com.mynikatech.apnafund.net.dto.PendingLoanEmiDto
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class LoanRepository(
    private val loansDao: LoansDao, private val loansEmiDao: LoanEmisDao,
    private val loansApi: LoansApi
) {

    suspend fun saveLoanAndDetails(loan: Loans, loanDetails: LoanDetails, requestorId: Int) {
        loansApi.insertLoanWithDetails(loan, loanDetails, requestorId)
    }

    suspend fun updateLoanAndDetails(loan: Loans, loanDetails: LoanDetails) {
        loansApi.updateLoanWithDetails(loan, loanDetails)
    }

    suspend fun getLoanById(loanId: Int): LoanCmplDetailsDto? {
        return loansApi.get(loanId)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getAllLoanEmisForFundForMonthYear(
        fundId: Int,
        month: Int,
        year: Int
    ): List<LoanEmiWithMemberNames> {
        val monthStr = month.toString()
        val yearStr = year.toString()
        val allLoans = loansApi.getAllLoanEmisForFundForMonthYear(fundId, monthStr, yearStr)
        val validLoans = allLoans.filter {
            isLoanActiveInMonthYear(it.issuedDate, it.period, it.maturityDate, month, year)
        }
        return validLoans.toEntity()
    }

    suspend fun getAllLoanEmisForLoan(
        loanId: Int
    ): List<LoanEmiWithMemberNames>? {
        val loanEmis = loansApi.getAllloanEmisForLoan(loanId)
        return loanEmis?.toEntity()
    }

    suspend fun getAllLoanDetailsForFundForUser(
        fundId: Int,
        userId: Int
    ): List<LoanDetailsWithMemberNames> {
        return loansApi.getAllLoanDetailsForFundForUser(fundId, userId).toEntity()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun saveOrUpdateAllAndFetch(
        loanEmis: List<LoanEmis>,
        fundId: Int,
        month: String,
        year: String
    ): List<LoanEmiWithMemberNames> {

        val allLoans =
            loansApi.saveOrUpdateAllLoanEmisAndFetch(
                loanEmis.toDto(),
                fundId,
                month,
                year
            )

        val validLoans = allLoans.filter {
            isLoanActiveInMonthYear(
                it.issuedDate,
                it.period,
                it.maturityDate,
                month.toInt(),
                year.toInt()
            )
        }

        return validLoans.toEntity()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun isLoanActiveInMonthYear(
        issuedDateStr: String,
        period: Double,
        maturityDateStr: String,
        selectedMonth: Int,
        selectedYear: Int
    ): Boolean {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val issuedDate = LocalDate.parse(issuedDateStr, formatter)
        val maturityDate = LocalDate.parse(maturityDateStr, formatter)
        val selectedDate = LocalDate.of(selectedYear, selectedMonth, 1)

        return !selectedDate.isBefore(issuedDate.withDayOfMonth(1)) && !selectedDate.isAfter(
            maturityDate
        )
    }

    suspend fun getTotalPendingAmount(userId: Int, fundId: Int): Double {
        return loansApi.getTotalPendingAmount(userId, fundId)
    }

    suspend fun getTotalCurrIntPaid(userId: Int, fundId: Int): Double {
        return loansApi.getTotalCurrIntPaid(userId, fundId)
    }

    suspend fun getUserLoanDetails(userId: Int, fundId: Int): UserLoanDetails {
        return loansApi.getUserLoanDetails(userId, fundId).toEntity()
    }

    suspend fun getLoanDetailsWithNamesForFund(
        fundId: Int
    ): List<LoanDetailsWithMemberNamesDto> {

        return loansApi.listByFund(fundId)
    }

    suspend fun requestLoanClosure(
        loanId: Int,
        requestedAmount: Double?,
        remarks: String?
    ) {
        loansApi.requestLoanClosure(
            LoanClosureRequestDto(
                loanId = loanId,
                requestedAmount = requestedAmount,
                remarks = remarks
            )
        )
    }

    suspend fun hasPendingClosureRequest(loanId: Int): Boolean {
        return loansApi.hasPendingClosureRequest(loanId)
    }

    suspend fun closeLoanDirect(loanId: Int, closureDate: String, userId: Int) {
        loansApi.closeLoanDirect(loanId, closureDate, userId)
    }

    suspend fun deleteLoan(loanId: Int, userId: Int) {
        loansApi.deleteLoan(loanId, userId)
    }

    suspend fun getPendingLoanEmisForClosure(
        loanId: Int,
        month: String,
        year: String
    ): List<PendingLoanEmiDto> {
        return loansApi.getPendingLoanEmisForClosure(
            loanId,
            month,
            year
        )
    }

}