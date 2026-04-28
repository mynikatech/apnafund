package com.mynikatech.apnafund.ui.loan

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.constants.ApnaBankConstants
import com.mynikatech.apnafund.data.model.LoanDetailsWithMemberNames
import com.mynikatech.apnafund.databinding.ItemLoanBinding
import com.mynikatech.apnafund.util.ApnaBankDate
import com.mynikatech.apnafund.util.Converters

class LoanAdapter(
    private val onEditLoanClick: (LoanDetailsWithMemberNames) -> Unit,
    private val onLoanEmiClick: (LoanDetailsWithMemberNames) -> Unit,
    private val onCloseLoanClick: (LoanDetailsWithMemberNames) -> Unit
) :
    RecyclerView.Adapter<LoanAdapter.ViewHolder>() {

    private var loans: List<LoanDetailsWithMemberNames> = listOf()

    private lateinit var context: Context

    private var isExpanded = false

    override fun getItemCount(): Int = loans.size

    fun setLoans(newLoans: List<LoanDetailsWithMemberNames>) {

        val diffCallback = object : DiffUtil.Callback() {

            override fun getOldListSize() = loans.size
            override fun getNewListSize() = newLoans.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return loans[oldItemPosition].loanId ==

                        newLoans[newItemPosition].loanId
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return loans[oldItemPosition] == newLoans[newItemPosition]
            }
        }

        val diffResult = DiffUtil.calculateDiff(diffCallback)

        loans = newLoans
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        context = parent.context
        return ViewHolder(ItemLoanBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(loans[position])
    }


    inner class ViewHolder(private val binding: ItemLoanBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(loan: LoanDetailsWithMemberNames) {

            binding.apply {
                textViewLoanNumberValue.text = loan.loanNumber

                textViewLoanAmountValue.text = Converters.formatCurrency(loan.loanAmount)
                val totalMonths = loan.period.toInt()
                 val formattedLoanPeriod = Converters.formatLoanPeriod(totalMonths)
                textViewLoanPeriodValue.text = formattedLoanPeriod
                textViewLoanIssueDateValue.text = loan.issuedDate
                textViewLoanMaturityDateValue.text = loan.maturityDate
                textViewCurrentEmiValue.text = Converters.formatCurrency(loan.emiInterest)
                // calculate emi value original from original principal, period and int
                val origEmi =
                    (loan.loanAmount * loan.rateOfInterest * loan.period / 12) / 100 / loan.period
                textViewLoanOrigEmiValue.text = Converters.formatCurrency(origEmi)
                textViewInterestPaidValue.text = Converters.formatCurrency(loan.currTotalIntPaid)
                textViewLoanOutstandingAmountValue.text =
                    Converters.formatCurrency(loan.currPrincipal)
                //setting closed field values
                textViewClosedDateValue.text = loan.closedDate
                val closureType = getClosureTypeDisplay(loan.closureType)
                textViewClosureTypeValue.text = closureType
                textViewClosureSourceValue.text = loan.closureSource
                textViewTotInterestPaidValue.text = Converters.formatCurrency(loan.totalInterest)
                textViewOriginalPeriodValue.text = formattedLoanPeriod
                textViewActualDurationValue.text = Converters.formatLoanPeriod(ApnaBankDate.getLoanDurationInMonths(loan.issuedDate, loan.closedDate))
                textViewLoanInterestRateValue.text = buildString {
                    append(loan.rateOfInterest.toString())
                    append("%")
                }

                val isClosed = loan.status == ApnaBankConstants.STATUS_CLOSED
                val isPending = loan.status == ApnaBankConstants.STATUS_PENDING
                if (isClosed) {
                    textViewLoanStatusValue.text = context.getString(R.string.status_closed)
                    textViewLoanStatusValue.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.status_rejected
                        )
                    )
                } else if (isPending) {
                    textViewLoanStatusValue.text = context.getString(R.string.status_pending)
                    textViewLoanStatusValue.setTypeface(null, Typeface.BOLD)
                    textViewLoanStatusValue.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.status_pending
                        )
                    )
                } else {

                    textViewLoanStatusValue.text = context.getString(R.string.status_active)
                    textViewLoanStatusValue.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.status_approved
                        )
                    )
                }

                binding.lineLoanDetails.visibility =
                    if (isExpanded) View.VISIBLE else View.GONE
                binding.loanDetailsSection.visibility =
                    if (isExpanded) View.VISIBLE else View.GONE
                binding.imageExpandCollapseLoan.setImageResource(
                    if (isExpanded) R.drawable.ic_up_arrow else R.drawable.ic_down_arrow
                )
                // VERY IMPORTANT → reset both (RecyclerView reuse fix)
                binding.layoutActiveSection.visibility = View.GONE
                binding.layoutClosedSection.visibility = View.GONE

                // 3Apply child logic ONLY when expanded
                if (isExpanded) {
                    if (isClosed) {
                        binding.layoutClosedSection.visibility = View.VISIBLE
                    } else {
                        binding.layoutActiveSection.visibility = View.VISIBLE
                    }
                }

                binding.imageMoreOptions.setOnClickListener { view ->

                    val popup = PopupMenu(view.context, view)
                    popup.menuInflater.inflate(R.menu.loan_item_menu, popup.menu)

                    val isClosed = loan.status == ApnaBankConstants.STATUS_CLOSED
                    val hasPaidEmi = loan.currTotalIntPaid > 0.0
                    val canEdit = !isClosed && !hasPaidEmi
                    val canClose = !isClosed

                    // Hide items instead of disabling
                    if (!canEdit) {
                        popup.menu.removeItem(R.id.menu_edit)
                    }

                    if (!canClose) {
                        popup.menu.removeItem(R.id.menu_close)
                    }

                    popup.setOnMenuItemClickListener { item ->
                        when (item.itemId) {

                            R.id.menu_edit -> {
                                onEditLoanClick(loan)
                                true
                            }

                            R.id.menu_close -> {
                                onCloseLoanClick(loan)
                                true
                            }

                            else -> false
                        }
                    }

                    popup.show()
                }

                imageExpandCollapseLoan.setOnClickListener {
                    isExpanded = !isExpanded
                    binding.lineLoanDetails.visibility =
                        if (isExpanded) View.VISIBLE else View.GONE
                    binding.loanDetailsSection.visibility =
                        if (isExpanded) View.VISIBLE else View.GONE
                    binding.imageExpandCollapseLoan.setImageResource(
                        if (isExpanded) R.drawable.ic_up_arrow else R.drawable.ic_down_arrow
                    )
                    if (isExpanded) {
                        if (isClosed) {
                            binding.layoutClosedSection.visibility = View.VISIBLE
                        } else {
                            binding.layoutActiveSection.visibility = View.VISIBLE
                        }
                    }
                }
                textViewLoanEmiDetails.setOnClickListener {
                    onLoanEmiClick(loan)
                }
            }
        }

    }

    fun getClosureTypeDisplay(type: String?): String {
        return when (type) {
            "FORECLOSURE" -> "Preclose"
            "MATURITY" -> "Matured"
            else -> "-"
        }
    }

}
