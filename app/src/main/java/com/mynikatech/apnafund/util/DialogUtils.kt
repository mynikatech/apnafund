package com.mynikatech.apnafund.util

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.data.model.LoanEmiWithMemberNames
import com.mynikatech.apnafund.databinding.DialogLoanDetailsBinding
import com.mynikatech.apnafund.net.dto.LoanDetailsWithMemberNamesDto

fun Context.showLoanDetailsDialog(loan: LoanEmiWithMemberNames) {

    val binding = DialogLoanDetailsBinding.inflate(LayoutInflater.from(this))

    binding.textBorrower.text = loan.fullName
    binding.textLoanNo.text = loan.loanNumber
    binding.textAmount.text = Converters.formatCurrency(loan.loanAmount)
    binding.textOutstanding.text = Converters.formatCurrency(loan.currPrincipal)
    binding.textInterest.text = Converters.formatCurrency(loan.currTotalIntPaid)
    binding.textIssuedDate.text = loan.issuedDate
    binding.textStatus.text = loan.status

    AlertDialog.Builder(this)
        .setTitle(getString(R.string.title_loan_details))
        .setView(binding.root)
        .setPositiveButton(getString(R.string.text_button_ok), null)
        .show()
}

fun Context.showLoanDetailsSummaryDialog(loan: LoanDetailsWithMemberNamesDto) {

    val binding = DialogLoanDetailsBinding.inflate(LayoutInflater.from(this))

    binding.textBorrower.text = loan.borrowerName
    binding.textLoanNo.text = loan.loanNumber
    binding.textAmount.text = Converters.formatCurrency(loan.loanAmount)
    binding.textOutstanding.text = Converters.formatCurrency(loan.currPrincipal)
    binding.textInterest.text = Converters.formatCurrency(loan.currTotalIntPaid)
    binding.textIssuedDate.text = loan.issuedDate
    binding.textStatus.text = loan.status

    AlertDialog.Builder(this)
        .setTitle(getString(R.string.title_loan_details))
        .setView(binding.root)
        .setPositiveButton(getString(R.string.text_button_ok), null)
        .show()
}
