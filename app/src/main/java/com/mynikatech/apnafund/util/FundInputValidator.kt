package com.mynikatech.apnafund.util

object FundInputValidator {

    fun isFundNameValid(name: String?): Boolean {
        return name?.trim()?.length ?: 0 >= 3
    }

    fun isPeriodValid(periodStr: String?): Boolean {
        return periodStr?.toDoubleOrNull() != null
    }

    fun isDepositAmountValid(depAmount: String?): Boolean {
        if (depAmount.isNullOrBlank()) return false

        val amount = depAmount.toDoubleOrNull() ?: return false

        return amount > 0
    }

    fun isLoanInterestValid(interestStr: String?): Boolean {
        return interestStr?.toDoubleOrNull() != null
    }

    fun isLateFeeValid(lateFeeStr: String?): Boolean {
        return lateFeeStr?.toDoubleOrNull() != null
    }

    fun isDepositLastDateValid(dayStr: String?): Boolean {
        val day = dayStr?.toIntOrNull()
        return day != null && day in 1..31
    }

    fun isStartDateValid(dateStr: String?): Boolean {
        return !dateStr.isNullOrBlank()
    }

    fun isDepFrequencyValid(freqStr: String?): Boolean {
        return !freqStr.isNullOrBlank()
    }

    fun isGroupValid(groupId: Int): Boolean {
        return groupId >= 0
    }

    fun isModeratorValid(moderator: Int): Boolean {
        return moderator >= 0
    }

    fun isAllInputValid(
        fundName: String?,
        startDate: String?,
        period: String?,
        depFreq: String?,
        depAmount: String?,
        loanRate: String?,
        lateFee: String?,
        depLastDate: String?,
        hasVariableInterestRate: Boolean,
        revisedLoanInterestRate: String?,
        interestRateRevisionAfterMonths: String?,
        groupId: Int,
        moderator: Int
    ): Boolean {
        return isFundNameValid(fundName)
                && isStartDateValid(startDate)
                && isPeriodValid(period)
                && isDepFrequencyValid(depFreq)
                && isDepositAmountValid(depAmount)
                && isLoanInterestValid(loanRate)
                && isLateFeeValid(lateFee)
                && isDepositLastDateValid(depLastDate)
                && isGroupValid(groupId)
                && isModeratorValid(moderator)
                && isVariableInterestConfigValid(
                hasVariableInterestRate,
                loanRate,
                revisedLoanInterestRate,
                interestRateRevisionAfterMonths
        )
    }

    private fun isVariableInterestConfigValid(
        hasVariableInterestRate: Boolean,
        loanRate: String?,
        revisedLoanInterestRate: String?,
        interestRateRevisionAfterMonths: String?
    ): Boolean {

        if (!hasVariableInterestRate) {
            return true
        }

        val currentRate =
            loanRate?.toDoubleOrNull()
                ?: return false

        val revisedRate =
            revisedLoanInterestRate?.toDoubleOrNull()
                ?: return false

        val revisionMonths =
            interestRateRevisionAfterMonths?.toIntOrNull()
                ?: return false

        return revisedRate > currentRate &&
                revisionMonths > 0
    }

    fun formatCurrency(value: Double): String {
        return "₹" + String.format("%,.2f", value) // use NumberFormat if needed
    }

    fun formatDecimal(value: Double?): String {

        if (value == null) {
            return ""
        }

        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            value.toString()
        }
    }

}