package com.mynikatech.apnafund.ui.loan

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.annotation.RequiresApi
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.data.model.LoanDetailsWithMemberNames
import com.mynikatech.apnafund.databinding.DialogAddLoanBinding
import com.mynikatech.apnafund.util.ApnaBankDate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddLoanDialog(
    private val borrowerName: String? = null,
    private val rateOfInterest: Double,
    private val fundMaturityDate: String,
    private val existingLoan: LoanDetailsWithMemberNames? = null,
    private val allowEditLoan: Boolean = true,
    private var totalAmountAvailable: Double,
    private val borrowerList: List<Pair<Int, String>>? = null,
    private val borrowerUserId: Int? = null,
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
        binding.textViewFundAvailableValue.text = "Available Amount - $totalAmountAvailable"
        val borrowerDropdown = binding.editTextBorrower
        var selectedUserId: Int? = null

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
            val loanAmountStr = binding.editTextLoanAmount.text.toString()
            val issueDate = binding.editTextIssueDate.text.toString().trim()
            val periodStr = binding.editTextPeriod.text.toString().trim()
            val userId = selectedUserId
            if (userId == null) {
                Snackbar.make(binding.root, "Please select a borrower", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (issueDate.isBlank() || loanAmountStr.isBlank() || loanAmountStr.toDouble() <= 0) {
                Snackbar.make(
                    binding.root,
                    "Issue Date and Loan Amount cannot be blank or 0",
                    Snackbar.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            val loanAmount = binding.editTextLoanAmount.text.toString().trim().toDouble()
            if(existingLoan != null)
                totalAmountAvailable = totalAmountAvailable + existingLoan.loanAmount
            if (loanAmount > totalAmountAvailable) {
                Snackbar.make(
                    binding.root,
                    "Amount cannot exceed total available fund $totalAmountAvailable",
                    Snackbar.LENGTH_LONG
                ).show()
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

            onSave(loanAmount, issueDate, period, maturityDateStr, userId)
            dismiss()
        }
        binding.buttonCancelLoan.setOnClickListener {
            dismiss()
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