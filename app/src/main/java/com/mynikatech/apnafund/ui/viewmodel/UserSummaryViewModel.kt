package com.mynikatech.apnafund.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mynikatech.apnafund.ApnaFundApplication
import com.mynikatech.apnafund.data.model.FundWithDetails
import com.mynikatech.apnafund.data.model.Funds
import com.mynikatech.apnafund.data.model.LoanDetails
import com.mynikatech.apnafund.data.model.LoanDetailsWithMemberNames
import com.mynikatech.apnafund.data.model.Loans
import com.mynikatech.apnafund.data.model.UserDetails
import com.mynikatech.apnafund.data.model.UserFundDetails
import com.mynikatech.apnafund.data.model.UserLoanDetails
import com.mynikatech.apnafund.data.model.UserNotifications
import com.mynikatech.apnafund.data.model.UserProfile
import kotlinx.coroutines.launch

class UserSummaryViewModel : ViewModel() {
    private val userSummaryRepository = ApnaFundApplication.userSummaryRepository

    private val userRoleRepository = ApnaFundApplication.userRolesRepository

    private val fundRepository = ApnaFundApplication.fundRepository

    private val loanRepository = ApnaFundApplication.loanRepository

    val userName = MutableLiveData<String>()
    private val userRoles = MutableLiveData<List<String>>()
    val groupName = MutableLiveData<String>()
    val groupId = MutableLiveData<Int>()
    var userFunds = MutableLiveData<List<Funds>>()
    private val userNoifications = MutableLiveData<List<UserNotifications>?>()
    val userLoans = MutableLiveData<List<LoanDetailsWithMemberNames>>()

    val selectedFundDetails = MutableLiveData<FundWithDetails?>()

    val fundDepositSummary = MutableLiveData<Double?>()
    val fundMaturity = MutableLiveData<Double?>()
    private val fundLoan = MutableLiveData<Double?>()

    fun loadUserSummary(userId: Int) {
        viewModelScope.launch {
            val userDetails = userSummaryRepository.getUserDetails(userId)
            val group = userDetails.group
            val funds = userDetails.userFunds
            val notifications = userDetails.userNotifications
            userRoles.postValue(userDetails.userRoles)
            userName.postValue("${userDetails.firstName} ${userDetails.lastName}")
            if (null != group) {
                groupName.postValue(group.groupName)
                groupId.postValue((group.groupId))
            }
            userFunds.postValue(funds)
            userNoifications.postValue(notifications)
        }
    }

    fun loadFundDetails(userId: Int, fundId: Int) {
        fundDepositSummary.postValue(null)
        fundMaturity.postValue(null)
        fundLoan.postValue(null)
        userLoans.postValue(emptyList())
        selectedFundDetails.postValue(null)
        viewModelScope.launch {
            val userFundDetails = getUserFundDetails(userId, fundId)
            if (userFundDetails != null) {
                val deposit = userFundDetails.totalDeposit
                val totalLoanAmount = userFundDetails.totalLoanAmount
                val loansDetails = loanRepository.getAllLoanDetailsForFundForUser(fundId, userId)
                val maturityAmount = userFundDetails.userExpMatAmount
                val selFund = userFundDetails.fundDetails
                fundDepositSummary.postValue(deposit)
                fundMaturity.postValue(maturityAmount)
                fundLoan.postValue(totalLoanAmount)
                selectedFundDetails.postValue(selFund)
                userLoans.postValue(loansDetails.ifEmpty { emptyList() })
            }
        }
    }

    suspend fun getUserFundDetails(userId: Int, fundId: Int): UserFundDetails? {
        return userSummaryRepository.getUserFundDetails(userId, fundId)
    }

    suspend fun getUserDetails(userId: Int): UserDetails? {
        return userSummaryRepository.getUserDetails(userId)
    }

    fun applyLoan(
        userId: Int,
        fundId: Int,
        loanAmount: Double,
        issueDate: String,
        period: Double,
        rateOfInt: Double,
        maturityDate: String
    ) {
        viewModelScope.launch {
            val loan = Loans(
                borrowerId = userId,
                issuedDate = issueDate,
                period = period,
                loanAmount = loanAmount,
                maturityDate = maturityDate,
                rateOfInterest = rateOfInt,
                status = "ACTIVE",// to implement approval workflow
                fundId = fundId,
                loanNumber = "LN$userId$issueDate$fundId"
            )
            // create loan details as well.
            // calculate total Interest

            val monthlyInt = (loanAmount * rateOfInt * 1 / 12) / 100
            val totalInt = monthlyInt * period
            val totalAmt = loanAmount + totalInt

            val loanDetails = LoanDetails(
                loanId = 0,
                origPrincipal = loanAmount,
                totalInterest = totalInt,
                totalAmount = totalAmt,
                emiInterest = monthlyInt,
                currPrincipal = loanAmount,
                currTotalIntPaid = 0.0
            )
            loanRepository.saveLoanAndDetails(loan, loanDetails)
            loadFundDetails(userId, fundId) // refresh loan info
        }
    }

    suspend fun getfundRateOfInterest(fundId: Int): Double {

        return userSummaryRepository.getRateOfInterestforFund(fundId)
    }

    suspend fun getFund(fundId: Int): Funds? {
        return fundRepository.fetchFund(fundId)
    }

    suspend fun getTotalAmountAvailableforFund(fundId: Int): Double? {
        return fundRepository.getAvailableFundAmount(fundId)
    }

    suspend fun getTotalLoanAmountforUserforFund(userId: Int, fundId: Int): Double {
        return userSummaryRepository.getTotalLoanAmount(userId, fundId)
    }

    suspend fun getFundDetails(fundId: Int): FundWithDetails {
        return fundRepository.getFundWithDetails(fundId)
    }

    suspend fun getTotalPendingAmount(userId: Int, fundId: Int): Double {
        return loanRepository.getTotalPendingAmount(userId, fundId)
    }

    suspend fun getTotalCurrIntPaid(userId: Int, fundId: Int): Double {
        return loanRepository.getTotalCurrIntPaid(userId, fundId)
    }

    suspend fun getUserProfile(userId: Int): List<UserProfile> {
        return userRoleRepository.getUserProfile(userId)
    }

    suspend fun getUserLoanDetails(userId: Int, fundId: Int): UserLoanDetails {
        return loanRepository.getUserLoanDetails(userId, fundId)
    }


}
