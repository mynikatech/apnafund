package com.mynikatech.apnafund.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mynikatech.apnafund.ApnaFundApplication
import com.mynikatech.apnafund.Exception.InvalidSessionException
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
import com.mynikatech.apnafund.session.SessionManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import android.util.Log

class UserSummaryViewModel : ViewModel() {
    private val userSummaryRepository = ApnaFundApplication.userSummaryRepository

    private val userRoleRepository = ApnaFundApplication.userRolesRepository

    private val fundRepository = ApnaFundApplication.fundRepository

    private val loanRepository = ApnaFundApplication.loanRepository

    private val groupsRepository = ApnaFundApplication.groupRepository

    private val _sessionInvalid = MutableSharedFlow<Unit>()
    val sessionInvalid = _sessionInvalid.asSharedFlow()

    val userName = MutableLiveData<String>()
    private val userRoles = MutableLiveData<List<String>>()
    val groupName = MutableLiveData<String>()
    val groupId = MutableLiveData<Int?>()
    var userFunds = MutableLiveData<List<Funds>>()
    private val userNoifications = MutableLiveData<List<UserNotifications>?>()
    val userLoans = MutableLiveData<List<LoanDetailsWithMemberNames>>()

    val selectedFundDetails = MutableLiveData<FundWithDetails?>()

    val fundDepositSummary = MutableLiveData<Double?>()
    val fundMaturity = MutableLiveData<Double?>()
    private val fundLoan = MutableLiveData<Double?>()

    fun loadUserSummary(userId: Int, selectedGroupId: Int?) {

        viewModelScope.launch {

            val userDetails = userSummaryRepository.getUserDetails(userId, selectedGroupId)

            val groups = userDetails.groups ?: emptyList()
            val funds = userDetails.userFunds
            val notifications = userDetails.userNotifications

            userRoles.postValue(userDetails.userRoles)
            userName.value = "${userDetails.firstName} ${userDetails.lastName}"

            SessionManager.userGroups = groups

            if (groups.isEmpty()) {
                SessionManager.userGroups = emptyList()
                SessionManager.groupId = null
                SessionManager.groupName = null

                groupName.postValue("--")
                groupId.postValue(null)

                userFunds.postValue(emptyList())
                userNoifications.postValue(notifications)

                return@launch
            }

            val selectedGroup =
                groups.firstOrNull { it.groupId == selectedGroupId }
                    ?: groups.first()

            SessionManager.groupId = selectedGroup.groupId
            SessionManager.groupName = selectedGroup.groupName

            groupName.postValue(selectedGroup.groupName)
            groupId.postValue(selectedGroup.groupId)

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
        maturityDate: String,
        autoapprove: Boolean,
        requestorId: Int
    ) {
        viewModelScope.launch {
            val workflowStatus =
                if (autoapprove) "APPROVED"
                else "PENDING_APPROVAL"

            val loan = Loans(
                borrowerId = userId,
                issuedDate = issueDate,
                period = period,
                loanAmount = loanAmount,
                maturityDate = maturityDate,
                rateOfInterest = rateOfInt,
                status = "ACTIVE",// to implement approval workflow
                workflowStatus = workflowStatus,
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
            loanRepository.saveLoanAndDetails(loan, loanDetails, requestorId)
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

    suspend fun getUserProfile(userId: Int): UserProfile {
        return try {
            userRoleRepository.getUserProfile(userId)
        } catch (e: InvalidSessionException) {
            _sessionInvalid.emit(Unit)
            throw e
        }
    }

    suspend fun getUserLoanDetails(userId: Int, fundId: Int): UserLoanDetails {
        return loanRepository.getUserLoanDetails(userId, fundId)
    }

    fun syncFirebaseUidIfNeeded() {

        Log.d("FireBase"," Firebase Synced ${SessionManager.isFirebaseSynced}")
        Log.d("FireBase"," Firebase UID ${SessionManager.firebaseUid}")
        Log.d("FireBase"," Groups ${SessionManager.userGroups}")

        if (SessionManager.isFirebaseSynced ||
            SessionManager.firebaseUid.isBlank()
        ) return

        val groups = SessionManager.userGroups ?: return

        viewModelScope.launch {
            try {

                groups.forEach { group ->
                    groupsRepository.syncFirebaseUid(
                        userId = SessionManager.userId,
                        groupId = group.groupId,
                        firebaseUid = SessionManager.firebaseUid
                    )
                }

                SessionManager.isFirebaseSynced = true

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


}
