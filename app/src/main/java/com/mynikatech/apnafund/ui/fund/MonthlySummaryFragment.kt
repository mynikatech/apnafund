package com.mynikatech.apnafund.ui.fund

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.icu.text.DateFormatSymbols
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.print.PrintManager
import android.provider.MediaStore
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.constants.ApnaBankConstants
import com.mynikatech.apnafund.data.model.Funds
import com.mynikatech.apnafund.data.model.SummaryExportModel
import com.mynikatech.apnafund.data.model.SummaryExportRow
import com.mynikatech.apnafund.databinding.ExportMonthlySummaryBinding
import com.mynikatech.apnafund.databinding.FragmentMonthlySummaryBinding
import com.mynikatech.apnafund.databinding.ItemExportMonthlySummaryRowBinding
import com.mynikatech.apnafund.net.dto.MonthlyFinancialSummaryResponseDto
import com.mynikatech.apnafund.net.dto.MonthlyMemberFinancialSummaryDto
import com.mynikatech.apnafund.ui.viewmodel.FundViewModel
import com.mynikatech.apnafund.util.ApnaBankDate
import com.mynikatech.apnafund.util.Converters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class MonthlySummaryFragment : Fragment() {

    private var _binding: FragmentMonthlySummaryBinding? = null

    private val binding
        get() = _binding!!

    private val fundViewModel: FundViewModel by viewModels()
    private var validMonthYearList = emptyList<Pair<String, String>>()
    private var currentSummary:
            MonthlyFinancialSummaryResponseDto? = null

    protected val monthNameToNumber = ApnaBankDate.getMonthNameToNumberMap()

    private var selectedFund: Funds? = null

    private var selectedMonth: Int? = null

    private var selectedYear: Int? = null

    private var fundId: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentMonthlySummaryBinding.inflate(
                inflater,
                container,
                false
            )
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(
            view,
            savedInstanceState
        )
        fundId = arguments?.getInt("fundId") ?: -1

        setupToolbar(view)

        setupListeners()
        loadFundAndSetupDropdowns()

    }

    private fun setupToolbar(view: View) {

        val toolbar = view.findViewById<MaterialToolbar>(R.id.monthly_summary_toolbar)
        // Enable back arrow
        val navController = findNavController()
        toolbar.setNavigationIcon(R.drawable.ic_back_arrow)
        toolbar.setNavigationOnClickListener {
            navController.navigateUp()
        }
        toolbar.title = getString(R.string.title_monthly_fund_summary)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadFundAndSetupDropdowns() {

        lifecycleScope.launch {

            val fund =
                fundViewModel.fetchFund(fundId)
                    ?: return@launch

            selectedFund = fund

            binding.textViewFundName.text =
                fund.fundName

            validMonthYearList =
                getValidMonthYearList(
                    fund.fundStartDate,
                    fund.fundMaturityDate
                )

            val validMonths =
                validMonthYearList
                    .map { it.first }
                    .distinct()

            val validYears =
                validMonthYearList
                    .map { it.second }
                    .distinct()

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

            val current =
                Calendar.getInstance()

            val currentMonth =
                current.getDisplayName(
                    Calendar.MONTH,
                    Calendar.LONG,
                    Locale.getDefault()
                ) ?: ""

            val currentYear =
                current.get(Calendar.YEAR)
                    .toString()

            val defaultMonth =
                if (validMonths.contains(currentMonth))
                    currentMonth
                else
                    validMonths.first()

            val defaultYear =
                if (validYears.contains(currentYear))
                    currentYear
                else
                    validYears.first()

            binding.dropdownMonth.setText(
                defaultMonth,
                false
            )

            binding.dropdownYear.setText(
                defaultYear,
                false
            )

            selectedMonth =
                monthNameToNumber[defaultMonth]
                    ?: (Calendar.getInstance().get(Calendar.MONTH) + 1)

            selectedYear =
                defaultYear.toIntOrNull()

            updateHeader()

            fetchSummary()
        }
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


    private fun setupListeners() {

        binding.dropdownMonth
            .setOnItemClickListener { _, _, _, _ ->

                selectedMonth =
                    monthNameToNumber[
                        binding.dropdownMonth.text.toString()
                    ]

                updateHeader()

                fetchSummary()
            }

        binding.dropdownYear
            .setOnItemClickListener { _, _, _, _ ->

                selectedYear =
                    binding.dropdownYear.text.toString()
                        .toIntOrNull()

                updateHeader()

                fetchSummary()
            }

        binding.buttonExportSummary
            .setOnClickListener {

                ExportOptionsBottomSheet(
                    requireContext(),
                    object : ExportOptionsBottomSheet.Listener {

                        override fun onShareImage() {
                            showShareOptions()
                        }

                        override fun onSaveToGallery() {
                            saveSummaryToGallery()
                        }

                        override fun onSaveAsPdf() {
                            exportSummaryPdf()
                        }

                        override fun onPrint() {
                            printSummary()
                        }
                    }
                ).show()
            }
    }

    private fun saveSummaryToGallery() {

        lifecycleScope.launch(Dispatchers.IO) {

            try {

                val bitmap = generateMonthlySummaryBitmap()

                val filename = "summary_${System.currentTimeMillis()}.png"

                val resolver = requireContext().contentResolver

                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/ApnaBank"
                    )
                }

                val imageUri = resolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )

                imageUri?.let { uri ->

                    resolver.openOutputStream(uri)?.use { outputStream ->
                        bitmap.compress(
                            Bitmap.CompressFormat.PNG,
                            100,
                            outputStream
                        )
                    }
                }

                withContext(Dispatchers.Main) {

                    Toast.makeText(
                        requireContext(),
                        "Saved to Gallery",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {

                withContext(Dispatchers.Main) {

                    Toast.makeText(
                        requireContext(),
                        "Failed to save image",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun exportSummaryPdf() {

        lifecycleScope.launch(Dispatchers.IO) {

            try {

                val bitmap = generateMonthlySummaryBitmap()

                val pdfDocument = PdfDocument()

                val pageInfo = PdfDocument.PageInfo.Builder(
                    bitmap.width,
                    bitmap.height,
                    1
                ).create()

                val page = pdfDocument.startPage(pageInfo)

                page.canvas.drawBitmap(bitmap, 0f, 0f, null)

                pdfDocument.finishPage(page)

                val filename = "summary_${System.currentTimeMillis()}.pdf"

                val resolver = requireContext().contentResolver

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_DOCUMENTS + "/ApnaBank"
                    )
                }

                val uri = resolver.insert(
                    MediaStore.Files.getContentUri("external"),
                    contentValues
                )

                uri?.let {

                    resolver.openOutputStream(it)?.use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }
                }

                pdfDocument.close()

                withContext(Dispatchers.Main) {

                    Toast.makeText(
                        requireContext(),
                        "PDF Saved",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {

                withContext(Dispatchers.Main) {

                    Toast.makeText(
                        requireContext(),
                        "Failed to export PDF",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun printSummary() {

        lifecycleScope.launch(Dispatchers.IO) {

            try {

                val bitmap = generateMonthlySummaryBitmap()

                withContext(Dispatchers.Main) {

                    val printManager = requireContext()
                        .getSystemService(Context.PRINT_SERVICE)
                            as PrintManager

                    val jobName = "ApnaBank Summary"

                    printManager.print(
                        jobName,
                        BitmapPrintDocumentAdapter(
                            requireContext(),
                            bitmap
                        ),
                        null
                    )
                }

            } catch (e: Exception) {

                withContext(Dispatchers.Main) {

                    Toast.makeText(
                        requireContext(),
                        "Failed to print",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun buildSummaryText(
        label: String,
        value: String
    ): SpannableStringBuilder {

        return SpannableStringBuilder().apply {

            val start = length

            append("$label ")

            setSpan(
                StyleSpan(Typeface.BOLD),
                start,
                length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            setSpan(
                ForegroundColorSpan(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.purple
                    )
                ),
                start,
                length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            append(value)
        }
    }

    private fun updateHeader() {

        binding.textViewFundName.text =
            selectedFund?.fundName ?: ""

        val month =
            selectedMonth?.let {

                DateFormatSymbols().months[it - 1]

            } ?: ""

        val year =
            selectedYear?.toString() ?: ""

        binding.textViewMonthYear.text =
            "$month $year"
    }

    private fun fetchSummary() {

        val month =
            selectedMonth ?: return

        val year =
            selectedYear ?: return

        lifecycleScope.launch {

            val response =
                fundViewModel.getFundMembersMonthlyFinancialSummForFund(
                    fundId,
                    month,
                    year
                )
            currentSummary = response

            updateSummary(response)

            populateTable(response.members)
        }
    }

    private fun updateSummary(
        summary: MonthlyFinancialSummaryResponseDto
    ) {

        binding.textSummaryDeposit.text =
            buildSummaryText(
                getString(R.string.text_summary_deposit),
                Converters.formatCurrency(
                    summary.totalDepositAmount ?: 0.0
                )
            )

        binding.textSummaryLoans.text =
            buildSummaryText(
                getString(R.string.text_summary_loans),
                Converters.formatCurrency(
                    summary.totalLoanIssuedAmount ?: 0.0
                )
            )

        binding.textSummaryInterest.text =
            buildSummaryText(
                getString(R.string.text_summary_interest),
                Converters.formatCurrency(
                    summary.totalInterestPaidAmount ?: 0.0
                )
            )

        binding.textSummaryFees.text =
            buildSummaryText(
                getString(R.string.text_summary_fees),
                Converters.formatCurrency(
                    summary.totalFeesPaidAmount ?: 0.0
                )
            )

        binding.textSummaryPrepayment.text =
            buildSummaryText(
                getString(R.string.text_summary_prepayment),
                Converters.formatCurrency(
                    summary.totalPrepaymentAmount ?: 0.0
                )
            )

        binding.textSummaryMembers.text =
            buildSummaryText(
                getString(R.string.text_summary_members),
                summary.totalMembers.toString()
            )
    }

    private fun populateTable(
        members: List<MonthlyMemberFinancialSummaryDto>
    ) {

        val table =
            binding.tableMonthlySummary

        while (table.childCount > 1) {

            table.removeViewAt(1)
        }

        members.forEach { member ->

            val row =
                TableRow(requireContext()).apply {

                    layoutParams =
                        TableRow.LayoutParams(
                            TableRow.LayoutParams.MATCH_PARENT,
                            TableRow.LayoutParams.WRAP_CONTENT
                        )
                }

            row.addView(
                createCell(
                    buildString {

                        append(member.firstName)

                        member.lastName?.let {

                            append(" ")

                            append(it)
                        }
                    }
                )
            )

            row.addView(
                createAmountCell(
                    member.depositAmount
                )
            )

            row.addView(
                createAmountCell(
                    member.loanIssuedAmount
                )
            )

            row.addView(
                createAmountCell(
                    member.loanPrepaymentAmount
                )
            )

            row.addView(
                createAmountCell(
                    member.interestPaidAmount
                )
            )

            row.addView(
                createAmountCell(
                    member.feesPaidAmount
                )
            )

            table.addView(row)
        }
    }

    private fun createCell(
        value: String
    ): TextView {

        return TextView(requireContext()).apply {

//            setTextAppearance(
//                R.style.ApnaFund_TableCellText
//            )

            background =
                ContextCompat.getDrawable(
                    context,
                    R.drawable.bg_table_cell
                )

            text = value
        }
    }

    private fun createAmountCell(
        amount: Double?
    ): TextView {

        return TextView(requireContext()).apply {

//            setTextAppearance(
//                R.style.ApnaFund_TableCellAmount
//            )

            background =
                ContextCompat.getDrawable(
                    context,
                    R.drawable.bg_table_cell
                )

            text =
                amount?.let {

                    Converters.formatCurrency(it)

                } ?: "-"
        }
    }

    private fun showShareOptions() {

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(
                R.string.title_export_summary
            )
            .setItems(
                arrayOf(
                    getString(
                        R.string.text_share_as_image
                    ),
                    getString(
                        R.string.text_share_as_pdf
                    )
                )
            ) { _, which ->

                when (which) {

                    0 -> shareSummaryImage()

                    1 -> shareSummaryPdf()
                }
            }
            .show()
    }

    private fun shareSummaryImage() {

        lifecycleScope.launch(Dispatchers.IO) {

            try {

                val bitmap = generateMonthlySummaryBitmap()

                val file = File(
                    requireContext().cacheDir,
                    "summary_${System.currentTimeMillis()}.png"
                )

                FileOutputStream(file).use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }

                val uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.provider",
                    file
                )

                withContext(Dispatchers.Main) {

                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    startActivity(
                        Intent.createChooser(intent, "Share Summary")
                    )
                }

            } catch (e: Exception) {

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Failed to share image",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun shareSummaryPdf() {

        lifecycleScope.launch(Dispatchers.IO) {

            try {

                val bitmap = generateMonthlySummaryBitmap()

                val file = File(
                    requireContext().cacheDir,
                    "summary_${System.currentTimeMillis()}.pdf"
                )

                val pdfDocument = PdfDocument()

                val pageInfo = PdfDocument.PageInfo.Builder(
                    bitmap.width,
                    bitmap.height,
                    1
                ).create()

                val page = pdfDocument.startPage(pageInfo)

                page.canvas.drawBitmap(
                    bitmap,
                    0f,
                    0f,
                    null
                )

                pdfDocument.finishPage(page)

                FileOutputStream(file).use {
                    pdfDocument.writeTo(it)
                }

                pdfDocument.close()

                val uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.provider",
                    file
                )

                withContext(Dispatchers.Main) {

                    val intent = Intent(Intent.ACTION_SEND).apply {

                        type = "application/pdf"

                        putExtra(
                            Intent.EXTRA_STREAM,
                            uri
                        )

                        addFlags(
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }

                    startActivity(
                        Intent.createChooser(
                            intent,
                            "Share Summary PDF"
                        )
                    )
                }

            } catch (e: Exception) {

                withContext(Dispatchers.Main) {

                    Toast.makeText(
                        requireContext(),
                        "Failed to share PDF",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private suspend fun generateMonthlySummaryBitmap(): Bitmap =
        withContext(Dispatchers.Default) {

            val exportModel = buildExportModel()

            val inflater =
                LayoutInflater.from(requireContext())

            val binding =
                ExportMonthlySummaryBinding.inflate(
                    inflater
                )

            /*
             * HEADER DETAILS
             */

            binding.textExportFundName.text =
                this@MonthlySummaryFragment.binding
                    .textViewFundName.text

            binding.textExportMonthYear.text =
                this@MonthlySummaryFragment.binding
                    .textViewMonthYear.text

            /*
             * SUMMARY
             */

            binding.textExportSummaryDeposit.text =
                this@MonthlySummaryFragment.binding
                    .textSummaryDeposit.text

            binding.textExportSummaryLoans.text =
                this@MonthlySummaryFragment.binding
                    .textSummaryLoans.text

            binding.textExportSummaryInterest.text =
                this@MonthlySummaryFragment.binding
                    .textSummaryInterest.text

            binding.textExportSummaryPrepayment.text =
                this@MonthlySummaryFragment.binding
                    .textSummaryPrepayment.text

            binding.textExportSummaryFees.text =
                this@MonthlySummaryFragment.binding
                    .textSummaryFees.text

            binding.textExportSummaryMembers.text =
                this@MonthlySummaryFragment.binding
                    .textSummaryMembers.text



            exportModel.rows.forEach { row ->

                val rowBinding =
                    ItemExportMonthlySummaryRowBinding.inflate(
                        inflater,
                        binding.tableExportSummary,
                        false
                    )

                val textViews = listOf(

                    rowBinding.textMemberName,

                    rowBinding.textDeposit,

                    rowBinding.textLoan,

                    rowBinding.textPrepayment,

                    rowBinding.textInterest,

                    rowBinding.textFees
                )

                row.columns.forEachIndexed { index, column ->

                    textViews[index].apply {

                        text = column

                        gravity = when (index) {

                            0 ->
                                Gravity.START or Gravity.CENTER_VERTICAL

                            else ->
                                Gravity.END or Gravity.CENTER_VERTICAL
                        }

                        setTextColor(Color.BLACK)

                        val px =
                            TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_SP,
                                13f,
                                resources.displayMetrics
                            )

                        setTextSize(
                            TypedValue.COMPLEX_UNIT_PX,
                            px
                        )

                        setPadding(
                            6,
                            8,
                            6,
                            8
                        )
                    }
                }

                binding.tableExportSummary.addView(
                    rowBinding.root
                )
            }

            val width =
                ApnaBankConstants.EXPORT_WIDTH

            val widthSpec =
                View.MeasureSpec.makeMeasureSpec(
                    width,
                    View.MeasureSpec.EXACTLY
                )

            val heightSpec =
                View.MeasureSpec.makeMeasureSpec(
                    0,
                    View.MeasureSpec.UNSPECIFIED
                )

            binding.root.measure(
                widthSpec,
                heightSpec
            )

            binding.root.layout(
                0,
                0,
                binding.root.measuredWidth,
                binding.root.measuredHeight
            )

            val bitmap =
                Bitmap.createBitmap(
                    binding.root.measuredWidth,
                    binding.root.measuredHeight,
                    Bitmap.Config.ARGB_8888
                )

            val canvas =
                Canvas(bitmap)

            binding.root.draw(canvas)

            bitmap
        }

    private fun buildExportModel(): SummaryExportModel {

        val headers = listOf(

            getString(
                R.string.header_member_name
            ),

            getString(
                R.string.text_deposit_amount
            ),

            getString(
                R.string.text_loan_issued
            ),

            getString(
                R.string.text_loan_prepayment
            ),

            getString(
                R.string.text_interest_paid
            ),

            getString(
                R.string.text_fees
            )
        )

        val rows =
            currentSummary?.members?.map { member ->

                SummaryExportRow(

                    columns = listOf(

                        buildString {

                            append(member.firstName)

                            member.lastName?.let {

                                append(" ")

                                append(it)
                            }
                        },

                        member.depositAmount?.let {

                            Converters.formatCurrency(it)

                        } ?: ApnaBankConstants.ZERO_AMOUNT,

                        member.loanIssuedAmount?.let {

                            Converters.formatCurrency(it)

                        } ?: ApnaBankConstants.ZERO_AMOUNT,

                        member.loanPrepaymentAmount?.let {

                            Converters.formatCurrency(it)

                        } ?: ApnaBankConstants.ZERO_AMOUNT,

                        member.interestPaidAmount?.let {

                            Converters.formatCurrency(it)

                        } ?: ApnaBankConstants.ZERO_AMOUNT,

                        member.feesPaidAmount?.let {

                            Converters.formatCurrency(it)

                        } ?: ApnaBankConstants.ZERO_AMOUNT
                    )
                )

            } ?: emptyList()

        return SummaryExportModel(

            title =
                binding.textViewFundName.text
                    .toString(),

            subTitle =
                binding.textViewMonthYear.text
                    .toString(),

            headers = headers,

            rows = rows
        )
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}