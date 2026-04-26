package com.mynikatech.apnafund.ui.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mynikatech.apnafund.ApnaFundApplication
import com.mynikatech.apnafund.data.model.LoanDetails
import com.mynikatech.apnafund.data.model.LoanDetailsWithMemberNames
import com.mynikatech.apnafund.data.model.LoanEmiWithMemberNames
import com.mynikatech.apnafund.data.model.LoanEmis
import com.mynikatech.apnafund.data.model.Loans
import com.mynikatech.apnafund.net.dto.LoanDetailsWithMemberNamesDto
import kotlinx.coroutines.launch
import android.util.Log

class LoansViewModel: ViewModel() {

    private val loanRepository = ApnaFundApplication.loanRepository

    private val _fundLoans = MutableLiveData<List<LoanDetailsWithMemberNamesDto>>()
    val fundLoans: LiveData<List<LoanDetailsWithMemberNamesDto>> = _fundLoans

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getAllLoanEmisForFundForMonthYear(
        fundId: Int,
        month: Int,
        year: Int
    ): List<LoanEmiWithMemberNames> {
        return loanRepository.getAllLoanEmisForFundForMonthYear(fundId, month, year)

    }

    suspend fun getAllLoanEmisForLoan(
        loanId: Int,
    ): List<LoanEmiWithMemberNames>? {
        return loanRepository.getAllLoanEmisForLoan(loanId)

    }

    suspend fun saveOrUpdateAllLoansEmiAndFetch(
        loanEmis: List<LoanEmis>,
        fundId: Int,
        month: String,
        year: String
    ): List<LoanEmiWithMemberNames> {
        return loanRepository.saveOrUpdateAllAndFetch(loanEmis, fundId, month, year)
    }

    fun updateLoan(
        loanId: Int,
        loanAmount: Double,
        issueDate: String,
        period: Double,
        maturityDate: String,
        existingloan: LoanDetailsWithMemberNames
    ) {
        viewModelScope.launch {
            val updatedLoan = Loans(
                loanId = loanId,
                borrowerId = existingloan.userId,
                issuedDate = issueDate,
                period = period,
                loanAmount = loanAmount,
                maturityDate = maturityDate,
                rateOfInterest = existingloan.rateOfInterest,
                status = existingloan.status,
                fundId = existingloan.fundId,
                loanNumber = existingloan.loanNumber,
                workflowStatus = existingloan.workflowStatus
            )
            val monthlyInt = (loanAmount * existingloan.rateOfInterest * 1/12)/100
            val totalInt =  monthlyInt * period
            val totalAmt = loanAmount + totalInt

            val loanDetails = existingloan.loanId?.let {
                existingloan.loanDetailsId?.let { it1 ->
                    LoanDetails(
                        loanId = it,
                        loanDetailsId = it1,
                        origPrincipal = loanAmount,
                        totalInterest = totalInt,
                        totalAmount = totalAmt,
                        emiInterest = monthlyInt,
                        currPrincipal = loanAmount,
                        currTotalIntPaid = 0.0
                    )
                }
            }
            if (loanDetails != null) {
                loanRepository.updateLoanAndDetails(updatedLoan,loanDetails)
            }
        }
    }

    fun loadLoansForFund(fundId: Int) {

        viewModelScope.launch {

            try {

                val loans =
                    loanRepository.getLoanDetailsWithNamesForFund(fundId)

                _fundLoans.postValue(loans)

            } catch (e: Exception) {

                Log.e("LoansViewModel", "Error loading fund loans", e)

                _fundLoans.postValue(emptyList())
            }
        }
    }
}