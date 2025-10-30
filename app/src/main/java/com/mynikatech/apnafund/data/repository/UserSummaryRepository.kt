package com.mynikatech.apnafund.data.repository

import com.mynikatech.apnafund.data.mappers.toDto
import com.mynikatech.apnafund.data.mappers.toEntity
import com.mynikatech.apnafund.data.model.Loans
import com.mynikatech.apnafund.data.model.UserDetails
import com.mynikatech.apnafund.data.model.UserFundDetails
import com.mynikatech.apnafund.data.model.UserNotifications
import com.mynikatech.apnafund.net.FundsApi
import com.mynikatech.apnafund.net.LoansApi
import com.mynikatech.apnafund.net.NotificationsApi
import com.mynikatech.apnafund.net.UserRolesApi
import com.mynikatech.apnafund.net.UsersApi

class UserSummaryRepository(
    private val usersApi: UsersApi,
    private val loansApi: LoansApi,
    private val usersRolesApi: UserRolesApi,
    private val fundsApi: FundsApi,
    private val notificationsApi: NotificationsApi
) {
    suspend fun getUser(userId: Int) = usersApi.getUser(userId)
    suspend fun addloan(loan: Loans) = loansApi.addLoan(loan.toDto())
    suspend fun isLoanforUserforFund(userId: Int, fundId: Int) =
        loansApi.isLoanforUserforFund(userId, fundId)

    suspend fun getTotalLoanAmount(userId: Int, fundId: Int): Double =
        loansApi.getTotalLoanAmount(userId, fundId)

    suspend fun getGroupForUser(userId: Int) = usersApi.getGroupForUser(userId)
    suspend fun getFundsForUser(userId: Int) = usersApi.getFundsForUser(userId)
    suspend fun getRateOfInterestforFund(fundId: Int) = fundsApi.getRateOfInterestforFund(fundId)
    suspend fun getTotalDeposit(userId: Int, fundId: Int): Double? {
        return usersApi.getTotalDeposit(userId, fundId)
    }

    suspend fun getPerMemberExpectedMaturityAmount(fundId: Int) =
        usersApi.getPerMemberExpectedMaturityAmount(fundId)

    suspend fun getAllUserRoles(userId: Int): List<String> {
        return usersRolesApi.getAllUserRoles(userId)
    }

    suspend fun getUserNotifications(userId: Int): List<UserNotifications>? {
        return notificationsApi.getUserNotifications(userId)?.toEntity()
    }

    suspend fun getUserDetails(userId: Int): UserDetails {
        return usersApi.getUserDetails(userId).toEntity()
    }

    suspend fun getUserFundDetails(userId: Int, fundId: Int): UserFundDetails? {
        return usersApi.getUserFundDetails(userId, fundId)?.toEntity()
    }
}
