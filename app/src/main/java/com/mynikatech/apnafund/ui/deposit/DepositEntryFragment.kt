package com.mynikatech.apnafund.ui.deposit

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.data.model.Deposits
import com.mynikatech.apnafund.data.model.DepositsWithMemberNames
import com.mynikatech.apnafund.databinding.FragmentDepositEntryBinding
import com.mynikatech.apnafund.ui.admin.BaseEntryFragment
import com.mynikatech.apnafund.ui.viewmodel.DepositViewModel
import com.mynikatech.apnafund.ui.viewmodel.FundSharedViewModel
import com.mynikatech.apnafund.ui.viewmodel.FundViewModel
import com.mynikatech.apnafund.util.ApnaBankDate
import com.mynikatech.apnafund.util.Converters
import kotlinx.coroutines.launch
import java.time.LocalDate.now
import java.util.Calendar
import java.util.Locale


class DepositEntryFragment : BaseEntryFragment() {

    private lateinit var binding: FragmentDepositEntryBinding
    private val viewModel: DepositViewModel by viewModels()
    private val fundViewModel: FundViewModel by viewModels()
    private val fundSharedViewModel: FundSharedViewModel by activityViewModels()
    private var validMonthYearList = emptyList<Pair<String, String>>()
    private var hasChanges = false
    private var hasAnyDepositEdits = false
    private lateinit var originalDeposits: List<DepositsWithMemberNames>


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDepositEntryBinding.inflate(inflater, container, false)
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

        binding.buttonSaveAll.setOnClickListener {
            saveAllDeposits()
        }
        attachAutoScroll(binding.tableDepositEntries, binding.verticalDepScrollView)
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
                getString(R.string.warning_select_fund), Toast.LENGTH_SHORT
            ).show()
            return
        }

        val fundId = fundMap[selectedFundName] ?: 0
        if (fundId == 0) {
            Toast.makeText(
                requireContext(),
                getString(R.string.error_invalid_fund_selected), Toast.LENGTH_SHORT
            ).show()
            return
        }

        val month = binding.dropdownMonth.text.toString()
        val year = binding.dropdownYear.text.toString()

        if (!validMonthYearList.contains(Pair(month, year))) {
            Toast.makeText(
                requireContext(),
                getString(R.string.error_invalid_month_year_for_selected_fund),
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

        refreshDepositsTable(fundId, monthNameToNumber[month] ?: return, year.toInt())
    }

    private fun setUiEnabled(enabled: Boolean) {
        binding.buttonSaveAll.isEnabled = enabled

        for (i in 1 until binding.tableDepositEntries.childCount) {
            val row = binding.tableDepositEntries.getChildAt(i) as TableRow
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
    private fun refreshDepositsTable(fundId: Int, month: Int, year: Int) {
        lifecycleScope.launch {
            binding.tableDepositEntries.removeAllViews()
            val header = createHeaderRow(TABLE_TYPE_DEPOSIT)
            binding.tableDepositEntries.addView(header)

            val depWithMemberNames =
                viewModel.getAllDepositsForFundForMonthYear(fundId, month, year)
            val sortedDepWithMemberNames = depWithMemberNames.sortedWith(
                compareBy<DepositsWithMemberNames> { it.depositAmount ?: 0.0 }
                    .thenBy { "${it.firstName} ${it.lastName}" }
            )
            originalDeposits = sortedDepWithMemberNames
            for (deposits in sortedDepWithMemberNames) {
                addDepositsMemberRow(month, year, deposits)
            }
            // check if any deposit has isEdit = true then enable to saveAll button else disable
            updateSaveAllButtonState(sortedDepWithMemberNames)
        }
    }

    private fun updateSaveAllButtonState(depositList: List<DepositsWithMemberNames>) {
        val hasEdits = depositList.any { it.isEdited }
        binding.buttonSaveAll.isEnabled = hasEdits
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun refreshDepositsTableWithData(updatedList: List<DepositsWithMemberNames>) {
        binding.tableDepositEntries.removeAllViews()  // Clear all existing rows
        val header = createHeaderRow(TABLE_TYPE_DEPOSIT)
        binding.tableDepositEntries.addView(header)
        if (updatedList.isNotEmpty()) {
            val sortedUpdatedList = updatedList.sortedWith(
                compareBy<DepositsWithMemberNames> { it.depositAmount ?: 0.0 }
                    .thenBy { "${it.firstName} ${it.lastName}" }
            )
            for (deposit in sortedUpdatedList) {
                addDepositsMemberRow(
                    month = monthNameToNumber[binding.dropdownMonth.text.toString()]
                        ?: currentMonth,
                    year = binding.dropdownYear.text.toString().toIntOrNull() ?: currentYear,
                    deposit = deposit
                )
            }
        }
    }

    private fun cleanTable() {
        binding.tableDepositEntries.removeAllViews()
        val header = createHeaderRow(TABLE_TYPE_DEPOSIT)
        binding.tableDepositEntries.addView(header)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun addDepositsMemberRow(
        month: Int,
        year: Int,
        deposit: DepositsWithMemberNames?
    ) {
        val recurringDepAmt = fundAmountMap[binding.dropdownFund.text.toString()] ?: 0.0
        val lastDepDate = fundLastDepDate[binding.dropdownFund.text.toString()] ?: 0
        val lateFeeRate = fundLateFeeRate[binding.dropdownFund.text.toString()] ?: 0.0
        val row = TableRow(requireContext())

        val tvName = createTextView("${deposit?.firstName} ${deposit?.lastName}")

        val etAmount = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(InputFilter { source, start, end, dest, dstart, dend ->
                val newText = dest.toString().substring(0, dstart) +
                        source.subSequence(start, end) +
                        dest.toString().substring(dend)
                val input = newText.toIntOrNull()
                if (input != null) {
                    when {
                        input > recurringDepAmt.toInt() -> {
                            Toast.makeText(
                                requireContext(),
                                getString(
                                    R.string.error_amount_exceeds_limit,
                                    Converters.formatCurrency(recurringDepAmt)
                                ),
                                Toast.LENGTH_SHORT
                            ).show()
                            ""
                        }

                        input < 0 -> {
                            Toast.makeText(
                                requireContext(),
                                context.getString(R.string.error_amount_negative),
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
                    binding.buttonSaveAll.isEnabled = true
                }

                override fun afterTextChanged(s: Editable?) {}
            })
        }
        val lateFee = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            isEnabled = false
        }

        val etDate = EditText(requireContext()).apply {
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
                        ApnaBankDate.createDateString(lastDepDate, month, year)
                    val lateFine =
                        calculateLateFee(text.toString(), formatedLastDepDate, lateFeeRate)
                    if (lateFine > 0) {
                        lateFee.setText(lateFine.toString())

                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })
        }


        val btnEdit = ImageButton(requireContext()).apply {
            setImageResource(R.drawable.ic_edit)
        }

        if (deposit?.depositAmount != null) {
            etAmount.setText(deposit.depositAmount.toString())
            etDate.setText(deposit.depositedDate)
            lateFee.setText(deposit.lateFee.toString())
            etAmount.isEnabled = false
            etDate.isEnabled = false
            lateFee.isEnabled = false
            deposit.isEdited = false
            // make the edit button enabled only if the deposit amount
            // is not fully paid for that month, however not sure if it
            // was done by mistake then how to correct it?
            // will implement later
            setButtonState(btnEdit, enabled = true)

            row.setBackgroundColor(Color.LTGRAY)

            btnEdit.setOnClickListener {
                etAmount.isEnabled = true
                etDate.isEnabled = true
                lateFee.isEnabled = true
                hasAnyDepositEdits = true
                deposit.isEdited = true
                binding.buttonSaveAll.isEnabled = true
                setButtonState(btnEdit, enabled = false)
                row.setBackgroundColor(Color.TRANSPARENT)
            }
        } else {
            etAmount.isEnabled = true
            etDate.isEnabled = true
            lateFee.isEnabled = true
            deposit?.isEdited = true
            setButtonState(btnEdit, enabled = false)
            etDate.setText(ApnaBankDate.getCurrentDate())
            row.setBackgroundColor(Color.TRANSPARENT)
        }

        row.addView(tvName)
        row.addView(etAmount)
        row.addView(lateFee)
        row.addView(etDate)
        row.addView(btnEdit)

        binding.tableDepositEntries.addView(row)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveAllDeposits() {
        lifecycleScope.launch {
            binding.buttonSaveAll.isEnabled = false  // Prevent multiple clicks
            val selectedFundName = binding.dropdownFund.text.toString()
            val fundId = fundMap[selectedFundName] ?: 0

            if (fundId == 0) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_invalid_fund_selected),
                    Toast.LENGTH_SHORT
                ).show()
                binding.buttonSaveAll.isEnabled = true
                return@launch
            }
            if (!hasChanges) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.message_no_changes_to_save), Toast.LENGTH_SHORT
                ).show()
                binding.buttonSaveAll.isEnabled = true
                return@launch
            } else {
                val selectedMonthName = binding.dropdownMonth.text.toString()
                val selectedMonth = monthNameToNumber[selectedMonthName] ?: now().monthValue
                val selectedYear = binding.dropdownYear.text.toString().toIntOrNull() ?: currentYear
                val updatedDepositList: List<DepositsWithMemberNames>
                val deposits = buildDepositsList(fundId, selectedMonth, selectedYear)
                if (deposits.isNotEmpty()) {
                    if (hasAnyDepositEdits) {
                        val editedDeposits = deposits.filter { row ->
                            val originalDepositsMap =
                                originalDeposits.associateBy { it.depositorId }
                            val original = originalDepositsMap[row.depositorId]
                            hasDepositsChanged(original, row)
                        }
                        updatedDepositList = viewModel.saveOrUpdateAllAndFetch(
                            editedDeposits,
                            fundId,
                            selectedMonth.toString(),
                            selectedYear.toString()
                        )
                        originalDeposits = updatedDepositList
                        refreshDepositsTableWithData(updatedDepositList)
                        Snackbar.make(
                            requireView(),
                            getString(
                                R.string.message_saved_updated_entries,
                                editedDeposits.size
                            ),
                            Snackbar.LENGTH_SHORT
                        )
                            .show()
                        fundSharedViewModel.refreshFunds()
                        hasAnyDepositEdits = false

                    } else {

                        updatedDepositList = viewModel.saveOrUpdateAllAndFetch(
                            deposits,
                            fundId,
                            selectedMonth.toString(),
                            selectedYear.toString()
                        )
                        originalDeposits = updatedDepositList
                        refreshDepositsTableWithData(updatedDepositList)
                        Snackbar.make(
                            requireView(),
                            getString(R.string.message_deposits_saved_success),
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
                binding.buttonSaveAll.isEnabled = true
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun buildDepositsList(
        fundId: Int,
        selectedMonth: Int,
        selectedYear: Int
    ): List<Deposits> {
        val deposits = mutableListOf<Deposits>()
        for (i in 1 until binding.tableDepositEntries.childCount) {
            val row = binding.tableDepositEntries.getChildAt(i) as TableRow
            val tvName = row.getChildAt(0) as TextView
            val etAmount = row.getChildAt(1) as EditText
            val etDate = row.getChildAt(3) as EditText
            val lateFee = row.getChildAt(2) as EditText

            val name = tvName.text.toString()
            val amountText = etAmount.text.toString()
            val dateText = etDate.text.toString()
            val lateFeeAmount = lateFee.text.toString()

            if (amountText.isBlank()) continue

            val member = fundViewModel.getMemberByNameForFund(name, fundId) ?: continue

            deposits.add(
                Deposits(
                    depositorId = member.userId,
                    fundId = fundId,
                    depositAmount = amountText.trim().toDouble(),
                    depositMonth = selectedMonth.toString(),
                    depositYear = selectedYear.toString(),
                    depositedDate = dateText.trim(),
                    lateFee = if (lateFeeAmount.isNotBlank()) lateFeeAmount.toDouble() else 0.0
                )
            )
        }
        return deposits
    }

    private fun hasDepositsChanged(original: DepositsWithMemberNames?, current: Deposits): Boolean {
        if (original == null) return true // New entry

        return original.depositAmount?.toString() != current.depositAmount.toString() ||
                original.depositedDate != current.depositedDate ||
                original.lateFee?.toString() != current.lateFee?.toString()
    }

}