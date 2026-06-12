package com.mynikatech.apnafund.ui.fund

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.print.PrintManager
import android.provider.MediaStore
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.constants.ApnaBankConstants
import com.mynikatech.apnafund.data.model.FundWithDetails
import com.mynikatech.apnafund.data.model.LoanDetailsWithMemberNames
import com.mynikatech.apnafund.data.model.SummaryExportModel
import com.mynikatech.apnafund.data.model.SummaryExportRow
import com.mynikatech.apnafund.databinding.FragmentFundDetailsBinding
import com.mynikatech.apnafund.databinding.LayoutExportRowBinding
import com.mynikatech.apnafund.databinding.LayoutExportSummaryBinding
import com.mynikatech.apnafund.net.dto.FundAvailabilityDto
import com.mynikatech.apnafund.net.dto.FundMemberFinancialSummaryDto
import com.mynikatech.apnafund.session.SessionManager
import com.mynikatech.apnafund.ui.loan.AddLoanDialog
import com.mynikatech.apnafund.ui.user.UserFundDepositsFragmentArgs
import com.mynikatech.apnafund.ui.viewmodel.FundViewModel
import com.mynikatech.apnafund.ui.viewmodel.LoansViewModel
import com.mynikatech.apnafund.ui.viewmodel.UserSummaryViewModel
import com.mynikatech.apnafund.util.Converters
import com.mynikatech.apnafund.util.FundDetailsTableRowViewHolder
import com.mynikatech.apnafund.util.showSuccessSnackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class FundDetailsFragment : Fragment() {
    private lateinit var binding: FragmentFundDetailsBinding

    private lateinit var tableLayoutFundDetails: TableLayout

    private val userSummaryViewModel: UserSummaryViewModel by viewModels()

    private val fundViewModel: FundViewModel by viewModels()

    private val loanViewModel: LoansViewModel by viewModels()

    private val args: UserFundDepositsFragmentArgs by navArgs()

    private var exportMemberSummaries = emptyList<FundMemberFinancialSummaryDto>()

    override fun onCreateView(

        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentFundDetailsBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)
        val fundId = args.fundId
        lifecycleScope.launch {
            val fundDetails = userSummaryViewModel.getFundDetails(fundId)
            fetchAllFundDetails(fundDetails)
        }
        binding.buttonShareSummary.setOnClickListener {

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
        val toolbar = view.findViewById<MaterialToolbar>(R.id.fund_details_toolbar)
        // Enable back arrow
        val navController = findNavController()
        toolbar.setNavigationIcon(R.drawable.ic_back_arrow) // your back icon
        toolbar.setNavigationOnClickListener {
            navController.navigateUp()
        }
        toolbar.title = getString(R.string.title_fund_detailed_summary)
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

    private fun shareSummaryPdf() {

        lifecycleScope.launch(Dispatchers.IO) {

            try {

                val bitmap = generateSummaryBitmap()

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

    private fun shareSummaryImage() {

        lifecycleScope.launch(Dispatchers.IO) {

            try {

                val bitmap = generateSummaryBitmap()

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

    private fun saveSummaryToGallery() {

        lifecycleScope.launch(Dispatchers.IO) {

            try {

                val bitmap = generateSummaryBitmap()

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

                val bitmap = generateSummaryBitmap()

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

                val bitmap = generateSummaryBitmap()

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

    private suspend fun generateSummaryBitmap(): Bitmap =
        withContext(Dispatchers.Default) {

            val exportModel = buildExportModel()

            val inflater = LayoutInflater.from(requireContext())

            val binding = LayoutExportSummaryBinding.inflate(inflater)

            binding.textTitle.text = exportModel.title

            binding.textSummaryStart.text =
                this@FundDetailsFragment.binding
                    .textSummaryStart.text

            binding.textSummaryEnd.text =
                this@FundDetailsFragment.binding
                    .textSummaryEnd.text

            binding.textSummaryMembers.text =
                this@FundDetailsFragment.binding
                    .textSummaryMembers.text

            binding.textSummaryDeposit.text =
                this@FundDetailsFragment.binding
                    .textSummaryDeposit.text

            binding.textSummaryCurrent.text =
                this@FundDetailsFragment.binding
                    .textSummaryCurrent.text

            binding.textSummaryMaturity.text =
                this@FundDetailsFragment.binding
                    .textSummaryMaturity.text

            /*
             * HEADER ROW
             */
            val headerBinding = LayoutExportRowBinding.inflate(
                inflater,
                binding.layoutContainer,
                false
            )

            headerBinding.rowContainer.removeAllViews()

            exportModel.headers.forEachIndexed { index, header ->

                val textView = TextView(requireContext()).apply {

                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        getColumnWeight(index)
                    )

                    text = header
                    maxLines = 2
                    gravity = when (index) {

                        //0 -> Gravity.CENTER

                        0 -> Gravity.START or Gravity.CENTER_VERTICAL

                        else -> Gravity.CENTER or Gravity.CENTER_VERTICAL
                    }
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(Color.BLACK)
                    val px = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_SP,
                        13f,
                        resources.displayMetrics
                    )

                    setTextSize(TypedValue.COMPLEX_UNIT_PX, px)

                    setPadding(6, 8, 6, 8)
                }

                headerBinding.rowContainer.addView(textView)
            }

            binding.layoutContainer.addView(headerBinding.root)

            /*
             * DATA ROWS
             */
            exportModel.rows.forEach { row ->

                val rowBinding = LayoutExportRowBinding.inflate(
                    inflater,
                    binding.layoutContainer,
                    false
                )

                rowBinding.rowContainer.removeAllViews()

                row.columns.forEachIndexed { index, column ->

                    val textView = TextView(requireContext()).apply {

                        layoutParams = LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            getColumnWeight(index)
                        )

                        text = column
                        setTextColor(Color.BLACK)
                        gravity = when (index) {

                            //0 -> Gravity.CENTER

                            0 -> Gravity.START or Gravity.CENTER_VERTICAL

                            else -> Gravity.END or Gravity.CENTER_VERTICAL
                        }
                        val px = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_SP,
                            13f,
                            resources.displayMetrics
                        )

                        setTextSize(TypedValue.COMPLEX_UNIT_PX, px)

                        setPadding(6, 8, 6, 8)

                    }

                    rowBinding.rowContainer.addView(textView)
                }

                binding.layoutContainer.addView(rowBinding.root)
            }

            //val displayMetrics = resources.displayMetrics

            //val width = displayMetrics.widthPixels
            val width = ApnaBankConstants.EXPORT_WIDTH

            val widthSpec = View.MeasureSpec.makeMeasureSpec(
                width,
                View.MeasureSpec.EXACTLY
            )

            val heightSpec = View.MeasureSpec.makeMeasureSpec(
                0,
                View.MeasureSpec.UNSPECIFIED
            )

            binding.root.measure(widthSpec, heightSpec)

            binding.root.layout(
                0,
                0,
                binding.root.measuredWidth,
                binding.root.measuredHeight
            )
            val scale = 1f
            val bitmap = Bitmap.createBitmap(
                binding.root.measuredWidth,
                binding.root.measuredHeight,
                Bitmap.Config.ARGB_8888
            )

            val canvas = Canvas(bitmap)
            canvas.scale(scale, scale)
            binding.root.draw(canvas)

            bitmap
        }

    private fun getColumnWeight(index: Int): Float {

        return when (index) {

            //0 -> 0.5f   // No

            0 -> 1.6f   // Member Name

            5 -> 1.6f   // Expected Maturity

            else -> 1.3f
        }
    }

    private fun buildExportModel(): SummaryExportModel {

        val headers = listOf(
            //"No",
            "Member Name",
            "Deposit Amount",
            "Loan Amount",
            "Pending Amount",
            "Interest Paid",
            "Expected Maturity"
        )

        val rows = exportMemberSummaries.mapIndexed { index, member ->

            SummaryExportRow(

                columns = listOf(

                    // (index + 1).toString(),

                    "${member.firstName} ${member.lastName}",

                    Converters.formatCurrency(
                        member.totalDeposit
                    ),

                    if (member.totalLoanAmount > 0)
                        Converters.formatCurrency(
                            member.totalLoanAmount
                        )
                    else
                        ApnaBankConstants.ZERO_AMOUNT,

                    if (member.totalOutstandingAmount > 0)
                        Converters.formatCurrency(
                            member.totalOutstandingAmount
                        )
                    else
                        ApnaBankConstants.ZERO_AMOUNT,

                    if (member.totalCurrIntPaid > 0)
                        Converters.formatCurrency(
                            member.totalCurrIntPaid
                        )
                    else
                        ApnaBankConstants.ZERO_AMOUNT,

                    if (member.expectedMatAmount > 0)
                        Converters.formatCurrency(
                            member.expectedMatAmount
                        )
                    else
                        ApnaBankConstants.ZERO_AMOUNT
                )
            )
        }

        return SummaryExportModel(
            title = binding.textViewFundName.text.toString(),
            subTitle = "",
            headers = headers,
            rows = rows
        )
    }



    private fun getBitmapFromView(view: View): Bitmap {

        val width =
            view.width

        val height =
            view.height

        val bitmap = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)

        view.draw(canvas)

        return bitmap
    }

    private fun saveBitmap(bitmap: Bitmap): Uri {

        val imagesFolder = File(
            requireContext().cacheDir,
            "images"
        )

        imagesFolder.mkdirs()

        val file = File(
            imagesFolder,
            "fund_summary_${System.currentTimeMillis()}.png"
        )

        val stream = FileOutputStream(file)

        bitmap.compress(
            Bitmap.CompressFormat.PNG,
            100,
            stream
        )

        stream.flush()
        stream.close()

        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            file
        )
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

    private fun fetchAllFundDetails(fund: FundWithDetails) {

        lifecycleScope.launch {

            binding.textViewFundName.text = fund.fundName



            tableLayoutFundDetails = binding.tableFundDetails

            cleanTable(tableLayoutFundDetails)

            val memberSummaries =
                fundViewModel.getFundMembersFinancialSummForFund(
                    fund.fundId
                )
            exportMemberSummaries = memberSummaries
            val activeMembers = memberSummaries.size
            binding.textSummaryStart.text =
                buildSummaryText(
                    getString(R.string.label_start),
                    fund.fundStartDate
                )

            binding.textSummaryEnd.text =
                buildSummaryText(
                    getString(R.string.label_end),
                    fund.fundMaturityDate
                )

            binding.textSummaryMembers.text =
                buildSummaryText(
                    getString(R.string.label_members),
                    activeMembers.toString()
                )
            binding.textSummaryDeposit.text =
                buildSummaryText(
                    getString(R.string.label_deposit),
                    Converters.formatCurrency(
                        fund.totalCurrentDeposit
                    )
                )

            binding.textSummaryCurrent.text =
                buildSummaryText(
                    getString(R.string.label_curramt),
                    Converters.formatCurrency(
                        fund.totalCurrAmount
                    )
                )

            binding.textSummaryMaturity.text =
                buildSummaryText(
                    getString(R.string.label_maturity),
                    Converters.formatCurrency(
                        fund.totalExpectedMaturityAmount
                    )
                )

            val applyLoanPriv = SessionManager.canManageLoanEmi()

            memberSummaries.forEachIndexed { index, member ->

                val viewHolder = FundDetailsTableRowViewHolder(
                    requireContext(),
                    applyLoanPriv
                )

                viewHolder.tvNo.text = (index + 1).toString()

                viewHolder.tvName.text =
                    getString(
                        R.string.text_member_name,
                        member.firstName,
                        member.lastName
                    )

                viewHolder.tvDepAmt.text =
                    Converters.formatCurrency(member.totalDeposit)

                viewHolder.tvLoanAmt.text =
                    if (member.totalLoanAmount > 0)
                        Converters.formatCurrency(member.totalLoanAmount)
                    else
                        ApnaBankConstants.ZERO_AMOUNT

                viewHolder.tvTotPendingAmt.text =
                    if (member.totalOutstandingAmount > 0)
                        Converters.formatCurrency(
                            member.totalOutstandingAmount
                        )
                    else
                        ApnaBankConstants.ZERO_AMOUNT

                viewHolder.tvTotIntPaid.text =
                    if (member.totalCurrIntPaid > 0)
                        Converters.formatCurrency(
                            member.totalCurrIntPaid
                        )
                    else
                        ApnaBankConstants.ZERO_AMOUNT

                viewHolder.tvExpMatAmt.text =
                    if (member.expectedMatAmount > 0)
                        Converters.formatCurrency(
                            member.expectedMatAmount
                        )
                    else
                        ApnaBankConstants.ZERO_AMOUNT

                viewHolder.tvApplyLoan.setOnClickListener {

                    showApplyLoanDialog(
                        member.userId,
                        fund.fundId,
                        borrowerName =
                            "${member.firstName} ${member.lastName}"
                    )
                }

                tableLayoutFundDetails.addView(viewHolder.row)
            }
        }
    }

    private fun cleanTable(table: TableLayout) {
        val childCount = table.childCount

        // Remove all rows except the first header row
        if (childCount > 1) {
            table.removeViews(1, childCount - 1)
        }
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val activity = requireActivity() as AppCompatActivity
        activity.supportActionBar?.show()
    }

    @Suppress("DEPRECATION")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                requireActivity().onBackPressedDispatcher.onBackPressed()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showApplyLoanDialog(
        borrowerUserId: Int,
        fundId: Int,
        borrowerName: String,
        existingLoan: LoanDetailsWithMemberNames? = null,
        allowEditLoan: Boolean = true
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            val fund = userSummaryViewModel.getFund(fundId)!!
            val fundRateOfInterest = fund.loanInterestRate
            var totalAmounts: FundAvailabilityDto? = null
            //Get current available amount in the Fund display to the user and also add a validation
            try {
                totalAmounts = userSummaryViewModel.getAllAmountAvailableforFund(fundId)
            } catch (e: Exception) {
                Log.e("LoanDialog", "Error fetching fund amounts", e)

                // Optional: show message
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_unable_fetch_fund_details),
                    Toast.LENGTH_SHORT
                ).show()
            }

            val dialog = AddLoanDialog(
                borrowerName = borrowerName,
                rateOfInterest = fundRateOfInterest,
                fundMaturityDate = fund.fundMaturityDate,
                fundStartDate = fund.fundStartDate,
                existingLoan = existingLoan,
                allowEditLoan = allowEditLoan,
                totalAmounts = totalAmounts,
                borrowerUserId = borrowerUserId

            ) { loanAmount, issueDate, period, loanMaturityDate, borrowerUserId ->
                if (existingLoan != null) {
                    existingLoan.loanId?.let {
                        loanViewModel.updateLoan(
                            loanId = it,
                            loanAmount = loanAmount,
                            issueDate = issueDate,
                            period = period.toDouble(),
                            maturityDate = loanMaturityDate,
                            existingloan = existingLoan
                        )
                    }
                } else {
                    userSummaryViewModel.applyLoan(
                        userId = borrowerUserId,
                        fundId = fundId,
                        loanAmount = loanAmount,
                        issueDate = issueDate,
                        period = period.toDouble(),
                        rateOfInt = fundRateOfInterest,
                        maturityDate = loanMaturityDate,
                        autoapprove = true,
                        requestorId = SessionManager.userId
                    )
                    showSuccessSnackbar(
                        getString(R.string.message_loan_created_approved)
                    )
                }
            }

            dialog.show(parentFragmentManager, ApnaBankConstants.TEXT_ADD_LOAN_DIALOG)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        (activity as? AppCompatActivity)?.setSupportActionBar(null)
    }
}