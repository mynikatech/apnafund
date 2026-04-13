package com.mynikatech.apnafund.server.users

import com.mynikatech.apnafund.net.dto.FundWithDetailsDto
import com.mynikatech.apnafund.net.dto.LoanDetailsWithMemberNamesDto
import com.mynikatech.apnafund.server.funds.FundsSql
import com.mynikatech.apnafund.server.loans.LoansSql

class UserFinanceService(
    private val userService: UsersSql,
    private val fundService: FundsSql,
    private val loanService: LoansSql
) {

    fun getFundsForUser(userId: Int): List<FundWithDetailsDto> {

        val groups = userService.getGroupsForUser(userId) ?: emptyList()

        val funds = mutableListOf<FundWithDetailsDto>()

        for (group in groups) {
            val groupFunds = fundService.allWithDetailsForGroup(group.groupId!!)
            funds.addAll(groupFunds)
        }

        return funds
    }

    fun getLoansForUser(userId: Int): List<LoanDetailsWithMemberNamesDto> {

        val funds = getFundsForUser(userId)

        val loans = mutableListOf<LoanDetailsWithMemberNamesDto>()

        for (fund in funds) {
            val fundLoans = loanService.getLoanDetailsWithNamesForFund(fund.fundId)
            loans.addAll(fundLoans)
        }
        return loans
    }
}