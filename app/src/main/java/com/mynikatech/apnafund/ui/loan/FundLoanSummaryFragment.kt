package com.mynikatech.apnafund.ui.loan

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.color.MaterialColors
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.constants.ApnaBankConstants
import com.mynikatech.apnafund.data.mappers.toEntity
import com.mynikatech.apnafund.data.model.LoanDetailsWithMemberNames
import com.mynikatech.apnafund.databinding.FragmentFundLoanSummaryBinding
import com.mynikatech.apnafund.net.dto.LoanDetailsWithMemberNamesDto
import com.mynikatech.apnafund.session.SessionManager
import com.mynikatech.apnafund.ui.viewmodel.LoansViewModel
import com.mynikatech.apnafund.util.Converters
import com.mynikatech.apnafund.util.applyStatusStyle
import com.mynikatech.apnafund.util.showLoanDetailsSummaryDialog
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
            val sortedLoans = loans.sortedBy { getLoanPriority(it) }

            sortedLoans.forEachIndexed { index, loan ->
                addLoanRow(loan, index)
            }
        }
    }

    private fun clearTable() {
        binding.tableLoans.removeAllViews()
    }

    private fun getLoanPriority(loan: LoanDetailsWithMemberNamesDto): Int {
        return when {
            loan.status.equals(ApnaBankConstants.STATUS_ACTIVE, true) &&
                    loan.workflowStatus.equals(ApnaBankConstants.STATUS_APPROVED, true) -> 1

            loan.status.equals(ApnaBankConstants.STATUS_ACTIVE, true) &&
                    loan.workflowStatus.equals(ApnaBankConstants.STATUS_PENDING, true) -> 2

            loan.status.equals(ApnaBankConstants.STATUS_CLOSED, true) &&
                    loan.workflowStatus.equals(ApnaBankConstants.STATUS_APPROVED, true) -> 3

            loan.workflowStatus.equals(ApnaBankConstants.STATUS_REJECTED, true) -> 4

            else -> 5
        }
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
            getString(R.string.text_status),
            getString(R.string.text_loan_num),
            getString(R.string.text_amount),
            getString(R.string.text_issued),
            getString(R.string.text_principal),
            getString(R.string.text_interest),
            getString(R.string.text_workflow),
            getString(R.string.label_action)
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
            if (index == 3 || index == 4 || index == 5)
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
            loan.status,
            loan.loanNumber,
            "₹${loan.loanAmount}",
            loan.issuedDate,
            "₹${loan.currPrincipal}",
            "₹${loan.currTotalIntPaid}",
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
            if (i == 2) {
                tv.maxWidth = resources.getDimensionPixelSize(R.dimen.loan_number_col_width)

                // Show full value on click
                tv.setOnClickListener {
                    context?.showLoanDetailsSummaryDialog(loan)
                }
            }

            // Right align numbers
            if (i == 3 || i == 4 || i == 5)
                tv.gravity = Gravity.END

            if (i == 1) {
                tv.setTextColor(getStatusColor(value))
                tv.setTypeface(null, Typeface.BOLD)
            }

            if (i == 7) {
                tv.setTextColor(getWorkflowColor(value))
            } else {
                tv.setTextColor(textColor)
            }


            row.addView(tv)
        }

        // ===============================
        // ACTION COLUMN
        // ===============================

        val actionLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 4, 8, 4)
        }

        val closeButton = Button(requireContext()).apply {
            isAllCaps = false
            textSize = 12f
            text = if (SessionManager.canManageFund(loan.fundId)) getString(R.string.text_close_loan) else getString(R.string.label_close_loan_request)
        }

        // Disable if already closed
        if (loan.status.equals(ApnaBankConstants.STATUS_CLOSED, true) || loan.status.equals(ApnaBankConstants.STATUS_REJECTED, true) ) {
            closeButton.isEnabled = false
        }

        closeButton.setOnClickListener {

            if (loan.status.equals(ApnaBankConstants.STATUS_CLOSED, true)) {
                Toast.makeText(context,
                    getString(R.string.error_loan_already_closed), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (SessionManager.canManageFund(loan.fundId)) {
                showDirectClosureConfirmation(loan.toEntity())
            } else {
                checkPendingAndProceed(loan.toEntity())
            }

        }

        actionLayout.addView(closeButton)

        row.addView(actionLayout)

        binding.tableLoans.addView(row)
    }

    private fun getStatusColor(status: String): Int {

        val context = requireContext()

        return when (status.uppercase()) {

            ApnaBankConstants.STATUS_ACTIVE ->
                ContextCompat.getColor(
                    context,
                    R.color.status_approved_text
                )

            ApnaBankConstants.STATUS_CLOSED ->
                ContextCompat.getColor(
                    context,
                    R.color.status_closed_text
                )

            ApnaBankConstants.STATUS_REJECTED ->
                ContextCompat.getColor(
                    context,
                    R.color.status_rejected_text
                )

            else ->
                MaterialColors.getColor(
                    context,
                    com.google.android.material.R.attr.colorOnSurface,
                    Color.DKGRAY
                )
        }
    }

    private fun showDirectClosureConfirmation(loan: LoanDetailsWithMemberNames) {

        val message = getString(R.string.message_close_loan_direct, loan.loanNumber,  Converters.formatCurrency(loan.currPrincipal))

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.title_close_loan_moderator))
            .setMessage(message)
            .setPositiveButton(getString(R.string.text_close_loan)) { _, _ ->
                loansViewModel.closeLoanDirect(loan.loanId ?: return@setPositiveButton, SessionManager.userId)
            }
            .setNegativeButton(getString(R.string.text_cancel_button), null)
            .show()
    }


    private fun checkPendingAndProceed(loan: LoanDetailsWithMemberNames) {

        lifecycleScope.launch {

            try {
                val loanId = loan.loanId
                if (loanId == null) {
                    Toast.makeText(requireContext(),
                        getString(R.string.error_invalid_loan), Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val isPending = loansViewModel.hasPendingClosureRequest(loanId)
                if (isPending) {

                    AlertDialog.Builder(requireContext())
                        .setTitle(getString(R.string.title_request_pending))
                        .setMessage(
                            getString(R.string.message_loan_closure_request_pending)
                        )
                        .setPositiveButton(getString(R.string.text_button_ok), null)
                        .show()

                } else {
                    showCloseLoanConfirmation(loan)
                }

            } catch (e: Exception) {
                Log.e("checkPendingAndProceed", e.message.toString())

                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_server),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showCloseLoanConfirmation(loan: LoanDetailsWithMemberNames) {

        val tentativeClosureAmount =
            (loan.currPrincipal) + (loan.emiInterest)

        val message = getString(
            R.string.message_close_loan_request,
            loan.loanNumber,
            Converters.formatCurrency(loan.loanAmount),
            Converters.formatCurrency(loan.currPrincipal),
            Converters.formatCurrency(loan.emiInterest),
            Converters.formatCurrency(tentativeClosureAmount)
        )

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.text_close_loan))
            .setMessage(message)
            .setPositiveButton(getString(R.string.text_button_ok)) { _, _ ->
                raiseLoanClosureRequest(loan)
            }
            .setNegativeButton(getString(R.string.text_cancel_button), null)
            .show()
    }

    private fun raiseLoanClosureRequest(loan: LoanDetailsWithMemberNames) {
        loan.loanId?.let { loanId ->
            loansViewModel.requestLoanClosure(
                loanId = loanId,
                requestedAmount = loan.loanAmount, // optional
                remarks = "User requested closure"
            )
        }
    }

    private fun updateSummary(loans: List<LoanDetailsWithMemberNamesDto>) {

        val activeLoans = loans.filter {
            it.status.equals(ApnaBankConstants.STATUS_ACTIVE, true) &&
                    it.workflowStatus.equals(ApnaBankConstants.STATUS_APPROVED, true)
        }

        // APPROVED (ACTIVE + CLOSED) → for interest
        val approvedLoans = loans.filter {
            it.workflowStatus.equals(ApnaBankConstants.STATUS_APPROVED, true)
        }

        val totalLoans = loans.size

        val outstanding = activeLoans.sumOf { it.currPrincipal }

        val interest = approvedLoans.sumOf { it.currTotalIntPaid }

        val expectedInterest = approvedLoans.sumOf { it.totalInterest }

//        val pending = loans.count {
//            it.workflowStatus.equals(ApnaBankConstants.STATUS_PENDING, true)
//        }

        binding.textTotalLoans.text = totalLoans.toString()

        binding.textTotalOutstanding.text = Converters.formatCurrency(outstanding)

        binding.textTotalInterest.text = Converters.formatCurrency(interest)

        binding.textTotalExpInterest.text = Converters.formatCurrency(expectedInterest)
    }

    private fun getWorkflowColor(status: String): Int {
        val context = requireContext()

        return when (status.uppercase()) {
            ApnaBankConstants.STATUS_PENDING_APPROVAL -> ContextCompat.getColor(
                context,
                R.color.status_pending
            )

            ApnaBankConstants.STATUS_APPROVED -> ContextCompat.getColor(
                context,
                R.color.status_approved
            )

            ApnaBankConstants.STATUS_REJECTED -> ContextCompat.getColor(
                context,
                R.color.status_rejected
            )

            else -> MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorOnSurface,
                Color.DKGRAY
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}