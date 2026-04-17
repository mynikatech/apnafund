package com.mynikatech.apnafund.ui.loan

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.color.MaterialColors
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.constants.ApnaBankConstants
import com.mynikatech.apnafund.databinding.FragmentFundLoanSummaryBinding
import com.mynikatech.apnafund.net.dto.LoanDetailsWithMemberNamesDto
import com.mynikatech.apnafund.ui.viewmodel.LoansViewModel
import com.mynikatech.apnafund.util.Converters
import kotlinx.coroutines.launch

class FundLoanSummaryFragment : Fragment() {

    private lateinit var binding: FragmentFundLoanSummaryBinding

    private val loansViewModel: LoansViewModel by viewModels()

    private var fundId: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentFundLoanSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        val args = FundLoanSummaryFragmentArgs.fromBundle(requireArguments())

        fundId = args.fundId
        binding.toolbar.title = getString(R.string.title_fund_loans_summary)

        binding.textFundName.text =
            getString(R.string.fund_loans_title, args.fundName)

        // Back button
        binding.toolbar.setNavigationIcon(R.drawable.ic_back_arrow)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        lifecycleScope.launch {
            loansViewModel.loadLoansForFund(fundId)
        }

        loansViewModel.fundLoans.observe(viewLifecycleOwner) { loans ->

            clearTable()

            addHeaderRow()

            updateSummary(loans)

            loans.forEachIndexed { index, loan ->
                addLoanRow(loan, index)
            }
        }
    }

    private fun clearTable() {
        binding.tableLoans.removeAllViews()
    }

    private fun addHeaderRow() {



        val row = TableRow(requireContext())
        val colorPrimary = MaterialColors.getColor(
            row,
            com.google.android.material.R.attr.colorPrimary
        )
        row.setBackgroundResource(R.drawable.table_cell_shape)
        row.setBackgroundColor(colorPrimary)
        val headers = listOf(
            getString(R.string.text_borrower),
            getString(R.string.text_loan_num),
            getString(R.string.text_amount),
            getString(R.string.text_issued),
            getString(R.string.text_principal),
            getString(R.string.text_interest),
            getString(R.string.text_status),
            getString(R.string.text_workflow)
        )

        headers.forEachIndexed { index, title ->

            val tv = TextView(requireContext())
            val colorOnPrimary = MaterialColors.getColor(
                tv,
                com.google.android.material.R.attr.colorOnPrimary
            )
            tv.text = title
            tv.setPadding(16, 10, 16, 10)
            tv.setTypeface(null, Typeface.BOLD)
            tv.setTextColor(colorOnPrimary)
            tv.isSingleLine = true
            tv.maxLines = 1
            if (index == 2 || index == 4 || index == 5)
                tv.gravity = Gravity.END

            row.addView(tv)
        }

        binding.tableLoans.addView(row)
    }

    private fun addLoanRow(loan: LoanDetailsWithMemberNamesDto, index: Int) {

        val row = TableRow(requireContext())

        val surfaceVariant = MaterialColors.getColor(
            row,
            com.google.android.material.R.attr.colorSurfaceVariant
        )

        if (index % 2 == 0) {
            row.setBackgroundColor(surfaceVariant)
        }

        val borrower = "${loan.firstName} ${loan.lastName}"

        val values = listOf(
            borrower,
            loan.loanNumber,
            "₹${loan.loanAmount}",
            loan.issuedDate,
            "₹${loan.currPrincipal}",
            "₹${loan.currTotalIntPaid}",
            loan.status,
            loan.workflowStatus
        )

        values.forEachIndexed { i, value ->

            val tv = TextView(requireContext())
            val textColor = MaterialColors.getColor(
                tv,
                com.google.android.material.R.attr.colorOnSurface
            )
            tv.text = value
            tv.setPadding(16, 8, 16, 8)

            // Prevent wrapping
            tv.isSingleLine = true
            tv.maxLines = 1
            tv.ellipsize = TextUtils.TruncateAt.END
            // Right align numbers
            if (i == 2 || i == 4 || i == 5)
                tv.gravity = Gravity.END

            if (i == 7) {
                tv.setTextColor(getWorkflowColor(value))
            } else {
                tv.setTextColor(textColor)
            }

            row.addView(tv)
        }

        binding.tableLoans.addView(row)
    }

    private fun updateSummary(loans: List<LoanDetailsWithMemberNamesDto>) {

        val totalLoans = loans.size

        val outstanding = loans.sumOf { it.currPrincipal }

        val interest = loans.sumOf { it.currTotalIntPaid }

        val pending = loans.count {
            it.workflowStatus.equals(ApnaBankConstants.STATUS_PENDING, true)
        }

        binding.textTotalLoans.text = totalLoans.toString()

        binding.textTotalOutstanding.text = Converters.formatCurrency(outstanding)

        binding.textTotalInterest.text =  Converters.formatCurrency(interest)

        binding.textPendingLoans.text = pending.toString()
    }

    private fun getWorkflowColor(status: String): Int {
        val context = requireContext()

        return when (status.uppercase()) {
            ApnaBankConstants.STATUS_PENDING_APPROVAL -> ContextCompat.getColor(context, R.color.status_pending)
            ApnaBankConstants.STATUS_APPROVED -> ContextCompat.getColor(context, R.color.status_approved)
            ApnaBankConstants.STATUS_REJECTED -> ContextCompat.getColor(context, R.color.status_rejected)
            else -> MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, Color.DKGRAY)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}