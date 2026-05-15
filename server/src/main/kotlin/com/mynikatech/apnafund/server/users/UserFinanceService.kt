package com.mynikatech.apnafund.server.users

import com.mynikatech.apnafund.net.dto.DepositsWithMemberNamesDto
import com.mynikatech.apnafund.net.dto.FundMemberWithNameDto
import com.mynikatech.apnafund.net.dto.FundWithDetailsDto
import com.mynikatech.apnafund.net.dto.GroupMemberWithNameDto
import com.mynikatech.apnafund.net.dto.GroupsDto
import com.mynikatech.apnafund.net.dto.LoanDetailsWithMemberNamesDto
import com.mynikatech.apnafund.net.dto.LoanEmiWithMemberNamesDto
import com.mynikatech.apnafund.server.deposits.DepositsSql
import com.mynikatech.apnafund.server.funds.FundsSql
import com.mynikatech.apnafund.server.groups.GroupsSql
import com.mynikatech.apnafund.server.loans.LoansSql

class UserFinanceService(
    private val userService: UsersSql,
    private val fundService: FundsSql,
    private val loanService: LoansSql,
    private val groupService: GroupsSql,
    private val depositService: DepositsSql
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
    fun getGroupssForUser(userId: Int): List<GroupsDto> {

        val groups = userService.getGroupsForUser(userId) ?: emptyList()

        return groups
    }

    fun getDepositsForUser(userId: Int): List<DepositsWithMemberNamesDto> {

        val funds = getFundsForUser(userId)

        return funds
            .mapNotNull { it.fundId } // handle null safely
            .flatMap { fundId ->
                depositService.getDepositsWithNamesForFund(fundId)
            }
    }

    fun getLoanEMIsForUser(userId: Int): List<LoanEmiWithMemberNamesDto> {

        val funds = getFundsForUser(userId)

        return funds
            .mapNotNull { it.fundId } // safe
            .flatMap { fundId ->
                loanService.getLoansForFund(fundId)
            }
            .mapNotNull { it.loanId } // safe
            .flatMap { loanId ->
                loanService.getAllLoanEmisWithNamesForLoan(loanId)
            }
    }

    fun getGroupMembersWithNamesForGroup(groupId: Int): List<GroupMemberWithNameDto> {

        val fundMembers = groupService.getAllMembersofGroupWithNames(groupId, false)

        return fundMembers
    }


    fun getFundMembersWithNamesForFund(fundId: Int): List<FundMemberWithNameDto> {

       val fundMembers = fundService.getFundMembersWithNamesForFund(fundId)

       return fundMembers
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