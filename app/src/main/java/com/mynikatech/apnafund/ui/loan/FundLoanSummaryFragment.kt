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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.databinding.FragmentFundLoanSummaryBinding
import com.mynikatech.apnafund.net.dto.LoanDetailsWithMemberNamesDto
import com.mynikatech.apnafund.ui.viewmodel.LoansViewModel
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
        binding.toolbar.title = "Fund Loans Summary"

        binding.textFundName.text = "${args.fundName} - Loans"

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
        row.setBackgroundResource(R.drawable.table_cell_shape)
        val headers = listOf(
            "Borrower",
            "Loan#",
            "Amount",
            "Issued",
            "Principal",
            "Interest",
            "Status",
            "Workflow"
        )

        headers.forEachIndexed { index, title ->

            val tv = TextView(requireContext())
            tv.text = title
            tv.setPadding(16, 10, 16, 10)
            tv.setTypeface(null, Typeface.BOLD)
            tv.setTextColor(Color.WHITE)
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

        if (index % 2 == 0)
            row.setBackgroundColor(Color.parseColor("#FAFAFA"))

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
            tv.text = value
            tv.setPadding(16, 8, 16, 8)

            // Prevent wrapping
            tv.isSingleLine = true
            tv.maxLines = 1
            tv.ellipsize = TextUtils.TruncateAt.END

            // Right align numbers
            if (i == 2 || i == 4 || i == 5)
                tv.gravity = Gravity.END

            if (i == 7)
                tv.setTextColor(getWorkflowColor(value))

            row.addView(tv)
        }

        binding.tableLoans.addView(row)
    }

    private fun updateSummary(loans: List<LoanDetailsWithMemberNamesDto>) {

        val totalLoans = loans.size

        val outstanding = loans.sumOf { it.currPrincipal }

        val interest = loans.sumOf { it.currTotalIntPaid }

        val pending = loans.count {
            it.workflowStatus.equals("PENDING", true)
        }

        binding.textTotalLoans.text = totalLoans.toString()

        binding.textTotalOutstanding.text = "₹$outstanding"

        binding.textTotalInterest.text = "₹$interest"

        binding.textPendingLoans.text = pending.toString()
    }

    private fun getWorkflowColor(status: String): Int {

        return when (status.uppercase()) {
            "PENDING" -> Color.parseColor("#FF9800")
            "APPROVED" -> Color.parseColor("#4CAF50")
            "REJECTED" -> Color.parseColor("#F44336")
            else -> Color.DKGRAY
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}