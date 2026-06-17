package com.mynikatech.apnafund.server.mapper

import com.mynikatech.apnafund.net.dto.FundWithDetailsDto
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet

class FundWithDetailsMapper : RowMapper<FundWithDetailsDto> {
    override fun map(rs: ResultSet, ctx: StatementContext): FundWithDetailsDto {
        return FundWithDetailsDto(
            fundId = rs.getInt("fundId"),
            fundName = rs.getString("fundName"),
            fundStartDate = rs.getString("fundStartDate"),
            fundMaturityDate = rs.getString("fundMaturityDate"),
            fundPeriod = rs.getDouble("fundPeriod"),
            depositionFrequency = rs.getString("depositionFrequency"),
            moderator = rs.getInt("moderator"),
            recurringDepositAmount = rs.getDouble("recurringDepositAmount"),
            fundStatus = rs.getString("fundStatus"),
            loanInterestRate = rs.getDouble("loanInterestRate"),
            hasVariableInterestRate =
                rs.getBoolean("hasVariableInterestRate"),

            revisedLoanInterestRate =
                rs.getObject(
                    "revisedLoanInterestRate",
                    java.lang.Double::class.java
                )?.toDouble(),

            interestRateRevisionAfterMonths =
                rs.getObject(
                    "interestRateRevisionAfterMonths",
                    Integer::class.java
                )?.toInt(),

            lateFeeRate = rs.getDouble("lateFeeRate"),
            monthlyDepDateBy = rs.getInt("monthlyDepDateBy"),
            groupId = rs.getInt("groupId"),
            fundCode = rs.getString("fundCode"),

            fundDetailsId = rs.getInt("fundDetailsId"),
            totalExpectedDeposit = rs.getDouble("totalExpectedDeposit"),
            totalCurrentDeposit = rs.getDouble("totalCurrentDeposit"),
            totalCurrentLateFee = rs.getDouble("totalCurrentLateFee"),
            totalCurrentInterestCollected = rs.getDouble("totalCurrentInterestCollected"),
            totalExpectedMaturityAmount = rs.getDouble("totalExpectedMaturityAmount"),
            totalCurrAmount = rs.getDouble("totalCurrAmount"),

            moderatorFirstName = rs.getString("moderatorFirstName"),
            moderatorLastName = rs.getString("moderatorLastName"),
            groupName = rs.getString("groupName")
        )
    }
}