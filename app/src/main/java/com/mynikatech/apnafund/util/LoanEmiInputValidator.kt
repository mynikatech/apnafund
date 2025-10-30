package com.mynikatech.apnafund.util

import android.os.Build
import androidx.annotation.RequiresApi
import com.mynikatech.apnafund.data.model.LoanEmiWithMemberNames
import com.mynikatech.apnafund.data.model.LoanEmis
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object LoanEmiInputValidator {

    @RequiresApi(Build.VERSION_CODES.O)
    fun validateLoanEmi(
        original: LoanEmiWithMemberNames,
        edited: LoanEmis,
        selectedMonth: Int,
        selectedYear: Int
    ): String? {

        val finalPayStatus = validateFinalPayment(original, edited, selectedMonth, selectedYear)
        // 1. Validate final EMI full payment principal plus emiInterest when 1
        // 4. Validate that if this is the final EMI then if Interest has been paid fully as well when 2
        when (finalPayStatus) {
            0 -> return null
            1 -> return "Final EMI month: ₹${original.currPrincipal} must be fully paid for Loan ${original.loanNumber}."
            2 -> return "Final EMI month: ₹${original.totalInterest - original.currTotalIntPaid} must be fully paid for Loan ${original.loanNumber}."
            //3 -> return  "The PrepayAmount or Principal is more than the ₹${original.currPrincipal} "
        }
        // 2. Validate prepayment multiple
        if (!DepositInputValidator.isPrepayMultipleOf(edited.prepaymentAmount, 5000)) {
            return "Can prepay only in multiple of Rs 5000"
        }

        // 3. Validate prepayment doesn't exceed balance
        if (edited.prepaymentAmount > original.currPrincipal.toInt()) {
            return "Only ₹${original.currPrincipal} is left to pay"
        }

        if (edited.emiDepositedAmount < (original.totalInterest - original.currTotalIntPaid)) {
            return """Final EMI month: ₹${original.totalInterest - original.currTotalIntPaid} must be fully paid for Loan ${original.loanNumber}."""
        }

        return null // No error
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun validateFinalPayment(
        loanEmiWithLoanDtls: LoanEmiWithMemberNames, loanEmi: LoanEmis,
        selectedMonth: Int, selectedYear: Int
    ): Int {
        var isFinalPrinNotPaid = 0
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        val issuedDate = LocalDate.parse(loanEmiWithLoanDtls.issuedDate, formatter)
        val maturityDate = issuedDate.plusMonths(loanEmiWithLoanDtls.period.toLong())

        // Final payment is expected in the *month after* the maturity date
        val finalEmiMonthDate = maturityDate.plusMonths(1)

        // Selected EMI month/year from dropdowns
        val selectedMonthDate = LocalDate.of(selectedYear, selectedMonth, 1)

        // Check if this is the final EMI month
        val isFinalMonth = selectedMonthDate.year == finalEmiMonthDate.year &&
                selectedMonthDate.month == finalEmiMonthDate.month

        if (isFinalMonth) {
            val totalPaid = loanEmi.prepaymentAmount
            val remaining = loanEmiWithLoanDtls.currPrincipal
            if (totalPaid - remaining > 0.00) {
                isFinalPrinNotPaid = 3
            } else if (totalPaid - remaining < 0.00) {
                isFinalPrinNotPaid = 1
            }
            if (loanEmi.emiDepositedAmount < (loanEmiWithLoanDtls.totalInterest
                        - (loanEmiWithLoanDtls.currTotalIntPaid - loanEmiWithLoanDtls.emiDepositedAmount!!))
            ) {
                isFinalPrinNotPaid = 2
            }
        }
        return isFinalPrinNotPaid
    }

}