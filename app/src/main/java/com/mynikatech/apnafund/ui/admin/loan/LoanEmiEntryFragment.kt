package com.mynikatech.apnafund.ui.admin.loan

import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.TableRow
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.data.model.LoanEmiWithMemberNames
import com.mynikatech.apnafund.data.model.LoanEmis
import com.mynikatech.apnafund.databinding.FragmentLoanEmiEntryBinding
import com.mynikatech.apnafund.ui.admin.BaseEntryFragment
import com.mynikatech.apnafund.ui.viewmodel.FundSharedViewModel
import com.mynikatech.apnafund.ui.viewmodel.FundViewModel
import com.mynikatech.apnafund.ui.viewmodel.LoansViewModel
import com.mynikatech.apnafund.util.ApnaBankDate
import com.mynikatech.apnafund.util.LoanEmiInputValidator
import com.mynikatech.apnafund.util.ViewTags
import com.mynikatech.apnafund.util.showLoanDetailsDialog
import kotlinx.coroutines.launch
import java.time.LocalDate.now
import java.util.Calendar
import java.util.Locale

class LoanEmiEntryFragment : BaseEntryFragment() {

    private lateinit var binding: FragmentLoanEmiEntryBinding
    private val loanViewModel: LoansViewModel by viewModels()
    private val fundViewModel: FundViewModel by viewModels()
    private val fundSharedViewModel: FundSharedViewModel by activityViewModels()
    private var validMonthYearList = emptyList<Pair<String, String>>()
    private var hasChanges = false
    private var hasAnyLoanEmiEdits = false
    private var originalLoanEmis: List<LoanEmiWithMemberNames> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoanEmiEntryBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        populateMonthAndYearDropdowns()
        val currentMonth = now().month.name
            .lowercase().replaceFirstChar { it.uppercase() }

        val currentYear = now().year.toString()

        setupDropdownWithAdapter(binding.dropdownMonth, monthList, currentMonth)
        setupDropdownWithAdapter(binding.dropdownYear, yearList, currentYear)
        setupFundDropdown(
            fundDropdown = binding.dropdownFund,
            fundViewModel = fundViewModel
        ) { selectedFund ->
            loadFundDatesAndSetupMonthYear(selectedFund)
        }
        binding.dropdownMonth.setOnItemClickListener { parent, _, position, _ ->
            val selectedMonth = parent.getItemAtPosition(position) as String
            binding.dropdownMonth.setText(selectedMonth, false)
            observeData()
        }

        binding.dropdownYear.setOnItemClickListener { parent, _, position, _ ->
            val selectedYear = parent.getItemAtPosition(position) as String
            binding.dropdownYear.setText(selectedYear, false)
            observeData()
        }

        binding.buttonSaveLoanEmi.setOnClickListener {
            saveAllLoansEmi()
        }
        attachAutoScroll(binding.tableLoanEmiEntries, binding.verticalEmiScrollView)
    }

    fun attachAutoScroll(view: View, scrollView: ScrollView) {
        if (view is EditText) {
            view.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    scrollView.post {
                        scrollView.smoothScrollTo(0, v.bottom)
                    }
                }
            }
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                attachAutoScroll(view.getChildAt(i), scrollView)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun loadFundDatesAndSetupMonthYear(selectedFundName: String) {
        val fundId = fundMap[selectedFundName] ?: return
        lifecycleScope.launch {
            val fund = fundViewModel.fetchFund(fundId)
            if (null != fund)
                validMonthYearList =
                    getValidMonthYearList(fund.fundStartDate, fund.fundMaturityDate)

            val validMonths = validMonthYearList.map { it.first }.distinct()
            val validYears = validMonthYearList.map { it.second }.distinct()

            binding.dropdownMonth.setAdapter(
                ArrayAdapter(
                    requireContext(),
                    R.layout.dropdown_item_apnabank,
                    validMonths
                )
            )
            binding.dropdownYear.setAdapter(
                ArrayAdapter(
                    requireContext(),
                    R.layout.dropdown_item_apnabank,
                    validYears
                )
            )
            // Get current month and year
            val current = Calendar.getInstance()
            val currentMonth =
                current.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: ""
            val currentYear = current.get(Calendar.YEAR).toString()

            val selectedMonth =
                if (validMonths.contains(currentMonth)) currentMonth else validMonths.first()
            val selectedYear =
                if (validYears.contains(currentYear)) currentYear else validYears.first()

            binding.dropdownMonth.setText(selectedMonth, false)
            binding.dropdownYear.setText(selectedYear, false)
            observeData()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun observeData() {
        val selectedFundName = binding.dropdownFund.text.toString()
        if (selectedFundName.isBlank()) {
            Toast.makeText(
                requireContext(),
                getString(R.string.warning_select_fund),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val fundId = fundMap[selectedFundName] ?: 0
        if (fundId == 0) {
            Toast.makeText(
                requireContext(),
                getString(R.string.error_invalid_fund_selected),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val month = binding.dropdownMonth.text.toString()
        val year = binding.dropdownYear.text.toString()
        if (!validMonthYearList.contains(Pair(month, year))) {
            Toast.makeText(
                requireContext(),
                getString(R.string.error_invalid_month_year_selected_loan),
                Toast.LENGTH_SHORT
            ).show()
            // disable UI and Save button
            // also clear the data on the screen
            setUiEnabled(false)
            cleanTable()
            return
        } else {
            setUiEnabled(true)
        }
        refreshLoanEmiTable(fundId, monthNameToNumber[month] ?: return, year.toInt())
    }

    private fun setUiEnabled(enabled: Boolean) {
        binding.buttonSaveLoanEmi.isEnabled = enabled

        for (i in 1 until binding.tableLoanEmiEntries.childCount) { // skip header
            val row = binding.tableLoanEmiEntries.getChildAt(i) as TableRow
            for (j in 0 until row.childCount) {
                val view = row.getChildAt(j)
                view.isEnabled = enabled
                view.isFocusable = enabled
                view.isClickable = enabled
                view.isFocusableInTouchMode = enabled
            }
        }
        binding.dropdownMonth.isEnabled = true // still allow changing month
        binding.dropdownYear.isEnabled = true  // still allow changing year
        binding.dropdownFund.isEnabled = true  // still allow changing fund

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun refreshLoanEmiTable(fundId: Int, month: Int, year: Int) {
        lifecycleScope.launch {
            binding.tableLoanEmiEntries.removeAllViews()
            val header = createHeaderRow(TABLE_TYPE_LOAN)
            binding.tableLoanEmiEntries.addView(header)

            val loanEmiWithMemberNames =
                loanViewModel.getAllLoanEmisForFundForMonthYear(fundId, month, year)
            if (loanEmiWithMemberNames.isNotEmpty()) {
                val sortedLoanEmiWithMemberNames = loanEmiWithMemberNames.sortedWith(
                    compareBy<LoanEmiWithMemberNames> { it.emiDepositedAmount ?: 0.0 }
                        .thenBy { "${it.firstName} ${it.lastName}" }
                )
                originalLoanEmis = sortedLoanEmiWithMemberNames
                for (loanEmis in sortedLoanEmiWithMemberNames) {
                    addLoanEmiMemberRow(month, year, loanEmis)
                }
                updateSaveAllLoanEmiButtonState(sortedLoanEmiWithMemberNames)
            } else {
                // there is no loan at present so disable the save loan button
                setUiEnabled(false)
            }
        }
    }

    private fun updateSaveAllLoanEmiButtonState(loanEmisList: List<LoanEmiWithMemberNames>) {
        val hasEdits = loanEmisList.any { it.isEdited }
        binding.buttonSaveLoanEmi.isEnabled = hasEdits
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun refreshLoanEmiTableWithData(updatedList: List<LoanEmiWithMemberNames>) {
        binding.tableLoanEmiEntries.removeAllViews()  // Clear all existing rows
        // Add the table header
        val header = createHeaderRow(TABLE_TYPE_LOAN)
        binding.tableLoanEmiEntries.addView(header)
        if (updatedList.isNotEmpty()) {
            val sortedUpdatedList = updatedList.sortedWith(
                compareBy<LoanEmiWithMemberNames> { it.emiDepositedAmount ?: 0.0 }
                    .thenBy { "${it.firstName} ${it.lastName}" }
            )
            for (loanEmis in sortedUpdatedList) {
                addLoanEmiMemberRow(
                    month = monthNameToNumber[binding.dropdownMonth.text.toString()]
                        ?: currentMonth,
                    year = binding.dropdownYear.text.toString().toIntOrNull() ?: currentYear,
                    loanEmis = loanEmis
                )
            }
        }
    }

    private fun cleanTable() {
        binding.tableLoanEmiEntries.removeAllViews()
        val header = createHeaderRow(TABLE_TYPE_DEPOSIT)
        binding.tableLoanEmiEntries.addView(header)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun addLoanEmiMemberRow(
        month: Int,
        year: Int,
        loanEmis: LoanEmiWithMemberNames
    ) {
        val loamEmi = loanEmis.emiInterest
        val lastDepDate = fundLastDepDate[binding.dropdownFund.text.toString()] ?: 0
        val lateFeeRate = fundLateFeeRate[binding.dropdownFund.text.toString()] ?: 0.0
        val row = TableRow(requireContext())
        val loanId = loanEmis.loanId
        row.setTag(ViewTags.LOAN_ID, loanId)
        val fullName = "${loanEmis.firstName} ${loanEmis.lastName}"
        val loanNumber = "(${loanEmis.loanNumber})"
        val combinedText = "$fullName\n$loanNumber"
        val ctx = requireContext()
        val spannable = SpannableString(combinedText).apply {
            val loanStart = fullName.length + 1
            val loanEnd = length

            // Size and style for loan number
            setSpan(RelativeSizeSpan(0.8f), loanStart, loanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(
                StyleSpan(Typeface.ITALIC),
                loanStart,
                loanEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Make the loan number clickable
            setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    // Navigate to Loan Details to do
                    widget.context.showLoanDetailsDialog(loanEmis)
                }

                override fun updateDrawState(ds: TextPaint) {
                    ds.isUnderlineText = false
                    ds.color = MaterialColors.getColor(
                        ctx,
                        com.google.android.material.R.attr.colorOnSurface,
                        Color.BLUE // fallback
                    )
                }
            }, loanStart, loanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        val tvBorrowerName = createTextView("").apply {
            text = spannable
            isSingleLine = false
            maxLines = 2
            movementMethod = LinkMovementMethod.getInstance()
            highlightColor = Color.TRANSPARENT
        }
        tvBorrowerName.layoutParams = TableRow.LayoutParams(
            resources.getDimensionPixelSize(R.dimen.loan_number_col_width),
            TableRow.LayoutParams.WRAP_CONTENT
        )
        val etEmiAmount = EditText(requireContext()).apply {

            inputType = InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(InputFilter { source, start, end, dest, dstart, dend ->
                val newText = dest.toString().substring(0, dstart) +
                        source.subSequence(start, end) +
                        dest.toString().substring(dend)
                val input = newText.toIntOrNull()
                if (input != null) {
                    when {
                        input > loamEmi.toInt() -> {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.error_amount_exceeds_limit),
                                Toast.LENGTH_SHORT
                            ).show()
                            ""
                        }

                        input < 0 -> {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.error_amount_negative),
                                Toast.LENGTH_SHORT
                            ).show()
                            ""
                        }

                        else -> null
                    }
                } else null
            })
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    hasChanges = true
                    binding.buttonSaveLoanEmi.isEnabled = true
                }

                override fun afterTextChanged(s: Editable?) {}
            })
            if (loanEmis.emiDepositedAmount == null || loanEmis.emiDepositedAmount == 0.0) {
                setText(loanEmis.emiInterest.toString())
            }
        }
        val lateFeeLoanEmi = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            isEnabled = false
        }
        val etPrepayAmount = EditText(requireContext()).apply {

            inputType = InputType.TYPE_CLASS_NUMBER
            // Basic character-level filtering (optional, to block non-digit input)
            filters = arrayOf(InputFilter { source, _, _, _, _, _ ->
                if (source.matches(Regex("[0-9]*"))) null else ""
            })
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                }

                override fun afterTextChanged(s: Editable?) {
                    val inputStr = s?.toString() ?: ""
                    val input = inputStr.toDoubleOrNull()

                    if (input != null && input < 0) {
                        showToast(getString(R.string.error_amount_negative))
                    }
                    hasChanges = true
                    binding.buttonSaveLoanEmi.isEnabled = true
                }
            })
            if (loanEmis.prepaymentAmount > 0.0) {
                setText(loanEmis.prepaymentAmount.toString())
            }
        }

        val etLoanEmiDepositDate = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_DATETIME
            setOnClickListener { showDatePickerDialog(this) }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    hasChanges = true
                    //Calculate Late fee if any and populate the late fee column
                    // form the lastDepDate value and format it as a date
                    val formatedLastDepDate =
                        ApnaBankDate.createDateString(lastDepDate, month + 1, year)
                    val lateFine =
                        calculateLateFee(text.toString(), formatedLastDepDate, lateFeeRate)
                    if (lateFine > 0) {
                        lateFeeLoanEmi.setText(lateFine.toString())

                    }

                }

                override fun afterTextChanged(s: Editable?) {
                }
            })
        }

        val btnLoanEmiEdit = ImageButton(requireContext()).apply {
            setImageResource(R.drawable.ic_edit)
        }

        if (loanEmis.emiDepositedAmount != null) {
            etEmiAmount.setText(loanEmis.emiDepositedAmount.toString())
            etLoanEmiDepositDate.setText(loanEmis.emiDepositedDate.toString())
            lateFeeLoanEmi.setText(loanEmis.lateFee.toString())
            etPrepayAmount.setText(loanEmis.prepaymentAmount.toString())
            etEmiAmount.isEnabled = false
            etLoanEmiDepositDate.isEnabled = false
            lateFeeLoanEmi.isEnabled = false
            etPrepayAmount.isEnabled = false
            loanEmis.isEdited = false
            setButtonState(btnLoanEmiEdit, enabled = true)
            row.setBackgroundColor(Color.LTGRAY)

            btnLoanEmiEdit.setOnClickListener {
                etEmiAmount.isEnabled = true
                etLoanEmiDepositDate.isEnabled = true
                lateFeeLoanEmi.isEnabled = true
                etPrepayAmount.isEnabled = true
                hasAnyLoanEmiEdits = true
                loanEmis.isEdited = true
                binding.buttonSaveLoanEmi.isEnabled = true
                setButtonState(btnLoanEmiEdit, enabled = false)
                row.setBackgroundColor(Color.TRANSPARENT)
            }
        } else {
            etEmiAmount.isEnabled = true
            etLoanEmiDepositDate.isEnabled = true
            lateFeeLoanEmi.isEnabled = true
            etPrepayAmount.isEnabled = true
            loanEmis.isEdited = true
            setButtonState(btnLoanEmiEdit, enabled = false)
            etLoanEmiDepositDate.setText(ApnaBankDate.getCurrentDate())
            row.setBackgroundColor(Color.TRANSPARENT)
        }
        row.addView(tvBorrowerName)
        row.addView(etEmiAmount)
        row.addView(lateFeeLoanEmi)
        row.addView(etPrepayAmount)
        row.addView(etLoanEmiDepositDate)
        row.addView(btnLoanEmiEdit)

        binding.tableLoanEmiEntries.addView(row)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveAllLoansEmi() {
        lifecycleScope.launch {
            binding.buttonSaveLoanEmi.isEnabled = false
            val selectedFundName = binding.dropdownFund.text.toString()
            val fundId = fundMap[selectedFundName] ?: 0

            if (fundId == 0) {
                originalLoanEmis
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_invalid_fund_selected),
                    Toast.LENGTH_SHORT
                ).show()
                binding.buttonSaveLoanEmi.isEnabled = true
                return@launch
            }
            if (!hasChanges) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.message_no_changes_to_save),
                    Toast.LENGTH_SHORT
                ).show()
                binding.buttonSaveLoanEmi.isEnabled = true
                return@launch
            } else {
                val selectedMonthName = binding.dropdownMonth.text.toString()
                val selectedMonth = monthNameToNumber[selectedMonthName] ?: now().monthValue
                val selectedYear = binding.dropdownYear.text.toString().toIntOrNull() ?: currentYear
                val updatedLoanEmiList: List<LoanEmiWithMemberNames>
                val loanEmis = buildLoansEmiList(selectedMonth, selectedYear)
                val originalLoanEmisMap = originalLoanEmis.associateBy { it.loanId }
                if (loanEmis.isNotEmpty()) {
                    if (hasAnyLoanEmiEdits) {
                        val editedLoanEmis = loanEmis.filter { row ->
                            val original = originalLoanEmisMap[row.loanId]
                            hasLoanEmiChanged(original, row)
                        }

                        // loan emi validations
                        editedLoanEmis.forEach { edited ->
                            val original = originalLoanEmisMap[edited.loanId] ?: return@forEach
                            val errorMessage = LoanEmiInputValidator.validateLoanEmi(
                                original,
                                edited,
                                selectedMonth,
                                selectedYear
                            )
                            if (errorMessage != null) {
                                showToast(errorMessage)
                                binding.buttonSaveLoanEmi.isEnabled = true
                                return@launch
                            }
                        }

                        updatedLoanEmiList = loanViewModel.saveOrUpdateAllLoansEmiAndFetch(
                            editedLoanEmis,
                            fundId,
                            selectedMonth.toString(),
                            selectedYear.toString()
                        )
                        originalLoanEmis = updatedLoanEmiList
                        refreshLoanEmiTableWithData(updatedLoanEmiList)
                        Snackbar.make(
                            requireView(),
                            getString(
                                R.string.message_saved_updated_loan_emis,
                                editedLoanEmis.size
                            ),
                            Snackbar.LENGTH_SHORT
                        )
                            .show()

                        fundSharedViewModel.refreshFunds()

                        hasAnyLoanEmiEdits = false

                    } else {
                        loanEmis.forEach { added ->
                            val original = originalLoanEmisMap[added.loanId] ?: return@forEach
                            val errorMessage = LoanEmiInputValidator.validateLoanEmi(
                                original,
                                added,
                                selectedMonth,
                                selectedYear
                            )
                            if (errorMessage != null) {
                                showToast(errorMessage)
                                binding.buttonSaveLoanEmi.isEnabled = true
                                return@launch
                            }
                        }
                        val updatedLoanEmisList = loanViewModel.saveOrUpdateAllLoansEmiAndFetch(
                            loanEmis,
                            fundId,
                            selectedMonth.toString(),
                            selectedYear.toString()
                        )
                        originalLoanEmis = updatedLoanEmisList
                        refreshLoanEmiTableWithData(updatedLoanEmisList)
                        Snackbar.make(
                            requireView(),
                            getString(R.string.message_loan_emis_saved_success),
                            Snackbar.LENGTH_SHORT
                        )
                            .show()
                        fundSharedViewModel.refreshFunds()
                    }

                } else {
                    Snackbar.make(
                        requireView(),
                        getString(R.string.message_no_changes_to_save),
                        Snackbar.LENGTH_SHORT
                    )
                        .show()

                }
                binding.buttonSaveLoanEmi.isEnabled = true
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildLoansEmiList(
        selectedMonth: Int,
        selectedYear: Int
    ): List<LoanEmis> {
        val loanEmis = mutableListOf<LoanEmis>()
        for (i in 1 until binding.tableLoanEmiEntries.childCount) {
            val row = binding.tableLoanEmiEntries.getChildAt(i) as TableRow
            val etLoanEmiAmount = row.getChildAt(1) as EditText
            val etLoanEmiDepositedDate = row.getChildAt(4) as EditText
            val etLoanPrepayAmount = row.getChildAt(3) as EditText
            val lateFee = row.getChildAt(2) as EditText
            val loanId = row.getTag(ViewTags.LOAN_ID) as? Int
            val amountText = etLoanEmiAmount.text.toString()
            val prepayAmount = etLoanPrepayAmount.text.toString()
            val dateText = etLoanEmiDepositedDate.text.toString()
            val lateFeeAmount = lateFee.text.toString()
            if (amountText.isBlank()) continue
            loanEmis.add(
                LoanEmis(
                    loanId = loanId ?: 0,
                    emiMonth = selectedMonth.toString(),
                    emiYear = selectedYear.toString(),
                    emiDepositedDate = dateText,
                    emiDepositedAmount = amountText.toDouble(),
                    prepaymentAmount = if (prepayAmount.isNotBlank()) prepayAmount.toDouble() else 0.0,
                    lateFee = if (lateFeeAmount.isNotBlank()) lateFeeAmount.toDouble() else 0.0
                )
            )
        }
        return loanEmis
    }


    private fun hasLoanEmiChanged(original: LoanEmiWithMemberNames?, current: LoanEmis): Boolean {
        if (original == null) return true // New entry

        return original.emiDepositedAmount?.toString() != current.emiDepositedAmount.toString() ||
                original.emiDepositedDate.toString() != current.emiDepositedDate ||
                original.lateFee.toString() != current.lateFee.toString() ||
                original.prepaymentAmount != current.prepaymentAmount
    }


}
