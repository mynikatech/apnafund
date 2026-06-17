package com.mynikatech.apnafund.ui.loan

import android.app.AlertDialog
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.data.model.LoanDetailsWithMemberNames
import com.mynikatech.apnafund.databinding.DialogAddLoanBinding
import com.mynikatech.apnafund.net.dto.FundAvailabilityDto
import com.mynikatech.apnafund.util.ApnaBankDate
import com.mynikatech.apnafund.util.Converters.formatCurrency
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddLoanDialog(
    private val borrowerName: String? = null,
    private val rateOfInterest: Double,
    private val fundMaturityDate: String,
    private val fundStartDate: String,
    private val existingLoan: LoanDetailsWithMemberNames? = null,
    private val allowEditLoan: Boolean = true,
    private val borrowerList: List<Pair<Int, String>>? = null,
    private val borrowerUserId: Int? = null,
    private val totalAmounts: FundAvailabilityDto?,
    private val onSave: (loanAmount: Double, issueDate: String, period: Double, loanMaturityDate: String, selectedUserId: Int) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogAddLoanBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddLoanBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val amount = formatCurrency(totalAmounts?.totalAmount ?: 0.0)
        val totalAvailable = formatCurrency(totalAmounts?.availableAmount ?: 0.0)
        val totalApproved = formatCurrency(totalAmounts?.approvedAmount ?: 0.0)
        val totalPending = formatCurrency(totalAmounts?.pendingAmount ?: 0.0)
        binding.textTotal.text = getString(R.string.text_total, amount)
        binding.textAvailable.text = getString(R.string.text_available, totalAvailable)
        binding.textApproved.text = getString(R.string.text_approved_amt, totalApproved)
        binding.textPending.text = getString(R.string.text_pending, totalPending)
        val borrowerDropdown = binding.editTextBorrower
        var selectedUserId: Int? = null
        binding.setupForm()
        binding.editTextRate.setText("$rateOfInterest")
        if (borrowerList != null) {
            val namesOnly = borrowerList.map { it.second }
            val adapter = ArrayAdapter(requireContext(), R.layout.dropdown_item_apnabank, namesOnly)
            borrowerDropdown.setAdapter(adapter)
            borrowerDropdown.setOnClickListener { borrowerDropdown.showDropDown() }
            borrowerDropdown.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) borrowerDropdown.showDropDown() }
            borrowerDropdown.setOnItemClickListener { _, _, position, _ ->
                selectedUserId = borrowerList[position].first
            }

        } else {
            // For regular user: pre-fill and disable
            borrowerDropdown.setText(borrowerName)
            borrowerDropdown.isEnabled = false
            selectedUserId = existingLoan?.userId ?: borrowerUserId

        }
        if (existingLoan != null) {
            binding.buttonSaveLoan.text = getString(R.string.text_update_loan)
            binding.editTextLoanAmount.setText(existingLoan.loanAmount.toString())
            binding.editTextIssueDate.setText(existingLoan.issuedDate)
            binding.editTextPeriod.setText(existingLoan.period.toString())

            binding.apply {
                editTextLoanAmount.isEnabled = allowEditLoan
                editTextIssueDate.isEnabled = allowEditLoan
                editTextPeriod.isEnabled = allowEditLoan
                //gray them out
                val alpha = if (allowEditLoan) 1f else 0.5f
                editTextLoanAmount.alpha = alpha
                editTextIssueDate.alpha = alpha
                editTextPeriod.alpha = alpha
            }
        }

        binding.editTextIssueDate.setOnClickListener {
            showMaterialDatePicker { date ->
                binding.editTextIssueDate.setText(date)
            }
        }

        binding.buttonSaveLoan.setOnClickListener {
            binding.buttonSaveLoan.isEnabled = false
            binding.buttonSaveLoan.text = getString(R.string.button_saving_progress)
            try {
                val loanAmountStr = binding.editTextLoanAmount.text.toString()
                val issueDate = binding.editTextIssueDate.text.toString().trim()
                val periodStr = binding.editTextPeriod.text.toString().trim()
                val userId = selectedUserId
                if (userId == null) {
                    Snackbar.make(binding.root, "Please select a borrower", Snackbar.LENGTH_LONG)
                        .show()
                    binding.buttonSaveLoan.isEnabled = true
                    binding.buttonSaveLoan.text = getString(R.string.text_save)
                    return@setOnClickListener
                }
                if (issueDate.isBlank() || loanAmountStr.isBlank() || loanAmountStr.toDouble() <= 0) {
                    Snackbar.make(
                        binding.root,
                        "Issue Date and Loan Amount cannot be blank or 0",
                        Snackbar.LENGTH_LONG
                    ).show()
                    binding.buttonSaveLoan.isEnabled = true
                    binding.buttonSaveLoan.text = getString(R.string.text_save)
                    return@setOnClickListener
                }

                val isIssueDateBeforeFundStart =
                    ApnaBankDate.compareDates(issueDate, fundStartDate, "dd/mm/yyyy")
                if (isIssueDateBeforeFundStart <= 0) {
                    Snackbar.make(
                        binding.root,
                        "Issue Date cannot be before Fund Start Date",
                        Snackbar.LENGTH_LONG
                    ).show()
                    binding.buttonSaveLoan.isEnabled = true
                    binding.buttonSaveLoan.text = getString(R.string.text_save)
                    return@setOnClickListener
                }
                val loanAmount = binding.editTextLoanAmount.text.toString().trim().toDouble()
                val availableAmount = totalAmounts?.availableAmount ?: 0.0
                val effectiveAvailable = if (existingLoan != null) {
                    availableAmount + (existingLoan.loanAmount)
                } else {
                    availableAmount
                }
                if (loanAmount > effectiveAvailable) {
                    Snackbar.make(
                        binding.root,
                        "Amount cannot exceed available fund ${formatCurrency(effectiveAvailable)}",
                        Snackbar.LENGTH_LONG
                    ).show()
                    binding.buttonSaveLoan.isEnabled = true
                    binding.buttonSaveLoan.text = getString(R.string.text_save)
                    return@setOnClickListener
                }
                // If period not provided default it from issuedMonth to the Maturity Date Month
                val period: Double = if (periodStr.isEmpty()) {
                    ApnaBankDate.getMonthsBetween(issueDate, fundMaturityDate).toDouble()
                } else {
                    periodStr.toDouble()
                }
                // Loan Maturity Date based on the period
                // equal to issueDate + period
                val maturityDateStr = ApnaBankDate.calculateMatDate(issueDate, period.toDouble())
                if (!isLoanWithinFundPeriod(maturityDateStr)) {

                    binding.editTextPeriod.error =
                        getString(R.string.error_loan_exceeds_fund_maturity)
                    binding.buttonSaveLoan.isEnabled = true
                    binding.buttonSaveLoan.text = getString(R.string.text_save)
                    return@setOnClickListener
                }

                binding.editTextPeriod.error = null

                onSave(loanAmount, issueDate, period, maturityDateStr, userId)
                dismiss()
            } catch (e: Exception) {
                Log.e("AddLoanDialog", e.message.toString())
                binding.buttonSaveLoan.isEnabled = true
                binding.buttonSaveLoan.text = getString(R.string.text_save)

                Toast.makeText(
                    requireContext(),
                    "Something went wrong. Please try again.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        binding.buttonCancelLoan.setOnClickListener {
            dismiss()
        }
    }

    fun TextInputLayout.setInfoDialog(
        titleRes: Int,
        messageRes: Int
    ) {
        setEndIconOnClickListener {
            AlertDialog.Builder(context)
                .setTitle(context.getString(titleRes))
                .setMessage(context.getString(messageRes))
                .setPositiveButton(context.getString(R.string.text_button_ok), null)
                .show()
        }
    }

    private fun isLoanWithinFundPeriod(
        loanMaturityDate: String
    ): Boolean {

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        sdf.isLenient = false

        return try {

            val fundMaturity =
                sdf.parse(fundMaturityDate)

            val loanMaturity =
                sdf.parse(loanMaturityDate)

            loanMaturity != null &&
                    fundMaturity != null &&
                    !loanMaturity.after(fundMaturity)

        } catch (e: Exception) {
            false
        }
    }

    fun TextInputLayout.markRequired() {
        val label = this.hint?.toString() ?: ""
        val spannable = SpannableString("$label *")
        spannable.setSpan(
            ForegroundColorSpan(Color.RED),
            spannable.length - 1,
            spannable.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        this.hint = spannable
    }

    fun DialogAddLoanBinding.setupForm() {

        // Required fields
        listOf(
            textInputBorrower,
            textInputRate,
            textInputPeriod,
            textInputLoanAmount,
            textInputIssueDate
        ).forEach { it.markRequired() }

        // Info dialogs
        val infoFields = mapOf(
            textInputBorrower to Pair(
                R.string.title_loan_borrower_info,
                R.string.info_loan_borrower
            ),
            textInputRate to Pair(
                R.string.title_loan_rate_interest,
                R.string.info_loan_rate_interest
            ),
            textInputPeriod to Pair(
                R.string.title_loan_period,
                R.string.info_loan_period
            ),
            textInputLoanAmount to Pair(
                R.string.title_loan_amount,
                R.string.info_loan_amount
            ),
            textInputIssueDate to Pair(
                R.string.title_loan_issue_date_info,
                R.string.info_loan_issue_date
            )

        )

        infoFields.forEach { (view, data) ->
            view.setInfoDialog(data.first, data.second)
        }
    }

    private fun showMaterialDatePicker(onDateSelected: (String) -> Unit) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Issue Date")
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val formattedDate = sdf.format(Date(selection))
            onDateSelected(formattedDate)
        }

        picker.show(parentFragmentManager, "MATERIAL_DATE_PICKER")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}