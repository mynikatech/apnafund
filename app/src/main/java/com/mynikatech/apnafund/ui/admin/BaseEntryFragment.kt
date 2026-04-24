package com.mynikatech.apnafund.ui.admin

import android.app.DatePickerDialog
import android.graphics.Typeface
import android.os.Build
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.session.SessionManager
import com.mynikatech.apnafund.ui.viewmodel.FundViewModel
import com.mynikatech.apnafund.util.ApnaBankDate
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

abstract class BaseEntryFragment : Fragment() {

    protected val fundMap = mutableMapOf<String, Int>()
    protected val fundAmountMap = mutableMapOf<String, Double>()
    protected val fundLateFeeRate = mutableMapOf<String, Double>()
    protected val fundLastDepDate = mutableMapOf<String, Int>()
    protected val monthNameToNumber = ApnaBankDate.getMonthNameToNumberMap()
    private val calendar = Calendar.getInstance()
    protected val currentMonth = calendar.get(Calendar.MONTH) + 1
    protected val currentYear = calendar.get(Calendar.YEAR)
    private lateinit var fundDropdown: AutoCompleteTextView
    protected lateinit var monthList: List<String>
    protected lateinit var yearList: List<String>

    protected val isAdmin = SessionManager.isAdmin()
    private val moderatorGroupId = SessionManager.groupId ?: 0


    abstract fun loadFundDatesAndSetupMonthYear(selectedFundName: String)

    @RequiresApi(Build.VERSION_CODES.O)
    protected fun populateMonthAndYearDropdowns() {
        // Month List: Jan, Feb, ..., Dec
        monthList = Month.entries.map { month ->
            month.name.lowercase().replaceFirstChar { it.uppercase() }
        }

        // Year List: current year and previous 4 years
        val currentYear = LocalDate.now().year
        yearList = (currentYear downTo currentYear - 4).map { it.toString() }
    }

    protected fun showDatePickerDialog(targetEditText: EditText) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val dateStr = String.format(
                    Locale.getDefault(),
                    "%02d/%02d/%04d",
                    dayOfMonth,
                    month + 1,
                    year
                )
                targetEditText.setText(dateStr)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    protected fun setButtonState(button: View, enabled: Boolean) {
        button.isEnabled = enabled
        button.isClickable = enabled
        button.isFocusable = enabled
        button.alpha = if (enabled) 1.0f else 0.4f
    }

    protected fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    protected fun getValidMonthYearList(
        startDateStr: String,
        endDateStr: String
    ): List<Pair<String, String>> {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val start = LocalDate.parse(startDateStr, formatter).withDayOfMonth(1)
        val end = LocalDate.parse(endDateStr, formatter).withDayOfMonth(1)

        val result = mutableListOf<Pair<String, String>>()
        var date = start

        while (!date.isAfter(end)) {
            val monthName = date.month.name.lowercase().replaceFirstChar { it.uppercase() }
            result.add(Pair(monthName, date.year.toString()))
            date = date.plusMonths(1)
        }
        return result
    }

    protected fun setupDropdownWithAdapter(
        dropdown: AutoCompleteTextView,
        values: List<String>,
        defaultValue: String? = null
    ) {
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.dropdown_item_apnabank,
            values
        )
        dropdown.setAdapter(adapter)

        dropdown.threshold = 0
        // Optional default selection (e.g. current month/year)
        defaultValue?.let {
            dropdown.setText(it, false)
        }

        dropdown.setOnClickListener { dropdown.showDropDown() }
        dropdown.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) dropdown.showDropDown()
        }
        dropdown.setOnEditorActionListener { _, _, _ ->
            hideKeyboard(dropdown)
            true
        }
    }

    protected fun setupFundDropdown(
        fundDropdown: AutoCompleteTextView,
        fundViewModel: FundViewModel,
        onFundSelected: (String) -> Unit
    ) {
        this.fundDropdown = fundDropdown

        viewLifecycleOwner.lifecycleScope.launch {
            val funds = fundViewModel.fetchAllActiveFunds(isAdmin, moderatorGroupId, SessionManager.userId)
            val fundNames = funds.map { it.fundName }

            fundMap.clear()
            fundAmountMap.clear()
            fundLastDepDate.clear()
            fundLateFeeRate.clear()

            fundMap.putAll(funds.associate { it.fundName to it.fundId })
            fundAmountMap.putAll(funds.associate { it.fundName to it.recurringDepositAmount })
            fundLastDepDate.putAll(funds.associate { it.fundName to it.monthlyDepDateBy })
            fundLateFeeRate.putAll(funds.associate { it.fundName to it.lateFeeRate })

            val adapter = ArrayAdapter(
                requireContext(),
                R.layout.dropdown_item_apnabank,
                fundNames
            )
            fundDropdown.setAdapter(adapter)

            if (fundNames.isNotEmpty()) {
                fundDropdown.setText(fundNames.first(), false)
                onFundSelected(fundNames.first())
            }

            fundDropdown.threshold = 0
            fundDropdown.setOnClickListener { fundDropdown.showDropDown() }
            fundDropdown.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) fundDropdown.showDropDown()
            }

            fundDropdown.setOnItemClickListener { parent, _, position, _ ->
                val selectedName = parent.getItemAtPosition(position) as String
                fundDropdown.setText(selectedName, false)
                onFundSelected(selectedName)
            }

            fundDropdown.setOnEditorActionListener { _, _, _ ->
                hideKeyboard(fundDropdown)
                true
            }
        }
    }

    private fun hideKeyboard(targetView: View) {
        val imm =
            requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(targetView.windowToken, 0)
    }

    protected fun calculateLateFee(
        depositDate: String,
        lastDepDate: String,
        lateFeeRate: Double
    ): Double {
        val depDate = ApnaBankDate.parseDate(depositDate)
        val lastDepositDate = ApnaBankDate.parseDate(lastDepDate)
        val daysOverDue = ApnaBankDate.getDaysBetween(lastDepositDate, depDate)
        return if (daysOverDue > 0) lateFeeRate * daysOverDue else 0.0
    }


    protected fun createTextView(
        text: String,
        bold: Boolean = false,
        header: Boolean = false,
        gravity: Int? = null
    ): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            setPadding(8, 8, 8, 8)

            this.gravity = gravity ?: if (header) Gravity.CENTER else Gravity.START

            if (bold) setTypeface(null, Typeface.BOLD)
        }
    }

    // tableType = 0 for Deposits Table and 1 for loan Emis
    protected fun createHeaderRow(tableType: Int): TableRow {
        val header = TableRow(requireContext())
        header.addView(createTextView(getString(R.string.header_member_name), bold = true, header = true,gravity = Gravity.START))
        header.addView(createTextView(getString(R.string.text_amount), bold = true, header = true))
        header.addView(createTextView(getString(R.string.text_late_fee), bold = true, header = true))
        if (tableType == 1)
            header.addView(createTextView(getString(R.string.text_prepay), bold = true, header = true))
        header.addView(createTextView(getString(R.string.text_date), bold = true, header = true))
        header.addView(createTextView(getString(R.string.text_edit), bold = true, header = true))
        return header
    }

    companion object {
        const val TABLE_TYPE_LOAN = 1
        const val TABLE_TYPE_DEPOSIT = 0

    }

}